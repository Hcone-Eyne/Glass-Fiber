package com.fox.mobile.net

import com.fox.mobile.protocol.FoxProtocolCatalog
import com.fox.mobile.protocol.FoxSchemaValidator
import com.fox.mobile.protocol.FoxTask
import com.fox.mobile.protocol.ProtocolError
import com.fox.mobile.protocol.ReplayGuard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.min
import kotlin.math.pow

/**
 * FoxLink v1 WebSocket Client (`com.fox.mobile.net`).
 *
 * Features:
 * 1. Strict `FoxSchemaValidator` validation on every inbound & outbound envelope.
 * 2. `ReplayGuard` sequence monotonicity and clock-skew filtering.
 * 3. Exponential backoff reconnect cycle: `reconnecting` -> `offline`.
 * 4. Coroutine-friendly StateFlow connection updates and clean lifecycle disposal.
 */
class FoxLinkClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build(),
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO + Job()),
    val replayGuard: ReplayGuard = ReplayGuard(maxSkewMs = 60_000L),
    val validStates: Set<String>? = null,
    val maxReconnectAttempts: Int = 5,
    val baseBackoffMs: Long = 1000L,
    val maxBackoffMs: Long = 16_000L
) {
    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        RECONNECTING,
        OFFLINE
    }

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var currentUrl: String? = null
    private var webSocket: WebSocket? = null
    private var reconnectJob: Job? = null
    private var reconnectAttempts = 0
    private var manualDisconnect = false

    var epoch: Long = System.currentTimeMillis()
    private val seqCounter = AtomicLong(0)

    // Callbacks
    var onTaskUpdate: ((FoxTask) -> Unit)? = null
    var onTaskList: ((List<FoxTask>) -> Unit)? = null
    var onStateEvent: ((stateId: String, detail: String?) -> Unit)? = null
    var onStateChanged: ((ConnectionState) -> Unit)? = null
    var onProtocolError: ((ProtocolError) -> Unit)? = null

    private fun setState(state: ConnectionState) {
        _connectionState.value = state
        onStateChanged?.invoke(state)
    }

    @Synchronized
    fun connect(url: String) {
        manualDisconnect = false
        currentUrl = url
        reconnectAttempts = 0
        reconnectJob?.cancel()
        setState(ConnectionState.CONNECTING)

        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = client.newWebSocket(request, createSocketListener())
    }

    @Synchronized
    fun disconnect() {
        manualDisconnect = true
        reconnectJob?.cancel()
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        setState(ConnectionState.DISCONNECTED)
    }

    private fun scheduleReconnect() {
        if (manualDisconnect) return
        val url = currentUrl ?: return

        if (reconnectAttempts >= maxReconnectAttempts) {
            setState(ConnectionState.OFFLINE)
            return
        }

        setState(ConnectionState.RECONNECTING)
        reconnectAttempts++

        val backoff = min(
            maxBackoffMs,
            (baseBackoffMs * (2.0.pow((reconnectAttempts - 1).toDouble()))).toLong()
        )

        reconnectJob?.cancel()
        reconnectJob = coroutineScope.launch {
            delay(backoff)
            if (!manualDisconnect) {
                val request = Request.Builder().url(url).build()
                webSocket = client.newWebSocket(request, createSocketListener())
            }
        }
    }

    private fun createSocketListener(): WebSocketListener {
        return object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                reconnectAttempts = 0
                setState(ConnectionState.CONNECTED)
                // Auto-send hello upon open
                sendHello()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleInboundMessage(text)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (!manualDisconnect) {
                    scheduleReconnect()
                } else {
                    setState(ConnectionState.DISCONNECTED)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (!manualDisconnect) {
                    scheduleReconnect()
                } else {
                    setState(ConnectionState.DISCONNECTED)
                }
            }
        }
    }

    fun handleInboundMessage(text: String): Boolean {
        val json = try {
            JSONObject(text)
        } catch (e: Exception) {
            onProtocolError?.invoke(ProtocolError("", "Invalid JSON: ${e.message}"))
            return false
        }

        // 1. Strict Schema Validation
        val err = FoxSchemaValidator.validateEnvelope(json, validStates)
        if (err != null) {
            onProtocolError?.invoke(err)
            return false
        }

        // 2. Replay Guard Check
        val msgEpoch = if (json.opt("epoch") is Number) json.getLong("epoch").toString() else json.optString("epoch", "")
        val seq = json.getLong("seq")
        val ts = json.getLong("ts")
        val replayErr = replayGuard.check(msgEpoch, seq, ts)
        if (replayErr != null) {
            onProtocolError?.invoke(replayErr)
            return false
        }

        // 3. Dispatch validated payload
        val type = json.getString("type")
        val payload = json.getJSONObject("payload")
        when (type) {
            "hello" -> {
                // Handshake acknowledged
            }
            "task.update" -> {
                val taskObj = payload.getJSONObject("task")
                val task = parseFoxTask(taskObj)
                onTaskUpdate?.invoke(task)
            }
            "task.list" -> {
                val tasksArr = payload.getJSONArray("tasks")
                val list = mutableListOf<FoxTask>()
                for (i in 0 until tasksArr.length()) {
                    list.add(parseFoxTask(tasksArr.getJSONObject(i)))
                }
                onTaskList?.invoke(list)
            }
            "state.event" -> {
                val stateId = payload.getString("state_id")
                val detail = if (payload.has("detail") && !payload.isNull("detail")) payload.getString("detail") else null
                onStateEvent?.invoke(stateId, detail)
            }
        }
        return true
    }

    private fun parseFoxTask(obj: JSONObject): FoxTask {
        val taskId = obj.getString("task_id")
        val project = obj.getString("project")
        val branch = if (obj.has("branch") && !obj.isNull("branch")) obj.getString("branch") else null
        val activeFile = if (obj.has("active_file") && !obj.isNull("active_file")) obj.getString("active_file") else null
        val summary = obj.getString("summary")
        val updatedAt = obj.getLong("updated_at")
        val uncommitted = if (obj.has("uncommitted_count") && !obj.isNull("uncommitted_count")) obj.getInt("uncommitted_count") else null
        return FoxTask(
            id = taskId,
            title = activeFile ?: project,
            source = "git",
            status = "active",
            ts = updatedAt,
            project = project,
            branch = branch,
            primaryFile = activeFile,
            summary = summary,
            pinned = false,
            uncommittedCount = uncommitted
        )
    }

    fun sendHello(
        deviceName: String = "Android Fox",
        appVersion: String = "0.1.0"
    ): Boolean {
        val payload = JSONObject().apply {
            put("role", "mobile")
            put("device_name", deviceName)
            put("app_version", appVersion)
            put("protocol", 1)
        }
        return send("hello", payload)
    }

    fun sendStateEvent(stateId: String, detail: String? = null): Boolean {
        val payload = JSONObject().apply {
            put("state_id", stateId)
            if (detail != null) {
                put("detail", detail)
            } else {
                put("detail", JSONObject.NULL)
            }
        }
        return send("state.event", payload)
    }

    fun send(type: String, payload: JSONObject): Boolean {
        val nextSeq = seqCounter.getAndIncrement()
        val envelope = JSONObject().apply {
            put("v", 1)
            put("type", type)
            put("id", "msg_${UUID.randomUUID().toString().replace("-", "").take(16)}")
            put("epoch", epoch)
            put("seq", nextSeq)
            put("ts", System.currentTimeMillis())
            put("payload", payload)
        }

        val err = FoxSchemaValidator.validateEnvelope(envelope, validStates)
        if (err != null) {
            onProtocolError?.invoke(err)
            return false
        }

        return webSocket?.send(envelope.toString()) ?: false
    }
}
