package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.example.data.model.GameCard
import com.example.ui.theme.ImmersiveBg
import com.example.ui.theme.ImmersiveGold
import com.example.ui.theme.ImmersiveGreen
import com.example.ui.theme.ImmersivePrimary
import com.example.ui.theme.ImmersivePrimaryContainer
import com.example.ui.theme.ImmersiveSecondary
import com.example.ui.theme.ImmersiveSurface
import com.example.ui.theme.ImmersiveSurfaceVariant
import com.example.ui.theme.ImmersiveTextPrimary

@Composable
fun Card3D(
    card: GameCard,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardFrameId: String = "frame_classic",
    cardBgColorHex: Long = 0xFF2B2930
) {
    val rotationY by animateFloatAsState(
        targetValue = if (card.isFaceUp || card.isMatched) 180f else 0f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "cardFlipAnimation"
    )

    val scale by animateFloatAsState(
        targetValue = if (card.isHighlighted) 1.08f else 1.0f,
        animationSpec = tween(durationMillis = 250),
        label = "highlightScaleAnimation"
    )

    val frameBorderBrush = when (cardFrameId) {
        "frame_golden" -> Brush.linearGradient(listOf(ImmersiveGold, ImmersiveSecondary))
        "frame_neon" -> Brush.linearGradient(listOf(ImmersiveGreen, ImmersivePrimary))
        "frame_magic" -> Brush.linearGradient(listOf(ImmersivePrimary, ImmersiveSecondary))
        else -> Brush.linearGradient(listOf(ImmersiveSurfaceVariant, ImmersivePrimary.copy(alpha = 0.5f)))
    }

    BoxWithConstraints(
        modifier = modifier
            .scale(scale)
            .testTag("game_card_${card.id}")
    ) {
        val cardW = maxWidth
        val cardH = maxHeight
        val minDim = min(cardW, cardH)

        val cornerRadius = (minDim * 0.18f).coerceIn(4.dp, 16.dp)
        val shape = RoundedCornerShape(cornerRadius)
        val borderWidth = if (card.isMatched) (minDim * 0.05f).coerceIn(2.dp, 3.5.dp) else (minDim * 0.03f).coerceIn(1.dp, 2.dp)

        val symbolSizeSp = (minDim.value * 0.42f).coerceIn(12f, 36f).sp
        val showName = cardH >= 46.dp && cardW >= 38.dp && card.name.isNotEmpty()
        val nameFontSizeSp = (minDim.value * 0.16f).coerceIn(7f, 11f).sp
        val starSize = (minDim * 0.22f).coerceIn(8.dp, 16.dp)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    this.rotationY = rotationY
                    cameraDistance = 12 * density
                }
                .clip(shape)
                .border(
                    width = borderWidth,
                    brush = if (card.isMatched) Brush.linearGradient(listOf(ImmersiveGreen, ImmersivePrimary)) else frameBorderBrush,
                    shape = shape
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = !card.isFaceUp && !card.isMatched,
                    onClick = onClick
                )
        ) {
            if (rotationY <= 90f) {
                // BACK OF CARD
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    ImmersiveSurface,
                                    ImmersiveBg
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "Carta virada para baixo",
                        tint = ImmersivePrimary.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxSize(0.42f)
                    )
                }
            } else {
                // FRONT OF CARD
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { this.rotationY = 180f }
                        .background(
                            if (card.isMatched) {
                                Brush.verticalGradient(listOf(ImmersivePrimaryContainer, ImmersiveSurfaceVariant))
                            } else {
                                Brush.verticalGradient(listOf(ImmersiveSurface, ImmersiveSurfaceVariant))
                            }
                        )
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = card.symbol,
                            fontSize = symbolSizeSp,
                            textAlign = TextAlign.Center
                        )

                        if (showName) {
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = card.name,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = nameFontSizeSp,
                                    lineHeight = nameFontSizeSp
                                ),
                                color = if (card.isMatched) ImmersiveGreen else ImmersiveTextPrimary,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )
                        }
                    }

                    if (card.isMatched) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Par Encontrado",
                            tint = ImmersiveGold,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(2.dp)
                                .size(starSize)
                        )
                    }
                }
            }
        }
    }
}


