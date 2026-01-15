package com.isratv.android.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteChannel(
    @PrimaryKey val id: String,
    val name: String,
    val logoUrl: String,
    val streamUrl: String
)
