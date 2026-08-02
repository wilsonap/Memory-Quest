package com.example.ui.screens.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.data.local.entity.PlayerEntity
import com.example.data.local.entity.StatisticsEntity
import com.example.ui.components.TopGameBar

@Composable
fun StatsScreen(
    player: PlayerEntity?,
    stats: StatisticsEntity?,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalGames = stats?.totalGames ?: 0
    val totalFlips = stats?.totalFlips ?: 0
    val correctFlips = stats?.correctFlips ?: 0
    val accuracy = if (totalFlips > 0) (correctFlips.toFloat() / totalFlips.toFloat() * 100f) else 0f
    val winRate = if (totalGames > 0) ((stats?.wins ?: 0).toFloat() / totalGames.toFloat() * 100f) else 0f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF1B1038),
                        Color(0xFF0F0824)
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopGameBar(
                coins = player?.coins ?: 0,
                title = stringResource(R.string.stats_title),
                onBackClick = onBackClick
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    StatCard(
                        title = stringResource(R.string.stats_highest_level),
                        value = "${player?.highestLevel ?: 1}",
                        icon = Icons.Default.EmojiEvents,
                        color = Color(0xFFFFB703)
                    )
                }

                item {
                    StatCard(
                        title = stringResource(R.string.stats_time_played),
                        value = formatHoursMinutes(stats?.totalTimeSeconds ?: 0),
                        icon = Icons.Default.Timer,
                        color = Color(0xFF4CC9F0)
                    )
                }

                item {
                    StatCard(
                        title = stringResource(R.string.stats_total_coins),
                        value = "${stats?.totalCoinsEarned ?: 0} 💰",
                        icon = Icons.Default.MonetizationOn,
                        color = Color(0xFFFFB703)
                    )
                }

                item {
                    StatCard(
                        title = stringResource(R.string.stats_pairs_found),
                        value = "${stats?.totalPairsFound ?: 0}",
                        icon = Icons.Default.Extension,
                        color = Color(0xFF06D6A0)
                    )
                }

                item {
                    StatCard(
                        title = stringResource(R.string.stats_accuracy),
                        value = String.format("%.1f%%", accuracy),
                        icon = Icons.Default.Percent,
                        color = Color(0xFFE0AAFF)
                    )
                }

                item {
                    StatCard(
                        title = stringResource(R.string.stats_win_rate),
                        value = String.format("%.1f%%", winRate),
                        icon = Icons.Default.CheckCircle,
                        color = Color(0xFF06D6A0)
                    )
                }

                item {
                    StatCard(
                        title = stringResource(R.string.stats_games_played),
                        value = "$totalGames",
                        icon = Icons.Default.PlayCircle,
                        color = Color(0xFF4CC9F0)
                    )
                }

                item {
                    StatCard(
                        title = "Maior Combo",
                        value = "${stats?.highestStreak ?: 0}x",
                        icon = Icons.Default.Bolt,
                        color = Color(0xFFFF70A6)
                    )
                }

                item {
                    StatCard(
                        title = "Vitórias Perfeitas",
                        value = "${stats?.totalFlawlessWins ?: 0}",
                        icon = Icons.Default.AutoAwesome,
                        color = Color(0xFFFFD700)
                    )
                }

                item {
                    StatCard(
                        title = "Dias Consecutivos",
                        value = "${stats?.consecutiveDays ?: 1} dia(s)",
                        icon = Icons.Default.CalendarToday,
                        color = Color(0xFFE0AAFF)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Surface(
        color = Color(0xFF221742),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color.White
                )
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color.White.copy(alpha = 0.7f)
                )
            )
        }
    }
}

private fun formatHoursMinutes(totalSeconds: Long): String {
    val hrs = totalSeconds / 3600
    val mins = (totalSeconds % 3600) / 60
    return if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"
}
