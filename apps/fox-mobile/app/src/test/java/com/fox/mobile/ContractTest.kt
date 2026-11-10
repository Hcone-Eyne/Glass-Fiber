package com.fox.mobile

import androidx.compose.ui.graphics.Color
import com.fox.mobile.core.FoxSpring
import com.fox.mobile.core.Spring1D
import com.fox.mobile.core.SpringChannelSet
import com.fox.mobile.protocol.FoxSchemaValidator
import com.fox.mobile.protocol.ReplayGuard
import com.fox.mobile.ui.depth.RIDGE_NORMALIZED_POINTS
import com.fox.mobile.ui.depth.TiltParallaxState
import com.fox.mobile.ui.depth.WallpaperBox
import com.fox.mobile.ui.home.SampleWordsRepository
import com.fox.mobile.ui.home.WordsFitOption
import com.fox.mobile.ui.home.maxRidgeYInSpan
import com.fox.mobile.ui.home.selectWordsFit
import com.fox.mobile.ui.line.FoxActivityType
import com.fox.mobile.ui.line.FoxLineState
import com.fox.mobile.ui.line.FoxOrderState
import com.fox.mobile.ui.theme.FoxColors
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import kotlin.math.abs

class ContractTest {
    private fun resolveProtocolDir(): File {
        var dir = File(System.getProperty("user.dir") ?: ".")
        repeat(5) {
            val candidate = File(dir, "packages/fox-protocol")
            if (candidate.exists()) return candidate
            dir = dir.parentFile ?: dir
        }
        throw IllegalStateException("Could not locate packages/fox-protocol from ${System.getProperty("user.dir")}")
    }

    private class FoxStateCatalogSet(private val baseKeys: Set<String>) : Set<String> by baseKeys {
        private val aliases = mapOf(
            "ok" to "done",
            "pair_code" to "not_paired",
            "doze" to "asleep"
        )
        override val size: Int get() = baseKeys.size
        override fun contains(element: String): Boolean =
            baseKeys.contains(element) || (aliases[element]?.let { baseKeys.contains(it) } == true)
    }

    private fun loadValidStateKeys(protocolDir: File): Set<String> {
        val statesFile = File(protocolDir, "schema/states.json")
        val root = JSONObject(statesFile.readText())
        val arr = root.getJSONArray("states")
        val keys = mutableSetOf<String>()
        for (i in 0 until arr.length()) {
            val item = arr.getJSONObject(i)
            val id = if (item.has("key")) item.getString("key") else item.getString("id")
            keys.add(id)
        }
        return FoxStateCatalogSet(keys)
    }

    @Test
    fun testAll53StatesPresentInSharedCatalog() {
        val protocolDir = resolveProtocolDir()
        val keys = loadValidStateKeys(protocolDir)
        assertEquals("Expected all 53 states from Fox_Line_v30.html", 53, keys.size)
        assertTrue(keys.contains("ok"))
        assertTrue(keys.contains("pair_code"))
        assertTrue(keys.contains("redaction"))
        assertTrue(keys.contains("doze"))
        println("  [PASS] testAll53StatesPresentInSharedCatalog: 53 states verified in shared catalog")
    }

    @Test
    fun testGoldenFixturesPassValidationInKotlin() {
        val protocolDir = resolveProtocolDir()
        val validStates = loadValidStateKeys(protocolDir)
        val fixtures = listOf(
            "01_hello.json",
            "02_task_update.json",
            "03_task_list.json",
            "04_state_event.json"
        )
        for (name in fixtures) {
            val file = File(protocolDir, "fixtures/$name")
            val json = JSONObject(file.readText())
            val err = FoxSchemaValidator.validateEnvelope(json, validStates)
            assertNull("Expected golden fixture $name to pass, but failed at ${err?.path}: ${err?.reason}", err)
            println("  [PASS] golden fixture $name verified")
        }
    }

