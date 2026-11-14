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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.DebtAccount
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RoseCrimson
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

@Composable
fun DebtAccountDialog(
    initial: DebtAccount?,
    onDismiss: () -> Unit,
    onSave: (DebtAccount) -> Unit
) {
    val defaultAprByCategory = mapOf(
        "Credit Card" to "21.9",
        "Personal Loan" to "11.5",
        "Auto Loan" to "6.8",
        "Student Loan" to "5.5",
        "Mortgage" to "6.5"
    )
    var category by remember { mutableStateOf(initial?.category ?: "Credit Card") }
    var name by remember { mutableStateOf(initial?.name ?: "Credit Card") }
    var balanceText by remember { mutableStateOf((initial?.balance ?: 5000.0).roundToInt().toString()) }
    var aprText by remember {
        mutableStateOf(
            if (initial != null) String.format(Locale.US, "%.1f", initial.annualApr * 100.0)
            else defaultAprByCategory[category] ?: "18.0"
        )
    }
    var minPaymentText by remember { mutableStateOf((initial?.minimumPayment ?: 150.0).roundToInt().toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceElevated,
        title = {
            Text(if (initial == null) "Add Loan / Credit Card" else "Edit Debt Account", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    defaultAprByCategory.keys.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = {
                                val prevCat = category
                                category = cat
                                if (initial == null && (name == prevCat || name.isBlank())) {
                                    name = cat
                                }
                                if (initial == null) {
                                    aprText = defaultAprByCategory[cat] ?: aprText
                                }
                            },
                            label = { Text(cat, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RoseCrimson,
                                selectedLabelColor = DeepObsidian,
                                containerColor = SurfaceCard,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account Name") },
                    singleLine = true,
                    colors = debtFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("debt_name_input")
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { balanceText = sanitizeDecimalInput(it) },
                    label = { Text("Current Balance") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = debtFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("debt_balance_input")
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = aprText,
                        onValueChange = { aprText = sanitizeDecimalInput(it) },
                        label = { Text("APR (%)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = debtFieldColors(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("debt_apr_input")
                    )
                    OutlinedTextField(
                        value = minPaymentText,
                        onValueChange = { minPaymentText = sanitizeDecimalInput(it) },
                        label = { Text("Min Monthly") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = debtFieldColors(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("debt_min_payment_input")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val bal = (balanceText.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
                    val apr = ((aprText.toDoubleOrNull() ?: 0.0) / 100.0).coerceIn(0.0, 0.99)
                    val minPay = (minPaymentText.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
                    onSave(
                        DebtAccount(
                            id = initial?.id ?: UUID.randomUUID().toString(),
                            name = name.trim().ifEmpty { category },
                            category = category,
                            balance = bal,
                            annualApr = apr,
                            minimumPayment = minPay
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                modifier = Modifier.testTag("save_debt_account_button")
            ) {
                Text("Save", color = DeepObsidian, fontWeight = FontWeight.Bold)
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
private fun debtFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedBorderColor = EmeraldGreen,
    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
    focusedLabelColor = EmeraldGreen,
    unfocusedLabelColor = TextSecondary
)

private fun sanitizeDecimalInput(raw: String): String {
    val filtered = raw.filter { it.isDigit() || it == '.' }
    val firstDot = filtered.indexOf('.')
    return if (firstDot >= 0) {
        filtered.substring(0, firstDot + 1) + filtered.substring(firstDot + 1).replace(".", "")
    } else {
        filtered
    }
}
