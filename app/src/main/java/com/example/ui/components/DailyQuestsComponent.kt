package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.DailyQuestEntity
import com.example.data.local.entity.PlayerEntity

private val ImmersiveSurface = Color(0xFF1D1B26)
private val ImmersiveSurfaceVariant = Color(0xFF2A2738)
private val ImmersiveGold = Color(0xFFFFD700)
private val ImmersiveTextPrimary = Color(0xFFF0EFF8)
private val ImmersiveTextSecondary = Color(0xFFA6A2B8)

fun DailyQuestEntity.getDescription(): String {
    return when (questType) {
        "FIND_PAIRS" -> "Encontrar $targetProgress pares"
        "COMPLETE_LEVELS" -> "Completar $targetProgress fases"
        "THREE_STARS" -> "Consiga 3 estrelas em uma fase"
        "WIN_NO_HELP" -> "Vença uma fase sem utilizar ajuda"
        "COMBO" -> "Faça $targetProgress acertos consecutivos"
        "FINISH_WITH_LIFE" -> "Termine uma fase com pelo menos 1 vida"
        else -> "Missão Diária"
    }
}

fun DailyQuestEntity.getIcon(): ImageVector {
    return when (questType) {
        "FIND_PAIRS" -> Icons.Default.Extension
        "COMPLETE_LEVELS" -> Icons.Default.EmojiEvents
        "THREE_STARS" -> Icons.Default.Star
        "WIN_NO_HELP" -> Icons.Default.Shield
        "COMBO" -> Icons.Default.Bolt
        "FINISH_WITH_LIFE" -> Icons.Default.Favorite
        else -> Icons.Default.CheckCircle
    }
}

