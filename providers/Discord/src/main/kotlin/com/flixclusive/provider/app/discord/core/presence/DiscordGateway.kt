package com.flixclusive.provider.app.discord.core.presence

import com.flixclusive.core.util.log.debugLog
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.core.util.log.warnLog
import com.flixclusive.provider.app.discord.core.config.DiscordConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

private const val OP_DISPATCH = 0
private const val OP_HEARTBEAT = 1
private const val OP_IDENTIFY = 2
private const val OP_PRESENCE_UPDATE = 3
private const val OP_RESUME = 6
private const val OP_RECONNECT = 7
private const val OP_INVALID_SESSION = 9
private const val OP_HELLO = 10
private const val OP_HEARTBEAT_ACK = 11

private const val CLOSE_RESTART = 4000
private const val HEARTBEAT_JITTER = 0.05

internal class DiscordGateway(
    private val client: OkHttpClient,
    private val scope: CoroutineScope,
    private val tokenProvider: suspend () -> String?,
    private val onAuthFailure: suspend () -> String?,
    private val onFatal: (String) -> Unit,
) {
    private val connectMutex = Mutex()
    private val socketIds = AtomicLong(0)
    private val lastAckAt = AtomicLong(0)
    private val sequence = AtomicInteger(0)

    @Volatile private var activeSocketId = 0L

    @Volatile private var webSocket: WebSocket? = null

    @Volatile private var sessionId: String? = null

    @Volatile private var resumeUrl: String? = null

    @Volatile private var isReady = false

    @Volatile private var closedByUser = false

    @Volatile private var pendingPresence: JSONObject? = null

    private var heartbeatJob: Job? = null
    private var reconnectJob: Job? = null
    private var attempts = 0

    suspend fun setPresence(payload: JSONObject) {
        pendingPresence = payload

        if (isReady) {
            send(JSONObject().put("op", OP_PRESENCE_UPDATE).put("d", payload))
        } else {
            connect()
        }
    }

    suspend fun close() {
        closedByUser = true
        connectMutex.withLock { teardown(bumpGeneration = true) }
    }

    private suspend fun connect(resume: Boolean = false) {
        connectMutex.withLock {
            if (webSocket != null) return

            val bearer = tokenProvider()
            if (bearer == null) {
                warnLog("Discord: cannot open the gateway without a token.")
                return
            }

            closedByUser = false
            openSocket(resume = resume, bearer = bearer)
        }
    }

    private fun openSocket(
        resume: Boolean,
        bearer: String,
    ) {
        val id = socketIds.incrementAndGet()
        activeSocketId = id
        lastAckAt.set(0)
        isReady = false

        val url = if (resume) resumeUrl ?: DiscordConfig.GATEWAY_URL else DiscordConfig.GATEWAY_URL
        val request = Request.Builder().url(url.withGatewayQuery()).build()

        webSocket = client.newWebSocket(request, listener(id, resume, bearer))
    }

    private fun listener(
        id: Long,
        resume: Boolean,
        bearer: String,
    ) = object : WebSocketListener() {
        override fun onMessage(
            webSocket: WebSocket,
            text: String,
        ) {
            if (id != activeSocketId) return

            runCatching { handleFrame(JSONObject(text), resume, bearer) }
                .onFailure { errorLog("Discord: malformed gateway frame — ${it.message}") }
        }

        override fun onClosed(
            webSocket: WebSocket,
            code: Int,
            reason: String,
        ) {
            if (id != activeSocketId) return
            handleDisconnect(code)
        }

        override fun onFailure(
            webSocket: WebSocket,
            t: Throwable,
            response: Response?,
        ) {
            if (id != activeSocketId) return
            errorLog("Discord: gateway failure — ${t.message}")
            handleDisconnect(response?.code ?: CLOSE_RESTART)
        }
    }

    private fun handleFrame(
        frame: JSONObject,
        resume: Boolean,
        bearer: String,
    ) {
        frame.optIntOrNull("s")?.let(sequence::set)

        when (frame.optInt("op", -1)) {
            OP_HELLO -> {
                val interval = frame.optJSONObject("d")?.optLong("heartbeat_interval", 41_250L) ?: 41_250L
                startHeartbeat(interval)

                val session = sessionId
                if (resume && session != null) {
                    sendAsync(resumeFrame(bearer, session, sequence.get()))
                } else {
                    sendAsync(identifyFrame(bearer))
                }
            }

            OP_HEARTBEAT -> sendAsync(heartbeatFrame())
            OP_HEARTBEAT_ACK -> lastAckAt.set(System.currentTimeMillis())
            OP_RECONNECT -> webSocket?.close(CLOSE_RESTART, "reconnect requested")

            OP_INVALID_SESSION -> {
                if (!frame.optBoolean("d", false)) sessionId = null
                webSocket?.close(CLOSE_RESTART, "invalid session")
            }

            OP_DISPATCH -> handleDispatch(frame)
        }
    }

    private fun handleDispatch(frame: JSONObject) {
        when (frame.optString("t")) {
            "READY" -> {
                val data = frame.optJSONObject("d")
                sessionId = data?.optString("session_id")?.takeIf(String::isNotBlank)
                resumeUrl = data?.optString("resume_gateway_url")?.takeIf(String::isNotBlank)
                onConnected()
            }

            "RESUMED" -> onConnected()
        }
    }

    private fun onConnected() {
        isReady = true
        attempts = 0
        debugLog("Discord: gateway ready.")

        pendingPresence?.let { payload ->
            sendAsync(JSONObject().put("op", OP_PRESENCE_UPDATE).put("d", payload))
        }
    }

    private fun startHeartbeat(intervalMs: Long) {
        heartbeatJob?.cancel()
        heartbeatJob =
            scope.launch {
                var lastSentAt = 0L

                while (isActive) {
                    delay(jitter(intervalMs))

                    if (lastSentAt != 0L && lastAckAt.get() < lastSentAt) {
                        warnLog("Discord: heartbeat timed out, restarting the gateway.")
                        webSocket?.close(CLOSE_RESTART, "heartbeat timeout")
                        break
                    }

                    lastSentAt = System.currentTimeMillis()
                    send(heartbeatFrame())
                }
            }
    }

    private fun handleDisconnect(closeCode: Int) {
        isReady = false
        heartbeatJob?.cancel()
        webSocket = null

        if (closedByUser) return

        val action =
            DiscordReconnectStrategy.decide(
                closeCode = closeCode,
                hadSession = sessionId != null,
                seq = sequence.get(),
                sessionId = sessionId,
            )

        if (action is ReconnectAction.SurfaceFatal) {
            onFatal("Discord refused the connection (close code $closeCode).")
            return
        }

        if (++attempts > DiscordReconnectStrategy.MAX_ATTEMPTS) {
            onFatal("Discord: giving up after ${DiscordReconnectStrategy.MAX_ATTEMPTS} reconnect attempts.")
            return
        }

        reconnectJob?.cancel()
        reconnectJob =
            scope.launch {
                delay(DiscordReconnectStrategy.backoffMillis(attempts))

                when (action) {
                    is ReconnectAction.Resume -> connect(resume = true)

                    is ReconnectAction.RefreshAndReIdentify -> {
                        sessionId = null
                        onAuthFailure()
                        connect()
                    }

                    else -> {
                        sessionId = null
                        connect()
                    }
                }
            }
    }

    private fun teardown(bumpGeneration: Boolean) {
        if (bumpGeneration) activeSocketId = socketIds.incrementAndGet()

        heartbeatJob?.cancel()
        reconnectJob?.cancel()
        heartbeatJob = null
        reconnectJob = null

        webSocket?.close(1000, "provider unloaded")
        webSocket = null

        isReady = false
        pendingPresence = null
        sessionId = null
    }

    private fun identifyFrame(bearer: String) =
        JSONObject()
            .put("op", OP_IDENTIFY)
            .put(
                "d",
                JSONObject()
                    .put("token", bearer)
                    .put(
                        "properties",
                        JSONObject()
                            .put("os", "android")
                            .put("browser", "Flixclusive")
                            .put("device", "Flixclusive"),
                    ).put("compress", false)
                    .put("intents", 0),
            )

    private fun resumeFrame(
        bearer: String,
        session: String,
        seq: Int,
    ) = JSONObject()
        .put("op", OP_RESUME)
        .put(
            "d",
            JSONObject()
                .put("token", bearer)
                .put("session_id", session)
                .put("seq", seq),
        )

    private fun heartbeatFrame() =
        JSONObject()
            .put("op", OP_HEARTBEAT)
            .put("d", sequence.get().takeIf { it > 0 } ?: JSONObject.NULL)

    private fun send(frame: JSONObject) {
        val sent = webSocket?.send(frame.toString()) ?: false
        if (!sent) warnLog("Discord: could not send op ${frame.optInt("op", -1)}, socket is not open.")
    }

    private fun sendAsync(frame: JSONObject) = send(frame)

    private fun jitter(intervalMs: Long): Long {
        val delta = (intervalMs * HEARTBEAT_JITTER).toLong()
        return intervalMs - delta + Random.nextLong(2 * delta + 1)
    }
}

private fun String.withGatewayQuery(): String = if (contains("?")) this else "$this?v=10&encoding=json"

private fun JSONObject.optIntOrNull(key: String): Int? = if (isNull(key)) null else optInt(key)
