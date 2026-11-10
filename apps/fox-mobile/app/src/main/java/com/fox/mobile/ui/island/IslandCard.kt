package com.fox.mobile.ui.island

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fox.mobile.protocol.FoxStateSpec
import com.fox.mobile.ui.line.FoxActivityType
import com.fox.mobile.ui.line.FoxLineState
import com.fox.mobile.ui.theme.FoxColors
import com.fox.mobile.ui.theme.FoxFonts

/**
 * Live Activity Island Card (`Fox_Line_v30.html` lines 161-180, 211-226, 697-758).
 * Renders the bottom-anchored floating card that expands when tapping the Fox Line.
 * Supports Order tracking with delivery rider, countdown Timer with alert mode,
 * Music player with album art canvas, Messages with smart summary, Transfers, and Mac handoff.
 */
@Composable
fun LiveActivityIslandCard(
    state: FoxLineState,
    modifier: Modifier = Modifier
) {
    val openType = state.openIslandType
    val isVisible = openType != null
    val view = LocalView.current

    var now by remember { mutableFloatStateOf(FoxLineState.nowSec()) }

    val needsLiveTick = isVisible && (
        openType == FoxActivityType.ORDER ||
        (openType == FoxActivityType.TIMER && state.timer?.done != true && state.timer?.paused != true) ||
        (openType == FoxActivityType.XFER && state.xfer?.done != true)
    )

    LaunchedEffect(needsLiveTick) {
        if (needsLiveTick) {
            var lastTime = FoxLineState.nowSec()
            while (true) {
                withFrameNanos {
                    val current = FoxLineState.nowSec()
                    val dt = (current - lastTime).coerceIn(0.001f, 0.032f)
                    lastTime = current
                    now = current
                    state.tick(current, dt)
                }
            }
        }
    }

    var lastActiveType by remember { mutableStateOf(FoxActivityType.IDLE) }
    if (openType != null) {
        lastActiveType = openType
    }

    val openProgress by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "islandOpenProgress"
    )

    Box(modifier = modifier.fillMaxSize()) {
        if (isVisible) {
            // Outside-tap catcher matching prototype pointerdown -> closeIsland()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        FoxLineState.performFoxBuzz(view, 4L)
                        state.closeIsland()
                    }
            )
        }

        val activeType = openType ?: lastActiveType
        val isAlert = (activeType == FoxActivityType.TIMER && state.timer?.done == true)
        val surfaceColor = if (isAlert) Color(0xE6460A0C) else Color(0xD912141C)
        val borderColor = if (isAlert) Color(0x73FF4D4D) else FoxColors.hair

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, bottom = 14.dp)
                .graphicsLayer {
                    val p = openProgress
                    alpha = p
                    val s = 0.90f + 0.10f * p
                    scaleX = s
                    scaleY = s
                    translationY = (1f - p) * 16.dp.toPx()
                    transformOrigin = TransformOrigin(0.5f, 1f)
                }
                .clip(RoundedCornerShape(30.dp))
                .background(surfaceColor)
                .border(1.dp, borderColor, RoundedCornerShape(30.dp))
                .then(
                    if (isVisible) {
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { /* Absorb clicks on the island card surface */ }
                    } else {
                        Modifier
                    }
                )
                .padding(start = 18.dp, top = 16.dp, end = 18.dp, bottom = 18.dp)
        ) {
            when (activeType) {
                FoxActivityType.ORDER -> {
                    IslandOrderContent(state = state, now = now)
                }
                FoxActivityType.TIMER -> {
                    IslandTimerContent(state = state, now = now, isAlert = isAlert)
                }
                FoxActivityType.MUSIC -> {
                    IslandMusicContent(state = state)
                }
                FoxActivityType.MSG -> {
                    IslandMsgContent(state = state)
                }
                FoxActivityType.XFER -> {
                    IslandXferContent(state = state, now = now)
                }
                else -> {
                    IslandDefaultContent(state = state)
                }
            }
        }
    }
}

/**
 * Convenience alias for [LiveActivityIslandCard].
 */
@Composable
fun IslandCard(
    state: FoxLineState,
    modifier: Modifier = Modifier
) {
    LiveActivityIslandCard(state = state, modifier = modifier)
}

// =============================================================================
// SUB-CONTENTS FOR EACH ACTIVITY TYPE
// =============================================================================