@Composable
fun DailyQuestsCard(
    quests: List<DailyQuestEntity>,
    player: PlayerEntity?,
    onClick: () -> Unit,
    onClaimChest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val completedCount = quests.count { it.isCompleted }
    val isChestReady = completedCount == 3 && !(player?.dailyChestClaimed ?: false)
    val isChestClaimed = player?.dailyChestClaimed ?: false

    Surface(
        color = ImmersiveSurface,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_quests_card")
            .border(
                width = 1.dp,
                color = if (isChestReady) ImmersiveGold else ImmersiveSurfaceVariant,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🎯 MISSÕES DIÁRIAS",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = ImmersiveGold,
                            letterSpacing = 1.sp,
                            fontSize = 13.sp
                        )
                    )
                }

                Surface(
                    color = ImmersiveSurfaceVariant,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "$completedCount/3",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (completedCount == 3) ImmersiveGold else ImmersiveTextPrimary
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { completedCount / 3f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = ImmersiveGold,
                trackColor = ImmersiveSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val subtitleText = when {
                    isChestClaimed -> "🎁 Baú Diário Resgatado!"
                    isChestReady -> "🎁 BAÚ DIÁRIO PRONTO PARA ABRIR!"
                    else -> "🎁 Falta ${3 - completedCount} missão(ões) para o Baú"
                }

                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isChestReady) ImmersiveGold else ImmersiveTextSecondary,
                        fontWeight = if (isChestReady) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier.weight(1f)
                )

                if (isChestReady) {
                    Button(
                        onClick = onClaimChest,
                        colors = ButtonDefaults.buttonColors(containerColor = ImmersiveGold),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("claim_daily_chest_button")
                    ) {
                        Text(
                            text = "ABRIR BAÚ",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                } else {
                    Text(
                        text = "Ver Missões ›",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = ImmersiveGold,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun DailyQuestsDialog(
    quests: List<DailyQuestEntity>,
    player: PlayerEntity?,
    onDismiss: () -> Unit,
    onClaimChest: () -> Unit,
    onDoubleReward: () -> Unit
) {
    val completedCount = quests.count { it.isCompleted }
    val isChestClaimed = player?.dailyChestClaimed ?: false
    val isChestDoubled = player?.dailyChestDoubled ?: false
    val isChestReady = completedCount == 3 && !isChestClaimed

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = ImmersiveSurface,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ImmersiveSurfaceVariant, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🎯 Missões Diárias",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = ImmersiveTextPrimary,
                            fontSize = 18.sp
                        )
                    )

                    Surface(
                        color = ImmersiveSurfaceVariant,
                        shape = CircleShape
                    ) {
                        Text(
                            text = "$completedCount/3",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = ImmersiveGold
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // List of Quests
                quests.forEach { quest ->
                    QuestItemRow(quest = quest)
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Daily Chest Section Card inside Dialog
                Surface(
                    color = ImmersiveSurfaceVariant,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = if (isChestReady) ImmersiveGold else Color.Transparent,
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🎁 BAÚ DIÁRIO",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = ImmersiveGold,
                                fontSize = 14.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        if (!isChestClaimed) {
                            if (isChestReady) {
                                Text(
                                    text = "Você concluiu todas as missões de hoje!",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = ImmersiveTextSecondary,
                                        fontSize = 12.sp
                                    )
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = onClaimChest,
                                    colors = ButtonDefaults.buttonColors(containerColor = ImmersiveGold),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "🎁 ABRIR BAÚ DIÁRIO",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = Color.Black,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            } else {
                                Text(
                                    text = "Complete 3 missões para resgatar o Baú!",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = ImmersiveTextSecondary,
                                        fontSize = 12.sp
                                    )
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                LinearProgressIndicator(
                                    progress = { completedCount / 3f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = ImmersiveGold,
                                    trackColor = ImmersiveSurface
                                )
                            }
                        } else {
                            // Chest is claimed! Show rewarded content/double option
                            val rewardType = player?.dailyChestRewardType ?: ""
                            val rewardAmount = player?.dailyChestRewardAmount ?: 0
                            val boosterId = player?.dailyChestRewardBoosterId ?: ""

                            val rewardText = when (rewardType) {
                                "COINS" -> "Recompensa: $rewardAmount Moedas 🪙"
                                "BOOSTER" -> "Recompensa: 1 Booster! 🎁"
                                else -> "Baú Resgatado ✨"
                            }

                            Text(
                                text = rewardText,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = ImmersiveGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            if (rewardType == "COINS" && !isChestDoubled) {
                                val remainingAdsToday = (5 - (player?.rewardedAdsToday ?: 0)).coerceAtLeast(0)
                                if (remainingAdsToday > 0) {
                                    Button(
                                        onClick = onDoubleReward,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CC9F0)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "🎬 Dobrar Recompensa (${rewardAmount * 2} 🪙)",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color.Black,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "Limite diário de vídeos atingido.",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = ImmersiveTextSecondary,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            } else if (isChestDoubled) {
                                Text(
                                    text = "✨ Recompensa dobrada com sucesso!",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFF4CC9F0),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Close Button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = "Fechar",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = ImmersiveTextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun QuestItemRow(quest: DailyQuestEntity) {
    Surface(
        color = ImmersiveSurfaceVariant,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (quest.isCompleted) ImmersiveGold.copy(alpha = 0.2f) else ImmersiveSurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = quest.getIcon(),
                    contentDescription = null,
                    tint = if (quest.isCompleted) ImmersiveGold else ImmersiveTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = quest.getDescription(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = ImmersiveTextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                LinearProgressIndicator(
                    progress = { (quest.currentProgress.toFloat() / quest.targetProgress.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = if (quest.isCompleted) ImmersiveGold else Color(0xFF4CC9F0),
                    trackColor = ImmersiveSurface
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = if (quest.isCompleted) "✔" else "${quest.currentProgress}/${quest.targetProgress}",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (quest.isCompleted) ImmersiveGold else ImmersiveTextSecondary,
                    fontSize = 12.sp
                )
            )
        }
    }
}
