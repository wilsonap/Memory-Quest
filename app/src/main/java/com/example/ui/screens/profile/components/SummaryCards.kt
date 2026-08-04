package com.example.ui.screens.profile.components

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
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.MilitaryTech
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ImmersiveGold
import com.example.ui.theme.ImmersiveGreen
import com.example.ui.theme.ImmersivePrimary
import com.example.ui.theme.ImmersiveSecondary
import com.example.ui.theme.ImmersiveSurface
import com.example.ui.theme.ImmersiveSurfaceVariant
import com.example.ui.theme.ImmersiveTextPrimary
import com.example.ui.theme.ImmersiveTextSecondary

@Composable
fun SummaryCards(
    rankingDisplay: String,
    coins: Int,
    lives: Int,
    unlockedAchievementsCount: Int,
    totalAchievementsCount: Int = 60,
    onRankingClick: () -> Unit = {},
    onAchievementsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SummaryItemCard(
                title = "Ranking",
                value = rankingDisplay,
                icon = Icons.Default.EmojiEvents,
                accentColor = ImmersiveGold,
                onClick = onRankingClick,
                testTag = "summary_ranking_card",
                modifier = Modifier.weight(1f)
            )

            SummaryItemCard(
                title = "Moedas",
                value = "$coins",
                icon = Icons.Default.MonetizationOn,
                accentColor = Color(0xFFFFB703),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SummaryItemCard(
                title = "Vidas",
                value = "$lives",
                icon = Icons.Default.Favorite,
                accentColor = Color(0xFFFF5252),
                modifier = Modifier.weight(1f)
            )

            SummaryItemCard(
                title = "Conquistas",
                value = "$unlockedAchievementsCount / $totalAchievementsCount",
                icon = Icons.Default.MilitaryTech,
                accentColor = ImmersivePrimary,
                onClick = onAchievementsClick,
                testTag = "summary_achievements_card",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SummaryItemCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: (() -> Unit)? = null,
    testTag: String = "",
    modifier: Modifier = Modifier
) {
    Surface(
        color = ImmersiveSurface,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier)
            .border(1.dp, ImmersiveSurfaceVariant, RoundedCornerShape(20.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = ImmersiveTextPrimary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = ImmersiveTextSecondary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
