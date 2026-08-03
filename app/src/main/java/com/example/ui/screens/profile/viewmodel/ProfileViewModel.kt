package com.example.ui.screens.profile.viewmodel

import androidx.lifecycle.ViewModel
import com.example.data.local.entity.AchievementEntity
import com.example.data.local.entity.PlayerEntity
import com.example.data.local.entity.StatisticsEntity
import com.example.data.local.entity.UnlockedThemeEntity
import com.example.ui.screens.profile.model.ProfileBadge
import com.example.ui.screens.profile.model.ProfileUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ProfileViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun updateData(
        player: PlayerEntity?,
        stats: StatisticsEntity?,
        achievements: List<AchievementEntity>,
        unlockedThemes: List<UnlockedThemeEntity>,
        rankingDisplay: String
    ) {
        val badges = computeBadges(player, stats, achievements, unlockedThemes, rankingDisplay)

        _uiState.value = _uiState.value.copy(
            player = player,
            stats = stats,
            achievements = achievements,
            unlockedThemes = unlockedThemes,
            rankingDisplay = rankingDisplay
        )
    }

    fun setShareDialogOpen(isOpen: Boolean) {
        _uiState.value = _uiState.value.copy(isShareDialogOpen = isOpen)
    }

    fun setEditNameDialogOpen(isOpen: Boolean) {
        _uiState.value = _uiState.value.copy(isEditNameDialogOpen = isOpen)
    }

    fun computeBadges(
        player: PlayerEntity?,
        stats: StatisticsEntity?,
        achievements: List<AchievementEntity>,
        unlockedThemes: List<UnlockedThemeEntity>,
        rankingDisplay: String
    ): List<ProfileBadge> {
        val totalFlips = stats?.totalFlips ?: 0
        val correctFlips = stats?.correctFlips ?: 0
        val accuracy = if (totalFlips > 0) (correctFlips.toFloat() / totalFlips.toFloat() * 100f) else 0f

        val unlockedAchCount = achievements.count { it.isUnlocked }

        return listOf(
            ProfileBadge(
                id = "top_global",
                title = "Top Global",
                iconEmoji = "🥇",
                description = "Entre no Top 100 do Ranking Global",
                isUnlocked = rankingDisplay.contains("#") || rankingDisplay.lowercase().contains("top 100") || (player?.highestLevel ?: 1) >= 10
            ),
            ProfileBadge(
                id = "memory_master",
                title = "Mestre da Memória",
                iconEmoji = "🧠",
                description = "Alcance 70% de precisão ou Fase 5",
                isUnlocked = accuracy >= 70f || (player?.highestLevel ?: 1) >= 5
            ),
            ProfileBadge(
                id = "streak_master",
                title = "Sequência",
                iconEmoji = "🔥",
                description = "Atinja um combo de 5x ou mais",
                isUnlocked = (stats?.highestStreak ?: 0) >= 5
            ),
            ProfileBadge(
                id = "collector",
                title = "Colecionador",
                iconEmoji = "💎",
                description = "Desbloqueie pelo menos 2 temas de jogo",
                isUnlocked = unlockedThemes.size >= 2
            ),
            ProfileBadge(
                id = "veteran",
                title = "Veterano",
                iconEmoji = "👑",
                description = "Jogue pelo menos 10 partidas de Memory Quest",
                isUnlocked = (stats?.totalGames ?: 0) >= 10
            ),
            ProfileBadge(
                id = "conqueror",
                title = "Conquistador",
                iconEmoji = "🏆",
                description = "Conclua 5 conquistas no jogo",
                isUnlocked = unlockedAchCount >= 5
            )
        )
    }
}
