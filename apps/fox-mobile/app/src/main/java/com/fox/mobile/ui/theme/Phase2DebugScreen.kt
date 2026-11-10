package com.fox.mobile.ui.theme

import android.graphics.BitmapFactory
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@RequiresApi(Build.VERSION_CODES.S)
private fun createSoftBlurEffect(radius: Float) =
    android.graphics.RenderEffect.createBlurEffect(
        radius,
        radius,
        android.graphics.Shader.TileMode.CLAMP
    ).asComposeRenderEffect()

/**
 * Phase 2 Temporary Debug Screen.
 * Demonstrates:
 * 1. Bricolage Grotesque (Display face): Clock (118sp Bold, -0.055em letterSpacing), Words (16sp Medium), Home sentence (23sp).
 * 2. Figtree (Body face): Date label (14sp SemiBold), Body text (14sp), Buttons (14sp SemiBold), Search text.
 * 3. JetBrains Mono (Code face): Branch/path meta (12sp Medium).
 * 4. Ink Surface Material: FoxInkBox (radius 24dp, inkSurface 60% fill, 1px hair border) with 4px backdrop blur on API 31+.
 * 5. Ink Search Capsule: 44dp tall, radius 22dp, inkSurface 60% + 1px hair + 4px blur backdrop.
 * 6. Fox Line bar at bottom: 118dp wide, 5dp thick, lineWarm at 85%, 12dp above bottom edge.
 */
