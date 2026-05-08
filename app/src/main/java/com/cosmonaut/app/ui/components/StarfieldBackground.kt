package com.cosmonaut.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.cosmonaut.app.ui.theme.CosmoMotion
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private const val PIXELS_PER_STAR = 3500
private const val NEBULA_GOLD_ALPHA = 0.035f
private const val NEBULA_BLUE_ALPHA = 0.025f
private const val STAR_WARM_WHITE_R = 255
private const val STAR_WARM_WHITE_G = 248
private const val STAR_WARM_WHITE_B = 230
private const val STAR_CORE_R = 255
private const val STAR_CORE_G = 252
private const val STAR_CORE_B = 245
private const val FLICKER_CHANCE = 0.7f
private const val FLICKER_MIN_WAIT_MS = 2000f
private const val FLICKER_MAX_WAIT_MS = 8000f
private const val FLICKER_MIN_DURATION = 100f
private const val FLICKER_MAX_DURATION = 200f

private data class Star(
    val baseX: Float,
    val baseY: Float,
    val size: Float,
    val opacity: Float,
    val twinkleSpeed: Float,
    val twinkleOffset: Float,
    val twinkleIntensity: Float,
    val depth: Float,
    var nextFlicker: Float = Random.nextFloat() * 10000f,
    var flickerDuration: Float = 0f,
    var isFlickering: Boolean = false,
    var flickerStart: Float = 0f,
)

private fun createStar(width: Float, height: Float): Star {
    val depth = Random.nextFloat()
    return Star(
        baseX = Random.nextFloat() * width,
        baseY = Random.nextFloat() * height,
        size = Random.nextFloat() * 1.5f + 0.3f + depth * 0.8f,
        opacity = Random.nextFloat() * 0.6f + 0.2f + depth * 0.2f,
        twinkleSpeed = Random.nextFloat() * 0.015f + 0.005f,
        twinkleOffset = Random.nextFloat() * PI.toFloat() * 2f,
        twinkleIntensity = Random.nextFloat() * 0.4f + 0.1f,
        depth = depth,
    )
}

@Composable
fun StarfieldBackground(modifier: Modifier = Modifier) {
    val isReduced = CosmoMotion.config.isReducedMotion
    var frameTime by remember { mutableLongStateOf(0L) }

    if (!isReduced) {
        LaunchedEffect(Unit) {
            while (isActive) {
                frameTime = awaitFrame() / 1_000_000
            }
        }
    }

    val stars = remember { mutableListOf<Star>() }
    val nebulaGold = remember { Color(149, 117, 52) }
    val nebulaBlue = remember { Color(100, 130, 180) }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val targetCount = (w * h / PIXELS_PER_STAR).toInt().coerceIn(40, 300)
        if (stars.size != targetCount) {
            stars.clear()
            repeat(targetCount) { stars.add(createStar(w, h)) }
        }

        val time = if (isReduced) 0f else frameTime.toFloat()

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    nebulaGold.copy(alpha = NEBULA_GOLD_ALPHA),
                    nebulaGold.copy(alpha = NEBULA_GOLD_ALPHA * 0.3f),
                    Color.Transparent,
                ),
                center = Offset(w * 0.3f, h * 0.4f),
                radius = w * 0.6f,
            ),
            radius = w * 0.6f,
            center = Offset(w * 0.3f, h * 0.4f),
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    nebulaBlue.copy(alpha = NEBULA_BLUE_ALPHA),
                    nebulaBlue.copy(alpha = NEBULA_BLUE_ALPHA * 0.4f),
                    Color.Transparent,
                ),
                center = Offset(w * 0.7f, h * 0.6f),
                radius = w * 0.5f,
            ),
            radius = w * 0.5f,
            center = Offset(w * 0.7f, h * 0.6f),
        )

        for (star in stars) {
            val baseTwinkle = sin(time * star.twinkleSpeed + star.twinkleOffset) *
                star.twinkleIntensity + 0.8f

            if (!star.isFlickering && time > star.nextFlicker) {
                star.isFlickering = true
                star.flickerStart = time
                star.flickerDuration = Random.nextFloat() * FLICKER_MAX_DURATION + FLICKER_MIN_DURATION
            }

            var flickerMultiplier = 1f
            if (star.isFlickering) {
                val flickerProgress = (time - star.flickerStart) / star.flickerDuration
                if (flickerProgress >= 1f) {
                    star.isFlickering = false
                    star.nextFlicker = time + Random.nextFloat() * FLICKER_MAX_WAIT_MS +
                        FLICKER_MIN_WAIT_MS
                } else {
                    flickerMultiplier = 1f + sin(flickerProgress * PI.toFloat() * 4f) * 0.5f
                    if (Random.nextFloat() > FLICKER_CHANCE) {
                        flickerMultiplier *= 0.5f + Random.nextFloat()
                    }
                }
            }

            val currentOpacity = (star.opacity * baseTwinkle * flickerMultiplier).coerceIn(0f, 1f)
            val center = Offset(star.baseX, star.baseY)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(STAR_WARM_WHITE_R, STAR_WARM_WHITE_G, STAR_WARM_WHITE_B,
                            (currentOpacity * 255).toInt()),
                        Color(STAR_WARM_WHITE_R, STAR_WARM_WHITE_G, STAR_WARM_WHITE_B,
                            (currentOpacity * 0.3f * 255).toInt()),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = star.size * 3f,
                ),
                radius = star.size * 3f,
                center = center,
            )

            drawCircle(
                color = Color(STAR_CORE_R, STAR_CORE_G, STAR_CORE_B,
                    (currentOpacity * 255).toInt()),
                radius = star.size,
                center = center,
            )
        }
    }
}
