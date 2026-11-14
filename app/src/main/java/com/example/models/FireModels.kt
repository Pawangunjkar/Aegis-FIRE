package com.example.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FireParameters(
    val currentAge: Int = 30,
    val targetAge: Int = 50,
    val currentSavings: Double = 100000.0,
    val monthlySip: Double = 2000.0,
    val stepUpRate: Double = 0.05,
    val expectedReturn: Double = 0.08,
    val inflationRate: Double = 0.03,
    val taxRate: Double = 0.15,
    val monthlyExpense: Double = 4000.0,
    val safeWithdrawalRate: Double = 0.04
)

@JsonClass(generateAdapter = true)
data class FireResult(
    val targetFireNumber: Double,
    val grossAccumulated: Double,
    val taxDrag: Double,
    val netAccumulated: Double,
    val trajectory: List<TrajectoryPoint>,
    val depletionYear: Int?,
    val coastFireNumber: Double = 0.0,
    val fireCrossoverAge: Int? = null
)

@JsonClass(generateAdapter = true)
data class TrajectoryPoint(
    val age: Int,
    val grossBalance: Double,
    val netBalance: Double,
    val targetBalance: Double? = null
)

@JsonClass(generateAdapter = true)
data class MonteCarloResult(
    val longevityProbability: Double,
    val longevity40Probability: Double = 0.0,
    val percentile10: List<Double>,
    val percentile50: List<Double>,
    val percentile90: List<Double>
)

enum class AppTab(val label: String) {
    FIRE("FIRE"),
    CALCULATORS("Calculators"),
    PORTFOLIO("Portfolio"),
    ROADMAP("Roadmap")
}

enum class CalculatorMode(val label: String) {
    SIP("SIP"),
    SWP("SWP"),
    FD_BONDS("FD & Bonds"),
    GOAL("Savings Goal"),
    INFLATION("Inflation Impact"),
    DEBT_PAYOFF("Debt Payoff"),
    TAX("Tax Estimator"),
    RETIREMENT("Retirement"),
    CASHFLOW("Cashflow"),
    BUDGET("Budget")
}

enum class CompoundingFrequency(val label: String, val periodsPerYear: Int) {
    MONTHLY("Monthly", 12),
    QUARTERLY("Quarterly", 4),
    HALF_YEARLY("Half-Yearly", 2),
    YEARLY("Yearly", 1)
}

enum class AssetType(val label: String, val defaultReturn: Double, val defaultTaxRate: Double) {
    EQUITY("Equity / Stocks", 0.12, 0.125),
    MUTUAL_FUND("Mutual Funds", 0.11, 0.125),
    FIXED_DEPOSIT("Fixed Deposit", 0.07, 0.30),
    BONDS("Bonds / Debt", 0.075, 0.20),
    GOLD_CASH("Gold / Liquid", 0.08, 0.20)
}

@JsonClass(generateAdapter = true)
data class AssetHolding(
    val id: String,
    val name: String,
    val assetType: AssetType,
    val currentValue: Double,
    val monthlyContribution: Double,
    val expectedReturn: Double,
    val taxRate: Double,
    val symbol: String = "",
    val quantity: Double = 0.0,
    val avgBuyPrice: Double = 0.0,
    val currentMarketPrice: Double = 0.0,
    val investedCost: Double = currentValue
) {
    val effectiveMarketValue: Double
        get() = if (quantity > 0.0 && currentMarketPrice > 0.0) quantity * currentMarketPrice else currentValue

    val effectiveCostBasis: Double
        get() = if (quantity > 0.0 && avgBuyPrice > 0.0) quantity * avgBuyPrice else investedCost

    val unrealizedPnL: Double
        get() = effectiveMarketValue - effectiveCostBasis

    val unrealizedPnLPercent: Double
        get() = if (effectiveCostBasis > 0.0) (unrealizedPnL / effectiveCostBasis) * 100.0 else 0.0
}

@JsonClass(generateAdapter = true)
data class LiabilityItem(
    val id: String,
    val name: String,
    val category: String = "Loan",
    val balance: Double,
    val interestRate: Double = 0.07
)

data class AssetAllocationSlice(
    val assetType: AssetType,
    val currentValue: Double,
    val percentage: Double
)

