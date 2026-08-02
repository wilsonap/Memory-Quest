package com.example.ui.screens.ranking

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.data.local.entity.PlayerEntity
import com.example.data.local.entity.StatisticsEntity
import com.example.data.repository.LeaderboardPlayer
import com.example.ui.components.BannerAdView
import com.example.ui.components.TopGameBar
import com.example.ui.theme.ImmersiveBg
import com.example.ui.theme.ImmersiveGold
import com.example.ui.theme.ImmersiveGreen
import com.example.ui.theme.ImmersivePrimary
import com.example.ui.theme.ImmersivePrimaryContainer
import com.example.ui.theme.ImmersiveSecondary
import com.example.ui.theme.ImmersiveSurface
import com.example.ui.theme.ImmersiveSurfaceVariant
import com.example.ui.theme.ImmersiveTextPrimary
import com.example.ui.theme.ImmersiveTextSecondary

@Composable
fun RankingScreen(
    player: PlayerEntity?,
    stats: StatisticsEntity?,
    leaderboardList: List<LeaderboardPlayer>,
    isLoading: Boolean,
    errorMessage: String?,
    onRefresh: () -> Unit,
    onBackClick: () -> Unit,
    isAdsRemoved: Boolean = false,
    modifier: Modifier = Modifier
) {
    val playerName = player?.name?.ifEmpty { "Explorador" } ?: "Explorador"
    val playerHighestLevel = player?.highestLevel?.toLong() ?: 1L
    val playerTotalPairs = stats?.totalPairsFound?.toLong() ?: 0L
    val playerBestStreak = stats?.highestStreak?.toLong() ?: 0L
    val playerGames = stats?.totalGames?.toLong() ?: 0L
    val calculatedScore = (playerHighestLevel * 1000L) + (playerTotalPairs * 10L) + (playerBestStreak * 50L) + (playerGames * 20L)

    val currentUserInLeaderboard = leaderboardList.find { it.isCurrentUser }
    val userRank = currentUserInLeaderboard?.rank ?: if (leaderboardList.isNotEmpty()) leaderboardList.size + 1 else 1

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ImmersiveBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopGameBar(
                coins = player?.coins ?: 0,
                title = stringResource(R.string.ranking_title),
                onBackClick = onBackClick
            )

            // Status Banner (Cloud Firestore sync status)
            Surface(
                color = ImmersiveSurface,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .border(1.dp, ImmersiveSurfaceVariant, RoundedCornerShape(20.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (errorMessage == null) Icons.Default.CloudDone else Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = if (errorMessage == null) ImmersiveGreen else ImmersiveSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (errorMessage == null) stringResource(R.string.ranking_firebase_online) else stringResource(R.string.ranking_offline_mode),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = ImmersiveTextPrimary
                                )
                            )
                            Text(
                                text = errorMessage ?: stringResource(R.string.ranking_sync_realtime),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = ImmersiveTextSecondary
                                ),
                                maxLines = 1
                            )
                        }
                    }

                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = ImmersivePrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier.testTag("ranking_refresh_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.ranking_refresh),
                                tint = ImmersivePrimary
                            )
                        }
                    }
                }
            }

            // Current User Stats Card
            Surface(
                color = ImmersivePrimaryContainer,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .border(1.5.dp, ImmersivePrimary.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(ImmersivePrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "#$userRank",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF1D192B)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "$playerName (Você)",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = ImmersiveTextPrimary
                                    )
                                )
                            }
                            Text(
                                text = "Pontuação: $calculatedScore pts • Fase $playerHighestLevel",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = ImmersiveTextSecondary
                                )
                            )
                        }
                    }

                    Surface(
                        color = ImmersivePrimary,
                        shape = CircleShape
                    ) {
                        Text(
                            text = "${playerTotalPairs} Pares",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1D192B)
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Top 100 Players List
            if (leaderboardList.isEmpty() && !isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = ImmersiveGold.copy(alpha = 0.6f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Ainda não há registros no ranking online.",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = ImmersiveTextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            text = "Toque em atualizar ou jogue uma partida para inaugurar!",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = ImmersiveTextSecondary
                            )
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(leaderboardList) { _, entry ->
                        val rankColor = when (entry.rank) {
                            1 -> ImmersiveGold
                            2 -> Color(0xFFE0E0E0)
                            3 -> Color(0xFFCD7F32)
                            else -> ImmersivePrimary
                        }

                        Surface(
                            color = if (entry.isCurrentUser) ImmersivePrimaryContainer.copy(alpha = 0.8f) else ImmersiveSurface,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (entry.isCurrentUser) 2.dp else 1.dp,
                                    color = if (entry.isCurrentUser) ImmersiveGold else ImmersiveSurfaceVariant,
                                    shape = RoundedCornerShape(20.dp)
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(rankColor.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "#${entry.rank}",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                color = rankColor
                                            )
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = entry.name,
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (entry.isCurrentUser) ImmersiveGold else ImmersiveTextPrimary
                                                )
                                            )
                                            if (entry.isCurrentUser) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = "Você",
                                                    tint = ImmersiveGold,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "${entry.totalScore} pts • ${entry.totalPairs} pares",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = ImmersiveTextSecondary
                                            )
                                        )
                                    }
                                }

                                Surface(
                                    color = ImmersiveSurfaceVariant,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "Fase ${entry.highestLevel}",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = ImmersiveTextPrimary
                                        ),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            BannerAdView(isAdsRemoved = isAdsRemoved)
        }
    }
}
