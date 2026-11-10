package com.fox.mobile.ui.line

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sin

enum class FoxActivityType {
    IDLE, MSG, MUSIC, TIMER, ORDER, XFER, CONT, FOX
}

data class FoxSong(
    val title: String,
    val artist: String,
    val color: Color,
    val bgStart: Color,
    val bgEnd: Color,
    val blobs: List<Pair<Color, Triple<Float, Float, Float>>>
)

data class FoxTimerState(
    val endSec: Float,
    val totalSec: Float,
    val paused: Boolean = false,
    val leftWhenPaused: Float = totalSec,
    val done: Boolean = false
)

data class FoxMusicState(
    val playing: Boolean = true,
    val songIndex: Int = 0
)

data class FoxOrderState(
    val t0Sec: Float
)

data class FoxOrderStage(
    val progress: Float,
    val label: String,
    val etaMinutes: Int,
    val delivered: Boolean
)

data class FoxMsgState(
    val from: String = "Kavin",
    val text: String,
    val count: Int = 1,
    val t0Sec: Float,
    val summarised: Boolean = false
)

data class FoxXferState(
    val t0Sec: Float,
    val durSec: Float = 1.6f,
    val dir: String = "out",
    val fileName: String = "MenuBarView.swift",
    val fileSizeMb: String = "2.4 MB",
    val done: Boolean = false,
    val gulpSec: Float = 0f
)

data class WaterDrop(
    val xNorm: Float = 0.5f,
    val yOffsetFromBottomDp: Float = 12f,
    val strength: Float,
    val color: Color,
    val inward: Boolean,
    val t0Sec: Float,
    val ringCount: Int = 1 + kotlin.math.round(strength * 4f).toInt(),
    val maxRadiusDp: Float = 240f + 540f * strength
)

class FoxLineState {

