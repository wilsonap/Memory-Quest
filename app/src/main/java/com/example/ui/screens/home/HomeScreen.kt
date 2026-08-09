package com.example.ui.screens.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.avatar.model.AvatarType
import com.example.avatar.ui.AvatarImage
import com.example.data.local.entity.DailyQuestEntity
import com.example.data.local.entity.PlayerEntity
import com.example.ui.components.BannerAdContainer
import com.example.ui.components.BannerAdView
import com.example.ui.components.DailyQuestsCard
import com.example.ui.components.DailyQuestsDialog
import com.example.ui.components.SparkleParticleOverlay
import com.example.ui.screens.profile.model.PlayerTitle
import com.example.ui.screens.profile.util.XPCalculator
import com.example.ui.theme.ImmersiveBg
import com.example.ui.theme.ImmersiveGold
import com.example.ui.theme.ImmersivePrimary
import com.example.ui.theme.ImmersivePrimaryContainer
import com.example.ui.theme.ImmersiveSecondary
import com.example.ui.theme.ImmersiveSurface
import com.example.ui.theme.ImmersiveSurfaceVariant
import com.example.ui.theme.ImmersiveTextPrimary
import com.example.ui.theme.ImmersiveTextSecondary

@Composable
fun HomeScreen(
    player: PlayerEntity?,
    dailyQuests: List<DailyQuestEntity> = emptyList(),
    onPlayClick: () -> Unit,
    onProfileClick: () -> Unit = {},
    onRankingClick: () -> Unit,
    onShopClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onStatsClick: () -> Unit,
    onAchievementsClick: () -> Unit,
    onClaimDailyReward: () -> Unit = {},
    onWatchRewardedAd: () -> Unit = {},
    onClaimDailyChest: () -> Unit = {},
    onDoubleDailyChestReward: () -> Unit = {},
    isAdsRemoved: Boolean = false,
    modifier: Modifier = Modifier
) {
    var showDailyQuestsDialog by remember { mutableStateOf(false) }

    val playerName = player?.name?.ifEmpty { "Alessandro" } ?: "Alessandro"
    val level = (player?.highestLevel ?: 1).coerceAtLeast(1)
    val playerTitle = PlayerTitle.getTitleForLevel(level)

    val xpInfo = remember(player) {
        XPCalculator.calculateXp(player, null)
    }

    val animatedProgress by animateFloatAsState(
        targetValue = xpInfo.progress,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "home_xp_anim"
    )

    Scaffold(
        containerColor = ImmersiveBg,
        bottomBar = {
            BannerAdContainer(isAdsRemoved = isAdsRemoved)
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            SparkleParticleOverlay()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
            // Header Player Profile Card (Tapping anywhere opens PROFILE)
            Surface(
                color = ImmersiveSurface,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_profile_card")
                    .border(1.dp, ImmersiveSurfaceVariant, RoundedCornerShape(24.dp))
                    .clickable(onClick = onProfileClick)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        AvatarImage(
                            avatarType = player?.avatarType ?: AvatarType.PRESET.name,
                            avatarPresetId = player?.avatarPresetId ?: "avatar_01",
                            avatarLocalPath = player?.avatarLocalPath ?: "",
                            size = 52.dp,
                            showEditBadge = false,
                            onClick = onProfileClick
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = playerName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = ImmersiveTextPrimary
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text = "$playerTitle • Nível $level",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = ImmersiveGold,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Small XP Bar
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(ImmersiveSurfaceVariant)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(animatedProgress)
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(Color(0xFFFFB703), ImmersiveGold)
                                                )
                                            )
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = "${xpInfo.currentLevelXp}/${xpInfo.requiredLevelXp} XP",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = ImmersiveTextSecondary,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Coins & Arrow Indicator
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(ImmersiveSurfaceVariant)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${player?.coins ?: 0}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = ImmersiveSecondary
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(ImmersiveGold)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = stringResource(R.string.profile_view_profile),
                            tint = ImmersivePrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Hero Branding
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "MEMORY\nQUEST",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        fontSize = 42.sp,
                        letterSpacing = (-1).sp,
                        lineHeight = 44.sp
                    ),
                    color = ImmersivePrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    color = ImmersivePrimaryContainer,
                    shape = CircleShape,
                    modifier = Modifier.border(1.dp, ImmersivePrimary.copy(alpha = 0.3f), CircleShape)
                ) {
                    Text(
                        text = stringResource(R.string.home_next_challenge, level),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = ImmersivePrimary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        ),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Main Play Hero Button
            Surface(
                onClick = onPlayClick,
                shape = RoundedCornerShape(40.dp),
                color = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(180.dp)
                    .border(3.dp, ImmersivePrimary.copy(alpha = 0.25f), RoundedCornerShape(40.dp))
                    .testTag("home_play_button")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(ImmersivePrimaryContainer, Color(0xFF4F378B))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(ImmersivePrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = stringResource(R.string.home_play),
                                tint = Color(0xFF1D192B),
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = stringResource(R.string.home_play),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = ImmersivePrimary,
                                letterSpacing = 3.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Daily Reward & Rewarded Video Section
            val dateFormat = remember { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()) }
            val todayString = remember { dateFormat.format(java.util.Date()) }
            val lastRewardDateString = remember(player?.lastDailyRewardDate) {
                if ((player?.lastDailyRewardDate ?: 0L) > 0L) dateFormat.format(java.util.Date(player!!.lastDailyRewardDate)) else ""
            }
            val isDailyClaimed = lastRewardDateString == todayString
            val remainingVideosToday = remember(player?.rewardedAdsDate, player?.rewardedAdsToday) {
                if (player?.rewardedAdsDate == todayString) {
                    (5 - (player.rewardedAdsToday)).coerceAtLeast(0)
                } else 5
            }

            Surface(
                color = ImmersiveSurface,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ImmersiveGold.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isDailyClaimed) "Recompensa Diária Coletada ✨" else "Recompensa Diária Disponível! 🎁",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = ImmersiveGold,
                                fontSize = 13.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isDailyClaimed) "Vídeos premiados hoje: $remainingVideosToday/5 restantes (+100 🪙 cada)" else "Resgate suas moedas diárias gratuitas!",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = ImmersiveTextSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    if (!isDailyClaimed) {
                        androidx.compose.material3.Button(
                            onClick = onClaimDailyReward,
                            colors = ButtonDefaults.buttonColors(containerColor = ImmersiveGold),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Resgatar", style = MaterialTheme.typography.labelMedium.copy(color = Color.Black, fontWeight = FontWeight.Bold))
                        }
                    } else if (remainingVideosToday > 0) {
                        OutlinedButton(
                            onClick = onWatchRewardedAd,
                            border = androidx.compose.foundation.BorderStroke(1.dp, ImmersivePrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Vídeo +100 🪙", style = MaterialTheme.typography.labelSmall.copy(color = ImmersivePrimary, fontWeight = FontWeight.Bold))
                        }
                    } else {
                        Surface(
                            color = ImmersiveSurfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Volte Amanhã ✓",
                                style = MaterialTheme.typography.labelSmall.copy(color = ImmersiveTextSecondary, fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Daily Quests Section
            DailyQuestsCard(
                quests = dailyQuests,
                player = player,
                onClick = { showDailyQuestsDialog = true },
                onClaimChest = onClaimDailyChest
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Bottom Navigation Cards Grid
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HomeMenuCard(
                        title = stringResource(R.string.home_shop_title),
                        subtitle = stringResource(R.string.home_shop_subtitle),
                        badgeEmoji = "🎁",
                        onClick = onShopClick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_shop_button")
                    )

                    HomeMenuCard(
                        title = stringResource(R.string.home_ranking_title),
                        subtitle = stringResource(R.string.home_ranking_subtitle),
                        badgeEmoji = "🏆",
                        onClick = onRankingClick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_ranking_button")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HomeMenuCard(
                        title = stringResource(R.string.home_stats_title),
                        subtitle = stringResource(R.string.home_stats_subtitle),
                        badgeEmoji = "📊",
                        onClick = onStatsClick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_stats_button")
                    )

                    HomeMenuCard(
                        title = stringResource(R.string.home_config_title),
                        subtitle = stringResource(R.string.home_config_subtitle),
                        badgeEmoji = "⚙️",
                        onClick = onSettingsClick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_settings_button")
                    )
                }

                OutlinedButton(
                    onClick = onAchievementsClick,
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveSurfaceVariant),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = ImmersiveSurfaceVariant.copy(alpha = 0.3f),
                        contentColor = ImmersiveTextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("home_achievements_button")
                ) {
                    Text(
                        text = stringResource(R.string.home_achievements_button),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = ImmersiveTextPrimary,
                            letterSpacing = 1.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.home_footer_text),
                style = MaterialTheme.typography.labelSmall.copy(color = ImmersiveTextSecondary.copy(alpha = 0.5f))
            )
        }
    }
}

    if (showDailyQuestsDialog) {
        DailyQuestsDialog(
            quests = dailyQuests,
            player = player,
            onDismiss = { showDailyQuestsDialog = false },
            onClaimChest = onClaimDailyChest,
            onDoubleReward = onDoubleDailyChestReward
        )
    }
}

@Composable
private fun HomeMenuCard(
    title: String,
    subtitle: String,
    badgeEmoji: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = ImmersiveSurfaceVariant.copy(alpha = 0.4f),
        modifier = modifier
            .height(72.dp)
            .border(1.dp, ImmersiveSurfaceVariant, RoundedCornerShape(20.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ImmersiveSurfaceVariant.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = badgeEmoji, fontSize = 20.sp)
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = ImmersiveTextPrimary,
                        fontSize = 13.sp
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = ImmersiveTextSecondary,
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}