@Composable
private fun IslandOrderContent(
    state: FoxLineState,
    now: Float
) {
    val stage = state.orderStage(now)
    val isDelivered = stage?.delivered == true
    val etaText = if (isDelivered) "Delivered" else "${stage?.etaMinutes ?: 14} min"
    val subText = if (isDelivered) "at your door" else "to your door"
    val stageLabel = stage?.label ?: "Preparing"
    val progress = stage?.progress ?: 0f

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Spice Route · 2 items",
            fontFamily = FoxFonts.Figtree,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = FoxColors.t2
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = Color(0xFF9FE3BC))) {
                    append(etaText)
                }
                append(" ")
                withStyle(SpanStyle(color = FoxColors.t1)) {
                    append(subText)
                }
            },
            fontFamily = FoxFonts.Bricolage,
            fontWeight = FontWeight.SemiBold,
            fontSize = 27.sp
        )

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = stageLabel,
            fontFamily = FoxFonts.Figtree,
            fontSize = 13.5.sp,
            color = FoxColors.t2
        )

        Spacer(modifier = Modifier.height(14.dp))

        OrderProgressBar(progress = progress)
    }
}

@Composable
private fun IslandTimerContent(
    state: FoxLineState,
    now: Float,
    isAlert: Boolean
) {
    val timer = state.timer
    val isDone = timer?.done == true
    val left = state.timerLeft(now)
    val total = timer?.totalSec?.coerceAtLeast(0.1f) ?: 20f

    val totalSec = left.toInt().coerceAtLeast(0)
    val mins = totalSec / 60
    val secs = totalSec % 60
    val timeFormatted = "%d:%02d".format(mins, secs)

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Timer · Tea",
            fontFamily = FoxFonts.Figtree,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = FoxColors.t2
        )

        Spacer(modifier = Modifier.height(4.dp))

        if (isDone) {
            Text(
                text = "Time's up",
                fontFamily = FoxFonts.Bricolage,
                fontWeight = FontWeight.SemiBold,
                fontSize = 27.sp,
                color = FoxColors.alert
            )
        } else {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = FoxColors.t1)) {
                        append(timeFormatted)
                    }
                    append(" ")
                    withStyle(SpanStyle(color = FoxColors.t2)) {
                        append("left")
                    }
                },
                fontFamily = FoxFonts.Bricolage,
                fontWeight = FontWeight.SemiBold,
                fontSize = 27.sp
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Timer track
        val fillFrac = if (isDone) 1f else (left / total).coerceIn(0f, 1f)
        val trackFillColor = if (isDone) FoxColors.alert else FoxColors.t1

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0x24FFFFFF))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fillFrac)
                    .fillMaxHeight()
                    .background(trackFillColor)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Button Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isDone) {
                IslandButton(
                    text = "Stop",
                    modifier = Modifier.weight(1f),
                    isPrimary = true,
                    isAlert = true,
                    onClick = {
                        state.stopTimer()
                        state.closeIsland()
                    }
                )
                IslandButton(
                    text = "+1 min",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        state.startTimer(60f)
                    }
                )
            } else {
                val isPaused = timer?.paused == true
                IslandButton(
                    text = if (isPaused) "Resume" else "Pause",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        state.toggleTimerPause()
                    }
                )
                IslandButton(
                    text = "+1 min",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        state.addTimerMinute()
                    }
                )
                IslandButton(
                    text = "Cancel",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        state.stopTimer()
                        state.closeIsland()
                    }
                )
            }
        }
    }
}

@Composable
private fun IslandMusicContent(
    state: FoxLineState
) {
    val songIdx = state.music?.songIndex ?: 0
    val song = state.songs.getOrNull(songIdx) ?: state.songs.first()

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Album art Canvas
            Canvas(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
            ) {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(song.bgStart, song.bgEnd),
                        start = Offset.Zero,
                        end = Offset(size.width, size.height)
                    )
                )
                song.blobs.forEach { (blobCol, coords) ->
                    val (bx, by, br) = coords
                    drawCircle(
                        color = blobCol,
                        radius = br * size.minDimension,
                        center = Offset(bx * size.width, by * size.height)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.artist,
                    fontFamily = FoxFonts.Figtree,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = FoxColors.t2
                )
                Text(
                    text = song.title,
                    fontFamily = FoxFonts.Bricolage,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 21.sp,
                    color = FoxColors.t1
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Music track with 30% fill in song color
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0x24FFFFFF))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.30f)
                    .fillMaxHeight()
                    .background(song.color)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Button Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IslandButton(
                text = "Back",
                modifier = Modifier.weight(1f),
                onClick = {
                    state.nextSong()
                }
            )
            val isPlaying = state.music?.playing == true
            IslandButton(
                text = if (isPlaying) "Pause" else "Play",
                modifier = Modifier.weight(1f),
                isPrimary = true,
                onClick = {
                    state.toggleMusicPlay()
                }
            )
            IslandButton(
                text = "Next",
                modifier = Modifier.weight(1f),
                onClick = {
                    state.nextSong()
                }
            )
        }
    }
}

