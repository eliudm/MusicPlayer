package com.example.musicplayer.presentation.player

import com.example.musicplayer.domain.model.Song

sealed interface PlayerEvent {
    data class PlaySong(val song: Song) : PlayerEvent
    data class Seek(val position: Long) : PlayerEvent
    data object TogglePlayPause : PlayerEvent
    data object ToggleFav : PlayerEvent
    data object SkipNext : PlayerEvent
    data object SkipPrevious : PlayerEvent
    data object ToggleShuffle : PlayerEvent
    data object CycleRepeat : PlayerEvent
}
