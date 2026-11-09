package com.fox.mobile.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fox.mobile.protocol.FoxStateSpec
import com.fox.mobile.protocol.FoxTask
import com.fox.mobile.ui.drawer.SpotlightSheet
import com.fox.mobile.ui.island.IslandCard
import com.fox.mobile.ui.line.FoxLineBar
import com.fox.mobile.ui.line.FoxLineMode
import com.fox.mobile.ui.line.SecondaryBead
import com.fox.mobile.ui.theme.FoxColors

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