data class PortfolioSummary(
    val totalCurrentValue: Double,
    val totalMonthlyContribution: Double,
    val weightedExpectedReturn: Double,
    val weightedTaxRate: Double,
    val projectedGross10Yr: Double,
    val projectedNet10Yr: Double,
    val projectedTaxDrag10Yr: Double,
    val allocationSlices: List<AssetAllocationSlice>,
    val yearlyNetTrajectory: List<Double>,
    val totalInvestedCost: Double = totalCurrentValue,
    val totalUnrealizedPnL: Double = 0.0,
    val totalUnrealizedPnLPercent: Double = 0.0,
    val totalLiabilities: Double = 0.0,
    val netWorth: Double = totalCurrentValue
)

data class SipParams(
    val lumpsum: Double = 25000.0,
    val monthlySip: Double = 1500.0,
    val annualStepUpRate: Double = 0.05,
    val expectedReturn: Double = 0.11,
    val years: Int = 15,
    val inflationRate: Double = 0.05
)

data class SipYearPoint(
    val year: Int,
    val totalInvested: Double,
    val maturityValue: Double
)

data class SipResult(
    val totalInvested: Double,
    val wealthGain: Double,
    val maturityValue: Double,
    val inflationAdjustedValue: Double,
    val timeline: List<SipYearPoint>
)

data class SwpParams(
    val initialCorpus: Double = 500000.0,
    val monthlyWithdrawal: Double = 3000.0,
    val annualWithdrawalStepUp: Double = 0.04,
    val expectedReturn: Double = 0.08,
    val years: Int = 25
)

data class SwpResult(
    val initialCorpus: Double,
    val totalWithdrawn: Double,
    val finalBalance: Double,
    val monthsLasted: Int,
    val depletedYear: Int?,
    val yearlyBalances: List<Double>
)

data class FdParams(
    val principal: Double = 100000.0,
    val annualRate: Double = 0.0725,
    val years: Int = 5,
    val compounding: CompoundingFrequency = CompoundingFrequency.QUARTERLY,
    val taxRate: Double = 0.20
)

data class FdResult(
    val principal: Double,
    val grossMaturity: Double,
    val grossInterest: Double,
    val taxDeducted: Double,
    val postTaxMaturity: Double,
    val effectivePostTaxYield: Double
)

data class GoalParams(
    val targetGoal: Double = 250000.0,
    val currentSavings: Double = 20000.0,
    val years: Int = 10,
    val expectedReturn: Double = 0.10,
    val inflationRate: Double = 0.05,
    val adjustForInflation: Boolean = true
)

data class GoalResult(
    val inflationAdjustedGoal: Double,
    val futureValueOfCurrentSavings: Double,
    val shortfallToCover: Double,
    val requiredMonthlySipFlat: Double,
    val requiredMonthlySipStepUp10: Double
)

data class InflationImpactParams(
    val goalAmountToday: Double = 100000.0,
    val annualInflationRate: Double = 0.06,
    val years: Int = 15,
    val expectedSavingsYield: Double = 0.04
)

data class InflationImpactResult(
    val goalAmountToday: Double,
    val futureInflatedCost: Double,
    val inflationCostIncrease: Double,
    val costMultiplier: Double,
    val uninvestedPurchasingPower: Double,
    val purchasingPowerLostPercent: Double,
    val realAnnualReturn: Double,
    val investedRealValue: Double,
    val inflatedCostTimeline: List<Double>,
    val purchasingPowerTimeline: List<Double>
)

@JsonClass(generateAdapter = true)
data class DebtAccount(
    val id: String,
    val name: String,
    val category: String = "Credit Card",
    val balance: Double,
    val annualApr: Double,
    val minimumPayment: Double
)

enum class DebtPayoffStrategy(val label: String, val tagline: String) {
    AVALANCHE("Avalanche", "Highest APR first • Max interest saved"),
    SNOWBALL("Snowball", "Smallest balance first • Fastest quick wins")
}

data class DebtAccountPayoffSummary(
    val id: String,
    val name: String,
    val category: String,
    val startingBalance: Double,
    val annualApr: Double,
    val minimumPayment: Double,
    val payoffMonth: Int,
    val totalInterestPaid: Double,
    val payoffOrder: Int,
    val priorityRank: Int = payoffOrder
)

data class DebtPayoffPlan(
    val strategy: DebtPayoffStrategy,
    val monthsToPayoff: Int,
    val totalInterestPaid: Double,
    val totalPaid: Double,
    val monthlyBalanceTimeline: List<Double>,
    val accountSummaries: List<DebtAccountPayoffSummary>
)

