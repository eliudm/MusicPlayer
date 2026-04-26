package com.example.musicplayer

import android.net.Uri
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.domain.repository.MusicRepository
import com.example.musicplayer.domain.usecase.PlaySongUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import kotlinx.coroutines.test.runTest
import org.junit.Test

class PlaySongUseCaseTest {

    private val repository = mockk<MusicRepository>(relaxed = true)
    private val useCase = PlaySongUseCase(repository)

    @Test
    fun `play song delegates to repository`() = runTest {
        val song = Song(
            id = 1L, title = "Test Song", artist = "Artist", album = "Album",
            albumId = 10L, duration = 180_000L, uri = Uri.EMPTY,
            trackNumber = 1, year = 2024, genre = "Pop", size = 1024L
        )
        coEvery { repository.play(song) } just Runs

        useCase(song)

        coVerify(exactly = 1) { repository.play(song) }
    }
}
