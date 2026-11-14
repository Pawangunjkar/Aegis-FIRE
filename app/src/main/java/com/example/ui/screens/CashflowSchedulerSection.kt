package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.RecurringTargetLink
import com.example.models.RecurringTransaction
import com.example.models.RecurringType
import com.example.providers.FireViewModel
import com.example.ui.components.MetricCard
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RoseCrimson
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun CashflowSchedulerSection(
    viewModel: FireViewModel,
    onFeedback: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val transactions by viewModel.recurringTransactions.collectAsState()
    val summary by viewModel.cashflowSummary.collectAsState()
    val logs by viewModel.cashflowLogs.collectAsState()
    val holdings by viewModel.holdings.collectAsState()
    val debts by viewModel.debtAccounts.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<RecurringTransaction?>(null) }

    // 2x2 KPI Overview
    Row(modifier = Modifier.fillMaxWidth()) {
        MetricCard(
            title = "Monthly Income",
            value = viewModel.formatShortCurrency(summary.totalMonthlyIncome),
            subtitle = "${summary.activeRuleCount} active rule(s)",
            color = EmeraldGreen,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(12.dp))
        MetricCard(
            title = "Monthly Expenses",
            value = viewModel.formatShortCurrency(summary.totalMonthlyExpense),
            subtitle = "Auto-SIP + Living + Debt",
            color = RoseCrimson,
            modifier = Modifier.weight(1f)
        )
    }

    Spacer(Modifier.height(12.dp))

    Row(modifier = Modifier.fillMaxWidth()) {
        val surplusPositive = summary.netMonthlyCashflow >= 0.0
        MetricCard(
            title = "Net Monthly Surplus",
            value = viewModel.formatShortCurrency(summary.netMonthlyCashflow),
            subtitle = if (surplusPositive) "Available to allocate" else "Monthly cash deficit",
            color = if (surplusPositive) CyanAccent else RoseCrimson,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(12.dp))
        MetricCard(
            title = "Auto-Linked Flow",
            value = viewModel.formatShortCurrency(summary.monthlyPortfolioAutoContribution + summary.monthlyDebtAutoRepayment),
            subtitle = "SIP ${viewModel.formatShortCurrency(summary.monthlyPortfolioAutoContribution)} • Debt ${viewModel.formatShortCurrency(summary.monthlyDebtAutoRepayment)}",
            color = AmberGold,
            modifier = Modifier.weight(1f)
        )
    }

    Spacer(Modifier.height(12.dp))

    // Scheduler Status & Trigger Bar
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("AUTOMATION STATUS", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(4.dp))
            Text(summary.nextDueSummary, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val count = viewModel.processDueRecurringTransactions(
                            context = context,
                            requireAutoExecuteFlag = false
                        )
                        onFeedback(
                            if (count > 0) "Processed $count due recurring entry(ies) into Portfolio & Debt ledgers"
                            else "No pending entries due yet for this month"
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("process_due_transactions_button")
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = DeepObsidian, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Run Due (${summary.dueThisMonthCount})", color = DeepObsidian, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("add_recurring_rule_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Rule", color = CyanAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val synced = viewModel.applyCashflowToPortfolioSip(context)
                        onFeedback("Updated Portfolio SIP inflow to ${viewModel.formatShortCurrency(synced)}/mo")
                    },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("sync_cashflow_portfolio_button")
                ) {
                    Icon(Icons.Rounded.Sync, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Sync Portfolio SIP", color = EmeraldGreen, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = {
                        viewModel.applyCashflowDebtRepayment()
                        onFeedback("Synced automated debt payment to Debt Payoff accelerator")
                    },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("sync_cashflow_debt_button")
                ) {
                    Icon(Icons.Rounded.Sync, contentDescription = null, tint = AmberGold, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Sync Debt Payoff", color = AmberGold, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    Spacer(Modifier.height(14.dp))

    Text("RECURRING SCHEDULES (${transactions.size})", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    Spacer(Modifier.height(8.dp))

    if (transactions.isEmpty()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("No Recurring Entries Scheduled", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                Text("Tap Add Rule to automate monthly salary, living expenses, asset SIPs, or debt repayments.", color = TextMuted, fontSize = 12.sp)
            }
        }
    } else {
        transactions.forEach { tx ->
            val accent = if (tx.type == RecurringType.INCOME) EmeraldGreen else RoseCrimson
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .background(accent.copy(alpha = 0.16f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = tx.type.label.uppercase(),
                                        color = accent,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Text(tx.title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = "${tx.frequency.label} • Day ${tx.dayOfMonth} • ${tx.targetLink.label}",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                            if (tx.lastProcessedYearMonth != null) {
                                Text(
                                    text = "Last posted: ${tx.lastProcessedYearMonth}",
                                    color = CyanAccent,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Text(
                            text = "${if (tx.type == RecurringType.INCOME) "+" else "-"}${viewModel.formatCurrency(tx.amount)}",
                            color = accent,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Switch(
                                checked = tx.isActive,
                                onCheckedChange = { viewModel.toggleRecurringTransactionActive(context, tx.id) },
                                colors = SwitchDefaults.colors(checkedThumbColor = DeepObsidian, checkedTrackColor = EmeraldGreen)
                            )
                            Text(if (tx.isActive) "Active" else "Paused", color = TextSecondary, fontSize = 11.sp)
                        }

                        Row {
                            IconButton(
                                onClick = { editingRule = tx },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Rounded.Edit, contentDescription = "Edit ${tx.title}", tint = TextSecondary, modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = { viewModel.removeRecurringTransaction(context, tx.id) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete ${tx.title}", tint = RoseCrimson, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (logs.isNotEmpty()) {
        Spacer(Modifier.height(14.dp))
        Text("RECENT AUTOMATED POSTINGS", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.height(6.dp))
        logs.take(5).forEach { entry ->
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${entry.title} (${entry.yearMonth})", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Text(entry.summaryNote, color = TextMuted, fontSize = 10.sp)
                    }
                    Text(
                        text = viewModel.formatShortCurrency(entry.amount),
                        color = if (entry.type == RecurringType.INCOME) EmeraldGreen else CyanAccent,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }

    if (showAddDialog || editingRule != null) {
        RecurringTransactionDialog(
            initial = editingRule,
            holdings = holdings,
            debts = debts,
            onDismiss = {
                showAddDialog = false
                editingRule = null
            },
            onSave = { saved ->
                viewModel.addOrUpdateRecurringTransaction(context, saved)
                showAddDialog = false
                editingRule = null
            }
        )
    }
}
