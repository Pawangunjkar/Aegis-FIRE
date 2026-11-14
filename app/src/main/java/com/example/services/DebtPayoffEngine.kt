package com.example.services

import com.example.models.DebtAccount
import com.example.models.DebtAccountPayoffSummary
import com.example.models.DebtPayoffPlan
import com.example.models.DebtPayoffResult
import com.example.models.DebtPayoffStrategy
import kotlin.math.min

object DebtPayoffEngine {

    private const val MAX_MONTHS = 600

    fun calculateDebtPayoff(
        accounts: List<DebtAccount>,
        extraMonthlyPayment: Double,
        selectedStrategy: DebtPayoffStrategy
    ): DebtPayoffResult {
        val activeAccounts = accounts.filter { it.balance > 0.0 }
        val extra = extraMonthlyPayment.coerceAtLeast(0.0)

        if (activeAccounts.isEmpty()) {
            val emptyAvalanche = DebtPayoffPlan(
                strategy = DebtPayoffStrategy.AVALANCHE,
                monthsToPayoff = 0,
                totalInterestPaid = 0.0,
                totalPaid = 0.0,
                monthlyBalanceTimeline = listOf(0.0),
                accountSummaries = emptyList()
            )
            val emptySnowball = emptyAvalanche.copy(strategy = DebtPayoffStrategy.SNOWBALL)
            val activeEmpty = if (selectedStrategy == DebtPayoffStrategy.AVALANCHE) emptyAvalanche else emptySnowball
            return DebtPayoffResult(
                totalDebtBalance = 0.0,
                totalMinimumPayment = 0.0,
                extraMonthlyPayment = extra,
                weightedAverageApr = 0.0,
                activePlan = activeEmpty,
                avalanchePlan = emptyAvalanche,
                snowballPlan = emptySnowball,
                minimumOnlyMonths = 0,
                minimumOnlyInterest = 0.0,
                minimumOnlyTimeline = listOf(0.0),
                interestSavedVsMinimum = 0.0,
                monthsSavedVsMinimum = 0,
                avalancheInterestAdvantage = 0.0
            )
        }

        val totalBalance = activeAccounts.sumOf { it.balance }
        val totalMin = activeAccounts.sumOf { it.minimumPayment.coerceAtLeast(0.0) }
        val weightedApr = if (totalBalance > 0.0) {
            activeAccounts.sumOf { it.balance * it.annualApr.coerceAtLeast(0.0) } / totalBalance
        } else {
            0.0
        }

        val avalanchePlan = simulatePlan(activeAccounts, extra, DebtPayoffStrategy.AVALANCHE, allowRollover = true)
        val snowballPlan = simulatePlan(activeAccounts, extra, DebtPayoffStrategy.SNOWBALL, allowRollover = true)
        val minOnlyPlan = simulatePlan(activeAccounts, 0.0, DebtPayoffStrategy.AVALANCHE, allowRollover = false)

        val activePlan = if (selectedStrategy == DebtPayoffStrategy.AVALANCHE) avalanchePlan else snowballPlan
        val interestSaved = (minOnlyPlan.totalInterestPaid - activePlan.totalInterestPaid).coerceAtLeast(0.0)
        val monthsSaved = (minOnlyPlan.monthsToPayoff - activePlan.monthsToPayoff).coerceAtLeast(0)
        val avalancheAdvantage = snowballPlan.totalInterestPaid - avalanchePlan.totalInterestPaid

        return DebtPayoffResult(
            totalDebtBalance = totalBalance,
            totalMinimumPayment = totalMin,
            extraMonthlyPayment = extra,
            weightedAverageApr = weightedApr,
            activePlan = activePlan,
            avalanchePlan = avalanchePlan,
            snowballPlan = snowballPlan,
            minimumOnlyMonths = minOnlyPlan.monthsToPayoff,
            minimumOnlyInterest = minOnlyPlan.totalInterestPaid,
            minimumOnlyTimeline = minOnlyPlan.monthlyBalanceTimeline,
            interestSavedVsMinimum = interestSaved,
            monthsSavedVsMinimum = monthsSaved,
            avalancheInterestAdvantage = avalancheAdvantage
        )
    }

