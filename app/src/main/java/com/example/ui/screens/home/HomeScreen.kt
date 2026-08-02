package com.example.ui.screens.home

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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.data.local.entity.PlayerEntity
import com.example.ui.components.BannerAdView
import com.example.ui.components.SparkleParticleOverlay
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
fun HomeScreen(
    player: PlayerEntity?,
    onPlayClick: () -> Unit,
    onRankingClick: () -> Unit,
    onShopClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onStatsClick: () -> Unit,
    onAchievementsClick: () -> Unit,
    onEditNameClick: () -> Unit,
    isAdsRemoved: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ImmersiveBg)
    ) {
        // Subtle particle sparkle overlay
        SparkleParticleOverlay()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Player Profile Card
            Surface(
                color = ImmersiveSurface,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ImmersiveSurfaceVariant, RoundedCornerShape(28.dp))
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
                        modifier = Modifier.clickable { onEditNameClick() }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(ImmersivePrimary, ImmersivePrimaryContainer)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (player?.name?.firstOrNull() ?: 'A').uppercase(),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1D192B)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = player?.name?.ifEmpty { "Alex Quest" } ?: "Alex Quest",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = ImmersiveTextPrimary
                                    )
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = stringResource(R.string.home_edit_name),
                                    tint = ImmersivePrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = stringResource(R.string.home_explorer_level, player?.currentLevel ?: 1),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = ImmersiveTextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            )
                            if (player?.usernameStatus == "PENDING_VALIDATION") {
                                Text(
                                    text = "⏳ " + stringResource(R.string.username_awaiting_confirmation),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFFFFD166),
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }
                    }

                    // Coins badge
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(ImmersiveSurfaceVariant)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${player?.coins ?: 0}",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = ImmersiveSecondary
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(ImmersiveGold)
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
                        text = stringResource(R.string.home_next_challenge, player?.currentLevel ?: 1),
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

            Spacer(modifier = Modifier.height(28.dp))

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
                        icon = Icons.Default.ShoppingBag,
                        badgeEmoji = "🎁",
                        onClick = onShopClick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_shop_button")
                    )

                    HomeMenuCard(
                        title = stringResource(R.string.home_ranking_title),
                        subtitle = stringResource(R.string.home_ranking_subtitle),
                        icon = Icons.Default.EmojiEvents,
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
                        icon = Icons.Default.BarChart,
                        badgeEmoji = "📊",
                        onClick = onStatsClick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_stats_button")
                    )

                    HomeMenuCard(
                        title = stringResource(R.string.home_config_title),
                        subtitle = stringResource(R.string.home_config_subtitle),
                        icon = Icons.Default.Settings,
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

            BannerAdView(isAdsRemoved = isAdsRemoved)

            Text(
                text = "Memory Quest • Immersive UI",
                style = MaterialTheme.typography.labelSmall.copy(color = ImmersiveTextSecondary.copy(alpha = 0.5f))
            )
        }
    }
}

@Composable
private fun HomeMenuCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
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

