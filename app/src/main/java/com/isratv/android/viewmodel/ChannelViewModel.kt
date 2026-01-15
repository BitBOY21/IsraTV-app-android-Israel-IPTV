package com.isratv.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isratv.android.model.Channel
import com.isratv.android.model.ChannelRepository
import com.isratv.android.repository.FavoriteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChannelViewModel @Inject constructor(
    private val repository: ChannelRepository,
    private val favoriteRepository: FavoriteRepository
) : ViewModel() {

    private val _channels = MutableStateFlow<List<Channel>>(emptyList())
    val channels: StateFlow<List<Channel>> = _channels

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    // Expose favorites directly from repository.
    val favorites = favoriteRepository.getAllFavorites()

    init {
        loadChannels()
    }

    fun loadChannels() {
        viewModelScope.launch {
            _isLoading.value = true
            val fetchedChannels = repository.loadChannelsFromWeb()
            _channels.value = fetchedChannels
            _isLoading.value = false
        }
    }

    fun getChannelById(id: String): Channel? {
        return repository.getChannelById(id)
    }

    fun isFavorite(channelId: String) = favoriteRepository.isFavoriteFlow(channelId)

    fun toggleFavorite(channel: Channel) {
        viewModelScope.launch {
            favoriteRepository.toggleFavorite(channel)
        }
    }
}
