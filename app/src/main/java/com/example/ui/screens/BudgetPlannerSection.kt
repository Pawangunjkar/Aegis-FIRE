package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.BudgetCategoryEntry
import com.example.models.BudgetCategoryStatus
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
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun BudgetPlannerSection(
    viewModel: FireViewModel,
    isIndian: Boolean,
    onFeedback: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val summary by viewModel.budgetSummary.collectAsState()
    val useRecurringIncome by viewModel.useRecurringBudgetIncome.collectAsState()
    val customIncome by viewModel.customBudgetIncome.collectAsState()
    val recurringTransactions by viewModel.recurringTransactions.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<BudgetCategoryEntry?>(null) }
    var customIncomeText by remember(customIncome) { mutableStateOf(customIncome.roundToInt().toString()) }

    val quickStep = if (isIndian) 1_000.0 else 50.0

    // 2x2 KPI Overview
    Row(modifier = Modifier.fillMaxWidth()) {
        MetricCard(
            title = "Monthly Budgeted",
            value = viewModel.formatShortCurrency(summary.totalBudgetedLimit),
            subtitle = "Income ${viewModel.formatShortCurrency(summary.monthlyIncome)}",
            color = CyanAccent,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(12.dp))
        MetricCard(
            title = "Actual + Recurring",
            value = viewModel.formatShortCurrency(summary.totalEffectiveSpending),
            subtitle = "Auto ${viewModel.formatShortCurrency(summary.totalRecurringExpenses)} • Var ${viewModel.formatShortCurrency(summary.totalManualSpending)}",
            color = if (summary.overBudgetCount > 0) RoseCrimson else AmberGold,
            modifier = Modifier.weight(1f)
        )
    }

    Spacer(Modifier.height(12.dp))

    Row(modifier = Modifier.fillMaxWidth()) {
        val targetMet = summary.totalProjectedCategorySavings >= summary.totalCategorySavingTargets
        MetricCard(
            title = "Category Savings",
            value = viewModel.formatShortCurrency(summary.totalProjectedCategorySavings),
            subtitle = "Target ${viewModel.formatShortCurrency(summary.totalCategorySavingTargets)}",
            color = if (targetMet) EmeraldGreen else AmberGold,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(12.dp))
        MetricCard(
            title = "Unallocated Surplus",
            value = viewModel.formatShortCurrency(summary.unallocatedIncome),
            subtitle = "Savings Rate ${String.format(Locale.US, "%.1f", summary.savingsRatePercent)}%",
            color = if (summary.unallocatedIncome >= 0.0) EmeraldGreen else RoseCrimson,
            modifier = Modifier.weight(1f)
        )
    }

    Spacer(Modifier.height(12.dp))

    // Income Integration & Allocation Health Card
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("INCOME & RECURRING SYNC", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (useRecurringIncome) {
                            "Auto-pulled from active Cashflow income rules"
                        } else {
                            "Using manual monthly budget income"
                        },
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Switch(
                    checked = useRecurringIncome,
                    onCheckedChange = { checked ->
                        viewModel.updateBudgetIncomeSettings(
                            context = context,
                            useRecurringIncome = checked,
                            customMonthlyIncome = customIncome
                        )
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = DeepObsidian, checkedTrackColor = EmeraldGreen),
                    modifier = Modifier.testTag("budget_income_mode_switch")
                )
            }

            if (!useRecurringIncome) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = customIncomeText,
                    onValueChange = { raw ->
                        val clean = raw.filter { it.isDigit() || it == '.' }
                        customIncomeText = clean
                        val parsed = clean.toDoubleOrNull()
                        if (parsed != null) {
                            viewModel.updateBudgetIncomeSettings(context, useRecurringIncome = false, customMonthlyIncome = parsed)
                        }
                    },
                    label = { Text("Custom Monthly Take-Home Income") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedLabelColor = CyanAccent,
                        unfocusedLabelColor = TextSecondary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("budget_custom_income_input")
                )
            }

            Spacer(Modifier.height(10.dp))
            BudgetAllocationStackedBar(
                income = summary.monthlyIncome,
                recurring = summary.totalRecurringExpenses,
                manual = summary.totalManualSpending
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Recurring: ${viewModel.formatShortCurrency(summary.totalRecurringExpenses)}", color = CyanAccent, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Text("Variable: ${viewModel.formatShortCurrency(summary.totalManualSpending)}", color = AmberGold, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Text(
                    text = if (summary.overBudgetCount == 0) "All On Track" else "${summary.overBudgetCount} Over Limit",
                    color = if (summary.overBudgetCount == 0) EmeraldGreen else RoseCrimson,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("add_budget_category_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, tint = DeepObsidian, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Category", color = DeepObsidian, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = {
                        val synced = viewModel.syncBudgetExpensesToFire()
                        onFeedback("Synced living expenses (${viewModel.formatShortCurrency(synced)}/mo) to FIRE Planner")
                    },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("sync_budget_to_fire_button")
                ) {
                    Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Sync to FIRE", color = CyanAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }

    Spacer(Modifier.height(14.dp))
    Text("CATEGORY BUDGETS & SAVING TARGETS (${summary.categories.size})", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    Spacer(Modifier.height(8.dp))

    if (summary.categories.isEmpty()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("No Budget Categories Defined", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                Text("Tap Add Category to organize monthly spending caps, track variable expenses, and set saving targets.", color = TextMuted, fontSize = 12.sp)
            }
        }
    } else {
        summary.categories.forEach { status ->
            BudgetCategoryCard(
                status = status,
                quickStep = quickStep,
                formatCurrency = viewModel::formatCurrency,
                formatShort = viewModel::formatShortCurrency,
                onAdjustManual = { delta ->
                    viewModel.adjustBudgetManualSpending(context, status.entry.id, delta)
                },
                onEdit = { editingCategory = status.entry },
                onDelete = { viewModel.removeBudgetCategory(context, status.entry.id) }
            )
        }
    }

    if (showAddDialog || editingCategory != null) {
        BudgetCategoryDialog(
            initial = editingCategory,
            recurringTransactions = recurringTransactions,
            formatCurrency = viewModel::formatCurrency,
            onDismiss = {
                showAddDialog = false
                editingCategory = null
            },
            onSave = { saved ->
                viewModel.addOrUpdateBudgetCategory(context, saved)
                showAddDialog = false
                editingCategory = null
            }
        )
    }
}

@Composable
private fun BudgetAllocationStackedBar(
    income: Double,
    recurring: Double,
    manual: Double
) {
    val denom = maxOf(income, recurring + manual, 1.0)
    val recurringFraction = (recurring / denom).toFloat().coerceIn(0f, 1f)
    val manualFraction = (manual / denom).toFloat().coerceIn(0f, 1f - recurringFraction)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White.copy(alpha = 0.08f))
    ) {
        if (recurringFraction > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(recurringFraction.coerceAtLeast(0.001f))
                    .background(CyanAccent)
            )
        }
        if (manualFraction > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(manualFraction.coerceAtLeast(0.001f))
                    .background(AmberGold)
            )
        }
        val remaining = (1f - recurringFraction - manualFraction).coerceAtLeast(0f)
        if (remaining > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(remaining.coerceAtLeast(0.001f))
                    .background(EmeraldGreen.copy(alpha = 0.25f))
            )
        }
    }
}

