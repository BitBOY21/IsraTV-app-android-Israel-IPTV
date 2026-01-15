package com.isratv.android.model

/**
 * Represents a single TV channel in the app.
 *
 * @param id      Short, stable identifier used for navigation / routing.
 * @param name    Display name shown in the UI.
 * @param url     Streaming URL (e.g. HLS .m3u8).
 * @param logoUrl Optional URL for the channel's logo.
 * @param referer Optional Referer header value for protected streams.
 * @param origin  Optional Origin header value for protected streams.
 */
data class Channel(
    val id: String,
    val name: String,
    val url: String,
    val logoUrl: String = "",
    val referer: String? = null,
    val origin: String? = null
)
