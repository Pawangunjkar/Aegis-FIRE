package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun InflationCalculatorSection(viewModel: FireViewModel, isIndian: Boolean) {
    val params by viewModel.inflationParams.collectAsState()
    val result by viewModel.inflationResult.collectAsState()

    val maxGoal = if (isIndian) 5_00_00_000f else 2_500_000f
    var inflationInputText by remember {
        mutableStateOf(String.format(Locale.US, "%.1f", params.annualInflationRate * 100.0))
    }

    LaunchedEffect(params.annualInflationRate) {
        val currentParsed = inflationInputText.toDoubleOrNull()
        val expectedPct = params.annualInflationRate * 100.0
        if (currentParsed == null || abs(currentParsed - expectedPct) > 0.05) {
            inflationInputText = String.format(Locale.US, "%.1f", expectedPct)
        }
    }

    // 2x2 Summary KPI Cards
    Row(modifier = Modifier.fillMaxWidth()) {
        MetricCard(
            title = "Future Goal Cost",
            value = viewModel.formatShortCurrency(result.futureInflatedCost),
            subtitle = "${String.format(Locale.US, "%.2f", result.costMultiplier)}x today's cost",
            color = AmberGold,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(12.dp))
        MetricCard(
            title = "Purchasing Power Left",
            value = viewModel.formatShortCurrency(result.uninvestedPurchasingPower),
            subtitle = "-${String.format(Locale.US, "%.0f", result.purchasingPowerLostPercent)}% eroded if uninvested",
            color = RoseCrimson,
            modifier = Modifier.weight(1f)
        )
    }

    Spacer(Modifier.height(12.dp))

    Row(modifier = Modifier.fillMaxWidth()) {
        val beatsInflation = result.realAnnualReturn >= 0.0
        MetricCard(
            title = "Real Yield (Fisher)",
            value = "${if (beatsInflation) "+" else ""}${String.format(Locale.US, "%.2f", result.realAnnualReturn * 100)}%",
            subtitle = if (beatsInflation) "Outpaces inflation" else "Losing real value",
            color = if (beatsInflation) EmeraldGreen else RoseCrimson,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(12.dp))
        MetricCard(
            title = "Real Value If Invested",
            value = viewModel.formatShortCurrency(result.investedRealValue),
            subtitle = "Extra inflation cost ${viewModel.formatShortCurrency(result.inflationCostIncrease)}",
            color = CyanAccent,
            modifier = Modifier.weight(1f)
        )
    }

    Spacer(Modifier.height(14.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Inflation Gap Over Time", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LegendDot("Future Cost", AmberGold)
            LegendDot("Cash Value", RoseCrimson)
        }
    }
    Spacer(Modifier.height(8.dp))

    DualSeriesChart(
        primaryValues = result.inflatedCostTimeline,
        secondaryValues = result.purchasingPowerTimeline,
        primaryColor = AmberGold,
        secondaryColor = RoseCrimson,
        xStartLabel = "Today",
        xEndLabel = "Yr ${params.years}",
        formatShort = viewModel::formatShortCurrency
    )

    Spacer(Modifier.height(14.dp))

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SliderInput(
                label = "Savings Goal (Today's Cost)",
                formattedValue = viewModel.formatShortCurrency(params.goalAmountToday),
                value = params.goalAmountToday.toFloat(),
                min = if (isIndian) 50_000f else 1_000f,
                max = maxGoal,
                valueColor = CyanAccent
            ) {
                viewModel.updateInflationParams(params.copy(goalAmountToday = it.toDouble()))
            }

            Spacer(Modifier.height(6.dp))
            Text(
                text = "ANNUAL INFLATION RATE (%)",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = inflationInputText,
                    onValueChange = { raw ->
                        val sanitized = raw.filter { it.isDigit() || it == '.' }
                        if (sanitized.count { it == '.' } <= 1) {
                            inflationInputText = sanitized
                            val parsed = sanitized.toDoubleOrNull()
                            if (parsed != null) {
                                val clampedRate = (parsed / 100.0).coerceIn(0.0, 0.50)
                                viewModel.updateInflationParams(params.copy(annualInflationRate = clampedRate))
                            }
                        }
                    },
                    suffix = { Text("%", color = AmberGold, fontFamily = FontFamily.Monospace) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AmberGold,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.16f)
                    ),
                    modifier = Modifier
                        .width(124.dp)
                        .testTag("inflation_rate_input")
                )

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(3.0, 6.0, 8.0, 10.0).forEach { pct ->
                        val isSelected = abs(params.annualInflationRate * 100.0 - pct) < 0.05
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                inflationInputText = String.format(Locale.US, "%.1f", pct)
                                viewModel.updateInflationParams(params.copy(annualInflationRate = pct / 100.0))
                            },
                            label = { Text("${pct.toInt()}%", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AmberGold,
                                selectedLabelColor = DeepObsidian,
                                containerColor = DeepObsidian,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }
            }

            SliderInput(
                label = "Inflation Rate Slider",
                formattedValue = "${String.format(Locale.US, "%.1f", params.annualInflationRate * 100.0)}%",
                value = (params.annualInflationRate * 100.0).toFloat(),
                min = 0f,
                max = 15f,
                valueColor = AmberGold,
                testTag = "slider_inflation_impact"
            ) {
                val tenths = (it * 10f).roundToInt() / 10.0
                inflationInputText = String.format(Locale.US, "%.1f", tenths)
                viewModel.updateInflationParams(params.copy(annualInflationRate = tenths / 100.0))
            }

            SliderInput(
                label = "Time Horizon",
                formattedValue = "${params.years} yrs",
                value = params.years.toFloat(),
                min = 1f,
                max = 40f,
                valueColor = EmeraldGreen
            ) {
                viewModel.updateInflationParams(params.copy(years = it.roundToInt()))
            }

            SliderInput(
                label = "Expected Savings Yield",
                formattedValue = "${String.format(Locale.US, "%.1f", params.expectedSavingsYield * 100.0)}%",
                value = (params.expectedSavingsYield * 100.0).toFloat(),
                min = 0f,
                max = 18f,
                valueColor = EmeraldGreen
            ) {
                val tenths = (it * 10f).roundToInt() / 10.0
                viewModel.updateInflationParams(params.copy(expectedSavingsYield = tenths / 100.0))
            }
        }
    }

    Spacer(Modifier.height(6.dp))
    Text(
        text = "Tip: Enter any custom annual inflation rate above to see how much more a future goal will cost and whether your savings yield beats inflation.",
        color = TextMuted,
        fontSize = 11.sp
    )
}
