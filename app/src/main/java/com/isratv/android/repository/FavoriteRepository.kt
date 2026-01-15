package com.isratv.android.repository

import com.isratv.android.data.local.FavoriteDao
import com.isratv.android.model.Channel
import com.isratv.android.model.FavoriteChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoriteRepository @Inject constructor(
    private val favoriteDao: FavoriteDao
) {

    fun getAllFavorites(): Flow<List<FavoriteChannel>> {
        return favoriteDao.getAllFavorites()
    }

    fun isFavoriteFlow(channelId: String): Flow<Boolean> {
        return favoriteDao.isFavorite(channelId)
    }

    suspend fun toggleFavorite(channel: Channel) {
        val isCurrentlyFavorite = isFavoriteFlow(channel.id).first()
        if (isCurrentlyFavorite) {
            favoriteDao.deleteById(channel.id)
        } else {
            val favoriteChannel = FavoriteChannel(
                id = channel.id,
                name = channel.name,
                logoUrl = channel.logoUrl,
                streamUrl = channel.url
            )
            favoriteDao.insert(favoriteChannel)
        }
    }
}

