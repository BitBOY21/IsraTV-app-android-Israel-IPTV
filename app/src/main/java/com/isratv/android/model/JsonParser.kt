package com.isratv.android.model

import org.json.JSONArray
import org.json.JSONObject

object JsonParser {

    /**
     * Parses a JSON string containing channel data.
     * Supports two formats:
     * 1. Old structure: [{"id": "...", "name": "...", "url": "..."}, ...]
     * 2. New structure: {"channels": [{"alias": "...", "url": "...", "referer": "...", "origin": "..."}, ...]}
     */
    fun parse(jsonContent: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        
        try {
            // Try to parse as new structure (root object with "channels" array)
            val rootObject = JSONObject(jsonContent)
            if (rootObject.has("channels")) {
                val jsonArray = rootObject.getJSONArray("channels")
                
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    
                    // Use 'alias' for display name, fallback to 'name'
                    val displayName = item.optString("alias", item.optString("name", ""))
                    val url = item.optString("url", "")
                    val referer = item.optString("referer", null).takeIf { !it.isNullOrEmpty() }
                    val origin = item.optString("origin", null).takeIf { !it.isNullOrEmpty() }
                    val id = item.optString("id", i.toString())
                    
                    if (displayName.isNotEmpty() && url.isNotEmpty()) {
                        channels.add(
                            Channel(
                                id = id,
                                name = displayName,
                                url = url,
                                logoUrl = item.optString("logoUrl", ""),
                                referer = referer,
                                origin = origin
                            )
                        )
                    }
                }
            } else {
                // Fallback: try to parse as old structure (direct array)
                val jsonArray = JSONArray(jsonContent)
                
                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    
                    val id = jsonObject.optString("id", i.toString())
                    val name = jsonObject.optString("name", "")
                    val url = jsonObject.optString("url", "")
                    val logoUrl = jsonObject.optString("logoUrl", "")
                    val referer = jsonObject.optString("referer", null).takeIf { !it.isNullOrEmpty() }
                    val origin = jsonObject.optString("origin", null).takeIf { !it.isNullOrEmpty() }
                    
                    if (name.isNotEmpty() && url.isNotEmpty()) {
                        channels.add(
                            Channel(
                                id = id,
                                name = name,
                                url = url,
                                logoUrl = logoUrl,
                                referer = referer,
                                origin = origin
                            )
                        )
                    }
                }
            }
        } catch (e: org.json.JSONException) {
            // If JSONObject parsing fails, try as array (old format)
            try {
                val jsonArray = JSONArray(jsonContent)
                
                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    
                    val id = jsonObject.optString("id", i.toString())
                    val name = jsonObject.optString("name", "")
                    val url = jsonObject.optString("url", "")
                    val logoUrl = jsonObject.optString("logoUrl", "")
                    val referer = jsonObject.optString("referer", null).takeIf { !it.isNullOrEmpty() }
                    val origin = jsonObject.optString("origin", null).takeIf { !it.isNullOrEmpty() }
                    
                    if (name.isNotEmpty() && url.isNotEmpty()) {
                        channels.add(
                            Channel(
                                id = id,
                                name = name,
                                url = url,
                                logoUrl = logoUrl,
                                referer = referer,
                                origin = origin
                            )
                        )
                    }
                }
            } catch (e2: Exception) {
                throw Exception("Error parsing JSON file: ${e.message ?: "Unknown error"}", e2)
            }
        } catch (e: Exception) {
            throw Exception("Error parsing JSON file: ${e.message ?: "Unknown error"}", e)
        }
        
        return channels
    }
}
