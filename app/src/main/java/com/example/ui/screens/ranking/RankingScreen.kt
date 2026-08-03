package com.example.ui.screens.ranking

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.avatar.model.AvatarType
import com.example.avatar.ui.AvatarImage
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

enum class RankingTab {
    GLOBAL, WEEKLY, MONTHLY
}

@Composable
fun RankingScreen(
    player: PlayerEntity?,
    stats: StatisticsEntity?,
    leaderboardList: List<LeaderboardPlayer>,
    isLoading: Boolean,
    errorMessage: String?,
    lastFetchTime: Long = 0L,
    isOnline: Boolean = true,
    onRefresh: () -> Unit,
    onBackClick: () -> Unit,
    isAdsRemoved: Boolean = false,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(RankingTab.GLOBAL) }

    val playerName = player?.name?.ifEmpty { "Explorador" } ?: "Explorador"
    val playerHighestLevel = player?.highestLevel?.toLong() ?: 1L
    val playerTotalPairs = stats?.totalPairsFound?.toLong() ?: 0L
    val playerBestStreak = stats?.highestStreak?.toLong() ?: 0L
    val playerGames = stats?.totalGames?.toLong() ?: 0L
    val calculatedScore = (playerHighestLevel * 1000L) + (playerTotalPairs * 10L) + (playerBestStreak * 50L) + (playerGames * 20L)

    // Current player in leaderboard list (if present)
    val currentUserInLeaderboard = remember(leaderboardList) {
        leaderboardList.find { it.isCurrentUser }
    }

    // Rank display text for current player
    val userRankDisplay = remember(currentUserInLeaderboard, leaderboardList) {
        when {
            currentUserInLeaderboard != null -> "#${currentUserInLeaderboard.rank}"
            leaderboardList.isNotEmpty() -> "Fora do Top 100"
            else -> "Sua Posição"
        }
    }

    // Filter out current player from list to prevent double rendering
    val otherPlayersInLeaderboard = remember(leaderboardList) {
        leaderboardList.distinctBy { it.uid }.filterNot { it.isCurrentUser }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ImmersiveBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header bar
            TopGameBar(
                coins = player?.coins ?: 0,
                title = stringResource(R.string.ranking_title),
                onBackClick = onBackClick
            )

            // Subtitle & Compact Status Bar
            CompactStatusBar(
                isLoading = isLoading,
                errorMessage = errorMessage,
                lastFetchTime = lastFetchTime,
                hasData = leaderboardList.isNotEmpty(),
                isOnline = isOnline,
                onRefresh = onRefresh
            )

            // Horizontal Tabs (Global, Semanal, Mensal)
            RankingTabsRow(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )

            Spacer(modifier = Modifier.height(8.dp))

            when (selectedTab) {
                RankingTab.GLOBAL -> {
                    // Fixed "Minha Posição" card at top
                    MyPositionCard(
                        userRankDisplay = userRankDisplay,
                        playerName = playerName,
                        playerScore = calculatedScore,
                        playerHighestLevel = playerHighestLevel,
                        playerTotalPairs = playerTotalPairs,
                        avatarType = player?.avatarType ?: AvatarType.PRESET.name,
                        avatarPresetId = player?.avatarPresetId ?: "avatar_01",
                        avatarLocalPath = player?.avatarLocalPath ?: ""
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // List Section Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "TOP 100 JOGADORES",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = ImmersiveGold,
                                letterSpacing = 1.sp
                            )
                        )

                        Text(
                            text = if (otherPlayersInLeaderboard.isNotEmpty()) {
                                "${otherPlayersInLeaderboard.size + if (currentUserInLeaderboard != null) 1 else 0} classificados"
                            } else "",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = ImmersiveTextSecondary
                            )
                        )
                    }

                    // Main Content Body (Loading Skeletons, Empty, Error, or List)
                    Box(modifier = Modifier.weight(1f)) {
                        when {
                            isLoading && leaderboardList.isEmpty() -> {
                                SkeletonLoadingList()
                            }
                            !isLoading && leaderboardList.isEmpty() && errorMessage != null -> {
                                ErrorOfflineView(onRetry = onRefresh)
                            }
                            !isLoading && leaderboardList.isEmpty() -> {
                                EmptyLeaderboardView(onRefresh = onRefresh)
                            }
                            else -> {
                                Top100List(
                                    players = otherPlayersInLeaderboard,
                                    player = player
                                )
                            }
                        }
                    }
                }
                RankingTab.WEEKLY -> {
                    ComingSoonTab(tabName = "Semanal")
                }
                RankingTab.MONTHLY -> {
                    ComingSoonTab(tabName = "Mensal")
                }
            }

            BannerAdView(isAdsRemoved = isAdsRemoved)
        }
    }
}

