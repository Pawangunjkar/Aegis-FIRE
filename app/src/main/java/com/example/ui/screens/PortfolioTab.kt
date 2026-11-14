package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.AppTab
import com.example.models.AssetHolding
import com.example.models.AssetType
import com.example.models.CalculatorMode
import com.example.providers.FireViewModel
import com.example.services.CsvExportScope
import com.example.ui.components.CsvExportCard
import com.example.ui.components.DualSeriesChart
import com.example.ui.components.LegendDot
import com.example.ui.components.MetricCard
import com.example.ui.components.PortfolioCashflowAndProCard
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
import kotlin.math.roundToInt

@Composable
fun PortfolioTab(
    viewModel: FireViewModel,
    onSyncedToFire: () -> Unit,
    onExportFeedback: (String) -> Unit = {},
    onOpenProUpgrade: () -> Unit = {}
) {
    val context = LocalContext.current
    val holdings by viewModel.holdings.collectAsState()
    val liabilities by viewModel.liabilities.collectAsState()
    val summary by viewModel.portfolioSummary.collectAsState()
    val projectionYears by viewModel.portfolioProjectionYears.collectAsState()
    val cashflowSummary by viewModel.cashflowSummary.collectAsState()
    val proState by viewModel.proState.collectAsState()

    var selectedFilter by remember { mutableStateOf<AssetType?>(null) }
    var editingHolding by remember { mutableStateOf<AssetHolding?>(null) }
    var quickPriceHolding by remember { mutableStateOf<AssetHolding?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showAddLiabilityDialog by remember { mutableStateOf(false) }

    val filteredHoldings = if (selectedFilter == null) holdings else holdings.filter { it.assetType == selectedFilter }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Text("NET WORTH & PORTFOLIO TRACKER", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Spacer(modifier = Modifier.height(10.dp))

        // Holistic Net Worth Hero Card
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, EmeraldGreen.copy(alpha = 0.28f), RoundedCornerShape(18.dp))
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.horizontalGradient(
                            listOf(EmeraldGreen.copy(alpha = 0.12f), CyanAccent.copy(alpha = 0.06f))
                        )
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Rounded.AccountBalanceWallet, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(18.dp))
                        Text("TOTAL NET WORTH", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    }
                    val pnlPositive = summary.totalUnrealizedPnL >= 0.0
                    Text(
                        text = "${if (pnlPositive) "+" else ""}${viewModel.formatShortCurrency(summary.totalUnrealizedPnL)} (${if (pnlPositive) "+" else ""}${String.format(Locale.US, "%.1f", summary.totalUnrealizedPnLPercent)}%)",
                        color = if (pnlPositive) EmeraldGreen else RoseCrimson,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = viewModel.formatCurrency(summary.netWorth),
                    color = TextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = (-0.8).sp,
                    modifier = Modifier.testTag("net_worth_value")
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("MARKET ASSETS", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(viewModel.formatShortCurrency(summary.totalCurrentValue), color = CyanAccent, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                    Column {
                        Text("INVESTED COST", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(viewModel.formatShortCurrency(summary.totalInvestedCost), color = TextSecondary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("LIABILITIES", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = viewModel.formatShortCurrency(summary.totalLiabilities),
                            color = if (summary.totalLiabilities > 0.0) RoseCrimson else TextSecondary,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Starter Portfolio Mixes
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Balanced" to "Balanced Mix", "Growth" to "Equity + MF Growth", "Conservative" to "FD + Bonds Safe").forEach { (key, label) ->
                FilterChip(
                    selected = false,
                    onClick = { viewModel.applyPortfolioPreset(context, key) },
                    label = { Text(label, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(containerColor = SurfaceCard, labelColor = TextSecondary),
                    border = FilterChipDefaults.filterChipBorder(enabled = true, selected = false, borderColor = Color.White.copy(alpha = 0.1f))
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            MetricCard(
                title = "Monthly SIP Flow",
                value = viewModel.formatShortCurrency(summary.totalMonthlyContribution),
                subtitle = "Blended CAGR ${String.format(Locale.US, "%.1f", summary.weightedExpectedReturn * 100)}%",
                color = CyanAccent,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            MetricCard(
                title = "${projectionYears}-Yr Net Value",
                value = viewModel.formatShortCurrency(summary.projectedNet10Yr),
                subtitle = "Tax drag ${viewModel.formatShortCurrency(summary.projectedTaxDrag10Yr)}",
                color = EmeraldGreen,
                modifier = Modifier.weight(1f)
            )
        }

        // Asset Allocation Stacked Bar
        if (summary.allocationSlices.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("ASSET CLASS ALLOCATION", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(RoundedCornerShape(7.dp))
                    ) {
                        summary.allocationSlices.forEach { slice ->
                            Box(
                                modifier = Modifier
                                    .weight(slice.percentage.toFloat().coerceAtLeast(1f))
                                    .fillMaxHeight()
                                    .background(assetColor(slice.assetType))
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        summary.allocationSlices.forEach { slice ->
                            LegendDot(
                                label = "${slice.assetType.label} (${slice.percentage.roundToInt()}%)",
                                color = assetColor(slice.assetType)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Holdings Header & Add Asset Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("TRACKED ASSETS (${holdings.size})", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            OutlinedButton(
                onClick = { showAddDialog = true },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.5f)),
                modifier = Modifier.testTag("add_holding_button")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add Asset", color = EmeraldGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Filter Holdings by Asset Type
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedFilter == null,
                onClick = { selectedFilter = null },
                label = { Text("All (${holdings.size})", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = EmeraldGreen,
                    selectedLabelColor = DeepObsidian,
                    containerColor = SurfaceCard,
                    labelColor = TextSecondary
                )
            )
            AssetType.entries.forEach { type ->
                val count = holdings.count { it.assetType == type }
                if (count > 0) {
                    FilterChip(
                        selected = selectedFilter == type,
                        onClick = { selectedFilter = if (selectedFilter == type) null else type },
                        label = { Text("${type.label} ($count)", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = assetColor(type),
                            selectedLabelColor = DeepObsidian,
                            containerColor = SurfaceCard,
                            labelColor = TextSecondary
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Tracked Asset Cards
        filteredHoldings.forEach { holding ->
            val pnl = holding.unrealizedPnL
            val pnlPct = holding.unrealizedPnLPercent
            val isProfit = pnl >= 0.0

            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(assetColor(holding.assetType), RoundedCornerShape(5.dp))
                                )
                                Text(holding.name, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                if (holding.symbol.isNotBlank()) {
                                    Text(
                                        text = holding.symbol,
                                        color = CyanAccent,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .background(CyanAccent.copy(alpha = 0.14f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            val unitSummary = if (holding.quantity > 0.0 && holding.currentMarketPrice > 0.0) {
                                "${holding.quantity} units @ ${viewModel.formatCurrency(holding.currentMarketPrice)}"
                            } else {
                                "${holding.assetType.label} • Return ${(holding.expectedReturn * 100).roundToInt()}%"
                            }
                            Text(unitSummary, color = TextMuted, fontSize = 11.sp)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = viewModel.formatCurrency(holding.effectiveMarketValue),
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "${if (isProfit) "+" else ""}${viewModel.formatShortCurrency(pnl)} (${if (isProfit) "+" else ""}${String.format(Locale.US, "%.1f", pnlPct)}%)",
                                color = if (isProfit) EmeraldGreen else RoseCrimson,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Cost ${viewModel.formatShortCurrency(holding.effectiveCostBasis)}  •  SIP ${viewModel.formatShortCurrency(holding.monthlyContribution)}/mo",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Row {
                            IconButton(
                                onClick = { quickPriceHolding = holding },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Rounded.TrendingUp, contentDescription = "Update Market Price for ${holding.name}", tint = EmeraldGreen, modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = { editingHolding = holding },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Rounded.Edit, contentDescription = "Edit ${holding.name}", tint = TextSecondary, modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = { viewModel.removeHolding(context, holding.id) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove ${holding.name}", tint = RoseCrimson, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Liabilities / Debt Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("LIABILITIES & LOANS (${liabilities.size})", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            OutlinedButton(
                onClick = { showAddLiabilityDialog = true },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, RoseCrimson.copy(alpha = 0.5f)),
                modifier = Modifier.testTag("add_liability_button")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, tint = RoseCrimson, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add Debt", color = RoseCrimson, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        if (liabilities.isEmpty()) {
            Text("No liabilities recorded — Net Worth equals 100% of market assets.", color = TextMuted, fontSize = 12.sp)
        } else {
            liabilities.forEach { debt ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .border(1.dp, RoseCrimson.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(debt.name, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(debt.category, color = TextMuted, fontSize = 11.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "-${viewModel.formatCurrency(debt.balance)}",
                                color = RoseCrimson,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            IconButton(onClick = { viewModel.removeLiability(context, debt.id) }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove ${debt.name}", tint = RoseCrimson)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Projection Horizon Slider + Growth Chart
        SliderInput(
            label = "Net Worth Projection Horizon",
            formattedValue = "$projectionYears yrs",
            value = projectionYears.toFloat(),
            min = 3f,
            max = 35f,
            valueColor = CyanAccent
        ) {
            viewModel.updatePortfolioProjectionYears(it.roundToInt())
        }

        if (summary.yearlyNetTrajectory.size >= 2) {
            DualSeriesChart(
                primaryValues = summary.yearlyNetTrajectory,
                secondaryValues = null,
                primaryColor = EmeraldGreen,
                secondaryColor = Color.Transparent,
                xStartLabel = "Today",
                xEndLabel = "+$projectionYears yrs",
                formatShort = viewModel::formatShortCurrency
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        PortfolioCashflowAndProCard(
            summary = summary,
            cashflowSummary = cashflowSummary,
            proState = proState,
            formatCurrency = viewModel::formatCurrency,
            formatShort = viewModel::formatShortCurrency,
            onSyncAutoSip = {
                val synced = viewModel.applyCashflowToPortfolioSip(context)
                onExportFeedback("Updated monthly SIP from recurring rules (${viewModel.formatShortCurrency(synced)}/mo)")
            },
            onOpenCashflowScheduler = {
                viewModel.selectCalculatorMode(CalculatorMode.CASHFLOW)
                viewModel.selectTab(AppTab.CALCULATORS)
            },
            onOpenProUpgrade = onOpenProUpgrade
        )

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = {
                viewModel.syncPortfolioToFire()
                onSyncedToFire()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("sync_portfolio_to_fire_button"),
            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Rounded.Sync, contentDescription = null, tint = DeepObsidian, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("SYNC NET WORTH & MIX TO FIRE PLANNER", color = DeepObsidian, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.6.sp)
        }

        Spacer(modifier = Modifier.height(14.dp))

        CsvExportCard(
            title = "Export Portfolio Data (CSV)",
            subtitle = "Save holdings, unit market prices, P&L, liabilities & net worth projection.",
            scope = CsvExportScope.PORTFOLIO,
            viewModel = viewModel,
            saveButtonTag = "export_portfolio_csv_button",
            shareButtonTag = "share_portfolio_csv_button",
            onExportFeedback = onExportFeedback
        )

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showAddDialog || editingHolding != null) {
        HoldingEditorDialog(
            initial = editingHolding,
            formatCurrency = viewModel::formatCurrency,
            onDismiss = {
                showAddDialog = false
                editingHolding = null
            },
            onSave = { saved ->
                viewModel.addOrUpdateHolding(context, saved)
                showAddDialog = false
                editingHolding = null
            }
        )
    }

    if (quickPriceHolding != null) {
        QuickPriceUpdateDialog(
            holding = quickPriceHolding!!,
            onDismiss = { quickPriceHolding = null },
            onUpdate = { newPriceOrValue ->
                viewModel.updateHoldingMarketValueOrPrice(context, quickPriceHolding!!.id, newPriceOrValue)
                quickPriceHolding = null
            }
        )
    }

    if (showAddLiabilityDialog) {
        LiabilityEditorDialog(
            onDismiss = { showAddLiabilityDialog = false },
            onSave = { debt ->
                viewModel.addOrUpdateLiability(context, debt)
                showAddLiabilityDialog = false
            }
        )
    }
}
