package com.example.musicplayer.domain.usecase

import com.example.musicplayer.domain.repository.MusicRepository
import javax.inject.Inject

class SeekUseCase @Inject constructor(private val repository: MusicRepository) {
    suspend operator fun invoke(positionMs: Long) = repository.seekTo(positionMs)
}
