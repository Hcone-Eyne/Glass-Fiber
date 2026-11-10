package com.fox.mobile.ui.depth

import android.content.Context
import android.graphics.BitmapFactory
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.provider.Settings
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.fox.mobile.ui.theme.FoxColors
import com.fox.mobile.ui.theme.FoxFonts
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Depth Wallpaper & Clock (Fox Mobile Phase 3)
 *
 * Exact specifications extracted from:
 * - docs/prototypes/Fox_Line_v30.html (lines 111-120, 518-530, 1647-1676)
 * - docs/prototypes/assets/ridge.json (191 points)
 * - docs/FOX_MOBILE_PLAN.md (Phase 3)
 *
 * 1. Geometry:
 *    ih = 1.1 * H
 *    iw = ih * 1136 / 615
 *    left = -(420 / 1136) * iw
 *    top = -0.05 * H
 *
 * 2. Three layers (back to front):
 *    - Back: Full night painting (wallpaper.webp)
 *    - Clock: Left 22dp, Top 24.5% of H. Date (Figtree 14sp 600 t2) above Time (Bricolage 118sp 700 line-height 0.86 letter-spacing -0.055em t1)
 *    - Front: Same painting clipped below the ridge (ridge.json 191 points closed along the bottom)
 *
 * 3. Parallax Tilt (low-pass 0.08 per frame):
 *    - Back:  (tiltX * 5,  tiltY * 4) px
 *    - Clock: (tiltX * 10, tiltY * 8) px
 *    - Front: (tiltX * 18, tiltY * 13) px
 *
 * 4. Dark Fade:
 *    Bottom 130dp: Transparent -> #050609 at 55% -> #050609 at 85% at bottom.
 */

data class WallpaperBox(
    val iw: Float,
    val ih: Float,
    val left: Float,
    val top: Float
) {
    companion object {
        const val IMAGE_WIDTH = 1136f
        const val IMAGE_HEIGHT = 615f
        const val ASPECT_RATIO = IMAGE_WIDTH / IMAGE_HEIGHT // ~1.8471545
        const val LEFT_RATIO = -(420f / IMAGE_WIDTH)       // ~-0.3697183
        const val HEIGHT_SCALE = 1.10f
        const val TOP_SCALE = -0.05f

        fun calculate(screenW: Float, screenH: Float): WallpaperBox {
            val ih = screenH * HEIGHT_SCALE
            val iw = ih * ASPECT_RATIO
            val left = iw * LEFT_RATIO
            val top = screenH * TOP_SCALE
            return WallpaperBox(iw = iw, ih = ih, left = left, top = top)
        }
    }
}

/**
 * 191 Normalized ridge points [0.0..1.0] from docs/prototypes/assets/ridge.json.
 * Traces the white mountain ridge line dividing the sky (above) from the ground (below).
 */
