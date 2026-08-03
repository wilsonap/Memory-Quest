package com.example.ui.screens.profile.model

import androidx.compose.ui.graphics.vector.ImageVector
import com.example.data.local.entity.AchievementEntity
import com.example.data.local.entity.PlayerEntity
import com.example.data.local.entity.StatisticsEntity
import com.example.data.local.entity.UnlockedThemeEntity

data class ProfileBadge(
    val id: String,
    val title: String,
    val iconEmoji: String,
    val description: String,
    val isUnlocked: Boolean
)

data class PlayerTitle(
    val title: String,
    val levelRequired: Int,
    val description: String
) {
    companion object {
        fun getTitleForLevel(level: Int): String = when {
            level >= 20 -> "Lenda Viva 👑"
            level >= 15 -> "Estrategista Supremo ⚡"
            level >= 10 -> "Guardião dos Pares 🛡️"
            level >= 6 -> "Mestre da Memória 🧠"
            level >= 3 -> "Iniciante Destemido ⚔️"
            else -> "Explorador 🧭"
        }
    }
}

data class ProfileUiState(
    val player: PlayerEntity? = null,
    val stats: StatisticsEntity? = null,
    val achievements: List<AchievementEntity> = emptyList(),
    val unlockedThemes: List<UnlockedThemeEntity> = emptyList(),
    val rankingDisplay: String = "Top 100",
    val isShareDialogOpen: Boolean = false,
    val isEditNameDialogOpen: Boolean = false
)
