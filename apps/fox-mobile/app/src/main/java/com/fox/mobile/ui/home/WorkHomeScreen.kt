package com.fox.mobile.ui.home

import android.content.Intent
import android.os.Build
import android.provider.AlarmClock
import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.fox.mobile.protocol.FoxStateSpec
import com.fox.mobile.protocol.FoxTask
import com.fox.mobile.ui.depth.WallpaperBox
import com.fox.mobile.ui.drawer.SpotlightSheet
import com.fox.mobile.ui.island.IslandCard
import com.fox.mobile.ui.line.FoxLineBar
import com.fox.mobile.ui.line.FoxLineMode
import com.fox.mobile.ui.line.SecondaryBead
import com.fox.mobile.ui.theme.FoxColors
import com.fox.mobile.ui.theme.FoxFonts
import com.fox.mobile.ui.theme.FoxMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Main Fox Launcher Home Surface (`Fox_Line_v30.html` architecture):
 * 1. Top Clock + Date + Connected Mac Pill (`Enoch's Mac · 2m`)
 * 2. Two Live Cards (`TODAY` Exam/Schedule card + `ON YOUR MAC` Live Task card)
 * 3. 53-State verification switcher chip bar (for walking-skeleton & hardware verification)
 * 4. Refractive Chromatic Search Lens + 56% Spotlight Drawer
 * 5. Bottom-anchored Island Card + 120Hz Fox Line Bar
 */
@Composable
fun WorkHomeScreen(
    currentTask: FoxTask,
    statesByKey: Map<String, FoxStateSpec>,
    initialStateKey: String = "ok",
    modifier: Modifier = Modifier
) {
    var activeStateKey by remember { mutableStateOf(initialStateKey) }
    var islandVisible by remember { mutableStateOf(false) }
    var lineMode by remember { mutableStateOf(FoxLineMode.CONT) }
    var lineProgress by remember { mutableFloatStateOf(0.42f) }
    var searchQuery by remember { mutableStateOf("") }
    var drawerExpanded by remember { mutableStateOf(false) }

    val activeState = statesByKey[activeStateKey]

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FoxColors.BgVoid)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(top = 18.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Upper region: Clock + Mac status pill + 2 Context Cards
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "09:41",
                        color = FoxColors.InkPrimary,
                        fontSize = 54.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = (-1.5).sp
                    )
                    Text(
                        text = "Mon, Nov 9 · Desk Focus",
                        color = FoxColors.InkMuted,
                        fontSize = 13.sp
                    )
                }

                // Mac link status pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(FoxColors.BgCard)
                        .border(1.dp, Color(0x1FF3EFE6), RoundedCornerShape(16.dp))
                        .clickable {
                            lineMode = FoxLineMode.STATE
                            islandVisible = !islandVisible
                        }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(FoxColors.OkMint)
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = "Enoch's Mac · live",
                        color = FoxColors.InkPrimary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Two side-by-side Live Cards (TODAY + ON YOUR MAC)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Card 1: TODAY
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(FoxColors.BgCard)
                        .border(1.dp, Color(0x1FF3EFE6), RoundedCornerShape(20.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = "TODAY",
                        color = FoxColors.InkDim,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Exam tomorrow 9:30",
                        color = FoxColors.InkPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Hall B · Chapters 4–7",
                        color = FoxColors.InkMuted,
                        fontSize = 12.sp
                    )
                }

                // Card 2: ON YOUR MAC (Slice S Live Task Card)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(FoxColors.BgCard)
                        .border(1.dp, Color(0x38F2B33D), RoundedCornerShape(20.dp))
                        .clickable {
                            activeStateKey = "ok"
                            lineMode = FoxLineMode.CONT
                            islandVisible = true
                        }
                        .padding(14.dp)
                ) {
                    Text(
                        text = "ON YOUR MAC",
                        color = FoxColors.Ember,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = currentTask.title,
                        color = FoxColors.InkPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = currentTask.summary ?: "${currentTask.project ?: "Mac"} · ${currentTask.branch ?: "main"}",
                        color = FoxColors.InkMuted,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick state / mode selector strip to test any of the 53 states on-device
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val quickKeys = listOf("ok", "reconnecting", "offline", "pair_code", "redaction", "allowlist", "doze", "clock")
                items(quickKeys) { key ->
                    val selected = activeStateKey == key && islandVisible
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) Color(0x2EF2B33D) else Color(0x14F3EFE6))
                            .clickable {
                                activeStateKey = key
                                lineMode = FoxLineMode.STATE
                                islandVisible = true
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = key,
                            color = if (selected) FoxColors.Ember else FoxColors.InkMuted,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Bottom region: Refractive Search Lens + Island Card + 120Hz Fox Line
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SpotlightSheet(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                isExpanded = drawerExpanded,
                onToggleExpand = { drawerExpanded = !drawerExpanded },
                onContactAction = { _, _ -> }
            )

            Spacer(modifier = Modifier.height(6.dp))

            IslandCard(
                visible = islandVisible,
                stateSpec = activeState,
                onActionClick = { _ -> },
                onDismiss = { islandVisible = false }
            )

            FoxLineBar(
                mode = lineMode,
                progress = lineProgress,
                stateColor = when (activeState?.color) {
                    "#ff4d4d" -> FoxColors.AlertRed
                    "#7fd6a4" -> FoxColors.OkMint
                    else -> FoxColors.Ember
                },
                secondaryBead = SecondaryBead("timer", "Tea 01:40", 0xFFF2B33D),
                onTapPrimary = { islandVisible = !islandVisible },
                onTapBead = {
                    lineMode = FoxLineMode.TIMER
                    islandVisible = true
                },
                onLongPressFox = {
                    lineMode = if (lineMode == FoxLineMode.FOX) FoxLineMode.CONT else FoxLineMode.FOX
                },
                onScrubDelta = { delta ->
                    lineProgress = (lineProgress + delta).coerceIn(0f, 1f)
                }
            )
        }
    }
}

