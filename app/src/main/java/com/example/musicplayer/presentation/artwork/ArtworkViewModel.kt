package com.example.musicplayer.presentation.artwork

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.domain.repository.MusicRepository
import com.example.musicplayer.util.artworkDir
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject

@HiltViewModel
class ArtworkViewModel @Inject constructor(
    private val repository: MusicRepository,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val songId: Long = savedStateHandle["songId"] ?: -1L

    private val _state = MutableStateFlow(ArtworkUiState())
    val state: StateFlow<ArtworkUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val song = repository.getSongs().first().find { it.id == songId } ?: return@launch
            _state.update { it.copy(song = song, searchQuery = "${song.artist} ${song.album}") }
        }
    }

    fun reset() {
        File(artworkDir(context), "$songId.jpg").delete()
        _state.update { it.copy(artworkVersion = it.artworkVersion + 1, message = "Artwork reset to default") }
    }

    fun saveFromGallery(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val dest = File(artworkDir(context), "$songId.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    dest.outputStream().use { input.copyTo(it) }
                }
            }.onSuccess {
                _state.update { it.copy(artworkVersion = it.artworkVersion + 1, message = "Artwork updated") }
            }.onFailure {
                _state.update { it.copy(message = "Failed to save image") }
            }
        }
    }

    fun setSearchQuery(q: String) = _state.update { it.copy(searchQuery = q) }

    fun toggleSearchPanel() = _state.update { it.copy(showSearchPanel = !it.showSearchPanel, searchResults = emptyList()) }

    fun searchOnline() {
        val query = _state.value.searchQuery.trim().ifBlank { return }
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isSearching = true, searchResults = emptyList()) }
            runCatching {
                val encoded = URLEncoder.encode(query, "UTF-8")
                val json = URL("https://itunes.apple.com/search?term=$encoded&media=music&entity=album&limit=12").readText()
                val arr = JSONObject(json).getJSONArray("results")
                (0 until arr.length()).mapNotNull { i ->
                    arr.getJSONObject(i).optString("artworkUrl100", null)
                        ?.replace("100x100bb", "600x600bb")
                }
            }.onSuccess { urls ->
                _state.update { it.copy(isSearching = false, searchResults = urls) }
            }.onFailure {
                _state.update { it.copy(isSearching = false, message = "Search failed — check connection") }
            }
        }
    }

    fun saveFromUrl(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isDownloading = true) }
            runCatching {
                val dest = File(artworkDir(context), "$songId.jpg")
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.connectTimeout = 10_000
                conn.readTimeout = 15_000
                conn.connect()
                conn.inputStream.use { input -> dest.outputStream().use { input.copyTo(it) } }
                conn.disconnect()
            }.onSuccess {
                _state.update { it.copy(
                    isDownloading = false,
                    artworkVersion = it.artworkVersion + 1,
                    searchResults = emptyList(),
                    showSearchPanel = false,
                    message = "Artwork saved"
                )}
            }.onFailure {
                _state.update { it.copy(isDownloading = false, message = "Download failed") }
            }
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }
}
