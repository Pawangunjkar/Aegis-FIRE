package com.example.services

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.example.models.AssetHolding
import com.example.models.BudgetPlanSummary
import com.example.models.DebtPayoffResult
import com.example.models.FdParams
import com.example.models.FdResult
import com.example.models.FiScoreSummary
import com.example.models.FireParameters
import com.example.models.FireResult
import com.example.models.GoalParams
import com.example.models.GoalResult
import com.example.models.InflationImpactParams
import com.example.models.InflationImpactResult
import com.example.models.LiabilityItem
import com.example.models.MonteCarloResult
import com.example.models.PortfolioSummary
import com.example.models.RecurringCashflowSummary
import com.example.models.RecurringTransaction
import com.example.models.RetirementPlanParams
import com.example.models.RetirementPlanResult
import com.example.models.SipParams
import com.example.models.SipResult
import com.example.models.SwpParams
import com.example.models.SwpResult
import com.example.models.TaxParams
import com.example.models.TaxResult
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class CsvExportScope(val filePrefix: String, val title: String) {
    PORTFOLIO("Aegis_FIRE_Portfolio", "Portfolio & Net Worth CSV"),
    PROJECTIONS("Aegis_FIRE_Projections", "Savings & FIRE Projections CSV"),
    FULL_BACKUP("Aegis_FIRE_Full_Backup", "Full Portfolio & Projections CSV")
}

object CsvExportService {