/**
 * Phase 4: The home content (Work block, Continue button, Next alarm row, and Ridge-fitted Words line).
 *
 * Rendered on top of [com.fox.mobile.ui.depth.DepthWallpaperAndClock] in MainActivity.
 *
 * Specs from docs/FOX_MOBILE_PLAN.md (lines 149-166) & docs/prototypes/Fox_Line_v30.html:
 * 1. Work block at left/right 22dp, top 54% of H:
 *    - Sentence "You were writing MenuBarView.swift on your Mac, 4 min ago." (Bricolage 23sp, t2, file name in t1 SemiBold)
 *    - Continue button: 52dp tall, pill shape (26dp radius), inkSurface (60%), 1dp hair border, Bricolage 16sp SemiBold + 18dp right arrow
 *    - .next row: "Exam tomorrow 9:30 · no alarm" + "Set 7:00" chip -> tapping sets AlarmClock intent & changes to "Alarm 7:00" (ok green)
 * 2. Words line 14dp above work block (bottom at 54% of H - 14dp, left 22dp):
 *    - Time-of-day initial word item from SampleWordsRepository
 *    - selectWordsFit(...) dynamically fits quote under the lowest mountain ridge point
 *    - Tapping animates 200ms alpha fade + 4dp blur (or immediate if ANIMATOR_DURATION_SCALE == 0f) and advances to next word
 */
