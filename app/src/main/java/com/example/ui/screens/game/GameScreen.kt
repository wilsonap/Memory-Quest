package com.example.ui.screens.game

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.avatar.model.AvatarType
import com.example.avatar.ui.AvatarImage
import com.example.data.local.entity.PlayerEntity
import com.example.data.model.LevelConfig
import com.example.ui.components.BannerAdView
import com.example.ui.components.Card3D
import com.example.ui.components.SparkleParticleOverlay
import com.example.ui.components.TopGameBar
import com.example.ui.screens.game.components.VictoryScreen
import com.example.ui.theme.ImmersiveBg
import com.example.ui.theme.ImmersivePrimary
import com.example.ui.theme.ImmersivePrimaryContainer
import com.example.ui.theme.ImmersiveSurface
import com.example.ui.viewmodel.GameState
import com.example.ui.viewmodel.GameUiStatus

private fun Context.findActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) return currentContext
        currentContext = currentContext.baseContext
    }
    return null
}

@Composable
fun GameScreen(
    state: GameState,
    coins: Int,
    player: PlayerEntity? = null,
    onCardClick: (Int) -> Unit,
    onUseHint: () -> Unit,
    onRevealPair: () -> Unit,
    onFreezeTimer: () -> Unit,
    onNextLevel: () -> Unit,
    onRestartLevel: () -> Unit,
    onGoToShop: () -> Unit,
    onBackToHome: () -> Unit,
    onAppBackgrounded: () -> Unit = {},
    isAdsRemoved: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    // 1. Protection against screenshots and screen recording (FLAG_SECURE)
    DisposableEffect(activity) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    // 2. Lifecycle observer to flip cards face down when app is backgrounded
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, onAppBackgrounded) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                onAppBackgrounded()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val levelConfig = LevelConfig.getConfigForLevel(state.levelNumber)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ImmersiveBg)
    ) {
        if (state.status is GameUiStatus.LevelCompleted) {
            SparkleParticleOverlay()
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            TopGameBar(
                coins = coins,
                lives = state.lives,
                level = state.levelNumber,
                onBackClick = onBackToHome
            )

            // Dynamic Banner Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        when (state.status) {
                            is GameUiStatus.Previewing -> ImmersivePrimaryContainer
                            is GameUiStatus.Playing -> ImmersiveSurface
                            else -> ImmersiveSurface
                        }
                    )
                    .padding(vertical = 8.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    when (val status = state.status) {
                        is GameUiStatus.Previewing -> {
                            Text(
                                text = stringResource(R.string.game_preview_text, status.remainingSeconds),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }

                        is GameUiStatus.Playing -> {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = if (state.isTimerFrozen) Color(0xFF4CC9F0) else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.game_time, formatTime(state.elapsedTimeSeconds)),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Text(
                                text = stringResource(R.string.game_pairs, state.pairsFound, state.totalPairs),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF06D6A0)
                                )
                            )

                            if (state.currentCombo > 1) {
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = stringResource(R.string.game_combo, state.currentCombo),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFFFB703)
                                    )
                                )
                            }
                        }

                        else -> {
                            Text(
                                text = "${stringResource(R.string.settings_theme)}: ${stringResource(state.theme.nameRes)}",
                                style = MaterialTheme.typography.titleSmall.copy(color = Color.White)
                            )
                        }
                    }
                }
            }

            // Cards Grid Layout
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                val availW = maxWidth
                val availH = maxHeight

                val gridLayout = remember(state.cards.size, availW, availH) {
                    calculateOptimalGrid(
                        cardCount = state.cards.size,
                        availableWidth = availW,
                        availableHeight = availH
                    )
                }

                Column(
                    modifier = Modifier
                        .width(gridLayout.totalGridWidth)
                        .height(gridLayout.totalGridHeight),
                    verticalArrangement = Arrangement.spacedBy(gridLayout.spacing),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    for (r in 0 until gridLayout.rows) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(gridLayout.cardHeight),
                            horizontalArrangement = Arrangement.spacedBy(gridLayout.spacing),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (c in 0 until gridLayout.cols) {
                                val index = r * gridLayout.cols + c
                                if (index < state.cards.size) {
                                    val card = state.cards[index]
                                    Card3D(
                                        card = card,
                                        onClick = { onCardClick(index) },
                                        cardFrameId = state.frameId,
                                        cardBgColorHex = state.theme.cardBgColorHex,
                                        modifier = Modifier
                                            .width(gridLayout.cardWidth)
                                            .height(gridLayout.cardHeight)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.width(gridLayout.cardWidth))
                                }
                            }
                        }
                    }
                }
            }

            // In-game Boosters Toolbar
            Surface(
                color = Color(0xFF160D2E),
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hint Booster
                    BoosterActionButton(
                        title = stringResource(R.string.game_hint_button, state.remainingHints),
                        icon = Icons.Default.Lightbulb,
                        tint = Color(0xFFFFB703),
                        onClick = onUseHint,
                        enabled = state.status is GameUiStatus.Playing && state.remainingHints > 0,
                        testTag = "booster_hint_button"
                    )

                    // Reveal Pair
                    BoosterActionButton(
                        title = "${stringResource(R.string.game_reveal_button)} (150💰)",
                        icon = Icons.Default.Visibility,
                        tint = Color(0xFF4CC9F0),
                        onClick = onRevealPair,
                        enabled = state.status is GameUiStatus.Playing && coins >= 150,
                        testTag = "booster_reveal_button"
                    )

                    // Freeze Timer
                    BoosterActionButton(
                        title = "${stringResource(R.string.game_freeze_button)} (110💰)",
                        icon = Icons.Default.AcUnit,
                        tint = Color(0xFFE0AAFF),
                        onClick = onFreezeTimer,
                        enabled = state.status is GameUiStatus.Playing && coins >= 110 && !state.isTimerFrozen,
                        testTag = "booster_freeze_button"
                    )
                }
            }
        }

        // Victory Screen Overlay
        if (state.status is GameUiStatus.LevelCompleted) {
            val completed = state.status as GameUiStatus.LevelCompleted
            VictoryScreen(
                completed = completed,
                player = player,
                onNextLevel = onNextLevel,
                onRestartLevel = onRestartLevel,
                onBackToHome = onBackToHome,
                isAdsRemoved = isAdsRemoved
            )
        }

        // Defeat Dialog
        if (state.status is GameUiStatus.Defeat) {
            val defeat = state.status as GameUiStatus.Defeat
            Dialog(
                onDismissRequest = {},
                properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF28111D),
                    tonalElevation = 12.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .border(2.dp, Color(0xFFE63946), RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "💔", fontSize = 56.sp)

                        Text(
                            text = stringResource(R.string.game_defeat_title),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFE63946)
                            ),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Você encontrou ${defeat.pairsFoundCount} de ${state.totalPairs} pares nesta fase.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.8f)),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = onRestartLevel,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE63946)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("try_again_button")
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "TENTAR NOVAMENTE 🔄",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onGoToShop,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.ShoppingBag, contentDescription = null, tint = Color(0xFFFFB703))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Loja", color = Color(0xFFFFB703))
                            }

                            OutlinedButton(
                                onClick = onBackToHome,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.Home, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Menu", color = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        BannerAdView(isAdsRemoved = isAdsRemoved)
                    }
                }
            }
        }
    }
}