val RIDGE_NORMALIZED_POINTS = arrayOf(
    Offset(0.0000f, 0.3415f),
    Offset(0.0053f, 0.3415f),
    Offset(0.0106f, 0.3431f),
    Offset(0.0159f, 0.3480f),
    Offset(0.0211f, 0.3545f),
    Offset(0.0264f, 0.3577f),
    Offset(0.0317f, 0.3626f),
    Offset(0.0370f, 0.3675f),
    Offset(0.0423f, 0.3724f),
    Offset(0.0476f, 0.3772f),
    Offset(0.0529f, 0.3805f),
    Offset(0.0581f, 0.3837f),
    Offset(0.0634f, 0.3870f),
    Offset(0.0687f, 0.3886f),
    Offset(0.0740f, 0.3919f),
    Offset(0.0793f, 0.3967f),
    Offset(0.0846f, 0.4016f),
    Offset(0.0899f, 0.4065f),
    Offset(0.0952f, 0.4098f),
    Offset(0.1004f, 0.4163f),
    Offset(0.1057f, 0.4195f),
    Offset(0.1110f, 0.4244f),
    Offset(0.1163f, 0.4260f),
    Offset(0.1216f, 0.4293f),
    Offset(0.1269f, 0.4341f),
    Offset(0.1322f, 0.4390f),
    Offset(0.1374f, 0.4423f),
    Offset(0.1427f, 0.4423f),
    Offset(0.1480f, 0.4423f),
    Offset(0.1533f, 0.4423f),
    Offset(0.1586f, 0.4423f),
    Offset(0.1639f, 0.4423f),
    Offset(0.1692f, 0.4407f),
    Offset(0.1744f, 0.4390f),
    Offset(0.1797f, 0.4390f),
    Offset(0.1850f, 0.4374f),
    Offset(0.1903f, 0.4390f),
    Offset(0.1956f, 0.4358f),
    Offset(0.2009f, 0.4341f),
    Offset(0.2062f, 0.4325f),
    Offset(0.2115f, 0.4309f),
    Offset(0.2167f, 0.4293f),
    Offset(0.2220f, 0.4276f),
    Offset(0.2273f, 0.4244f),
    Offset(0.2326f, 0.4211f),
    Offset(0.2379f, 0.4195f),
    Offset(0.2432f, 0.4130f),
    Offset(0.2485f, 0.4081f),
    Offset(0.2537f, 0.4033f),
    Offset(0.2590f, 0.3984f),
    Offset(0.2643f, 0.3967f),
    Offset(0.2696f, 0.3935f),
    Offset(0.2749f, 0.3902f),
    Offset(0.2802f, 0.3854f),
    Offset(0.2855f, 0.3821f),
    Offset(0.2907f, 0.3805f),
    Offset(0.2960f, 0.3789f),
    Offset(0.3013f, 0.3756f),
    Offset(0.3066f, 0.3740f),
    Offset(0.3119f, 0.3707f),
    Offset(0.3172f, 0.3675f),
    Offset(0.3225f, 0.3691f),
    Offset(0.3278f, 0.3642f),
    Offset(0.3330f, 0.3626f),
    Offset(0.3383f, 0.3626f),
    Offset(0.3436f, 0.3610f),
    Offset(0.3489f, 0.3626f),
    Offset(0.3542f, 0.3626f),
    Offset(0.3595f, 0.3626f),
    Offset(0.3648f, 0.3626f),
    Offset(0.3700f, 0.3610f),
    Offset(0.3753f, 0.3626f),
    Offset(0.3806f, 0.3610f),
    Offset(0.3859f, 0.3577f),
    Offset(0.3912f, 0.3626f),
    Offset(0.3965f, 0.3610f),
    Offset(0.4018f, 0.3610f),
    Offset(0.4070f, 0.3593f),
    Offset(0.4123f, 0.3626f),
    Offset(0.4176f, 0.3626f),
    Offset(0.4229f, 0.3642f),
    Offset(0.4282f, 0.3675f),
    Offset(0.4335f, 0.3724f),
    Offset(0.4388f, 0.3756f),
    Offset(0.4441f, 0.3756f),
    Offset(0.4493f, 0.3837f),
    Offset(0.4546f, 0.3870f),
    Offset(0.4599f, 0.3886f),
    Offset(0.4652f, 0.3902f),
    Offset(0.4705f, 0.3902f),
    Offset(0.4758f, 0.3919f),
    Offset(0.4811f, 0.3951f),
    Offset(0.4863f, 0.3951f),
    Offset(0.4916f, 0.3984f),
    Offset(0.4969f, 0.4016f),
    Offset(0.5022f, 0.4049f),
    Offset(0.5075f, 0.4098f),
    Offset(0.5128f, 0.4130f),
    Offset(0.5181f, 0.4195f),
    Offset(0.5233f, 0.4228f),
    Offset(0.5286f, 0.4260f),
    Offset(0.5339f, 0.4293f),
    Offset(0.5392f, 0.4325f),
    Offset(0.5445f, 0.4358f),
    Offset(0.5498f, 0.4390f),
    Offset(0.5551f, 0.4439f),
    Offset(0.5604f, 0.4488f),
    Offset(0.5656f, 0.4553f),
    Offset(0.5709f, 0.4602f),
    Offset(0.5762f, 0.4683f),
    Offset(0.5815f, 0.4764f),
    Offset(0.5868f, 0.4813f),
    Offset(0.5921f, 0.4829f),
    Offset(0.5974f, 0.4846f),
    Offset(0.6026f, 0.4846f),
    Offset(0.6079f, 0.4894f),
    Offset(0.6132f, 0.4927f),
    Offset(0.6185f, 0.4984f),
    Offset(0.6238f, 0.5041f),
    Offset(0.6291f, 0.5089f),
    Offset(0.6344f, 0.5122f),
    Offset(0.6396f, 0.5187f),
    Offset(0.6449f, 0.5236f),
    Offset(0.6502f, 0.5301f),
    Offset(0.6555f, 0.5480f),
    Offset(0.6608f, 0.6228f),
    Offset(0.6661f, 0.6293f),
    Offset(0.6714f, 0.6293f),
    Offset(0.6767f, 0.6293f),
    Offset(0.6819f, 0.6293f),
    Offset(0.6872f, 0.6325f),
    Offset(0.6925f, 0.6325f),
    Offset(0.6978f, 0.6325f),
    Offset(0.7031f, 0.6309f),
    Offset(0.7084f, 0.6293f),
    Offset(0.7137f, 0.6293f),
    Offset(0.7189f, 0.6276f),
    Offset(0.7242f, 0.6244f),
    Offset(0.7295f, 0.6211f),
    Offset(0.7348f, 0.6211f),
    Offset(0.7401f, 0.6179f),
    Offset(0.7454f, 0.6130f),
    Offset(0.7507f, 0.6114f),
    Offset(0.7559f, 0.6081f),
    Offset(0.7612f, 0.6081f),
    Offset(0.7665f, 0.6065f),
    Offset(0.7718f, 0.6049f),
    Offset(0.7771f, 0.5984f),
    Offset(0.7824f, 0.5951f),
    Offset(0.7877f, 0.5919f),
    Offset(0.7930f, 0.5919f),
    Offset(0.7982f, 0.6049f),
    Offset(0.8035f, 0.6081f),
    Offset(0.8088f, 0.6146f),
    Offset(0.8141f, 0.6195f),
    Offset(0.8194f, 0.6260f),
    Offset(0.8247f, 0.6325f),
    Offset(0.8300f, 0.6407f),
    Offset(0.8352f, 0.6455f),
    Offset(0.8405f, 0.6520f),
    Offset(0.8458f, 0.6602f),
    Offset(0.8511f, 0.6650f),
    Offset(0.8564f, 0.6715f),
    Offset(0.8617f, 0.6764f),
    Offset(0.8670f, 0.6813f),
    Offset(0.8722f, 0.6878f),
    Offset(0.8775f, 0.6927f),
    Offset(0.8828f, 0.6943f),
    Offset(0.8881f, 0.6959f),
    Offset(0.8934f, 0.7220f),
    Offset(0.8987f, 0.7415f),
    Offset(0.9040f, 0.7545f),
    Offset(0.9093f, 0.7659f),
    Offset(0.9145f, 0.7756f),
    Offset(0.9198f, 0.7854f),
    Offset(0.9251f, 0.7935f),
    Offset(0.9304f, 0.8049f),
    Offset(0.9357f, 0.8130f),
    Offset(0.9410f, 0.8211f),
    Offset(0.9463f, 0.8276f),
    Offset(0.9515f, 0.8341f),
    Offset(0.9568f, 0.8423f),
    Offset(0.9621f, 0.8504f),
    Offset(0.9674f, 0.8569f),
    Offset(0.9727f, 0.8650f),
    Offset(0.9780f, 0.8764f),
    Offset(0.9833f, 0.8943f),
    Offset(0.9885f, 0.9050f),
    Offset(0.9938f, 0.9158f),
    Offset(0.9991f, 0.9265f),
    Offset(1.0000f, 0.9283f)
)

