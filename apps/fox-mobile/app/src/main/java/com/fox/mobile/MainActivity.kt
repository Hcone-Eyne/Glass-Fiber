package com.fox.mobile

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.AlarmClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.fox.mobile.ui.depth.DepthWallpaperAndClock
import com.fox.mobile.ui.home.Phase4HomeContent
import com.fox.mobile.ui.island.LiveActivityIslandCard
import com.fox.mobile.ui.line.FoxLineDebugMenu
import com.fox.mobile.ui.line.FoxLineState
import com.fox.mobile.ui.line.FoxLineTouchStrip
import com.fox.mobile.ui.line.FoxWakeOverlay
import com.fox.mobile.ui.line.TransferRippleCanvas
import com.fox.mobile.ui.theme.FoxTheme

class MainActivity : ComponentActivity() {
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

        setContent {
            FoxTheme {
                val foxLineState = remember { FoxLineState() }
                DepthWallpaperAndClock(
                    onClockLongPress = { foxLineState.debugMenuOpen = true }
                ) { wpBox ->
                    Phase4HomeContent(
                        box = wpBox,
                        foxLineState = foxLineState
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
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                    LiveActivityIslandCard(state = foxLineState)
                    FoxLineDebugMenu(state = foxLineState)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        requestHighRefreshRate()
        hideNavigationBar()
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
