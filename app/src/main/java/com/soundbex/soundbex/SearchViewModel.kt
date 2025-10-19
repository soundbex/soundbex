package com.soundbex.soundbex

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody

data class SearchResultSong(
    val title: String,
    val artist: String,
    val imageUrl: String?,
    val videoId: String,
    val duration: String = "0:00"
)

class SearchViewModel : ViewModel() {

    private val client = OkHttpClient()
    private val BACKEND_URL = "http://10.0.2.2:3000"

    private val _results = MutableStateFlow<List<SearchResultSong>>(emptyList())
    val results: StateFlow<List<SearchResultSong>> = _results

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _playlistId = MutableStateFlow<String?>(null)
    val playlistId: StateFlow<String?> = _playlistId

    fun searchSongs(query: String) {
        if (query.isBlank()) {
            _results.value = emptyList()
            _playlistId.value = null
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val request = Request.Builder()
                    .url("$BACKEND_URL/api/search?q=${query.trim()}")
                    .get()
                    .build()

                val json = withContext(Dispatchers.IO) {
                    client.newCall(request).execute().use { response ->
                        response.body?.string()
                    }
                }

                if (json != null) {
                    val parsed = JsonParser.parseString(json).asJsonObject
                    val tracks = parsed["result"]?.asJsonArray ?: emptyList()

                    val list = tracks.mapNotNull { element ->
                        try {
                            val data = element.asJsonObject
                            SearchResultSong(
                                title = data["title"]?.asString ?: "Bilinmeyen Şarkı",
                                artist = data["author"]?.asString ?: "Bilinmeyen Sanatçı",
                                imageUrl = data["thumbnail"]?.asString,
                                videoId = data["videoId"]?.asString ?: return@mapNotNull null,
                                duration = data["duration"]?.asString ?: "0:00"
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }

                    _results.value = list

                    if (list.isNotEmpty()) {
                        createPlaylist(list)
                    }
                } else {
                    _results.value = emptyList()
                    _playlistId.value = null
                }

            } catch (e: Exception) {
                _results.value = emptyList()
                _playlistId.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun createPlaylist(songs: List<SearchResultSong>) {
        try {
            val playlistSongs = songs.map { song ->
                mapOf(
                    "title" to song.title,
                    "artist" to song.artist,
                    "duration" to song.duration,
                    "imageUrl" to song.imageUrl,
                    "videoId" to song.videoId
                )
            }

            val jsonBody = JsonParser.parseString(
                """{"songs": ${
                    com.google.gson.Gson().toJson(playlistSongs)
                }}"""
            ).asJsonObject

            val requestBody = RequestBody.create(
                "application/json".toMediaTypeOrNull(),
                jsonBody.toString()
            )

            val request = Request.Builder()
                .url("$BACKEND_URL/api/playlist")
                .post(requestBody)
                .build()

            val response = withContext(Dispatchers.IO) {
                client.newCall(request).execute().use { response ->
                    response.body?.string()
                }
            }

            if (response != null) {
                val jsonResponse = JsonParser.parseString(response).asJsonObject
                if (jsonResponse["success"].asBoolean) {
                    _playlistId.value = jsonResponse["playlistId"].asString
                }
            }
        } catch (e: Exception) {
            _playlistId.value = null
        }
    }

    suspend fun getNextSong(playlistId: String): NextPreviousResult? {
        return try {
            val request = Request.Builder()
                .url("$BACKEND_URL/api/playlist/$playlistId/next")
                .get()
                .build()

            val response = withContext(Dispatchers.IO) {
                client.newCall(request).execute().use { response ->
                    response.body?.string()
                }
            }

            if (response != null) {
                val json = JsonParser.parseString(response).asJsonObject
                if (json["success"].asBoolean) {
                    val songData = json["song"].asJsonObject
                    NextPreviousResult(
                        song = SearchResultSong(
                            title = songData["title"].asString,
                            artist = songData["artist"].asString,
                            imageUrl = songData["imageUrl"]?.asString,
                            videoId = songData["videoId"].asString,
                            duration = songData["duration"].asString
                        ),
                        currentIndex = json["currentIndex"].asInt,
                        totalSongs = json["totalSongs"].asInt,
                        hasNext = json["hasNext"].asBoolean,
                        hasPrevious = json["hasPrevious"].asBoolean
                    )
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getPreviousSong(playlistId: String): NextPreviousResult? {
        return try {
            val request = Request.Builder()
                .url("$BACKEND_URL/api/playlist/$playlistId/previous")
                .get()
                .build()

            val response = withContext(Dispatchers.IO) {
                client.newCall(request).execute().use { response ->
                    response.body?.string()
                }
            }

            if (response != null) {
                val json = JsonParser.parseString(response).asJsonObject
                if (json["success"].asBoolean) {
                    val songData = json["song"].asJsonObject
                    NextPreviousResult(
                        song = SearchResultSong(
                            title = songData["title"].asString,
                            artist = songData["artist"].asString,
                            imageUrl = songData["imageUrl"]?.asString,
                            videoId = songData["videoId"].asString,
                            duration = songData["duration"].asString
                        ),
                        currentIndex = json["currentIndex"].asInt,
                        totalSongs = json["totalSongs"].asInt,
                        hasNext = json["hasNext"].asBoolean,
                        hasPrevious = json["hasPrevious"].asBoolean
                    )
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getCurrentSong(playlistId: String): NextPreviousResult? {
        return try {
            val request = Request.Builder()
                .url("$BACKEND_URL/api/playlist/$playlistId/current")
                .get()
                .build()

            val response = withContext(Dispatchers.IO) {
                client.newCall(request).execute().use { response ->
                    response.body?.string()
                }
            }

            if (response != null) {
                val json = JsonParser.parseString(response).asJsonObject
                if (json["success"].asBoolean) {
                    val songData = json["song"].asJsonObject
                    NextPreviousResult(
                        song = SearchResultSong(
                            title = songData["title"].asString,
                            artist = songData["artist"].asString,
                            imageUrl = songData["imageUrl"]?.asString,
                            videoId = songData["videoId"].asString,
                            duration = songData["duration"].asString
                        ),
                        currentIndex = json["currentIndex"].asInt,
                        totalSongs = json["totalSongs"].asInt,
                        hasNext = json["hasNext"].asBoolean,
                        hasPrevious = json["hasPrevious"].asBoolean
                    )
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getSongDetails(videoId: String): SearchResultSong? {
        return try {
            val request = Request.Builder()
                .url("$BACKEND_URL/api/song/$videoId")
                .get()
                .build()

            val response = withContext(Dispatchers.IO) {
                client.newCall(request).execute().use { response ->
                    response.body?.string()
                }
            }

            if (response != null) {
                val json = JsonParser.parseString(response).asJsonObject
                if (json["success"].asBoolean) {
                    val songData = json["song"].asJsonObject
                    SearchResultSong(
                        title = songData["title"].asString,
                        artist = songData["artist"].asString,
                        imageUrl = songData["thumbnail"]?.asString,
                        videoId = videoId,
                        duration = songData["duration"].asString
                    )
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun clearResults() {
        _results.value = emptyList()
        _playlistId.value = null
    }

    data class NextPreviousResult(
        val song: SearchResultSong,
        val currentIndex: Int,
        val totalSongs: Int,
        val hasNext: Boolean,
        val hasPrevious: Boolean
    )
}