package com.fox.mobile.protocol

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import kotlin.math.abs

/**
 * FoxLink v1 Kotlin protocol validator & state catalog.
 *
 * Implements the exact same JSON Schema subset and error-path reporting as
 * `apps/fox-desk/Sources/FoxDeskCore/FoxProtocol.swift` so both pass the golden
 * fixtures in `packages/fox-protocol`.
 */
class ProtocolError(val path: String, val reason: String) :
    IllegalArgumentException("${path.ifEmpty { "<root>" }}: $reason") {
    override fun toString(): String = "${path.ifEmpty { "<root>" }}: $reason"
}

data class FoxTask(
    val id: String = "",
    val title: String = "",
    val source: String = "git",
    val status: String = "active",
    val ts: Long = 0L,
    val project: String = "",
    val branch: String? = null,
    val primaryFile: String? = null,
    val summary: String = "",
    val pinned: Boolean = false,
    val uncommittedCount: Int? = null
)

data class FoxStateSpec(
    val key: String,
    val group: String = "General",
    val color: String = "#f2b33d",
    val tag: String = "",
    val sub: String = "",
    val title: String = "",
    val body: String = "",
    val code: String? = null,
    val actions: List<String> = emptyList(),
    val log: List<LogEntry> = emptyList()
)

data class LogEntry(
    val time: String = "",
    val event: String = "",
    val status: String = "info"
)

class FoxProtocolCatalog(val protocolDir: File? = null) {
    private val payloadCache = mutableMapOf<String, JSONObject>()

    private fun resolveDir(): File? {
        if (protocolDir != null && protocolDir.exists()) return protocolDir
        var dir = File(System.getProperty("user.dir") ?: ".")
        repeat(6) {
            val candidate = File(dir, "packages/fox-protocol")
            if (candidate.exists()) return candidate
            dir = dir.parentFile ?: return null
        }
        return null
    }

    fun getEnvelopeSchema(): JSONObject {
        val dir = resolveDir()
        if (dir != null) {
            val f = File(dir, "schema/envelope.schema.json")
            if (f.exists()) return JSONObject(f.readText())
        }
        return JSONObject(FoxSchemaValidator.EMBEDDED_ENVELOPE_SCHEMA)
    }

    fun getPayloadSchema(named: String): JSONObject {
        val filename = if (named.endsWith(".json")) named else "$named.json"
        payloadCache[filename]?.let { return it }

        val dir = resolveDir()
        if (dir != null) {
            val f = File(dir, "schema/payloads/$filename")
            if (f.exists()) {
                val obj = JSONObject(f.readText())
                payloadCache[filename] = obj
                return obj
            }
        }

        val embedded = FoxSchemaValidator.EMBEDDED_PAYLOADS[filename]
            ?: throw ProtocolError("", "schema not found for $filename")
        val obj = JSONObject(embedded)
        payloadCache[filename] = obj
        return obj
    }
}

object FoxSchemaValidator {
    private val ALLOWED_KEYWORDS = setOf(
        "\$schema", "\$id", "title", "description",
        "type", "required", "properties", "additionalProperties", "enum", "const",
        "minimum", "maxLength", "minLength", "pattern", "items", "maxItems", "\$ref"
    )

    private val catalog = FoxProtocolCatalog()

    private fun join(path: String, key: String): String =
        if (path.isEmpty()) key else "$path.$key"

    private fun isNull(v: Any?): Boolean = v == null || v === JSONObject.NULL

    private fun isInt(v: Any?): Boolean {
        if (v == null || v is Boolean) return false
        if (v is Int || v is Long || v is Short || v is Byte) return true
        if (v is Number) {
            val d = v.toDouble()
            return d == Math.floor(d) && !d.isInfinite() && !d.isNaN()
        }
        return false
    }

    private fun isNumber(v: Any?): Boolean =
        v != null && v !is Boolean && v is Number

    private fun typeOk(value: Any?, t: String): Boolean = when (t) {
        "null" -> isNull(value)
        "boolean" -> value is Boolean
        "integer" -> isInt(value)
        "number" -> isNumber(value)
        "string" -> value is String
        "array" -> value is JSONArray
        "object" -> value is JSONObject
        else -> throw ProtocolError("", "unknown schema type '$t'")
    }

    private fun valuesEqual(a: Any?, b: Any?): Boolean {
        if (isNull(a) && isNull(b)) return true
        if (a is Boolean && b is Boolean) return a == b
        if (isNumber(a) && isNumber(b)) {
            return (a as Number).toDouble() == (b as Number).toDouble()
        }
        if (a is String && b is String) return a == b
        return a == b
    }