@Composable
fun Phase2DebugScreen() {
    val context = LocalContext.current

    // Load wallpaper.webp from assets using BitmapFactory.decodeStream inside remember
    val wallpaperBitmap = remember {
        try {
            context.assets.open("wallpaper.webp").use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Throwable) {
            null
        }
    }
    val imageBitmap = remember(wallpaperBitmap) { wallpaperBitmap?.asImageBitmap() }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(FoxColors.screenBg)
    ) {
        val density = LocalDensity.current
        val screenH = with(density) { maxHeight.toPx() }
        val screenW = with(density) { maxWidth.toPx() }

        // Prototype .wp sizing:
        // ih = 1.1 * H, iw = ih * 1136 / 615, left = -(420 / 1136) * iw, top = -0.05 * H
        val ih = screenH * 1.1f
        val iw = ih * 1136f / 615f
        val left = -(420f / 1136f) * iw
        val top = -screenH * 0.05f

        // 1. Draw full-bleed wallpaper on FoxColors.screenBg (#07080C)
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (imageBitmap != null) {
                drawImage(
                    image = imageBitmap,
                    dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                    dstSize = IntSize(iw.roundToInt(), ih.roundToInt())
                )
            }
        }

        // 2. Foreground content column (scrollable so all elements remain accessible)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 22.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Date label in Figtree 14sp SemiBold FoxColors.t2
            Text(
                text = "Mon, Nov 9",
                style = TextStyle(
                    fontFamily = FoxFonts.Figtree,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = FoxColors.t2
                )
            )

            // Clock in Bricolage 118sp Bold FoxColors.t1 with letterSpacing -0.055.em
            Text(
                text = "09:41",
                style = TextStyle(
                    fontFamily = FoxFonts.Bricolage,
                    fontWeight = FontWeight.Bold,
                    fontSize = 118.sp,
                    letterSpacing = (-0.055).em,
                    lineHeight = (118 * 0.86).sp,
                    color = FoxColors.t1
                )
            )

            // Words line in Bricolage 16sp Medium FoxColors.t1
            Text(
                text = "\"Stay hungry. Stay foolish.\"",
                style = TextStyle(
                    fontFamily = FoxFonts.Bricolage,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    color = FoxColors.t1
                )
            )

            // Home sentence in Bricolage 23sp with MenuBarView.swift bold in FoxColors.t1
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            fontFamily = FoxFonts.Bricolage,
                            fontWeight = FontWeight.Normal,
                            fontSize = 23.sp,
                            color = FoxColors.t2
                        )
                    ) {
                        append("You were writing ")
                    }
                    withStyle(
                        SpanStyle(
                            fontFamily = FoxFonts.Bricolage,
                            fontWeight = FontWeight.Bold,
                            fontSize = 23.sp,
                            color = FoxColors.t1
                        )
                    ) {
                        append("MenuBarView.swift")
                    }
                    withStyle(
                        SpanStyle(
                            fontFamily = FoxFonts.Bricolage,
                            fontWeight = FontWeight.Normal,
                            fontSize = 23.sp,
                            color = FoxColors.t2
                        )
                    ) {
                        append(" on your Mac, 4 min ago.")
                    }
                },
                lineHeight = (23 * 1.25).sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Ink Surface Card (FoxInkBox, radius 24.dp, inkSurface 60% + 1px hair border + soft 4px backdrop blur)
            FoxInkBox(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 24.dp,
                contentPadding = PaddingValues(0.dp)
            ) {
                // Background image layer inside the clipped card with 4px backdrop blur on API 31+
                if (imageBitmap != null) {
                    var cardOffset by remember { mutableStateOf(Offset.Zero) }
                    Canvas(
                        modifier = Modifier
                            .matchParentSize()
                            .onGloballyPositioned { coordinates ->
                                cardOffset = coordinates.positionInRoot()
                            }
                            .then(
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    Modifier.graphicsLayer {
                                        renderEffect = createSoftBlurEffect(4f)
                                    }
                                } else Modifier
                            )
                    ) {
                        val relX = (left - cardOffset.x).roundToInt()
                        val relY = (top - cardOffset.y).roundToInt()
                        drawImage(
                            image = imageBitmap,
                            dstOffset = IntOffset(relX, relY),
                            dstSize = IntSize(iw.roundToInt(), ih.roundToInt())
                        )
                    }
                    // Overlay the 60% inkSurface fill so the card maintains its ink material depth
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(FoxColors.inkSurface)
                    )
                }

                // Foreground content inside the card stays razor-sharp
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Card Title (24sp Bricolage SemiBold FoxColors.t1)
                    Text(
                        text = "MenuBarView.swift",
                        style = TextStyle(
                            fontFamily = FoxFonts.Bricolage,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 24.sp,
                            letterSpacing = (-0.02).em,
                            color = FoxColors.t1
                        )
                    )

                    // JetBrains Mono (Code face, 12sp Medium FoxColors.t2)
                    Text(
                        text = "glass_horizon · feat/menubar-view",
                        style = TextStyle(
                            fontFamily = FoxFonts.JetBrainsMono,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = FoxColors.t2
                        )
                    )

                    // Figtree (Body face, 14sp FoxColors.t2)
                    Text(
                        text = "Edited on your Mac 4 min ago · open in Xcode, Terminal, Safari",
                        style = TextStyle(
                            fontFamily = FoxFonts.Figtree,
                            fontWeight = FontWeight.Normal,
                            fontSize = 14.sp,
                            color = FoxColors.t2
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Action row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Ember pill button (FoxColors.ember, height 44.dp, radius 22.dp, Figtree 14sp SemiBold #17130B)
                        Box(
                            modifier = Modifier
                                .height(44.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(FoxColors.ember)
                                .padding(horizontal = 18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Continue on Mac",
                                style = TextStyle(
                                    fontFamily = FoxFonts.Figtree,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF17130B)
                                )
                            )
                        }

                        // Ghost pill button (Color(0x14FFFFFF), height 44.dp, radius 22.dp, Figtree 14sp SemiBold FoxColors.t1)
                        Box(
                            modifier = Modifier
                                .height(44.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(Color(0x14FFFFFF))
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Details",
                                style = TextStyle(
                                    fontFamily = FoxFonts.Figtree,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = FoxColors.t1
                                )
                            )
                        }
                    }
                }
            }

            // Ink Search Capsule (44.dp tall, fully round 22.dp radius, inkSurface 60% + 1px hair border + 4px blur backdrop)
            FoxInkBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                cornerRadius = 22.dp,
                contentPadding = PaddingValues(0.dp)
            ) {
                if (imageBitmap != null) {
                    var capsuleOffset by remember { mutableStateOf(Offset.Zero) }
                    Canvas(
                        modifier = Modifier
                            .matchParentSize()
                            .onGloballyPositioned { coordinates ->
                                capsuleOffset = coordinates.positionInRoot()
                            }
                            .then(
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    Modifier.graphicsLayer {
                                        renderEffect = createSoftBlurEffect(4f)
                                    }
                                } else Modifier
                            )
                    ) {
                        val relX = (left - capsuleOffset.x).roundToInt()
                        val relY = (top - capsuleOffset.y).roundToInt()
                        drawImage(
                            image = imageBitmap,
                            dstOffset = IntOffset(relX, relY),
                            dstSize = IntSize(iw.roundToInt(), ih.roundToInt())
                        )
                    }
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(FoxColors.inkSurface)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Search icon outline
                    Canvas(modifier = Modifier.size(15.dp)) {
                        val strokeW = 1.8.dp.toPx()
                        val r = size.minDimension * 0.32f
                        val centerPt = Offset(size.width * 0.4f, size.height * 0.4f)
                        drawCircle(
                            color = FoxColors.t2,
                            radius = r,
                            center = centerPt,
                            style = Stroke(width = strokeW)
                        )
                        val angle = Math.toRadians(45.0)
                        val startX = (centerPt.x + r * Math.cos(angle)).toFloat()
                        val startY = (centerPt.y + r * Math.sin(angle)).toFloat()
                        drawLine(
                            color = FoxColors.t2,
                            start = Offset(startX, startY),
                            end = Offset(size.width * 0.88f, size.height * 0.88f),
                            strokeWidth = strokeW,
                            cap = StrokeCap.Round
                        )
                    }

                    // Figtree 14sp FoxColors.t2 "Search apps and files"
                    Text(
                        text = "Search apps and files",
                        style = TextStyle(
                            fontFamily = FoxFonts.Figtree,
                            fontWeight = FontWeight.Normal,
                            fontSize = 14.sp,
                            color = FoxColors.t2
                        )
                    )
                }
            }

            // Bottom spacer to ensure scrolling clears the Fox Line bar
            Spacer(modifier = Modifier.height(48.dp))
        }

        // Fox Line bar at bottom (118.dp wide, 5.dp thick, round caps, FoxColors.lineWarm #F6F0E6 at 85%, centered 12.dp above bottom edge)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 12.dp)
                .width(118.dp)
                .height(5.dp)
                .background(color = FoxColors.lineWarm, shape = RoundedCornerShape(2.5.dp))
        )
    }
}