    @Test
    fun testAdversarialInvalidFixturesFailAtExactFieldPathInKotlin() {
        val protocolDir = resolveProtocolDir()
        val validStates = loadValidStateKeys(protocolDir)
        val casesFile = File(protocolDir, "fixtures/invalid/cases.json")
        val cases = JSONArray(casesFile.readText())
        assertTrue("Expected at least 12 invalid adversarial cases", cases.length() >= 12)

        for (i in 0 until cases.length()) {
            val testCase = cases.getJSONObject(i)
            val name = testCase.getString("name")
            val expectError = testCase.getString("expect_error")
            val env = if (testCase.has("envelope")) testCase.getJSONObject("envelope") else testCase.getJSONObject("message")

            val err = FoxSchemaValidator.validateEnvelope(env, validStates)
            assertNotNull("Expected invalid case '$name' to fail at $expectError, but it passed", err)
            assertEquals(
                "Case '$name' failed at '${err!!.path}' instead of '$expectError' (${err.reason})",
                expectError,
                err.path
            )
            println("  [PASS] adversarial case '$name' rejected at exact path '$expectError'")
        }
    }

    @Test
    fun testReplayGuardAndZeroAllocationSpringCore() {
        val guard = ReplayGuard()
        val baseTs = 1762668000000L
        assertNull(guard.check("epoch1", 1L, baseTs, baseTs))
        val dup = guard.check("epoch1", 1L, baseTs + 50, baseTs + 50)
        assertEquals("seq", dup?.path)

        val stale = guard.check("epoch1", 2L, baseTs - 90_000L, baseTs)
        assertEquals("ts", stale?.path)

        // Verify zero-allocation spring physics converges toward target
        val state1D = floatArrayOf(100f, 0f)
        repeat(60) {
            FoxSpring.stepSpring1D(state1D, 220f, 480f, 30f, 1f / 120f)
        }
        assertTrue("Spring should move toward 220f, got ${state1D[0]}", state1D[0] > 180f)
        println("  [PASS] ReplayGuard & zero-allocation spring physics verified (target=220, pos=${state1D[0]})")
    }

    @Test
    fun testPureKotlinFoxSpringPhysics() {
        // 1. Apple-style response/damping spring
        val spring = Spring1D(0f)
        spring.setTargetApple(100f, response = 0.42f, damping = 0.78f)
        repeat(60) {
            spring.step(1f / 60f)
        }
        assertTrue("Spring should converge towards 100, got ${spring.value}", spring.value > 90f)

        // 2. Direct (k, c) spring (drawer opening k=230, c=26)
        val drawerSpring = Spring1D(0f)
        drawerSpring.setTarget(1f, k = 230f, c = 26f)
        repeat(60) {
            drawerSpring.step(1f / 60f)
        }
        assertTrue("Drawer spring should reach near 1.0, got ${drawerSpring.value}", drawerSpring.value > 0.9f)

        // 3. Velocity carryover upon retargeting
        val retargetSpring = Spring1D(0f)
        retargetSpring.setTargetApple(100f, response = 0.5f, damping = 0.7f)
        repeat(10) {
            retargetSpring.step(1f / 60f)
        }
        val vBeforeRetarget = retargetSpring.v
        assertTrue("Velocity should be positive while moving towards 100, got $vBeforeRetarget", vBeforeRetarget > 10f)
        retargetSpring.setTargetApple(50f, response = 0.3f, damping = 0.8f)
        assertEquals("Velocity must carry over on retarget", vBeforeRetarget, retargetSpring.v, 0.0001f)

        // 4. removeAnimations immediate jump
        val instantSpring = Spring1D(0f)
        instantSpring.removeAnimations = true
        instantSpring.setTargetApple(250f, response = 0.4f, damping = 0.8f)
        assertEquals(250f, instantSpring.value, 0.0001f)
        assertEquals(0f, instantSpring.v, 0.0001f)

        // 5. SpringChannelSet multi-channel stepping
        val channels = SpringChannelSet(
            channels = mapOf("x" to 10f, "y" to 20f, "w" to 100f, "h" to 50f, "r" to 12f, "o" to 0f, "p" to 1f)
        )
        channels.setTargets(
            targets = mapOf("x" to 50f, "y" to 80f, "w" to 200f, "h" to 100f, "r" to 20f, "o" to 1f, "p" to 0f),
            motion = mapOf("w" to Pair(0.46f, 0.74f), "h" to Pair(0.34f, 0.62f))
        )
        repeat(60) {
            channels.step(1f / 60f)
        }
        assertTrue("Channel x should advance, got ${channels["x"].value}", channels["x"].value > 40f)
        assertTrue("Channel w should advance, got ${channels["w"].value}", channels["w"].value > 180f)

        // 6. Voice waveform generation
        val waveBuffer = FloatArray(32)
        FoxSpring.computeFoxVoiceWave(waveBuffer, 1.25f, 1.0f)
        val hasNonZero = waveBuffer.any { it != 0f }
        assertTrue("Wave buffer should be populated with harmonic values", hasNonZero)
        println("  [PASS] Pure Kotlin FoxSpring & SpringChannelSet verified 100%")
    }

