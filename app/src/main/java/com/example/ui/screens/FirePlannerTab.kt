package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.AppTab
import com.example.providers.FireViewModel
import com.example.ui.components.AboutAuthorDialog
import com.example.ui.components.AuthorFooterBadge
import com.example.ui.components.DailyTipCard
import com.example.ui.components.FiScoreCompactBanner
import com.example.ui.components.LegendDot
import com.example.ui.components.MetricCard
import com.example.ui.components.SliderInput
import com.example.ui.components.TrajectoryChart
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
import kotlin.math.roundToInt

@Composable
fun FirePlannerTab(
    viewModel: FireViewModel,
    isAdLoading: Boolean,
    onRunMonteCarlo: () -> Unit,
    onExportPdf: () -> Unit,
    onFeedbackMessage: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val parameters by viewModel.parameters.collectAsState()
    val result by viewModel.result.collectAsState()
    val isIndian by viewModel.isIndianCurrency.collectAsState()
    val isSimulating by viewModel.isSimulating.collectAsState()
    val fiScoreSummary by viewModel.fiScoreSummary.collectAsState()
    val dailyTip by viewModel.dailyTip.collectAsState()
    val bookmarkedTipIds by viewModel.bookmarkedTipIds.collectAsState()
    var showAuthorDialog by remember { mutableStateOf(false) }

    val maxExpense = if (isIndian) 5_00_000f else 25_000f
    val maxSavings = if (isIndian) 5_00_00_000f else 5_000_000f
    val maxSip = if (isIndian) 5_00_000f else 25_000f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Quick FIRE Tier Presets
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Lean", "Standard", "Fat").forEach { tier ->
                FilterChip(
                    selected = false,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.applyFirePreset(tier)
                    },
                    label = { Text("$tier FIRE", fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = SurfaceCard,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = false,
                        borderColor = Color.White.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.testTag("preset_${tier.lowercase()}_chip")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2x2 Summary KPI Cards
        val res = result
        val targetVal = res?.targetFireNumber ?: 0.0
        val netVal = res?.netAccumulated ?: 0.0
        val isOnTrack = netVal >= targetVal

        Row(modifier = Modifier.fillMaxWidth()) {
            MetricCard(
                title = "Target FIRE",
                value = viewModel.formatShortCurrency(targetVal),
                subtitle = "At age ${parameters.targetAge}",
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            MetricCard(
                title = "Net Projected",
                value = viewModel.formatShortCurrency(netVal),
                subtitle = if (isOnTrack) "Surplus achieved" else "Shortfall ${viewModel.formatShortCurrency((targetVal - netVal).coerceAtLeast(0.0))}",
                color = if (isOnTrack) EmeraldGreen else RoseCrimson,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            MetricCard(
                title = "Tax Drag Lost",
                value = viewModel.formatShortCurrency(res?.taxDrag ?: 0.0),
                subtitle = "Gross ${viewModel.formatShortCurrency(res?.grossAccumulated ?: 0.0)}",
                color = AmberGold,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            MetricCard(
                title = "Retirement Runway",
                value = res?.depletionYear?.let { "Age $it" } ?: "50+ Yrs",
                subtitle = res?.fireCrossoverAge?.let { "Crosses FIRE at $it" } ?: "Not crossed yet",
                color = if (res?.depletionYear == null) CyanAccent else RoseCrimson,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Coast FIRE Milestone Banner
        if (res != null) {
            val coastReached = parameters.currentSavings >= res.coastFireNumber
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceElevated, RoundedCornerShape(12.dp))
                    .border(
                        1.dp,
                        (if (coastReached) EmeraldGreen else CyanAccent).copy(alpha = 0.25f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = if (coastReached) EmeraldGreen else CyanAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text("COAST FIRE THRESHOLD", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text(
                            text = if (coastReached) "Coast FIRE unlocked today" else "Save ${viewModel.formatShortCurrency((res.coastFireNumber - parameters.currentSavings).coerceAtLeast(0.0))} more to coast",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
                Text(
                    text = viewModel.formatShortCurrency(res.coastFireNumber),
                    color = if (coastReached) EmeraldGreen else CyanAccent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        FiScoreCompactBanner(
            scoreSummary = fiScoreSummary,
            onClick = { viewModel.selectTab(AppTab.ROADMAP) }
        )

        Spacer(modifier = Modifier.height(12.dp))
        DailyTipCard(
            tip = dailyTip,
            isBookmarked = bookmarkedTipIds.contains(dailyTip.id),
            onPreviousTip = { viewModel.previousDailyTip() },
            onNextTip = { viewModel.nextDailyTip() },
            onToggleBookmark = { viewModel.toggleBookmarkDailyTip(context) }
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Trajectory Chart with complete 3-series legend
        if (res != null && res.trajectory.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Growth vs Tax Drag",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LegendDot("Gross", EmeraldGreen)
                    LegendDot("Net", CyanAccent)
                    LegendDot("Target", RoseCrimson)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            TrajectoryChart(
                trajectory = res.trajectory,
                formatShort = viewModel::formatShortCurrency
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Core Timeline & Capital Card
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("CAPITAL & TIMELINE", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                Spacer(modifier = Modifier.height(8.dp))
                SliderInput("Current Age", "${parameters.currentAge} yrs", parameters.currentAge.toFloat(), 18f, 70f, testTag = "slider_current_age") {
                    viewModel.updateParameters(parameters.copy(currentAge = it.roundToInt()))
                }
                SliderInput("Target FIRE Age", "${parameters.targetAge} yrs", parameters.targetAge.toFloat(), (parameters.currentAge + 1).toFloat(), 80f, testTag = "slider_target_age") {
                    viewModel.updateParameters(parameters.copy(targetAge = it.roundToInt()))
                }
                SliderInput("Monthly Expense", viewModel.formatShortCurrency(parameters.monthlyExpense), parameters.monthlyExpense.toFloat(), if (isIndian) 10_000f else 500f, maxExpense, testTag = "slider_monthly_expense") {
                    viewModel.updateParameters(parameters.copy(monthlyExpense = it.toDouble()))
                }
                SliderInput("Current Savings", viewModel.formatShortCurrency(parameters.currentSavings), parameters.currentSavings.toFloat(), 0f, maxSavings, testTag = "slider_current_savings") {
                    viewModel.updateParameters(parameters.copy(currentSavings = it.toDouble()))
                }
                SliderInput("Monthly SIP / Invest", viewModel.formatShortCurrency(parameters.monthlySip), parameters.monthlySip.toFloat(), 0f, maxSip, testTag = "slider_monthly_sip") {
                    viewModel.updateParameters(parameters.copy(monthlySip = it.toDouble()))
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Previously missing engine assumptions card (Step-Up, Return, Inflation, Tax, SWR)
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("TAX, RETURN & STEP-UP ASSUMPTIONS", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                Spacer(modifier = Modifier.height(8.dp))
                SliderInput("Expected Return", "${(parameters.expectedReturn * 100).roundToInt()}%", (parameters.expectedReturn * 100).toFloat(), 0f, 22f, valueColor = EmeraldGreen, testTag = "slider_expected_return") {
                    viewModel.updateParameters(parameters.copy(expectedReturn = it / 100.0))
                }
                SliderInput("Annual SIP Step-Up", "${(parameters.stepUpRate * 100).roundToInt()}%", (parameters.stepUpRate * 100).toFloat(), 0f, 25f, valueColor = CyanAccent, testTag = "slider_step_up") {
                    viewModel.updateParameters(parameters.copy(stepUpRate = it / 100.0))
                }
                SliderInput("Inflation Rate", "${(parameters.inflationRate * 100).roundToInt()}%", (parameters.inflationRate * 100).toFloat(), 0f, 12f, valueColor = AmberGold, testTag = "slider_inflation") {
                    viewModel.updateParameters(parameters.copy(inflationRate = it / 100.0))
                }
                SliderInput("Capital Gains Tax", "${(parameters.taxRate * 100).roundToInt()}%", (parameters.taxRate * 100).toFloat(), 0f, 40f, valueColor = RoseCrimson, testTag = "slider_tax_rate") {
                    viewModel.updateParameters(parameters.copy(taxRate = it / 100.0))
                }
                SliderInput(
                    "Safe Withdrawal Rate",
                    "${String.format(Locale.US, "%.1f", parameters.safeWithdrawalRate * 100)}%",
                    (parameters.safeWithdrawalRate * 100).toFloat(),
                    2f,
                    7f,
                    valueColor = CyanAccent,
                    testTag = "slider_swr"
                ) {
                    val roundedTenths = (it * 10f).roundToInt() / 1000.0
                    viewModel.updateParameters(parameters.copy(safeWithdrawalRate = roundedTenths))
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onRunMonteCarlo()
                },
                enabled = !isSimulating && !isAdLoading,
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .testTag("run_monte_carlo_button"),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSimulating || isAdLoading) {
                    CircularProgressIndicator(
                        color = DeepObsidian,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                } else {
                    Icon(Icons.Rounded.AutoGraph, contentDescription = null, tint = DeepObsidian, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = if (isSimulating) "SIMULATING..." else "STRESS TEST",
                    color = DeepObsidian,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 0.8.sp
                )
            }

            OutlinedButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onExportPdf()
                },
                enabled = !isAdLoading,
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .testTag("export_pdf_button"),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
            ) {
                Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("PDF ROADMAP", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.8.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        AuthorFooterBadge(onClick = { showAuthorDialog = true })
        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showAuthorDialog) {
        AboutAuthorDialog(
            onFeedbackMessage = onFeedbackMessage,
            onOpenPrivacyPolicy = {
                showAuthorDialog = false
                viewModel.openPrivacyPolicy()
            },
            onDismiss = { showAuthorDialog = false }
        )
    }
}
