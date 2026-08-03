package com.example.ui.screens.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.UnlockedThemeEntity
import com.example.ui.theme.ImmersiveGold
import com.example.ui.theme.ImmersivePrimary
import com.example.ui.theme.ImmersiveSurface
import com.example.ui.theme.ImmersiveSurfaceVariant
import com.example.ui.theme.ImmersiveTextPrimary
import com.example.ui.theme.ImmersiveTextSecondary

data class ThemeThumbnail(
    val id: String,
    val name: String,
    val emoji: String,
    val color: Color,
    val isUnlocked: Boolean
)

@Composable
fun ThemesPreview(
    unlockedThemes: List<UnlockedThemeEntity>,
    onNavigateToShop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val unlockedThemeIds = unlockedThemes.map { it.themeId }.toSet()

    // Up to 8 predefined game themes
    val themesList = listOf(
        ThemeThumbnail("theme_classic", "Clássico", "🎴", Color(0xFF7209B7), true),
        ThemeThumbnail("theme_neon", "Neon Cyber", "⚡", Color(0xFF4CC9F0), unlockedThemeIds.contains("theme_neon")),
        ThemeThumbnail("theme_nature", "Floresta", "🌿", Color(0xFF4EAD69), unlockedThemeIds.contains("theme_nature")),
        ThemeThumbnail("theme_galaxy", "Galáxia", "🌌", Color(0xFF3F37C9), unlockedThemeIds.contains("theme_galaxy")),
        ThemeThumbnail("theme_volcano", "Vulcão", "🌋", Color(0xFFF72585), unlockedThemeIds.contains("theme_volcano")),
        ThemeThumbnail("theme_candy", "Doce", "🍭", Color(0xFFFF70A6), unlockedThemeIds.contains("theme_candy")),
        ThemeThumbnail("theme_ocean", "Oceano", "🌊", Color(0xFF00B4D8), unlockedThemeIds.contains("theme_ocean")),
        ThemeThumbnail("theme_gold", "Lendário", "👑", Color(0xFFFFD08A), unlockedThemeIds.contains("theme_gold"))
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TEMAS DESBLOQUEADOS",
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

        LazyRow(
            contentPadding = PaddingValues(horizontal = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(themesList) { theme ->
                ThemeCardItem(theme = theme, onClick = onNavigateToShop)
            }
        }
    }
}

@Composable
private fun ThemeCardItem(
    theme: ThemeThumbnail,
    onClick: () -> Unit
) {
    Surface(
        color = ImmersiveSurface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .width(84.dp)
            .height(96.dp)
            .border(
                1.dp,
                if (theme.isUnlocked) theme.color.copy(alpha = 0.8f) else ImmersiveSurfaceVariant,
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (theme.isUnlocked) theme.color.copy(alpha = 0.3f) else ImmersiveSurfaceVariant)
            ) {
                if (theme.isUnlocked) {
                    Text(text = theme.emoji, fontSize = 20.sp)
                } else {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Bloqueado",
                        tint = ImmersiveTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = theme.name,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (theme.isUnlocked) FontWeight.Bold else FontWeight.Normal,
                    color = if (theme.isUnlocked) ImmersiveTextPrimary else ImmersiveTextSecondary,
                    fontSize = 10.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