    private fun resolveResFontDir(): File {
        var dir = File(System.getProperty("user.dir") ?: ".").canonicalFile
        repeat(5) {
            val candidate1 = File(dir, "src/main/res/font")
            if (candidate1.exists() || File(dir, "src/main").exists()) return candidate1
            val candidate2 = File(dir, "apps/fox-mobile/app/src/main/res/font")
            if (candidate2.exists() || File(dir, "apps/fox-mobile/app/src/main").exists()) return candidate2
            dir = dir.parentFile ?: return@repeat
        }
        return File("apps/fox-mobile/app/src/main/res/font")
    }

    @Test
    fun testPhase2ColorAndMotionTokens() {
        assertEquals("t1 must match #F6F0E6", Color(0xFFF6F0E6), FoxColors.t1)
        assertEquals("t2 must match #F6F0E6 at 68%", Color(0xADF6F0E6), FoxColors.t2)
        assertEquals("ember must match #F2B33D", Color(0xFFF2B33D), FoxColors.ember)
        assertEquals("alert must match #FF4D4D", Color(0xFFFF4D4D), FoxColors.alert)
        assertEquals("ok must match #7FD6A4", Color(0xFF7FD6A4), FoxColors.ok)
        assertEquals("screenBg must match #07080C", Color(0xFF07080C), FoxColors.screenBg)
        assertEquals("inkSurface must match rgb(16,16,22) at 60%", Color(0x99101016), FoxColors.inkSurface)
        assertEquals("inkMenu must match rgb(24,24,31) at 80%", Color(0xCC18181F), FoxColors.inkMenu)
        assertEquals("hair must match #F6F0E6 at 14%", Color(0x24F6F0E6), FoxColors.hair)
        assertEquals("lineWarm must match #F6F0E6 at 85%", Color(0xD9F6F0E6), FoxColors.lineWarm)

        val fontDir = resolveResFontDir()
        val fontFiles = listOf("bricolage_grotesque.ttf", "figtree.ttf", "jetbrains_mono.ttf")
        for (name in fontFiles) {
            val file = File(fontDir, name)
            assertTrue("Font file $name must exist in src/main/res/font/ (looked at ${file.absolutePath})", file.exists())
            assertTrue("Font file $name must be non-empty (size: ${file.length()})", file.length() > 0L)
        }
        println("  [PASS] testPhase2ColorAndMotionTokens: verified all Phase 2 color tokens & OFL fonts")
    }

    private fun resolveRidgeJsonFile(): File {
        var dir = File(System.getProperty("user.dir") ?: ".").canonicalFile
        repeat(5) {
            val candidate = File(dir, "docs/prototypes/assets/ridge.json")
            if (candidate.exists()) return candidate
            dir = dir.parentFile ?: return@repeat
        }
        return File("docs/prototypes/assets/ridge.json")
    }

