package com.fox.mobile

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.AlarmClock
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.ViewCompat
import com.fox.mobile.ui.depth.DepthWallpaperAndClock
import com.fox.mobile.ui.dashboard.FoxDashboard
import com.fox.mobile.ui.drawer.SpotlightSheet
import com.fox.mobile.ui.home.Phase4HomeContent
import com.fox.mobile.ui.island.LiveActivityIslandCard
import com.fox.mobile.ui.line.FoxLineDebugMenu
import com.fox.mobile.ui.line.FoxLineState
import com.fox.mobile.ui.line.FoxLineTouchStrip
import com.fox.mobile.ui.line.FoxWakeOverlay
import com.fox.mobile.ui.line.TransferRippleCanvas
import com.fox.mobile.ui.theme.FoxTheme

class MainActivity : ComponentActivity() {
    private var bottomSystemSafeInsetDp by mutableFloatStateOf(0f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestHighRefreshRate()

        // Configure edge-to-edge drawing
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        // Keep light status-bar icons visible and hide system bottom gesture pill
        // so the Fox Line is the sole bottom bar
        hideNavigationBar()
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, insets ->
            val visible = insets.isVisible(WindowInsetsCompat.Type.navigationBars())
            val gestureInsetPx = insets.getInsets(WindowInsetsCompat.Type.systemGestures()).bottom
            val visibleBarInsetPx = if (visible) insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom else 0
            val indicatorShown = runCatching {
                Settings.System.getInt(contentResolver, "show_navigationbar", 1) == 1
            }.getOrDefault(true)
            bottomSystemSafeInsetDp = if (indicatorShown) {
                maxOf(gestureInsetPx, visibleBarInsetPx) / resources.displayMetrics.density + 24f
            } else 0f
            insets
        }
        ViewCompat.requestApplyInsets(window.decorView)

