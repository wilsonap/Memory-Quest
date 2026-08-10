package com.example.ui.screens.game.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.audio.GameAudioManager
import com.example.avatar.model.AvatarType
import com.example.avatar.ui.AvatarImage
import com.example.data.local.entity.PlayerEntity
import com.example.ui.components.BannerAdContainer
import com.example.ui.components.BannerAdView
import com.example.ui.viewmodel.GameUiStatus
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun VictoryScreen(
    completed: GameUiStatus.LevelCompleted,
    player: PlayerEntity?,
    onNextLevel: () -> Unit,
    onRestartLevel: () -> Unit,
    onBackToHome: () -> Unit,
    isAdsRemoved: Boolean = false
) {
    val context = LocalContext.current
    val audioManager = remember(context) { GameAudioManager.getInstance(context) }

    // Animation timeline state flags
    var headerVisible by remember { mutableStateOf(false) }
    var visibleStars by remember { mutableStateOf(0) }
    var summaryVisible by remember { mutableStateOf(false) }
    var rewardsVisible by remember { mutableStateOf(false) }
    var xpBarVisible by remember { mutableStateOf(false) }
    var buttonsVisible by remember { mutableStateOf(false) }

    // Coins animation counter
    val animatedCoins = remember { Animatable(0f) }

    // XP Progress animation
    val xpProgressAnim = remember { Animatable(completed.oldXpProgress) }

    // Sequential Animation & Audio Timeline
    LaunchedEffect(completed) {
        // 0 ms: Play victory fanfare & start particles
        audioManager.playVictory()

        // 300 ms: Header entrance
        delay(300)
        headerVisible = true

        // 600 ms: Stars sequence
        delay(300)
        if (completed.starsCount >= 1) {
            visibleStars = 1
            audioManager.playCardFlip()
            delay(250)
        }
        if (completed.starsCount >= 2) {
            visibleStars = 2
            audioManager.playCardFlip()
            delay(250)
        }
        if (completed.starsCount >= 3) {
            visibleStars = 3
            audioManager.playCardFlip()
            delay(250)
        }

        // 1000 ms: Summary cards slide in
        delay(150)
        summaryVisible = true

        // 1400 ms: Rewards section & coin count up
        delay(400)
        rewardsVisible = true
        audioManager.playCoin()
        animatedCoins.animateTo(
            targetValue = completed.coinsEarned.toFloat(),
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )

        // 1800 ms: XP Bar fills smoothly
        delay(200)
        xpBarVisible = true
        xpProgressAnim.animateTo(
            targetValue = completed.newXpProgress,
            animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
        )

        if (completed.isLevelUp) {
            audioManager.playLevelUp()
        }

        if (completed.unlockedAchievement != null) {
            delay(300)
            audioManager.playAchievement()
        }

        // 2200 ms: Buttons fade in
        delay(300)
        buttonsVisible = true
    }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xEE09041A)),
            contentAlignment = Alignment.Center
        ) {
            // Golden background floating particles
            GoldenParticleBackground()

            // Victory confetti falling overlay
            VictoryConfettiOverlay()

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                val maxHeightDp = maxHeight

                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = Color(0xFF1C103B).copy(alpha = 0.95f),
                    border = BorderStroke(
                        width = 1.5.dp,
                        brush = Brush.horizontalGradient(
                            listOf(
                                Color(0xFFFFD700),
                                Color(0xFFFF9E00),
                                Color(0xFFFFD700)
                            )
                        )
                    ),
                    tonalElevation = 16.dp,
                    shadowElevation = 24.dp,
                    modifier = Modifier
                        .fillMaxWidth(if (maxWidth > 600.dp) 0.8f else 0.96f)
                        .padding(vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 1. HEADER (Title, Level Name, Avatar & Record Badge)
                        AnimatedVisibility(
                            visible = headerVisible,
                            enter = fadeIn(tween(400)) + scaleIn(tween(400, easing = FastOutSlowInEasing)) + slideInVertically { -40 }
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                AvatarImage(
                                    avatarType = player?.avatarType ?: AvatarType.PRESET.name,
                                    avatarPresetId = player?.avatarPresetId ?: "avatar_01",
                                    avatarLocalPath = player?.avatarLocalPath ?: "",
                                    size = 60.dp,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .border(2.dp, Color(0xFFFFD700), CircleShape)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "🏆 FASE CONCLUÍDA",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp,
                                        brush = Brush.horizontalGradient(
                                            listOf(Color(0xFFFFE57F), Color(0xFFFFB703), Color(0xFFFFD700))
                                        )
                                    ),
                                    textAlign = TextAlign.Center
                                )

                                val stageTitle = if (completed.themeNameRes != 0) {
                                    stringResource(completed.themeNameRes)
                                } else completed.themeCategory

                                Text(
                                    text = "$stageTitle • Fase ${completed.levelCompletedNumber}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White.copy(alpha = 0.9f)
                                    ),
                                    textAlign = TextAlign.Center
                                )

                                if (completed.isNewRecord) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Surface(
                                        color = Color(0xFFFFB703).copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, Color(0xFFFFD700))
                                    ) {
                                        Text(
                                            text = "🏆 NOVO RECORDE!",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFFFFD700)
                                            ),
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 2. STARS SYSTEM (3 Large Animated Stars)
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (starIndex in 1..3) {
                                val isFilled = starIndex <= visibleStars
                                val starScale by animateFloatAsState(
                                    targetValue = if (isFilled) 1.2f else 0.85f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )

                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 6.dp)
                                        .scale(starScale)
                                ) {
                                    Icon(
                                        imageVector = if (isFilled) Icons.Default.Star else Icons.Default.StarOutline,
                                        contentDescription = "Star $starIndex",
                                        tint = if (isFilled) Color(0xFFFFD700) else Color(0xFF4A3E6B),
                                        modifier = Modifier.size(46.dp)
                                    )
                                }
                            }
                        }

                        val starLabel = when (completed.starsCount) {
                            3 -> "⭐⭐⭐ Perfeito! Sem Erros!"
                            2 -> "⭐⭐ Muito Bom! Poucos Erros!"
                            else -> "⭐ Fase Concluída!"
                        }
                        Text(
                            text = starLabel,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFE57F)
                            ),
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // 3. MATCH SUMMARY CARDS
                        AnimatedVisibility(
                            visible = summaryVisible,
                            enter = fadeIn(tween(400)) + slideInVertically { 30 }
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SummaryChip(
                                        icon = Icons.Default.Timer,
                                        label = "Tempo",
                                        value = formatTime(completed.timeSeconds),
                                        accentColor = Color(0xFF4CC9F0),
                                        modifier = Modifier.weight(1f)
                                    )

                                    SummaryChip(
                                        icon = Icons.Default.Psychology,
                                        label = "Precisão",
                                        value = "${completed.accuracyPercent}%",
                                        accentColor = Color(0xFF06D6A0),
                                        modifier = Modifier.weight(1f)
                                    )

                                    SummaryChip(
                                        icon = Icons.Default.EmojiEvents,
                                        label = "Pares",
                                        value = "${completed.pairsFound}/${completed.totalPairs}",
                                        accentColor = Color(0xFFFFB703),
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SummaryChip(
                                        icon = Icons.Default.Close,
                                        label = "Erros",
                                        value = "${completed.errorsCount}",
                                        accentColor = if (completed.errorsCount == 0) Color(0xFF06D6A0) else Color(0xFFFF4D6D),
                                        modifier = Modifier.weight(1f)
                                    )

                                    SummaryChip(
                                        icon = Icons.Default.LocalFireDepartment,
                                        label = "Maior Combo",
                                        value = "${completed.maxCombo}x",
                                        accentColor = Color(0xFFFF7B00),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 4. RECOMPENSAS & MOEDAS
                        AnimatedVisibility(
                            visible = rewardsVisible,
                            enter = fadeIn(tween(400)) + slideInVertically { 30 }
                        ) {
                            Surface(
                                color = Color(0xFF140B2D),
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, Color(0xFF32225B)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "RECOMPENSAS",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                letterSpacing = 1.sp,
                                                color = Color(0xFFFFD700)
                                            )
                                        )

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "🪙 +${animatedCoins.value.toInt()}",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Black,
                                                    color = Color(0xFFFFD700)
                                                )
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    RewardRowItem("Vitória da Fase", "+40 Moedas 🪙")
                                    if (completed.comboBonus > 0) {
                                        RewardRowItem("Bônus 3 Estrelas ⭐", "+${completed.comboBonus} Moedas 🪙")
                                    }
                                    if (completed.flawlessBonus > 0) {
                                        RewardRowItem("Bônus Sem Ajuda ✨", "+${completed.flawlessBonus} Moedas 🪙")
                                    }
                                    RewardRowItem("Experiência Ganha", "+${completed.xpEarned} XP ⭐")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 5. BARRA DE XP ANIMADA
                        AnimatedVisibility(
                            visible = xpBarVisible,
                            enter = fadeIn(tween(400))
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Progresso de XP",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White.copy(alpha = 0.9f)
                                        )
                                    )
                                    Text(
                                        text = "+${completed.xpEarned} XP",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF06D6A0)
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(12.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF2A1C4E))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(xpProgressAnim.value)
                                            .height(12.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(Color(0xFF7209B7), Color(0xFFF72585), Color(0xFFFFD700))
                                                )
                                            )
                                    )
                                }

                                if (completed.isLevelUp) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Surface(
                                        color = Color(0xFF06D6A0).copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, Color(0xFF06D6A0)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "🎉 NOVO NÍVEL ALCANÇADO!",
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFF06D6A0)
                                            ),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // 6. NOVO DESBLOQUEIO / CONQUISTA
                        if (completed.unlockedAchievement != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                color = Color(0xFF26123D),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, Color(0xFFFFB703)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        tint = Color(0xFFFFD700),
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "🏅 NOVA CONQUISTA!",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFFFFD700)
                                            )
                                        )
                                        Text(
                                            text = completed.unlockedAchievement.title,
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        )
                                        Text(
                                            text = completed.unlockedAchievement.description,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Color.White.copy(alpha = 0.8f)
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // 7. BOTÕES DE AÇÃO (Apenas 3 botões)
                        AnimatedVisibility(
                            visible = buttonsVisible,
                            enter = fadeIn(tween(400)) + slideInVertically { 30 }
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = onNextLevel,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF7209B7)
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .border(
                                            1.dp,
                                            Color(0xFFFFD700).copy(alpha = 0.6f),
                                            RoundedCornerShape(16.dp)
                                        )
                                        .testTag("next_level_button")
                                ) {
                                    Text(
                                        text = "Próxima Fase",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = onRestartLevel,
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.dp, Color(0xFF533F7E)),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = Color(0xFF1B1038)
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Jogar Novamente",
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = onBackToHome,
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.dp, Color(0xFF533F7E)),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = Color(0xFF1B1038)
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Home,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Home",
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        )
                                    }
                                }
                            }

                            // Banner Ad on Victory Screen
                            Spacer(modifier = Modifier.height(16.dp))
                            BannerAdContainer(screenName = "Game_Victory", isAdsRemoved = isAdsRemoved)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryChip(
    icon: ImageVector,
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF140A2B),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFF2E1C55)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            )
        }
    }
}