    private fun simulatePlan(
        accounts: List<DebtAccount>,
        extraMonthly: Double,
        strategy: DebtPayoffStrategy,
        allowRollover: Boolean
    ): DebtPayoffPlan {
        data class MutableDebtState(
            val account: DebtAccount,
            var balance: Double,
            var interestPaid: Double = 0.0,
            var totalPaid: Double = 0.0,
            var payoffMonth: Int = 0,
            var payoffOrder: Int = 0
        )

        val states = accounts.map { acc ->
            MutableDebtState(
                account = acc,
                balance = acc.balance.coerceAtLeast(0.0)
            )
        }

        val totalMonthlyBudget = accounts.sumOf { it.minimumPayment.coerceAtLeast(0.0) } + extraMonthly
        val timeline = mutableListOf(states.sumOf { it.balance })
        var month = 0
        var nextPayoffOrder = 1

        while (month < MAX_MONTHS && states.any { it.balance > 0.01 }) {
            month++
            var paidThisMonth = 0.0

            // 1. Accrue monthly interest
            states.forEach { state ->
                if (state.balance > 0.01) {
                    val monthlyRate = state.account.annualApr.coerceAtLeast(0.0) / 12.0
                    val interest = state.balance * monthlyRate
                    state.balance += interest
                    state.interestPaid += interest
                }
            }

            // 2. Pay minimum payments on active accounts
            states.forEach { state ->
                if (state.balance > 0.01) {
                    // Guard against endless negative amortization when no extra budget exists
                    val floorMin = state.account.minimumPayment.coerceAtLeast(0.0)
                    val payment = min(state.balance, floorMin)
                    state.balance -= payment
                    state.totalPaid += payment
                    paidThisMonth += payment
                    if (state.balance <= 0.01) {
                        state.balance = 0.0
                        if (state.payoffMonth == 0) {
                            state.payoffMonth = month
                            state.payoffOrder = nextPayoffOrder++
                        }
                    }
                }
            }

            // 3. Apply extra + rolled-over payments according to Snowball or Avalanche priority
            var pool = if (allowRollover) {
                (totalMonthlyBudget - paidThisMonth).coerceAtLeast(0.0)
            } else {
                extraMonthly.coerceAtLeast(0.0)
            }

            if (pool > 0.01) {
                val orderedTargets = when (strategy) {
                    DebtPayoffStrategy.AVALANCHE -> states
                        .filter { it.balance > 0.01 }
                        .sortedWith(
                            compareByDescending<MutableDebtState> { it.account.annualApr }
                                .thenBy { it.balance }
                        )
                    DebtPayoffStrategy.SNOWBALL -> states
                        .filter { it.balance > 0.01 }
                        .sortedWith(
                            compareBy<MutableDebtState> { it.balance }
                                .thenByDescending { it.account.annualApr }
                        )
                }

                for (target in orderedTargets) {
                    if (pool <= 0.01) break
                    val payment = min(target.balance, pool)
                    target.balance -= payment
                    target.totalPaid += payment
                    pool -= payment
                    if (target.balance <= 0.01) {
                        target.balance = 0.0
                        if (target.payoffMonth == 0) {
                            target.payoffMonth = month
                            target.payoffOrder = nextPayoffOrder++
                        }
                    }
                }
            }

            timeline.add(states.sumOf { it.balance })
        }

        // Any debt still open at MAX_MONTHS gets capped order/month
        states.filter { it.balance > 0.01 }.forEach { state ->
            state.payoffMonth = MAX_MONTHS
            if (state.payoffOrder == 0) {
                state.payoffOrder = nextPayoffOrder++
            }
        }

        val initialOrder = when (strategy) {
            DebtPayoffStrategy.AVALANCHE -> accounts.sortedWith(
                compareByDescending<DebtAccount> { it.annualApr }.thenBy { it.balance }
            )
            DebtPayoffStrategy.SNOWBALL -> accounts.sortedWith(
                compareBy<DebtAccount> { it.balance }.thenByDescending { it.annualApr }
            )
        }
        val priorityRankById = initialOrder.mapIndexed { index, acc -> acc.id to (index + 1) }.toMap()

        val summaries = states.map { s ->
            val rank = priorityRankById[s.account.id] ?: s.payoffOrder
            DebtAccountPayoffSummary(
                id = s.account.id,
                name = s.account.name,
                category = s.account.category,
                startingBalance = s.account.balance,
                annualApr = s.account.annualApr,
                minimumPayment = s.account.minimumPayment,
                payoffMonth = s.payoffMonth,
                totalInterestPaid = s.interestPaid,
                payoffOrder = s.payoffOrder,
                priorityRank = rank
            )
        }.sortedBy { it.priorityRank }

        return DebtPayoffPlan(
            strategy = strategy,
            monthsToPayoff = month,
            totalInterestPaid = states.sumOf { it.interestPaid },
            totalPaid = states.sumOf { it.totalPaid },
            monthlyBalanceTimeline = timeline,
            accountSummaries = summaries
        )
    }
}
