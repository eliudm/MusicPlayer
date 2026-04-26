package com.example.musicplayer.domain.usecase

import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.domain.repository.MusicRepository
import javax.inject.Inject

class PlaySongUseCase @Inject constructor(private val repository: MusicRepository) {
    suspend operator fun invoke(song: Song) = repository.play(song)
}
