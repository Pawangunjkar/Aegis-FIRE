package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
fun RetirementCalculatorSection(
    viewModel: FireViewModel,
    isIndian: Boolean,
    onFeedback: (String) -> Unit = {}
) {
    val params by viewModel.retirementPlanParams.collectAsState()
    val result by viewModel.retirementPlanResult.collectAsState()

    val maxExpense = if (isIndian) 5_00_000f else 25_000f
    val maxSavings = if (isIndian) 5_00_00_000f else 2_000_000f
    val currencySymbol = if (isIndian) "₹" else "$"

    var retireAgeText by remember { mutableStateOf(params.retirementAge.toString()) }
    var lifeExpectancyText by remember { mutableStateOf(params.lifeExpectancy.toString()) }
    var monthlyExpenseText by remember { mutableStateOf(params.targetMonthlyExpensesToday.roundToInt().toString()) }
    var inflationText by remember {
        mutableStateOf(String.format(Locale.US, "%.1f", params.annualInflationRate * 100.0))
    }

    LaunchedEffect(params.retirementAge) {
        if (retireAgeText.toIntOrNull() != params.retirementAge) {
            retireAgeText = params.retirementAge.toString()
        }
    }
    LaunchedEffect(params.lifeExpectancy) {
        if (lifeExpectancyText.toIntOrNull() != params.lifeExpectancy) {
            lifeExpectancyText = params.lifeExpectancy.toString()
        }
    }
    LaunchedEffect(params.targetMonthlyExpensesToday) {
        val currentVal = monthlyExpenseText.toDoubleOrNull()
        if (currentVal == null || abs(currentVal - params.targetMonthlyExpensesToday) >= 1.0) {
            monthlyExpenseText = params.targetMonthlyExpensesToday.roundToInt().toString()
        }
    }
    LaunchedEffect(params.annualInflationRate) {
        val currentPct = inflationText.toDoubleOrNull()
        val expectedPct = params.annualInflationRate * 100.0
        if (currentPct == null || abs(currentPct - expectedPct) > 0.05) {
            inflationText = String.format(Locale.US, "%.1f", expectedPct)
        }
    }

    // 2x2 Summary KPI Cards
    Row(modifier = Modifier.fillMaxWidth()) {
        MetricCard(
            title = "Required Corpus",
            value = viewModel.formatShortCurrency(result.requiredCorpusAtRetirement),
            subtitle = "At age ${result.retirementAge} • ${result.yearsInRetirement}y drawdown",
            color = EmeraldGreen,
            modifier = Modifier
                .weight(1f)
                .testTag("retirement_required_corpus_card")
        )
        Spacer(Modifier.width(12.dp))
        MetricCard(
            title = "Monthly Expense Then",
            value = viewModel.formatShortCurrency(result.monthlyExpenseAtRetirement),
            subtitle = "${viewModel.formatShortCurrency(result.finalMonthlyExpenseAtLifeExpectancy)}/mo at age ${result.lifeExpectancy}",
            color = AmberGold,
            modifier = Modifier.weight(1f)
        )
    }

    Spacer(Modifier.height(12.dp))

    Row(modifier = Modifier.fillMaxWidth()) {
        MetricCard(
            title = "Required Monthly SIP",
            value = viewModel.formatShortCurrency(result.requiredMonthlySip),
            subtitle = "Over ${result.yearsToRetirement} yrs • Shortfall ${viewModel.formatShortCurrency(result.corpusShortfall)}",
            color = CyanAccent,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(12.dp))
        val realYieldPct = result.realPostRetirementReturn * 100.0
        MetricCard(
            title = "Existing Savings FV",
            value = viewModel.formatShortCurrency(result.projectedCurrentSavingsAtRetirement),
            subtitle = "Real ret ${if (realYieldPct >= 0) "+" else ""}${String.format(Locale.US, "%.1f", realYieldPct)}%",
            color = if (result.corpusShortfall <= 0.0) EmeraldGreen else TextPrimary,
            modifier = Modifier.weight(1f)
        )
    }

    Spacer(Modifier.height(14.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Lifecycle Corpus & Drawdown", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LegendDot("Corpus", EmeraldGreen)
            LegendDot("Target", AmberGold)
        }
    }
    Spacer(Modifier.height(8.dp))

    DualSeriesChart(
        primaryValues = result.lifecycleTrajectory.map { it.projectedCorpus },
        secondaryValues = result.lifecycleTrajectory.map { it.targetCorpusBenchmark },
        primaryColor = EmeraldGreen,
        secondaryColor = AmberGold,
        xStartLabel = "Age ${result.currentAge}",
        xEndLabel = "Age ${result.lifeExpectancy}",
        formatShort = viewModel::formatShortCurrency
    )

    Spacer(Modifier.height(14.dp))

    // Inputs Card
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "RETIREMENT HORIZON & EXPENSES",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(10.dp))

            // Direct Precision Inputs Row 1: Retire Age & Life Expectancy
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = retireAgeText,
                    onValueChange = { raw ->
                        val digits = raw.filter { it.isDigit() }
                        retireAgeText = digits
                        val parsed = digits.toIntOrNull()
                        if (parsed != null && parsed in (params.currentAge + 1)..95) {
                            val adjustedLife = params.lifeExpectancy.coerceAtLeast(parsed + 1)
                            viewModel.updateRetirementPlanParams(
                                params.copy(retirementAge = parsed, lifeExpectancy = adjustedLife)
                            )
                        }
                    },
                    label = { Text("Retire Age", fontSize = 11.sp) },
                    suffix = { Text("yrs", color = TextMuted, fontSize = 11.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.16f),
                        focusedLabelColor = CyanAccent,
                        unfocusedLabelColor = TextSecondary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("retirement_age_input")
                )

                OutlinedTextField(
                    value = lifeExpectancyText,
                    onValueChange = { raw ->
                        val digits = raw.filter { it.isDigit() }
                        lifeExpectancyText = digits
                        val parsed = digits.toIntOrNull()
                        if (parsed != null && parsed in (params.retirementAge + 1)..110) {
                            viewModel.updateRetirementPlanParams(params.copy(lifeExpectancy = parsed))
                        }
                    },
                    label = { Text("Life Expectancy", fontSize = 11.sp) },
                    suffix = { Text("yrs", color = TextMuted, fontSize = 11.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = EmeraldGreen,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.16f),
                        focusedLabelColor = EmeraldGreen,
                        unfocusedLabelColor = TextSecondary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("retirement_life_expectancy_input")
                )
            }

            Spacer(Modifier.height(10.dp))

            // Direct Precision Inputs Row 2: Monthly Expense & Inflation Rate
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = monthlyExpenseText,
                    onValueChange = { raw ->
                        val sanitized = raw.filter { it.isDigit() || it == '.' }
                        if (sanitized.count { it == '.' } <= 1) {
                            monthlyExpenseText = sanitized
                            val parsed = sanitized.toDoubleOrNull()
                            if (parsed != null && parsed >= 0.0) {
                                viewModel.updateRetirementPlanParams(
                                    params.copy(targetMonthlyExpensesToday = parsed)
                                )
                            }
                        }
                    },
                    label = { Text("Monthly Expense", fontSize = 11.sp) },
                    prefix = { Text(currencySymbol, color = AmberGold, fontFamily = FontFamily.Monospace) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AmberGold,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.16f),
                        focusedLabelColor = AmberGold,
                        unfocusedLabelColor = TextSecondary
                    ),
                    modifier = Modifier
                        .weight(1.25f)
                        .testTag("retirement_monthly_expense_input")
                )

                OutlinedTextField(
                    value = inflationText,
                    onValueChange = { raw ->
                        val sanitized = raw.filter { it.isDigit() || it == '.' }
                        if (sanitized.count { it == '.' } <= 1) {
                            inflationText = sanitized
                            val parsed = sanitized.toDoubleOrNull()
                            if (parsed != null && parsed in 0.0..30.0) {
                                viewModel.updateRetirementPlanParams(
                                    params.copy(annualInflationRate = parsed / 100.0)
                                )
                            }
                        }
                    },
                    label = { Text("Inflation", fontSize = 11.sp) },
                    suffix = { Text("%", color = RoseCrimson, fontFamily = FontFamily.Monospace) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = RoseCrimson,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.16f),
                        focusedLabelColor = RoseCrimson,
                        unfocusedLabelColor = TextSecondary
                    ),
                    modifier = Modifier
                        .weight(0.85f)
                        .testTag("retirement_inflation_input")
                )
            }

            Spacer(Modifier.height(10.dp))

            SliderInput(
                label = "Current Age",
                formattedValue = "${params.currentAge} yrs",
                value = params.currentAge.toFloat(),
                min = 18f,
                max = 75f,
                valueColor = TextPrimary,
                testTag = "retirement_current_age_slider"
            ) {
                val age = it.roundToInt()
                val retire = params.retirementAge.coerceAtLeast(age + 1)
                val life = params.lifeExpectancy.coerceAtLeast(retire + 1)
                viewModel.updateRetirementPlanParams(
                    params.copy(currentAge = age, retirementAge = retire, lifeExpectancy = life)
                )
            }

            SliderInput(
                label = "Target Retirement Age",
                formattedValue = "${params.retirementAge} yrs",
                value = params.retirementAge.toFloat(),
                min = (params.currentAge + 1).toFloat(),
                max = 85f,
                valueColor = CyanAccent,
                testTag = "retirement_age_slider"
            ) {
                val retire = it.roundToInt()
                val life = params.lifeExpectancy.coerceAtLeast(retire + 1)
                viewModel.updateRetirementPlanParams(
                    params.copy(retirementAge = retire, lifeExpectancy = life)
                )
            }

            SliderInput(
                label = "Life Expectancy",
                formattedValue = "${params.lifeExpectancy} yrs",
                value = params.lifeExpectancy.toFloat(),
                min = (params.retirementAge + 1).toFloat(),
                max = 105f,
                valueColor = EmeraldGreen,
                testTag = "retirement_life_expectancy_slider"
            ) {
                viewModel.updateRetirementPlanParams(params.copy(lifeExpectancy = it.roundToInt()))
            }

            SliderInput(
                label = "Target Monthly Expenses (Today)",
                formattedValue = viewModel.formatShortCurrency(params.targetMonthlyExpensesToday),
                value = params.targetMonthlyExpensesToday.toFloat().coerceAtMost(maxExpense),
                min = if (isIndian) 10_000f else 500f,
                max = maxExpense,
                valueColor = AmberGold
            ) {
                viewModel.updateRetirementPlanParams(params.copy(targetMonthlyExpensesToday = it.toDouble()))
            }

            SliderInput(
                label = "Expected Inflation Rate",
                formattedValue = "${String.format(Locale.US, "%.1f", params.annualInflationRate * 100.0)}%",
                value = (params.annualInflationRate * 100.0).toFloat(),
                min = 0f,
                max = 15f,
                valueColor = RoseCrimson
            ) {
                val tenths = (it * 10f).roundToInt() / 10.0
                viewModel.updateRetirementPlanParams(params.copy(annualInflationRate = tenths / 100.0))
            }

            SliderInput(
                label = "Pre-Retirement Return",
                formattedValue = "${String.format(Locale.US, "%.1f", params.preRetirementReturn * 100.0)}%",
                value = (params.preRetirementReturn * 100.0).toFloat(),
                min = 2f,
                max = 18f,
                valueColor = EmeraldGreen
            ) {
                val tenths = (it * 10f).roundToInt() / 10.0
                viewModel.updateRetirementPlanParams(params.copy(preRetirementReturn = tenths / 100.0))
            }

            SliderInput(
                label = "Post-Retirement Return",
                formattedValue = "${String.format(Locale.US, "%.1f", params.postRetirementReturn * 100.0)}%",
                value = (params.postRetirementReturn * 100.0).toFloat(),
                min = 1f,
                max = 14f,
                valueColor = CyanAccent
            ) {
                val tenths = (it * 10f).roundToInt() / 10.0
                viewModel.updateRetirementPlanParams(params.copy(postRetirementReturn = tenths / 100.0))
            }

            SliderInput(
                label = "Current Retirement Savings",
                formattedValue = viewModel.formatShortCurrency(params.currentRetirementSavings),
                value = params.currentRetirementSavings.toFloat().coerceAtMost(maxSavings),
                min = 0f,
                max = maxSavings,
                valueColor = TextPrimary
            ) {
                viewModel.updateRetirementPlanParams(params.copy(currentRetirementSavings = it.toDouble()))
            }
        }
    }

    Spacer(Modifier.height(14.dp))

    // Inflation & Payout Breakdown Card
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "INFLATION-ADJUSTED RETIREMENT SUMMARY",
                color = TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(10.dp))
            RetirementSummaryRow(
                label = "Accumulation / Retirement Span",
                value = "${result.yearsToRetirement} yrs / ${result.yearsInRetirement} yrs",
                color = TextPrimary
            )
            RetirementSummaryRow(
                label = "Annual Expense Today",
                value = viewModel.formatShortCurrency(params.targetMonthlyExpensesToday * 12.0),
                color = TextSecondary
            )
            RetirementSummaryRow(
                label = "Annual Expense at Age ${result.retirementAge}",
                value = viewModel.formatShortCurrency(result.monthlyExpenseAtRetirement * 12.0),
                color = AmberGold
            )
            RetirementSummaryRow(
                label = "Annual Expense at Age ${result.lifeExpectancy}",
                value = viewModel.formatShortCurrency(result.finalMonthlyExpenseAtLifeExpectancy * 12.0),
                color = RoseCrimson
            )
            RetirementSummaryRow(
                label = "Total Payout Over ${result.yearsInRetirement} Years",
                value = viewModel.formatShortCurrency(result.totalRetirementPayoutNominal),
                color = CyanAccent
            )
            RetirementSummaryRow(
                label = "Required Corpus at Age ${result.retirementAge}",
                value = viewModel.formatShortCurrency(result.requiredCorpusAtRetirement),
                color = EmeraldGreen
            )
        }
    }

    Spacer(Modifier.height(14.dp))

    Button(
        onClick = {
            viewModel.syncRetirementPlanToFire()
            onFeedback("Synced retirement ages, inflation & ${viewModel.formatShortCurrency(result.requiredMonthlySip)}/mo SIP to FIRE")
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("sync_retirement_to_fire_button"),
        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(Icons.Rounded.Sync, contentDescription = null, tint = DeepObsidian, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = "SYNC RETIREMENT PLAN TO FIRE",
            color = DeepObsidian,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 0.6.sp
        )
    }
}

@Composable
private fun RetirementSummaryRow(label: String, value: String, color: Color) {
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