@Composable
private fun CompactStatusBar(
    isLoading: Boolean,
    errorMessage: String?,
    lastFetchTime: Long,
    hasData: Boolean,
    isOnline: Boolean,
    onRefresh: () -> Unit
) {
    Surface(
        color = ImmersiveSurface.copy(alpha = 0.8f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .border(1.dp, ImmersiveSurfaceVariant, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                val statusColor = when {
                    isOnline && errorMessage == null -> ImmersiveGreen
                    !isOnline && hasData -> Color(0xFFFFB74D) // Orange cache warning
                    isOnline && hasData -> Color(0xFFFFB74D)
                    else -> MaterialTheme.colorScheme.error
                }

                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    val statusTitle = when {
                        isOnline && errorMessage == null -> "🟢 Online"
                        !isOnline && hasData -> "🟡 Offline"
                        !isOnline -> "🔴 Offline"
                        hasData -> "🟡 Dados salvos no dispositivo"
                        else -> "🔴 Sem conexão"
                    }

                    Text(
                        text = statusTitle,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = ImmersiveTextPrimary
                        )
                    )

                    val timeText = when {
                        isOnline && errorMessage == null -> formatRelativeTime(lastFetchTime)
                        !isOnline && hasData -> "Exibindo último ranking salvo"
                        !isOnline -> "Sem conexão com a internet"
                        hasData -> "Modo offline • Exibindo cache local"
                        else -> errorMessage ?: "Não foi possível conectar ao servidor"
                    }

                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = ImmersiveTextSecondary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = ImmersiveGold,
                    strokeWidth = 2.dp
                )
            } else {
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("ranking_refresh_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.ranking_refresh),
                        tint = ImmersiveGold,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RankingTabsRow(
    selectedTab: RankingTab,
    onTabSelected: (RankingTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(ImmersiveSurfaceVariant.copy(alpha = 0.5f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        RankingTabPill(
            title = "Global",
            icon = Icons.Default.Public,
            isSelected = selectedTab == RankingTab.GLOBAL,
            onClick = { onTabSelected(RankingTab.GLOBAL) },
            testTag = "ranking_tab_global",
            modifier = Modifier.weight(1f)
        )
        RankingTabPill(
            title = "Semanal",
            icon = Icons.Default.EmojiEvents,
            isSelected = selectedTab == RankingTab.WEEKLY,
            onClick = { onTabSelected(RankingTab.WEEKLY) },
            testTag = "ranking_tab_weekly",
            modifier = Modifier.weight(1f)
        )
        RankingTabPill(
            title = "Mensal",
            icon = Icons.Default.DateRange,
            isSelected = selectedTab == RankingTab.MONTHLY,
            onClick = { onTabSelected(RankingTab.MONTHLY) },
            testTag = "ranking_tab_monthly",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun RankingTabPill(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) ImmersivePrimaryContainer else Color.Transparent
    val contentColor = if (isSelected) ImmersiveGold else ImmersiveTextSecondary
    val borderColor = if (isSelected) ImmersivePrimary.copy(alpha = 0.8f) else Color.Transparent

    Box(
        modifier = modifier
            .testTag(testTag)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                    color = contentColor
                )
            )
        }
    }
}

@Composable
private fun MyPositionCard(
    userRankDisplay: String,
    playerName: String,
    playerScore: Long,
    playerHighestLevel: Long,
    playerTotalPairs: Long,
    avatarType: String,
    avatarPresetId: String,
    avatarLocalPath: String
) {
    Surface(
        color = ImmersivePrimaryContainer,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .border(1.5.dp, ImmersiveGold.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = "MINHA POSIÇÃO",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = ImmersiveGold,
                    letterSpacing = 1.sp
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Position Badge
                    Box(
                        modifier = Modifier
                            .height(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ImmersivePrimary)
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userRankDisplay,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF1D192B)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Player Avatar
                    AvatarImage(
                        avatarType = avatarType,
                        avatarPresetId = avatarPresetId,
                        avatarLocalPath = avatarLocalPath,
                        size = 44.dp
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    // Name and stats
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = playerName,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = ImmersiveTextPrimary
                                ),
                                modifier = Modifier.weight(1f, fill = false)
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            // Compact VOCÊ badge
                            Surface(
                                color = ImmersiveGold,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "VOCÊ",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF1D192B)
                                    ),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "${playerScore} pts • Fase $playerHighestLevel • $playerTotalPairs pares",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = ImmersiveTextSecondary
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Top100List(
    players: List<LeaderboardPlayer>,
    player: PlayerEntity?
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            items = players,
            key = { it.uid }
        ) { entry ->
            LeaderboardRowItem(entry = entry, player = player)
        }
    }
}

@Composable
private fun LeaderboardRowItem(
    entry: LeaderboardPlayer,
    player: PlayerEntity?
) {
    val isTop1 = entry.rank == 1
    val isTop2 = entry.rank == 2
    val isTop3 = entry.rank == 3

    val (rankColor, borderColor, cardBgColor) = when {
        isTop1 -> Triple(
            ImmersiveGold,
            ImmersiveGold,
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF2D2305),
                    ImmersiveSurface
                )
            )
        )
        isTop2 -> Triple(
            Color(0xFFE0E0E0),
            Color(0xFFC0C0C0),
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF1F2228),
                    ImmersiveSurface
                )
            )
        )
        isTop3 -> Triple(
            Color(0xFFCD7F32),
            Color(0xFFCD7F32),
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF23180E),
                    ImmersiveSurface
                )
            )
        )
        else -> Triple(
            ImmersivePrimary,
            ImmersiveSurfaceVariant,
            Brush.linearGradient(
                colors = listOf(
                    ImmersiveSurface,
                    ImmersiveSurface
                )
            )
        )
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isTop1) 1.8.dp else if (isTop2 || isTop3) 1.2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBgColor)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Rank badge
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

                    Spacer(modifier = Modifier.width(10.dp))

                    // Crown for #1
                    if (isTop1) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Campeão",
                            tint = ImmersiveGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    // Avatar
                    AvatarImage(
                        avatarType = if (entry.isCurrentUser && player != null) player.avatarType else entry.avatarType,
                        avatarPresetId = if (entry.isCurrentUser && player != null) player.avatarPresetId else entry.avatarValue,
                        avatarLocalPath = if (entry.isCurrentUser && player != null) player.avatarLocalPath else "",
                        size = 38.dp
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    // Name and Score
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = entry.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isTop1) ImmersiveGold else ImmersiveTextPrimary
                                ),
                                modifier = Modifier.weight(1f, fill = false)
                            )

                            if (entry.isCurrentUser) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = ImmersiveGold,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "VOCÊ",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF1D192B)
                                        ),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = "${entry.totalScore} pts • ${entry.totalPairs} pares",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = ImmersiveTextSecondary
                            )
                        )
                    }
                }

                // Level pill
                Surface(
                    color = ImmersiveSurfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = "Fase ${entry.highestLevel}",
                        maxLines = 1,
                        softWrap = false,
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

@Composable
private fun SkeletonLoadingList() {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton_alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(6) {
            Surface(
                color = ImmersiveSurface,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .alpha(alpha)
            ) {}
        }
    }
}

