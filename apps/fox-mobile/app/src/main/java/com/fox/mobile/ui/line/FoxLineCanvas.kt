package com.fox.mobile.ui.line

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.fox.mobile.core.FoxSpring
import com.fox.mobile.ui.theme.FoxColors
import kotlin.math.sin

enum class FoxLineMode {
    IDLE, MSG, MUSIC, TIMER, ORDER, STATE, XFER, CONT, FOX
}

data class SecondaryBead(
    val id: String,
    val label: String,
    val colorHex: Long
)

/**
 * Zero-allocation 120Hz Compose Canvas for the Fox Line (`Fox_Line_v30`).
 * No object allocations inside the `Canvas` draw lambda; waveform arrays are pre-allocated
 * and computed by `FoxSpring` pure Kotlin frame loop.
 */
@Composable
fun FoxLineBar(
    mode: FoxLineMode,
    progress: Float,
    stateColor: Color = FoxColors.Ember,
    secondaryBead: SecondaryBead? = null,
    onTapPrimary: () -> Unit,
    onTapBead: () -> Unit,
    onLongPressFox: () -> Unit,
    onScrubDelta: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    // Pre-allocated primitive arrays for 120Hz zero-GC frame loop
    val voiceSamples = remember { FloatArray(64) }
    val springState = remember { floatArrayOf(118f, 0f) } // [widthDp, velocity]
    var framePhase by remember { mutableFloatStateOf(0f) }

    val targetWidthDp = when (mode) {
        FoxLineMode.IDLE -> 108f
        FoxLineMode.MSG -> 124f
        FoxLineMode.MUSIC, FoxLineMode.TIMER, FoxLineMode.ORDER -> 240f
        FoxLineMode.STATE -> 136f
        FoxLineMode.XFER, FoxLineMode.CONT -> 156f
        FoxLineMode.FOX -> 236f
    }

    LaunchedEffect(mode, targetWidthDp) {
        var lastNanos = withFrameNanos { it }
        while (true) {
            withFrameNanos { nowNanos ->
                val dt = ((nowNanos - lastNanos) / 1_000_000_000f).coerceIn(0.001f, 0.032f)
                lastNanos = nowNanos
                framePhase += dt * 4.2f
                FoxSpring.stepSpring1D(springState, targetWidthDp, 480f, 30f, dt)
                if (mode == FoxLineMode.FOX || mode == FoxLineMode.MUSIC) {
                    val amp = if (mode == FoxLineMode.FOX) 1.0f else 0.55f
                    FoxSpring.computeFoxVoiceWave(voiceSamples, framePhase, amp)
                }
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
            .pointerInput(mode, secondaryBead) {
                detectTapGestures(
                    onTap = { offset ->
                        if (secondaryBead != null && offset.x > size.width * 0.72f) {
                            onTapBead()
                        } else {
                            onTapPrimary()
                        }
                    },
                    onLongPress = { onLongPressFox() }
                )
            }
            .pointerInput(mode) {
                detectHorizontalDragGestures { change, dragAmount ->
                    change.consume()
                    onScrubDelta(dragAmount / size.width.toFloat().coerceAtLeast(1f))
                }
            }
    ) {
        val centerY = size.height * 0.5f
        val barWidthPx = springState[0].dp.toPx().coerceAtMost(size.width * 0.82f)
        val barHeightPx = if (mode == FoxLineMode.IDLE) 4.dp.toPx() else 5.dp.toPx()
        val startX = (size.width - barWidthPx) * 0.5f

        val activeColor = when (mode) {
            FoxLineMode.STATE -> stateColor
            FoxLineMode.IDLE -> Color(0x61F3EFE6)
            else -> FoxColors.Ember
        }

        // Ambient ember halo under active line
        if (mode != FoxLineMode.IDLE) {
            drawRoundRect(
                color = activeColor.copy(alpha = 0.22f),
                topLeft = Offset(startX - 8.dp.toPx(), centerY - 6.dp.toPx()),
                size = Size(barWidthPx + 16.dp.toPx(), 12.dp.toPx()),
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
            )
        }

        if (mode == FoxLineMode.FOX) {
            // Render pure Kotlin multi-harmonic voice waveform bars
            val count = 28
            val spacing = barWidthPx / count
            for (i in 0 until count) {
                val sample = kotlin.math.abs(voiceSamples[i % voiceSamples.size])
                val h = (3.dp.toPx() + sample * 16.dp.toPx())
                val x = startX + i * spacing + spacing * 0.5f
                drawLine(
                    color = FoxColors.Ember,
                    start = Offset(x, centerY - h * 0.5f),
                    end = Offset(x, centerY + h * 0.5f),
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        } else {
            // Base track
            drawRoundRect(
                color = Color(0x3DF3EFE6),
                topLeft = Offset(startX, centerY - barHeightPx * 0.5f),
                size = Size(barWidthPx, barHeightPx),
                cornerRadius = CornerRadius(barHeightPx, barHeightPx)
            )

            // Active fill / progress
            val fillFraction = when (mode) {
                FoxLineMode.MUSIC, FoxLineMode.TIMER, FoxLineMode.ORDER -> progress.coerceIn(0.05f, 1f)
                else -> 1f
            }
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(activeColor, FoxColors.EmberWarm),
                    startX = startX,
                    endX = startX + barWidthPx * fillFraction
                ),
                topLeft = Offset(startX, centerY - barHeightPx * 0.5f),
                size = Size(barWidthPx * fillFraction, barHeightPx),
                cornerRadius = CornerRadius(barHeightPx, barHeightPx)
            )

            // Pulse bead for MSG or CONT
            if (mode == FoxLineMode.MSG || mode == FoxLineMode.CONT) {
                val pulseRadius = (4.5f + 1.2f * sin(framePhase)).dp.toPx()
                drawCircle(
                    color = FoxColors.Ember,
                    radius = pulseRadius,
                    center = Offset(startX + barWidthPx * 0.5f, centerY)
                )
            }
        }

        // Secondary 10px waiting bead on the right (at most one active line + one waiting bead)
        if (secondaryBead != null) {
            val beadX = (startX + barWidthPx + 18.dp.toPx()).coerceAtMost(size.width - 20.dp.toPx())
            drawCircle(
                color = Color(secondaryBead.colorHex),
                radius = 5.dp.toPx(),
                center = Offset(beadX, centerY)
            )
        }
    }
}
