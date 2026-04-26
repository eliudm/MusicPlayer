package com.example.musicplayer.domain.usecase

import com.example.musicplayer.domain.repository.MusicRepository
import javax.inject.Inject

class CreatePlaylistUseCase @Inject constructor(private val repository: MusicRepository) {
    suspend operator fun invoke(name: String): Long = repository.createPlaylist(name)
}
