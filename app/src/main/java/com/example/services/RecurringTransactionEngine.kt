package com.example.services

import com.example.models.AssetHolding
import com.example.models.CashflowExecutionLog
import com.example.models.DebtAccount
import com.example.models.RecurringCashflowSummary
import com.example.models.RecurringTargetLink
import com.example.models.RecurringTransaction
import com.example.models.RecurringType
import java.util.Calendar
import java.util.Locale
import java.util.UUID

data class RecurringExecutionOutcome(
    val updatedTransactions: List<RecurringTransaction>,
    val updatedHoldings: List<AssetHolding>,
    val updatedDebts: List<DebtAccount>,
    val newLogs: List<CashflowExecutionLog>,
    val executedCount: Int
)

object RecurringTransactionEngine {

    fun currentYearMonth(calendar: Calendar = Calendar.getInstance()): String {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        return String.format(Locale.US, "%04d-%02d", year, month)
    }

    fun currentDayOfMonth(calendar: Calendar = Calendar.getInstance()): Int {
        return calendar.get(Calendar.DAY_OF_MONTH).coerceIn(1, 31)
    }

    private fun parseTotalMonths(yearMonth: String?): Int? {
        if (yearMonth.isNullOrBlank()) return null
        val parts = yearMonth.split("-")
        if (parts.size != 2) return null
        val y = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        if (m !in 1..12) return null
        return y * 12 + (m - 1)
    }

    fun isDueForCycle(
        tx: RecurringTransaction,
        yearMonth: String,
        dayOfMonth: Int
    ): Boolean {
        if (!tx.isActive || tx.amount <= 0.0) return false
        if (dayOfMonth < tx.dayOfMonth.coerceIn(1, 28)) return false
        val lastMonths = parseTotalMonths(tx.lastProcessedYearMonth) ?: return true
        val currentMonths = parseTotalMonths(yearMonth) ?: return false
        return (currentMonths - lastMonths) >= tx.frequency.monthsInterval
    }

    fun calculateCashflowSummary(
        transactions: List<RecurringTransaction>,
        yearMonth: String = currentYearMonth(),
        dayOfMonth: Int = currentDayOfMonth()
    ): RecurringCashflowSummary {
        var monthlyIncome = 0.0
        var monthlyExpense = 0.0
        var monthlyPortfolioAuto = 0.0
        var monthlyDebtAuto = 0.0
        var activeCount = 0
        var dueCount = 0
        var nextPendingDay: Int? = null
        var nextPendingTitle: String? = null

        transactions.forEach { tx ->
            if (tx.isActive) {
                activeCount++
                val monthlyEquiv = tx.amount / tx.frequency.monthsInterval.toDouble()
                when (tx.type) {
                    RecurringType.INCOME -> monthlyIncome += monthlyEquiv
                    RecurringType.EXPENSE -> monthlyExpense += monthlyEquiv
                }
                when (tx.targetLink) {
                    RecurringTargetLink.PORTFOLIO_SIP -> monthlyPortfolioAuto += monthlyEquiv
                    RecurringTargetLink.DEBT_PAYMENT -> monthlyDebtAuto += monthlyEquiv
                    RecurringTargetLink.GENERAL -> Unit
                }
                if (isDueForCycle(tx, yearMonth, dayOfMonth)) {
                    dueCount++
                } else if (tx.lastProcessedYearMonth != yearMonth && tx.dayOfMonth > dayOfMonth) {
                    if (nextPendingDay == null || tx.dayOfMonth < nextPendingDay!!) {
                        nextPendingDay = tx.dayOfMonth
                        nextPendingTitle = tx.title
                    }
                }
            }
        }

        val nextSummary = when {
            dueCount > 0 -> "$dueCount automated entry(ies) due now"
            nextPendingDay != null && nextPendingTitle != null -> "Next: $nextPendingTitle (Day $nextPendingDay)"
            activeCount == 0 -> "No active recurring schedules"
            else -> "All scheduled entries settled this month"
        }

        return RecurringCashflowSummary(
            totalMonthlyIncome = monthlyIncome,
            totalMonthlyExpense = monthlyExpense,
            netMonthlyCashflow = monthlyIncome - monthlyExpense,
            monthlyPortfolioAutoContribution = monthlyPortfolioAuto,
            monthlyDebtAutoRepayment = monthlyDebtAuto,
            activeRuleCount = activeCount,
            dueThisMonthCount = dueCount,
            nextDueSummary = nextSummary
        )
    }