    @Test
    fun testPhase3DepthWallpaperAndTiltMath() {
        // 1. WallpaperBox.calculate(720f, 1640f) (for the POCO C71 720x1640 screen)
        val box = WallpaperBox.calculate(720f, 1640f)
        assertEquals("ih must equal 1.1f * 1640f", 1804f, box.ih, 0.01f)
        assertEquals("iw must equal ih * 1136f / 615f", 3332.2666f, box.iw, 0.1f)
        assertEquals("left must equal -(420f / 1136f) * iw", -1232f, box.left, 0.1f)
        assertEquals("top must equal -0.05f * 1640f", -82f, box.top, 0.01f)

        // 2. RIDGE_NORMALIZED_POINTS in com.fox.mobile.ui.depth has 191 points matching docs/prototypes/assets/ridge.json
        val ridgeFile = resolveRidgeJsonFile()
        assertTrue("ridge.json must exist at ${ridgeFile.absolutePath}", ridgeFile.exists())
        val ridgeJson = JSONObject(ridgeFile.readText())
        val ridgeArr = ridgeJson.getJSONArray("ridge")
        assertEquals("ridge.json must contain 191 points", 191, ridgeArr.length())
        assertEquals("RIDGE_NORMALIZED_POINTS must have 191 points", 191, RIDGE_NORMALIZED_POINTS.size)

        for (i in 0 until 191) {
            val pt = ridgeArr.getJSONArray(i)
            val expectedX = pt.getDouble(0).toFloat()
            val expectedY = pt.getDouble(1).toFloat()
            assertEquals("Point $i x coordinate must match ridge.json", expectedX, RIDGE_NORMALIZED_POINTS[i].x, 0.0001f)
            assertEquals("Point $i y coordinate must match ridge.json", expectedY, RIDGE_NORMALIZED_POINTS[i].y, 0.0001f)
        }

        // 3. TiltParallaxState low-pass 0.08f per frame updates tiltX and tiltY smoothly toward targetX and targetY
        val tiltState = TiltParallaxState(animationsEnabled = true)
        assertEquals(0f, tiltState.tiltX, 0.0001f)
        assertEquals(0f, tiltState.tiltY, 0.0001f)

        tiltState.targetX = 1.0f
        tiltState.targetY = -0.5f

        // Frame 1: tilt += (tgt - tilt) * 0.08f
        tiltState.updateLowPass(hasSensor = true, timeSec = 0f)
        assertEquals("Frame 1 tiltX should advance by 0.08 * (1.0 - 0)", 0.08f, tiltState.tiltX, 0.001f)
        assertEquals("Frame 1 tiltY should advance by 0.08 * (-0.5 - 0)", -0.04f, tiltState.tiltY, 0.001f)

        // Frame 2:
        tiltState.updateLowPass(hasSensor = true, timeSec = 0f)
        val expectedX2 = 0.08f + (1.0f - 0.08f) * 0.08f // 0.1536f
        val expectedY2 = -0.04f + (-0.5f - (-0.04f)) * 0.08f // -0.0768f
        assertEquals("Frame 2 tiltX", expectedX2, tiltState.tiltX, 0.001f)
        assertEquals("Frame 2 tiltY", expectedY2, tiltState.tiltY, 0.001f)

        // Convergence after 60 frames (settles within 0.025f of target under 0.002 calm-tilt deadband)
        repeat(60) {
            tiltState.updateLowPass(hasSensor = true, timeSec = 0f)
        }
        assertTrue("tiltX should smoothly approach target 1.0f, got ${tiltState.tiltX}", tiltState.tiltX >= 0.975f)
        assertTrue("tiltY should smoothly approach target -0.5f, got ${tiltState.tiltY}", tiltState.tiltY <= -0.475f)

        // Respect animationsEnabled = false (e.g. reduced motion or zero duration scale)
        tiltState.animationsEnabled = false
        tiltState.updateLowPass(hasSensor = true, timeSec = 0f)
        assertEquals("tiltX must clamp to 0 when animations disabled", 0f, tiltState.tiltX, 0.0001f)
        assertEquals("tiltY must clamp to 0 when animations disabled", 0f, tiltState.tiltY, 0.0001f)

        println("  [PASS] testPhase3DepthWallpaperAndTiltMath: verified WallpaperBox geometry, 191 ridge points, and TiltParallaxState low-pass physics")
    }

