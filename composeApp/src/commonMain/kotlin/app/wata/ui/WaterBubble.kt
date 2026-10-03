package app.wata.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

private const val TAU = (2 * PI).toFloat()

/**
 * A glass sphere slowly filling with animated water. [content] is drawn twice — dry and wet
 * (`wet = true`, clipped to the water) — so text can switch color as the
 * waterline passes through it.
 *
 * @param splash 0..1, kicked to 1 after each drink and decaying back; makes the water slosh.
 */
@Composable
fun WaterBubble(
    progress: Float,
    splash: () -> Float,
    modifier: Modifier = Modifier,
    content: @Composable (wet: Boolean) -> Unit,
) {
    val colors = LocalWataColors.current
    val level = remember { Animatable(0f) }
    LaunchedEffect(progress) {
        level.animateTo(progress.coerceIn(0f, 1f), spring(dampingRatio = 0.8f, stiffness = 35f))
    }
    val time = rememberInfiniteTransition(label = "water").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(12_000, easing = LinearEasing)),
        label = "time",
    )
    val backWave = remember { Path() }
    val frontWave = remember { Path() }
    val textClip = remember { Path() }

    Box(modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        // Soft glow and the ripple ring that pulses out after a drink.
        Canvas(Modifier.matchParentSize()) {
            val r = size.minDimension / 2
            drawCircle(
                brush = Brush.radialGradient(
                    0.7f to colors.waterLight.copy(alpha = if (colors.isDark) 0.20f else 0.30f),
                    1f to Color.Transparent,
                    center = center,
                    radius = r * 1.3f,
                ),
                radius = r * 1.3f,
            )
            val s = splash()
            if (s > 0f) {
                drawCircle(
                    color = colors.waterLight.copy(alpha = 0.6f * s),
                    radius = r * (1f + 0.2f * (1f - s)),
                    style = Stroke(width = 1.dp.toPx() + 3.dp.toPx() * s),
                )
            }
        }

        Box(Modifier.matchParentSize().clip(CircleShape).background(colors.bubbleFill)) {
            Canvas(Modifier.matchParentSize()) {
                val t = time.value
                val amp = amplitude(level.value, splash())
                val surface = surfaceY(level.value, amp)
                drawPath(
                    wavePath(backWave, surface - amp * 0.7f, amp * 1.2f, phase = 1.7f - t * TAU * 2, cycles = 1.1f),
                    color = colors.waterBack,
                )
                drawPath(
                    wavePath(frontWave, surface, amp, phase = t * TAU * 3, cycles = 1.4f),
                    brush = Brush.verticalGradient(
                        listOf(colors.waterLight, colors.waterDeep),
                        startY = surface - amp,
                        endY = size.height,
                    ),
                )
                drawBubbles(t, surface)
            }
            Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
                content(false)
            }
            Box(
                Modifier
                    .matchParentSize()
                    .clearAndSetSemantics {} // Same text as the dry layer; read it once.
                    .drawWithContent {
                        val amp = amplitude(level.value, splash())
                        val surface = surfaceY(level.value, amp)
                        clipPath(wavePath(textClip, surface, amp, phase = time.value * TAU * 3, cycles = 1.4f)) {
                            this@drawWithContent.drawContent()
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                content(true)
            }
        }

        // Glass highlight and rim.
        Canvas(Modifier.matchParentSize()) {
            val r = size.minDimension / 2
            val inset = r * 0.17f
            drawArc(
                color = Color.White.copy(alpha = if (colors.isDark) 0.12f else 0.6f),
                startAngle = 198f,
                sweepAngle = 48f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - inset * 2, size.height - inset * 2),
                style = Stroke(width = r * 0.045f, cap = StrokeCap.Round),
            )
            drawCircle(colors.bubbleRing, radius = r - 0.75.dp.toPx(), style = Stroke(1.5.dp.toPx()))
        }
    }
}

/** Waves flatten out when the sphere is nearly empty or full, and grow while sloshing. */
private fun DrawScope.amplitude(level: Float, splash: Float): Float {
    val edge = (min(level, 1f - level) / 0.06f).coerceIn(0f, 1f)
    return size.minDimension * 0.018f * edge * (1f + 2.2f * splash)
}

private fun DrawScope.surfaceY(level: Float, amplitude: Float): Float =
    (size.height + amplitude * 2) + (-amplitude * 2 - (size.height + amplitude * 2)) * level

private fun DrawScope.wavePath(path: Path, surface: Float, amplitude: Float, phase: Float, cycles: Float): Path {
    val w = size.width
    val h = size.height
    path.reset()
    path.moveTo(0f, h)
    val steps = 48
    for (i in 0..steps) {
        val x = w * i / steps
        path.lineTo(x, surface + amplitude * sin(phase + x / w * cycles * TAU))
    }
    path.lineTo(w, h)
    path.close()
    return path
}

private class BubbleSpec(val x: Float, val speed: Int, val radius: Float, val offset: Float)

private val Bubbles = listOf(
    BubbleSpec(0.30f, 3, 0.018f, 0.00f),
    BubbleSpec(0.42f, 2, 0.012f, 0.35f),
    BubbleSpec(0.55f, 4, 0.022f, 0.60f),
    BubbleSpec(0.67f, 3, 0.014f, 0.15f),
    BubbleSpec(0.37f, 5, 0.010f, 0.80f),
    BubbleSpec(0.61f, 2, 0.016f, 0.50f),
    BubbleSpec(0.49f, 4, 0.009f, 0.25f),
)

private fun DrawScope.drawBubbles(t: Float, surface: Float) {
    val depth = size.height - surface
    if (depth < size.height * 0.12f) return
    val r = size.minDimension / 2
    for (b in Bubbles) {
        val p = (t * b.speed + b.offset) % 1f
        val y = size.height - p * depth
        val x = size.width * b.x + sin(p * TAU * 2 + b.offset * 6) * r * 0.03f
        drawCircle(
            color = Color.White.copy(alpha = 0.3f * sin(p * PI.toFloat())),
            radius = r * b.radius * (0.6f + 0.4f * p),
            center = Offset(x, y),
        )
    }
}
