package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.FiAchievement
import com.example.models.FiAchievementCategory
import com.example.models.FiScoreSummary
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
import kotlin.math.roundToInt

@Composable
fun FiScoreCompactBanner(
    scoreSummary: FiScoreSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        SurfaceElevated,
                        EmeraldGreen.copy(alpha = 0.08f)
                    )
                )
            )
            .border(1.dp, AmberGold.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("fi_score_compact_banner"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(AmberGold.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.EmojiEvents,
                    contentDescription = "Financial Independence Score",
                    tint = AmberGold,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    text = "FI SCORE • ${scoreSummary.rank.title.uppercase()}",
                    color = AmberGold,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.9.sp
                )
                Text(
                    text = "${scoreSummary.unlockedCount}/${scoreSummary.totalAchievements} badges unlocked • Tap for milestones",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${scoreSummary.totalScore}",
                color = EmeraldGreen,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "/1000",
                color = TextMuted,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = "Open achievements",
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun FiScoreGamificationSection(
    scoreSummary: FiScoreSummary,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf<FiAchievementCategory?>(null) }

    val filteredAchievements = remember(scoreSummary.achievements, selectedFilter) {
        val base = if (selectedFilter == null) {
            scoreSummary.achievements
        } else {
            scoreSummary.achievements.filter { it.category == selectedFilter }
        }
        base.sortedByDescending { it.isUnlocked }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, EmeraldGreen.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
            .testTag("fi_score_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EmojiEvents,
                            contentDescription = null,
                            tint = AmberGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "FINANCIAL INDEPENDENCE SCORE",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.1.sp
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = scoreSummary.rank.title,
                        color = TextPrimary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = scoreSummary.rank.tagline,
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }

                // Dynamic Canvas Radial Score Gauge
                Box(
                    modifier = Modifier.size(98.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val ratio = (scoreSummary.totalScore / scoreSummary.maxScore.toFloat()).coerceIn(0f, 1f)
                    Canvas(modifier = Modifier.size(94.dp)) {
                        val strokePx = 9.dp.toPx()
                        val diameter = minOf(size.width, size.height) - strokePx
                        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                        val arcSize = Size(diameter, diameter)

                        drawArc(
                            color = Color.White.copy(alpha = 0.1f),
                            startAngle = 140f,
                            sweepAngle = 260f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round)
                        )
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(CyanAccent, EmeraldGreen, AmberGold)
                            ),
                            startAngle = 140f,
                            sweepAngle = 260f * ratio,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${scoreSummary.totalScore}",
                            color = EmeraldGreen,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.testTag("fi_score_value")
                        )
                        Text(
                            text = "/ ${scoreSummary.maxScore}",
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            if (scoreSummary.nextRank != null) {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceElevated, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = AmberGold, modifier = Modifier.size(14.dp))
                        Text(
                            text = "Next Rank: ${scoreSummary.nextRank.title}",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = "${scoreSummary.pointsToNextRank} pts left",
                        color = AmberGold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // 3 Score Pillar Breakdown Bars
            ScorePillarBar("Portfolio & Net Worth", scoreSummary.portfolioScore, 450, EmeraldGreen)
            Spacer(Modifier.height(6.dp))
            ScorePillarBar("Debt Freedom & Payoff", scoreSummary.debtFreedomScore, 300, CyanAccent)
            Spacer(Modifier.height(6.dp))
            ScorePillarBar("Cashflow & Savings Discipline", scoreSummary.cashflowScore, 250, AmberGold)

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MILESTONE ACHIEVEMENTS",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${scoreSummary.unlockedCount} / ${scoreSummary.totalAchievements} Unlocked",
                    color = EmeraldGreen,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("fi_unlocked_count")
                )
            }

            Spacer(Modifier.height(8.dp))

            // Category Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == null,
                    onClick = { selectedFilter = null },
                    label = { Text("All", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmeraldGreen,
                        selectedLabelColor = DeepObsidian,
                        containerColor = SurfaceElevated,
                        labelColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("fi_filter_all")
                )
                FiAchievementCategory.entries.forEach { category ->
                    FilterChip(
                        selected = selectedFilter == category,
                        onClick = { selectedFilter = category },
                        label = { Text(category.label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanAccent,
                            selectedLabelColor = DeepObsidian,
                            containerColor = SurfaceElevated,
                            labelColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("fi_filter_${category.name.lowercase()}")
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            filteredAchievements.forEach { achievement ->
                AchievementRowCard(achievement = achievement)
            }
        }
    }
}

@Composable
private fun ScorePillarBar(
    label: String,
    score: Int,
    maxScore: Int,
    color: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = TextSecondary, fontSize = 11.sp)
            Text(
                text = "$score / $maxScore",
                color = color,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White.copy(alpha = 0.08f))
        ) {
            val fill = (score.toFloat() / maxScore.toFloat()).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth(fill)
                    .fillMaxHeight()
                    .background(color)
            )
        }
    }
}

@Composable
private fun AchievementRowCard(achievement: FiAchievement) {
    val accent = if (achievement.isUnlocked) EmeraldGreen else TextMuted
    val borderColor = if (achievement.isUnlocked) EmeraldGreen.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.07f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(SurfaceElevated, RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(12.dp)
            .testTag("achievement_${achievement.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    if (achievement.isUnlocked) EmeraldGreen.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.06f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (achievement.isUnlocked) Icons.Rounded.Verified else Icons.Rounded.Lock,
                contentDescription = if (achievement.isUnlocked) "${achievement.title} unlocked" else "${achievement.title} locked",
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = achievement.title,
                    color = if (achievement.isUnlocked) TextPrimary else TextSecondary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .background(
                            if (achievement.isUnlocked) AmberGold.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.07f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "+${achievement.points} PTS",
                        color = if (achievement.isUnlocked) AmberGold else TextMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = achievement.description,
                color = TextMuted,
                fontSize = 11.sp
            )
            Spacer(Modifier.height(5.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    val fill = achievement.progress.toFloat().coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fill)
                            .fillMaxHeight()
                            .background(if (achievement.isUnlocked) EmeraldGreen else CyanAccent)
                    )
                }
                Text(
                    text = if (achievement.isUnlocked) "UNLOCKED" else achievement.progressLabel,
                    color = if (achievement.isUnlocked) EmeraldGreen else CyanAccent,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
