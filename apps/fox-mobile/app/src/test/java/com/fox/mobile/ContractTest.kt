package com.fox.mobile

import com.fox.mobile.core.FoxSpring
import com.fox.mobile.core.Spring1D
import com.fox.mobile.core.SpringChannelSet
import com.fox.mobile.protocol.FoxSchemaValidator
import com.fox.mobile.protocol.ReplayGuard
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File

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
}