@Composable
private fun RewardRowItem(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color.White.copy(alpha = 0.8f)
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge.copy(
                color = Color(0xFFFFD700),
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
private fun GoldenParticleBackground() {
    val particles = remember {
        List(24) {
            ParticleData(
                xPct = Random.nextFloat(),
                yPct = Random.nextFloat(),
                radius = Random.nextFloat() * 3f + 1.5f,
                alpha = Random.nextFloat() * 0.5f + 0.2f
            )
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        particles.forEach { p ->
            drawCircle(
                color = Color(0xFFFFD700).copy(alpha = p.alpha),
                radius = p.radius.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(p.xPct * w, p.yPct * h)
            )
        }
    }
}

@Composable
private fun VictoryConfettiOverlay() {
    val confettiList = remember {
        List(32) {
            ConfettiData(
                xPct = Random.nextFloat(),
                yPct = Random.nextFloat() * 0.8f,
                sizeDp = Random.nextFloat() * 6f + 4f,
                color = when (Random.nextInt(4)) {
                    0 -> Color(0xFFFFD700)
                    1 -> Color(0xFF7209B7)
                    2 -> Color(0xFF06D6A0)
                    else -> Color(0xFFFF4D6D)
                }
            )
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        confettiList.forEach { c ->
            drawRect(
                color = c.color.copy(alpha = 0.75f),
                topLeft = androidx.compose.ui.geometry.Offset(c.xPct * w, c.yPct * h),
                size = androidx.compose.ui.geometry.Size(c.sizeDp.dp.toPx(), c.sizeDp.dp.toPx() * 1.5f)
            )
        }
    }
}

private data class ParticleData(val xPct: Float, val yPct: Float, val radius: Float, val alpha: Float)
private data class ConfettiData(val xPct: Float, val yPct: Float, val sizeDp: Float, val color: Color)

private fun formatTime(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}
