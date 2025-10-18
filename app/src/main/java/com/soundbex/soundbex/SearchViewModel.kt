package com.soundbex.soundbex

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import com.google.gson.JsonParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.IOException

data class SongResult(
    val title: String,
    val artist: String,
    val imageUrl: String?
)

class SearchViewModel : ViewModel() {

    private val client = OkHttpClient()

    private val _results = MutableStateFlow<List<SongResult>>(emptyList())
    val results: StateFlow<List<SongResult>> = _results

    private val _errorChannel = MutableSharedFlow<String>()
    val errorFlow: SharedFlow<String> = _errorChannel.asSharedFlow()

    fun searchSongs(query: String) {
        if (query.isBlank()) {
            _results.value = emptyList()
            return
        }

        viewModelScope.launch {
            try {
                val request = Request.Builder()
                    .url("https://youtube-music-api3.p.rapidapi.com/search?q=$query")
                    .get()
                    .addHeader("X-RapidAPI-Key", "dcc8f2234amsh5bd1976ece1278ep170784jsnc7aaa922afb3")
                    .addHeader("X-RapidAPI-Host", "youtube-music-api3.p.rapidapi.com")
                    .build()

                val json = withContext(Dispatchers.IO) {
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            val errorCode = response.code
                            val errorMessage = when(errorCode) {
                                401, 403 -> "API Anahtarı Geçersiz veya Yetkisiz. (Kod: $errorCode)"
                                429 -> "Kota Doldu. Lütfen daha sonra tekrar deneyin."
                                else -> "Sunucu Hatası: Kod $errorCode"
                            }
                            _errorChannel.emit(errorMessage)
                            return@use null
                        }
                        response.body?.string()
                    }
                } ?: return@launch

                val parsed = JsonParser.parseString(json).asJsonObject

                val tracks = parsed["result"]?.asJsonArray ?: run {
                    _errorChannel.emit("API yanıtının 'result' listesi bulunamadı.")
                    return@launch
                }

                val list = tracks.mapNotNull { element ->
                    try {
                        val data = element.asJsonObject

                        val name = data["title"]?.asString ?: "Bilinmeyen Şarkı"

                        val artist = data["author"]?.asString ?: "Bilinmeyen Sanatçı"

                        val image = data["thumbnail"]?.asString

                        SongResult(name, artist, image)

                    } catch (e: Exception) {
                        null
                    }
                }

                _results.value = list

                if (list.isEmpty()) {
                    _errorChannel.emit("'$query' araması için hiçbir sonuç bulunamadı.")
                }

            } catch (e: IOException) {
                _errorChannel.emit("Ağ bağlantı hatası. İnternetinizi kontrol edin.")
                _results.value = emptyList()
            } catch (e: Exception) {
                _errorChannel.emit("Beklenmeyen bir hata oluştu: ${e.localizedMessage}")
                _results.value = emptyList()
            }
        }
    }
}