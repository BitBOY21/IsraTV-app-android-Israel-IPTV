package com.isratv.android.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.isratv.android.model.FavoriteChannel

@Database(entities = [FavoriteChannel::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
}