/**
 * Builds the clipping path for the front painting layer (ground layer).
 * Traverses all 191 ridge points from left to right, then closes along the bottom corners:
 * (iw + left, ih + top) -> (left, ih + top) -> close.
 */
fun buildRidgePath(box: WallpaperBox): Path {
    val path = Path()
    if (RIDGE_NORMALIZED_POINTS.isEmpty()) return path

    val first = RIDGE_NORMALIZED_POINTS[0]
    path.moveTo(box.left + first.x * box.iw, box.top + first.y * box.ih)

    for (i in 1 until RIDGE_NORMALIZED_POINTS.size) {
        val pt = RIDGE_NORMALIZED_POINTS[i]
        path.lineTo(box.left + pt.x * box.iw, box.top + pt.y * box.ih)
    }

    // Close along bottom of image
    path.lineTo(box.left + box.iw, box.top + box.ih)
    path.lineTo(box.left, box.top + box.ih)
    path.close()

    return path
}

/**
 * Interpolates RIDGE_NORMALIZED_POINTS to return the screen Y coordinate (in px)
 * of the ridge at a given screenX coordinate.
 * Used by Phase 4 fitWords() to ensure text fits above the mountain ridge.
 */
fun ridgeYAtScreenX(screenX: Float, box: WallpaperBox): Float {
    if (RIDGE_NORMALIZED_POINTS.isEmpty() || box.iw <= 0f) return box.top
    val normX = (screenX - box.left) / box.iw
    if (normX <= RIDGE_NORMALIZED_POINTS.first().x) {
        return box.top + RIDGE_NORMALIZED_POINTS.first().y * box.ih
    }
    if (normX >= RIDGE_NORMALIZED_POINTS.last().x) {
        return box.top + RIDGE_NORMALIZED_POINTS.last().y * box.ih
    }

    for (i in 0 until RIDGE_NORMALIZED_POINTS.size - 1) {
        val p0 = RIDGE_NORMALIZED_POINTS[i]
        val p1 = RIDGE_NORMALIZED_POINTS[i + 1]
        if (normX >= p0.x && normX <= p1.x) {
            val spanX = p1.x - p0.x
            val normY = if (spanX > 1e-6f) {
                val t = (normX - p0.x) / spanX
                p0.y + t * (p1.y - p0.y)
            } else {
                p0.y
            }
            return box.top + normY * box.ih
        }
    }

    return box.top + RIDGE_NORMALIZED_POINTS.last().y * box.ih
}

