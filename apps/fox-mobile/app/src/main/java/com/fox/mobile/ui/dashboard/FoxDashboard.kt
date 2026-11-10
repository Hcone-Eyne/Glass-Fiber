package com.fox.mobile.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.withTimeoutOrNull
import com.fox.mobile.ui.theme.FoxColors
import com.fox.mobile.ui.theme.FoxFonts

/**
 * Scrollable v40 dashboard surface. The host controls presentation/visibility and supplies
 * progress (0..1) for its entrance/exit. Mac actions are demo callbacks; this composable
 * performs no networking or remote execution itself.
 */
@Composable
fun FoxDashboard(
    progress: Float,
    onClose: () -> Unit,
    onContinueMac: () -> Unit,
    onRerunTestsMac: () -> Unit,
    onSetExamAlarm: () -> Unit,
    onDismissSuggestedFix: () -> Unit,
    onApplySuggestedFix: () -> Unit,
    onOpenSpace: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var diffExpanded by remember { mutableStateOf(false) }
    var testsRunning by remember { mutableStateOf(false) }
    var fixDismissed by remember { mutableStateOf(false) }
    var fixApplied by remember { mutableStateOf(false) }
    var continueDetails by remember { mutableStateOf(false) }
    val alpha = progress.coerceIn(0f, 1f)

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha }.clickable { if (alpha > .5f) onClose() })
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp)
                // Fit into the opening below the risen clock, like v40's fitDash().
                .padding(top = maxHeight * .16f, bottom = maxHeight * .17f)
                .graphicsLayer { this.alpha = alpha },
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(Modifier.padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(FoxColors.ok))
                    Spacer(Modifier.width(8.dp))
                    Text("MacBook Air", color = FoxColors.t1, fontFamily = FoxFonts.Figtree,
                        fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("Live · 38 ms", color = FoxColors.t2, fontFamily = FoxFonts.Figtree, fontSize = 12.sp)
                }
            }
            item {
                DashboardCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("MenuBarView.swift", color = FoxColors.t1, fontFamily = FoxFonts.Bricolage,
                            fontWeight = FontWeight.SemiBold, fontSize = 22.sp)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Text("glass_horizon · feat/menubar-view", color = FoxColors.t2,
                            fontFamily = FoxFonts.JetBrainsMono, fontSize = 11.sp)
                        Text("Edited on your Mac 4 min ago · open in Xcode, Terminal, Safari",
                            color = FoxColors.t2, fontFamily = FoxFonts.Figtree, fontSize = 13.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            ActionButton("Continue on Mac", onContinueMac, Modifier.weight(1f))
                            TextButtonLabel(if (continueDetails) "Less" else "Details", onClick = { continueDetails = !continueDetails })
                        }
                        if (continueDetails) Text("swift build · 0 errors, 2 warnings\nAlso open: DeviceCellEngine.swift, LazyBonjour.swift",
                            color = FoxColors.t2, fontFamily = FoxFonts.JetBrainsMono, fontSize = 11.sp)
                    }
                }
            }
            item { SectionHeading("Needs you") }
            item {
                DashboardCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Eyebrow("TESTS FAILED", FoxColors.alert)
                            Spacer(Modifier.weight(1f))
                            Text("Mac · 2 min ago", color = FoxColors.t2, fontSize = 11.sp)
                        }
                        Text("pytest: 2 failed, 238 passed", color = FoxColors.t1,
                            fontFamily = FoxFonts.Figtree, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Text("test/test_runtime.py::test_refine_loop", color = FoxColors.t2,
                            fontFamily = FoxFonts.JetBrainsMono, fontSize = 10.sp)
                        var holding by remember { mutableStateOf(false) }
                        val holdProgress by animateFloatAsState(
                            targetValue = if (holding) 1f else 0f,
                            animationSpec = tween(850), label = "rerunHold"
                        )
                        Box(
                            Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(23.dp))
                                .background(Color(0x22F6F0E6)).border(1.dp, FoxColors.hair, RoundedCornerShape(23.dp))
                                .pointerInput(testsRunning) {
                                    detectTapGestures(onPress = {
                                        if (!testsRunning) {
                                            holding = true
                                            val heldLongEnough = withTimeoutOrNull(850L) { tryAwaitRelease(); false } ?: true
                                            if (heldLongEnough) {
                                                testsRunning = true
                                                onRerunTestsMac()
                                            }
                                            holding = false
                                        }
                                    })
                                }, contentAlignment = Alignment.Center
                        ) {
                            Box(Modifier.fillMaxWidth(holdProgress).height(46.dp).align(Alignment.CenterStart)
                                .background(FoxColors.ember.copy(alpha = 0.28f)))
                            Text(if (testsRunning) "Running tests on Mac…" else "Hold to re-run on Mac",
                                color = FoxColors.t1, fontFamily = FoxFonts.Figtree,
                                fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                    }
                }
            }
            item {
                DashboardCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Eyebrow("TOMORROW · FOX NOTICED", FoxColors.ember)
                        Text("Exam at 9:30 and no alarm set", color = FoxColors.t1,
                            fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Text("Battery is 76%. You usually sleep after 1 AM before exams.",
                            color = FoxColors.t2, fontSize = 13.sp)
                        ActionButton("Set 7:00 alarm", onSetExamAlarm)
                    }
                }
            }
            if (!fixDismissed) item {
                DashboardCard {
                    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Eyebrow("FIX READY · FOX DESK", FoxColors.ember)
                            Spacer(Modifier.weight(1f))
                            TextButtonLabel(if (fixApplied) "Applied" else "Dismiss", onClick = {
                                if (!fixApplied) { fixDismissed = true; onDismissSuggestedFix() }
                            })
                        }
                        Text(if (fixApplied) "Fix applied in worktree" else "Suggested fix for openai_provider.py", color = FoxColors.t1,
                            fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text("“choice” → “choices” · 1 line", color = FoxColors.t2, fontSize = 13.sp)
                        Text("Fox applies this in a separate worktree, so your branch is untouched.",
                            color = FoxColors.t2, fontSize = 12.sp)
                        if (diffExpanded) {
                            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .background(Color(0xAA08090D)).padding(11.dp)) {
                                Text("@@ -41,7 +41,7 @@ def complete(self, prompt):", color = FoxColors.t2,
                                    fontFamily = FoxFonts.JetBrainsMono, fontSize = 10.sp)
                                Text("- return resp.json()[\"choice\"][0][\"message\"][\"content\"]",
                                    color = FoxColors.alert, fontFamily = FoxFonts.JetBrainsMono, fontSize = 10.sp)
                                Text("+ return resp.json()[\"choices\"][0][\"message\"][\"content\"]",
                                    color = FoxColors.ok, fontFamily = FoxFonts.JetBrainsMono, fontSize = 10.sp)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                TextButtonLabel("Hide diff", onClick = { diffExpanded = false })
                                ActionButton(if (fixApplied) "Applied" else "Apply in worktree", {
                                    if (!fixApplied) { fixApplied = true; onApplySuggestedFix() }
                                }, Modifier.weight(1f))
                            }
                        } else {
                            TextButtonLabel(if (fixApplied) "View diff" else "Review diff", onClick = { diffExpanded = true })
                        }
                    }
                }
            }
            item { SectionHeading("Spaces") }
            items(
                listOf(
                    DashboardSpace("Glass Horizon", "Menu bar app and Android link", "3 tasks", FoxColors.ember),
                    DashboardSpace("College", "Exam tomorrow at 9:30", "2 due", Color(0xFFF08A24)),
                    DashboardSpace("Mates-Helper", "Hack Club · 3am · 19 to-dos left", "19 left", Color(0xFF79C8D2)),
                    DashboardSpace("Life", "₹450 spent this week", "2 notes", Color(0xFFB6A0E8))
                )
            ) { space ->
                DashboardCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().pointerInput(space.name) {
                            detectTapGestures(onTap = { onOpenSpace(space.name) })
                        },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(38.dp).clip(RoundedCornerShape(13.dp)).background(space.accent.copy(alpha = .18f)),
                            contentAlignment = Alignment.Center) {
                            Text(space.name.take(1), color = space.accent, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(space.name, color = FoxColors.t1, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Text(space.subtitle, color = FoxColors.t2, fontSize = 12.sp,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Spacer(Modifier.width(7.dp))
                        Text(space.badge, color = space.accent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

private data class DashboardSpace(val name: String, val subtitle: String, val badge: String, val accent: Color)

@Composable
private fun DashboardCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(FoxColors.inkSurface)
            .border(1.dp, FoxColors.hair, RoundedCornerShape(22.dp)).padding(16.dp),
        content = { content() }
    )
}

@Composable
private fun Eyebrow(text: String, color: Color = FoxColors.t2) {
    Text(text, color = color, fontFamily = FoxFonts.Figtree, fontWeight = FontWeight.Bold,
        fontSize = 10.sp, letterSpacing = 1.1.sp)
}

@Composable
private fun SectionHeading(text: String) {
    Text(text, color = FoxColors.t1, fontFamily = FoxFonts.Bricolage,
        fontWeight = FontWeight.SemiBold, fontSize = 21.sp, modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun ActionButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(22.dp),
        colors = ButtonDefaults.buttonColors(containerColor = FoxColors.ember, contentColor = Color(0xFF17130B)),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
    ) { Text(text, fontFamily = FoxFonts.Figtree, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
        maxLines = 1, overflow = TextOverflow.Ellipsis) }
}

@Composable
private fun TextButtonLabel(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        modifier = Modifier.clip(RoundedCornerShape(22.dp)).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 12.dp),
        color = FoxColors.t1,
        fontFamily = FoxFonts.Figtree,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp
    )
}
