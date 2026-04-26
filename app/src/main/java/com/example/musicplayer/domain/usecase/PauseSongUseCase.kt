package com.example.musicplayer.domain.usecase

import com.example.musicplayer.domain.repository.MusicRepository
import javax.inject.Inject

class PauseSongUseCase @Inject constructor(private val repository: MusicRepository) {
    suspend operator fun invoke() = repository.pause()
}