    fun validate(
        value: Any?,
        schema: JSONObject,
        resolveRef: (String) -> JSONObject,
        path: String = ""
    ) {
        val keys = schema.keys().asSequence().toSet()
        val unknown = keys - ALLOWED_KEYWORDS
        if (unknown.isNotEmpty()) {
            throw ProtocolError(path, "schema uses unsupported keywords ${unknown.sorted()}")
        }
        if (schema.has("\$ref")) {
            val ref = schema.getString("\$ref")
            if (ref.contains('/') || ref.contains('\\') || ref.startsWith('.')) {
                throw ProtocolError("", "illegal \$ref '$ref'")
            }
            validate(value, resolveRef(ref), resolveRef, path)
            return
        }
        if (schema.has("type")) {
            val typeSpec = schema.get("type")
            val types = when (typeSpec) {
                is JSONArray -> (0 until typeSpec.length()).map { typeSpec.getString(it) }
                is String -> listOf(typeSpec)
                else -> throw ProtocolError(path, "invalid type spec")
            }
            if (types.none { typeOk(value, it) }) {
                throw ProtocolError(path, "expected ${types.joinToString("/")}")
            }
        }
        if (schema.has("const")) {
            val expected = schema.get("const")
            if (!valuesEqual(value, expected)) {
                throw ProtocolError(path, "must be $expected")
            }
        }
        if (schema.has("enum")) {
            val arr = schema.getJSONArray("enum")
            val allowed = (0 until arr.length()).map { arr.get(it) }
            if (allowed.none { valuesEqual(value, it) }) {
                throw ProtocolError(path, "not an allowed value")
            }
        }
        if (isNull(value)) return

        if (isNumber(value)) {
            val num = (value as Number).toDouble()
            if (schema.has("minimum")) {
                val min = schema.getDouble("minimum")
                if (num < min) {
                    throw ProtocolError(path, "below minimum $min")
                }
            }
        }
        if (value is String) {
            val count = value.length
            if (schema.has("minLength") && count < schema.getInt("minLength")) {
                throw ProtocolError(path, "too short")
            }
            if (schema.has("maxLength") && count > schema.getInt("maxLength")) {
                throw ProtocolError(path, "too long")
            }
            if (schema.has("pattern")) {
                val regex = Regex(schema.getString("pattern"))
                if (!regex.matches(value)) {
                    throw ProtocolError(path, "does not match pattern")
                }
            }
        }
        if (value is JSONArray) {
            if (schema.has("maxItems") && value.length() > schema.getInt("maxItems")) {
                throw ProtocolError(path, "too many items")
            }
            if (schema.has("items")) {
                val itemSchema = schema.getJSONObject("items")
                for (i in 0 until value.length()) {
                    validate(value.get(i), itemSchema, resolveRef, "$path[$i]")
                }
            }
        }
        if (value is JSONObject) {
            val props = schema.optJSONObject("properties") ?: JSONObject()
            if (schema.has("required")) {
                val req = schema.getJSONArray("required")
                for (i in 0 until req.length()) {
                    val k = req.getString(i)
                    if (!value.has(k)) {
                        throw ProtocolError(join(path, k), "required")
                    }
                }
            }
            if (schema.optBoolean("additionalProperties", true) == false) {
                val allowedKeys = props.keys().asSequence().toSet()
                for (k in value.keys().asSequence().sorted()) {
                    if (k !in allowedKeys) {
                        throw ProtocolError(join(path, k), "not allowed")
                    }
                }
            }
            for (k in props.keys().asSequence().sorted()) {
                if (value.has(k)) {
                    validate(value.get(k), props.getJSONObject(k), resolveRef, join(path, k))
                }
            }
        }
    }

    fun validateEnvelope(
        msg: JSONObject,
        validStates: Set<String>? = null
    ): ProtocolError? {
        return try {
            val envSchema = catalog.getEnvelopeSchema()
            validate(msg, envSchema, { catalog.getPayloadSchema(it) }, "")
            val type = msg.getString("type")
            val pSchema = catalog.getPayloadSchema(type)
            validate(msg.getJSONObject("payload"), pSchema, { catalog.getPayloadSchema(it) }, "payload")

            if (type == "state.event" && validStates != null) {
                val payload = msg.getJSONObject("payload")
                val stateId = payload.getString("state_id")
                if (!validStates.contains(stateId)) {
                    return ProtocolError("payload.state_id", "unknown state '$stateId'")
                }
            }
            null
        } catch (e: ProtocolError) {
            e
        } catch (e: Exception) {
            ProtocolError("", e.message ?: "unknown validation error")
        }
    }

    fun validateMessage(
        rawJson: String,
        envelopeSchema: JSONObject = catalog.getEnvelopeSchema(),
        resolvePayloadSchema: (String) -> JSONObject = { catalog.getPayloadSchema(it) }
    ): JSONObject {
        val msg = try {
            JSONObject(rawJson)
        } catch (e: JSONException) {
            throw ProtocolError("", "not JSON: ${e.message}")
        }
        validate(msg, envelopeSchema, resolvePayloadSchema, "")
        val type = msg.getString("type")
        validate(msg.getJSONObject("payload"), resolvePayloadSchema(type), resolvePayloadSchema, "payload")
        return msg
    }

