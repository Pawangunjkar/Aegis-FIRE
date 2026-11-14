package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.AssetHolding
import com.example.models.DebtAccount
import com.example.models.RecurringFrequency
import com.example.models.RecurringTargetLink
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
fun RecurringTransactionDialog(
    initial: RecurringTransaction?,
    holdings: List<AssetHolding>,
    debts: List<DebtAccount>,
    onDismiss: () -> Unit,
    onSave: (RecurringTransaction) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var amountText by remember { mutableStateOf(if (initial != null) initial.amount.roundToInt().toString() else "500") }
    var type by remember { mutableStateOf(initial?.type ?: RecurringType.INCOME) }
    var frequency by remember { mutableStateOf(initial?.frequency ?: RecurringFrequency.MONTHLY) }
    var targetLink by remember { mutableStateOf(initial?.targetLink ?: RecurringTargetLink.GENERAL) }
    var linkedEntityId by remember { mutableStateOf(initial?.linkedEntityId) }
    var dayText by remember { mutableStateOf((initial?.dayOfMonth ?: 1).toString()) }
    var category by remember { mutableStateOf(initial?.category ?: "Salary") }
    var autoExecute by remember { mutableStateOf(initial?.autoExecuteOnDue ?: true) }

    val categories = if (type == RecurringType.INCOME) {
        listOf("Salary", "Freelance", "Dividends", "Rental Income", "Other")
    } else {
        listOf("Investment SIP", "Debt Payment", "Housing", "Food & Dining", "Utilities", "Transport", "Subscription", "Other")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceElevated,
        title = {
            Text(
                text = if (initial == null) "Schedule Recurring Entry" else "Edit Recurring Rule",
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RecurringType.entries.forEach { entry ->
                        val selected = type == entry
                        FilterChip(
                            selected = selected,
                            onClick = {
                                type = entry
                                if (initial == null) {
                                    category = if (entry == RecurringType.INCOME) "Salary" else "Investment SIP"
                                    if (entry == RecurringType.INCOME && targetLink == RecurringTargetLink.DEBT_PAYMENT) {
                                        targetLink = RecurringTargetLink.GENERAL
                                        linkedEntityId = null
                                    }
                                }
                            },
                            label = { Text(entry.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (entry == RecurringType.INCOME) EmeraldGreen else RoseCrimson,
                                selectedLabelColor = DeepObsidian,
                                containerColor = SurfaceCard,
                                labelColor = TextSecondary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("recurring_type_${entry.name.lowercase()}")
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Entry Title (e.g. Monthly Bonus)") },
                    singleLine = true,
                    colors = cashflowFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recurring_title_input")
                )

                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = sanitizeCashflowDecimal(it) },
                        label = { Text("Amount") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = cashflowFieldColors(),
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("recurring_amount_input")
                    )
                    OutlinedTextField(
                        value = dayText,
                        onValueChange = { dayText = it.filter { ch -> ch.isDigit() }.take(2) },
                        label = { Text("Day (1-28)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = cashflowFieldColors(),
                        modifier = Modifier
                            .weight(0.8f)
                            .testTag("recurring_day_input")
                    )
                }

                Spacer(Modifier.height(10.dp))
                Text("FREQUENCY", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RecurringFrequency.entries.forEach { freq ->
                        FilterChip(
                            selected = frequency == freq,
                            onClick = { frequency = freq },
                            label = { Text(freq.label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanAccent,
                                selectedLabelColor = DeepObsidian,
                                containerColor = SurfaceCard,
                                labelColor = TextSecondary
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text("CATEGORY", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AmberGold,
                                selectedLabelColor = DeepObsidian,
                                containerColor = SurfaceCard,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text("CALCULATOR INTEGRATION LINK", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.height(4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    RecurringTargetLink.entries.forEach { link ->
                        FilterChip(
                            selected = targetLink == link,
                            onClick = {
                                targetLink = link
                                linkedEntityId = null
                            },
                            label = { Text(link.label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldGreen,
                                selectedLabelColor = DeepObsidian,
                                containerColor = SurfaceCard,
                                labelColor = TextSecondary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("recurring_link_${link.name.lowercase()}")
                        )
                    }
                }

                if (targetLink == RecurringTargetLink.PORTFOLIO_SIP && holdings.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("TARGET ASSET (OPTIONAL)", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        holdings.forEach { holding ->
                            FilterChip(
                                selected = linkedEntityId == holding.id,
                                onClick = { linkedEntityId = if (linkedEntityId == holding.id) null else holding.id },
                                label = { Text(holding.name, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyanAccent,
                                    selectedLabelColor = DeepObsidian,
                                    containerColor = SurfaceCard,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }
                }

                if (targetLink == RecurringTargetLink.DEBT_PAYMENT && debts.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("TARGET DEBT (OPTIONAL)", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        debts.forEach { debt ->
                            FilterChip(
                                selected = linkedEntityId == debt.id,
                                onClick = { linkedEntityId = if (linkedEntityId == debt.id) null else debt.id },
                                label = { Text(debt.name, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RoseCrimson,
                                    selectedLabelColor = DeepObsidian,
                                    containerColor = SurfaceCard,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-post on due day", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text("Applies automatically once per cycle", color = TextMuted, fontSize = 11.sp)
                    }
                    Switch(
                        checked = autoExecute,
                        onCheckedChange = { autoExecute = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = DeepObsidian, checkedTrackColor = EmeraldGreen)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = (amountText.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
                    val safeDay = (dayText.toIntOrNull() ?: 1).coerceIn(1, 28)
                    val finalTitle = title.trim().ifEmpty { "${type.label}: $category" }
                    onSave(
                        RecurringTransaction(
                            id = initial?.id ?: UUID.randomUUID().toString(),
                            title = finalTitle,
                            amount = amt,
                            type = type,
                            category = category,
                            dayOfMonth = safeDay,
                            frequency = frequency,
                            targetLink = targetLink,
                            linkedEntityId = linkedEntityId,
                            isActive = initial?.isActive ?: true,
                            autoExecuteOnDue = autoExecute,
                            lastProcessedYearMonth = initial?.lastProcessedYearMonth
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                modifier = Modifier.testTag("save_recurring_rule_button")
            ) {
                Text("Save Rule", color = DeepObsidian, fontWeight = FontWeight.Bold)
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
private fun cashflowFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedBorderColor = EmeraldGreen,
    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
    focusedLabelColor = EmeraldGreen,
    unfocusedLabelColor = TextSecondary
)

private fun sanitizeCashflowDecimal(raw: String): String {
    val filtered = raw.filter { it.isDigit() || it == '.' }
    val firstDot = filtered.indexOf('.')
    return if (firstDot >= 0) {
        filtered.substring(0, firstDot + 1) + filtered.substring(firstDot + 1).replace(".", "")
    } else {
        filtered
    }
}