@Composable
private fun IslandMsgContent(
    state: FoxLineState
) {
    val msg = state.msg

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Messages",
            fontFamily = FoxFonts.Figtree,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = FoxColors.t2
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = msg?.from ?: "Kavin",
                fontFamily = FoxFonts.Bricolage,
                fontWeight = FontWeight.SemiBold,
                fontSize = 27.sp,
                color = FoxColors.t1
            )
            val count = msg?.count ?: 1
            if (count > 1) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$count new",
                    fontFamily = FoxFonts.Bricolage,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 27.sp,
                    color = FoxColors.t2
                )
            }
        }

        Spacer(modifier = Modifier.height(5.dp))

        val bodyText = when {
            msg == null -> "No messages"
            msg.summarised -> "Kavin asks if lab is at 9 tomorrow and says to bring the record book."
            else -> msg.text
        }

        Text(
            text = bodyText,
            fontFamily = FoxFonts.Figtree,
            fontSize = 13.5.sp,
            color = FoxColors.t2
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IslandButton(
                text = "Yes, 9 works",
                modifier = Modifier.weight(1f),
                onClick = {
                    state.replyMessage()
                }
            )
            if (msg?.summarised != true) {
                val actionLabel = if ((msg?.count ?: 1) > 1) "Summarise" else "Open"
                IslandButton(
                    text = actionLabel,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        state.summariseMessage()
                    }
                )
            }
        }
    }
}

@Composable
private fun IslandXferContent(
    state: FoxLineState,
    now: Float
) {
    val xfer = state.xfer
    val isOut = xfer?.dir == "out"
    val srcText = if (isOut) "Sending to MacBook Air" else "Receiving from MacBook Air"
    val fileName = xfer?.fileName ?: "MenuBarView.swift"
    val sizeText = xfer?.fileSizeMb ?: "2.4 MB"

    val dur = xfer?.durSec?.coerceAtLeast(0.1f) ?: 1.6f
    val elapsed = now - (xfer?.t0Sec ?: now)
    val progress = (elapsed / dur).coerceIn(0f, 1f)
    val barColor = if (isOut) Color(0xFFF2B33D) else Color(0xFFAAC8FF)

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = srcText,
            fontFamily = FoxFonts.Figtree,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = FoxColors.t2
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = fileName,
            fontFamily = FoxFonts.Bricolage,
            fontWeight = FontWeight.SemiBold,
            fontSize = 27.sp,
            color = FoxColors.t1
        )

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = sizeText,
            fontFamily = FoxFonts.Figtree,
            fontSize = 13.5.sp,
            color = FoxColors.t2
        )

        Spacer(modifier = Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0x24FFFFFF))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(barColor)
            )
        }
    }
}

@Composable
private fun IslandDefaultContent(
    state: FoxLineState
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Last on your Mac · 4 min ago",
            fontFamily = FoxFonts.Figtree,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = FoxColors.t2
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "MenuBarView.swift",
            fontFamily = FoxFonts.Bricolage,
            fontWeight = FontWeight.SemiBold,
            fontSize = 27.sp,
            color = FoxColors.t1
        )

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = "glass_horizon · feat/menubar-view",
            fontFamily = FoxFonts.Figtree,
            fontSize = 13.5.sp,
            color = FoxColors.t2
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IslandButton(
                text = "Timer",
                modifier = Modifier.weight(1f),
                onClick = {
                    state.startTimer(20f)
                    state.openIsland(FoxActivityType.TIMER)
                }
            )
            IslandButton(
                text = "Music",
                modifier = Modifier.weight(1f),
                onClick = {
                    state.startMusic()
                    state.openIsland(FoxActivityType.MUSIC)
                }
            )
            IslandButton(
                text = "Continue",
                modifier = Modifier.weight(1f),
                isPrimary = true,
                onClick = {
                    state.closeIsland()
                    state.triggerContinue()
                }
            )
        }
    }
}

