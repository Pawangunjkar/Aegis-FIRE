package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.ProEntitlementState
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ProUpgradeDialog(
    proState: ProEntitlementState,
    onPurchaseClick: () -> Unit,
    onRestoreClick: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceElevated,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (proState.isProUnlocked) Icons.Rounded.Verified else Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = AmberGold,
                    modifier = Modifier.size(22.dp)
                )
                Column {
                    Text(
                        text = if (proState.isProUnlocked) "Aegis Pro Active" else "Upgrade to Aegis Pro",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = if (proState.isProUnlocked) "Lifetime license verified" else "One-time purchase • ${proState.formattedPrice} forever",
                        color = AmberGold,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Pay once, keep forever — no subscriptions. Keep full access to every calculator while unlocking Pro privileges:",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                ProFeatureItem("100% Ad-Free Experience", "Removes banner, interstitial, and rewarded ad waits permanently.")
                ProFeatureItem("Instant Monte Carlo & PDF Exports", "Run 1,000-path stress tests and share PDF roadmaps without interruptions.")
                ProFeatureItem("Portfolio Efficiency & Dividend Estimator", "Pro risk-adjusted return score and estimated annual dividend yield.")
                ProFeatureItem("Cashflow Surplus Auto-Sweep", "Allocate 50% of net monthly cashflow surplus directly into debt elimination.")

                if (proState.isPurchasePending) {
                    Text(
                        text = "Your Google Play payment is pending confirmation.",
                        color = AmberGold,
                        fontSize = 11.sp
                    )
                }
            }
        },
        confirmButton = {
            if (!proState.isProUnlocked) {
                Button(
                    onClick = onPurchaseClick,
                    enabled = !proState.isPurchasePending,
                    colors = ButtonDefaults.buttonColors(containerColor = AmberGold),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("purchase_pro_confirm_button")
                ) {
                    Icon(Icons.Rounded.LockOpen, contentDescription = null, tint = DeepObsidian, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Unlock Forever • ${proState.formattedPrice}", color = DeepObsidian, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            } else {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Done", color = DeepObsidian, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            Row {
                if (!proState.isProUnlocked) {
                    TextButton(
                        onClick = onRestoreClick,
                        modifier = Modifier.testTag("restore_pro_purchase_button")
                    ) {
                        Text("Restore", color = CyanAccent, fontWeight = FontWeight.SemiBold)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Close", color = TextSecondary)
                }
            }
        }
    )
}

@Composable
private fun ProFeatureItem(title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceCard, RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(18.dp))
        Column {
            Text(title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = TextMuted, fontSize = 11.sp)
        }
    }
}

@Composable
fun ProUnlockBannerCard(
    proPriceLabel: String,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, AmberGold.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(AmberGold.copy(alpha = 0.14f), CyanAccent.copy(alpha = 0.06f))
                    )
                )
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("AEGIS PRO • ONE-TIME $proPriceLabel", color = AmberGold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.height(3.dp))
                Text("Go ad-free, unlock Pro portfolio analytics & instant exports forever.", color = TextSecondary, fontSize = 11.sp)
            }
            Spacer(Modifier.width(10.dp))
            OutlinedButton(
                onClick = onUpgradeClick,
                border = BorderStroke(1.dp, AmberGold),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("pro_banner_upgrade_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("Upgrade", color = AmberGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
