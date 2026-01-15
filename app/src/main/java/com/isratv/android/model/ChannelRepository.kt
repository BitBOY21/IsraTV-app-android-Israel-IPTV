package com.isratv.android.model

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChannelRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Direct link to the Gist containing the channel list.
    private val BASE_URL = "https://gist.githubusercontent.com/BitBOY21/b0b95de46a230d88f98aea8304c30d3c/raw/channels.json"
    
    // In-memory cache for channels after loading.
    private var loadedChannels: List<Channel> = emptyList()

    // Function to load channels (called from ViewModel).
    suspend fun loadChannelsFromWeb(): List<Channel> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("ChannelRepository", "Attempting to fetch channels from Gist...")

                // 1. Fetch text content from the web.
                val jsonContent = URL("$BASE_URL?t=${System.currentTimeMillis()}").readText()

                // 2. Parse into a list of channels using the JsonParser.
                val remoteChannels = JsonParser.parse(jsonContent)

                if (remoteChannels.isNotEmpty()) {
                    Log.d("ChannelRepository", "Success! Loaded ${remoteChannels.size} channels from remote Gist")
                    loadedChannels = remoteChannels
                    return@withContext remoteChannels
                }
            } catch (e: Exception) {
                // Log error if fetching fails (e.g., no internet or bad URL).
                Log.e("ChannelRepository", "Error loading from web: ${e.message}")
                e.printStackTrace()
            }

            // 3. Fallback: If loading fails, return the manual local list.
            Log.w("ChannelRepository", "Falling back to local manual channels")
            val manualChannels = getManualChannels()
            loadedChannels = manualChannels
            return@withContext manualChannels
        }
    }

    // Backup list for when there is no internet connection on first launch.
    private fun getManualChannels(): List<Channel> {
        return listOf(
            Channel("11", "Kan 11", "https://kan11.media.kan.org.il/hls/live/2024514/2024514/master.m3u8", "kan_11_il"),
            Channel("12", "Keshet 12", "https://mako-streaming.akamaized.net/stream/hls/live/2033791/k12dvr/index.m3u8", "keshet_12_il"),
            Channel("13", "Reshet 13", "https://d18b0e6mopany4.cloudfront.net/out/v1/089428c7346a4892a643a539c8713481/index.m3u8", "reshet_13_il"),
            Channel("14", "Now 14", "https://now14.gostreaming.tv/live-now14/now14-live/playlist.m3u8", "now_14_il"),
            Channel("99", "Knesset Channel", "https://kneset.gostreaming.tv/p2-kneset/_definst_/myStream/playlist.m3u8", "knesset_channel_il"),
            Channel("24", "i24 News", "https://bcovlive-a.akamaihd.net/1961355542001/i24news_en@1961355542001_1/chunklist.m3u8", "i24_news_il")
        )
    }

    // Helper function to find a channel by ID (used by the player).
    fun getChannelById(id: String): Channel? {
        return loadedChannels.find { it.id == id }
    }
}
