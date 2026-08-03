package com.example.ui.screens.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.avatar.model.AvatarType
import com.example.data.local.entity.AchievementEntity
import com.example.data.local.entity.PlayerEntity
import com.example.data.local.entity.StatisticsEntity
import com.example.data.local.entity.UnlockedThemeEntity
import com.example.data.model.UsernameUiState
import com.example.data.repository.UsernameReservationResult
import com.example.ui.components.NameEntryDialog
import com.example.ui.components.TopGameBar
import com.example.ui.screens.profile.components.ActionButtons
import com.example.ui.screens.profile.components.AchievementsPreview
import com.example.ui.screens.profile.components.BadgesSection
import com.example.ui.screens.profile.components.PlayerProfileCard
import com.example.ui.screens.profile.components.ShareProfileDialog
import com.example.ui.screens.profile.components.SummaryCards
import com.example.ui.screens.profile.components.ThemesPreview
import com.example.ui.screens.profile.util.XPCalculator
import com.example.ui.screens.profile.viewmodel.ProfileViewModel
import com.example.ui.theme.ImmersiveBg

@Composable
fun ProfileScreen(
    player: PlayerEntity?,
    stats: StatisticsEntity?,
    achievements: List<AchievementEntity> = emptyList(),
    unlockedThemes: List<UnlockedThemeEntity> = emptyList(),
    rankingDisplay: String = "Top 100",
    usernameUiState: UsernameUiState = UsernameUiState(),
    onNameInputChange: (String, Boolean) -> Unit = { _, _ -> },
    onReserveUsername: (String, (UsernameReservationResult) -> Unit) -> Unit = { _, _ -> },
    onEditAvatarClick: () -> Unit = {},
    onNavigateToStats: () -> Unit = {},
    onNavigateToShop: () -> Unit = {},
    onNavigateToAchievements: () -> Unit = {},
    onNavigateToRanking: () -> Unit = {},
    onBackClick: () -> Unit = {},
    profileViewModel: ProfileViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by profileViewModel.uiState.collectAsState()

    var showEditNameModal by remember { mutableStateOf(false) }

    val xpInfo = remember(player, stats) {
        XPCalculator.calculateXp(player, stats)
    }

    val badges = remember(player, stats, achievements, unlockedThemes, rankingDisplay) {
        profileViewModel.computeBadges(player, stats, achievements, unlockedThemes, rankingDisplay)
    }

    val playerName = player?.name?.ifEmpty { "Explorador" } ?: "Explorador"
    val unlockedAchievementsCount = achievements.count { it.isUnlocked }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ImmersiveBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Top Bar
            TopGameBar(
                coins = player?.coins ?: 0,
                title = stringResource(R.string.profile_title),
                onBackClick = onBackClick
            )

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // 1. HERO CARD (Avatar, Name, Title, Level, XP Bar, Share)
                PlayerProfileCard(
                    playerName = playerName,
                    avatarType = player?.avatarType ?: AvatarType.PRESET.name,
                    avatarPresetId = player?.avatarPresetId ?: "avatar_01",
                    avatarLocalPath = player?.avatarLocalPath ?: "",
                    xpInfo = xpInfo,
                    onEditAvatarClick = onEditAvatarClick,
                    onEditNameClick = { showEditNameModal = true },
                    onShareClick = { profileViewModel.setShareDialogOpen(true) }
                )

                // 2. RESUMO (Ranking, Coins, Lives, Achievements)
                SummaryCards(
                    rankingDisplay = rankingDisplay,
                    coins = player?.coins ?: 0,
                    lives = 3,
                    unlockedAchievementsCount = unlockedAchievementsCount,
                    totalAchievementsCount = if (achievements.isNotEmpty()) achievements.size else 60,
                    onRankingClick = onNavigateToRanking,
                    onAchievementsClick = onNavigateToAchievements
                )

                // 3. MEDALHAS
                BadgesSection(badges = badges)

                // 4. TEMAS DESBLOQUEADOS
                ThemesPreview(
                    unlockedThemes = unlockedThemes,
                    onNavigateToShop = onNavigateToShop
                )

                // 5. ÚLTIMAS CONQUISTAS
                AchievementsPreview(
                    achievements = achievements,
                    onNavigateToAchievements = onNavigateToAchievements
                )

                // 6. AÇÕES DO PERFIL (Editar Nome, Trocar Avatar, Compartilhar, Ver Stats)
                ActionButtons(
                    onEditNameClick = { showEditNameModal = true },
                    onEditAvatarClick = onEditAvatarClick,
                    onShareClick = { profileViewModel.setShareDialogOpen(true) },
                    onViewStatsClick = onNavigateToStats
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // COMPARTILHAR PERFIL DIALOG
        if (uiState.isShareDialogOpen) {
            ShareProfileDialog(
                playerName = playerName,
                avatarType = player?.avatarType ?: AvatarType.PRESET.name,
                avatarPresetId = player?.avatarPresetId ?: "avatar_01",
                avatarLocalPath = player?.avatarLocalPath ?: "",
                level = xpInfo.level,
                rankingDisplay = rankingDisplay,
                totalScore = xpInfo.totalXp,
                onDismiss = { profileViewModel.setShareDialogOpen(false) }
            )
        }

        // EDIT NAME DIALOG
        if (showEditNameModal) {
            NameEntryDialog(
                initialName = playerName,
                title = stringResource(R.string.settings_edit_name_title),
                subtitle = stringResource(R.string.settings_edit_name_subtitle),
                uiState = usernameUiState,
                onNameInputChange = onNameInputChange,
                onConfirm = { newName ->
                    onReserveUsername(newName) { result ->
                        if (result is UsernameReservationResult.Success || result is UsernameReservationResult.PendingOffline) {
                            showEditNameModal = false
                            Toast.makeText(context, "Nome atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                        } else if (result is UsernameReservationResult.Error) {
                            Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                        } else if (result is UsernameReservationResult.Taken) {
                            Toast.makeText(context, "Nome em uso, escolha uma sugestão", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onDismiss = { showEditNameModal = false }
            )
        }
    }
}
