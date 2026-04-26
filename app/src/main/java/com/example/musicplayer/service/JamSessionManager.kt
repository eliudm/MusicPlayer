package com.example.musicplayer.service

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.example.musicplayer.di.ApplicationScope
import com.example.musicplayer.domain.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

data class JamSession(val name: String, val host: String, val port: Int)

data class JamState(
    val isHosting: Boolean = false,
    val sessionName: String = "",
    val connectedPeers: Int = 0,
    val isSearching: Boolean = false,
    val availableSessions: List<JamSession> = emptyList(),
    val joinedSession: JamSession? = null
)

data class RemoteSong(val id: Long, val title: String, val artist: String, val album: String)

@Singleton
class JamSessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val appScope: CoroutineScope
) {
    private val _state = MutableStateFlow(JamState())
    val state: StateFlow<JamState> = _state.asStateFlow()

    private val _songAddEvents = MutableSharedFlow<Long>(extraBufferCapacity = 32)
    val songAddEvents: SharedFlow<Long> = _songAddEvents.asSharedFlow()

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var serverSocket: ServerSocket? = null
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    @Volatile private var hostSongs: List<Song> = emptyList()

    // ── Host ─────────────────────────────────────────────────────────────────

    fun startHosting(sessionName: String, songs: List<Song>) {
        if (_state.value.isHosting) return
        hostSongs = songs

        val socket = ServerSocket(0).also { serverSocket = it }
        val port = socket.localPort
        _state.update { it.copy(isHosting = true, sessionName = sessionName, connectedPeers = 0) }

        Thread {
            while (!socket.isClosed) {
                try {
                    val client = socket.accept()
                    Thread { handleRequest(client) }.apply { isDaemon = true }.start()
                } catch (e: Exception) {
                    if (!socket.isClosed) Log.w(TAG, "accept: ${e.message}")
                }
            }
        }.apply { isDaemon = true; start() }

        val info = NsdServiceInfo().apply {
            this.serviceName = sessionName
            this.serviceType = SERVICE_TYPE
            this.port = port
        }
        registrationListener = object : NsdManager.RegistrationListener {
            override fun onRegistrationFailed(si: NsdServiceInfo, code: Int) {
                Log.w(TAG, "NSD register failed $code")
            }
            override fun onUnregistrationFailed(si: NsdServiceInfo, code: Int) {}
            override fun onServiceRegistered(si: NsdServiceInfo) {
                Log.d(TAG, "registered: ${si.serviceName}")
            }
            override fun onServiceUnregistered(si: NsdServiceInfo) {}
        }.also { nsdManager.registerService(info, NsdManager.PROTOCOL_DNS_SD, it) }
    }

    fun updateHostSongs(songs: List<Song>) { hostSongs = songs }

    fun stopHosting() {
        registrationListener?.let { runCatching { nsdManager.unregisterService(it) } }
        registrationListener = null
        serverSocket?.close()
        serverSocket = null
        _state.update { it.copy(isHosting = false, connectedPeers = 0) }
    }

    // ── Client ────────────────────────────────────────────────────────────────

    fun startDiscovery() {
        if (_state.value.isSearching) return
        _state.update { it.copy(isSearching = true, availableSessions = emptyList()) }
        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(st: String, code: Int) {
                _state.update { it.copy(isSearching = false) }
            }
            override fun onStopDiscoveryFailed(st: String, code: Int) {}
            override fun onDiscoveryStarted(st: String) {}
            override fun onDiscoveryStopped(st: String) {
                _state.update { it.copy(isSearching = false) }
            }
            override fun onServiceFound(si: NsdServiceInfo) { resolveService(si) }
            override fun onServiceLost(si: NsdServiceInfo) {
                _state.update { s ->
                    s.copy(availableSessions = s.availableSessions.filterNot { it.name == si.serviceName })
                }
            }
        }.also { nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, it) }
    }

    fun stopDiscovery() {
        discoveryListener?.let { runCatching { nsdManager.stopServiceDiscovery(it) } }
        discoveryListener = null
        _state.update { it.copy(isSearching = false) }
    }

    fun leaveSession() = _state.update { it.copy(joinedSession = null) }

    private fun resolveService(si: NsdServiceInfo) {
        nsdManager.resolveService(si, object : NsdManager.ResolveListener {
            override fun onResolveFailed(s: NsdServiceInfo, code: Int) {
                Log.w(TAG, "resolve failed $code for ${s.serviceName}")
            }
            override fun onServiceResolved(s: NsdServiceInfo) {
                val host = s.host?.hostAddress ?: return
                val session = JamSession(s.serviceName, host, s.port)
                _state.update { st ->
                    val others = st.availableSessions.filterNot { it.name == session.name }
                    st.copy(availableSessions = others + session)
                }
            }
        })
    }

    suspend fun fetchHostSongs(session: JamSession): List<RemoteSong> =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                val body = httpGet("http://${session.host}:${session.port}/songs")
                val arr = JSONArray(body)
                (0 until arr.length()).map { i ->
                    arr.getJSONObject(i).let { o ->
                        RemoteSong(o.getLong("id"), o.getString("title"),
                            o.getString("artist"), o.getString("album"))
                    }
                }
            }.getOrElse { Log.w(TAG, "fetchSongs: ${it.message}"); emptyList() }
        }

    suspend fun addSongToHost(session: JamSession, songId: Long): Boolean =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                val body = httpGet("http://${session.host}:${session.port}/add?id=$songId")
                val ok = JSONObject(body).optBoolean("success", false)
                if (ok) _state.update { it.copy(joinedSession = session) }
                ok
            }.getOrElse { Log.w(TAG, "addSong: ${it.message}"); false }
        }

    // ── HTTP helpers ──────────────────────────────────────────────────────────

    private fun httpGet(url: String): String {
        val uri = URI(url)
        val path = if (uri.rawQuery != null) "${uri.path}?${uri.rawQuery}" else uri.path
        Socket().use { socket ->
            socket.connect(InetSocketAddress(uri.host, uri.port), 5_000)
            PrintWriter(socket.getOutputStream(), true).apply {
                println("GET $path HTTP/1.0")
                println("Host: ${uri.host}")
                println()
                flush()
            }
            val lines = socket.getInputStream().bufferedReader().readLines()
            val start = lines.indexOfFirst { it.isBlank() }
            return if (start >= 0) lines.drop(start + 1).joinToString("\n") else ""
        }
    }

    private fun handleRequest(socket: Socket) {
        try {
            _state.update { it.copy(connectedPeers = it.connectedPeers + 1) }
            val line = socket.getInputStream().bufferedReader().readLine() ?: return
            val path = line.split(" ").getOrNull(1) ?: return
            val writer = PrintWriter(socket.getOutputStream(), true)
            when {
                path == "/songs" -> respond(writer, 200, buildSongsJson(hostSongs))
                path.startsWith("/add") -> {
                    val id = path.substringAfter("id=", "").toLongOrNull()
                    if (id != null) {
                        appScope.launch { _songAddEvents.emit(id) }
                        respond(writer, 200, """{"success":true}""")
                    } else {
                        respond(writer, 400, """{"error":"bad id"}""")
                    }
                }
                else -> respond(writer, 404, "Not Found")
            }
        } catch (e: Exception) {
            Log.w(TAG, "handleRequest: ${e.message}")
        } finally {
            _state.update { it.copy(connectedPeers = (it.connectedPeers - 1).coerceAtLeast(0)) }
            socket.close()
        }
    }

    private fun respond(writer: PrintWriter, code: Int, body: String) {
        val status = when (code) { 200 -> "OK"; 400 -> "Bad Request"; else -> "Not Found" }
        writer.println("HTTP/1.0 $code $status")
        writer.println("Content-Type: application/json")
        writer.println("Content-Length: ${body.toByteArray().size}")
        writer.println()
        writer.print(body)
        writer.flush()
    }

    private fun buildSongsJson(songs: List<Song>): String = JSONArray().apply {
        songs.forEach { s ->
            put(JSONObject().apply {
                put("id", s.id); put("title", s.title)
                put("artist", s.artist); put("album", s.album)
            })
        }
    }.toString()

    companion object {
        private const val TAG = "JamSession"
        private const val SERVICE_TYPE = "_musicjam._tcp."
    }
}
