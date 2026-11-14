package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.CalculatorMode
import com.example.models.CompoundingFrequency
import com.example.providers.FireViewModel
import com.example.services.CsvExportScope
import com.example.ui.components.CsvExportCard
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
import kotlin.math.roundToInt

@Composable
fun CalculatorsTab(
    viewModel: FireViewModel,
    onExportFeedback: (String) -> Unit = {},
    onOpenProUpgrade: () -> Unit = {}
) {
    val mode by viewModel.calculatorMode.collectAsState()
    val isIndian by viewModel.isIndianCurrency.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Text("SAVINGS & INVESTMENT SUITE", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CalculatorMode.entries.forEach { calcMode ->
                val selected = mode == calcMode
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.selectCalculatorMode(calcMode) },
                    label = { Text(calcMode.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmeraldGreen,
                        selectedLabelColor = DeepObsidian,
                        containerColor = SurfaceCard,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = Color.White.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.testTag("calc_mode_${calcMode.name.lowercase()}")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (mode) {
            CalculatorMode.SIP -> SipCalculatorSection(viewModel, isIndian)
            CalculatorMode.SWP -> SwpCalculatorSection(viewModel, isIndian)
            CalculatorMode.FD_BONDS -> FdCalculatorSection(viewModel, isIndian)
            CalculatorMode.GOAL -> GoalCalculatorSection(viewModel, isIndian)
            CalculatorMode.INFLATION -> InflationCalculatorSection(viewModel, isIndian)
            CalculatorMode.DEBT_PAYOFF -> DebtPayoffCalculatorSection(viewModel, isIndian, onExportFeedback, onOpenProUpgrade)
            CalculatorMode.TAX -> TaxCalculatorSection(viewModel, onExportFeedback)
            CalculatorMode.RETIREMENT -> RetirementCalculatorSection(viewModel, isIndian, onExportFeedback)
            CalculatorMode.CASHFLOW -> CashflowSchedulerSection(viewModel, onExportFeedback)
            CalculatorMode.BUDGET -> BudgetPlannerSection(viewModel, isIndian, onExportFeedback)
        }

        Spacer(modifier = Modifier.height(14.dp))

        CsvExportCard(
            title = "Export Savings Projections (CSV)",
            subtitle = "Save SIP, SWP, FD, Goals, Inflation, Debt, Tax, Retirement, Cashflow & Budget plans.",
            scope = CsvExportScope.PROJECTIONS,
            viewModel = viewModel,
            saveButtonTag = "export_projections_csv_button",
            shareButtonTag = "share_projections_csv_button",
            onExportFeedback = onExportFeedback
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SipCalculatorSection(viewModel: FireViewModel, isIndian: Boolean) {
    val params by viewModel.sipParams.collectAsState()
    val result by viewModel.sipResult.collectAsState()

    val maxLumpsum = if (isIndian) 1_00_00_000f else 500_000f
    val maxSip = if (isIndian) 3_00_000f else 15_000f

    Row(modifier = Modifier.fillMaxWidth()) {
        MetricCard(
            title = "Maturity Value",
            value = viewModel.formatShortCurrency(result.maturityValue),
            subtitle = "Real value ${viewModel.formatShortCurrency(result.inflationAdjustedValue)}",
            color = EmeraldGreen,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(12.dp))
        MetricCard(
            title = "Wealth Gain",
            value = viewModel.formatShortCurrency(result.wealthGain),
            subtitle = "Invested ${viewModel.formatShortCurrency(result.totalInvested)}",
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
        Text("SIP Compounding Curve", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LegendDot("Value", EmeraldGreen)
            LegendDot("Invested", CyanAccent)
        }
    }
    Spacer(Modifier.height(8.dp))
    DualSeriesChart(
        primaryValues = result.timeline.map { it.maturityValue },
        secondaryValues = result.timeline.map { it.totalInvested },
        primaryColor = EmeraldGreen,
        secondaryColor = CyanAccent,
        xStartLabel = "Yr 0",
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
            SliderInput("Monthly SIP", viewModel.formatShortCurrency(params.monthlySip), params.monthlySip.toFloat(), 0f, maxSip) {
                viewModel.updateSipParams(params.copy(monthlySip = it.toDouble()))
            }
            SliderInput("Initial Lumpsum", viewModel.formatShortCurrency(params.lumpsum), params.lumpsum.toFloat(), 0f, maxLumpsum) {
                viewModel.updateSipParams(params.copy(lumpsum = it.toDouble()))
            }
            SliderInput("Annual Step-Up", "${(params.annualStepUpRate * 100).roundToInt()}%", (params.annualStepUpRate * 100).toFloat(), 0f, 25f, CyanAccent) {
                viewModel.updateSipParams(params.copy(annualStepUpRate = it / 100.0))
            }
            SliderInput("Expected Return", "${(params.expectedReturn * 100).roundToInt()}%", (params.expectedReturn * 100).toFloat(), 1f, 22f, EmeraldGreen) {
                viewModel.updateSipParams(params.copy(expectedReturn = it / 100.0))
            }
            SliderInput("Duration", "${params.years} yrs", params.years.toFloat(), 1f, 40f) {
                viewModel.updateSipParams(params.copy(years = it.roundToInt()))
            }
            SliderInput("Inflation Rate", "${(params.inflationRate * 100).roundToInt()}%", (params.inflationRate * 100).toFloat(), 0f, 12f, AmberGold) {
                viewModel.updateSipParams(params.copy(inflationRate = it / 100.0))
            }
        }
    }
}

@Composable
private fun SwpCalculatorSection(viewModel: FireViewModel, isIndian: Boolean) {
    val params by viewModel.swpParams.collectAsState()
    val result by viewModel.swpResult.collectAsState()

    val maxCorpus = if (isIndian) 10_00_00_000f else 5_000_000f
    val maxWithdrawal = if (isIndian) 5_00_000f else 25_000f

    Row(modifier = Modifier.fillMaxWidth()) {
        MetricCard(
            title = "Total Withdrawn",
            value = viewModel.formatShortCurrency(result.totalWithdrawn),
            subtitle = "${result.monthsLasted / 12}y ${result.monthsLasted % 12}m income",
            color = CyanAccent,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(12.dp))
        MetricCard(
            title = "Final Balance",
            value = viewModel.formatShortCurrency(result.finalBalance),
            subtitle = result.depletedYear?.let { "Depletes in Yr $it" } ?: "Sustainable corpus",
            color = if (result.depletedYear == null) EmeraldGreen else RoseCrimson,
            modifier = Modifier.weight(1f)
        )
    }

    Spacer(Modifier.height(14.dp))
    Text("SWP Corpus Trajectory", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
    DualSeriesChart(
        primaryValues = result.yearlyBalances,
        secondaryValues = null,
        primaryColor = if (result.depletedYear == null) CyanAccent else AmberGold,
        secondaryColor = Color.Transparent,
        xStartLabel = "Yr 0",
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
            SliderInput("Initial Corpus", viewModel.formatShortCurrency(params.initialCorpus), params.initialCorpus.toFloat(), if (isIndian) 1_00_000f else 10_000f, maxCorpus) {
                viewModel.updateSwpParams(params.copy(initialCorpus = it.toDouble()))
            }
            SliderInput("Monthly Withdrawal", viewModel.formatShortCurrency(params.monthlyWithdrawal), params.monthlyWithdrawal.toFloat(), if (isIndian) 5_000f else 200f, maxWithdrawal, AmberGold) {
                viewModel.updateSwpParams(params.copy(monthlyWithdrawal = it.toDouble()))
            }
            SliderInput("Annual Withdrawal Step-Up", "${(params.annualWithdrawalStepUp * 100).roundToInt()}%", (params.annualWithdrawalStepUp * 100).toFloat(), 0f, 12f, RoseCrimson) {
                viewModel.updateSwpParams(params.copy(annualWithdrawalStepUp = it / 100.0))
            }
            SliderInput("Expected Return", "${(params.expectedReturn * 100).roundToInt()}%", (params.expectedReturn * 100).toFloat(), 1f, 18f, EmeraldGreen) {
                viewModel.updateSwpParams(params.copy(expectedReturn = it / 100.0))
            }
            SliderInput("Payout Horizon", "${params.years} yrs", params.years.toFloat(), 5f, 45f) {
                viewModel.updateSwpParams(params.copy(years = it.roundToInt()))
            }
        }
    }
}

@Composable
private fun FdCalculatorSection(viewModel: FireViewModel, isIndian: Boolean) {
    val params by viewModel.fdParams.collectAsState()
    val result by viewModel.fdResult.collectAsState()

    val maxPrincipal = if (isIndian) 2_00_00_000f else 1_000_000f

    Row(modifier = Modifier.fillMaxWidth()) {
        MetricCard(
            title = "Post-Tax Maturity",
            value = viewModel.formatShortCurrency(result.postTaxMaturity),
            subtitle = "Gross ${viewModel.formatShortCurrency(result.grossMaturity)}",
            color = EmeraldGreen,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(12.dp))
        MetricCard(
            title = "Post-Tax Yield",
            value = "${String.format(Locale.US, "%.2f", result.effectivePostTaxYield * 100)}%",
            subtitle = "Tax deducted ${viewModel.formatShortCurrency(result.taxDeducted)}",
            color = AmberGold,
            modifier = Modifier.weight(1f)
        )
    }

    Spacer(Modifier.height(12.dp))

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Gross Interest Earned", color = TextSecondary, fontSize = 13.sp)
                Text(viewModel.formatShortCurrency(result.grossInterest), color = CyanAccent, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Net Interest After Tax", color = TextSecondary, fontSize = 13.sp)
                Text(viewModel.formatShortCurrency(result.grossInterest - result.taxDeducted), color = EmeraldGreen, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        }
    }

    Spacer(Modifier.height(12.dp))
    Text("COMPOUNDING FREQUENCY", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    Spacer(Modifier.height(6.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CompoundingFrequency.entries.forEach { freq ->
            val selected = params.compounding == freq
            FilterChip(
                selected = selected,
                onClick = { viewModel.updateFdParams(params.copy(compounding = freq)) },
                label = { Text(freq.label, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CyanAccent,
                    selectedLabelColor = DeepObsidian,
                    containerColor = SurfaceCard,
                    labelColor = TextSecondary
                )
            )
        }
    }

    Spacer(Modifier.height(10.dp))
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SliderInput("Deposit / Bond Principal", viewModel.formatShortCurrency(params.principal), params.principal.toFloat(), if (isIndian) 10_000f else 1_000f, maxPrincipal) {
                viewModel.updateFdParams(params.copy(principal = it.toDouble()))
            }
            SliderInput(
                "Annual Interest / Coupon",
                "${String.format(Locale.US, "%.2f", params.annualRate * 100)}%",
                (params.annualRate * 100).toFloat(),
                2f,
                14f,
                EmeraldGreen
            ) {
                val quarterStep = (it * 4f).roundToInt() / 400.0
                viewModel.updateFdParams(params.copy(annualRate = quarterStep))
            }
            SliderInput("Tenure", "${params.years} yrs", params.years.toFloat(), 1f, 30f) {
                viewModel.updateFdParams(params.copy(years = it.roundToInt()))
            }
            SliderInput("Tax Slab / TDS", "${(params.taxRate * 100).roundToInt()}%", (params.taxRate * 100).toFloat(), 0f, 40f, RoseCrimson) {
                viewModel.updateFdParams(params.copy(taxRate = it / 100.0))
            }
        }
    }
}

@Composable
private fun GoalCalculatorSection(viewModel: FireViewModel, isIndian: Boolean) {
    val params by viewModel.goalParams.collectAsState()
    val result by viewModel.goalResult.collectAsState()

    val maxGoal = if (isIndian) 5_00_00_000f else 2_500_000f
    val maxCurrent = if (isIndian) 2_00_00_000f else 1_000_000f

    Row(modifier = Modifier.fillMaxWidth()) {
        MetricCard(
            title = "Required Monthly SIP",
            value = viewModel.formatShortCurrency(result.requiredMonthlySipFlat),
            subtitle = "Flat SIP each month",
            color = EmeraldGreen,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(12.dp))
        MetricCard(
            title = "With 10% Step-Up",
            value = viewModel.formatShortCurrency(result.requiredMonthlySipStepUp10),
            subtitle = "Starting SIP today",
            color = CyanAccent,
            modifier = Modifier.weight(1f)
        )
    }

    Spacer(Modifier.height(12.dp))

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Adjust Goal for Inflation", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text(
                        "Future target: ${viewModel.formatShortCurrency(result.inflationAdjustedGoal)}",
                        color = AmberGold,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Switch(
                    checked = params.adjustForInflation,
                    onCheckedChange = { viewModel.updateGoalParams(params.copy(adjustForInflation = it)) },
                    colors = SwitchDefaults.colors(checkedThumbColor = DeepObsidian, checkedTrackColor = EmeraldGreen)
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Existing Savings Future Value", color = TextSecondary, fontSize = 12.sp)
                Text(viewModel.formatShortCurrency(result.futureValueOfCurrentSavings), color = CyanAccent, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Remaining Shortfall to Fund", color = TextSecondary, fontSize = 12.sp)
                Text(viewModel.formatShortCurrency(result.shortfallToCover), color = TextPrimary, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    Spacer(Modifier.height(14.dp))
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SliderInput("Target Savings Goal", viewModel.formatShortCurrency(params.targetGoal), params.targetGoal.toFloat(), if (isIndian) 1_00_000f else 5_000f, maxGoal) {
                viewModel.updateGoalParams(params.copy(targetGoal = it.toDouble()))
            }
            SliderInput("Current Earmarked Savings", viewModel.formatShortCurrency(params.currentSavings), params.currentSavings.toFloat(), 0f, maxCurrent) {
                viewModel.updateGoalParams(params.copy(currentSavings = it.toDouble()))
            }
            SliderInput("Time Horizon", "${params.years} yrs", params.years.toFloat(), 1f, 35f) {
                viewModel.updateGoalParams(params.copy(years = it.roundToInt()))
            }
            SliderInput("Expected Return", "${(params.expectedReturn * 100).roundToInt()}%", (params.expectedReturn * 100).toFloat(), 1f, 20f, EmeraldGreen) {
                viewModel.updateGoalParams(params.copy(expectedReturn = it / 100.0))
            }
            SliderInput("Inflation Rate", "${(params.inflationRate * 100).roundToInt()}%", (params.inflationRate * 100).toFloat(), 0f, 12f, AmberGold) {
                viewModel.updateGoalParams(params.copy(inflationRate = it / 100.0))
            }
        }
    }
}
