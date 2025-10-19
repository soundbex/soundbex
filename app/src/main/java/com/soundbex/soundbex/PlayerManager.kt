package com.soundbex.soundbex

import android.content.Context
import android.util.Log
import androidx.compose.runtime.*
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

@UnstableApi
class PlayerManager(private val context: Context) {
    val player: ExoPlayer = ExoPlayer.Builder(context).build()

    var currentVideoId by mutableStateOf<String?>(null)
    var currentSongTitle by mutableStateOf("")
    var currentSongArtist by mutableStateOf("")
    var isPlayingState by mutableStateOf(false)
    var isLoadingState by mutableStateOf(false)

    private val TAG = "SoundBexPlayer"
    private val BACKEND_URL = "http://10.0.2.2:3000"
    private val client = OkHttpClient()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    init {
        player.playWhenReady = true
        addPlayerListener()
    }

    private fun addPlayerListener() {
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> Log.d(TAG, "Buffering...")
                    Player.STATE_READY -> {
                        Log.d(TAG, "Ready to play")
                        isPlayingState = player.isPlaying
                    }
                    Player.STATE_ENDED -> {
                        Log.d(TAG, "Playback ended")
                        isPlayingState = false
                    }
                    Player.STATE_IDLE -> Log.d(TAG, "Idle")
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                isPlayingState = isPlaying
                Log.d(TAG, "Playing state: $isPlaying")
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Log.e(TAG, "Player Error: ${error.message}")
                isPlayingState = false
                Log.e(TAG, "Müzik çalınamadı, lütfen başka bir şarkı deneyin")
            }
        })
    }

    fun playSong(videoId: String, title: String = "", artist: String = "") {
        currentVideoId = videoId
        currentSongTitle = title
        currentSongArtist = artist
        isLoadingState = true

        coroutineScope.launch {
            try {
                val streamData = getStreamFromBackend(videoId)

                withContext(Dispatchers.Main) {
                    try {
                        val mediaItem = MediaItem.fromUri(streamData.url)
                        player.setMediaItem(mediaItem)
                        player.prepare()
                        player.play()
                        isPlayingState = true
                        Log.d(TAG, "Uygulamada çalınıyor: $title - Type: ${streamData.type}")
                    } catch (exoError: Exception) {
                        Log.e(TAG, "ExoPlayer error: ${exoError.message}")
                        isPlayingState = false
                    } finally {
                        isLoadingState = false
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Play error: ${e.message}")
                isPlayingState = false
                isLoadingState = false
            }
        }
    }

    private suspend fun getStreamFromBackend(videoId: String): StreamData {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("$BACKEND_URL/api/stream?videoId=$videoId")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Backend error: ${response.code}")
                }

                val responseBody = response.body?.string()
                val json = JSONObject(responseBody ?: "")

                if (!json.getBoolean("success")) {
                    throw IOException("Backend failed: ${json.optString("error", "Unknown error")}")
                }

                val streamUrl = json.getString("streamUrl")
                val type = json.optString("type", "unknown")
                val source = json.optString("source", "")

                Log.d(TAG, "Stream alındı - Type: $type, Source: $source")

                StreamData(streamUrl, type, 0, "", source)
            }
        }
    }

    fun play() {
        try {
            player.play()
            isPlayingState = true
        } catch (e: Exception) {
            Log.e(TAG, "Play error: ${e.message}")
        }
    }

    fun pause() {
        try {
            player.pause()
            isPlayingState = false
        } catch (e: Exception) {
            Log.e(TAG, "Pause error: ${e.message}")
        }
    }

    fun stop() {
        try {
            player.stop()
            isPlayingState = false
        } catch (e: Exception) {
            Log.e(TAG, "Stop error: ${e.message}")
        }
    }

    fun seekTo(position: Long) {
        try {
            player.seekTo(position)
        } catch (e: Exception) {
            Log.e(TAG, "Seek error: ${e.message}")
        }
    }

    fun getCurrentPosition(): Long {
        return try {
            player.currentPosition
        } catch (e: Exception) {
            0L
        }
    }

    fun getDuration(): Long {
        return try {
            player.duration
        } catch (e: Exception) {
            0L
        }
    }

    fun releasePlayer() {
        try {
            coroutineScope.cancel()
            player.release()
        } catch (e: Exception) {
            Log.e(TAG, "Release error: ${e.message}")
        }
    }

    data class StreamData(
        val url: String,
        val type: String,
        val bitrate: Int = 0,
        val format: String = "",
        val source: String = ""
    )
}