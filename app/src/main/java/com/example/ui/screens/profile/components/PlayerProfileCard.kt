package com.example.ui.screens.profile.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.avatar.model.AvatarType
import com.example.avatar.ui.AvatarImage
import com.example.ui.screens.profile.model.PlayerTitle
import com.example.ui.screens.profile.util.XPInfo
import com.example.ui.theme.ImmersiveGold
import com.example.ui.theme.ImmersivePrimary
import com.example.ui.theme.ImmersivePrimaryContainer
import com.example.ui.theme.ImmersiveSurface
import com.example.ui.theme.ImmersiveSurfaceVariant
import com.example.ui.theme.ImmersiveTextPrimary
import com.example.ui.theme.ImmersiveTextSecondary

@Composable
fun PlayerProfileCard(
    playerName: String,
    avatarType: String,
    avatarPresetId: String,
    avatarLocalPath: String,
    xpInfo: XPInfo,
    onEditAvatarClick: () -> Unit,
    onEditNameClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = xpInfo.progress,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "xp_progress_anim"
    )

    val playerTitle = PlayerTitle.getTitleForLevel(xpInfo.level)

    Surface(
        color = ImmersivePrimaryContainer,
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        ImmersiveGold.copy(alpha = 0.8f),
                        ImmersivePrimary.copy(alpha = 0.4f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Top Row: Avatar + Name + Title + Edit buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Large Avatar with click edit badge
                Box(contentAlignment = Alignment.BottomEnd) {
                    AvatarImage(
                        avatarType = avatarType,
                        avatarPresetId = avatarPresetId,
                        avatarLocalPath = avatarLocalPath,
                        size = 64.dp,
                        showEditBadge = true,
                        onClick = onEditAvatarClick
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Name, Title, and Level
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = playerName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = ImmersiveTextPrimary
                            ),
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .clickable { onEditNameClick() }
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        IconButton(
                            onClick = onEditNameClick,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("profile_edit_name_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar Nome",
                                tint = ImmersiveGold,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Player Title (e.g. Mestre da Memória)
                    Surface(
                        color = ImmersiveSurface,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = playerTitle,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = ImmersiveGold
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // XP Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = ImmersiveGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Nível ${xpInfo.level}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = ImmersiveTextPrimary
                        )
                    )
                }

                Text(
                    text = "${(animatedProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = ImmersiveGold
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Animated XP Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(ImmersiveSurfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFFFFB703),
                                    ImmersiveGold
                                )
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${xpInfo.currentLevelXp} / ${xpInfo.requiredLevelXp} XP",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = ImmersiveTextSecondary
                    )
                )

                Text(
                    text = "${xpInfo.xpRemaining} XP para o próximo nível",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = ImmersiveGold,
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Compartilhar Perfil Button
            OutlinedButton(
                onClick = onShareClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = ImmersiveSurface.copy(alpha = 0.6f),
                    contentColor = ImmersiveGold
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveGold.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .testTag("share_profile_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Compartilhar Perfil",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