data class DebtPayoffResult(
    val totalDebtBalance: Double,
    val totalMinimumPayment: Double,
    val extraMonthlyPayment: Double,
    val weightedAverageApr: Double,
    val activePlan: DebtPayoffPlan,
    val avalanchePlan: DebtPayoffPlan,
    val snowballPlan: DebtPayoffPlan,
    val minimumOnlyMonths: Int,
    val minimumOnlyInterest: Double,
    val minimumOnlyTimeline: List<Double>,
    val interestSavedVsMinimum: Double,
    val monthsSavedVsMinimum: Int,
    val avalancheInterestAdvantage: Double
)

enum class TaxRegime(val label: String, val isIndian: Boolean) {
    US_SINGLE("US Single", false),
    US_JOINT("US Joint", false),
    IN_OLD("India Old (80C)", true),
    IN_NEW("India New Regime", true)
}

data class TaxParams(
    val regime: TaxRegime = TaxRegime.US_SINGLE,
    val annualIncome: Double = 110_000.0,
    val retirementDeduction: Double = 15_000.0,
    val additionalInvestmentDeduction: Double = 4_000.0,
    val applyStandardDeduction: Boolean = true,
    val realizedCapitalGains: Double = 8_000.0,
    val longTermGains: Boolean = true
)

data class TaxBracketSlice(
    val bracketLabel: String,
    val rate: Double,
    val taxableAmount: Double,
    val taxAmount: Double
)

data class TaxResult(
    val grossIncome: Double,
    val standardDeductionUsed: Double,
    val investmentDeductionsUsed: Double,
    val totalDeductions: Double,
    val taxableIncome: Double,
    val ordinaryIncomeTax: Double,
    val capitalGainsTax: Double,
    val cessOrSurtax: Double,
    val totalTaxLiability: Double,
    val netTakeHomeIncome: Double,
    val effectiveTaxRate: Double,
    val marginalBracketRate: Double,
    val taxSavedFromDeductions: Double,
    val bracketBreakdown: List<TaxBracketSlice>
)

data class RetirementPlanParams(
    val currentAge: Int = 30,
    val retirementAge: Int = 60,
    val lifeExpectancy: Int = 85,
    val targetMonthlyExpensesToday: Double = 4_000.0,
    val annualInflationRate: Double = 0.04,
    val preRetirementReturn: Double = 0.10,
    val postRetirementReturn: Double = 0.065,
    val currentRetirementSavings: Double = 50_000.0
)

data class RetirementPlanPoint(
    val age: Int,
    val projectedCorpus: Double,
    val targetCorpusBenchmark: Double,
    val annualExpenseNominal: Double
)

data class RetirementPlanResult(
    val currentAge: Int,
    val retirementAge: Int,
    val lifeExpectancy: Int,
    val yearsToRetirement: Int,
    val yearsInRetirement: Int,
    val monthlyExpenseAtRetirement: Double,
    val finalMonthlyExpenseAtLifeExpectancy: Double,
    val requiredCorpusAtRetirement: Double,
    val projectedCurrentSavingsAtRetirement: Double,
    val corpusShortfall: Double,
    val requiredMonthlySip: Double,
    val totalRetirementPayoutNominal: Double,
    val realPostRetirementReturn: Double,
    val lifecycleTrajectory: List<RetirementPlanPoint>
)

enum class RecurringType(val label: String) {
    INCOME("Income"),
    EXPENSE("Expense")
}

enum class RecurringFrequency(val label: String, val monthsInterval: Int) {
    MONTHLY("Monthly", 1),
    QUARTERLY("Quarterly", 3),
    YEARLY("Yearly", 12)
}

enum class RecurringTargetLink(val label: String) {
    GENERAL("Cashflow Reserve"),
    PORTFOLIO_SIP("Portfolio Auto-SIP"),
    DEBT_PAYMENT("Debt Auto-Pay")
}

@JsonClass(generateAdapter = true)
data class RecurringTransaction(
    val id: String,
    val title: String,
    val amount: Double,
    val type: RecurringType,
    val category: String,
    val dayOfMonth: Int = 1,
    val frequency: RecurringFrequency = RecurringFrequency.MONTHLY,
    val targetLink: RecurringTargetLink = RecurringTargetLink.GENERAL,
    val linkedEntityId: String? = null,
    val isActive: Boolean = true,
    val autoExecuteOnDue: Boolean = true,
    val lastProcessedYearMonth: String? = null
)