    companion object {
        private val baseUptime: Float = runCatching {
            val up = SystemClock.uptimeMillis() / 1000f
            if (up > 0f) up - 10f else (System.nanoTime() / 1_000_000_000f) - 10f
        }.getOrElse { (System.nanoTime() / 1_000_000_000f) - 10f }

        fun nowSec(): Float {
            return runCatching {
                val up = SystemClock.uptimeMillis() / 1000f
                if (up > 0f) up - baseUptime else (System.nanoTime() / 1_000_000_000f) - baseUptime
            }.getOrElse { (System.nanoTime() / 1_000_000_000f) - baseUptime }
        }

        fun performFoxBuzz(view: View, ms: Long = 6L) {
            try {
                val context = view.context
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vm?.defaultVibrator ?: (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }
                if (vibrator != null && vibrator.hasVibrator()) {
                    val effect = VibrationEffect.createOneShot(ms.coerceAtLeast(1L), VibrationEffect.DEFAULT_AMPLITUDE)
                    vibrator.vibrate(effect)
                    return
                }
            } catch (_: Throwable) {}
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }

        fun performFoxBuzz(view: View, timings: LongArray, amplitudes: IntArray? = null) {
            try {
                val context = view.context
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vm?.defaultVibrator ?: (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }
                if (vibrator != null && vibrator.hasVibrator()) {
                    val effect = if (amplitudes != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        VibrationEffect.createWaveform(timings, amplitudes, -1)
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        VibrationEffect.createWaveform(timings, -1)
                    } else {
                        null
                    }
                    if (effect != null) {
                        vibrator.vibrate(effect)
                        return
                    }
                }
            } catch (_: Throwable) {}
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    var timer: FoxTimerState? by mutableStateOf(null)
    var music: FoxMusicState? by mutableStateOf(null)
    var order: FoxOrderState? by mutableStateOf(null)
    var msg: FoxMsgState? by mutableStateOf(null)
    var xfer: FoxXferState? by mutableStateOf(null)
    var contT0Sec: Float? by mutableStateOf(null)
    var swapped: Boolean by mutableStateOf(false)
    var openIslandType: FoxActivityType? by mutableStateOf(null)
    var foxAwake: Boolean by mutableStateOf(false)
    var foxClosing: Boolean by mutableStateOf(false)
    var foxCloseT0Sec: Float by mutableFloatStateOf(0f)
    var holdActive: Boolean by mutableStateOf(false)
    var holdT0Sec: Float by mutableFloatStateOf(0f)
    var holdProgress: Float by mutableFloatStateOf(0f)
    var splashT0Sec: Float by mutableFloatStateOf(-9f)
    var splashAmp: Float by mutableFloatStateOf(0f)
    var debugMenuOpen: Boolean by mutableStateOf(false)
    var alarmSet: Boolean by mutableStateOf(false)
    val drops = mutableStateListOf<WaterDrop>()

    private var contDropFired: Boolean = false
    private val kavinMessages = listOf(
        "Lab tomorrow at 9?",
        "Bring the record book",
        "Viva is from unit 3"
    )

    val songs: List<FoxSong> = listOf(
        FoxSong(
            title = "Night Drive",
            artist = "Fox Radio",
            color = Color(0xFF8E8BFF),
            bgStart = Color(0xFF120F33),
            bgEnd = Color(0xFF2B2468),
            blobs = listOf(
                Color(0xFF6E6BFF) to Triple(0.62f, 0.38f, 0.34f),
                Color(0xFFB9A8FF) to Triple(0.30f, 0.70f, 0.16f),
                Color(0xFF1E1A4F) to Triple(0.80f, 0.85f, 0.30f)
            )
        ),
        FoxSong(
            title = "Ember Rain",
            artist = "Fox Radio",
            color = Color(0xFFFF784F),
            bgStart = Color(0xFF2A0E08),
            bgEnd = Color(0xFF5A1D10),
            blobs = listOf(
                Color(0xFFFF6A3D) to Triple(0.40f, 0.45f, 0.36f),
                Color(0xFFFFC07A) to Triple(0.72f, 0.25f, 0.14f),
                Color(0xFF3A120A) to Triple(0.20f, 0.85f, 0.30f)
            )
        )
    )

    fun timerLeft(now: Float = nowSec()): Float {
        val t = timer ?: return 0f
        return if (t.paused) t.leftWhenPaused else max(0f, t.endSec - now)
    }

    fun orderStage(now: Float = nowSec()): FoxOrderStage? {
        val o = order ?: return null
        val p = ((now - o.t0Sec) / 40f).coerceIn(0f, 1f)
        val label = when {
            p < 0.15f -> "Preparing"
            p < 0.85f -> "On the way"
            p < 1f -> "Arriving"
            else -> "Delivered"
        }
        val etaMinutes = max(0, ceil((1f - p) * 14f).toInt())
        return FoxOrderStage(
            progress = p,
            label = label,
            etaMinutes = etaMinutes,
            delivered = p >= 1f
        )
    }

    fun activeList(now: Float = nowSec()): List<FoxActivityType> {
        val list = mutableListOf<FoxActivityType>()
        if (xfer != null) list.add(FoxActivityType.XFER)
        val tl = timerLeft(now)
        if (timer != null && (timer!!.done || tl <= 10f)) list.add(FoxActivityType.TIMER)
        if (msg != null) list.add(FoxActivityType.MSG)
        if (contT0Sec != null) list.add(FoxActivityType.CONT)
        if (order != null) list.add(FoxActivityType.ORDER)
        if (timer != null && !list.contains(FoxActivityType.TIMER)) list.add(FoxActivityType.TIMER)
        if (music != null) list.add(FoxActivityType.MUSIC)

        if (swapped && list.size > 1 && !(timer?.done == true && list[0] == FoxActivityType.TIMER)) {
            val first = list[0]
            list[0] = list[1]
            list[1] = first
        }
        return list.take(2)
    }

    fun wavyBarOffset(u: Float, ageSec: Float): Float {
        val a = splashAmp * exp(-ageSec * 3.6f)
        return a * sin(Math.PI.toFloat() * u) * sin(u * 19f - ageSec * 22f)
    }

    fun isSplashActive(now: Float = nowSec()): Boolean {
        val age = now - splashT0Sec
        return age in 0f..1.1f && splashAmp * exp(-age * 3.6f) >= 0.15f
    }

    fun startTimer(seconds: Float = 20f, markDoneImmediately: Boolean = false) {
        val now = nowSec()
        timer = FoxTimerState(
            endSec = if (markDoneImmediately) now else now + seconds,
            totalSec = seconds,
            paused = false,
            leftWhenPaused = if (markDoneImmediately) 0f else seconds,
            done = markDoneImmediately
        )
    }

    fun toggleTimerPause() {
        val t = timer ?: return
        val now = nowSec()
        if (t.paused) {
            timer = t.copy(
                paused = false,
                endSec = now + t.leftWhenPaused
            )
        } else {
            val left = timerLeft(now)
            timer = t.copy(
                paused = true,
                leftWhenPaused = left
            )
        }
    }

    fun addTimerMinute() {
        val t = timer ?: return
        if (t.paused) {
            timer = t.copy(
                leftWhenPaused = t.leftWhenPaused + 60f,
                totalSec = t.totalSec + 60f
            )
        } else {
            timer = t.copy(
                endSec = t.endSec + 60f,
                totalSec = t.totalSec + 60f
            )
        }
    }

    fun stopTimer() {
        timer = null
        if (openIslandType == FoxActivityType.TIMER) {
            closeIsland()
        }
    }

    fun startMusic() {
        music = FoxMusicState(playing = true, songIndex = music?.songIndex ?: 0)
    }

    fun toggleMusicPlay() {
        val m = music
        if (m != null) {
            music = m.copy(playing = !m.playing)
        } else {
            startMusic()
        }
    }

    fun nextSong() {
        val nextIdx = ((music?.songIndex ?: 0) + 1) % songs.size
        music = FoxMusicState(playing = true, songIndex = nextIdx)
    }

    fun startOrder() {
        order = FoxOrderState(t0Sec = nowSec())
    }

    fun triggerMessage() {
        val currentCount = msg?.count ?: 0
        val nextCount = currentCount + 1
        val text = kavinMessages[(nextCount - 1) % kavinMessages.size]
        msg = FoxMsgState(
            from = "Kavin",
            text = text,
            count = nextCount,
            t0Sec = nowSec(),
            summarised = false
        )
    }

    fun summariseMessage() {
        msg = msg?.copy(
            text = "Kavin asks if lab is at 9 tomorrow and says to bring the record book.",
            summarised = true
        )
    }

    fun replyMessage() {
        msg = null
        if (openIslandType == FoxActivityType.MSG) {
            closeIsland()
        }
    }

    fun triggerContinue() {
        contT0Sec = nowSec()
        contDropFired = false
        splashBar(1f)
    }

    fun triggerTransfer(dir: String = "out", strength: Float = 0.5f) {
        val now = nowSec()
        xfer = FoxXferState(t0Sec = now, dir = dir)
        splashBar(1f)
        drop(
            strength = 0.12f + 0.1f * strength,
            color = if (dir == "in") Color(0xFFAAC8FF) else Color(0xFFF2B33D),
            inward = dir == "out"
        )
    }

    fun splashBar(k: Float = 1f) {
        splashT0Sec = nowSec()
        splashAmp = 3.2f * k
    }

    fun drop(strength: Float = 0.45f, color: Color = Color(0xFFF2B33D), inward: Boolean = false) {
        val s = strength.coerceIn(0.1f, 1f)
        drops.add(
            WaterDrop(
                xNorm = 0.5f,
                yOffsetFromBottomDp = 12f,
                strength = s,
                color = color,
                inward = inward,
                t0Sec = nowSec()
            )
        )
    }

    fun openIsland(type: FoxActivityType? = null) {
        openIslandType = type ?: activeList().firstOrNull() ?: FoxActivityType.IDLE
    }

    fun closeIsland() {
        openIslandType = null
    }

    fun summonFox() {
        foxAwake = true
        foxClosing = false
        closeIsland()
    }

    fun closeFox() {
        if (!foxAwake || foxClosing) return
        foxClosing = true
        foxCloseT0Sec = nowSec()
    }

    fun clearAll() {
        timer = null
        music = null
        order = null
        msg = null
        xfer = null
        contT0Sec = null
        swapped = false
        openIslandType = null
        foxAwake = false
        foxClosing = false
        holdActive = false
        holdProgress = 0f
        splashT0Sec = -9f
        splashAmp = 0f
        alarmSet = false
        drops.clear()
        contDropFired = false
    }

    fun needsFrameLoop(): Boolean {
        val now = nowSec()
        return holdActive || foxAwake || foxClosing ||
            activeList(now).isNotEmpty() || isSplashActive(now) ||
            drops.isNotEmpty() || contT0Sec != null || xfer != null
    }

    fun tick(now: Float, dt: Float) {
        // 1. Timer completion
        val t = timer
        if (t != null && !t.paused && !t.done && timerLeft(now) <= 0f) {
            timer = t.copy(done = true)
        }

        // 2. Cont completion
        val ct0 = contT0Sec
        if (ct0 != null) {
            val age = now - ct0
            if (age >= 1.1f && !contDropFired) {
                contDropFired = true
                drop(strength = 0.45f, color = Color(0xFFF2B33D), inward = false)
            }
            if (age >= 3.3f) {
                contT0Sec = null
                contDropFired = false
            }
        }

        // 3. Xfer completion
        val xf = xfer
        if (xf != null) {
            val age = now - xf.t0Sec
            if (age >= xf.durSec && !xf.done) {
                xfer = xf.copy(done = true)
                splashBar(1f)
            }
            if (age >= xf.durSec + 1.1f) {
                xfer = null
            }
        }

        // 4. Remove expired drops
        if (drops.isNotEmpty()) {
            drops.removeAll { d ->
                val age = now - d.t0Sec
                val life = 1.1f + 0.9f * d.strength
                age > life + d.ringCount * 0.12f
            }
        }

        // 5. Hold buzzes if holdActive
        if (holdActive) {
            val p = ((now - holdT0Sec) / 1.1f).coerceIn(0f, 1f)
            holdProgress = p
        }

        // 6. Fox closing completion (260ms matching prototype)
        if (foxClosing && (now - foxCloseT0Sec) >= 0.26f) {
            foxAwake = false
            foxClosing = false
        }
    }
}