/**
 * Low-pass filtered tilt parallax state.
 * Reads accelerometer / gravity sensor and smooths with factor 0.08 per frame.
 * Falls back to subtle ambient breathing when sensors are unavailable.
 * Respects system animations: when ANIMATOR_DURATION_SCALE is 0, tilt offsets remain 0.
 */
class TiltParallaxState(
    animationsEnabled: Boolean = true
) {
    var animationsEnabled by mutableStateOf(animationsEnabled)
    var tiltX by mutableFloatStateOf(0f)
    var tiltY by mutableFloatStateOf(0f)
    var targetX by mutableFloatStateOf(0f)
    var targetY by mutableFloatStateOf(0f)

    constructor(context: Context) : this(
        animationsEnabled = isAnimationScaleNonZero(context)
    )

    fun updateLowPass(hasSensor: Boolean, timeSec: Float) {
        if (!animationsEnabled) {
            tiltX = 0f
            tiltY = 0f
            return
        }
        // Fallback subtle breathing if no sensor active (from prototype frame(): sin(t/3.1)*0.22, cos(t/4.3)*0.13)
        val tgtX = if (hasSensor) targetX else (sin(timeSec / 3.1f) * 0.22f)
        val tgtY = if (hasSensor) targetY else (cos(timeSec / 4.3f) * 0.13f)

        // Low-pass 0.08 filter: tilt += (tgt - tilt) * 0.08
        tiltX += (tgtX - tiltX) * 0.08f
        tiltY += (tgtY - tiltY) * 0.08f
    }

    companion object {
        fun isAnimationScaleNonZero(context: Context): Boolean {
            return try {
                val scale = Settings.Global.getFloat(
                    context.contentResolver,
                    Settings.Global.ANIMATOR_DURATION_SCALE,
                    1f
                )
                scale != 0f
            } catch (_: Exception) {
                true
            }
        }
    }
}

