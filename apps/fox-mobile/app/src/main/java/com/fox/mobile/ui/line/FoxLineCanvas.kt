package com.fox.mobile.ui.line

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fox.mobile.ui.theme.FoxColors
import kotlinx.coroutines.flow.first
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Glass spring simulation tracking width, height, and opacity of the glass capsule.
 */
private class GlassSpringState {
    var w: Float = 118f
    var h: Float = 5f
    var o: Float = 0f
    var vw: Float = 0f
    var vh: Float = 0f

    fun step(
        dt: Float,
        targetW: Float,
        targetH: Float,
        targetO: Float,
        isAwake: Boolean,
        isHold: Boolean
    ) {
        val k = if (isAwake) 280f else if (isHold) 520f else 760f
        val c = if (isAwake) 20f else if (isHold) 44f else 50f
        val subDt = dt / 4f
        for (i in 0 until 4) {
            vw += ((targetW - w) * k - vw * c) * subDt
            w += vw * subDt
            vh += ((targetH - h) * k - vh * c) * subDt
            h += vh * subDt
        }
        val speed = if (targetO > o) 0.35f else 0.30f
        o += (targetO - o) * speed
        if (o < 0.002f && targetO <= 0f) {
            o = 0f
        }
    }
}

private fun easeOut(x: Float): Float {
    val clamped = x.coerceIn(0f, 1f)
    return 1f - (1f - clamped).pow(3)
}

private fun warmColor(g: Float, alpha: Float): Color {
    return if (g > 0.03f) {
        val green = (236f - 66f * g).roundToInt().coerceIn(0, 255)
        val blue = (214f - 166f * g).roundToInt().coerceIn(0, 255)
        Color(255, green, blue, (alpha * 255).roundToInt().coerceIn(0, 255))
    } else {
        Color(246, 240, 230, (alpha * 255).roundToInt().coerceIn(0, 255))
    }
}

/**
 * Phase 5: The Fox Line Touch Strip (56dp tall).
 * Anchored at the bottom of the screen.
 * Zero-allocation 120Hz Compose Canvas frame loop with zero CPU load when idle and settled.
 */