@Composable
private fun BoosterActionButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit,
    enabled: Boolean,
    testTag: String
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        color = if (enabled) Color(0xFF261845) else Color(0xFF19122B),
        modifier = Modifier.testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (enabled) tint else Color.Gray,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) Color.White else Color.Gray
                )
            )
        }
    }
}

@Composable
private fun RewardRow(title: String, value: String, isBold: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color.White.copy(alpha = 0.8f),
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = if (isBold) Color(0xFFFFB703) else Color.White,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

private fun formatTime(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}

private data class GridLayoutInfo(
    val cols: Int,
    val rows: Int,
    val cardWidth: Dp,
    val cardHeight: Dp,
    val spacing: Dp,
    val totalGridWidth: Dp,
    val totalGridHeight: Dp
)

private fun calculateOptimalGrid(
    cardCount: Int,
    availableWidth: Dp,
    availableHeight: Dp
): GridLayoutInfo {
    if (cardCount <= 0 || availableWidth <= 0.dp || availableHeight <= 0.dp) {
        return GridLayoutInfo(2, 2, 80.dp, 100.dp, 8.dp, 168.dp, 208.dp)
    }

    val spacing = when {
        cardCount <= 8 -> 8.dp
        cardCount <= 16 -> 6.dp
        cardCount <= 32 -> 4.dp
        else -> 2.5.dp
    }

    var bestCols = 2
    var bestRows = (cardCount + 1) / 2
    var bestCardW = 40.dp
    var bestCardH = 50.dp
    var bestScore = -1f

    val maxColsToTry = minOf(cardCount, 8)
    val minColsToTry = when {
        cardCount >= 36 -> 5
        cardCount >= 16 -> 3
        else -> 2
    }

    for (c in minColsToTry..maxColsToTry) {
        val r = (cardCount + c - 1) / c
        val totalHorizSpacing = spacing * (c - 1)
        val totalVertSpacing = spacing * (r - 1)

        val availWForCards = availableWidth - totalHorizSpacing
        val availHForCards = availableHeight - totalVertSpacing

        if (availWForCards <= 0.dp || availHForCards <= 0.dp) continue

        val maxWPerCard = availWForCards / c
        val maxHPerCard = availHForCards / r

        // Target aspect ratio (width / height) ~ 0.74f
        val targetRatio = 0.74f

        var cW = maxWPerCard
        var cH = cW / targetRatio

        if (cH > maxHPerCard) {
            cH = maxHPerCard
            cW = cH * targetRatio
        }

        if (cW > maxWPerCard) cW = maxWPerCard
        if (cH > maxHPerCard) cH = maxHPerCard

        val area = cW.value * cH.value
        val unusedSlots = (c * r) - cardCount
        val score = area - (unusedSlots * area * 0.04f)

        if (score > bestScore) {
            bestScore = score
            bestCols = c
            bestRows = r
            bestCardW = cW
            bestCardH = cH
        }
    }

    val totalW = (bestCardW * bestCols) + (spacing * (bestCols - 1))
    val totalH = (bestCardH * bestRows) + (spacing * (bestRows - 1))

    return GridLayoutInfo(
        cols = bestCols,
        rows = bestRows,
        cardWidth = bestCardW,
        cardHeight = bestCardH,
        spacing = spacing,
        totalGridWidth = totalW,
        totalGridHeight = totalH
    )
}