@Composable
fun rememberTiltParallaxState(): TiltParallaxState {
    val context = LocalContext.current
    val animationsEnabled = remember(context) {
        TiltParallaxState.isAnimationScaleNonZero(context)
    }
    val state = remember { TiltParallaxState(animationsEnabled = animationsEnabled) }
    state.animationsEnabled = animationsEnabled
    var hasSensor by remember { mutableStateOf(false) }

    DisposableEffect(context, animationsEnabled) {
        if (!animationsEnabled) {
            state.tiltX = 0f
            state.tiltY = 0f
            return@DisposableEffect onDispose {}
        }

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                hasSensor = true
                // Convert Android accelerometer/gravity coordinates to normalized [-1..1] tilt
                // Portrait mode: event.values[0] is X (tilt right -> -9.8), event.values[1] is Y (tilt up -> +9.8)
                val rawX = -event.values[0] / 9.81f
                val rawY = (event.values[1] - 5.5f) / 5.5f // Neutral viewing angle ~55 degrees
                state.targetX = rawX.coerceIn(-1.5f, 1.5f)
                state.targetY = rawY.coerceIn(-1.5f, 1.5f)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (sensorManager != null && sensor != null) {
            sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        }

        onDispose {
            sensorManager?.unregisterListener(listener)
        }
    }

    LaunchedEffect(animationsEnabled) {
        if (!animationsEnabled) {
            state.tiltX = 0f
            state.tiltY = 0f
            return@LaunchedEffect
        }
        var startTime = -1L
        while (isActive) {
            withInfiniteAnimationFrameMillis { frameTimeMillis ->
                if (startTime < 0) startTime = frameTimeMillis
                val timeSec = (frameTimeMillis - startTime) / 1000f
                state.updateLowPass(hasSensor, timeSec)
            }
        }
    }

    return state
}

/**
 * Loads wallpaper ImageBitmap from Android assets "wallpaper.webp".
 */
@Composable
fun rememberWallpaperBitmap(): ImageBitmap? {
    val context = LocalContext.current
    return produceState<ImageBitmap?>(initialValue = null, context) {
        value = try {
            context.assets.open("wallpaper.webp").use { inputStream ->
                BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
            }
        } catch (e: Exception) {
            null
        }
    }.value
}

/**
 * Main Depth Wallpaper & Clock Composable (Phase 3).
 */