        setContent {
            FoxTheme {
                val foxLineState = remember { FoxLineState() }
                val foxBottomClearance = bottomSystemSafeInsetDp.dp
                var drawerExpanded by remember { androidx.compose.runtime.mutableStateOf(false) }
                var searchQuery by remember { androidx.compose.runtime.mutableStateOf("") }
                var dashboardOpen by remember { androidx.compose.runtime.mutableStateOf(false) }
                var dashboardDragProgress by remember { androidx.compose.runtime.mutableStateOf<Float?>(null) }
                val animatedDashboardProgress by animateFloatAsState(
                    targetValue = if (dashboardOpen) 1f else 0f,
                    animationSpec = spring(dampingRatio = 0.88f, stiffness = 240f),
                    label = "ridgeDashboard"
                )
                val dashboardProgress = dashboardDragProgress ?: animatedDashboardProgress
                BackHandler(dashboardOpen) { dashboardOpen = false }
                val drawerProgress by animateFloatAsState(
                    targetValue = if (drawerExpanded) 1f else 0f,
                    animationSpec = tween(220, easing = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)),
                    label = "drawerHomeTransition"
                )
                Box(Modifier.fillMaxSize()) {
                DepthWallpaperAndClock(
                    modifier = Modifier.fillMaxSize()
                        .graphicsLayer {
                            alpha = 1f - 0.5f * drawerProgress
                            scaleX = 1f - 0.025f * drawerProgress
                            scaleY = 1f - 0.025f * drawerProgress
                        }
                        .pointerInput(drawerExpanded, dashboardOpen) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                                val eligible = !drawerExpanded && down.position.y < size.height * .82f
                                val startY = down.position.y
                                val startX = down.position.x
                                val startProgress = animatedDashboardProgress
                                var dragging = false
                                var finished = false
                                while (!finished) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (!change.pressed) {
                                        if (dragging) dashboardOpen = (dashboardDragProgress ?: animatedDashboardProgress) >= .5f
                                        if (dragging) dashboardDragProgress = null
                                        finished = true
                                        continue
                                    }
                                    val dy = change.position.y - startY
                                    val dx = change.position.x - startX
                                    if (eligible && !dragging && kotlin.math.abs(dy) > viewConfiguration.touchSlop && kotlin.math.abs(dy) > kotlin.math.abs(dx)) {
                                        dragging = true
                                        dashboardDragProgress = startProgress
                                    }
                                    if (dragging) {
                                        dashboardDragProgress = (startProgress + dy / (size.height * .52f)).coerceIn(0f, 1f)
                                        change.consume()
                                    }
                                }
                                if (dragging) dashboardDragProgress = null
                            }
                        },
                    dashProgressProvider = { dashboardProgress },
                    dashboardContent = {
                        if (dashboardOpen || dashboardProgress > .001f) {
                            FoxDashboard(
                                progress = dashboardProgress,
                                onClose = { dashboardOpen = false },
                                onContinueMac = { foxLineState.triggerContinue() },
                                onRerunTestsMac = { foxLineState.triggerContinue() },
                                onSetExamAlarm = {
                                    runCatching {
                                        startActivity(Intent(AlarmClock.ACTION_SET_ALARM).apply {
                                            putExtra(AlarmClock.EXTRA_HOUR, 7)
                                            putExtra(AlarmClock.EXTRA_MINUTES, 0)
                                            putExtra(AlarmClock.EXTRA_MESSAGE, "Exam tomorrow 9:30")
                                        })
                                    }
                                },
                                onDismissSuggestedFix = {},
                                onApplySuggestedFix = { foxLineState.triggerContinue() },
                                onOpenSpace = { space -> android.widget.Toast.makeText(this@MainActivity, "Opening $space", android.widget.Toast.LENGTH_SHORT).show() }
                            )
                        }
                    },
                    onClockLongPress = { foxLineState.debugMenuOpen = true }
                ) { wpBox ->
                    Phase4HomeContent(
                        box = wpBox,
                        foxLineState = foxLineState,
                        modifier = Modifier.graphicsLayer {
                            val progress = dashboardProgress
                            translationY = size.height * .546f * progress
                            alpha = (1f - progress * 1.6f).coerceAtLeast(0f)
                        }
                    )
                    TransferRippleCanvas(state = foxLineState)
                    FoxWakeOverlay(
                        state = foxLineState,
                        onContinueMac = {},
                        onSetAlarm = {
                            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                                putExtra(AlarmClock.EXTRA_HOUR, 7)
                                putExtra(AlarmClock.EXTRA_MINUTES, 0)
                                putExtra(AlarmClock.EXTRA_MESSAGE, "Exam tomorrow 9:30")
                                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            runCatching { startActivity(intent) }
                        }
                    )
                    FoxLineTouchStrip(
                        state = foxLineState,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = foxBottomClearance)
                    )
                    LiveActivityIslandCard(state = foxLineState)
                    FoxLineDebugMenu(state = foxLineState)
                }
                SpotlightSheet(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    isExpanded = drawerExpanded,
                    onToggleExpand = { drawerExpanded = !drawerExpanded },
                    bottomSafeInset = foxBottomClearance,
                    onContactAction = { contact, action ->
                        val intent = if (action == "call") {
                            Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phone}"))
                        } else {
                            Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${contact.phone}"))
                        }
                        runCatching { startActivity(intent) }
                    },
                    onAction = { action ->
                        when (action) {
                            "Continue on Mac" -> foxLineState.triggerContinue()
                            "Send a file" -> runCatching {
                                startActivity(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                    addCategory(Intent.CATEGORY_OPENABLE)
                                    type = "*/*"
                                })
                            }
                            "Timer" -> foxLineState.startTimer(60f)
                            "Music" -> foxLineState.startMusic()
                            "Order food" -> foxLineState.startOrder()
                            "Set 7:00 alarm" -> runCatching {
                                startActivity(Intent(AlarmClock.ACTION_SET_ALARM).apply {
                                    putExtra(AlarmClock.EXTRA_HOUR, 7)
                                    putExtra(AlarmClock.EXTRA_MINUTES, 0)
                                    putExtra(AlarmClock.EXTRA_MESSAGE, "Exam tomorrow 9:30")
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                })
                            }
                        }
                    }
                )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        requestHighRefreshRate()
        hideNavigationBar()
        ViewCompat.requestApplyInsets(window.decorView)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideNavigationBar()
        }
    }

    private fun hideNavigationBar() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.navigationBars())
        }
    }

    private fun requestHighRefreshRate() {
        val currentDisplay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay
        } ?: return

        val modes = currentDisplay.supportedModes ?: return
        if (modes.isEmpty()) return

        val activeMode = currentDisplay.mode
        val bestMode = modes
            .filter { it.physicalWidth == activeMode.physicalWidth && it.physicalHeight == activeMode.physicalHeight }
            .maxByOrNull { it.refreshRate }
            ?: modes.maxByOrNull { it.refreshRate }
            ?: return

        window.attributes = window.attributes.apply {
            preferredDisplayModeId = bestMode.modeId
            @Suppress("DEPRECATION")
            preferredRefreshRate = bestMode.refreshRate
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.decorView.post {
                window.attributes = window.attributes.apply {
                    preferredDisplayModeId = bestMode.modeId
                    @Suppress("DEPRECATION")
                    preferredRefreshRate = bestMode.refreshRate
                }
            }
        }
    }
}
