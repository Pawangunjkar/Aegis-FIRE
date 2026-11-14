package com.example.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.TaxRegime
import com.example.providers.FireViewModel
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

@Composable
fun TaxCalculatorSection(
    viewModel: FireViewModel,
    onFeedback: (String) -> Unit = {}
) {
    val params by viewModel.taxParams.collectAsState()
    val result by viewModel.taxResult.collectAsState()

    val isIndianTax = params.regime.isIndian
    val fmtShort: (Double) -> String = { amount -> viewModel.formatShortCurrency(amount, forceIndian = isIndianTax) }

    val maxIncome = if (isIndianTax) 6_000_000f else 400_000f
    val maxRetirement = if (isIndianTax) 150_000f else 30_500f
    val maxExtraDed = if (isIndianTax) 150_000f else 12_000f
    val maxCapGains = if (isIndianTax) 2_000_000f else 150_000f

    // 2x2 Summary KPI Cards
    Row(modifier = Modifier.fillMaxWidth()) {
        MetricCard(
            title = "Est. Annual Tax",
            value = fmtShort(result.totalTaxLiability),
            subtitle = "Marginal ${String.format(Locale.US, "%.0f", result.marginalBracketRate * 100)}% bracket",
            color = RoseCrimson,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(12.dp))
        MetricCard(
            title = "Effective Tax Rate",
            value = "${String.format(Locale.US, "%.1f", result.effectiveTaxRate * 100)}%",
            subtitle = "On ${fmtShort(params.annualIncome + params.realizedCapitalGains)} total",
            color = AmberGold,
            modifier = Modifier.weight(1f)
        )
    }

    Spacer(Modifier.height(12.dp))

    Row(modifier = Modifier.fillMaxWidth()) {
        MetricCard(
            title = "Net After-Tax Income",
            value = fmtShort(result.netTakeHomeIncome),
            subtitle = "Taxable base ${fmtShort(result.taxableIncome)}",
            color = EmeraldGreen,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(12.dp))
        MetricCard(
            title = "Deduction Tax Saved",
            value = fmtShort(result.taxSavedFromDeductions),
            subtitle = "Deductions ${fmtShort(result.totalDeductions)}",
            color = CyanAccent,
            modifier = Modifier.weight(1f)
        )
    }

    Spacer(Modifier.height(14.dp))

    // Tax System & Filing Regime Selector
    Text("TAX REGIME & FILING BRACKETS", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    Spacer(Modifier.height(8.dp))
    val regimeRows = TaxRegime.entries.chunked(2)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        regimeRows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { regimeOption ->
                    val selected = params.regime == regimeOption
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.selectTaxRegime(regimeOption) },
                        label = {
                            Text(
                                text = regimeOption.label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanAccent,
                            selectedLabelColor = DeepObsidian,
                            containerColor = SurfaceCard,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = Color.White.copy(alpha = 0.1f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tax_regime_${regimeOption.name.lowercase()}")
                    )
                }
            }
        }
    }

    Spacer(Modifier.height(12.dp))

    // Income & Investment Deductions Inputs
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SliderInput(
                label = "Gross Annual Income",
                formattedValue = fmtShort(params.annualIncome),
                value = params.annualIncome.toFloat().coerceAtMost(maxIncome),
                min = 0f,
                max = maxIncome,
                valueColor = TextPrimary,
                testTag = "tax_income_slider"
            ) {
                viewModel.updateTaxParams(params.copy(annualIncome = it.toDouble()))
            }

            if (params.regime != TaxRegime.IN_NEW) {
                SliderInput(
                    label = if (isIndianTax) "Sec 80C (ELSS / PPF / EPF)" else "401(k) / Traditional IRA",
                    formattedValue = fmtShort(params.retirementDeduction),
                    value = params.retirementDeduction.toFloat().coerceAtMost(maxRetirement),
                    min = 0f,
                    max = maxRetirement,
                    valueColor = EmeraldGreen,
                    testTag = "tax_retirement_deduction_slider"
                ) {
                    viewModel.updateTaxParams(params.copy(retirementDeduction = it.toDouble()))
                }

                SliderInput(
                    label = if (isIndianTax) "NPS 80CCD(1B) & Sec 80D" else "HSA / Itemized Investment Ded.",
                    formattedValue = fmtShort(params.additionalInvestmentDeduction),
                    value = params.additionalInvestmentDeduction.toFloat().coerceAtMost(maxExtraDed),
                    min = 0f,
                    max = maxExtraDed,
                    valueColor = CyanAccent
                ) {
                    viewModel.updateTaxParams(params.copy(additionalInvestmentDeduction = it.toDouble()))
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceElevated, RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.Savings, contentDescription = null, tint = AmberGold, modifier = Modifier.size(16.dp))
                    Text(
                        text = "India New Regime applies lower tax slabs with ₹75,000 standard deduction instead of 80C/80D.",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
                Spacer(Modifier.height(6.dp))
            }

            SliderInput(
                label = "Realized Investment Capital Gains",
                formattedValue = fmtShort(params.realizedCapitalGains),
                value = params.realizedCapitalGains.toFloat().coerceAtMost(maxCapGains),
                min = 0f,
                max = maxCapGains,
                valueColor = AmberGold
            ) {
                viewModel.updateTaxParams(params.copy(realizedCapitalGains = it.toDouble()))
            }

            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Apply Standard Deduction", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text("Reduces ordinary taxable base (${fmtShort(result.standardDeductionUsed)})", color = TextMuted, fontSize = 11.sp)
                }
                Switch(
                    checked = params.applyStandardDeduction,
                    onCheckedChange = { viewModel.updateTaxParams(params.copy(applyStandardDeduction = it)) },
                    colors = SwitchDefaults.colors(checkedThumbColor = DeepObsidian, checkedTrackColor = EmeraldGreen)
                )
            }

            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Long-Term Capital Gains (LTCG)", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text(
                        text = if (params.longTermGains) "Preferential LTCG brackets applied" else "Short-term gains taxed at higher rates",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = params.longTermGains,
                    onCheckedChange = { viewModel.updateTaxParams(params.copy(longTermGains = it)) },
                    colors = SwitchDefaults.colors(checkedThumbColor = DeepObsidian, checkedTrackColor = CyanAccent)
                )
            }
        }
    }

    Spacer(Modifier.height(14.dp))

    // Progressive Bracket & Tax Composition Breakdown
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("PROGRESSIVE BRACKET BREAKDOWN", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(10.dp))

            if (result.bracketBreakdown.isEmpty()) {
                Text("Taxable income is $0 after deductions — no ordinary income tax due.", color = EmeraldGreen, fontSize = 12.sp)
            } else {
                val maxSliceTax = result.bracketBreakdown.maxOfOrNull { it.taxAmount }?.coerceAtLeast(1.0) ?: 1.0
                result.bracketBreakdown.forEach { slice ->
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(slice.bracketLabel, color = TextPrimary, fontSize = 12.sp)
                            Text(
                                text = "${fmtShort(slice.taxAmount)} on ${fmtShort(slice.taxableAmount)}",
                                color = CyanAccent,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        val fillRatio = (slice.taxAmount / maxSliceTax).toFloat().coerceIn(0.04f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(DeepObsidian)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fillRatio)
                                    .fillMaxHeight()
                                    .background(if (slice.rate >= 0.22) RoseCrimson else EmeraldGreen)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            TaxSummaryRow("Ordinary Income Tax", fmtShort(result.ordinaryIncomeTax), TextPrimary)
            TaxSummaryRow("Capital Gains Tax", fmtShort(result.capitalGainsTax), AmberGold)
            if (result.cessOrSurtax > 0.0) {
                TaxSummaryRow("4% Health & Education Cess", fmtShort(result.cessOrSurtax), RoseCrimson)
            }
        }
    }

    Spacer(Modifier.height(14.dp))

    Button(
        onClick = {
            viewModel.syncEffectiveTaxRateToFire()
            val pct = String.format(Locale.US, "%.1f", result.effectiveTaxRate * 100.0)
            onFeedback("Applied $pct% effective tax rate to FIRE Planner")
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("sync_tax_rate_to_fire_button"),
        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(Icons.Rounded.Sync, contentDescription = null, tint = DeepObsidian, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("APPLY EFFECTIVE RATE TO FIRE TAX-DRAG", color = DeepObsidian, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.6.sp)
    }
}

@Composable
private fun TaxSummaryRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextSecondary, fontSize = 12.sp)
        Text(value, color = color, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}