@Composable
fun DepthWallpaperAndClock(
    modifier: Modifier = Modifier,
    fontDisplay: FontFamily = FoxFonts.Bricolage,
    fontBody: FontFamily = FoxFonts.Figtree,
    tiltState: TiltParallaxState = rememberTiltParallaxState(),
    overlayContent: @Composable BoxScope.(WallpaperBox) -> Unit = {}
) {
    val density = LocalDensity.current
    val wallpaperBitmap = rememberWallpaperBitmap()

    // Clock and date time states (ticks every 15 seconds)
    var timeString by remember { mutableStateOf("") }
    var dateString by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, d MMM", Locale.getDefault())
        while (isActive) {
            val now = Date()
            timeString = timeFormat.format(now)
            dateString = dateFormat.format(now)
            delay(15_000L)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(FoxColors.screenBg)
    ) {
        val screenW = with(density) { maxWidth.toPx() }
        val screenH = with(density) { maxHeight.toPx() }
        val box = remember(screenW, screenH) { WallpaperBox.calculate(screenW, screenH) }
        val ridgePath = remember(box) { buildRidgePath(box) }

        // Parallax offsets in pixels:
        // back:  tiltX * 5,  tiltY * 4
        // clock: tiltX * 10, tiltY * 8
        // front: tiltX * 18, tiltY * 13
        val backOffsetX = tiltState.tiltX * 5f
        val backOffsetY = tiltState.tiltY * 4f

        val clockOffsetX = tiltState.tiltX * 10f
        val clockOffsetY = tiltState.tiltY * 8f

        val frontOffsetX = tiltState.tiltX * 18f
        val frontOffsetY = tiltState.tiltY * 13f

        // -------------------------------------------------------------
        // LAYER 1: BACK WALLPAPER (Sky + background painting)
        // -------------------------------------------------------------
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            if (wallpaperBitmap != null) {
                drawImage(
                    image = wallpaperBitmap,
                    dstOffset = IntOffset(
                        (box.left + backOffsetX).roundToInt(),
                        (box.top + backOffsetY).roundToInt()
                    ),
                    dstSize = IntSize(box.iw.roundToInt(), box.ih.roundToInt())
                )
            } else {
                // Pre-render gradient fallback for preview/testing
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(FoxColors.screenBg, Color(0xFF131722), Color(0xFF221E2C))
                    )
                )
            }
        }

        // -------------------------------------------------------------
        // LAYER 2: CLOCK & DATE (Tucked behind the ridge)
        // Position: left 22dp, top 24.5% of H
        // -------------------------------------------------------------
        val clockTopDp = maxHeight * 0.245f
        Column(
            modifier = Modifier
                .padding(start = 22.dp)
                .offset {
                IntOffset(
                    x = clockOffsetX.roundToInt(),
                    y = (clockOffsetY + with(density) { clockTopDp.toPx() }).roundToInt()
                )
            }
        ) {
            // Date: Figtree 14sp, 600, t2 color (F6F0E6 68%), shadow 0 1px 8px rgba(0,0,0,.6), margin-left 6dp
            Text(
                text = dateString.ifEmpty { "Sunday, 8 Nov" },
                fontFamily = fontBody,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                letterSpacing = 0.02.em,
                color = FoxColors.t2,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color(0x99000000), // rgba(0,0,0,0.6)
                        offset = Offset(0f, 1f),
                        blurRadius = 8f
                    )
                ),
                modifier = Modifier.padding(start = 6.dp)
            )

            // Time: Bricolage 118sp, 700, line-height 0.86, letter-spacing -0.055em, t1 (F6F0E6), shadow 0 2px 24px rgba(0,0,0,.35)
            Text(
                text = timeString.ifEmpty { "20:18" },
                fontFamily = fontDisplay,
                fontWeight = FontWeight.Bold,
                fontSize = 118.sp,
                lineHeight = (118 * 0.86f).sp,
                letterSpacing = (-0.055).em,
                color = FoxColors.t1,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color(0x59000000), // rgba(0,0,0,0.35)
                        offset = Offset(0f, 2f),
                        blurRadius = 24f
                    )
                )
            )
        }

        // -------------------------------------------------------------
        // LAYER 3: FRONT WALLPAPER (Ground / Ridge layer)
        // Clipped to ridge path inside withTransform, moves by (tiltX * 18, tiltY * 13)
        // Both the ridge clip boundary and front painting move together
        // -------------------------------------------------------------
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            withTransform({ translate(left = frontOffsetX, top = frontOffsetY) }) {
                clipPath(ridgePath) {
                    if (wallpaperBitmap != null) {
                        drawImage(
                            image = wallpaperBitmap,
                            dstOffset = IntOffset(box.left.roundToInt(), box.top.roundToInt()),
                            dstSize = IntSize(box.iw.roundToInt(), box.ih.roundToInt())
                        )
                    } else {
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xEEFFFFFF), Color(0xFF2B3A4A), Color(0xFF0C0D14))
                            )
                        )
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // LAYER 4: BOTTOM DARK FADE (130dp)
        // .screen::after { height:130px; background:linear-gradient(transparent,rgba(5,6,9,.55) 55%,rgba(5,6,9,.85)) }
        // -------------------------------------------------------------
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(130.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to Color.Transparent,
                            0.55f to Color(0x8C050609), // rgba(5,6,9, 0.55)
                            1.00f to Color(0xD9050609)  // rgba(5,6,9, 0.85)
                        )
                    )
                )
        )

        // -------------------------------------------------------------
        // LAYER 5: OVERLAY CONTENT (Rendered above bottom fade)
        // E.g., Phase 4 home work block and words line
        // -------------------------------------------------------------
        overlayContent(box)
    }
}
