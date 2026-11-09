package com.fox.mobile.ui.island

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fox.mobile.protocol.FoxStateSpec
import com.fox.mobile.ui.theme.FoxColors

/**
 * Renders the bottom-anchored `.island` card from `Fox_Line_v30.html` with
 * the `liquidMerge` spring expansion and 53-state diagnostic/action buttons.
 */
@Composable
fun IslandCard(
    visible: Boolean,
    stateSpec: FoxStateSpec?,
    onActionClick: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mergeScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.88f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "liquidMergeScale"
    )

    AnimatedVisibility(
        visible = visible && stateSpec != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        if (stateSpec != null) {
            val dotColor = when (stateSpec.color) {
                "#ff4d4d" -> FoxColors.AlertRed
                "#7fd6a4" -> FoxColors.OkMint
                "#f2b33d" -> FoxColors.Ember
                else -> FoxColors.InkMuted
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .graphicsLayer {
                        scaleX = mergeScale
                        scaleY = mergeScale
                    }
                    .clip(RoundedCornerShape(26.dp))
                    .background(FoxColors.BgCard)
                    .border(1.dp, Color(0x1FF3EFE6), RoundedCornerShape(26.dp))
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
                            color = FoxColors.InkMuted,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.1.sp
                        )
                    }
                    Text(
                        text = stateSpec.sub,
                        color = FoxColors.InkDim,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = stateSpec.title,
                    color = FoxColors.InkPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stateSpec.body,
                    color = FoxColors.InkMuted,
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
                            color = FoxColors.Ember,
                            fontSize = 20.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 3.sp
                        )
                    }
                }

                if (stateSpec.log.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x66000000))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        stateSpec.log.forEach { entry ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${entry.time}  ${entry.event}",
                                    color = FoxColors.InkMuted,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = entry.status,
                                    color = FoxColors.Ember,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
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
                                    color = if (isPrimary) FoxColors.Ember else FoxColors.InkPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