// =============================================================================
// REUSABLE COMPONENTS
// =============================================================================

@Composable
private fun OrderProgressBar(
    progress: Float,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
            .padding(horizontal = 7.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        val totalWidth = maxWidth
        val clampedProgress = progress.coerceIn(0f, 1f)

        // 4.dp tall track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0x24FFFFFF))
        ) {
            // #7FD6A4 fill bar
            Box(
                modifier = Modifier
                    .fillMaxWidth(clampedProgress)
                    .fillMaxHeight()
                    .background(Color(0xFF7FD6A4))
            )
        }

        // Destination dot (#F2B33D circle at right end)
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(14.dp)
                .background(Color(0xFFF2B33D), CircleShape)
                .border(2.dp, Color(0x40F2B33D), CircleShape)
        )

        // Rider (28.dp x 15.dp #F6F0E6 rounded 8.dp pill with #1B1C22 inner window)
        val maxTravel = (totalWidth - 28.dp).coerceAtLeast(0.dp)
        val riderOffset = maxTravel * clampedProgress

        Box(
            modifier = Modifier
                .offset(x = riderOffset)
                .align(Alignment.CenterStart)
                .size(width = 28.dp, height = 15.dp)
                .shadow(4.dp, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFF6F0E6)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(width = 16.dp, height = 8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF1B1C22))
            )
        }
    }
}

@Composable
private fun IslandButton(
    text: String,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
    isAlert: Boolean = false,
    onClick: () -> Unit
) {
    val view = LocalView.current
    val bgColor = when {
        isAlert -> FoxColors.alert
        isPrimary -> FoxColors.t1
        else -> Color(0x17FFFFFF)
    }
    val textColor = when {
        isAlert -> Color.White
        isPrimary -> Color(0xFF121318)
        else -> FoxColors.t1
    }

    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .clickable {
                FoxLineState.performFoxBuzz(view, 6L)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontFamily = FoxFonts.Figtree,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.5.sp,
            maxLines = 1
        )
    }
}

// =============================================================================
// BACKWARDS COMPATIBILITY OVERLOAD
// =============================================================================

/**
 * Backwards compatibility overload for [com.fox.mobile.ui.home.WorkHomeScreen].
 */
@Composable
fun IslandCard(
    visible: Boolean,
    stateSpec: FoxStateSpec?,
    onActionClick: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible || stateSpec == null) return

    val dotColor = when (stateSpec.color) {
        "#ff4d4d" -> FoxColors.alert
        "#7fd6a4" -> FoxColors.ok
        "#f2b33d" -> FoxColors.ember
        else -> FoxColors.t2
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(FoxColors.inkSurface)
            .border(1.dp, FoxColors.hair, RoundedCornerShape(26.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(dotColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stateSpec.tag.uppercase(),
                    color = FoxColors.t2,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.1.sp
                )
            }
            Text(
                text = stateSpec.sub,
                color = FoxColors.hair,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = stateSpec.title,
            color = FoxColors.t1,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stateSpec.body,
            color = FoxColors.t2,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )

        if (stateSpec.code != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x14F2B33D))
                    .border(1.dp, Color(0x47F2B33D), RoundedCornerShape(14.dp))
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stateSpec.code,
                    color = FoxColors.ember,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 3.sp
                )
            }
        }

        if (stateSpec.actions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                stateSpec.actions.forEachIndexed { idx, action ->
                    val isPrimary = idx == 0
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isPrimary) Color(0x2EF2B33D) else Color(0x14F3EFE6))
                            .border(
                                1.dp,
                                if (isPrimary) Color(0x52F2B33D) else Color(0x1FF3EFE6),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                onActionClick(action)
                                if (action.equals("Dismiss", ignoreCase = true) ||
                                    action.equals("OK", ignoreCase = true)
                                ) {
                                    onDismiss()
                                }
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = action,
                            color = if (isPrimary) FoxColors.ember else FoxColors.t1,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
