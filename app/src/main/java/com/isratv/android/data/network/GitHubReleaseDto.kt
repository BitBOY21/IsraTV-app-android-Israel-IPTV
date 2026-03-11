package com.isratv.android.data.network

import org.json.JSONObject

data class GitHubReleaseDto(
    val tagName: String,
    val downloadUrl: String
) {
    companion object {
        fun fromJson(json: String): GitHubReleaseDto? {
            try {
                val jsonObject = JSONObject(json)
                val tagName = jsonObject.optString("tag_name")
                
                val assets = jsonObject.optJSONArray("assets")
                if (assets != null && assets.length() > 0) {
                    // Assuming the first asset is the APK we want
                    val firstAsset = assets.getJSONObject(0)
                    val downloadUrl = firstAsset.optString("browser_download_url")
                    
                    if (tagName.isNotEmpty() && downloadUrl.isNotEmpty()) {
                        return GitHubReleaseDto(tagName, downloadUrl)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }
    }
}