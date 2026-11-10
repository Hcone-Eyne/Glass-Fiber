package com.fox.mobile

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.fox.mobile.ui.depth.DepthWallpaperAndClock
import com.fox.mobile.ui.home.Phase4HomeContent
import com.fox.mobile.ui.theme.FoxTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestHighRefreshRate()

        // Configure edge-to-edge drawing
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        // Keep light status-bar icons visible
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        setContent {
            FoxTheme {
                DepthWallpaperAndClock { wpBox ->
                    Phase4HomeContent(box = wpBox)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        requestHighRefreshRate()
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
