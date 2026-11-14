package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.providers.FireViewModel
import com.example.services.CsvExportScope
import com.example.ui.components.AboutAuthorCard
import com.example.ui.components.CsvExportCard
import com.example.ui.components.FiScoreGamificationSection
import com.example.ui.components.MetricCard
import com.example.ui.components.MonteCarloFanChart
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

@Composable
fun RoadmapTab(
    viewModel: FireViewModel,
    isAdLoading: Boolean,
    onRunMonteCarlo: () -> Unit,
    onPrintPdf: () -> Unit,
    onSharePdf: () -> Unit,
    onLoadProfile: (String) -> Unit,
    onExportFeedback: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val mcResult by viewModel.monteCarloResult.collectAsState()
    val isSimulating by viewModel.isSimulating.collectAsState()
    val savedProfiles by viewModel.savedProfileNames.collectAsState()
    val fiScoreSummary by viewModel.fiScoreSummary.collectAsState()

    var newProfileName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        FiScoreGamificationSection(scoreSummary = fiScoreSummary)
        Spacer(modifier = Modifier.height(18.dp))
        Text("STRESS TEST & SAVED ROADMAPS", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Spacer(modifier = Modifier.height(10.dp))

        // Monte Carlo Section
        Button(
            onClick = onRunMonteCarlo,
            enabled = !isSimulating && !isAdLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("roadmap_monte_carlo_button"),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isSimulating || isAdLoading) {
                CircularProgressIndicator(color = DeepObsidian, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            } else {
                Icon(Icons.Rounded.AutoGraph, contentDescription = null, tint = DeepObsidian, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = if (isSimulating) "RUNNING 1,000 PATHS..." else "RUN 1,000-PATH MONTE CARLO",
                color = DeepObsidian,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 0.8.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        val mc = mcResult
        if (mc != null) {
            Row(modifier = Modifier.fillMaxWidth()) {
                MetricCard(
                    title = "30-Yr Survival",
                    value = "${String.format(Locale.US, "%.1f", mc.longevityProbability)}%",
                    subtitle = "1,000 market runs",
                    color = if (mc.longevityProbability >= 80.0) EmeraldGreen else AmberGold,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                MetricCard(
                    title = "40-Yr Survival",
                    value = "${String.format(Locale.US, "%.1f", mc.longevity40Probability)}%",
                    subtitle = "Extended horizon",
                    color = if (mc.longevity40Probability >= 75.0) CyanAccent else RoseCrimson,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            MonteCarloFanChart(
                mcResult = mc,
                formatShort = viewModel::formatShortCurrency
            )

            Spacer(modifier = Modifier.height(12.dp))
            val yr30 = 30.coerceAtMost(mc.percentile50.lastIndex)
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("YEAR 30 RETIREMENT OUTCOME BANDS", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(8.dp))
                    PercentileRow("90th Percentile (Bull Market)", viewModel.formatShortCurrency(mc.percentile90[yr30]), EmeraldGreen)
                    PercentileRow("50th Percentile (Median)", viewModel.formatShortCurrency(mc.percentile50[yr30]), CyanAccent)
                    PercentileRow("10th Percentile (Bear Market)", viewModel.formatShortCurrency(mc.percentile10[yr30]), AmberGold)
                }
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("No Stress Test Run Yet", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Tap above to simulate 1,000 randomized market volatility sequences (15% std dev) against your inflation-adjusted retirement expenses.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Export & Share PDF Roadmap
        Text("EXPORT & SHARE ROADMAP", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onPrintPdf,
                enabled = !isAdLoading,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("print_pdf_button"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            ) {
                Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("PRINT / SAVE", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            Button(
                onClick = onSharePdf,
                enabled = !isAdLoading,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("share_pdf_button"),
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Rounded.Share, contentDescription = null, tint = DeepObsidian, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("SHARE PDF", color = DeepObsidian, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        CsvExportCard(
            title = "Full Data Backup (CSV)",
            subtitle = "Export all portfolio holdings, liabilities, FIRE & calculator projections in one CSV.",
            scope = CsvExportScope.FULL_BACKUP,
            viewModel = viewModel,
            saveButtonTag = "export_full_csv_button",
            shareButtonTag = "share_full_csv_button",
            onExportFeedback = onExportFeedback
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Saved Scenarios / Profiles Manager
        Text("SAVED FIRE SCENARIOS", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = newProfileName,
                onValueChange = { newProfileName = it },
                placeholder = { Text("Scenario name (e.g. Early 45)", color = TextMuted, fontSize = 13.sp) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = EmeraldGreen,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.16f)
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("scenario_name_input")
            )
            Button(
                onClick = {
                    viewModel.saveProfile(context, newProfileName)
                    newProfileName = ""
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .height(54.dp)
                    .testTag("save_scenario_button")
            ) {
                Icon(Icons.Rounded.BookmarkAdd, contentDescription = "Save scenario", tint = DeepObsidian)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (savedProfiles.isEmpty()) {
            Text("No saved scenarios yet. Save your current FIRE parameters above.", color = TextMuted, fontSize = 12.sp)
        } else {
            savedProfiles.forEach { profileName ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .clickable { onLoadProfile(profileName) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(profileName, color = EmeraldGreen, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Tap to load into FIRE planner", color = TextMuted, fontSize = 11.sp)
                        }
                        IconButton(onClick = { viewModel.deleteProfile(context, profileName) }) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete $profileName", tint = RoseCrimson)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        AboutAuthorCard(
            onFeedbackMessage = onExportFeedback,
            onOpenPrivacyPolicy = { viewModel.openPrivacyPolicy() }
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun PercentileRow(label: String, value: String, color: Color) {
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
