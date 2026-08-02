package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

data class Particle(
    val xRatio: Float,
    val yRatio: Float,
    val radius: Float,
    val color: Color,
    val speedY: Float
)

@Composable
fun SparkleParticleOverlay(modifier: Modifier = Modifier) {
    val particles = remember {
        val colors = listOf(
            Color(0xFFFFB703),
            Color(0xFF4CC9F0),
            Color(0xFF06D6A0),
            Color(0xFFFF70A6),
            Color(0xFFE0AAFF)
        )
        List(30) {
            Particle(
                xRatio = Random.nextFloat(),
                yRatio = Random.nextFloat(),
                radius = Random.nextFloat() * 8f + 4f,
                color = colors.random(),
                speedY = Random.nextFloat() * 0.3f + 0.1f
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "particleTransition")
    val offsetYRatio by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particleYAnimation"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        particles.forEach { p ->
            val currentY = ((p.yRatio + offsetYRatio * p.speedY) % 1.0f) * h
            val currentX = p.xRatio * w

            drawCircle(
                color = p.color,
                radius = p.radius,
                center = Offset(currentX, currentY)
            )
        }
    }
}
