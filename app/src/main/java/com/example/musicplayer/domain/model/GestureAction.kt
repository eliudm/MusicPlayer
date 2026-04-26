package com.example.musicplayer.domain.model

enum class GestureAction(val label: String) {
    NONE("None — disabled"),
    SKIP_NEXT("Skip Next"),
    SKIP_PREVIOUS("Skip Previous"),
    TOGGLE_PLAY_PAUSE("Play / Pause"),
    TOGGLE_FAVORITE("Toggle Favourite"),
    TOGGLE_SHUFFLE("Toggle Shuffle"),
    CYCLE_REPEAT("Cycle Repeat"),
    TOGGLE_LYRICS("Toggle Lyrics")
}

data class GestureSettings(
    val swipeEnabled: Boolean = true,
    val pinchEnabled: Boolean = true,
    val swipeUpAction: GestureAction = GestureAction.TOGGLE_LYRICS,
    val swipeDownAction: GestureAction = GestureAction.NONE
)