@Composable
fun FoxLineTouchStrip(
    state: FoxLineState,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val screenWidthDp = config.screenWidthDp.toFloat()

    val screenWidthPx = with(density) { screenWidthDp.dp.toPx() }
    val initialLineWPx = with(density) { 118.dp.toPx() }

    val gs = remember { GlassSpringState().apply { w = initialLineWPx } }
    var frameNowSec by remember { mutableFloatStateOf(FoxLineState.nowSec()) }
    var lvW by remember { mutableFloatStateOf(initialLineWPx) }
    var lvBead by remember { mutableFloatStateOf(0f) }
    var lvBeadType by remember { mutableStateOf<FoxActivityType?>(null) }
    var lvPress by remember { mutableFloatStateOf(0f) }
    var beadXPx by remember { mutableFloatStateOf(0f) }
    var ordP by remember { mutableFloatStateOf(0f) }

    var holdB1Fired by remember { mutableStateOf(false) }
    var holdB2Fired by remember { mutableStateOf(false) }
    var wasDelivered by remember { mutableStateOf(false) }
    var ringingFired by remember { mutableStateOf(false) }

    // Pre-allocated text paint for message badge
    val textPaint = remember {
        Paint().apply {
            isAntiAlias = true
            color = 0xFF16171C.toInt()
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
    }

    // 120Hz Frame Loop: SUSPENDS completely when idle and settled
    LaunchedEffect(Unit) {
        while (true) {
            // Wait until any animation or dynamic activity requires a frame loop
            snapshotFlow {
                val now = FoxLineState.nowSec()
                val list = state.activeList(now)
                val targetW = when {
                    state.foxAwake && !state.foxClosing -> screenWidthPx * 0.6f
                    list.isNotEmpty() -> when (list[0]) {
                        FoxActivityType.FOX -> screenWidthPx * 0.6f
                        FoxActivityType.XFER -> with(density) { 160.dp.toPx() }
                        FoxActivityType.ORDER -> with(density) { 210.dp.toPx() }
                        FoxActivityType.CONT -> with(density) { 160.dp.toPx() }
                        FoxActivityType.TIMER -> with(density) { 140.dp.toPx() }
                        FoxActivityType.MUSIC -> with(density) { 150.dp.toPx() }
                        FoxActivityType.IDLE, FoxActivityType.MSG -> with(density) { 118.dp.toPx() }
                    }
                    else -> with(density) { 118.dp.toPx() }
                }
                val hasSec = list.size > 1
                val targetBead = if (hasSec) 1f else 0f
                val widthUnsettled = abs(targetW - lvW) > 0.5f || abs(targetBead - lvBead) > 0.01f
                val glassUnsettled = gs.o > 0.005f || abs(gs.vw) > 0.5f || abs(gs.vh) > 0.5f

                state.needsFrameLoop() || widthUnsettled || glassUnsettled || lvPress > 0.01f
            }.first { it }

            var lastFrameNanos = withFrameNanos { it }
            while (true) {
                withFrameNanos { nowNanos ->
                    val dt = ((nowNanos - lastFrameNanos) / 1_000_000_000f).coerceIn(0.001f, 0.032f)
                    lastFrameNanos = nowNanos
                    val now = FoxLineState.nowSec()
                    frameNowSec = now

                    // Tick state engine
                    state.tick(now, dt)

                    // Step order smoothing
                    val oStage = state.orderStage(now)
                    if (oStage != null) {
                        ordP += (oStage.progress - ordP) * 0.06f
                        if (oStage.progress >= 1f && !wasDelivered) {
                            wasDelivered = true
                            FoxLineState.performFoxBuzz(view, longArrayOf(10L, 30L, 10L))
                        }
                    } else {
                        ordP = 0f
                        wasDelivered = false
                    }

                    // Timer alarm haptics
                    val tDone = state.timer?.done == true
                    if (tDone && !ringingFired) {
                        ringingFired = true
                        FoxLineState.performFoxBuzz(view, longArrayOf(30L, 60L, 30L))
                    } else if (!tDone) {
                        ringingFired = false
                    }

                    // Hold progress and buzz milestones
                    if (state.holdActive) {
                        val p = ((now - state.holdT0Sec) / 1.1f).coerceIn(0f, 1f)
                        state.holdProgress = p
                        if (p > 0.33f && !holdB1Fired) {
                            holdB1Fired = true
                            FoxLineState.performFoxBuzz(view, 6L)
                        }
                        if (p > 0.66f && !holdB2Fired) {
                            holdB2Fired = true
                            FoxLineState.performFoxBuzz(view, 8L)
                        }
                        if (p >= 1f) {
                            state.holdActive = false
                            state.summonFox()
                            FoxLineState.performFoxBuzz(view, longArrayOf(8L, 30L, 14L))
                        }
                    } else {
                        holdB1Fired = false
                        holdB2Fired = false
                    }

                    // Glass spring step
                    val g = if (state.holdActive) easeOut(state.holdProgress) else 0f
                    val awake = state.foxAwake && !state.foxClosing
                    val ready = if (state.holdActive && state.holdProgress > 0.8f) {
                        (state.holdProgress - 0.8f) / 0.2f
                    } else 0f

                    val targetGsW = if (awake) {
                        screenWidthPx * 0.66f
                    } else {
                        initialLineWPx + (screenWidthPx * 0.58f - initialLineWPx) * g + sin(now * 38f) * with(density) { 2.5.dp.toPx() } * ready
                    }
                    val targetGsH = if (awake) {
                        with(density) { 44.dp.toPx() }
                    } else {
                        with(density) { 5.dp.toPx() + 31.dp.toPx() * g } + sin(now * 38f + 1f) * with(density) { 1.2.dp.toPx() } * ready
                    }
                    val targetGsO = if (awake) 1f else (g * 4f).coerceIn(0f, 1f)

                    gs.step(
                        dt = dt,
                        targetW = targetGsW,
                        targetH = targetGsH,
                        targetO = targetGsO,
                        isAwake = awake,
                        isHold = state.holdActive
                    )

                    // Line width & bead interpolation
                    val active = state.activeList(now)
                    val listening = state.foxAwake && !state.foxClosing
                    val prim = if (listening) FoxActivityType.FOX else active.firstOrNull() ?: FoxActivityType.IDLE
                    val sec = if (listening) null else active.getOrNull(1)

                    val targetPrimW = when (prim) {
                        FoxActivityType.FOX -> screenWidthPx * 0.6f
                        FoxActivityType.XFER -> with(density) { 160.dp.toPx() }
                        FoxActivityType.ORDER -> with(density) { 210.dp.toPx() }
                        FoxActivityType.CONT -> with(density) { 160.dp.toPx() }
                        FoxActivityType.TIMER -> with(density) { 140.dp.toPx() }
                        FoxActivityType.MUSIC -> with(density) { 150.dp.toPx() }
                        FoxActivityType.IDLE, FoxActivityType.MSG -> with(density) { 118.dp.toPx() }
                    }

                    if (gs.o > 0.02f && (prim == FoxActivityType.IDLE || prim == FoxActivityType.FOX)) {
                        val subW = if (prim == FoxActivityType.FOX) with(density) { 44.dp.toPx() } else with(density) { 30.dp.toPx() }
                        lvW = max(initialLineWPx, gs.w - subW)
                    } else {
                        lvW += (targetPrimW - lvW) * 0.14f
                    }

                    val targetBeadVal = if (sec != null) 1f else 0f
                    lvBead += (targetBeadVal - lvBead) * 0.12f
                    if (sec != null) lvBeadType = sec

                    lvPress *= 0.86f
                }

                // Check termination condition for frame loop
                val now = FoxLineState.nowSec()
                val list = state.activeList(now)
                val targetW = when {
                    state.foxAwake && !state.foxClosing -> screenWidthPx * 0.6f
                    list.isNotEmpty() -> when (list[0]) {
                        FoxActivityType.FOX -> screenWidthPx * 0.6f
                        FoxActivityType.XFER -> with(density) { 160.dp.toPx() }
                        FoxActivityType.ORDER -> with(density) { 210.dp.toPx() }
                        FoxActivityType.CONT -> with(density) { 160.dp.toPx() }
                        FoxActivityType.TIMER -> with(density) { 140.dp.toPx() }
                        FoxActivityType.MUSIC -> with(density) { 150.dp.toPx() }
                        FoxActivityType.IDLE, FoxActivityType.MSG -> with(density) { 118.dp.toPx() }
                    }
                    else -> with(density) { 118.dp.toPx() }
                }
                val hasSec = list.size > 1
                val targetBead = if (hasSec) 1f else 0f
                val widthSettled = abs(targetW - lvW) <= 0.5f && abs(targetBead - lvBead) <= 0.01f
                val glassSettled = gs.o <= 0.005f && abs(gs.vw) <= 0.5f && abs(gs.vh) <= 0.5f

                if (!state.needsFrameLoop() && widthSettled && glassSettled && lvPress <= 0.01f) {
                    break
                }
            }
        }
    }

    // Touch strip container (56dp tall, hidden when island is open via GPU graphicsLayer)
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .graphicsLayer {
                alpha = if (state.openIslandType != null) 0f else 1f
            }
            .pointerInput(state) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val downX = down.position.x
                        var swipeStartX = downX

                        // Stop completed timer on tap anywhere
                        if (state.timer?.done == true) {
                            state.timer = null
                            state.closeIsland()
                            FoxLineState.performFoxBuzz(view, 6L)
                            continue
                        }

                        // Bead hit test (48dp hit zone centered at beadXPx)
                        val hasBead = lvBead > 0.5f && lvBeadType != null && state.activeList().size > 1
                        val beadHitRadius = 24.dp.toPx()
                        val isBeadHit = hasBead && abs(downX - beadXPx) <= beadHitRadius

                        if (isBeadHit) {
                            lvPress = 1f
                            FoxLineState.performFoxBuzz(view, 6L)
                            val up = waitForUpOrCancellation()
                            if (up != null && abs(up.position.x - beadXPx) <= 30.dp.toPx()) {
                                val sec = state.activeList().getOrNull(1)
                                if (sec != null) {
                                    state.openIsland(sec)
                                }
                            }
                            continue
                        }

                        // Close Fox if awake
                        if (state.foxAwake) {
                            state.closeFox()
                            FoxLineState.performFoxBuzz(view, 5L)
                            continue
                        }

                        // Start hold
                        state.holdActive = true
                        state.holdT0Sec = FoxLineState.nowSec()
                        state.holdProgress = 0f
                        FoxLineState.performFoxBuzz(view, 4L)

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) {
                                // Pointer released
                                val upX = change.position.x
                                val deltaX = upX - swipeStartX
                                val wasHold = state.holdActive

                                state.holdActive = false

                                if (abs(deltaX) > 30.dp.toPx() && state.activeList().size > 1) {
                                    state.swapped = !state.swapped
                                    FoxLineState.performFoxBuzz(view, 6L)
                                } else if (wasHold) {
                                    if (state.holdProgress < 0.15f) {
                                        if (state.openIslandType == null) {
                                            state.openIsland()
                                        } else {
                                            state.closeIsland()
                                        }
                                    } else {
                                        FoxLineState.performFoxBuzz(view, 4L)
                                    }
                                }
                                break
                            } else {
                                // Pointer dragging: cancel hold if horizontal movement exceeds 14dp
                                val currX = change.position.x
                                if (abs(currX - swipeStartX) > 14.dp.toPx()) {
                                    state.holdActive = false
                                }
                            }
                        }
                    }
                }
            }
    ) {
        val now = if (frameNowSec > 0f) frameNowSec else FoxLineState.nowSec()
        val baseCenterY = 44.dp.toPx()
        val rise = max(0f, gs.h / 2f - 4.dp.toPx()) * min(1f, gs.o * 1.5f)
        val Y = baseCenterY - rise

        // 1. Draw Glass Capsule (gbar) behind line when gs.o > 0.01f (NO orb, NO halo!)
        if (gs.o > 0.01f) {
            val capsuleLeft = (size.width - gs.w) / 2f
            val capsuleTop = Y - gs.h / 2f
            val cornerRadius = CornerRadius(gs.h / 2f, gs.h / 2f)

            // Fill: subtle rgba(255, 255, 255, 0.10)
            drawRoundRect(
                color = Color.White.copy(alpha = 0.10f * gs.o),
                topLeft = Offset(capsuleLeft, capsuleTop),
                size = Size(gs.w, gs.h),
                cornerRadius = cornerRadius
            )
            // Border: 1dp FoxColors.hair
            drawRoundRect(
                color = FoxColors.hair.copy(alpha = FoxColors.hair.alpha * gs.o),
                topLeft = Offset(capsuleLeft, capsuleTop),
                size = Size(gs.w, gs.h),
                cornerRadius = cornerRadius,
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // Determine active primary and secondary items
        val active = state.activeList(now)
        val listening = state.foxAwake && !state.foxClosing
        val prim = if (listening) FoxActivityType.FOX else active.firstOrNull() ?: FoxActivityType.IDLE
        val sec = if (listening) null else active.getOrNull(1)

        val br = 6.dp.toPx() * lvBead
        val gap = 12.dp.toPx() * lvBead
        val total = lvW + if (lvBead > 0.02f) gap + br * 2f else 0f
        val x0 = (size.width - total) / 2f

        // 2. Render Primary Line Shape
        renderPrimaryLine(
            type = prim,
            x0 = x0,
            w = lvW,
            Y = Y,
            now = now,
            state = state,
            ordP = ordP,
            textPaint = textPaint
        )

        // 3. Render Secondary Bead (if present)
        if (lvBead > 0.02f && lvBeadType != null) {
            val beadX = x0 + lvW + gap + br
            beadXPx = beadX
            val beadAlpha = min(1f, lvBead * 1.2f)
            val currentRadius = br * (1f + 0.35f * lvPress)

            renderSecondaryBead(
                type = lvBeadType!!,
                beadX = beadX,
                radius = currentRadius,
                Y = Y,
                now = now,
                alpha = beadAlpha,
                state = state
            )
        }
    }
}

/**
 * Draws the primary line shape matching prototype LINE[prim].
 */
private fun DrawScope.renderPrimaryLine(
    type: FoxActivityType,
    x0: Float,
    w: Float,
    Y: Float,
    now: Float,
    state: FoxLineState,
    ordP: Float,
    textPaint: Paint
) {
    when (type) {
        FoxActivityType.IDLE -> {
            val g = if (state.holdActive) easeOut(state.holdProgress) else 0f
            val h = 5.dp.toPx() + 2.dp.toPx() * g
            val color = warmColor(g, 0.85f + 0.15f * g)
            if (!drawWavyBarIfActive(state, now, x0, w, Y, h, color)) {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x0, Y - h / 2f),
                    size = Size(w, h),
                    cornerRadius = CornerRadius(h / 2f, h / 2f)
                )
            }
        }
        FoxActivityType.MSG -> {
            // Base idle bar
            val g = if (state.holdActive) easeOut(state.holdProgress) else 0f
            val h = 5.dp.toPx() + 2.dp.toPx() * g
            val color = warmColor(g, 0.85f + 0.15f * g)
            if (!drawWavyBarIfActive(state, now, x0, w, Y, h, color)) {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x0, Y - h / 2f),
                    size = Size(w, h),
                    cornerRadius = CornerRadius(h / 2f, h / 2f)
                )
            }

            val m = state.msg
            val age = if (m != null) now - m.t0Sec else 1f
            val grow = (age / 0.35f).coerceIn(0f, 1f)
            val easeGrow = 1f - (1f - grow).pow(3)
            val r = 11.dp.toPx() * easeGrow
            val cx = x0 + w / 2f
            val cy = Y - 12.dp.toPx() * grow

            // Neck connecting bar to bead
            val neckPath = Path().apply {
                moveTo(cx - 9.dp.toPx(), Y)
                quadraticTo(cx, Y - 2.dp.toPx(), cx - r * 0.7f, cy + r * 0.5f)
                lineTo(cx + r * 0.7f, cy + r * 0.5f)
                quadraticTo(cx, Y - 2.dp.toPx(), cx + 9.dp.toPx(), Y)
                close()
            }
            drawPath(neckPath, color = Color(0xF2F6F0E6))
            drawCircle(color = Color(0xF2F6F0E6), radius = r, center = Offset(cx, cy))

            // Badge text inside drop circle
            if (m != null && grow > 0.7f) {
                textPaint.textSize = 11.sp.toPx()
                val badgeText = if (m.count > 1) m.count.toString() else m.from.take(1)
                val metrics = textPaint.fontMetrics
                val textY = cy - (metrics.ascent + metrics.descent) / 2f + 0.5.dp.toPx()
                drawContext.canvas.nativeCanvas.drawText(badgeText, cx, textY, textPaint)
            }
        }
        FoxActivityType.MUSIC -> {
            val song = state.songs.getOrElse(state.music?.songIndex ?: 0) { state.songs[0] }
            val playing = state.music?.playing == true
            val color = song.color.copy(alpha = if (playing) 1f else 0.5f)

            val wavePath = Path()
            val stepCount = (w / 2.dp.toPx()).toInt().coerceAtLeast(10)
            for (i in 0..stepCount) {
                val u = i.toFloat() / stepCount
                val fy = if (playing) {
                    (sin(u * 22f + now * 9f) * 4.2.dp.toPx() +
                        sin(u * 45f - now * 14f) * 2.4.dp.toPx() +
                        sin(u * 12f + now * 5f) * 2.4.dp.toPx()) * sin(PI.toFloat() * u)
                } else {
                    sin(u * 20f + now * 2f) * 0.6.dp.toPx() * sin(PI.toFloat() * u)
                }
                val px = x0 + u * w
                val py = Y + fy
                if (i == 0) wavePath.moveTo(px, py) else wavePath.lineTo(px, py)
            }
            drawPath(
                path = wavePath,
                color = color,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
        FoxActivityType.TIMER -> {
            val left = state.timerLeft(now)
            val tot = state.timer?.totalSec ?: 1f
            val done = state.timer?.done == true
            val hot = state.timer != null && left <= 10f
            val h = 5.dp.toPx()

            if (done) {
                val alpha = 0.82f + 0.18f * sin(now * 3f)
                val c = Color(1f, 0.3f, 0.3f, alpha)
                drawRoundRect(
                    color = c,
                    topLeft = Offset(x0, Y - h / 2f),
                    size = Size(w, h),
                    cornerRadius = CornerRadius(h / 2f, h / 2f)
                )
            } else {
                // Background track
                drawRoundRect(
                    color = Color(0x38F6F0E6),
                    topLeft = Offset(x0, Y - h / 2f),
                    size = Size(w, h),
                    cornerRadius = CornerRadius(h / 2f, h / 2f)
                )
                // Active fill
                val fillW = max(h, w * (left / tot).coerceIn(0f, 1f))
                val fillColor = when {
                    hot -> Color(0xFFFF4D4D)
                    left < tot * 0.35f -> Color(0xFFF2B33D)
                    else -> Color(0xFFF6F0E6)
                }
                drawRoundRect(
                    color = fillColor,
                    topLeft = Offset(x0, Y - h / 2f),
                    size = Size(fillW, h),
                    cornerRadius = CornerRadius(h / 2f, h / 2f)
                )
            }
        }
        FoxActivityType.ORDER -> {
            val o = state.orderStage(now) ?: FoxOrderStage(1f, "Delivered", 0, true)
            val del = o.progress >= 1f
            val p = if (del) 1f else ordP
            val a = x0 + 16.dp.toPx()
            val b = x0 + w - 16.dp.toPx()
            val rx = a + (b - a) * p

            // Inactive track
            drawLine(
                color = Color(0x33F6F0E6),
                start = Offset(a, Y),
                end = Offset(b, Y),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Progress line with gradient
            val startCol = if (del) Color(0x807FD6A4) else Color(0x26F2B33D)
            val endCol = if (del) Color(0xFF7FD6A4) else Color(0xFFF2B33D)
            val brush = Brush.horizontalGradient(
                colors = listOf(startCol, endCol),
                startX = a,
                endX = max(a + 1f, rx)
            )
            drawLine(
                brush = brush,
                start = Offset(a, Y),
                end = Offset(rx, Y),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Ticks at 15%, 50%, 85%
            for (s in floatArrayOf(0.15f, 0.5f, 0.85f)) {
                val tx = a + (b - a) * s
                val tickCol = if (p >= s) {
                    if (del) Color(0xFF7FD6A4) else Color(0xFFF6F0E6)
                } else Color(0x4DF6F0E6)

                drawRect(
                    color = tickCol,
                    topLeft = Offset(tx - 0.75.dp.toPx(), Y - 4.dp.toPx()),
                    size = Size(1.5.dp.toPx(), 8.dp.toPx())
                )
            }

            // Bowl icon at left
            drawOrderBowl(x0 + 6.dp.toPx(), Y, if (p < 0.15f) now else 0f)

            // House icon at right
            drawOrderHouse(x0 + w - 6.dp.toPx(), Y, if (!del && p > 0.85f) now else 0f, del)

            // Rider pill if not yet delivered
            if (!del) {
                val pillW = 14.dp.toPx()
                val pillH = 7.dp.toPx()
                drawRoundRect(
                    color = Color(0xFFF2B33D),
                    topLeft = Offset(rx - pillW / 2f, Y - pillH / 2f),
                    size = Size(pillW, pillH),
                    cornerRadius = CornerRadius(pillH / 2f, pillH / 2f)
                )
                drawCircle(
                    color = Color(0xE6140E06),
                    radius = 1.4.dp.toPx(),
                    center = Offset(rx + 3.dp.toPx(), Y)
                )
            }
        }
        FoxActivityType.XFER -> {
            val xf = state.xfer
            val p = if (xf?.done == true) 1f else ((now - (xf?.t0Sec ?: now)) / (xf?.durSec ?: 1.6f)).coerceIn(0f, 1f)
            val ep = 1f - (1f - p) * (1f - p)
            val col = if (xf?.done == true) Color(0xFF7FD6A4) else if (xf?.dir == "in") Color(0xFFAAC8FF) else Color(0xFFF2B33D)
            val gulp = if (xf != null && xf.gulpSec > 0f) max(0f, 1f - (now - xf.gulpSec) / 0.35f) else 0f
            val h = 5.dp.toPx() + 3.dp.toPx() * gulp

            if (xf?.done == true && drawWavyBarIfActive(state, now, x0, w, Y, h, col)) {
                return
            }

            // Track
            drawRoundRect(
                color = Color(0x33F6F0E6),
                topLeft = Offset(x0, Y - h / 2f),
                size = Size(w, h),
                cornerRadius = CornerRadius(h / 2f, h / 2f)
            )

            // Fill
            val fillW = max(h, w * ep)
            val fillX = if (xf?.dir == "in") x0 + w * (1f - ep) else x0
            drawRoundRect(
                color = col,
                topLeft = Offset(fillX, Y - h / 2f),
                size = Size(fillW, h),
                cornerRadius = CornerRadius(h / 2f, h / 2f)
            )
        }
        FoxActivityType.CONT -> {
            val p = if (state.contT0Sec != null) ((now - state.contT0Sec!!) / 1.1f).coerceIn(0f, 1f) else 1f
            val done = p >= 1f
            val h = 5.dp.toPx()

            // Track
            drawRoundRect(
                color = Color(0x38F6F0E6),
                topLeft = Offset(x0, Y - h / 2f),
                size = Size(w, h),
                cornerRadius = CornerRadius(h / 2f, h / 2f)
            )

            // Fill
            val easeP = 1f - (1f - p).pow(3)
            val fillW = max(h, w * easeP)
            val col = if (done) Color(0xFF7FD6A4) else Color(0xFFF2B33D)
            drawRoundRect(
                color = col,
                topLeft = Offset(x0, Y - h / 2f),
                size = Size(fillW, h),
                cornerRadius = CornerRadius(h / 2f, h / 2f)
            )
        }
        FoxActivityType.FOX -> {
            val phrase = if (sin(now * 0.7f) > -0.35f) 1f else 0f
            val syl = max(0f, sin(now * 6.1f)).pow(0.7f) * (0.55f + 0.45f * sin(now * 2.3f + 1f))
            val s = phrase * (0.2f + 0.8f * syl)

            val foxPath = Path()
            val stepCount = (w / 2.dp.toPx()).toInt().coerceAtLeast(10)
            for (i in 0..stepCount) {
                val u = i.toFloat() / stepCount
                val waveY = s * (sin(i * 0.14f - now * 11f) * 6.dp.toPx() + sin(i * 0.05f + now * 4f) * 3.dp.toPx()) * sin(PI.toFloat() * u)
                val px = x0 + u * w
                val py = Y + waveY
                if (i == 0) foxPath.moveTo(px, py) else foxPath.lineTo(px, py)
            }
            drawPath(
                path = foxPath,
                color = Color(0xFFFFB03A),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

/**
 * Draws the secondary bead next to the line matching prototype BEAD[sec].
 */
private fun DrawScope.renderSecondaryBead(
    type: FoxActivityType,
    beadX: Float,
    radius: Float,
    Y: Float,
    now: Float,
    alpha: Float,
    state: FoxLineState
) {
    when (type) {
        FoxActivityType.TIMER -> {
            val left = state.timerLeft(now)
            val tot = state.timer?.totalSec ?: 1f
            val f = if (state.timer?.done == true) 1f else (left / tot).coerceIn(0f, 1f)

            drawCircle(
                color = Color(0x38F6F0E6).copy(alpha = 0.22f * alpha),
                radius = radius,
                center = Offset(beadX, Y),
                style = Stroke(width = 2.dp.toPx())
            )
            val arcCol = if (left <= 10f) Color(0xFFFF4D4D) else Color(0xFFF6F0E6)
            drawArc(
                color = arcCol.copy(alpha = arcCol.alpha * alpha),
                startAngle = -90f,
                sweepAngle = 360f * f,
                useCenter = false,
                topLeft = Offset(beadX - radius, Y - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        FoxActivityType.MUSIC -> {
            val song = state.songs.getOrElse(state.music?.songIndex ?: 0) { state.songs[0] }
            drawCircle(
                color = song.color.copy(alpha = alpha),
                radius = radius * 0.85f,
                center = Offset(beadX, Y)
            )
        }
        FoxActivityType.ORDER -> {
            drawCircle(
                color = Color(0xFFF2B33D).copy(alpha = alpha),
                radius = radius * 0.75f,
                center = Offset(beadX, Y)
            )
        }
        FoxActivityType.MSG -> {
            drawCircle(
                color = Color(0xF2F6F0E6).copy(alpha = 0.95f * alpha),
                radius = radius,
                center = Offset(beadX, Y)
            )
        }
        FoxActivityType.XFER, FoxActivityType.CONT -> {
            drawCircle(
                color = Color(0xFFF2B33D).copy(alpha = alpha),
                radius = radius * 0.8f,
                center = Offset(beadX, Y)
            )
        }
        FoxActivityType.FOX, FoxActivityType.IDLE -> {
            drawCircle(
                color = Color(0xF2F6F0E6).copy(alpha = alpha),
                radius = radius * 0.75f,
                center = Offset(beadX, Y)
            )
        }
    }
}

/**
 * Draws wavy water wave if splash is active.
 */
private fun DrawScope.drawWavyBarIfActive(
    state: FoxLineState,
    now: Float,
    x0: Float,
    w: Float,
    Y: Float,
    h: Float,
    color: Color
): Boolean {
    if (!state.isSplashActive(now)) return false
    val age = now - state.splashT0Sec
    val wavyPath = Path()
    val steps = (w / 3.dp.toPx()).toInt().coerceAtLeast(10)
    for (i in 0..steps) {
        val u = i.toFloat() / steps
        val offset = state.wavyBarOffset(u, age)
        val px = x0 + u * w
        val py = Y + offset
        if (i == 0) wavyPath.moveTo(px, py) else wavyPath.lineTo(px, py)
    }
    drawPath(
        path = wavyPath,
        color = color,
        style = Stroke(width = h, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
    return true
}

private fun DrawScope.drawOrderBowl(x: Float, y: Float, t: Float) {
    val r = 5.dp.toPx()
    drawArc(
        color = Color(0xD9F6F0E6),
        startAngle = 0f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(x - r, y - 1.dp.toPx() - r),
        size = Size(r * 2f, r * 2f),
        style = Stroke(width = 1.5.dp.toPx())
    )
    if (t > 0f) {
        val steam = Path()
        for (i in 0..8) {
            val sx = x + sin(i * 0.8f - t * 6f) * 1.5.dp.toPx()
            val sy = y - 4.dp.toPx() - i.toFloat() * 1.dp.toPx()
            if (i == 0) steam.moveTo(sx, sy) else steam.lineTo(sx, sy)
        }
        drawPath(
            path = steam,
            color = Color(0x80F6F0E6),
            style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

private fun DrawScope.drawOrderHouse(x: Float, y: Float, t: Float, del: Boolean) {
    val s = if (t > 0f) 1f + sin(t * 6f) * 0.1f else 1f
    val col = if (del) Color(0xFF7FD6A4) else Color(0xD9F6F0E6)

    val roof = Path().apply {
        moveTo(x - 5.5.dp.toPx() * s, y)
        lineTo(x, y - 5.dp.toPx() * s)
        lineTo(x + 5.5.dp.toPx() * s, y)
    }
    val walls = Path().apply {
        moveTo(x - 3.6.dp.toPx() * s, y - 1.4.dp.toPx() * s)
        lineTo(x - 3.6.dp.toPx() * s, y + 4.dp.toPx() * s)
        lineTo(x + 3.6.dp.toPx() * s, y + 4.dp.toPx() * s)
        lineTo(x + 3.6.dp.toPx() * s, y - 1.4.dp.toPx() * s)
    }
    drawPath(roof, color = col, style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(walls, color = col, style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/**
 * Phase 5 Ripple Canvas: Draws water ripple drops for phone <-> Mac transfers.
 * Full-screen non-interactive canvas. Draws only when state.drops is non-empty.
 */
@Composable
fun TransferRippleCanvas(
    state: FoxLineState,
    modifier: Modifier = Modifier
) {
    var rippleNowSec by remember { mutableFloatStateOf(FoxLineState.nowSec()) }

    if (state.drops.isNotEmpty()) {
        LaunchedEffect(state.drops.size) {
            while (state.drops.isNotEmpty()) {
                withFrameNanos {
                    rippleNowSec = FoxLineState.nowSec()
                }
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        if (state.drops.isEmpty()) return@Canvas
        val now = if (rippleNowSec > 0f) rippleNowSec else FoxLineState.nowSec()

        state.drops.forEach { d ->
            val age = now - d.t0Sec
            val life = 1.1f + 0.9f * d.strength
            if (age > life + d.ringCount * 0.12f) return@forEach

            val cx = size.width * d.xNorm
            val cy = size.height - d.yOffsetFromBottomDp.dp.toPx()
            val maxRadiusPx = d.maxRadiusDp.dp.toPx()

            for (j in 0 until d.ringCount) {
                val t = age - j * 0.12f
                val k = t / life
                if (t <= 0f || k >= 1f) continue

                val easedK = 1f - (1f - k).pow(3)
                val radFactor = if (d.inward) 1f - easedK else easedK
                val rad = 4.dp.toPx() + radFactor * maxRadiusPx * (1f - j * 0.1f)
                val a = (1f - k).pow(1.6f) * (0.75f - j * 0.12f) * (0.5f + 0.5f * d.strength)
                if (a <= 0f || rad <= 0f) continue

                val lw = (1.2f + 1.6f * d.strength * (1f - k)).dp.toPx()

                // 1. Leading radial glow band on outermost crest
                if (j == 0) {
                    val innerGlow = max(0f, rad - (12f + 40f * d.strength).dp.toPx())
                    if (rad > innerGlow) {
                        val glowBrush = Brush.radialGradient(
                            colors = listOf(
                                d.color.copy(alpha = 0f),
                                d.color.copy(alpha = (a * 0.2f).coerceIn(0f, 1f))
                            ),
                            center = Offset(cx, cy),
                            radius = rad
                        )
                        drawCircle(
                            brush = glowBrush,
                            radius = rad,
                            center = Offset(cx, cy)
                        )
                    }
                }

                // 2. Dark trough behind crest
                val troughRad = max(0f, rad - lw * 1.6f)
                if (troughRad > 0f) {
                    drawCircle(
                        color = Color.Black.copy(alpha = (a * 0.55f).coerceIn(0f, 1f)),
                        radius = troughRad,
                        center = Offset(cx, cy),
                        style = Stroke(width = lw * 1.4f)
                    )
                }

                // 3. Light crest ring
                drawCircle(
                    color = d.color.copy(alpha = a.coerceIn(0f, 1f)),
                    radius = rad,
                    center = Offset(cx, cy),
                    style = Stroke(width = lw)
                )
            }
        }
    }
}

/**
 * Phase 5 Fox Wake Overlay:
 * Dims the screen and presents the bottom row of action chips (.fox-chips)
 * matching lines 987-994 of Fox_Line_v30.html with NO orb and NO HUD.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FoxWakeOverlay(
    state: FoxLineState,
    onContinueMac: () -> Unit,
    onSetAlarm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current

    AnimatedVisibility(
        visible = state.foxAwake && !state.foxClosing,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(250)),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    state.closeFox()
                    FoxLineState.performFoxBuzz(view, 5L)
                }
        ) {
            FlowRow(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 76.dp, start = 16.dp, end = 16.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* prevent backdrop close */ },
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Chip 1: Continue on Mac
                FoxChip(
                    text = "Continue on Mac",
                    onClick = {
                        state.closeFox()
                        FoxLineState.performFoxBuzz(view, 6L)
                        state.triggerContinue()
                        onContinueMac()
                    }
                )

                // Chip 2: Alarm 7:00 (if alarm not yet set)
                if (!state.alarmSet) {
                    FoxChip(
                        text = "Alarm 7:00",
                        onClick = {
                            state.closeFox()
                            FoxLineState.performFoxBuzz(view, 10L)
                            state.alarmSet = true
                            onSetAlarm()
                        }
                    )
                }

                // Chip 3: Summarise Kavin or Focus 25 min
                if (state.msg != null) {
                    FoxChip(
                        text = "Summarise Kavin",
                        onClick = {
                            state.closeFox()
                            FoxLineState.performFoxBuzz(view, 6L)
                            state.openIsland(FoxActivityType.MSG)
                        }
                    )
                } else {
                    FoxChip(
                        text = "Focus 25 min",
                        onClick = {
                            state.closeFox()
                            FoxLineState.performFoxBuzz(view, 8L)
                            state.startTimer(1500f)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FoxChip(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x1AFFFFFF))
            .border(0.5.dp, Color(0x38FFFFFF), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = FoxColors.t1,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// -----------------------------------------------------------------------------
// Legacy Compat API for existing WorkHomeScreen.kt and callers
// -----------------------------------------------------------------------------

enum class FoxLineMode {
    IDLE, MSG, MUSIC, TIMER, ORDER, STATE, XFER, CONT, FOX
}

data class SecondaryBead(
    val id: String,
    val label: String,
    val colorHex: Long
)

@Composable
fun FoxLineBar(
    mode: FoxLineMode,
    progress: Float,
    stateColor: Color = FoxColors.ember,
    secondaryBead: SecondaryBead? = null,
    onTapPrimary: () -> Unit,
    onTapBead: () -> Unit,
    onLongPressFox: () -> Unit,
    onScrubDelta: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val syntheticState = remember { FoxLineState() }

    LaunchedEffect(mode, progress) {
        when (mode) {
            FoxLineMode.IDLE -> syntheticState.clearAll()
            FoxLineMode.MSG -> syntheticState.triggerMessage()
            FoxLineMode.MUSIC -> syntheticState.startMusic()
            FoxLineMode.TIMER -> syntheticState.startTimer(60f)
            FoxLineMode.ORDER -> syntheticState.startOrder()
            FoxLineMode.CONT -> syntheticState.triggerContinue()
            FoxLineMode.XFER -> syntheticState.triggerTransfer("out")
            FoxLineMode.FOX -> syntheticState.summonFox()
            FoxLineMode.STATE -> {
                syntheticState.clearAll()
                syntheticState.splashBar(1f)
            }
        }
    }

    FoxLineTouchStrip(
        state = syntheticState,
        modifier = modifier
    )
}
