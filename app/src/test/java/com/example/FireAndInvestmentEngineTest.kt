package com.example

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.models.AppTab
import com.example.models.AssetHolding
import com.example.models.AssetType
import com.example.models.BudgetCategoryEntry
import com.example.models.CompoundingFrequency
import com.example.models.DebtAccount
import com.example.models.DebtPayoffStrategy
import com.example.models.FdParams
import com.example.models.FireParameters
import com.example.models.GoalParams
import com.example.models.InflationImpactParams
import com.example.models.LiabilityItem
import com.example.models.RecurringFrequency
import com.example.models.RecurringTargetLink
import com.example.models.RecurringTransaction
import com.example.models.RecurringType
import com.example.models.RetirementPlanParams
import com.example.models.SipParams
import com.example.models.SwpParams
import com.example.models.TaxParams
import com.example.models.TaxRegime
import com.example.providers.FireViewModel
import com.example.services.BudgetEngine
import com.example.services.CsvExportScope
import com.example.services.CsvExportService
import com.example.services.DailyTipProvider
import com.example.services.DebtPayoffEngine
import com.example.services.FireEngine
import com.example.services.InvestmentEngine
import com.example.services.RecurringTransactionEngine
import com.example.services.RetirementEngine
import com.example.services.TaxEngine
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FireAndInvestmentEngineTest {

    @Test
    fun `fire engine computes positive trajectory, tax drag, and coast fire`() {
        val params = FireParameters(
            currentAge = 30,
            targetAge = 50,
            currentSavings = 100_000.0,
            monthlySip = 2_000.0,
            stepUpRate = 0.05,
            expectedReturn = 0.08,
            inflationRate = 0.03,
            taxRate = 0.15,
            monthlyExpense = 4_000.0,
            safeWithdrawalRate = 0.04
        )
        val result = FireEngine.calculateFire(params)
        assertEquals(21, result.trajectory.size)
        assertTrue(result.grossAccumulated > result.netAccumulated)
        assertTrue(result.taxDrag > 0.0)
        assertTrue(result.coastFireNumber in 1.0..result.targetFireNumber)
    }

    @Test
    fun `monte carlo produces ordered percentile bands`() = runTest {
        val params = FireParameters(expectedReturn = 0.08, inflationRate = 0.03, monthlyExpense = 3_000.0)
        val mc = FireEngine.runMonteCarloSimulation(params, startBalance = 1_500_000.0, random = Random(42))
        assertEquals(41, mc.percentile50.size)
        assertTrue(mc.longevityProbability in 0.0..100.0)
        assertTrue(mc.longevity40Probability in 0.0..mc.longevityProbability)
        for (i in 0..40) {
            assertTrue(mc.percentile10[i] <= mc.percentile50[i])
            assertTrue(mc.percentile50[i] <= mc.percentile90[i])
        }
    }

    @Test
    fun `sip calculator computes maturity above invested and real value below nominal`() {
        val result = InvestmentEngine.calculateSip(
            SipParams(
                lumpsum = 50_000.0,
                monthlySip = 2_000.0,
                annualStepUpRate = 0.08,
                expectedReturn = 0.12,
                years = 15,
                inflationRate = 0.05
            )
        )
        assertEquals(16, result.timeline.size)
        assertTrue(result.maturityValue > result.totalInvested)
        assertTrue(result.wealthGain > 0.0)
        assertTrue(result.inflationAdjustedValue < result.maturityValue)
    }

    @Test
    fun `swp calculator detects depletion for excessive withdrawal and sustainability for conservative withdrawal`() {
        val aggressive = InvestmentEngine.calculateSwp(
            SwpParams(
                initialCorpus = 100_000.0,
                monthlyWithdrawal = 4_000.0,
                annualWithdrawalStepUp = 0.05,
                expectedReturn = 0.06,
                years = 20
            )
        )
        assertNotNull(aggressive.depletedYear)
        assertEquals(0.0, aggressive.finalBalance, 0.01)

        val conservative = InvestmentEngine.calculateSwp(
            SwpParams(
                initialCorpus = 1_000_000.0,
                monthlyWithdrawal = 2_500.0,
                annualWithdrawalStepUp = 0.02,
                expectedReturn = 0.09,
                years = 25
            )
        )
        assertNull(conservative.depletedYear)
        assertTrue(conservative.finalBalance > 0.0)
    }

    @Test
    fun `fd calculator applies compounding frequency and post-tax yield accurately`() {
        val quarterly = InvestmentEngine.calculateFd(
            FdParams(
                principal = 100_000.0,
                annualRate = 0.08,
                years = 5,
                compounding = CompoundingFrequency.QUARTERLY,
                taxRate = 0.30
            )
        )
        val yearly = InvestmentEngine.calculateFd(
            FdParams(
                principal = 100_000.0,
                annualRate = 0.08,
                years = 5,
                compounding = CompoundingFrequency.YEARLY,
                taxRate = 0.30
            )
        )
        assertTrue(quarterly.grossMaturity > yearly.grossMaturity)
        assertEquals(quarterly.grossInterest * 0.30, quarterly.taxDeducted, 0.01)
        assertTrue(quarterly.effectivePostTaxYield < 0.08)
    }

    @Test
    fun `goal calculator returns lower starting sip for 10 percent step-up than flat sip`() {
        val goal = InvestmentEngine.calculateGoal(
            GoalParams(
                targetGoal = 500_000.0,
                currentSavings = 50_000.0,
                years = 12,
                expectedReturn = 0.10,
                inflationRate = 0.05,
                adjustForInflation = true
            )
        )
        assertTrue(goal.inflationAdjustedGoal > 500_000.0)
        assertTrue(goal.requiredMonthlySipFlat > goal.requiredMonthlySipStepUp10)
        assertTrue(goal.requiredMonthlySipStepUp10 > 0.0)
    }

    @Test
    fun `portfolio engine blends equity, mutual funds, fd, and bonds accurately`() {
        val holdings = listOf(
            AssetHolding("1", "Stocks", AssetType.EQUITY, 50_000.0, 1_000.0, 0.12, 0.125),
            AssetHolding("2", "Mutual Fund", AssetType.MUTUAL_FUND, 25_000.0, 500.0, 0.10, 0.125),
            AssetHolding("3", "Fixed Deposit", AssetType.FIXED_DEPOSIT, 15_000.0, 0.0, 0.07, 0.30),
            AssetHolding("4", "Bonds", AssetType.BONDS, 10_000.0, 0.0, 0.08, 0.20)
        )
        val summary = InvestmentEngine.calculatePortfolio(holdings, projectionYears = 10)
        assertEquals(100_000.0, summary.totalCurrentValue, 0.01)
        assertEquals(1_500.0, summary.totalMonthlyContribution, 0.01)
        assertEquals(100.0, summary.allocationSlices.sumOf { it.percentage }, 0.01)
        assertTrue(summary.projectedNet10Yr < summary.projectedGross10Yr)
        assertEquals(11, summary.yearlyNetTrajectory.size)
    }

    @Test
    fun `portfolio engine computes unit-based market value, unrealized PnL, and net worth minus liabilities`() {
        val stockHolding = AssetHolding(
            id = "eq1",
            name = "Global Equity ETF",
            assetType = AssetType.EQUITY,
            currentValue = 0.0,
            monthlyContribution = 500.0,
            expectedReturn = 0.12,
            taxRate = 0.125,
            symbol = "VT",
            quantity = 200.0,
            avgBuyPrice = 90.0,
            currentMarketPrice = 110.0,
            investedCost = 18_000.0
        )
        val bondHolding = AssetHolding(
            id = "bd1",
            name = "Treasury Bond",
            assetType = AssetType.BONDS,
            currentValue = 30_000.0,
            monthlyContribution = 0.0,
            expectedReturn = 0.06,
            taxRate = 0.20,
            investedCost = 28_000.0
        )
        assertEquals(22_000.0, stockHolding.effectiveMarketValue, 0.01)
        assertEquals(18_000.0, stockHolding.effectiveCostBasis, 0.01)
        assertEquals(4_000.0, stockHolding.unrealizedPnL, 0.01)
        assertEquals(22.22, stockHolding.unrealizedPnLPercent, 0.05)

        val liabilities = listOf(
            LiabilityItem(id = "l1", name = "Auto Loan", category = "Auto Loan", balance = 12_000.0)
        )
        val summary = InvestmentEngine.calculatePortfolio(
            holdings = listOf(stockHolding, bondHolding),
            projectionYears = 10,
            liabilities = liabilities
        )
        assertEquals(52_000.0, summary.totalCurrentValue, 0.01)
        assertEquals(46_000.0, summary.totalInvestedCost, 0.01)
        assertEquals(6_000.0, summary.totalUnrealizedPnL, 0.01)
        assertEquals(12_000.0, summary.totalLiabilities, 0.01)
        assertEquals(40_000.0, summary.netWorth, 0.01)
    }

    @Test
    fun `view model persists profiles, updates market price and liabilities, and syncs net worth to FIRE`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val vm = FireViewModel()

        vm.updateParameters(vm.parameters.value.copy(currentAge = 29, targetAge = 48))
        vm.saveProfile(context, "Early Freedom 48")
        assertTrue(vm.getSavedProfiles(context).contains("Early Freedom 48"))

        vm.updateParameters(vm.parameters.value.copy(currentAge = 35, targetAge = 60))
        assertTrue(vm.loadProfile(context, "Early Freedom 48"))
        assertEquals(29, vm.parameters.value.currentAge)
        assertEquals(48, vm.parameters.value.targetAge)

        vm.deleteProfile(context, "Early Freedom 48")
        assertTrue(!vm.getSavedProfiles(context).contains("Early Freedom 48"))

        val firstHolding = vm.holdings.value.first()
        val oldValue = vm.portfolioSummary.value.totalCurrentValue
        vm.updateHoldingMarketValueOrPrice(context, firstHolding.id, firstHolding.currentMarketPrice + 50.0)
        assertTrue(vm.portfolioSummary.value.totalCurrentValue > oldValue)

        vm.addOrUpdateLiability(context, LiabilityItem("debt1", "Mortgage", "Mortgage", 10_000.0))
        assertEquals(10_000.0, vm.portfolioSummary.value.totalLiabilities, 0.01)
        assertEquals(
            vm.portfolioSummary.value.totalCurrentValue - 10_000.0,
            vm.portfolioSummary.value.netWorth,
            0.01
        )

        // Verify persistence into a fresh ViewModel instance
        val reloadedVm = FireViewModel()
        reloadedVm.initPortfolioAndProfiles(context)
        assertEquals(10_000.0, reloadedVm.portfolioSummary.value.totalLiabilities, 0.01)

        reloadedVm.syncPortfolioToFire()
        assertEquals(AppTab.FIRE, reloadedVm.selectedTab.value)
        assertEquals(reloadedVm.portfolioSummary.value.netWorth, reloadedVm.parameters.value.currentSavings, 0.01)
    }

    @Test
    fun `inflation impact calculator computes future cost, purchasing power erosion, and Fisher real yield`() {
        val result = InvestmentEngine.calculateInflationImpact(
            InflationImpactParams(
                goalAmountToday = 100_000.0,
                annualInflationRate = 0.06,
                years = 10,
                expectedSavingsYield = 0.04
            )
        )
        assertEquals(11, result.inflatedCostTimeline.size)
        assertEquals(11, result.purchasingPowerTimeline.size)
        assertEquals(179_084.77, result.futureInflatedCost, 1.0)
        assertTrue(result.uninvestedPurchasingPower < 60_000.0)
        assertTrue(result.purchasingPowerLostPercent in 40.0..50.0)
        // Savings yield (4%) < inflation (6%) => negative real return & real invested value < 100k
        assertTrue(result.realAnnualReturn < 0.0)
        assertTrue(result.investedRealValue < 100_000.0)
    }

    @Test
    fun `csv export properly escapes fields and writes portfolio, projections, and full backup CSV files`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val vm = FireViewModel()

        // Include comma & quotes in asset name to verify RFC-4180 CSV quoting
        vm.addOrUpdateHolding(
            context,
            AssetHolding(
                id = "custom_eq",
                name = "Vanguard \"Total\", ETF",
                assetType = AssetType.EQUITY,
                currentValue = 12_000.0,
                monthlyContribution = 300.0,
                expectedReturn = 0.11,
                taxRate = 0.125,
                symbol = "VTI",
                quantity = 40.0,
                avgBuyPrice = 250.0,
                currentMarketPrice = 300.0,
                investedCost = 10_000.0
            )
        )
        vm.addOrUpdateLiability(context, LiabilityItem("debt_csv", "Home Loan", "Mortgage", 15_000.0))

        val portfolioCsv = vm.generateCsvContent(CsvExportScope.PORTFOLIO)
        assertTrue(portfolioCsv.contains("[PORTFOLIO HOLDINGS]"))
        assertTrue(portfolioCsv.contains("\"Vanguard \"\"Total\"\", ETF\""))
        assertTrue(portfolioCsv.contains("[LIABILITIES & LOANS]"))
        assertTrue(portfolioCsv.contains("Home Loan"))
        assertTrue(portfolioCsv.contains("[PORTFOLIO NET WORTH PROJECTION]"))

        val projectionsCsv = vm.generateCsvContent(CsvExportScope.PROJECTIONS)
        assertTrue(projectionsCsv.contains("[FIRE RETIREMENT SUMMARY]"))
        assertTrue(projectionsCsv.contains("[SIP PROJECTION]"))
        assertTrue(projectionsCsv.contains("[SWP WITHDRAWAL PROJECTION]"))
        assertTrue(projectionsCsv.contains("[FD & BONDS PROJECTION]"))
        assertTrue(projectionsCsv.contains("[SAVINGS GOAL PROJECTION]"))
        assertTrue(projectionsCsv.contains("[INFLATION IMPACT PROJECTION]"))

        val fullBackupCsv = vm.generateCsvContent(CsvExportScope.FULL_BACKUP)
        assertTrue(fullBackupCsv.contains("[PORTFOLIO HOLDINGS]"))
        assertTrue(fullBackupCsv.contains("[SIP PROJECTION]"))

        // Verify URI writing and file creation on disk
        val tempFile = File(context.cacheDir, "test_export_backup.csv")
        assertTrue(CsvExportService.writeCsvToUri(context, Uri.fromFile(tempFile), fullBackupCsv))
        assertTrue(tempFile.exists() && tempFile.readText().contains("Home Loan"))

        val sharedTemp = CsvExportService.writeTempCsvFile(context, "shared_portfolio.csv", portfolioCsv)
        assertNotNull(sharedTemp)
        assertTrue(sharedTemp!!.exists() && sharedTemp.readText().contains("VTI"))
    }

    @Test
    fun `debt payoff engine compares Avalanche vs Snowball payoff ordering and saves interest vs minimum-only`() {
        val accounts = listOf(
            DebtAccount("d1", "Small Low-APR Loan", "Personal Loan", 2_000.0, 0.06, 80.0),
            DebtAccount("d2", "High-APR Credit Card", "Credit Card", 8_000.0, 0.24, 220.0),
            DebtAccount("d3", "Mid-APR Auto Loan", "Auto Loan", 5_000.0, 0.09, 140.0)
        )

        val avResult = DebtPayoffEngine.calculateDebtPayoff(accounts, extraMonthlyPayment = 300.0, DebtPayoffStrategy.AVALANCHE)
        val sbResult = DebtPayoffEngine.calculateDebtPayoff(accounts, extraMonthlyPayment = 300.0, DebtPayoffStrategy.SNOWBALL)

        // Avalanche prioritizes highest APR (#1 = 24% Credit Card) and saves at least as much interest as Snowball
        assertEquals("d2", avResult.activePlan.accountSummaries.first().id)
        assertTrue(avResult.avalanchePlan.totalInterestPaid < sbResult.snowballPlan.totalInterestPaid)
        assertTrue(avResult.avalancheInterestAdvantage > 0.0)

        // Snowball prioritizes smallest balance (#1 = $2,000 Small Loan)
        assertEquals("d1", sbResult.activePlan.accountSummaries.first().id)

        // Extra payments + rollover significantly beat minimum-only schedule
        assertTrue(avResult.interestSavedVsMinimum > 0.0)
        assertTrue(avResult.monthsSavedVsMinimum > 0)
        assertEquals(0.0, avResult.activePlan.monthlyBalanceTimeline.last(), 0.01)
    }

    @Test
    fun `view model persists debt accounts, imports liabilities, and includes debt schedule in projections CSV`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val vm = FireViewModel()

        val startBalance = vm.debtPayoffResult.value.totalDebtBalance
        vm.addOrUpdateDebtAccount(
            context,
            DebtAccount("cc_custom", "Travel Card", "Credit Card", 3_500.0, 0.229, 105.0)
        )
        assertEquals(startBalance + 3_500.0, vm.debtPayoffResult.value.totalDebtBalance, 0.01)

        vm.selectDebtStrategy(DebtPayoffStrategy.SNOWBALL)
        assertEquals(DebtPayoffStrategy.SNOWBALL, vm.debtPayoffResult.value.activePlan.strategy)

        vm.addOrUpdateLiability(context, LiabilityItem("liab_import_1", "Medical Loan", "Personal Loan", 4_000.0))
        val importedCount = vm.importLiabilitiesToDebtTracker(context)
        assertTrue(importedCount >= 1)

        // Verify persistence into fresh ViewModel
        val reloaded = FireViewModel()
        reloaded.initPortfolioAndProfiles(context)
        assertTrue(reloaded.debtAccounts.value.any { it.name == "Travel Card" })

        val csv = reloaded.generateCsvContent(CsvExportScope.PROJECTIONS)
        assertTrue(csv.contains("[DEBT PAYOFF SCHEDULE]"))
        assertTrue(csv.contains("Travel Card"))
    }

    @Test
    fun `tax engine computes US Federal progressive brackets, LTCG, and investment deduction savings`() {
        val result = TaxEngine.calculateTax(
            TaxParams(
                regime = TaxRegime.US_SINGLE,
                annualIncome = 120_000.0,
                retirementDeduction = 20_000.0,
                additionalInvestmentDeduction = 4_000.0,
                applyStandardDeduction = true,
                realizedCapitalGains = 10_000.0,
                longTermGains = true
            )
        )
        // Taxable ordinary income = 120,000 - 14,600 - 24,000 = 81,400 -> 22% marginal bracket
        assertEquals(14_600.0, result.standardDeductionUsed, 0.01)
        assertEquals(24_000.0, result.investmentDeductionsUsed, 0.01)
        assertEquals(81_400.0, result.taxableIncome, 0.01)
        assertEquals(0.22, result.marginalBracketRate, 0.001)
        assertTrue(result.taxSavedFromDeductions > 5_000.0)
        // Combined 81,400 + 10,000 is between $47,025 and $518,900 -> 15% LTCG = $1,500
        assertEquals(1_500.0, result.capitalGainsTax, 0.01)
        assertEquals(130_000.0 - result.totalTaxLiability, result.netTakeHomeIncome, 0.01)
    }

    @Test
    fun `tax engine supports India Old 80C deductions, New Regime 87A rebate, and FIRE sync`() {
        // Under India New Regime, ₹7,75,000 gross - ₹75,000 std ded = ₹7,00,000 taxable -> Sec 87A rebate makes ordinary tax ₹0
        val newRegimeZero = TaxEngine.calculateTax(
            TaxParams(
                regime = TaxRegime.IN_NEW,
                annualIncome = 775_000.0,
                retirementDeduction = 150_000.0,
                additionalInvestmentDeduction = 0.0,
                applyStandardDeduction = true,
                realizedCapitalGains = 0.0
            )
        )
        assertEquals(0.0, newRegimeZero.totalTaxLiability, 0.01)

        // Under India Old Regime at ₹15L, 80C + NPS deductions save substantial tax & add 4% cess
        val oldRegime = TaxEngine.calculateTax(
            TaxParams(
                regime = TaxRegime.IN_OLD,
                annualIncome = 1_500_000.0,
                retirementDeduction = 150_000.0,
                additionalInvestmentDeduction = 50_000.0,
                applyStandardDeduction = true,
                realizedCapitalGains = 225_000.0,
                longTermGains = true
            )
        )
        assertTrue(oldRegime.taxSavedFromDeductions > 50_000.0)
        // LTCG exemption of ₹1.25L on ₹2.25L leaves ₹1L @ 12.5% = ₹12,500
        assertEquals(12_500.0, oldRegime.capitalGainsTax, 0.01)
        assertTrue(oldRegime.cessOrSurtax > 0.0)

        val vm = FireViewModel()
        vm.selectTaxRegime(TaxRegime.US_JOINT)
        vm.syncEffectiveTaxRateToFire()
        assertEquals(vm.taxResult.value.effectiveTaxRate, vm.parameters.value.taxRate, 0.001)

        val csv = vm.generateCsvContent(CsvExportScope.PROJECTIONS)
        assertTrue(csv.contains("[TAX LIABILITY ESTIMATION]"))
        assertTrue(csv.contains("US Joint"))
    }

    @Test
    fun `retirement engine calculates inflation-adjusted corpus, drawdown to zero at life expectancy, and monthly SIP`() {
        val baseParams = RetirementPlanParams(
            currentAge = 30,
            retirementAge = 60,
            lifeExpectancy = 85,
            targetMonthlyExpensesToday = 4_000.0,
            annualInflationRate = 0.05,
            preRetirementReturn = 0.10,
            postRetirementReturn = 0.06,
            currentRetirementSavings = 50_000.0
        )
        val res = RetirementEngine.calculateRetirementPlan(baseParams)

        assertEquals(30, res.yearsToRetirement)
        assertEquals(25, res.yearsInRetirement)
        assertEquals(56, res.lifecycleTrajectory.size) // ages 30..85 inclusive
        // 4000 * 1.05^30 = 17,287.77
        assertEquals(4_000.0 * Math.pow(1.05, 30.0), res.monthlyExpenseAtRetirement, 0.5)
        // Peak at retirement age equals requiredCorpusAtRetirement, draws down to ~0 at life expectancy
        val atRetirement = res.lifecycleTrajectory.first { it.age == 60 }
        assertEquals(res.requiredCorpusAtRetirement, atRetirement.projectedCorpus, 5.0)
        assertEquals(0.0, res.lifecycleTrajectory.last().projectedCorpus, 1.0)
        assertTrue(res.requiredMonthlySip > 0.0)

        // Higher life expectancy and higher inflation strictly increase required corpus
        val longerLife = RetirementEngine.calculateRetirementPlan(baseParams.copy(lifeExpectancy = 95))
        val higherInflation = RetirementEngine.calculateRetirementPlan(baseParams.copy(annualInflationRate = 0.07))
        assertTrue(longerLife.requiredCorpusAtRetirement > res.requiredCorpusAtRetirement)
        assertTrue(higherInflation.requiredCorpusAtRetirement > res.requiredCorpusAtRetirement)

        // ViewModel sync and CSV export integration
        val vm = FireViewModel()
        vm.updateRetirementPlanParams(baseParams)
        vm.syncRetirementPlanToFire()
        assertEquals(30, vm.parameters.value.currentAge)
        assertEquals(60, vm.parameters.value.targetAge)
        assertEquals(4_000.0, vm.parameters.value.monthlyExpense, 0.01)
        assertEquals(0.05, vm.parameters.value.inflationRate, 0.0001)

        val csv = vm.generateCsvContent(CsvExportScope.PROJECTIONS)
        assertTrue(csv.contains("[RETIREMENT PLANNING PROJECTION]"))
        assertTrue(csv.contains("Required Retirement Corpus"))
    }

    @Test
    fun `recurring transaction engine posts due income, portfolio SIP, and debt payments idempotently`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val vm = FireViewModel()

        val targetHolding = vm.holdings.value.first()
        val targetDebt = vm.debtAccounts.value.first()
        val initialAssetVal = targetHolding.effectiveMarketValue
        val initialDebtBal = targetDebt.balance

        // Add one due PORTFOLIO_SIP rule and one due DEBT_PAYMENT rule on Day 1
        vm.addOrUpdateRecurringTransaction(
            context,
            RecurringTransaction(
                id = "rule_auto_sip",
                title = "Monthly Index Booster",
                amount = 600.0,
                type = RecurringType.EXPENSE,
                category = "Investment SIP",
                dayOfMonth = 1,
                frequency = RecurringFrequency.MONTHLY,
                targetLink = RecurringTargetLink.PORTFOLIO_SIP,
                linkedEntityId = targetHolding.id,
                isActive = true,
                autoExecuteOnDue = true,
                lastProcessedYearMonth = null
            )
        )
        vm.addOrUpdateRecurringTransaction(
            context,
            RecurringTransaction(
                id = "rule_auto_debt",
                title = "Card Avalanche Booster",
                amount = 400.0,
                type = RecurringType.EXPENSE,
                category = "Debt Payment",
                dayOfMonth = 1,
                frequency = RecurringFrequency.MONTHLY,
                targetLink = RecurringTargetLink.DEBT_PAYMENT,
                linkedEntityId = targetDebt.id,
                isActive = true,
                autoExecuteOnDue = true,
                lastProcessedYearMonth = null
            )
        )

        val executedFirstPass = vm.processDueRecurringTransactions(
            context = context,
            requireAutoExecuteFlag = true,
            yearMonth = "2026-11",
            dayOfMonth = 14
        )
        assertEquals(2, executedFirstPass)

        val updatedHolding = vm.holdings.value.first { it.id == targetHolding.id }
        val updatedDebt = vm.debtAccounts.value.first { it.id == targetDebt.id }
        assertEquals(initialAssetVal + 600.0, updatedHolding.effectiveMarketValue, 0.05)
        assertEquals(initialDebtBal - 400.0, updatedDebt.balance, 0.05)

        // Running again in the same month must be idempotent (0 double-postings)
        val executedRepeatPass = vm.processDueRecurringTransactions(
            context = context,
            requireAutoExecuteFlag = true,
            yearMonth = "2026-11",
            dayOfMonth = 20
        )
        assertEquals(0, executedRepeatPass)

        // Syncing recurring cashflow updates Debt Payoff extra payment & CSV export
        vm.applyCashflowDebtRepayment()
        assertTrue(vm.extraDebtPayment.value >= 400.0)
        val csv = vm.generateCsvContent(CsvExportScope.PROJECTIONS)
        assertTrue(csv.contains("[RECURRING CASHFLOW SCHEDULER]"))
        assertTrue(csv.contains("Monthly Index Booster"))
    }

    @Test
    fun `budget engine integrates recurring income and categorized expenses, computes saving targets, and exports CSV`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Pure engine test with monthly & quarterly recurring expenses
        val categories = listOf(
            BudgetCategoryEntry("b_rent", "Housing", monthlyLimit = 2_000.0, manualSpending = 0.0, savingTarget = 200.0, isEssential = true),
            BudgetCategoryEntry("b_util", "Utilities", monthlyLimit = 300.0, manualSpending = 100.0, savingTarget = 80.0, isEssential = true),
            BudgetCategoryEntry("b_food", "Food & Dining", monthlyLimit = 500.0, manualSpending = 550.0, savingTarget = 100.0, isEssential = true)
        )
        val recurring = listOf(
            RecurringTransaction("r_inc", "Salary", 5_000.0, RecurringType.INCOME, "Salary"),
            RecurringTransaction("r_rent", "Apartment Rent", 1_750.0, RecurringType.EXPENSE, "Housing"),
            RecurringTransaction("r_water", "Quarterly Water Bill", 300.0, RecurringType.EXPENSE, "Utilities", frequency = RecurringFrequency.QUARTERLY)
        )

        val summary = BudgetEngine.calculateBudgetSummary(
            categories = categories,
            recurringTransactions = recurring,
            useRecurringIncome = true
        )
        assertEquals(5_000.0, summary.monthlyIncome, 0.01)
        // Recurring expenses = 1,750 + (300 / 3 = 100) = 1,850
        assertEquals(1_850.0, summary.totalRecurringExpenses, 0.01)
        assertEquals(650.0, summary.totalManualSpending, 0.01)
        assertEquals(2_500.0, summary.totalEffectiveSpending, 0.01)
        assertEquals(2_500.0, summary.unallocatedIncome, 0.01)

        val housing = summary.categories.first { it.entry.id == "b_rent" }
        assertEquals(1_750.0, housing.recurringCommitted, 0.01)
        assertEquals(250.0, housing.projectedCategorySavings, 0.01)
        assertEquals(1.0, housing.savingTargetProgress, 0.001)
        assertEquals(false, housing.isOverBudget)

        val utilities = summary.categories.first { it.entry.id == "b_util" }
        assertEquals(100.0, utilities.recurringCommitted, 0.01)
        assertEquals(200.0, utilities.effectiveSpent, 0.01)
        assertEquals(100.0, utilities.projectedCategorySavings, 0.01)

        val food = summary.categories.first { it.entry.id == "b_food" }
        assertEquals(true, food.isOverBudget)
        assertEquals(0.0, food.projectedCategorySavings, 0.01)
        assertEquals(1, summary.overBudgetCount)

        // ViewModel persistence + recurring integration + CSV export
        val vm = FireViewModel()
        val initialCategories = vm.budgetCategories.value.size
        vm.addOrUpdateBudgetCategory(
            context,
            BudgetCategoryEntry(
                id = "b_travel",
                name = "Transport",
                monthlyLimit = 600.0,
                manualSpending = 150.0,
                savingTarget = 200.0,
                isEssential = true
            )
        )
        assertEquals(initialCategories + 1, vm.budgetCategories.value.size)

        vm.addOrUpdateRecurringTransaction(
            context,
            RecurringTransaction(
                id = "r_transit",
                title = "Transit Pass",
                amount = 100.0,
                type = RecurringType.EXPENSE,
                category = "Transport",
                dayOfMonth = 2,
                lastProcessedYearMonth = RecurringTransactionEngine.currentYearMonth()
            )
        )
        val transportStatus = vm.budgetSummary.value.categories.first { it.entry.name == "Transport" }
        assertEquals(100.0, transportStatus.recurringCommitted, 0.01)
        assertEquals(250.0, transportStatus.effectiveSpent, 0.01)
        assertEquals(350.0, transportStatus.projectedCategorySavings, 0.01)

        val reloaded = FireViewModel()
        reloaded.initPortfolioAndProfiles(context)
        assertTrue(reloaded.budgetCategories.value.any { it.name == "Transport" })

        val csv = reloaded.generateCsvContent(CsvExportScope.PROJECTIONS)
        assertTrue(csv.contains("[MONTHLY BUDGET PLANNER]"))
        assertTrue(csv.contains("Transport"))
    }

    @Test
    fun `fi score engine tracks portfolio and debt reduction milestones and unlocks achievements dynamically`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val vm = FireViewModel()

        val initialScore = vm.fiScoreSummary.value
        assertTrue(initialScore.totalScore in 1..1000)
        // High-APR card starts with balance > 0, so High-APR Slayer & Debt-Free start locked
        assertFalse(initialScore.achievements.first { it.id == "high_apr_slayer" }.isUnlocked)
        assertFalse(initialScore.achievements.first { it.id == "debt_free_sovereign" }.isUnlocked)

        // Eliminate the high-APR credit card (reduces debt by >10% & clears >15% APR)
        val creditCard = vm.debtAccounts.value.first { it.annualApr >= 0.15 }
        vm.removeDebtAccount(context, creditCard.id)

        val afterCardClear = vm.fiScoreSummary.value
        assertTrue(afterCardClear.debtFreedomScore > initialScore.debtFreedomScore)
        assertTrue(afterCardClear.totalScore > initialScore.totalScore)
        assertTrue(afterCardClear.achievements.first { it.id == "high_apr_slayer" }.isUnlocked)
        assertTrue(afterCardClear.achievements.first { it.id == "first_debt_dent" }.isUnlocked)
        assertTrue(vm.recentAchievementUnlock.value != null)

        // Clear all remaining debts to hit 100% Debt-Free Sovereign
        vm.debtAccounts.value.toList().forEach { acct ->
            vm.removeDebtAccount(context, acct.id)
        }
        val debtFreeSummary = vm.fiScoreSummary.value
        assertEquals(300, debtFreeSummary.debtFreedomScore)
        assertTrue(debtFreeSummary.achievements.first { it.id == "debt_free_sovereign" }.isUnlocked)

        // CSV export contains FI Score & achievement rows
        val csv = vm.generateCsvContent(CsvExportScope.PROJECTIONS)
        assertTrue(csv.contains("[FINANCIAL INDEPENDENCE SCORE & ACHIEVEMENTS]"))
        assertTrue(csv.contains("100% Debt-Free"))
    }

    @Test
    fun `daily tip provider rotates deterministically across calendar days and persists bookmarks offline`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val day1 = java.util.Calendar.getInstance().apply { set(2026, java.util.Calendar.NOVEMBER, 14) }
        val day2 = java.util.Calendar.getInstance().apply { set(2026, java.util.Calendar.NOVEMBER, 15) }

        val idx1 = DailyTipProvider.dailyIndexForDate(day1)
        val idx2 = DailyTipProvider.dailyIndexForDate(day2)
        assertEquals((idx1 + 1) % DailyTipProvider.curatedTips.size, idx2)
        assertTrue(DailyTipProvider.curatedTips.size >= 20)

        val vm = FireViewModel()
        val initialTip = vm.dailyTip.value
        vm.nextDailyTip()
        val nextTip = vm.dailyTip.value
        assertEquals((initialTip.id % DailyTipProvider.curatedTips.size) + 1, nextTip.id)

        vm.previousDailyTip()
        assertEquals(initialTip.id, vm.dailyTip.value.id)

        vm.toggleBookmarkDailyTip(context)
        assertTrue(vm.bookmarkedTipIds.value.contains(initialTip.id))

        val reloaded = FireViewModel()
        reloaded.initPortfolioAndProfiles(context)
        assertTrue(reloaded.bookmarkedTipIds.value.contains(initialTip.id))
    }
}
