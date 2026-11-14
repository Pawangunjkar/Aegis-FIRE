package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.models.MonteCarloResult
import com.example.models.TrajectoryPoint
import com.example.services.AdMobService
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RoseCrimson
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String? = null,
    color: Color = TextPrimary,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title.uppercase(),
                color = TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = color,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = (-0.5).sp
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun SliderInput(
    label: String,
    formattedValue: String,
    value: Float,
    min: Float,
    max: Float,
    valueColor: Color = EmeraldGreen,
    testTag: String = "",
    onValueChange: (Float) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val safeMax = max.coerceAtLeast(min + 1f)
    val clamped = value.coerceIn(min, safeMax)

    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label.uppercase(),
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
            Text(
                text = formattedValue,
                color = valueColor,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold
            )
        }
        Slider(
            value = clamped,
            onValueChange = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onValueChange(it)
            },
            valueRange = min..safeMax,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = valueColor,
                inactiveTrackColor = DeepObsidian
            ),
            modifier = Modifier
                .height(28.dp)
                .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier)
        )
    }
}

@Composable
fun LegendDot(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, RoundedCornerShape(4.dp))
        )
        Text(label, color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun TrajectoryChart(
    trajectory: List<TrajectoryPoint>,
    formatShort: (Double) -> String,
    modifier: Modifier = Modifier
) {
    val rawMax = trajectory.maxOfOrNull { maxOf(it.grossBalance, it.netBalance, it.targetBalance ?: 0.0) } ?: 1.0
    val maxVal = if (rawMax <= 0.0) 1.0 else rawMax * 1.08

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceCard, RoundedCornerShape(16.dp))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Peak ${formatShort(rawMax)}", color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Text("Mid ${formatShort(maxVal * 0.5)}", color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(155.dp)
        ) {
            if (trajectory.size < 2) return@Canvas

            val width = size.width
            val height = size.height
            val xStep = width / (trajectory.size - 1).coerceAtLeast(1).toFloat()

            fun getY(value: Double): Float {
                val norm = (value / maxVal).coerceIn(0.0, 1.0)
                return (height - norm * height).toFloat()
            }

            // Subtle horizontal guide lines
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            listOf(0.25f, 0.5f, 0.75f).forEach { ratio ->
                val y = height * ratio
                drawLine(
                    color = Color.White.copy(alpha = 0.06f),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dashEffect
                )
            }

            // Target FIRE dashed line
            val targetPath = Path()
            targetPath.moveTo(0f, getY(trajectory.first().targetBalance ?: 0.0))
            trajectory.forEachIndexed { index, pt ->
                targetPath.lineTo(index * xStep, getY(pt.targetBalance ?: 0.0))
            }
            drawPath(
                path = targetPath,
                color = RoseCrimson,
                style = Stroke(width = 1.8.dp.toPx(), pathEffect = dashEffect)
            )

            // Gross area gradient
            val grossFill = Path().apply {
                moveTo(0f, height)
                trajectory.forEachIndexed { index, pt ->
                    lineTo(index * xStep, getY(pt.grossBalance))
                }
                lineTo((trajectory.size - 1) * xStep, height)
                close()
            }
            drawPath(
                path = grossFill,
                brush = Brush.verticalGradient(
                    listOf(EmeraldGreen.copy(alpha = 0.16f), Color.Transparent)
                )
            )

            // Net balance line
            val netPath = Path()
            netPath.moveTo(0f, getY(trajectory.first().netBalance))
            trajectory.forEachIndexed { index, pt ->
                netPath.lineTo(index * xStep, getY(pt.netBalance))
            }
            drawPath(netPath, color = CyanAccent, style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round))

            // Gross balance line
            val grossPath = Path()
            grossPath.moveTo(0f, getY(trajectory.first().grossBalance))
            trajectory.forEachIndexed { index, pt ->
                grossPath.lineTo(index * xStep, getY(pt.grossBalance))
            }
            drawPath(grossPath, color = EmeraldGreen, style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val startAge = trajectory.firstOrNull()?.age ?: 0
            val midAge = trajectory.getOrNull(trajectory.size / 2)?.age ?: startAge
            val endAge = trajectory.lastOrNull()?.age ?: startAge
            Text("Age $startAge", color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Text("Age $midAge", color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Text("Age $endAge", color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun DualSeriesChart(
    primaryValues: List<Double>,
    secondaryValues: List<Double>?,
    primaryColor: Color,
    secondaryColor: Color,
    xStartLabel: String,
    xEndLabel: String,
    formatShort: (Double) -> String,
    modifier: Modifier = Modifier
) {
    val maxPrimary = primaryValues.maxOrNull() ?: 0.0
    val maxSecondary = secondaryValues?.maxOrNull() ?: 0.0
    val rawMax = maxOf(maxPrimary, maxSecondary)
    val maxVal = if (rawMax <= 0.0) 1.0 else rawMax * 1.08

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceCard, RoundedCornerShape(16.dp))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Text(
            text = "Max ${formatShort(rawMax)}",
            color = TextMuted,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(6.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            if (primaryValues.size < 2) return@Canvas
            val width = size.width
            val height = size.height
            val xStep = width / (primaryValues.size - 1).coerceAtLeast(1).toFloat()

            fun getY(v: Double): Float {
                val norm = (v / maxVal).coerceIn(0.0, 1.0)
                return (height - norm * height).toFloat()
            }

            if (secondaryValues != null && secondaryValues.size == primaryValues.size) {
                val secPath = Path()
                secPath.moveTo(0f, getY(secondaryValues.first()))
                secondaryValues.forEachIndexed { idx, v -> secPath.lineTo(idx * xStep, getY(v)) }
                drawPath(secPath, color = secondaryColor, style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round))
            }

            val fillPath = Path().apply {
                moveTo(0f, height)
                primaryValues.forEachIndexed { idx, v -> lineTo(idx * xStep, getY(v)) }
                lineTo((primaryValues.size - 1) * xStep, height)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(listOf(primaryColor.copy(alpha = 0.18f), Color.Transparent))
            )

            val priPath = Path()
            priPath.moveTo(0f, getY(primaryValues.first()))
            primaryValues.forEachIndexed { idx, v -> priPath.lineTo(idx * xStep, getY(v)) }
            drawPath(priPath, color = primaryColor, style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(xStartLabel, color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Text(xEndLabel, color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun MonteCarloFanChart(
    mcResult: MonteCarloResult,
    formatShort: (Double) -> String,
    modifier: Modifier = Modifier
) {
    val rawMax = mcResult.percentile90.maxOrNull() ?: 1.0
    val maxVal = if (rawMax <= 0.0) 1.0 else rawMax * 1.05

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceCard, RoundedCornerShape(16.dp))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("40-Yr Wealth Fan (P10 / P50 / P90)", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LegendDot("P90", EmeraldGreen)
                LegendDot("P50", CyanAccent)
                LegendDot("P10", AmberGold)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text("90th Peak: ${formatShort(rawMax)}", color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        Spacer(modifier = Modifier.height(6.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            val count = mcResult.percentile50.size
            if (count < 2) return@Canvas
            val width = size.width
            val height = size.height
            val xStep = width / (count - 1).coerceAtLeast(1).toFloat()

            fun getY(v: Double): Float {
                val norm = (v / maxVal).coerceIn(0.0, 1.0)
                return (height - norm * height).toFloat()
            }

            // Shaded confidence cone between P90 and P10
            val conePath = Path().apply {
                moveTo(0f, getY(mcResult.percentile90.first()))
                mcResult.percentile90.forEachIndexed { i, v -> lineTo(i * xStep, getY(v)) }
                for (i in count - 1 downTo 0) {
                    lineTo(i * xStep, getY(mcResult.percentile10[i]))
                }
                close()
            }
            drawPath(
                path = conePath,
                brush = Brush.verticalGradient(listOf(CyanAccent.copy(alpha = 0.16f), AmberGold.copy(alpha = 0.04f)))
            )

            val p90Path = Path().apply {
                moveTo(0f, getY(mcResult.percentile90.first()))
                mcResult.percentile90.forEachIndexed { i, v -> lineTo(i * xStep, getY(v)) }
            }
            drawPath(p90Path, color = EmeraldGreen, style = Stroke(width = 2.dp.toPx()))

            val p50Path = Path().apply {
                moveTo(0f, getY(mcResult.percentile50.first()))
                mcResult.percentile50.forEachIndexed { i, v -> lineTo(i * xStep, getY(v)) }
            }
            drawPath(p50Path, color = CyanAccent, style = Stroke(width = 2.8.dp.toPx()))

            val p10Path = Path().apply {
                moveTo(0f, getY(mcResult.percentile10.first()))
                mcResult.percentile10.forEachIndexed { i, v -> lineTo(i * xStep, getY(v)) }
            }
            drawPath(p10Path, color = AmberGold, style = Stroke(width = 2.dp.toPx()))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Yr 0", color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Text("Yr 20", color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Text("Yr 40", color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun AdMobBanner(modifier: Modifier = Modifier) {
    var adFailed by remember { mutableStateOf(false) }
    if (adFailed) return

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceCard)
            .border(1.dp, Color.White.copy(alpha = 0.05f))
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        val adWidthDp = maxWidth.value.toInt().coerceIn(320, 468)
        AndroidView(
            modifier = Modifier.wrapContentSize(),
            factory = { ctx ->
                val adaptiveSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, adWidthDp)
                AdView(ctx).apply {
                    setAdSize(adaptiveSize)
                    adUnitId = AdMobService.BANNER_AD_UNIT_ID
                    adListener = object : AdListener() {
                        override fun onAdFailedToLoad(error: LoadAdError) {
                            adFailed = true
                        }
                    }
                    loadAd(AdRequest.Builder().build())
                }
            }
        )
    }
}