    fun processDueTransactions(
        transactions: List<RecurringTransaction>,
        holdings: List<AssetHolding>,
        debts: List<DebtAccount>,
        yearMonth: String = currentYearMonth(),
        dayOfMonth: Int = currentDayOfMonth(),
        requireAutoExecuteFlag: Boolean = true,
        nowMillis: Long = System.currentTimeMillis()
    ): RecurringExecutionOutcome {
        val mutableHoldings = holdings.toMutableList()
        val mutableDebts = debts.toMutableList()
        val newLogs = mutableListOf<CashflowExecutionLog>()
        var executedCount = 0

        val updatedTxs = transactions.map { tx ->
            val autoOk = !requireAutoExecuteFlag || tx.autoExecuteOnDue
            if (!autoOk || !isDueForCycle(tx, yearMonth, dayOfMonth)) {
                tx
            } else {
                executedCount++
                val note = when (tx.targetLink) {
                    RecurringTargetLink.PORTFOLIO_SIP -> {
                        val idx = if (!tx.linkedEntityId.isNullOrBlank()) {
                            mutableHoldings.indexOfFirst { it.id == tx.linkedEntityId }
                        } else {
                            if (mutableHoldings.isNotEmpty()) 0 else -1
                        }
                        if (idx >= 0) {
                            val h = mutableHoldings[idx]
                            val updatedHolding = if (h.quantity > 0.0 && h.currentMarketPrice > 0.0) {
                                val addedUnits = tx.amount / h.currentMarketPrice
                                h.copy(
                                    quantity = h.quantity + addedUnits,
                                    currentValue = (h.quantity + addedUnits) * h.currentMarketPrice,
                                    investedCost = h.effectiveCostBasis + tx.amount
                                )
                            } else {
                                h.copy(
                                    currentValue = h.currentValue + tx.amount,
                                    investedCost = h.investedCost + tx.amount
                                )
                            }
                            mutableHoldings[idx] = updatedHolding
                            "Added to ${h.name}"
                        } else {
                            "Recorded portfolio auto-SIP cashflow"
                        }
                    }

                    RecurringTargetLink.DEBT_PAYMENT -> {
                        val idx = if (!tx.linkedEntityId.isNullOrBlank()) {
                            mutableDebts.indexOfFirst { it.id == tx.linkedEntityId && it.balance > 0.0 }
                        } else {
                            mutableDebts.indices
                                .filter { mutableDebts[it].balance > 0.0 }
                                .maxByOrNull { mutableDebts[it].annualApr }
                                ?: -1
                        }
                        if (idx >= 0) {
                            val d = mutableDebts[idx]
                            val applied = minOf(tx.amount, d.balance)
                            mutableDebts[idx] = d.copy(balance = (d.balance - applied).coerceAtLeast(0.0))
                            "Reduced ${d.name} balance"
                        } else {
                            "Recorded extra debt repayment"
                        }
                    }

                    RecurringTargetLink.GENERAL -> {
                        "Automated ${tx.type.label.lowercase(Locale.US)} (${tx.category}) posted"
                    }
                }

                newLogs.add(
                    CashflowExecutionLog(
                        id = UUID.randomUUID().toString(),
                        transactionId = tx.id,
                        title = tx.title,
                        amount = tx.amount,
                        type = tx.type,
                        yearMonth = yearMonth,
                        executedAtMillis = nowMillis,
                        summaryNote = note
                    )
                )
                tx.copy(lastProcessedYearMonth = yearMonth)
            }
        }

        return RecurringExecutionOutcome(
            updatedTransactions = updatedTxs,
            updatedHoldings = mutableHoldings,
            updatedDebts = mutableDebts,
            newLogs = newLogs,
            executedCount = executedCount
        )
    }
}
