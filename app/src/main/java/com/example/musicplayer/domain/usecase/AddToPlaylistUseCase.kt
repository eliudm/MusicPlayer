package com.example.musicplayer.domain.usecase

import com.example.musicplayer.domain.repository.MusicRepository
import javax.inject.Inject

class AddToPlaylistUseCase @Inject constructor(private val repository: MusicRepository) {
    suspend operator fun invoke(playlistId: Long, songId: Long, position: Int) =
        repository.addSongToPlaylist(playlistId, songId, position)
}
