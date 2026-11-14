package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.BudgetCategoryEntry
import com.example.models.RecurringTransaction
import com.example.models.RecurringType
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
import java.util.UUID
import kotlin.math.roundToInt

@Composable
fun BudgetCategoryDialog(
    initial: BudgetCategoryEntry?,
    recurringTransactions: List<RecurringTransaction>,
    formatCurrency: (Double) -> String,
    onDismiss: () -> Unit,
    onSave: (BudgetCategoryEntry) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var limitText by remember { mutableStateOf(if (initial != null) initial.monthlyLimit.roundToInt().toString() else "600") }
    var manualSpentText by remember { mutableStateOf(if (initial != null) initial.manualSpending.roundToInt().toString() else "0") }
    var savingTargetText by remember { mutableStateOf(if (initial != null) initial.savingTarget.roundToInt().toString() else "100") }
    var isEssential by remember { mutableStateOf(initial?.isEssential ?: true) }

    val quickPresets = listOf(
        "Housing",
        "Food & Dining",
        "Utilities",
        "Transport",
        "Healthcare",
        "Subscription",
        "Debt Payment",
        "Investment SIP",
        "Other"
    )

    val matchingRecurring = recurringTransactions
        .filter {
            it.isActive &&
                it.type == RecurringType.EXPENSE &&
                it.category.trim().equals(name.trim(), ignoreCase = true)
        }
        .sumOf { it.amount / it.frequency.monthsInterval.toDouble() }

    val parsedLimit = limitText.toDoubleOrNull() ?: 0.0
    val parsedManual = manualSpentText.toDoubleOrNull() ?: 0.0
    val parsedTarget = savingTargetText.toDoubleOrNull() ?: 0.0
    val projectedSavings = (parsedLimit - (matchingRecurring + parsedManual)).coerceAtLeast(0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceElevated,
        title = {
            Text(
                text = if (initial == null) "Add Budget Category" else "Edit Budget Category",
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("QUICK CATEGORY PRESETS", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickPresets.forEach { preset ->
                        FilterChip(
                            selected = name.trim().equals(preset, ignoreCase = true),
                            onClick = { name = preset },
                            label = { Text(preset, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AmberGold,
                                selectedLabelColor = DeepObsidian,
                                containerColor = SurfaceCard,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name") },
                    singleLine = true,
                    colors = budgetFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("budget_category_name_input")
                )

                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = limitText,
                        onValueChange = { limitText = sanitizeBudgetDecimal(it) },
                        label = { Text("Monthly Limit") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = budgetFieldColors(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("budget_limit_input")
                    )
                    OutlinedTextField(
                        value = manualSpentText,
                        onValueChange = { manualSpentText = sanitizeBudgetDecimal(it) },
                        label = { Text("Manual Spend") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = budgetFieldColors(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("budget_spent_input")
                    )
                }

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = savingTargetText,
                    onValueChange = { savingTargetText = sanitizeBudgetDecimal(it) },
                    label = { Text("Category Saving Target (Under-Spend Goal)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = budgetFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("budget_saving_target_input")
                )

                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Essential Expense", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text("Tag as fixed/core need vs discretionary", color = TextMuted, fontSize = 11.sp)
                    }
                    Switch(
                        checked = isEssential,
                        onCheckedChange = { isEssential = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = DeepObsidian, checkedTrackColor = EmeraldGreen)
                    )
                }

                Spacer(Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceCard, RoundedCornerShape(10.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Auto-linked Recurring:", color = TextSecondary, fontSize = 11.sp)
                        Text(formatCurrency(matchingRecurring), color = CyanAccent, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    Spacer(Modifier.height(3.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Projected Category Savings:", color = TextSecondary, fontSize = 11.sp)
                        val meetsTarget = projectedSavings >= parsedTarget
                        Text(
                            text = formatCurrency(projectedSavings),
                            color = if (meetsTarget) EmeraldGreen else if (projectedSavings > 0.0) AmberGold else RoseCrimson,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanName = name.trim().ifEmpty { "Custom Category" }
                    onSave(
                        BudgetCategoryEntry(
                            id = initial?.id ?: UUID.randomUUID().toString(),
                            name = cleanName,
                            monthlyLimit = (limitText.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0),
                            manualSpending = (manualSpentText.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0),
                            savingTarget = (savingTargetText.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0),
                            isEssential = isEssential
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                modifier = Modifier.testTag("save_budget_category_button")
            ) {
                Text("Save Category", color = DeepObsidian, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun budgetFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedBorderColor = EmeraldGreen,
    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
    focusedLabelColor = EmeraldGreen,
    unfocusedLabelColor = TextSecondary
)

private fun sanitizeBudgetDecimal(raw: String): String {
    val filtered = raw.filter { it.isDigit() || it == '.' }
    val firstDot = filtered.indexOf('.')
    return if (firstDot >= 0) {
        filtered.substring(0, firstDot + 1) + filtered.substring(firstDot + 1).replace(".", "")
    } else {
        filtered
    }
}