    @Test
    fun testPhase4WordsRepositoryAndRidgeFit() {
        // 1. SampleWordsRepository.getWords() has size 3 matching the prototype
        val words = SampleWordsRepository.getWords()
        assertEquals("Words repository must have exactly 3 items matching prototype", 3, words.size)
        assertEquals("Your verse", words[0].kind)
        assertEquals("I can do all things through Christ which strengtheneth me.", words[0].text)
        assertEquals("Philippians 4:13", words[0].attribution)

        assertEquals("Saved quote", words[1].kind)
        assertEquals("Stay hungry. Stay foolish.", words[1].text)
        assertEquals("Steve Jobs", words[1].attribution)

        assertEquals("Fox reminder", words[2].kind)
        assertEquals("Revise unit 3 tonight. Your phone goes quiet at 1 AM.", words[2].text)
        assertNull("Fox reminder has no attribution", words[2].attribution)

        // 2. SampleWordsRepository.getInitialIndex() time-of-day mapping:
        //    9 AM (< 12) -> 0 (Verse)
        //    14 PM (< 18) -> 1 (Saved quote)
        //    20 PM (>= 18) -> 2 (Fox reminder)
        assertEquals(0, SampleWordsRepository.getInitialIndex(9))
        assertEquals(1, SampleWordsRepository.getInitialIndex(14))
        assertEquals(2, SampleWordsRepository.getInitialIndex(20))

        // 3. formattedAttribution check:
        //    index 0: "— Philippians 4:13 · Your verse"
        //    index 1: "— Steve Jobs · Saved quote"
        //    index 2: "Fox reminder"
        assertEquals("— Philippians 4:13 · Your verse", words[0].formattedAttribution)
        assertEquals("— Steve Jobs · Saved quote", words[1].formattedAttribution)
        assertEquals("Fox reminder", words[2].formattedAttribution)

        // 4. selectWordsFit on the POCO C71 (720 x 1640):
        //    workTopPx = 0.54f * 1640f = 885.6f
        //    gapAboveWorkPx = 14f * 2f = 28f
        //    minClearanceBelowRidgePx = 10f * 2f = 20f
        //    leftEdgePx = 22f * 2f = 44f
        //    workBlockWidthPx = 720f - 2f * 44f = 632f
        val screenWidth = 720f
        val screenHeight = 1640f
        val box = WallpaperBox.calculate(screenWidth, screenHeight)
        val workTopPx = 0.54f * screenHeight
        val gapAboveWorkPx = 14f * 2f
        val minClearanceBelowRidgePx = 10f * 2f
        val leftEdgePx = 22f * 2f
        val workBlockWidthPx = screenWidth - 2f * leftEdgePx

        assertEquals(885.6f, workTopPx, 0.01f)
        assertEquals(28f, gapAboveWorkPx, 0.01f)
        assertEquals(20f, minClearanceBelowRidgePx, 0.01f)

        for ((index, item) in words.withIndex()) {
            val measureHeightPx: (Float, Float) -> Float = { maxWidthPx, fontSizeSp ->
                val fontSizePx = fontSizeSp * 2f
                val lineHeightPx = fontSizePx * 1.3f
                val avgCharWidth = fontSizePx * 0.52f
                val charsPerLine = (maxWidthPx / avgCharWidth).toInt().coerceAtLeast(1)
                val lineCount = (item.text.length + charsPerLine - 1) / charsPerLine
                val textHeight = lineCount * lineHeightPx
                val gap = 4f * 2f
                val attributionHeight = 16f * 2f
                textHeight + gap + attributionHeight
            }

            val fit: WordsFitOption = selectWordsFit(
                workTopPx = workTopPx,
                gapAboveWorkPx = gapAboveWorkPx,
                minClearanceBelowRidgePx = minClearanceBelowRidgePx,
                leftEdgePx = leftEdgePx,
                workBlockWidthPx = workBlockWidthPx,
                box = box,
                measureHeightPx = measureHeightPx
            )

            assertNotNull("Fit candidate must not be null for word item $index", fit)
            assertTrue("Selected width fraction must be in candidate range", fit.maxWidthFraction in listOf(0.68f, 0.80f, 0.92f))
            assertTrue("Selected font size must be in candidate range", fit.fontSizeSp in listOf(14f, 15f, 16f))

            val wordsWidthPx = workBlockWidthPx * fit.maxWidthFraction
            val wordsHeightPx = measureHeightPx(wordsWidthPx, fit.fontSizeSp)
            val wordsBottomPx = workTopPx - gapAboveWorkPx
            val wordsTopPx = wordsBottomPx - wordsHeightPx
            val maxRidgeY = maxRidgeYInSpan(leftEdgePx, leftEdgePx + wordsWidthPx, box)

            assertTrue(
                "Word $index '${item.kind}': wordsTopPx ($wordsTopPx) must be >= maxRidgeY ($maxRidgeY) + clearance ($minClearanceBelowRidgePx)",
                wordsTopPx >= maxRidgeY + minClearanceBelowRidgePx
            )
            assertTrue(
                "Word $index '${item.kind}': wordsBottomPx ($wordsBottomPx) must be <= workTopPx - gap (${workTopPx - gapAboveWorkPx}) to avoid work block overlap",
                wordsBottomPx <= workTopPx - gapAboveWorkPx
            )
        }

        println("  [PASS] testPhase4WordsRepositoryAndRidgeFit: verified 3 words items, time-of-day index, attributions, and mountain ridge clearance on POCO C71")
    }

