package com.example.lyricscompanion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.net.HttpURLConnection
import java.net.URL
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.provider.Settings
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    @Volatile
    private var monitoring = false

    private var monitorThread: Thread? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        fun inspectMediaSessions(): String {
            return try {
                val manager = getSystemService(Context.MEDIA_SESSION_SERVICE)
                        as MediaSessionManager

                val listener = ComponentName(
                    this,
                    LyricsNotificationListener::class.java
                )

                val sessions = manager.getActiveSessions(listener)

                if (sessions.isEmpty()) {
                    return "No active media sessions found. Start music, then inspect again."
                }

                sessions.joinToString(separator = "\n\n") { controller ->
                    val metadata = controller.metadata
                    val playback = controller.playbackState

                    val stateText = when (playback?.state) {
                        PlaybackState.STATE_PLAYING -> "PLAYING"
                        PlaybackState.STATE_PAUSED -> "PAUSED"
                        PlaybackState.STATE_BUFFERING -> "BUFFERING"
                        PlaybackState.STATE_STOPPED -> "STOPPED"
                        null -> "(not provided)"
                        else -> "Other state (${playback.state})"
                    }

                    """
            App: ${controller.packageName}
            Title: ${metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "(not provided)"}
            Artist: ${metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "(not provided)"}
            Playback state: $stateText
            Position: ${playback?.position?.let { "$it ms" } ?: "(not provided)"}
            """.trimIndent()
                }
            } catch (error: SecurityException) {
                "Notification access is not enabled for Lyrics Display yet."
            } catch (error: Exception) {
                "Could not inspect sessions: ${error.message}"
            }
        }
        super.onCreate(savedInstanceState)

        setContent {
            var status by remember { mutableStateOf("Ready to send test data") }

            var sessionReport by remember {
                mutableStateOf("Media session results will appear here.")
            }

            MaterialTheme {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = sessionReport,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    Text(status, modifier = Modifier.padding(top = 16.dp))
                    Button(
                        modifier = Modifier.padding(top = 16.dp),
                        onClick = {
                            status = "Sending…"

                            Thread {
                                val result = sendTestMessage()

                                runOnUiThread {
                                    status = result
                                }
                            }.start()
                        }
                    ) {
                        Text("Send test data")
                    }
                    Button(
                        modifier = Modifier.padding(top = 16.dp),
                        onClick = {
                            startActivity(
                                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            )
                        }
                    ) {
                        Text("Open notification access settings")
                    }

                    Button(
                        modifier = Modifier.padding(top = 8.dp),
                        onClick = {
                            sessionReport = inspectMediaSessions()
                        }
                    ) {
                        Text("Inspect media sessions")
                    }
                    var isMonitoring by remember { mutableStateOf(false) }

                    Button(
                        modifier = Modifier.padding(top = 8.dp),
                        onClick = {
                            if (isMonitoring) {
                                stopSessionUpdates()
                                isMonitoring = false
                                status = "Automatic updates stopped"
                            } else {
                                isMonitoring = true
                                status = "Starting automatic updates…"
                                startSessionUpdates { result ->
                                    status = result
                                }
                            }
                        }
                    ) {
                        Text(if (isMonitoring) "Stop automatic updates" else "Start automatic updates")
                    }
                    Button(
                        modifier = Modifier.padding(top = 8.dp),
                        onClick = {
                            status = "Sending current session…"

                            Thread {
                                val result = sendCurrentSession()

                                runOnUiThread {
                                    status = result
                                }
                            }.start()
                        }
                    ) {
                        Text("Send current session")

                    }

                }
            }
        }
    }
    override fun onDestroy() {
        stopSessionUpdates()
        super.onDestroy()
    }
    private fun sendTestMessage(): String {
        val json = """
            {
              "title": "Shape of you",
              "artist": "Ed Sheeran",
              "position_ms": 8500,
              "is_playing": true
            }
        """.trimIndent()

        return sendJsonMessage(json)
    }

    private fun sendCurrentSession(): String {
        return try {
            val manager = getSystemService(Context.MEDIA_SESSION_SERVICE)
                    as MediaSessionManager

            val listener = ComponentName(this, LyricsNotificationListener::class.java)
            val controller = manager.getActiveSessions(listener)
                .firstOrNull {
                    it.packageName == "app.revanced.android.apps.youtube.music"
                }
                ?: return "No YouTube Music session found. Inspect sessions first."

            val metadata = controller.metadata
                ?: return "YouTube Music has not provided track metadata."

            val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE)
                ?: return "YouTube Music did not provide a title."

            val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: return "YouTube Music did not provide an artist."

            val playback = controller.playbackState
                ?: return "YouTube Music did not provide playback state."

            val body = JSONObject()
                .put("title", title)
                .put("artist", artist)
                .put("position_ms", playback.position.coerceAtLeast(0L))
                .put("is_playing", playback.state == PlaybackState.STATE_PLAYING)
                .toString()

            sendJsonMessage(body)
        } catch (error: SecurityException) {
            "Notification access is not enabled for Lyrics Display."
        } catch (error: Exception) {
            "Could not send session: ${error.message}"
        }
    }
    private fun startSessionUpdates(onStatus: (String) -> Unit) {
        if (monitoring) return

        monitoring = true
        monitorThread = Thread {
            while (monitoring) {
                val result = sendCurrentSession()

                runOnUiThread {
                    if (monitoring) {
                        onStatus(result)
                    }
                }

                try {
                    Thread.sleep(1_000)
                } catch (_: InterruptedException) {
                    break
                }
            }
        }.also { it.start() }
    }

    private fun stopSessionUpdates() {
        monitoring = false
        monitorThread?.interrupt()
        monitorThread = null
    }
    private fun sendJsonMessage(json: String): String {
        val endpoint ="https://cubbyhole-tried-perennial.ngrok-free.dev/now-playing"

        return try {
            val connection = URL(endpoint).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")

            connection.outputStream.use { output ->
                output.write(json.toByteArray(Charsets.UTF_8))
            }

            val code = connection.responseCode
            connection.disconnect()
            "Server response: HTTP $code"
        } catch (error: Exception) {
            "Could not send: ${error.message}"
        }
    }
}