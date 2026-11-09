package com.fox.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.fox.mobile.protocol.FoxStateSpec
import com.fox.mobile.protocol.FoxTask
import com.fox.mobile.ui.home.WorkHomeScreen
import com.fox.mobile.ui.theme.FoxTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val defaultTask = FoxTask(
            id = "task_fox_desk_01",
            title = "MenuBarView.swift",
            source = "git",
            status = "active",
            ts = 1762668001000L,
            project = "FoxDesk",
            branch = "main",
            primaryFile = "MenuBarView.swift",
            summary = "FoxDesk · main · 2 uncommitted",
            pinned = true
        )

        val defaultStates = mapOf(
            "ok" to FoxStateSpec(
                key = "ok",
                group = "Connection",
                color = "#7fd6a4",
                tag = "Connected · LAN",
                sub = "3ms · TLS 1.3",
                title = "Enoch's Mac",
                body = "Same Wi-Fi. Handoff, clipboard, and files are live.",
                actions = listOf("Send clipboard", "Disconnect")
            ),
            "reconnecting" to FoxStateSpec(
                key = "reconnecting",
                group = "Connection",
                color = "#f2b33d",
                tag = "Reconnecting",
                sub = "try 3 of 5 · 4s",
                title = "Looking for Enoch's Mac...",
                body = "Wi-Fi dropped briefly. Queued items will send when it's back.",
                actions = listOf("Try now", "Cancel")
            ),
            "offline" to FoxStateSpec(
                key = "offline",
                group = "Connection",
                color = "#ff4d4d",
                tag = "Mac unreachable",
                sub = "last seen 14m ago",
                title = "Can't reach Enoch's Mac",
                body = "Mac may be asleep or on another network. Nothing is lost.",
                actions = listOf("Queue for later", "Retry")
            ),
            "pair_code" to FoxStateSpec(
                key = "pair_code",
                group = "Pairing and security",
                color = "#f2b33d",
                tag = "Pairing · step 1 of 2",
                sub = "expires 00:48",
                title = "Match this code on your Mac",
                body = "Confirm both screens show the same 6-digit code before approving.",
                code = "482 · 917",
                actions = listOf("They match", "Cancel")
            ),
            "redaction" to FoxStateSpec(
                key = "redaction",
                group = "Caution",
                color = "#f2b33d",
                tag = "Redacted",
                sub = "1 secret masked",
                title = "Secret masked before send",
                body = "High-entropy token was replaced with [REDACTED] by FoxScrubber.",
                actions = listOf("OK")
            ),
            "allowlist" to FoxStateSpec(
                key = "allowlist",
                group = "Caution",
                color = "#8a8680",
                tag = "Allowlist",
                sub = "silent",
                title = "Outside allowlisted folders",
                body = "Current folder is not in your FoxDesk allowlist. Nothing is shared.",
                actions = listOf("OK")
            ),
            "doze" to FoxStateSpec(
                key = "doze",
                group = "Phone-side",
                color = "#f2b33d",
                tag = "Battery optimization",
                sub = "Android Doze",
                title = "Background sync throttled",
                body = "Disable battery optimization for Fox so Mac handoff stays instant.",
                actions = listOf("Allow unrestricted", "Later")
            ),
            "clock" to FoxStateSpec(
                key = "clock",
                group = "Caution",
                color = "#ff4d4d",
                tag = "Clock skew",
                sub = "> 60s drift",
                title = "Clocks are out of sync",
                body = "Phone and Mac clocks differ by more than 60 seconds. Replay guard paused.",
                actions = listOf("Sync time", "Dismiss")
            )
        )

        setContent {
            FoxTheme {
                WorkHomeScreen(
                    currentTask = defaultTask,
                    statesByKey = defaultStates
                )
            }
        }
    }
}
