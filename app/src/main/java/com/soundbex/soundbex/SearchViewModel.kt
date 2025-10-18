package com.soundbex.soundbex

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

data class SearchResultSong(
    val title: String,
    val artist: String,
    val imageUrl: String?,
    val videoId: String,
    val duration: String = "0:00"
)

class SearchViewModel : ViewModel() {

    private val client = OkHttpClient()
    private val BACKEND_URL = "http://10.0.2.2:3000" // Python backend

    private val _results = MutableStateFlow<List<SearchResultSong>>(emptyList())
    val results: StateFlow<List<SearchResultSong>> = _results

    private val _errorChannel = MutableSharedFlow<String>()
    val errorFlow: SharedFlow<String> = _errorChannel.asSharedFlow()

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
                        if (!response.isSuccessful) {
                            val errorMessage = "Backend hatası: ${response.code}"
                            _errorChannel.emit(errorMessage)
                            return@use null
                        }
                        response.body?.string()
                    }
                } ?: return@launch

                val parsed = JsonParser.parseString(json).asJsonObject

                if (!parsed.get("success").asBoolean) {
                    val errorMsg = parsed.get("error")?.asString ?: "Bilinmeyen hata"
                    _errorChannel.emit(errorMsg)
                    return@launch
                }

                val tracks = parsed["result"]?.asJsonArray ?: run {
                    _errorChannel.emit("Arama sonuçları alınamadı")
                    return@launch
                }

                val list = tracks.mapNotNull { element ->
                    try {
                        val data = element.asJsonObject

                        val name = data["title"]?.asString ?: "Bilinmeyen Şarkı"
                        val artist = data["author"]?.asString ?: "Bilinmeyen Sanatçı"
                        val image = data["thumbnail"]?.asString
                        val videoId = data["videoId"]?.asString ?: return@mapNotNull null
                        val duration = data["duration"]?.asString ?: "0:00"

                        SearchResultSong(name, artist, image, videoId, duration)

                    } catch (e: Exception) {
                        null
                    }
                }

                _results.value = list

                if (list.isEmpty()) {
                    _errorChannel.emit("'$query' için sonuç bulunamadı")
                }

            } catch (e: IOException) {
                _errorChannel.emit("Backend bağlantı hatası. Sunucu: $BACKEND_URL")
                _results.value = emptyList()
            } catch (e: Exception) {
                _errorChannel.emit("Hata: ${e.localizedMessage}")
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