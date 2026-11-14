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
import com.example.models.AssetHolding
import com.example.models.AssetType
import com.example.models.LiabilityItem
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
import com.example.ui.theme.VioletAccent
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

fun assetColor(type: AssetType): Color = when (type) {
    AssetType.EQUITY -> EmeraldGreen
    AssetType.MUTUAL_FUND -> CyanAccent
    AssetType.FIXED_DEPOSIT -> AmberGold
    AssetType.BONDS -> VioletAccent
    AssetType.GOLD_CASH -> RoseCrimson
}

@Composable
fun HoldingEditorDialog(
    initial: AssetHolding?,
    formatCurrency: (Double) -> String,
    onDismiss: () -> Unit,
    onSave: (AssetHolding) -> Unit
) {
    var selectedType by remember { mutableStateOf(initial?.assetType ?: AssetType.EQUITY) }
    var name by remember { mutableStateOf(initial?.name ?: selectedType.label) }
    var symbol by remember { mutableStateOf(initial?.symbol ?: "") }

    val initialUsesUnits = (initial?.quantity ?: 0.0) > 0.0 && (initial?.currentMarketPrice ?: 0.0) > 0.0
    var trackByUnits by remember { mutableStateOf(initial == null || initialUsesUnits) }

    var quantityText by remember {
        mutableStateOf(
            if ((initial?.quantity ?: 0.0) > 0.0) initial!!.quantity.toString() else "50"
        )
    }
    var avgBuyText by remember {
        mutableStateOf(
            if ((initial?.avgBuyPrice ?: 0.0) > 0.0) initial!!.avgBuyPrice.toString() else "100"
        )
    }
    var marketPriceText by remember {
        mutableStateOf(
            if ((initial?.currentMarketPrice ?: 0.0) > 0.0) initial!!.currentMarketPrice.toString() else "120"
        )
    }
    var investedCostText by remember {
        mutableStateOf((initial?.effectiveCostBasis ?: 10000.0).roundToInt().toString())
    }
    var currentValText by remember {
        mutableStateOf((initial?.effectiveMarketValue ?: 11000.0).roundToInt().toString())
    }
    var monthlyText by remember {
        mutableStateOf((initial?.monthlyContribution ?: 500.0).roundToInt().toString())
    }
    var returnPct by remember {
        mutableStateOf(((initial?.expectedReturn ?: selectedType.defaultReturn) * 100).toFloat())
    }
    var taxPct by remember {
        mutableStateOf(((initial?.taxRate ?: selectedType.defaultTaxRate) * 100).toFloat())
    }

    val previewQty = quantityText.toDoubleOrNull() ?: 0.0
    val previewBuy = avgBuyText.toDoubleOrNull() ?: 0.0
    val previewPrice = marketPriceText.toDoubleOrNull() ?: 0.0
    val previewCost = if (trackByUnits) previewQty * previewBuy else (investedCostText.toDoubleOrNull() ?: 0.0)
    val previewMarket = if (trackByUnits) previewQty * previewPrice else (currentValText.toDoubleOrNull() ?: 0.0)
    val previewPnL = previewMarket - previewCost

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceElevated,
        title = {
            Text(
                text = if (initial == null) "Add Asset Holding" else "Edit Asset Holding",
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("ASSET CLASS", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AssetType.entries.forEach { type ->
                        val isSel = selectedType == type
                        FilterChip(
                            selected = isSel,
                            onClick = {
                                val prevDefaultLabel = selectedType.label
                                selectedType = type
                                if (initial == null && (name == prevDefaultLabel || name.isBlank())) {
                                    name = type.label
                                }
                                if (initial == null) {
                                    returnPct = (type.defaultReturn * 100).toFloat()
                                    taxPct = (type.defaultTaxRate * 100).toFloat()
                                    trackByUnits = type == AssetType.EQUITY || type == AssetType.MUTUAL_FUND || type == AssetType.BONDS
                                }
                            },
                            label = { Text(type.label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = assetColor(type),
                                selectedLabelColor = DeepObsidian,
                                containerColor = SurfaceCard,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Asset Name") },
                        singleLine = true,
                        colors = dialogFieldColors(),
                        modifier = Modifier
                            .weight(1.7f)
                            .testTag("holding_name_input")
                    )
                    OutlinedTextField(
                        value = symbol,
                        onValueChange = { symbol = it.uppercase().take(10) },
                        label = { Text("Ticker") },
                        singleLine = true,
                        colors = dialogFieldColors(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("holding_symbol_input")
                    )
                }

                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceCard, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Track Units & Market Price", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Text("Best for stocks, mutual funds & bonds", color = TextMuted, fontSize = 10.sp)
                    }
                    Switch(
                        checked = trackByUnits,
                        onCheckedChange = { trackByUnits = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = DeepObsidian, checkedTrackColor = EmeraldGreen)
                    )
                }

                Spacer(Modifier.height(8.dp))
                if (trackByUnits) {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = sanitizeDecimal(it) },
                        label = { Text("Units / Shares Held") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = dialogFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("holding_quantity_input")
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = avgBuyText,
                            onValueChange = { avgBuyText = sanitizeDecimal(it) },
                            label = { Text("Avg Buy Price") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = dialogFieldColors(),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("holding_buy_price_input")
                        )
                        OutlinedTextField(
                            value = marketPriceText,
                            onValueChange = { marketPriceText = sanitizeDecimal(it) },
                            label = { Text("Market Price") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = dialogFieldColors(),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("holding_market_price_input")
                        )
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = investedCostText,
                            onValueChange = { investedCostText = sanitizeDecimal(it) },
                            label = { Text("Invested Cost") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = dialogFieldColors(),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = currentValText,
                            onValueChange = { currentValText = sanitizeDecimal(it) },
                            label = { Text("Market Value") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = dialogFieldColors(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceCard, RoundedCornerShape(10.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("MARKET VALUE", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(formatCurrency(previewMarket), color = TextPrimary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("UNREALIZED P&L", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${if (previewPnL >= 0) "+" else ""}${formatCurrency(previewPnL)}",
                            color = if (previewPnL >= 0) EmeraldGreen else RoseCrimson,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = monthlyText,
                    onValueChange = { monthlyText = sanitizeDecimal(it) },
                    label = { Text("Monthly Contribution / SIP") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = dialogFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                SliderInput(
                    label = "Expected Annual Return",
                    formattedValue = "${String.format(Locale.US, "%.1f", returnPct)}%",
                    value = returnPct,
                    min = 1f,
                    max = 22f,
                    valueColor = EmeraldGreen
                ) { returnPct = it }
                SliderInput(
                    label = "Tax Rate",
                    formattedValue = "${taxPct.roundToInt()}%",
                    value = taxPct,
                    min = 0f,
                    max = 40f,
                    valueColor = AmberGold
                ) { taxPct = it }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val sip = monthlyText.toDoubleOrNull() ?: 0.0
                    val holding = if (trackByUnits) {
                        val qty = (quantityText.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
                        val buy = (avgBuyText.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
                        val mkt = (marketPriceText.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
                        AssetHolding(
                            id = initial?.id ?: UUID.randomUUID().toString(),
                            name = name.trim().ifEmpty { selectedType.label },
                            assetType = selectedType,
                            currentValue = qty * mkt,
                            monthlyContribution = sip,
                            expectedReturn = (returnPct / 100.0).coerceIn(0.0, 0.30),
                            taxRate = (taxPct / 100.0).coerceIn(0.0, 0.50),
                            symbol = symbol.trim(),
                            quantity = qty,
                            avgBuyPrice = buy,
                            currentMarketPrice = mkt,
                            investedCost = qty * buy
                        )
                    } else {
                        val cost = (investedCostText.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
                        val cur = (currentValText.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
                        AssetHolding(
                            id = initial?.id ?: UUID.randomUUID().toString(),
                            name = name.trim().ifEmpty { selectedType.label },
                            assetType = selectedType,
                            currentValue = cur,
                            monthlyContribution = sip,
                            expectedReturn = (returnPct / 100.0).coerceIn(0.0, 0.30),
                            taxRate = (taxPct / 100.0).coerceIn(0.0, 0.50),
                            symbol = symbol.trim(),
                            quantity = 0.0,
                            avgBuyPrice = 0.0,
                            currentMarketPrice = 0.0,
                            investedCost = cost
                        )
                    }
                    onSave(holding)
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                modifier = Modifier.testTag("save_holding_confirm_button")
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
fun QuickPriceUpdateDialog(
    holding: AssetHolding,
    onDismiss: () -> Unit,
    onUpdate: (Double) -> Unit
) {
    val isUnitTracked = holding.quantity > 0.0 && holding.currentMarketPrice > 0.0
    var inputText by remember {
        mutableStateOf(
            if (isUnitTracked) holding.currentMarketPrice.toString()
            else holding.effectiveMarketValue.roundToInt().toString()
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceElevated,
        title = {
            Text("Update Market ${if (isUnitTracked) "Price" else "Value"}", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(holding.name, color = CyanAccent, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                if (isUnitTracked) {
                    Text("${holding.quantity} units held", color = TextMuted, fontSize = 12.sp)
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = sanitizeDecimal(it) },
                    label = { Text(if (isUnitTracked) "Current Market Price / Unit" else "Current Market Value") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = dialogFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quick_price_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val value = inputText.toDoubleOrNull()
                    if (value != null) onUpdate(value)
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                modifier = Modifier.testTag("quick_price_save_button")
            ) {
                Text("Update", color = DeepObsidian, fontWeight = FontWeight.Bold)
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
fun LiabilityEditorDialog(
    onDismiss: () -> Unit,
    onSave: (LiabilityItem) -> Unit
) {
    var name by remember { mutableStateOf("Home Mortgage") }
    var category by remember { mutableStateOf("Mortgage") }
    var balanceText by remember { mutableStateOf("25000") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceElevated,
        title = { Text("Add Liability / Debt", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Mortgage", "Auto Loan", "Credit Card", "Personal Loan").forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = {
                                category = cat
                                name = cat
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
                    label = { Text("Liability Name") },
                    singleLine = true,
                    colors = dialogFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { balanceText = sanitizeDecimal(it) },
                    label = { Text("Outstanding Balance") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = dialogFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("liability_balance_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val bal = (balanceText.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
                    onSave(
                        LiabilityItem(
                            id = UUID.randomUUID().toString(),
                            name = name.trim().ifEmpty { category },
                            category = category,
                            balance = bal
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = RoseCrimson),
                modifier = Modifier.testTag("save_liability_confirm_button")
            ) {
                Text("Add Debt", color = DeepObsidian, fontWeight = FontWeight.Bold)
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
private fun dialogFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedBorderColor = EmeraldGreen,
    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
    focusedLabelColor = EmeraldGreen,
    unfocusedLabelColor = TextSecondary
)

private fun sanitizeDecimal(raw: String): String {
    val filtered = raw.filter { it.isDigit() || it == '.' }
    val firstDot = filtered.indexOf('.')
    return if (firstDot >= 0) {
        filtered.substring(0, firstDot + 1) + filtered.substring(firstDot + 1).replace(".", "")
    } else {
        filtered
    }
}
