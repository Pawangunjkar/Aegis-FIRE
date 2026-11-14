package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.DebtAccount
import com.example.models.DebtPayoffStrategy
import com.example.providers.FireViewModel
import com.example.ui.components.DualSeriesChart
import com.example.ui.components.LegendDot
import com.example.ui.components.MetricCard
import com.example.ui.components.SliderInput
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RoseCrimson
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

@Composable
fun DebtPayoffCalculatorSection(
    viewModel: FireViewModel,
    isIndian: Boolean,
    onFeedback: (String) -> Unit = {},
    onOpenProUpgrade: () -> Unit = {}
) {
    val context = LocalContext.current
    val accounts by viewModel.debtAccounts.collectAsState()
    val liabilities by viewModel.liabilities.collectAsState()
    val extraPayment by viewModel.extraDebtPayment.collectAsState()
    val strategy by viewModel.debtStrategy.collectAsState()
    val result by viewModel.debtPayoffResult.collectAsState()
    val cashflowSummary by viewModel.cashflowSummary.collectAsState()
    val proState by viewModel.proState.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<DebtAccount?>(null) }

    val maxExtra = if (isIndian) 1_50_000f else 5_000f
    val activePlan = result.activePlan

    // 2x2 Summary KPI Cards
    Row(modifier = Modifier.fillMaxWidth()) {
        MetricCard(
            title = "Debt-Free In",
            value = formatMonthsCompact(activePlan.monthsToPayoff),
            subtitle = if (result.monthsSavedVsMinimum > 0) "Saves ${result.monthsSavedVsMinimum}m vs min" else "Baseline schedule",
            color = EmeraldGreen,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(12.dp))
        MetricCard(
            title = "Interest Saved",
            value = viewModel.formatShortCurrency(result.interestSavedVsMinimum),
            subtitle = "Min-only ${viewModel.formatShortCurrency(result.minimumOnlyInterest)}",
            color = CyanAccent,
            modifier = Modifier.weight(1f)
        )
    }

    Spacer(Modifier.height(12.dp))

    Row(modifier = Modifier.fillMaxWidth()) {
        MetricCard(
            title = "Total Interest",
            value = viewModel.formatShortCurrency(activePlan.totalInterestPaid),
            subtitle = "Avg APR ${String.format(Locale.US, "%.1f", result.weightedAverageApr * 100)}%",
            color = AmberGold,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(12.dp))
        MetricCard(
            title = "Monthly Budget",
            value = viewModel.formatShortCurrency(result.totalMinimumPayment + result.extraMonthlyPayment),
            subtitle = "Min ${viewModel.formatShortCurrency(result.totalMinimumPayment)} + Extra",
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
    }

    Spacer(Modifier.height(14.dp))

    // Snowball vs Avalanche Strategy Selector
    Text("PAYOFF STRATEGY", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    Spacer(Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        DebtPayoffStrategy.entries.forEach { option ->
            val selected = strategy == option
            val planForOption = if (option == DebtPayoffStrategy.AVALANCHE) result.avalanchePlan else result.snowballPlan
            val borderColor = if (selected) EmeraldGreen else Color.White.copy(alpha = 0.1f)
            val bgColor = if (selected) EmeraldGreen.copy(alpha = 0.12f) else SurfaceCard
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(bgColor)
                    .border(1.dp, borderColor, RoundedCornerShape(14.dp))
                    .clickable { viewModel.selectDebtStrategy(option) }
                    .padding(12.dp)
                    .testTag("strategy_${option.name.lowercase()}")
            ) {
                Text(
                    text = option.label.uppercase(),
                    color = if (selected) EmeraldGreen else TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (option == DebtPayoffStrategy.AVALANCHE) "Highest APR first" else "Smallest balance first",
                    color = TextMuted,
                    fontSize = 10.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "${formatMonthsCompact(planForOption.monthsToPayoff)} • Int ${viewModel.formatShortCurrency(planForOption.totalInterestPaid)}",
                    color = if (selected) CyanAccent else TextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    if (accounts.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        val firstPayoff = result.snowballPlan.accountSummaries.firstOrNull()
        val advantageText = when {
            result.avalancheInterestAdvantage > 1.0 ->
                "Avalanche saves ${viewModel.formatCurrency(result.avalancheInterestAdvantage)} more in interest vs Snowball."
            firstPayoff != null ->
                "Snowball eliminates \"${firstPayoff.name}\" first in Month ${firstPayoff.payoffMonth}."
            else -> "Both strategies yield identical payoff timelines for your debts."
        }
        Text(advantageText, color = CyanAccent, fontSize = 11.sp)
    }

    // Debt Payoff Balance Curve
    if (activePlan.monthlyBalanceTimeline.size >= 2) {
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Remaining Balance Curve", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LegendDot(strategy.label, EmeraldGreen)
                LegendDot("Min Only", RoseCrimson)
            }
        }
        Spacer(Modifier.height(8.dp))
        val maxHorizon = maxOf(activePlan.monthlyBalanceTimeline.size, result.minimumOnlyTimeline.size)
        val paddedActive = padTimeline(activePlan.monthlyBalanceTimeline, maxHorizon)
        val paddedMin = padTimeline(result.minimumOnlyTimeline, maxHorizon)
        DualSeriesChart(
            primaryValues = paddedActive,
            secondaryValues = paddedMin,
            primaryColor = EmeraldGreen,
            secondaryColor = RoseCrimson,
            xStartLabel = "Now",
            xEndLabel = formatMonthsCompact(maxHorizon - 1),
            formatShort = viewModel::formatShortCurrency
        )
    }

    Spacer(Modifier.height(14.dp))

    // Extra Monthly Payment Accelerator Slider
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SliderInput(
                label = "Extra Monthly Repayment Accelerator",
                formattedValue = viewModel.formatShortCurrency(extraPayment),
                value = extraPayment.toFloat().coerceAtMost(maxExtra),
                min = 0f,
                max = maxExtra,
                valueColor = EmeraldGreen,
                testTag = "extra_debt_payment_slider"
            ) {
                viewModel.updateExtraDebtPayment(it.toDouble())
            }
            Text(
                text = "Freed minimum payments automatically roll over into the next debt as each account is cleared.",
                color = TextMuted,
                fontSize = 11.sp
            )

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeepObsidian.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("RECURRING CASHFLOW LINK", color = CyanAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                    Text(
                        text = "Auto-Pay ${viewModel.formatShortCurrency(cashflowSummary.monthlyDebtAutoRepayment)}/mo • Net Surplus ${viewModel.formatShortCurrency(cashflowSummary.netMonthlyCashflow)}/mo",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.applyCashflowDebtRepayment()
                        onFeedback("Applied recurring debt auto-pay to accelerator")
                    },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("debt_sync_autopay_button")
                ) {
                    Text("Use Auto-Pay", color = CyanAccent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = {
                        if (proState.isProUnlocked) {
                            viewModel.applyCashflowSurplusToDebt()
                            onFeedback("Pro Sweep applied 50% of net monthly surplus to debt payoff")
                        } else {
                            onOpenProUpgrade()
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("debt_pro_sweep_button")
                ) {
                    Text(
                        text = if (proState.isProUnlocked) "Pro: Sweep Surplus" else "Pro: Sweep (${proState.formattedPrice})",
                        color = AmberGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    Spacer(Modifier.height(16.dp))

    // Tracked Loans & Credit Cards + Payoff Order Schedule
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("REPAYMENT SCHEDULE (${accounts.size})", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (liabilities.isNotEmpty()) {
                OutlinedButton(
                    onClick = {
                        val added = viewModel.importLiabilitiesToDebtTracker(context)
                        onFeedback(
                            if (added > 0) "Imported $added liability item(s)"
                            else "All portfolio liabilities already tracked"
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f)),
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Rounded.Sync, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Sync", color = CyanAccent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            OutlinedButton(
                onClick = { showAddDialog = true },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.5f)),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("add_debt_account_button")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add Debt", color = EmeraldGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    Spacer(Modifier.height(8.dp))

    if (accounts.isEmpty()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Debt-Free Baseline", color = EmeraldGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                Text("Add a credit card, auto loan, personal loan, or mortgage above to model early repayment schedules.", color = TextMuted, fontSize = 12.sp)
            }
        }
    } else {
        activePlan.accountSummaries.forEach { summaryItem ->
            val rawAccount = accounts.find { it.id == summaryItem.id }
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .border(
                        1.dp,
                        if (summaryItem.priorityRank == 1) EmeraldGreen.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f),
                        RoundedCornerShape(14.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (summaryItem.priorityRank == 1) EmeraldGreen else CyanAccent.copy(alpha = 0.16f),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (summaryItem.priorityRank == 1) "#1 TARGET" else "#${summaryItem.priorityRank}",
                                        color = if (summaryItem.priorityRank == 1) DeepObsidian else CyanAccent,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Text(summaryItem.name, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = "${summaryItem.category} • ${String.format(Locale.US, "%.1f", summaryItem.annualApr * 100)}% APR • Min ${viewModel.formatShortCurrency(summaryItem.minimumPayment)}/mo",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = viewModel.formatCurrency(summaryItem.startingBalance),
                                color = RoseCrimson,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Paid off Mo ${summaryItem.payoffMonth}",
                                color = EmeraldGreen,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Est. Interest: ${viewModel.formatShortCurrency(summaryItem.totalInterestPaid)}",
                            color = AmberGold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Row {
                            if (rawAccount != null) {
                                IconButton(
                                    onClick = { editingAccount = rawAccount },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(Icons.Rounded.Edit, contentDescription = "Edit ${summaryItem.name}", tint = TextSecondary, modifier = Modifier.size(18.dp))
                                }
                            }
                            IconButton(
                                onClick = { viewModel.removeDebtAccount(context, summaryItem.id) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove ${summaryItem.name}", tint = RoseCrimson, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog || editingAccount != null) {
        DebtAccountDialog(
            initial = editingAccount,
            onDismiss = {
                showAddDialog = false
                editingAccount = null
            },
            onSave = { saved ->
                viewModel.addOrUpdateDebtAccount(context, saved)
                showAddDialog = false
                editingAccount = null
            }
        )
    }
}

private fun formatMonthsCompact(months: Int): String {
    if (months <= 0) return "0 mos"
    val yrs = months / 12
    val rem = months % 12
    return when {
        yrs == 0 -> "${rem}m"
        rem == 0 -> "${yrs}y"
        else -> "${yrs}y ${rem}m"
    }
}

private fun padTimeline(values: List<Double>, targetSize: Int): List<Double> {
    if (values.size >= targetSize) return values
    return values + List(targetSize - values.size) { 0.0 }
}
