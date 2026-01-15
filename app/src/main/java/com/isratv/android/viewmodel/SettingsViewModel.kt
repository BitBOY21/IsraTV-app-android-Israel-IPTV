package com.isratv.android.viewmodel

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppSettings(
    val viewMode: String = "list", // "list" or "grid"
    val autoPlay: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : ViewModel() {
    
    private val VIEW_MODE_KEY = stringPreferencesKey("view_mode")
    private val AUTO_PLAY_KEY = booleanPreferencesKey("auto_play")
    
    val settings: Flow<AppSettings> = dataStore.data.map { preferences ->
        AppSettings(
            viewMode = preferences[VIEW_MODE_KEY] ?: "list",
            autoPlay = preferences[AUTO_PLAY_KEY] ?: true
        )
    }
    
    fun setViewMode(mode: String) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[VIEW_MODE_KEY] = mode
            }
        }
    }
    
    fun setAutoPlay(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[AUTO_PLAY_KEY] = enabled
            }
        }
    }
}