@Composable
private fun EmptyLeaderboardView(onRefresh: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = ImmersiveGold.copy(alpha = 0.8f),
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Ainda não há jogadores no ranking",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = ImmersiveTextPrimary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Seja o primeiro a alcançar o topo do Hall da Fama!",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = ImmersiveTextSecondary,
                    textAlign = TextAlign.Center
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onRefresh,
                colors = ButtonDefaults.buttonColors(containerColor = ImmersivePrimaryContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = ImmersiveGold)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Atualizar Ranking", color = ImmersiveGold, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ErrorOfflineView(onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.WifiOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(56.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Sem conexão no momento",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = ImmersiveTextPrimary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Não foi possível carregar o ranking online. Verifique sua conexão e tente novamente.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = ImmersiveTextSecondary,
                    textAlign = TextAlign.Center
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = ImmersivePrimaryContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = ImmersiveGold)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Tentar Novamente", color = ImmersiveGold, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ComingSoonTab(tabName: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = ImmersiveSurface,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ImmersiveSurfaceVariant, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.HourglassTop,
                    contentDescription = null,
                    tint = ImmersiveGold,
                    modifier = Modifier.size(56.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Ranking $tabName em Breve!",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = ImmersiveTextPrimary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "O ranking ${tabName.lowercase()} será disponibilizado nas próximas atualizações. Continue jogando no ranking Global para acumular pontos!",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = ImmersiveTextSecondary,
                        textAlign = TextAlign.Center
                    )
                )
            }
        }
    }
}

private fun formatRelativeTime(lastFetchTime: Long): String {
    if (lastFetchTime <= 0L) return "Atualizado agora"
    val diffSec = (System.currentTimeMillis() - lastFetchTime) / 1000
    return when {
        diffSec < 30 -> "Atualizado agora"
        diffSec < 60 -> "Atualizado há alguns segundos"
        diffSec < 3600 -> "Atualizado há ${diffSec / 60} min"
        else -> "Atualizado há ${diffSec / 3600}h"
    }
}