    fun defaultFileName(scope: CsvExportScope, date: Date = Date()): String {
        val stamp = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)
        return "${scope.filePrefix}_$stamp.csv"
    }

    fun escapeCsv(value: String): String {
        val needsQuoting = value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')
        return if (needsQuoting) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    private fun row(vararg cells: Any?): String {
        return cells.joinToString(",") { cell ->
            val raw = when (cell) {
                null -> ""
                is Double -> String.format(Locale.US, "%.2f", cell)
                is Float -> String.format(Locale.US, "%.2f", cell.toDouble())
                else -> cell.toString()
            }
            escapeCsv(raw)
        }
    }

    fun buildPortfolioCsv(
        holdings: List<AssetHolding>,
        liabilities: List<LiabilityItem>,
        summary: PortfolioSummary,
        projectionYears: Int,
        isIndianCurrency: Boolean
    ): String {
        val currency = if (isIndianCurrency) "INR" else "USD"
        val sb = StringBuilder()

        sb.appendLine(row("AEGIS FIRE PORTFOLIO & NET WORTH EXPORT"))
        sb.appendLine(row("Currency", currency))
        sb.appendLine(row("Total Market Assets", summary.totalCurrentValue))
        sb.appendLine(row("Total Invested Cost", summary.totalInvestedCost))
        sb.appendLine(row("Total Unrealized P&L", summary.totalUnrealizedPnL))
        sb.appendLine(row("Total Unrealized P&L (%)", summary.totalUnrealizedPnLPercent))
        sb.appendLine(row("Total Liabilities", summary.totalLiabilities))
        sb.appendLine(row("Net Worth", summary.netWorth))
        sb.appendLine(row("Total Monthly SIP", summary.totalMonthlyContribution))
        sb.appendLine(row("Blended Expected Return (%)", summary.weightedExpectedReturn * 100.0))
        sb.appendLine(row("Blended Tax Rate (%)", summary.weightedTaxRate * 100.0))
        sb.appendLine(row("Projection Horizon (Years)", projectionYears))
        sb.appendLine(row("Projected Gross Value", summary.projectedGross10Yr))
        sb.appendLine(row("Projected Net Post-Tax Value", summary.projectedNet10Yr))
        sb.appendLine(row("Projected Tax Drag", summary.projectedTaxDrag10Yr))
        sb.appendLine()

        sb.appendLine(row("[PORTFOLIO HOLDINGS]"))
        sb.appendLine(
            row(
                "ID",
                "Name",
                "Ticker",
                "Asset Type",
                "Units",
                "Avg Buy Price",
                "Current Market Price",
                "Cost Basis",
                "Market Value",
                "Unrealized P&L",
                "Unrealized P&L (%)",
                "Monthly SIP",
                "Expected Return (%)",
                "Tax Rate (%)"
            )
        )
        holdings.forEach { h ->
            sb.appendLine(
                row(
                    h.id,
                    h.name,
                    h.symbol,
                    h.assetType.label,
                    h.quantity,
                    h.avgBuyPrice,
                    h.currentMarketPrice,
                    h.effectiveCostBasis,
                    h.effectiveMarketValue,
                    h.unrealizedPnL,
                    h.unrealizedPnLPercent,
                    h.monthlyContribution,
                    h.expectedReturn * 100.0,
                    h.taxRate * 100.0
                )
            )
        }
        sb.appendLine()

        sb.appendLine(row("[LIABILITIES & LOANS]"))
        sb.appendLine(row("ID", "Name", "Category", "Outstanding Balance", "Interest Rate (%)"))
        liabilities.forEach { l ->
            sb.appendLine(row(l.id, l.name, l.category, l.balance, l.interestRate * 100.0))
        }
        sb.appendLine()

        sb.appendLine(row("[PORTFOLIO NET WORTH PROJECTION]"))
        sb.appendLine(row("Year", "Projected Net Post-Tax Value"))
        summary.yearlyNetTrajectory.forEachIndexed { year, value ->
            sb.appendLine(row(year, value))
        }

        return sb.toString()
    }

    fun buildSavingsProjectionsCsv(
        fireParams: FireParameters,
        fireResult: FireResult?,
        monteCarloResult: MonteCarloResult?,
        sipParams: SipParams,
        sipResult: SipResult,
        swpParams: SwpParams,
        swpResult: SwpResult,
        fdParams: FdParams,
        fdResult: FdResult,
        goalParams: GoalParams,
        goalResult: GoalResult,
        inflationParams: InflationImpactParams,
        inflationResult: InflationImpactResult,
        isIndianCurrency: Boolean,
        debtPayoffResult: DebtPayoffResult? = null,
        taxParams: TaxParams? = null,
        taxResult: TaxResult? = null,
        retirementParams: RetirementPlanParams? = null,
        retirementResult: RetirementPlanResult? = null,
        recurringSummary: RecurringCashflowSummary? = null,
        recurringTransactions: List<RecurringTransaction> = emptyList(),
        budgetSummary: BudgetPlanSummary? = null,
        fiScoreSummary: FiScoreSummary? = null
    ): String {
        val currency = if (isIndianCurrency) "INR" else "USD"
        val sb = StringBuilder()

        sb.appendLine(row("AEGIS FIRE SAVINGS & PROJECTIONS EXPORT"))
        sb.appendLine(row("Currency", currency))
        sb.appendLine()

        sb.appendLine(row("[FIRE RETIREMENT SUMMARY]"))
        sb.appendLine(row("Current Age", fireParams.currentAge))
        sb.appendLine(row("Target Age", fireParams.targetAge))
        sb.appendLine(row("Current Savings", fireParams.currentSavings))
        sb.appendLine(row("Monthly SIP", fireParams.monthlySip))
        sb.appendLine(row("Annual Step-Up (%)", fireParams.stepUpRate * 100.0))
        sb.appendLine(row("Expected Return (%)", fireParams.expectedReturn * 100.0))
        sb.appendLine(row("Inflation Rate (%)", fireParams.inflationRate * 100.0))
        sb.appendLine(row("Tax Rate (%)", fireParams.taxRate * 100.0))
        sb.appendLine(row("Monthly Expense", fireParams.monthlyExpense))
        sb.appendLine(row("Safe Withdrawal Rate (%)", fireParams.safeWithdrawalRate * 100.0))
        if (fireResult != null) {
            sb.appendLine(row("Target FIRE Number", fireResult.targetFireNumber))
            sb.appendLine(row("Coast FIRE Number", fireResult.coastFireNumber))
            sb.appendLine(row("Gross Accumulated", fireResult.grossAccumulated))
            sb.appendLine(row("Estimated Tax Drag", fireResult.taxDrag))
            sb.appendLine(row("Net Post-Tax Corpus", fireResult.netAccumulated))
            sb.appendLine(row("FIRE Crossover Age", fireResult.fireCrossoverAge ?: "Not Reached"))
            sb.appendLine()
            sb.appendLine(row("[FIRE ACCUMULATION TRAJECTORY]"))
            sb.appendLine(row("Age", "Gross Balance", "Net Post-Tax Balance", "Target FIRE Balance"))
            fireResult.trajectory.forEach { pt ->
                sb.appendLine(row(pt.age, pt.grossBalance, pt.netBalance, pt.targetBalance ?: fireResult.targetFireNumber))
            }
        }
        sb.appendLine()

        if (monteCarloResult != null) {
            sb.appendLine(row("[MONTE CARLO STRESS TEST]"))
            sb.appendLine(row("30-Yr Survival (%)", monteCarloResult.longevityProbability))
            sb.appendLine(row("40-Yr Survival (%)", monteCarloResult.longevity40Probability))
            sb.appendLine(row("Year", "P10 Bear", "P50 Median", "P90 Bull"))
            val count = minOf(
                monteCarloResult.percentile10.size,
                monteCarloResult.percentile50.size,
                monteCarloResult.percentile90.size
            )
            for (i in 0 until count) {
                sb.appendLine(
                    row(
                        i,
                        monteCarloResult.percentile10[i],
                        monteCarloResult.percentile50[i],
                        monteCarloResult.percentile90[i]
                    )
                )
            }
            sb.appendLine()
        }

        sb.appendLine(row("[SIP PROJECTION]"))
        sb.appendLine(
            row(
                "Initial Lumpsum",
                "Monthly SIP",
                "Annual Step-Up (%)",
                "Expected Return (%)",
                "Duration (Years)",
                "Inflation (%)",
                "Total Invested",
                "Wealth Gain",
                "Maturity Value",
                "Inflation-Adjusted Value"
            )
        )
        sb.appendLine(
            row(
                sipParams.lumpsum,
                sipParams.monthlySip,
                sipParams.annualStepUpRate * 100.0,
                sipParams.expectedReturn * 100.0,
                sipParams.years,
                sipParams.inflationRate * 100.0,
                sipResult.totalInvested,
                sipResult.wealthGain,
                sipResult.maturityValue,
                sipResult.inflationAdjustedValue
            )
        )
        sb.appendLine(row("Year", "Total Invested", "Maturity Value"))
        sipResult.timeline.forEach { pt ->
            sb.appendLine(row(pt.year, pt.totalInvested, pt.maturityValue))
        }
        sb.appendLine()

        sb.appendLine(row("[SWP WITHDRAWAL PROJECTION]"))
        sb.appendLine(
            row(
                "Initial Corpus",
                "Monthly Withdrawal",
                "Annual Withdrawal Step-Up (%)",
                "Expected Return (%)",
                "Years",
                "Total Withdrawn",
                "Final Balance",
                "Depleted Year"
            )
        )
        sb.appendLine(
            row(
                swpParams.initialCorpus,
                swpParams.monthlyWithdrawal,
                swpParams.annualWithdrawalStepUp * 100.0,
                swpParams.expectedReturn * 100.0,
                swpParams.years,
                swpResult.totalWithdrawn,
                swpResult.finalBalance,
                swpResult.depletedYear ?: "Sustainable"
            )
        )
        sb.appendLine(row("Year", "Remaining Balance"))
        swpResult.yearlyBalances.forEachIndexed { yr, bal ->
            sb.appendLine(row(yr, bal))
        }
        sb.appendLine()

        sb.appendLine(row("[FD & BONDS PROJECTION]"))
        sb.appendLine(
            row(
                "Principal",
                "Annual Rate (%)",
                "Tenure (Years)",
                "Compounding",
                "Tax Rate (%)",
                "Gross Maturity",
                "Gross Interest",
                "Tax Deducted",
                "Post-Tax Maturity",
                "Effective Post-Tax Yield (%)"
            )
        )
        sb.appendLine(
            row(
                fdParams.principal,
                fdParams.annualRate * 100.0,
                fdParams.years,
                fdParams.compounding.label,
                fdParams.taxRate * 100.0,
                fdResult.grossMaturity,
                fdResult.grossInterest,
                fdResult.taxDeducted,
                fdResult.postTaxMaturity,
                fdResult.effectivePostTaxYield * 100.0
            )
        )
        sb.appendLine()

        sb.appendLine(row("[SAVINGS GOAL PROJECTION]"))
        sb.appendLine(
            row(
                "Target Goal Today",
                "Current Earmarked Savings",
                "Horizon (Years)",
                "Expected Return (%)",
                "Inflation (%)",
                "Inflation Adjusted",
                "Future Target",
                "Existing Savings FV",
                "Shortfall",
                "Required Flat Monthly SIP",
                "Required 10% Step-Up Monthly SIP"
            )
        )
        sb.appendLine(
            row(
                goalParams.targetGoal,
                goalParams.currentSavings,
                goalParams.years,
                goalParams.expectedReturn * 100.0,
                goalParams.inflationRate * 100.0,
                goalParams.adjustForInflation,
                goalResult.inflationAdjustedGoal,
                goalResult.futureValueOfCurrentSavings,
                goalResult.shortfallToCover,
                goalResult.requiredMonthlySipFlat,
                goalResult.requiredMonthlySipStepUp10
            )
        )
        sb.appendLine()

        sb.appendLine(row("[INFLATION IMPACT PROJECTION]"))
        sb.appendLine(
            row(
                "Goal Cost Today",
                "Annual Inflation (%)",
                "Horizon (Years)",
                "Savings Yield (%)",
                "Future Inflated Cost",
                "Uninvested Purchasing Power",
                "Real Annual Return (%)",
                "Invested Real Value"
            )
        )
        sb.appendLine(
            row(
                inflationParams.goalAmountToday,
                inflationParams.annualInflationRate * 100.0,
                inflationParams.years,
                inflationParams.expectedSavingsYield * 100.0,
                inflationResult.futureInflatedCost,
                inflationResult.uninvestedPurchasingPower,
                inflationResult.realAnnualReturn * 100.0,
                inflationResult.investedRealValue
            )
        )
        sb.appendLine(row("Year", "Future Inflated Goal Cost", "Uninvested Purchasing Power"))
        val infCount = minOf(inflationResult.inflatedCostTimeline.size, inflationResult.purchasingPowerTimeline.size)
        for (y in 0 until infCount) {
            sb.appendLine(row(y, inflationResult.inflatedCostTimeline[y], inflationResult.purchasingPowerTimeline[y]))
        }

        if (debtPayoffResult != null && debtPayoffResult.activePlan.accountSummaries.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine(row("[DEBT PAYOFF SCHEDULE]"))
            sb.appendLine(row("Active Strategy", debtPayoffResult.activePlan.strategy.label))
            sb.appendLine(row("Total Debt Balance", debtPayoffResult.totalDebtBalance))
            sb.appendLine(row("Monthly Minimums", debtPayoffResult.totalMinimumPayment))
            sb.appendLine(row("Extra Monthly Payment", debtPayoffResult.extraMonthlyPayment))
            sb.appendLine(row("Months To Debt Free", debtPayoffResult.activePlan.monthsToPayoff))
            sb.appendLine(row("Total Interest Paid", debtPayoffResult.activePlan.totalInterestPaid))
            sb.appendLine(row("Interest Saved vs Minimum", debtPayoffResult.interestSavedVsMinimum))
            sb.appendLine(row("Payoff Order", "Name", "Category", "Starting Balance", "APR (%)", "Min Payment", "Payoff Month", "Total Interest"))
            debtPayoffResult.activePlan.accountSummaries.forEach { acc ->
                sb.appendLine(
                    row(
                        acc.payoffOrder,
                        acc.name,
                        acc.category,
                        acc.startingBalance,
                        acc.annualApr * 100.0,
                        acc.minimumPayment,
                        acc.payoffMonth,
                        acc.totalInterestPaid
                    )
                )
            }
        }

        if (taxParams != null && taxResult != null) {
            sb.appendLine()
            sb.appendLine(row("[TAX LIABILITY ESTIMATION]"))
            sb.appendLine(row("Tax Regime", taxParams.regime.label))
            sb.appendLine(row("Gross Annual Income", taxResult.grossIncome))
            sb.appendLine(row("Realized Capital Gains", taxParams.realizedCapitalGains))
            sb.appendLine(row("Standard Deduction", taxResult.standardDeductionUsed))
            sb.appendLine(row("Investment Deductions", taxResult.investmentDeductionsUsed))
            sb.appendLine(row("Taxable Ordinary Income", taxResult.taxableIncome))
            sb.appendLine(row("Ordinary Income Tax", taxResult.ordinaryIncomeTax))
            sb.appendLine(row("Capital Gains Tax", taxResult.capitalGainsTax))
            sb.appendLine(row("Cess / Surtax", taxResult.cessOrSurtax))
            sb.appendLine(row("Total Annual Tax Liability", taxResult.totalTaxLiability))
            sb.appendLine(row("Net After-Tax Income", taxResult.netTakeHomeIncome))
            sb.appendLine(row("Effective Tax Rate (%)", taxResult.effectiveTaxRate * 100.0))
            sb.appendLine(row("Marginal Tax Bracket (%)", taxResult.marginalBracketRate * 100.0))
            sb.appendLine(row("Tax Saved From Deductions", taxResult.taxSavedFromDeductions))
        }

        if (retirementParams != null && retirementResult != null) {
            sb.appendLine()
            sb.appendLine(row("[RETIREMENT PLANNING PROJECTION]"))
            sb.appendLine(row("Current Age", retirementResult.currentAge))
            sb.appendLine(row("Retirement Age", retirementResult.retirementAge))
            sb.appendLine(row("Life Expectancy", retirementResult.lifeExpectancy))
            sb.appendLine(row("Target Monthly Expense (Today)", retirementParams.targetMonthlyExpensesToday))
            sb.appendLine(row("Annual Inflation Rate (%)", retirementParams.annualInflationRate * 100.0))
            sb.appendLine(row("Pre-Retirement Return (%)", retirementParams.preRetirementReturn * 100.0))
            sb.appendLine(row("Post-Retirement Return (%)", retirementParams.postRetirementReturn * 100.0))
            sb.appendLine(row("Current Retirement Savings", retirementParams.currentRetirementSavings))
            sb.appendLine(row("Monthly Expense At Retirement", retirementResult.monthlyExpenseAtRetirement))
            sb.appendLine(row("Monthly Expense At Life Expectancy", retirementResult.finalMonthlyExpenseAtLifeExpectancy))
            sb.appendLine(row("Required Retirement Corpus", retirementResult.requiredCorpusAtRetirement))
            sb.appendLine(row("Projected Savings At Retirement", retirementResult.projectedCurrentSavingsAtRetirement))
            sb.appendLine(row("Corpus Shortfall", retirementResult.corpusShortfall))
            sb.appendLine(row("Required Monthly SIP", retirementResult.requiredMonthlySip))
            sb.appendLine(row("Total Nominal Retirement Payout", retirementResult.totalRetirementPayoutNominal))
            sb.appendLine(row("Age", "Projected Corpus", "Required Corpus Benchmark", "Annual Expense (Nominal)"))
            retirementResult.lifecycleTrajectory.forEach { pt ->
                sb.appendLine(row(pt.age, pt.projectedCorpus, pt.targetCorpusBenchmark, pt.annualExpenseNominal))
            }
        }

        if (recurringSummary != null || recurringTransactions.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine(row("[RECURRING CASHFLOW SCHEDULER]"))
            if (recurringSummary != null) {
                sb.appendLine(row("Total Monthly Income", recurringSummary.totalMonthlyIncome))
                sb.appendLine(row("Total Monthly Expenses", recurringSummary.totalMonthlyExpense))
                sb.appendLine(row("Net Monthly Cashflow", recurringSummary.netMonthlyCashflow))
                sb.appendLine(row("Monthly Portfolio Auto-SIP", recurringSummary.monthlyPortfolioAutoContribution))
                sb.appendLine(row("Monthly Debt Auto-Repayment", recurringSummary.monthlyDebtAutoRepayment))
            }
            sb.appendLine(row("ID", "Title", "Type", "Category", "Amount", "Frequency", "Day of Month", "Link Target", "Active", "Last Processed"))
            recurringTransactions.forEach { tx ->
                sb.appendLine(
                    row(
                        tx.id,
                        tx.title,
                        tx.type.label,
                        tx.category,
                        tx.amount,
                        tx.frequency.label,
                        tx.dayOfMonth,
                        tx.targetLink.label,
                        tx.isActive,
                        tx.lastProcessedYearMonth ?: "Never"
                    )
                )
            }
        }

        if (budgetSummary != null) {
            sb.appendLine()
            sb.appendLine(row("[MONTHLY BUDGET PLANNER]"))
            sb.appendLine(row("Monthly Income", budgetSummary.monthlyIncome))
            sb.appendLine(row("Total Budgeted Limit", budgetSummary.totalBudgetedLimit))
            sb.appendLine(row("Recurring Committed Spend", budgetSummary.totalRecurringExpenses))
            sb.appendLine(row("Variable Manual Spend", budgetSummary.totalManualSpending))
            sb.appendLine(row("Total Monthly Spend", budgetSummary.totalEffectiveSpending))
            sb.appendLine(row("Category Saving Targets", budgetSummary.totalCategorySavingTargets))
            sb.appendLine(row("Projected Category Savings", budgetSummary.totalProjectedCategorySavings))
            sb.appendLine(row("Unallocated Income", budgetSummary.unallocatedIncome))
            sb.appendLine(row("Net Savings Rate (%)", budgetSummary.savingsRatePercent))
            sb.appendLine(row("Category", "Essential", "Monthly Limit", "Recurring Committed", "Manual Spend", "Total Spent", "Saving Target", "Projected Category Saved", "Status"))
            budgetSummary.categories.forEach { cat ->
                sb.appendLine(
                    row(
                        cat.entry.name,
                        cat.entry.isEssential,
                        cat.entry.monthlyLimit,
                        cat.recurringCommitted,
                        cat.entry.manualSpending,
                        cat.effectiveSpent,
                        cat.entry.savingTarget,
                        cat.projectedCategorySavings,
                        if (cat.isOverBudget) "Over Budget" else "On Track"
                    )
                )
            }
        }

        if (fiScoreSummary != null) {
            sb.appendLine()
            sb.appendLine(row("[FINANCIAL INDEPENDENCE SCORE & ACHIEVEMENTS]"))
            sb.appendLine(row("FI Score", "${fiScoreSummary.totalScore} / ${fiScoreSummary.maxScore}"))
            sb.appendLine(row("Rank Tier", fiScoreSummary.rank.title))
            sb.appendLine(row("Portfolio Score", "${fiScoreSummary.portfolioScore} / 450"))
            sb.appendLine(row("Debt Freedom Score", "${fiScoreSummary.debtFreedomScore} / 300"))
            sb.appendLine(row("Cashflow Discipline Score", "${fiScoreSummary.cashflowScore} / 250"))
            sb.appendLine(row("Achievements Unlocked", "${fiScoreSummary.unlockedCount} / ${fiScoreSummary.totalAchievements}"))
            sb.appendLine(row("Achievement", "Category", "Points", "Status", "Progress"))
            fiScoreSummary.achievements.forEach { ach ->
                sb.appendLine(
                    row(
                        ach.title,
                        ach.category.label,
                        ach.points,
                        if (ach.isUnlocked) "Unlocked" else "Locked",
                        ach.progressLabel
                    )
                )
            }
        }

        return sb.toString()
    }

    fun writeCsvToUri(context: Context, uri: Uri, csvContent: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(csvContent.toByteArray(Charsets.UTF_8))
                out.flush()
            } ?: return false
            true
        } catch (e: Exception) {
            Log.e("CsvExportService", "Failed saving CSV to URI", e)
            false
        }
    }

    fun writeTempCsvFile(context: Context, fileName: String, csvContent: String): File? {
        return try {
            val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                ?: File(context.cacheDir, "csvs").apply { mkdirs() }
            if (!baseDir.exists()) baseDir.mkdirs()
            val file = File(baseDir, fileName)
            file.writeText(csvContent, Charsets.UTF_8)
            file
        } catch (e: Exception) {
            Log.e("CsvExportService", "Failed writing CSV temp file", e)
            null
        }
    }

    fun shareCsv(context: Context, fileName: String, csvContent: String, subject: String): Boolean {
        val file = writeTempCsvFile(context, fileName, csvContent) ?: return false
        return try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, subject)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, subject))
            true
        } catch (e: Exception) {
            Log.e("CsvExportService", "Failed sharing CSV", e)
            false
        }
    }
}