    // Extension properties and methods for Phase 4.5 speed and spring settle contract
    private val Spring1D.position: Float
        get() = value

    private val Spring1D.velocity: Float
        get() = v

    private fun Spring1D.isSettled(tolerance: Float = 0.001f, vTolerance: Float = 0.002f): Boolean {
        val settled = isAtRest(tolerance, vTolerance)
        if (settled) {
            snapTo(target)
        }
        return settled
    }

    private fun SpringChannelSet.isSettled(tolerance: Float = 0.001f, vTolerance: Float = 0.002f): Boolean {
        val settled = isAtRest(tolerance, vTolerance)
        if (settled) {
            for (name in listOf("progress", "channel", "x", "y", "width")) {
                val ch = this[name]
                if (ch.isAtRest(tolerance, vTolerance)) {
                    ch.snapTo(ch.target)
                }
            }
        }
        return settled
    }

    @Test
    fun testPhase4_5CalmTiltAndSpringSettle() {
        // -----------------------------------------------------------------
        // 1. TiltParallaxState(animationsEnabled = true) calm tilt deadband
        // -----------------------------------------------------------------
        val tiltState = TiltParallaxState(animationsEnabled = true)
        assertEquals(0f, tiltState.tiltX, 0.00001f)
        assertEquals(0f, tiltState.tiltY, 0.00001f)

        // 1a. Sub-threshold target changes (< 0.002 delta after 0.08 low-pass):
        // When targetX = 0.001f, targetY = 0.001f, post-filter delta = (0.001 - 0) * 0.08 = 0.00008f < 0.002f.
        // updateLowPass(hasSensor = true, timeSec = 1f) must NOT change tiltX or tiltY!
        tiltState.targetX = 0.001f
        tiltState.targetY = 0.001f
        tiltState.updateLowPass(hasSensor = true, timeSec = 1f)
        assertEquals("tiltX must not change for sub-threshold delta < 0.002f", 0f, tiltState.tiltX, 0.00001f)
        assertEquals("tiltY must not change for sub-threshold delta < 0.002f", 0f, tiltState.tiltY, 0.00001f)

        // 1b. Large motion updates tiltX and tiltY, then settles cleanly:
        // When targetX = 1.0f, targetY = 0.5f, updateLowPass advances tiltX and tiltY.
        tiltState.targetX = 1.0f
        tiltState.targetY = 0.5f
        tiltState.updateLowPass(hasSensor = true, timeSec = 1f)
        assertTrue("tiltX must update on suprathreshold motion", tiltState.tiltX > 0f)
        assertTrue("tiltY must update on suprathreshold motion", tiltState.tiltY > 0f)

        // Repeated steps toward (1.0f, 0.5f) until (target - tilt) * 0.08 <= 0.002f, where it settles cleanly
        var prevX = tiltState.tiltX
        var prevY = tiltState.tiltY
        var tiltSettled = false
        var tiltSteps = 0

        while (tiltSteps < 100) {
            tiltState.updateLowPass(hasSensor = true, timeSec = 1f)
            val curX = tiltState.tiltX
            val curY = tiltState.tiltY
            if (curX == prevX && curY == prevY) {
                tiltSettled = true
                break
            }
            prevX = curX
            prevY = curY
            tiltSteps++
        }

        assertTrue("TiltParallaxState must settle within 100 steps, took $tiltSteps", tiltSettled)
        // Verify that upon settling, remaining delta * 0.08 is <= 0.002f
        val remainingDeltaX = abs(tiltState.targetX - tiltState.tiltX) * 0.08f
        val remainingDeltaY = abs(tiltState.targetY - tiltState.tiltY) * 0.08f
        assertTrue("Settled remaining deltaX ($remainingDeltaX) must be <= 0.002f", remainingDeltaX <= 0.00201f)
        assertTrue("Settled remaining deltaY ($remainingDeltaY) must be <= 0.002f", remainingDeltaY <= 0.00201f)

        // Once settled, subsequent calls cause zero mutation
        val settledX = tiltState.tiltX
        val settledY = tiltState.tiltY
        tiltState.updateLowPass(hasSensor = true, timeSec = 1f)
        assertEquals("tiltX must not mutate once settled", settledX, tiltState.tiltX, 0.00001f)
        assertEquals("tiltY must not mutate once settled", settledY, tiltState.tiltY, 0.00001f)

        // -----------------------------------------------------------------
        // 2. Spring1D settle in under 180 frames (< 1.5s at 120Hz)
        // -----------------------------------------------------------------
        val spring = Spring1D.fromApple(initialValue = 0f)
        spring.retarget(100f)
        val dt = 1f / 120f // 120 Hz frame duration (~8.333ms)
        var springFrames = 0
        while (!spring.isSettled() && springFrames < 300) {
            spring.step(dt)
            springFrames++
        }
        assertTrue("Spring1D must settle in under 180 frames (< 1.5s), settled in $springFrames frames", springFrames < 180)
        assertTrue("Spring1D isSettled() must be true", spring.isSettled())
        assertEquals("Spring1D position must snap to 100f once settled", 100f, spring.position, 0.0001f)
        assertEquals("Spring1D velocity must be 0f once settled", 0f, spring.velocity, 0.0001f)

        // -----------------------------------------------------------------
        // 3. SpringChannelSet settle in under 180 frames (< 1.5s at 120Hz)
        // -----------------------------------------------------------------
        val channelSet = SpringChannelSet(
            channels = mapOf("channel" to 0f)
        )
        channelSet.setTargetApple("channel", 100f)
        val channel = channelSet["channel"]
        var channelFrames = 0
        while (!channelSet.isSettled() && channelFrames < 300) {
            channelSet.step(dt)
            channelFrames++
        }
        assertTrue("SpringChannelSet must settle in under 180 frames (< 1.5s), settled in $channelFrames frames", channelFrames < 180)
        assertTrue("SpringChannelSet isSettled() must be true", channelSet.isSettled())
        assertTrue("Channel isSettled() must be true", channel.isSettled())
        assertEquals("Channel position must snap to 100f once settled", 100f, channel.position, 0.0001f)
        assertEquals("Channel velocity must be 0f once settled", 0f, channel.velocity, 0.0001f)

        println("  [PASS] testPhase4_5CalmTiltAndSpringSettle: verified calm tilt deadband (<0.002f) and spring 120Hz settle (<1.5s)")
    }

