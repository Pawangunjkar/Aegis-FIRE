package com.example.services

import com.example.models.BudgetPlanSummary
import com.example.models.DebtAccount
import com.example.models.FiAchievement
import com.example.models.FiAchievementCategory
import com.example.models.FiRankTier
import com.example.models.FiScoreSummary
import com.example.models.FireParameters
import com.example.models.FireResult
import com.example.models.PortfolioSummary
import com.example.models.RecurringTargetLink
import com.example.models.RecurringTransaction
import java.util.Locale
import kotlin.math.roundToInt

object FiScoreEngine {

    fun calculateScore(
        portfolioSummary: PortfolioSummary,
        debtAccounts: List<DebtAccount>,
        peakDebtBalance: Double,
        fireParams: FireParameters,
        fireResult: FireResult?,
        budgetSummary: BudgetPlanSummary,
        recurringTransactions: List<RecurringTransaction>,
        extraDebtPayment: Double,
        isIndianCurrency: Boolean
    ): FiScoreSummary {
        val currencyFactor = if (isIndianCurrency) 25.0 else 1.0
        val currentAssets = portfolioSummary.totalCurrentValue.coerceAtLeast(0.0)
        val trackedAccountDebt = debtAccounts.sumOf { it.balance.coerceAtLeast(0.0) }
        val totalDebt = maxOf(trackedAccountDebt, portfolioSummary.totalLiabilities.coerceAtLeast(0.0))
        val effectivePeakDebt = maxOf(peakDebtBalance, totalDebt)
        val netWorth = (currentAssets - totalDebt).coerceAtLeast(0.0)

        // 1. Portfolio Sub-Score (0..450)
        val targetFire = (fireResult?.targetFireNumber ?: (fireParams.monthlyExpense * 300.0)).coerceAtLeast(1.0)
        val fireRatio = (netWorth / targetFire).coerceIn(0.0, 1.0)
        val fireProgressPts = (fireRatio * 300.0).roundToInt()

        val activeAssetClasses = portfolioSummary.allocationSlices.count { it.currentValue > 0.0 }
        val diversificationPts = ((activeAssetClasses / 3.0).coerceIn(0.0, 1.0) * 50.0).roundToInt()
        val positiveNetWorthPts = if (currentAssets > totalDebt && currentAssets > 0.0) 50 else 0
        val sipTargetMonthly = 1_500.0 * currencyFactor
        val sipMomentumPts = ((portfolioSummary.totalMonthlyContribution / sipTargetMonthly).coerceIn(0.0, 1.0) * 50.0).roundToInt()
        val portfolioScore = (fireProgressPts + diversificationPts + positiveNetWorthPts + sipMomentumPts).coerceIn(0, 450)

        // 2. Debt Freedom Sub-Score (0..300)
        val debtReductionRatio = if (effectivePeakDebt > 0.0) {
            ((effectivePeakDebt - totalDebt) / effectivePeakDebt).coerceIn(0.0, 1.0)
        } else {
            1.0
        }
        val zeroHighApr = debtAccounts.none { it.balance > 0.0 && it.annualApr >= 0.15 }
        val debtFreePts = if (totalDebt <= 0.01) {
            300
        } else {
            val solvencyRatio = (currentAssets / (currentAssets + totalDebt)).coerceIn(0.0, 1.0)
            val solvencyPts = (solvencyRatio * 140.0).roundToInt()
            val paydownPts = (debtReductionRatio * 110.0).roundToInt()
            val aprBonus = if (zeroHighApr) 30 else 0
            val totalMin = debtAccounts.filter { it.balance > 0.0 }.sumOf { it.minimumPayment }.coerceAtLeast(1.0)
            val extraEffortPts = ((extraDebtPayment / (totalMin * 0.5)).coerceIn(0.0, 1.0) * 20.0).roundToInt()
            (solvencyPts + paydownPts + aprBonus + extraEffortPts).coerceIn(0, 295)
        }

        // 3. Cashflow & Savings Discipline Sub-Score (0..250)
        val savingsRateRatio = (budgetSummary.savingsRatePercent / 35.0).coerceIn(0.0, 1.0)
        val savingsRatePts = (savingsRateRatio * 150.0).roundToInt()

        val activeAutoRules = recurringTransactions.count {
            it.isActive && (it.targetLink == RecurringTargetLink.PORTFOLIO_SIP || it.targetLink == RecurringTargetLink.DEBT_PAYMENT)
        }
        val automationPts = if (activeAutoRules > 0) 50 else 0

        val budgetTargetRatio = if (budgetSummary.totalCategorySavingTargets > 0.0) {
            (budgetSummary.totalProjectedCategorySavings / budgetSummary.totalCategorySavingTargets).coerceIn(0.0, 1.0)
        } else {
            1.0
        }
        val budgetTargetPts = (budgetTargetRatio * 50.0).roundToInt()
        val cashflowScore = (savingsRatePts + automationPts + budgetTargetPts).coerceIn(0, 250)

        val totalScore = (portfolioScore + debtFreePts + cashflowScore).coerceIn(0, 1000)

        val rank = FiRankTier.entries.lastOrNull { totalScore >= it.minScore } ?: FiRankTier.SPARK_STARTER
        val nextRank = FiRankTier.entries.firstOrNull { it.minScore > totalScore }
        val pointsToNext = if (nextRank != null) (nextRank.minScore - totalScore).coerceAtLeast(0) else 0

        val milestone25k = 25_000.0 * currencyFactor
        val milestone100k = 100_000.0 * currencyFactor
        val coastThreshold = (fireResult?.coastFireNumber ?: (targetFire * 0.4)).coerceAtLeast(1.0)

        val achievements = listOf(
            FiAchievement(
                id = "first_25k_net_worth",
                title = if (isIndianCurrency) "First ₹6.25L Net Worth" else "First $25K Net Worth",
                description = "Build positive net worth above the initial emergency & investing threshold.",
                category = FiAchievementCategory.PORTFOLIO,
                points = 50,
                progress = (netWorth / milestone25k).coerceIn(0.0, 1.0),
                progressLabel = "${((netWorth / milestone25k).coerceIn(0.0, 1.0) * 100).roundToInt()}%",
                isUnlocked = netWorth >= milestone25k
            ),
            FiAchievement(
                id = "six_figure_builder",
                title = if (isIndianCurrency) "₹25L Portfolio Club" else "Six-Figure Net Worth",
                description = "Grow net worth to the compounding inflection milestone.",
                category = FiAchievementCategory.PORTFOLIO,
                points = 100,
                progress = (netWorth / milestone100k).coerceIn(0.0, 1.0),
                progressLabel = "${((netWorth / milestone100k).coerceIn(0.0, 1.0) * 100).roundToInt()}%",
                isUnlocked = netWorth >= milestone100k
            ),
            FiAchievement(
                id = "multi_asset_shield",
                title = "Multi-Asset Shield",
                description = "Diversify across 3+ asset classes (Equities, Mutual Funds, FDs, or Bonds).",
                category = FiAchievementCategory.PORTFOLIO,
                points = 60,
                progress = (activeAssetClasses / 3.0).coerceIn(0.0, 1.0),
                progressLabel = "$activeAssetClasses / 3 classes",
                isUnlocked = activeAssetClasses >= 3
            ),
            FiAchievement(
                id = "steady_compounder",
                title = "Steady Compounder",
                description = "Sustain monthly portfolio SIPs of at least ${if (isIndianCurrency) "₹37.5k" else "$1,500"}/mo.",
                category = FiAchievementCategory.PORTFOLIO,
                points = 60,
                progress = (portfolioSummary.totalMonthlyContribution / sipTargetMonthly).coerceIn(0.0, 1.0),
                progressLabel = "${((portfolioSummary.totalMonthlyContribution / sipTargetMonthly).coerceIn(0.0, 1.0) * 100).roundToInt()}%",
                isUnlocked = portfolioSummary.totalMonthlyContribution >= sipTargetMonthly
            ),
            FiAchievement(
                id = "first_debt_dent",
                title = "First Debt Dent (10%)",
                description = "Pay down at least 10% from your peak tracked debt balance.",
                category = FiAchievementCategory.DEBT,
                points = 70,
                progress = if (totalDebt <= 0.01) 1.0 else (debtReductionRatio / 0.10).coerceIn(0.0, 1.0),
                progressLabel = if (totalDebt <= 0.01) "Debt-Free" else "${String.format(Locale.US, "%.0f", debtReductionRatio * 100)}% paid",
                isUnlocked = totalDebt <= 0.01 || debtReductionRatio >= 0.10
            ),
            FiAchievement(
                id = "high_apr_slayer",
                title = "High-APR Slayer",
                description = "Eliminate all high-interest debt accounts (15%+ APR credit cards).",
                category = FiAchievementCategory.DEBT,
                points = 90,
                progress = if (zeroHighApr) 1.0 else 0.0,
                progressLabel = if (zeroHighApr) "Cleared" else "High-APR active",
                isUnlocked = zeroHighApr
            ),
            FiAchievement(
                id = "debt_free_sovereign",
                title = "100% Debt-Free",
                description = "Bring total balances across all debt accounts and liabilities to zero.",
                category = FiAchievementCategory.DEBT,
                points = 140,
                progress = if (totalDebt <= 0.01) 1.0 else debtReductionRatio,
                progressLabel = if (totalDebt <= 0.01) "Zero Debt" else "${String.format(Locale.US, "%.0f", debtReductionRatio * 100)}% cleared",
                isUnlocked = totalDebt <= 0.01
            ),
            FiAchievement(
                id = "target_saver",
                title = "Category Target Saver",
                description = "Hit at least 75% of your combined monthly budget saving targets.",
                category = FiAchievementCategory.CASHFLOW,
                points = 70,
                progress = (budgetTargetRatio / 0.75).coerceIn(0.0, 1.0),
                progressLabel = "${(budgetTargetRatio * 100).roundToInt()}% of goal",
                isUnlocked = budgetTargetRatio >= 0.75
            ),
            FiAchievement(
                id = "autopilot_wealth",
                title = "Autopilot Wealth",
                description = "Run active recurring Portfolio SIP or Debt Auto-Pay automation rules.",
                category = FiAchievementCategory.CASHFLOW,
                points = 60,
                progress = if (activeAutoRules > 0) 1.0 else 0.0,
                progressLabel = "$activeAutoRules active",
                isUnlocked = activeAutoRules > 0
            ),
            FiAchievement(
                id = "coast_fire_unlocked",
                title = "Coast FIRE Reached",
                description = "Net worth covers Coast FIRE so compounding alone reaches target age.",
                category = FiAchievementCategory.FIRE_MILESTONE,
                points = 150,
                progress = (netWorth / coastThreshold).coerceIn(0.0, 1.0),
                progressLabel = "${((netWorth / coastThreshold).coerceIn(0.0, 1.0) * 100).roundToInt()}%",
                isUnlocked = netWorth >= coastThreshold
            )
        )

        return FiScoreSummary(
            totalScore = totalScore,
            maxScore = 1000,
            rank = rank,
            nextRank = nextRank,
            pointsToNextRank = pointsToNext,
            portfolioScore = portfolioScore,
            debtFreedomScore = debtFreePts,
            cashflowScore = cashflowScore,
            fireCompletionRatio = fireRatio,
            debtReductionRatio = debtReductionRatio,
            unlockedCount = achievements.count { it.isUnlocked },
            totalAchievements = achievements.size,
            achievements = achievements
        )
    }
}