    internal const val EMBEDDED_ENVELOPE_SCHEMA = """{
  "${'$'}schema": "https://json-schema.org/draft/2020-12/schema",
  "${'$'}id": "fox-protocol/envelope.schema.json",
  "title": "FoxLink v1 envelope",
  "type": "object",
  "additionalProperties": false,
  "required": ["v", "type", "id", "epoch", "seq", "ts", "payload"],
  "properties": {
    "v": { "type": "integer", "const": 1 },
    "type": { "type": "string", "enum": ["hello", "task.update", "task.list", "state.event"] },
    "id": { "type": "string", "pattern": "^msg_[A-Za-z0-9]{6,32}${'$'}" },
    "epoch": { "type": "integer", "minimum": 0 },
    "seq": { "type": "integer", "minimum": 0 },
    "ts": { "type": "integer", "minimum": 0 },
    "payload": { "type": "object" }
  }
}"""

    internal val EMBEDDED_PAYLOADS = mapOf(
        "hello.json" to """{
  "title": "hello",
  "type": "object",
  "additionalProperties": false,
  "required": ["role", "device_name", "app_version", "protocol"],
  "properties": {
    "role": { "type": "string", "enum": ["desk", "mobile"] },
    "device_name": { "type": "string", "minLength": 1, "maxLength": 64 },
    "app_version": { "type": "string", "pattern": "^[0-9]+\\.[0-9]+\\.[0-9]+${'$'}" },
    "protocol": { "type": "integer", "const": 1 }
  }
}""",
        "task.json" to """{
  "title": "task",
  "type": "object",
  "additionalProperties": false,
  "required": ["task_id", "project", "branch", "active_file", "summary", "updated_at", "uncommitted_count"],
  "properties": {
    "task_id": { "type": "string", "pattern": "^tsk_[A-Za-z0-9_-]{1,64}${'$'}" },
    "project": { "type": "string", "minLength": 1, "maxLength": 128 },
    "branch": { "type": ["string", "null"], "maxLength": 128 },
    "active_file": { "type": ["string", "null"], "maxLength": 256 },
    "summary": { "type": "string", "minLength": 1, "maxLength": 280 },
    "updated_at": { "type": "integer", "minimum": 0 },
    "uncommitted_count": { "type": ["integer", "null"], "minimum": 0 }
  }
}""",
        "task.update.json" to """{
  "title": "task.update",
  "type": "object",
  "additionalProperties": false,
  "required": ["task"],
  "properties": {
    "task": { "${'$'}ref": "task.json" }
  }
}""",
        "task.list.json" to """{
  "title": "task.list",
  "type": "object",
  "additionalProperties": false,
  "required": ["tasks"],
  "properties": {
    "tasks": { "type": "array", "maxItems": 20, "items": { "${'$'}ref": "task.json" } }
  }
}""",
        "state.event.json" to """{
  "title": "state.event",
  "type": "object",
  "additionalProperties": false,
  "required": ["state_id"],
  "properties": {
    "state_id": { "type": "string", "pattern": "^[a-z0-9_]{2,32}${'$'}" },
    "detail": { "type": ["string", "null"], "maxLength": 280 }
  }
}"""
    )
}

class ReplayGuard(
    val maxSkewMs: Long = 60_000L,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    var epoch: String? = null
    var lastSeq: Long = -1L

    /**
     * Checks if a message with (epoch, seq, ts) is fresh and in-sequence.
     * Returns null if valid, or a ProtocolError (path="seq" or "ts") if rejected.
     */
    fun check(
        epoch: String,
        seq: Long,
        ts: Long,
        nowMs: Long = clock()
    ): ProtocolError? {
        if (abs(nowMs - ts) > maxSkewMs) {
            return ProtocolError("ts", "timestamp skew exceeds ${maxSkewMs}ms (diff=${abs(nowMs - ts)}ms)")
        }
        if (this.epoch == null || this.epoch != epoch) {
            this.epoch = epoch
            this.lastSeq = seq
            return null
        }
        if (seq <= lastSeq) {
            return ProtocolError("seq", "duplicate or out-of-order sequence: $seq <= $lastSeq")
        }
        this.lastSeq = seq
        return null
    }

    /**
     * Checks a raw JSONObject envelope, returning null if fresh or a state_id ("skew" | "replay") if rejected.
     */
    fun check(msg: JSONObject): String? {
        val ts = msg.optLong("ts", -1L)
        val epochVal = if (msg.opt("epoch") is Number) msg.getLong("epoch").toString() else msg.optString("epoch", "")
        val seq = msg.optLong("seq", -1L)
        val err = check(epochVal, seq, ts)
        return when (err?.path) {
            "ts" -> "skew"
            "seq" -> "replay"
            else -> null
        }
    }
}
