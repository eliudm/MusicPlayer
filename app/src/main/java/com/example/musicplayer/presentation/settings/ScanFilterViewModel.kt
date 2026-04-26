package com.example.musicplayer.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.data.preferences.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScanFilterViewModel @Inject constructor(
    private val prefs: AppPreferences
) : ViewModel() {

    val minDurationSec: StateFlow<Int> = prefs.minDurationSec
        .stateIn(viewModelScope, SharingStarted.Eagerly, 30)

    val minSizeKb: StateFlow<Int> = prefs.minSizeKb
        .stateIn(viewModelScope, SharingStarted.Eagerly, 100)

    fun setMinDurationSec(seconds: Int) = viewModelScope.launch {
        prefs.setMinDurationSec(seconds)
    }

    fun setMinSizeKb(kb: Int) = viewModelScope.launch {
        prefs.setMinSizeKb(kb)
    }
}