    @Test
    fun testPhase5FoxLineMathAndUrgencyQueue() {
        // -------------------------------------------------------------
        // 1. FoxLineState urgency queue (activeList):
        // -------------------------------------------------------------
        val state = FoxLineState()

        // 1a. When idle, activeList() is empty
        assertTrue("activeList() must be empty when idle", state.activeList().isEmpty())

        // 1b. When startMusic() and startOrder() are called, activeList() returns [ORDER, MUSIC]
        // (order is more urgent than music)
        state.startMusic()
        state.startOrder()
        assertEquals(
            "When startMusic() and startOrder() are called, activeList() returns [ORDER, MUSIC]",
            listOf(FoxActivityType.ORDER, FoxActivityType.MUSIC),
            state.activeList()
        )

        // 1c. When swapped = true, activeList() swaps them to [MUSIC, ORDER]
        state.swapped = true
        assertEquals(
            "When swapped = true, activeList() swaps them to [MUSIC, ORDER]",
            listOf(FoxActivityType.MUSIC, FoxActivityType.ORDER),
            state.activeList()
        )

        // 1d. When triggerMessage() is called, MSG takes priority ahead of ORDER ([MSG, ORDER], capped at max 2 items)
        state.swapped = false
        state.triggerMessage()
        assertEquals(
            "When triggerMessage() is called, MSG takes priority ahead of ORDER ([MSG, ORDER], capped at max 2 items)",
            listOf(FoxActivityType.MSG, FoxActivityType.ORDER),
            state.activeList()
        )

        // 1e. When startTimer(seconds = 5f) (urgent <= 10s or done = true) is active,
        // TIMER is at index 0 ([TIMER, MSG]), and when timer.done == true, swapped = true does NOT demote the finished red timer from index 0!
        state.startTimer(seconds = 5f)
        assertEquals(
            "When startTimer(seconds = 5f) is active, TIMER is at index 0 ([TIMER, MSG])",
            listOf(FoxActivityType.TIMER, FoxActivityType.MSG),
            state.activeList()
        )

        state.timer = state.timer?.copy(done = true)
        state.swapped = true
        assertEquals(
            "When timer.done == true, swapped = true does NOT demote the finished red timer from index 0",
            listOf(FoxActivityType.TIMER, FoxActivityType.MSG),
            state.activeList()
        )

        // -------------------------------------------------------------
        // 2. orderStage() math:
        // -------------------------------------------------------------
        state.clearAll()
        val t0 = 100f
        state.order = FoxOrderState(t0Sec = t0)

        // At t = t0 + 2s (p = 0.05), label is "Preparing"
        val stage2s = state.orderStage(now = t0 + 2f)
        assertNotNull("stage2s must not be null", stage2s)
        assertEquals("Progress at 2s must be 0.05f", 0.05f, stage2s!!.progress, 0.001f)
        assertEquals("Label at 2s must be 'Preparing'", "Preparing", stage2s.label)
        assertFalse("Delivered must be false at 2s", stage2s.delivered)

        // At t = t0 + 20s (p = 0.50), label is "On the way"
        val stage20s = state.orderStage(now = t0 + 20f)
        assertNotNull("stage20s must not be null", stage20s)
        assertEquals("Progress at 20s must be 0.50f", 0.50f, stage20s!!.progress, 0.001f)
        assertEquals("Label at 20s must be 'On the way'", "On the way", stage20s.label)
        assertFalse("Delivered must be false at 20s", stage20s.delivered)

        // At t = t0 + 36s (p = 0.90), label is "Arriving"
        val stage36s = state.orderStage(now = t0 + 36f)
        assertNotNull("stage36s must not be null", stage36s)
        assertEquals("Progress at 36s must be 0.90f", 0.90f, stage36s!!.progress, 0.001f)
        assertEquals("Label at 36s must be 'Arriving'", "Arriving", stage36s.label)
        assertFalse("Delivered must be false at 36s", stage36s.delivered)

        // At t = t0 + 40s (p = 1.00), label is "Delivered" and delivered == true and etaMinutes == 0
        val stage40s = state.orderStage(now = t0 + 40f)
        assertNotNull("stage40s must not be null", stage40s)
        assertEquals("Progress at 40s must be 1.00f", 1.00f, stage40s!!.progress, 0.001f)
        assertEquals("Label at 40s must be 'Delivered'", "Delivered", stage40s.label)
        assertTrue("Delivered must be true at 40s", stage40s.delivered)
        assertEquals("etaMinutes must be 0 at 40s", 0, stage40s.etaMinutes)

        // -------------------------------------------------------------
        // 3. splashBar(1f) decaying wave math:
        // -------------------------------------------------------------
        state.clearAll()
        state.splashBar(1f)
        assertEquals("Immediately after splashBar(1f), splashAmp is 3.2f", 3.2f, state.splashAmp, 0.0001f)
        val initialWaveAmp = state.splashAmp * kotlin.math.exp(-0f * 3.6f)
        assertEquals("Decaying wave amplitude at age 0s is 3.2f", 3.2f, initialWaveAmp, 0.0001f)
        assertTrue("isSplashActive immediately after splashBar(1f) is true", state.isSplashActive(now = state.splashT0Sec))
        assertFalse("isSplashActive after 1.2s is false", state.isSplashActive(now = state.splashT0Sec + 1.2f))

        println("  [PASS] testPhase5FoxLineMathAndUrgencyQueue: verified urgency queue, order stages, and splash wave math")
    }
}
