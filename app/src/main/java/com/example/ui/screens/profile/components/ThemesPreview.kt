package com.example.ui.screens.profile.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.UnlockedThemeEntity
import com.example.data.model.GameTheme
import com.example.ui.theme.ImmersiveGold
import com.example.ui.theme.ImmersiveSurface
import com.example.ui.theme.ImmersiveTextPrimary
import com.example.ui.theme.ImmersiveTextSecondary

@Composable
fun ThemesPreview(
    unlockedThemes: List<UnlockedThemeEntity>,
    equippedThemeId: String = "animals",
    onNavigateToShop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val unlockedThemeIds = unlockedThemes.map { it.themeId }.toSet()

    // Filter real game themes from GameTheme.ALL_THEMES that are unlocked
    val unlockedGameThemes = GameTheme.ALL_THEMES.filter { theme ->
        theme.isDefaultUnlocked || unlockedThemeIds.contains(theme.id)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TEMAS DESBLOQUEADOS (${unlockedGameThemes.size})",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = ImmersiveGold,
                    letterSpacing = 1.sp
                )
            )

            OutlinedButton(
                onClick = onNavigateToShop,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("view_shop_from_profile_button")
            ) {
                Text(
                    text = "Ver Loja",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = ImmersiveGold
                    )
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = ImmersiveGold,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (unlockedGameThemes.isEmpty()) {
            Surface(
                color = ImmersiveSurface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Nenhum tema desbloqueado. Visite a Loja!",
                    style = MaterialTheme.typography.bodySmall.copy(color = ImmersiveTextSecondary),
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(unlockedGameThemes, key = { it.id }) { theme ->
                    val isEquipped = theme.id == equippedThemeId
                    UnlockedThemeCardItem(
                        theme = theme,
                        isEquipped = isEquipped,
                        onClick = onNavigateToShop
                    )
                }
            }
        }
    }
}

@Composable
private fun UnlockedThemeCardItem(
    theme: GameTheme,
    isEquipped: Boolean,
    onClick: () -> Unit
) {
    val themeName = stringResource(theme.nameRes)
    val emoji = theme.symbols.firstOrNull()?.first ?: "🎴"
    val themeColor = Color(theme.primaryColorHex)

    Surface(
        color = ImmersiveSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = if (isEquipped) 2.dp else 1.dp,
            color = if (isEquipped) ImmersiveGold else themeColor.copy(alpha = 0.6f)
        ),
        modifier = Modifier
            .width(108.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isEquipped) ImmersiveGold.copy(alpha = 0.25f)
                        else themeColor.copy(alpha = 0.25f)
                    )
            ) {
                Text(text = emoji, fontSize = 22.sp)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = themeName,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = ImmersiveTextPrimary,
                    fontSize = 11.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(2.dp))

            if (isEquipped) {
                Surface(
                    color = ImmersiveGold.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "EQUIPADO",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = ImmersiveGold,
                            fontSize = 8.sp,
                            letterSpacing = 0.5.sp
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            } else {
                Text(
                    text = "Desbloqueado",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium,
                        color = ImmersiveTextSecondary,
                        fontSize = 9.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