@Composable
fun Phase4HomeContent(
    box: WallpaperBox,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val textMeasurer = rememberTextMeasurer()

    // Words state (read only inside graphicsLayer lambda & click handler for zero recomposition)
    val allWords = remember { SampleWordsRepository.getWords() }
    var wordIndex by remember { mutableIntStateOf(SampleWordsRepository.getInitialIndex()) }
    val swapAlpha = remember { Animatable(1f) }
    var isSwapping by remember { mutableStateOf(false) }

    val animScale = remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            )
        }.getOrDefault(1f)
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val screenW = with(density) { maxWidth.toPx() }
        val screenH = with(density) { maxHeight.toPx() }

        if (screenW <= 0f || screenH <= 0f) return@BoxWithConstraints

        val leftEdgeDp = 22.dp
        val leftEdgePx = with(density) { leftEdgeDp.toPx() }
        val workBlockWidthPx = screenW - 2f * leftEdgePx

        // Pre-compute ridge fit for all words once so swapping is O(1) with zero measure spikes
        val fitsList = remember(screenW, screenH, box) {
            val workTopPx = screenH * 0.54f
            val gapAboveWorkPx = with(density) { 14.dp.toPx() }
            val minClearanceBelowRidgePx = with(density) { 10.dp.toPx() }
            val gapAttrPx = with(density) { 4.dp.toPx() }
            allWords.map { word ->
                selectWordsFit(
                    workTopPx = workTopPx,
                    gapAboveWorkPx = gapAboveWorkPx,
                    minClearanceBelowRidgePx = minClearanceBelowRidgePx,
                    leftEdgePx = leftEdgePx,
                    workBlockWidthPx = workBlockWidthPx,
                    box = box
                ) { maxWidthPx, fontSizeSp ->
                    val quoteResult = textMeasurer.measure(
                        text = AnnotatedString(word.text),
                        style = TextStyle(
                            fontFamily = FoxFonts.Bricolage,
                            fontSize = fontSizeSp.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = (fontSizeSp * 1.35f).sp,
                            letterSpacing = (-0.01).em
                        ),
                        constraints = Constraints(maxWidth = maxWidthPx.roundToInt().coerceAtLeast(1))
                    )
                    val attrText = word.formattedAttribution
                    val attrHeight = if (attrText.isNotEmpty()) {
                        val attrResult = textMeasurer.measure(
                            text = AnnotatedString(attrText),
                            style = TextStyle(
                                fontFamily = FoxFonts.Figtree,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            constraints = Constraints(maxWidth = maxWidthPx.roundToInt().coerceAtLeast(1))
                        )
                        gapAttrPx + attrResult.size.height
                    } else {
                        0f
                    }
                    quoteResult.size.height + attrHeight
                }
            }
        }

        val onWordsTap: () -> Unit = {
            if (!isSwapping) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                if (animScale == 0f) {
                    val (nextIdx, _) = SampleWordsRepository.getNextWord(wordIndex)
                    wordIndex = nextIdx
                } else {
                    coroutineScope.launch {
                        isSwapping = true
                        swapAlpha.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(durationMillis = 140, easing = FoxMotion.EnterEasing)
                        )
                        val (nextIdx, _) = SampleWordsRepository.getNextWord(wordIndex)
                        wordIndex = nextIdx
                        swapAlpha.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(durationMillis = 140, easing = FoxMotion.EnterEasing)
                        )
                        isSwapping = false
                    }
                }
            }
        }

        Layout(
            content = {
                // [0..2] Pre-composed Words line blocks (toggled purely on GPU via graphicsLayer alpha)
                for (idx in allWords.indices) {
                    val word = allWords[idx]
                    val wordFit = fitsList.getOrElse(idx) { WordsFitOption(0.92f, 16f) }
                    Column(
                        modifier = Modifier
                            .semantics { contentDescription = word.contentDescription }
                            .graphicsLayer {
                                compositingStrategy = CompositingStrategy.Offscreen
                                alpha = if (wordIndex == idx) swapAlpha.value else 0f
                            }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onWordsTap()
                            }
                    ) {
                        Text(
                            text = word.text,
                            fontFamily = FoxFonts.Bricolage,
                            fontSize = wordFit.fontSizeSp.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = (wordFit.fontSizeSp * 1.35f).sp,
                            letterSpacing = (-0.01).em,
                            color = FoxColors.t1,
                            style = TextStyle(
                                shadow = Shadow(
                                    color = Color(0xBF000000),
                                    offset = Offset(0f, 1f),
                                    blurRadius = 10f
                                )
                            )
                        )

                        if (word.formattedAttribution.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = word.formattedAttribution,
                                fontFamily = FoxFonts.Figtree,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = FoxColors.t2,
                                style = TextStyle(
                                    shadow = Shadow(
                                        color = Color(0xBF000000),
                                        offset = Offset(0f, 1f),
                                        blurRadius = 10f
                                    )
                                )
                            )
                        }
                    }
                }

                // [3] Work block
                Column {
                    // Work sentence with "MenuBarView.swift" highlighted in t1 SemiBold (650)
                    val sentence = remember {
                        buildAnnotatedString {
                            withStyle(
                                SpanStyle(
                                    color = FoxColors.t2,
                                    fontFamily = FoxFonts.Bricolage,
                                    fontSize = 23.sp,
                                    letterSpacing = (-0.015).em
                                )
                            ) {
                                append("You were writing ")
                                withStyle(
                                    SpanStyle(
                                        color = FoxColors.t1,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                ) {
                                    append("MenuBarView.swift")
                                }
                                append(" on your Mac, 4 min ago.")
                            }
                        }
                    }

                    Text(
                        text = sentence,
                        fontFamily = FoxFonts.Bricolage,
                        fontSize = 23.sp,
                        lineHeight = (23 * 1.22).sp,
                        letterSpacing = (-0.015).em,
                        style = TextStyle(
                            shadow = Shadow(
                                color = Color(0xBF000000),
                                offset = Offset(0f, 1f),
                                blurRadius = 10f
                            )
                        ),
                        modifier = Modifier.graphicsLayer {
                            compositingStrategy = CompositingStrategy.Offscreen
                        }
                    )

                    // 14.dp below sentence: Continue button (isolated recomposition scope)
                    Spacer(modifier = Modifier.height(14.dp))
                    ContinueActionPill()

                    // 12.dp below Continue button: .next row (isolated recomposition scope)
                    Spacer(modifier = Modifier.height(12.dp))
                    NextAlarmRow()
                }
            },
            modifier = Modifier.fillMaxSize()
        ) { measurables, constraints ->
            val leftPaddingPx = 22.dp.roundToPx()
            val availableWidthPx = constraints.maxWidth - leftPaddingPx * 2

            // Measure all 3 Words blocks
            val wordPlaceables = allWords.indices.map { idx ->
                val wordFit = fitsList.getOrElse(idx) { WordsFitOption(0.92f, 16f) }
                val wordsMaxWidthPx = (availableWidthPx * wordFit.maxWidthFraction).roundToInt().coerceAtLeast(1)
                measurables[idx].measure(
                    Constraints(minWidth = 0, maxWidth = wordsMaxWidthPx)
                )
            }

            // Measure Work block
            val workPlaceable = measurables[allWords.size].measure(
                Constraints(minWidth = availableWidthPx, maxWidth = availableWidthPx)
            )

            val workTopY = (constraints.maxHeight * 0.54f).roundToInt()
            val gapPx = 14.dp.roundToPx()

            layout(constraints.maxWidth, constraints.maxHeight) {
                for (placeable in wordPlaceables) {
                    val wordsTopY = workTopY - gapPx - placeable.height
                    placeable.place(x = leftPaddingPx, y = wordsTopY)
                }
                workPlaceable.place(x = leftPaddingPx, y = workTopY)
            }
        }
    }
}

