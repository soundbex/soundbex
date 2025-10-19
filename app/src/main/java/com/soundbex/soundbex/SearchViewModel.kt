package com.soundbex.soundbex

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

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

    fun searchSongs(query: String) {
        if (query.isBlank()) {
            _results.value = emptyList()
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
                } else {
                    _results.value = emptyList()
                }

            } catch (e: Exception) {
                _results.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearResults() {
        _results.value = emptyList()
    }
}