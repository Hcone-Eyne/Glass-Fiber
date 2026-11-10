package com.fox.mobile.ui.line

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fox.mobile.ui.theme.FoxColors
import com.fox.mobile.ui.theme.FoxFonts

/**
 * Temporary Clock Debug Menu for Phase 5 (`docs/FOX_MOBILE_PLAN.md` line 245).
 * Triggered by long-pressing the clock widget on the home screen.
 * Provides instant triggers for every Fox Line live activity, ripple, splash wave, and idle state.
 */
@Composable
fun FoxLineDebugMenu(
    state: FoxLineState,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = state.debugMenuOpen,
        enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                scaleIn(
                    initialScale = 0.92f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
                ),
        exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                scaleOut(
                    targetScale = 0.92f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                ),
        modifier = modifier
    ) {
        val view = LocalView.current

        // Scrim backdrop
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x8007080C))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    FoxLineState.performFoxBuzz(view, 6L)
                    state.debugMenuOpen = false
                },
            contentAlignment = Alignment.Center
        ) {
            // Debug card
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(24.dp))
                    .shadow(24.dp, RoundedCornerShape(24.dp))
                    .background(FoxColors.inkMenu)
                    .border(1.dp, FoxColors.hair, RoundedCornerShape(24.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* Consume clicks inside menu */ }
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Fox Line Debug",
                            fontFamily = FoxFonts.Figtree,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = FoxColors.t1
                        )
                        Text(
                            text = "Phase 5 Bar State Testing",
                            fontFamily = FoxFonts.Figtree,
                            fontSize = 12.sp,
                            color = FoxColors.t2
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x24F2B33D))
                            .border(1.dp, Color(0x59F2B33D), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "CLOCK HOOK",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp,
                            letterSpacing = 1.sp,
                            color = FoxColors.ember
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Grid (2-column rows)
                DebugActionRow(
                    leftText = "Wake Fox",
                    leftAccent = true,
                    onLeftClick = {
                        FoxLineState.performFoxBuzz(view, 8L)
                        state.summonFox()
                        state.debugMenuOpen = false
                    },
                    rightText = "Splash Wave",
                    rightAccent = false,
                    onRightClick = {
                        FoxLineState.performFoxBuzz(view, 6L)
                        state.splashBar(1f)
                        state.debugMenuOpen = false
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                DebugActionRow(
                    leftText = "Timer (20s)",
                    leftAccent = false,
                    onLeftClick = {
                        FoxLineState.performFoxBuzz(view, 6L)
                        state.startTimer(20f)
                        state.debugMenuOpen = false
                    },
                    rightText = "Timer (Red/Done)",
                    rightAccent = false,
                    onRightClick = {
                        FoxLineState.performFoxBuzz(view, 6L)
                        state.startTimer(20f, markDoneImmediately = true)
                        state.debugMenuOpen = false
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                DebugActionRow(
                    leftText = "Music (Wave)",
                    leftAccent = false,
                    onLeftClick = {
                        FoxLineState.performFoxBuzz(view, 6L)
                        state.startMusic()
                        state.debugMenuOpen = false
                    },
                    rightText = "Food Order",
                    rightAccent = false,
                    onRightClick = {
                        FoxLineState.performFoxBuzz(view, 6L)
                        state.startOrder()
                        state.debugMenuOpen = false
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                DebugActionRow(
                    leftText = "Message (Drop)",
                    leftAccent = false,
                    onLeftClick = {
                        FoxLineState.performFoxBuzz(view, 6L)
                        state.triggerMessage()
                        state.debugMenuOpen = false
                    },
                    rightText = "Send File (In)",
                    rightAccent = false,
                    onRightClick = {
                        FoxLineState.performFoxBuzz(view, 6L)
                        state.triggerTransfer("out", 0.6f)
                        state.debugMenuOpen = false
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                DebugActionRow(
                    leftText = "Receive File (Out)",
                    leftAccent = false,
                    onLeftClick = {
                        FoxLineState.performFoxBuzz(view, 6L)
                        state.triggerTransfer("in", 0.6f)
                        state.debugMenuOpen = false
                    },
                    rightText = "Stop All (Idle)",
                    rightAccent = false,
                    onRightClick = {
                        FoxLineState.performFoxBuzz(view, 6L)
                        state.clearAll()
                        state.debugMenuOpen = false
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Close Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x1AFFFFFF))
                        .border(1.dp, FoxColors.hair, RoundedCornerShape(14.dp))
                        .clickable {
                            FoxLineState.performFoxBuzz(view, 6L)
                            state.debugMenuOpen = false
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Close Menu",
                        fontFamily = FoxFonts.Figtree,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = FoxColors.t2
                    )
                }
            }
        }
    }
}

@Composable
private fun DebugActionRow(
    leftText: String,
    leftAccent: Boolean,
    onLeftClick: () -> Unit,
    rightText: String,
    rightAccent: Boolean,
    onRightClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DebugActionButton(
            text = leftText,
            isAccent = leftAccent,
            onClick = onLeftClick,
            modifier = Modifier.weight(1f)
        )
        DebugActionButton(
            text = rightText,
            isAccent = rightAccent,
            onClick = onRightClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DebugActionButton(
    text: String,
    isAccent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isAccent) Color(0x2BF2B33D) else Color(0x14FFFFFF)
    val borderCol = if (isAccent) Color(0x66F2B33D) else FoxColors.hair
    val textCol = if (isAccent) FoxColors.ember else FoxColors.t1

    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, borderCol, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontFamily = FoxFonts.Figtree,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.5.sp,
            textAlign = TextAlign.Center,
            color = textCol,
            maxLines = 1
        )
    }
}
