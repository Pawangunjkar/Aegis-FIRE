package com.example.services

import com.example.models.BudgetCategoryEntry
import com.example.models.BudgetCategoryStatus
import com.example.models.BudgetPlanSummary
import com.example.models.RecurringTransaction
import com.example.models.RecurringType
import java.util.Locale

object BudgetEngine {

    fun calculateBudgetSummary(
        categories: List<BudgetCategoryEntry>,
        recurringTransactions: List<RecurringTransaction>,
        useRecurringIncome: Boolean = true,
        customMonthlyIncome: Double = 0.0
    ): BudgetPlanSummary {
        val activeRules = recurringTransactions.filter { it.isActive && it.amount > 0.0 }
        val recurringIncome = activeRules
            .filter { it.type == RecurringType.INCOME }
            .sumOf { it.amount / it.frequency.monthsInterval.toDouble() }

        val effectiveIncome = if (useRecurringIncome && recurringIncome > 0.0) {
            recurringIncome
        } else {
            customMonthlyIncome.coerceAtLeast(0.0)
        }

        val activeExpenses = activeRules.filter { it.type == RecurringType.EXPENSE }

        // Group recurring expense monthly equivalents and rule counts by normalized category key
        val categoryKeyByLower = categories.associate { it.name.trim().lowercase(Locale.US) to it.id }
        val fallbackOtherId = categories.firstOrNull {
            it.name.trim().equals("Other", ignoreCase = true)
        }?.id

        val recurringAmountByCategoryId = mutableMapOf<String, Double>()
        val recurringCountByCategoryId = mutableMapOf<String, Int>()
        var unmappedRecurringAmount = 0.0
        var unmappedRecurringCount = 0

        activeExpenses.forEach { tx ->
            val monthlyEq = tx.amount / tx.frequency.monthsInterval.toDouble()
            val normalizedCat = tx.category.trim().lowercase(Locale.US)
            val matchedId = categoryKeyByLower[normalizedCat] ?: fallbackOtherId
            if (matchedId != null) {
                recurringAmountByCategoryId[matchedId] =
                    (recurringAmountByCategoryId[matchedId] ?: 0.0) + monthlyEq
                recurringCountByCategoryId[matchedId] =
                    (recurringCountByCategoryId[matchedId] ?: 0) + 1
            } else {
                unmappedRecurringAmount += monthlyEq
                unmappedRecurringCount++
            }
        }

        val statuses = categories.map { entry ->
            val recurringAmt = recurringAmountByCategoryId[entry.id] ?: 0.0
            val recurringCount = recurringCountByCategoryId[entry.id] ?: 0
            buildStatus(entry, recurringAmt, recurringCount)
        }.toMutableList()

        // Ensure unmatched recurring expenses are never silently dropped even if the user removed "Other"
        if (unmappedRecurringAmount > 0.0) {
            val virtualEntry = BudgetCategoryEntry(
                id = "unmapped_recurring_other",
                name = "Other (Recurring)",
                monthlyLimit = unmappedRecurringAmount,
                manualSpending = 0.0,
                savingTarget = 0.0,
                isEssential = true
            )
            statuses.add(buildStatus(virtualEntry, unmappedRecurringAmount, unmappedRecurringCount))
        }

        val totalLimit = statuses.sumOf { it.entry.monthlyLimit }
        val totalRecurring = statuses.sumOf { it.recurringCommitted }
        val totalManual = statuses.sumOf { it.entry.manualSpending }
        val totalEffective = statuses.sumOf { it.effectiveSpent }
        val totalSavingTargets = statuses.sumOf { it.entry.savingTarget }
        val totalProjectedSavings = statuses.sumOf { it.projectedCategorySavings }
        val unallocated = effectiveIncome - totalEffective
        val overBudgetCount = statuses.count { it.isOverBudget }

        val investmentSipOutflow = activeExpenses
            .filter {
                it.category.equals("Investment SIP", ignoreCase = true) ||
                    it.targetLink == com.example.models.RecurringTargetLink.PORTFOLIO_SIP
            }
            .sumOf { it.amount / it.frequency.monthsInterval.toDouble() }

        val netSavingsInclSip = (unallocated + investmentSipOutflow).coerceAtLeast(0.0)
        val savingsRate = if (effectiveIncome > 0.0) {
            (netSavingsInclSip / effectiveIncome) * 100.0
        } else {
            0.0
        }

        return BudgetPlanSummary(
            monthlyIncome = effectiveIncome,
            totalBudgetedLimit = totalLimit,
            totalRecurringExpenses = totalRecurring,
            totalManualSpending = totalManual,
            totalEffectiveSpending = totalEffective,
            totalCategorySavingTargets = totalSavingTargets,
            totalProjectedCategorySavings = totalProjectedSavings,
            unallocatedIncome = unallocated,
            savingsRatePercent = savingsRate,
            overBudgetCount = overBudgetCount,
            categories = statuses
        )
    }

    private fun buildStatus(
        entry: BudgetCategoryEntry,
        recurringCommitted: Double,
        matchingRecurringCount: Int
    ): BudgetCategoryStatus {
        val safeLimit = entry.monthlyLimit.coerceAtLeast(0.0)
        val safeManual = entry.manualSpending.coerceAtLeast(0.0)
        val safeTarget = entry.savingTarget.coerceAtLeast(0.0)
        val effectiveSpent = recurringCommitted + safeManual
        val remaining = safeLimit - effectiveSpent
        val utilization = when {
            safeLimit > 0.0 -> effectiveSpent / safeLimit
            effectiveSpent > 0.0 -> 1.0
            else -> 0.0
        }
        val categorySavings = remaining.coerceAtLeast(0.0)
        val savingProgress = if (safeTarget > 0.0) {
            (categorySavings / safeTarget).coerceIn(0.0, 1.0)
        } else {
            1.0
        }

        return BudgetCategoryStatus(
            entry = entry,
            recurringCommitted = recurringCommitted,
            effectiveSpent = effectiveSpent,
            remainingBudget = remaining,
            utilizationRatio = utilization,
            projectedCategorySavings = categorySavings,
            savingTargetProgress = savingProgress,
            isOverBudget = effectiveSpent > safeLimit + 0.01,
            matchingRecurringCount = matchingRecurringCount
        )
    }
}