@JsonClass(generateAdapter = true)
data class CashflowExecutionLog(
    val id: String,
    val transactionId: String,
    val title: String,
    val amount: Double,
    val type: RecurringType,
    val yearMonth: String,
    val executedAtMillis: Long,
    val summaryNote: String
)

data class RecurringCashflowSummary(
    val totalMonthlyIncome: Double,
    val totalMonthlyExpense: Double,
    val netMonthlyCashflow: Double,
    val monthlyPortfolioAutoContribution: Double,
    val monthlyDebtAutoRepayment: Double,
    val activeRuleCount: Int,
    val dueThisMonthCount: Int,
    val nextDueSummary: String
)

data class ProEntitlementState(
    val isProUnlocked: Boolean = false,
    val productId: String = "aegis_fire_pro",
    val formattedPrice: String = "$3.99",
    val isStoreReady: Boolean = false,
    val isPurchasePending: Boolean = false,
    val statusMessage: String? = null
)

@JsonClass(generateAdapter = true)
data class BudgetCategoryEntry(
    val id: String,
    val name: String,
    val monthlyLimit: Double,
    val manualSpending: Double = 0.0,
    val savingTarget: Double = 0.0,
    val isEssential: Boolean = true
)

data class BudgetCategoryStatus(
    val entry: BudgetCategoryEntry,
    val recurringCommitted: Double,
    val effectiveSpent: Double,
    val remainingBudget: Double,
    val utilizationRatio: Double,
    val projectedCategorySavings: Double,
    val savingTargetProgress: Double,
    val isOverBudget: Boolean,
    val matchingRecurringCount: Int
)

data class BudgetPlanSummary(
    val monthlyIncome: Double,
    val totalBudgetedLimit: Double,
    val totalRecurringExpenses: Double,
    val totalManualSpending: Double,
    val totalEffectiveSpending: Double,
    val totalCategorySavingTargets: Double,
    val totalProjectedCategorySavings: Double,
    val unallocatedIncome: Double,
    val savingsRatePercent: Double,
    val overBudgetCount: Int,
    val categories: List<BudgetCategoryStatus>
)

enum class FiAchievementCategory(val label: String) {
    PORTFOLIO("Portfolio"),
    DEBT("Debt Crush"),
    CASHFLOW("Cashflow"),
    FIRE_MILESTONE("FIRE Path")
}

enum class FiRankTier(
    val title: String,
    val minScore: Int,
    val tagline: String
) {
    SPARK_STARTER("Spark Starter", 0, "Building initial financial momentum"),
    FOUNDATION_BUILDER("Foundation Builder", 200, "Controlling debt & monthly cashflow"),
    WEALTH_ACCELERATOR("Wealth Accelerator", 420, "Compounding assets & crushing APRs"),
    COAST_COMMANDER("Coast Commander", 650, "Portfolio growth leads living costs"),
    SOVEREIGN_FIRE("Sovereign FIRE", 850, "Financially independent & debt-free")
}

data class FiAchievement(
    val id: String,
    val title: String,
    val description: String,
    val category: FiAchievementCategory,
    val points: Int,
    val progress: Double,
    val progressLabel: String,
    val isUnlocked: Boolean
)

data class FiScoreSummary(
    val totalScore: Int,
    val maxScore: Int = 1000,
    val rank: FiRankTier,
    val nextRank: FiRankTier?,
    val pointsToNextRank: Int,
    val portfolioScore: Int,
    val debtFreedomScore: Int,
    val cashflowScore: Int,
    val fireCompletionRatio: Double,
    val debtReductionRatio: Double,
    val unlockedCount: Int,
    val totalAchievements: Int,
    val achievements: List<FiAchievement>
)

enum class DailyWisdomCategory(val label: String) {
    COMPOUNDING("Compounding"),
    DEBT_FREEDOM("Debt Crush"),
    TAX_ALPHA("Tax Efficiency"),
    BEHAVIORAL("Mindset"),
    SAVINGS_RATE("Cashflow")
}

data class DailyFinancialTip(
    val id: Int,
    val category: DailyWisdomCategory,
    val quote: String,
    val author: String,
    val actionableTip: String
)