@Composable
private fun ContinueActionPill() {
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    var continueText by remember { mutableStateOf("Continue") }
    val continueInteractionSource = remember { MutableInteractionSource() }
    val isContinuePressed by continueInteractionSource.collectIsPressedAsState()
    val continueScale by animateFloatAsState(
        targetValue = if (isContinuePressed) FoxMotion.PressScale else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "continueScale"
    )

    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = continueScale
                scaleY = continueScale
            }
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(FoxColors.inkSurface)
            .border(1.dp, FoxColors.hair, RoundedCornerShape(26.dp))
            .clickable(
                interactionSource = continueInteractionSource,
                indication = null
            ) {
                if (continueText == "Continue") {
                    coroutineScope.launch {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        continueText = "Sending to your Mac"
                        delay(1100L)
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        continueText = "Open on your Mac"
                        delay(2200L)
                        continueText = "Continue"
                    }
                }
            }
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = continueText,
                fontFamily = FoxFonts.Bricolage,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = FoxColors.t1
            )
            Canvas(modifier = Modifier.size(18.dp)) {
                val w = size.width
                val h = size.height
                val strokeWidth = 1.8f * (w / 24f)
                val stroke = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
                drawLine(
                    color = FoxColors.t1,
                    start = Offset(5f / 24f * w, h * 0.5f),
                    end = Offset(19f / 24f * w, h * 0.5f),
                    strokeWidth = stroke.width,
                    cap = stroke.cap
                )
                val chevronPath = Path().apply {
                    moveTo(13f / 24f * w, 6f / 24f * h)
                    lineTo(19f / 24f * w, 12f / 24f * h)
                    lineTo(13f / 24f * w, 18f / 24f * h)
                }
                drawPath(
                    path = chevronPath,
                    color = FoxColors.t1,
                    style = stroke
                )
            }
        }
    }
}

@Composable
private fun NextAlarmRow() {
    val context = LocalContext.current
    val view = LocalView.current
    var alarmSet by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = if (alarmSet) "Exam tomorrow 9:30" else "Exam tomorrow 9:30 · no alarm",
            fontFamily = FoxFonts.Figtree,
            fontSize = 13.sp,
            color = FoxColors.t2,
            style = TextStyle(
                shadow = Shadow(
                    color = Color(0xBF000000),
                    offset = Offset(0f, 1f),
                    blurRadius = 8f
                )
            )
        )

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(if (alarmSet) Color(0x247FD6A4) else Color(0x29F2B33D))
                .clickable(enabled = !alarmSet) {
                    alarmSet = true
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                        putExtra(AlarmClock.EXTRA_HOUR, 7)
                        putExtra(AlarmClock.EXTRA_MINUTES, 0)
                        putExtra(AlarmClock.EXTRA_MESSAGE, "Exam tomorrow 9:30")
                        putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    runCatching { context.startActivity(intent) }
                }
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(
                text = if (alarmSet) "Alarm 7:00" else "Set 7:00",
                fontFamily = FoxFonts.Figtree,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (alarmSet) FoxColors.ok else FoxColors.ember
            )
        }
    }
}

@Composable
fun WorkHomeScreen(
    box: WallpaperBox,
    modifier: Modifier = Modifier
) {
    Phase4HomeContent(box = box, modifier = modifier)
}
