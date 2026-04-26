package com.example.musicplayer.domain.usecase

import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.domain.repository.MusicRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetQueueUseCase @Inject constructor(private val repository: MusicRepository) {
    operator fun invoke(): Flow<List<Song>> = repository.getQueue()
}