@Composable
private fun BudgetCategoryCard(
    status: BudgetCategoryStatus,
    quickStep: Double,
    formatCurrency: (Double) -> String,
    formatShort: (Double) -> String,
    onAdjustManual: (Double) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val entry = status.entry
    val isVirtual = entry.id == "unmapped_recurring_other"
    val spendColor = when {
        status.isOverBudget -> RoseCrimson
        status.utilizationRatio >= 0.85 -> AmberGold
        else -> EmeraldGreen
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(
                1.dp,
                if (status.isOverBudget) RoseCrimson.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.08f),
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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(entry.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Box(
                            modifier = Modifier
                                .background(
                                    (if (entry.isEssential) CyanAccent else AmberGold).copy(alpha = 0.14f),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (entry.isEssential) "ESSENTIAL" else "FLEX",
                                color = if (entry.isEssential) CyanAccent else AmberGold,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    if (status.matchingRecurringCount > 0) {
                        Spacer(Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Rounded.Repeat, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(12.dp))
                            Text(
                                text = "${status.matchingRecurringCount} recurring (${formatShort(status.recurringCommitted)}/mo) + ${formatShort(entry.manualSpending)} var",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    } else {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Variable spend: ${formatShort(entry.manualSpending)}",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${formatCurrency(status.effectiveSpent)} / ${formatCurrency(entry.monthlyLimit)}",
                        color = spendColor,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = if (status.remainingBudget >= 0.0) {
                            "${formatShort(status.remainingBudget)} left"
                        } else {
                            "Over by ${formatShort(-status.remainingBudget)}"
                        },
                        color = if (status.remainingBudget >= 0.0) TextSecondary else RoseCrimson,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Spend Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.08f))
            ) {
                val fill = status.utilizationRatio.toFloat().coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fill)
                        .fillMaxHeight()
                        .background(spendColor)
                )
            }

            if (entry.savingTarget > 0.0) {
                Spacer(Modifier.height(8.dp))
                val targetAchieved = status.projectedCategorySavings >= entry.savingTarget
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(
                            imageVector = if (targetAchieved) Icons.Rounded.CheckCircle else if (status.isOverBudget) Icons.Rounded.WarningAmber else Icons.Rounded.Savings,
                            contentDescription = null,
                            tint = if (targetAchieved) EmeraldGreen else if (status.isOverBudget) RoseCrimson else AmberGold,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Saving Target: ${formatShort(status.projectedCategorySavings)} / ${formatShort(entry.savingTarget)}",
                            color = if (targetAchieved) EmeraldGreen else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = "${(status.savingTargetProgress * 100).roundToInt()}%",
                        color = if (targetAchieved) EmeraldGreen else AmberGold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (!isVirtual) {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = { onAdjustManual(-quickStep) },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(Icons.Rounded.Remove, contentDescription = "Decrease ${entry.name} manual spend", tint = TextSecondary, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(2.dp))
                            Text(formatShort(quickStep), color = TextSecondary, fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = { onAdjustManual(quickStep) },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.4f)),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "Increase ${entry.name} manual spend", tint = AmberGold, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(2.dp))
                            Text(formatShort(quickStep), color = AmberGold, fontSize = 11.sp)
                        }
                    }

                    Row {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Edit ${entry.name}", tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete ${entry.name}", tint = RoseCrimson, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}
