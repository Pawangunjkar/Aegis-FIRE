package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.PortfolioSummary
import com.example.models.ProEntitlementState
import com.example.models.RecurringCashflowSummary
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun PortfolioCashflowAndProCard(
    summary: PortfolioSummary,
    cashflowSummary: RecurringCashflowSummary,
    proState: ProEntitlementState,
    formatCurrency: (Double) -> String,
    formatShort: (Double) -> String,
    onSyncAutoSip: () -> Unit,
    onOpenCashflowScheduler: () -> Unit,
    onOpenProUpgrade: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyanAccent.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("AUTOMATED CASHFLOW LINK", color = CyanAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "Scheduled SIP: ${formatShort(cashflowSummary.monthlyPortfolioAutoContribution)}/mo • Net Surplus: ${formatShort(cashflowSummary.netMonthlyCashflow)}/mo",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onSyncAutoSip,
                    border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("portfolio_sync_autosip_button")
                ) {
                    Icon(Icons.Rounded.Sync, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Sync Auto-SIP", color = EmeraldGreen, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = onOpenCashflowScheduler,
                    border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.45f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("portfolio_open_cashflow_button")
                ) {
                    Text("Edit Schedule", color = CyanAccent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    Spacer(Modifier.height(12.dp))

    if (proState.isProUnlocked) {
        val estAnnualYieldIncome = summary.totalCurrentValue * (summary.weightedExpectedReturn * 0.25)
        val diversificationCount = summary.allocationSlices.size
        val efficiencyScore = ((summary.weightedExpectedReturn * 100.0) * 6.0 + diversificationCount * 8.0).coerceIn(10.0, 99.0)

        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AmberGold.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                .testTag("pro_portfolio_analytics_card")
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = AmberGold, modifier = Modifier.size(16.dp))
                    Text("PRO PORTFOLIO ANALYTICS", color = AmberGold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
                Spacer(Modifier.height(8.dp))
                InsightRow("Est. Annual Passive Yield", formatCurrency(estAnnualYieldIncome), EmeraldGreen)
                InsightRow("Risk-Adjusted Efficiency", "${String.format(Locale.US, "%.0f", efficiencyScore)} / 100", CyanAccent)
                InsightRow("Asset Class Diversification", "$diversificationCount classes active", TextPrimary)
            }
        }
    } else {
        ProUnlockBannerCard(
            proPriceLabel = proState.formattedPrice,
            onUpgradeClick = onOpenProUpgrade
        )
    }
}

@Composable
private fun InsightRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextMuted, fontSize = 12.sp)
        Text(value, color = color, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}
