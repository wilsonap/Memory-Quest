package com.example.ui.screens.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.PlayerEntity
import com.example.data.local.entity.StatisticsEntity
import com.example.ui.theme.ImmersiveGold
import com.example.ui.theme.ImmersiveGreen
import com.example.ui.theme.ImmersivePrimary
import com.example.ui.theme.ImmersiveSecondary
import com.example.ui.theme.ImmersiveSurface
import com.example.ui.theme.ImmersiveTextPrimary
import com.example.ui.theme.ImmersiveTextSecondary

data class StatItem(
    val title: String,
    val value: String,
    val icon: ImageVector,
    val accentColor: Color
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatsGridSection(
    player: PlayerEntity?,
    stats: StatisticsEntity?,
    modifier: Modifier = Modifier
) {
    val totalGames = stats?.totalGames ?: 0
    val wins = stats?.wins ?: 0
    val losses = (totalGames - wins).coerceAtLeast(0)

    val winRate = if (totalGames > 0) (wins.toFloat() / totalGames.toFloat() * 100f) else 0f

    val totalFlips = stats?.totalFlips ?: 0
    val correctFlips = stats?.correctFlips ?: 0
    val accuracy = if (totalFlips > 0) (correctFlips.toFloat() / totalFlips.toFloat() * 100f) else 0f
    val wrongFlips = (totalFlips - correctFlips).coerceAtLeast(0)

    val totalTime = stats?.totalTimeSeconds ?: 0L
    val avgTimeSec = if (totalGames > 0) (totalTime / totalGames) else 0L
    val bestTimeSec = if (wins > 0) (totalTime / wins) else 0L

    val highestLevel = player?.highestLevel ?: 1
    val maxScore = (highestLevel * 1000L) + ((stats?.totalPairsFound ?: 0) * 10L)

    val coinsEarned = stats?.totalCoinsEarned ?: 0
    val currentCoins = player?.coins ?: 0
    val coinsSpent = (coinsEarned - currentCoins).coerceAtLeast(0)
    val boostersUsed = (totalFlips / 12).coerceAtLeast(0)

    val items = listOf(
        StatItem("Partidas", if (totalGames > 0) "$totalGames" else "--", Icons.Default.PlayCircle, Color(0xFF4CC9F0)),
        StatItem("Vitórias", if (totalGames > 0) "$wins" else "--", Icons.Default.CheckCircle, ImmersiveGreen),
        StatItem("Derrotas", if (totalGames > 0) "$losses" else "--", Icons.Default.Cancel, Color(0xFFFF5252)),
        StatItem("Taxa de Vitória", if (totalGames > 0) String.format("%.1f%%", winRate) else "--", Icons.Default.TrendingUp, ImmersiveGreen),
        StatItem("Precisão", if (totalFlips > 0) String.format("%.1f%%", accuracy) else "--", Icons.Default.Percent, ImmersivePrimary),
        StatItem("Pares Encontrados", if (stats != null) "${stats.totalPairsFound}" else "--", Icons.Default.Extension, ImmersiveGreen),
        StatItem("Cartas Viradas", if (totalFlips > 0) "$totalFlips" else "--", Icons.Default.Flip, ImmersiveSecondary),
        StatItem("Tentativas Erradas", if (totalFlips > 0) "$wrongFlips" else "--", Icons.Default.Shield, Color(0xFFFF7B00)),
        StatItem("Maior Sequência", if (stats != null && stats.highestStreak > 0) "${stats.highestStreak}x" else "--", Icons.Default.Bolt, ImmersiveSecondary),
        StatItem("Tempo Total", if (totalTime > 0) formatHoursMinutes(totalTime) else "--", Icons.Default.Timer, Color(0xFF4CC9F0)),
        StatItem("Tempo Médio", if (avgTimeSec > 0) "${avgTimeSec}s" else "--", Icons.Default.Speed, Color(0xFF4CC9F0)),
        StatItem("Melhor Tempo", if (bestTimeSec > 0) "${bestTimeSec}s" else "--", Icons.Default.AutoAwesome, ImmersiveGold),
        StatItem("Maior Fase", "$highestLevel", Icons.Default.EmojiEvents, ImmersiveGold),
        StatItem("Maior Pontuação", "$maxScore", Icons.Default.Star, ImmersiveGold),
        StatItem("Vitórias Perfeitas", if (stats != null) "${stats.totalFlawlessWins}" else "--", Icons.Default.AutoAwesome, ImmersiveGold),
        StatItem("Dias Consecutivos", if (stats != null) "${stats.consecutiveDays} d" else "--", Icons.Default.CalendarToday, ImmersivePrimary),
        StatItem("Moedas Ganhas", "$coinsEarned", Icons.Default.MonetizationOn, Color(0xFFFFB703)),
        StatItem("Moedas Gastas", "$coinsSpent", Icons.Default.PointOfSale, Color(0xFFFF7B00)),
        StatItem("Boosters Usados", "$boostersUsed", Icons.Default.FlashOn, ImmersiveSecondary)
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "REGISTROS E DESEMPENHO",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = ImmersiveGold,
                letterSpacing = 1.sp
            ),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            maxItemsInEachRow = 2
        ) {
            items.forEach { item ->
                Box(modifier = Modifier.weight(1f)) {
                    SingleStatCard(item = item)
                }
            }
        }
    }
}

@Composable
private fun SingleStatCard(item: StatItem) {
    Surface(
        color = ImmersiveSurface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, item.accentColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(item.accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = item.accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = item.value,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = ImmersiveTextPrimary,
                        fontSize = 15.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = item.title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = ImmersiveTextSecondary,
                        fontSize = 10.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun formatHoursMinutes(totalSeconds: Long): String {
    val hrs = totalSeconds / 3600
    val mins = (totalSeconds % 3600) / 60
    return if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"
}
