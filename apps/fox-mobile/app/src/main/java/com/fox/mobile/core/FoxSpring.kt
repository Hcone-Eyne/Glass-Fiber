package com.fox.mobile.core

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sin

/**
 * Pure Kotlin Spring Physics for Fox Mobile.
 *
 * Implements Apple-style response/damping springs (k = (2π / response)², c = 4π * damping / response),
 * direct (k, c) springs, velocity carryover, 4-substep integration per frame,
 * and removeAnimations support. Zero C/JNI/CMake dependencies.
 */
class Spring1D(
    initialValue: Float = 0f
) {
    var value: Float = initialValue
    var v: Float = 0f
    var target: Float = initialValue
        set(newTarget) {
            field = newTarget
            if (removeAnimations) {
                value = newTarget
                v = 0f
            }
        }
    var k: Float = 230f
    var c: Float = 26f
    var removeAnimations: Boolean = false

    fun setCoefficientsApple(response: Float, damping: Float) {
        val (newK, newC) = FoxSpring.coefficientsFromApple(response, damping)
        k = newK
        c = newC
    }

    fun retarget(target: Float) {
        this.target = target
    }

    /** Direct k (stiffness) and c (damping coefficient) */
    fun setTarget(target: Float, k: Float, c: Float) {
        this.k = k
        this.c = c
        this.target = target
    }

    /** Apple-style response and damping ratio */
    fun setTargetApple(target: Float, response: Float, damping: Float) {
        setCoefficientsApple(response, damping)
        this.target = target
        if (removeAnimations) {
            value = target
            v = 0f
        }
    }

    fun setTargetResponse(target: Float, response: Float, damping: Float) =
        setTargetApple(target, response, damping)

    fun snapTo(value: Float) {
        this.value = value
        this.target = value
        this.v = 0f
    }

    fun step(dt: Float) {
        if (removeAnimations) {
            value = target
            v = 0f
            return
        }
        val h = dt * 0.25f
        repeat(4) {
            v += ((target - value) * k - v * c) * h
            value += v * h
        }
    }

    fun isAtRest(tolerance: Float = 0.001f, vTolerance: Float = 0.002f): Boolean {
        return abs(target - value) < tolerance && abs(v) < vTolerance
    }

    companion object {
        fun fromApple(initialValue: Float = 0f, response: Float = 0.42f, damping: Float = 0.78f): Spring1D {
            val spring = Spring1D(initialValue)
            spring.setTargetApple(initialValue, response, damping)
            return spring
        }

        fun fromDirect(initialValue: Float = 0f, k: Float = 230f, c: Float = 26f): Spring1D {
            val spring = Spring1D(initialValue)
            spring.setTarget(initialValue, k, c)
            return spring
        }
    }
}

/**
 * A coordinated set of named 1D spring channels (e.g. x, y, w, h, r, o, p for liquid shapes).
 * Velocity carries over across retargeting calls.
 */
class SpringChannelSet(
    channels: Map<String, Float> = emptyMap(),
    val defaultResponse: Float = 0.42f,
    val defaultDamping: Float = 0.78f
) {
    private val channelMap = mutableMapOf<String, Spring1D>()

    var removeAnimations: Boolean = false
        set(value) {
            field = value
            for (ch in channelMap.values) {
                ch.removeAnimations = value
            }
        }

    init {
        for ((name, initial) in channels) {
            val spring = Spring1D(initial)
            spring.setTargetApple(initial, defaultResponse, defaultDamping)
            spring.removeAnimations = removeAnimations
            channelMap[name] = spring
        }
    }

    fun getOrCreate(name: String, initialValue: Float = 0f): Spring1D {
        return channelMap.getOrPut(name) {
            val spring = Spring1D(initialValue)
            spring.setTargetApple(initialValue, defaultResponse, defaultDamping)
            spring.removeAnimations = removeAnimations
            spring
        }
    }

    operator fun get(name: String): Spring1D = getOrCreate(name)

    fun setTarget(name: String, target: Float) {
        getOrCreate(name).target = target
    }

    fun setTarget(name: String, target: Float, k: Float, c: Float) {
        getOrCreate(name).setTarget(target, k, c)
    }

    fun setTargetApple(name: String, target: Float, response: Float = defaultResponse, damping: Float = defaultDamping) {
        getOrCreate(name).setTargetApple(target, response, damping)
    }

    fun setTargetResponse(name: String, target: Float, response: Float = defaultResponse, damping: Float = defaultDamping) {
        getOrCreate(name).setTargetApple(target, response, damping)
    }

    fun setTargets(
        targets: Map<String, Float>,
        motion: Map<String, Pair<Float, Float>> = emptyMap()
    ) {
        for ((name, target) in targets) {
            val pair = motion[name] ?: Pair(defaultResponse, defaultDamping)
            setTargetApple(name, target, pair.first, pair.second)
        }
    }

    fun step(dt: Float) {
        for (ch in channelMap.values) {
            ch.step(dt)
        }
    }

    fun isAtRest(tolerance: Float = 0.001f, vTolerance: Float = 0.002f): Boolean {
        return channelMap.values.all { it.isAtRest(tolerance, vTolerance) }
    }
}

/**
 * Zero-allocation primitive math helpers for frame loops and waveform generation.
 */
object FoxSpring {
    /** Step a state array [value, velocity] using direct k and c */
    fun stepSpring1D(
        state: FloatArray,
        target: Float,
        k: Float,
        c: Float,
        dt: Float,
        removeAnimations: Boolean = false
    ) {
        if (removeAnimations) {
            state[0] = target
            state[1] = 0f
            return
        }
        val h = dt * 0.25f
        repeat(4) {
            state[1] += ((target - state[0]) * k - state[1] * c) * h
            state[0] += state[1] * h
        }
    }

    /** Step a state array [value, velocity] using Apple response and damping */
    fun stepSpring1DApple(
        state: FloatArray,
        target: Float,
        response: Float,
        damping: Float,
        dt: Float,
        removeAnimations: Boolean = false
    ) {
        val (k, c) = coefficientsFromApple(response, damping)
        stepSpring1D(state, target, k, c, dt, removeAnimations)
    }

    fun coefficientsFromApple(response: Float, damping: Float): Pair<Float, Float> {
        val omega = (2f * PI.toFloat()) / response
        val k = omega * omega
        val c = (4f * PI.toFloat() * damping) / response
        return Pair(k, c)
    }

    fun computeFoxVoiceWave(outY: FloatArray, t: Float, amp: Float = 1.0f) {
        val phrase = if (sin(t * 0.7f) > -0.35f) 1f else 0f
        val sRaw = sin(t * 6.1f)
        val syl = (if (sRaw > 0f) sRaw else 0f).pow(0.7f) * (0.55f + 0.45f * sin(t * 2.3f + 1f))
        val speech = phrase * (0.2f + 0.8f * syl) * amp
        val pi = PI.toFloat()
        val count = outY.size
        val width = 100f
        for (idx in 0 until count) {
            val u = if (count > 1) idx.toFloat() / (count - 1).toFloat() else 0f
            val x = u * width
            val envSin = sin(pi * u)
            val wave = speech * (sin(x * 0.14f - t * 11f) * 6f + sin(x * 0.05f + t * 4f) * 3f)
            outY[idx] = envSin * wave
        }
    }
}
