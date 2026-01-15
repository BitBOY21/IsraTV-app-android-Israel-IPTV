package com.isratv.android.model

import java.io.BufferedReader
import java.io.StringReader

object M3uParser {

    fun parse(m3uContent: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        
        if (m3uContent.isBlank()) return emptyList()

        val reader = BufferedReader(StringReader(m3uContent))
        
        var line: String? = reader.readLine()
        var currentName: String? = null
        var currentLogo: String? = null
        var idCounter = 1

        while (line != null) {
            val trimmedLine = line.trim()

            if (trimmedLine.startsWith("#EXTINF:")) {
                // Attempt to extract metadata from the EXTINF line.
                // Common format: #EXTINF:-1 tvg-id=".." tvg-logo="url",Channel Name
                
                // 1. Extract the channel name (located after the last comma).
                val commaIndex = trimmedLine.lastIndexOf(',')
                if (commaIndex != -1) {
                    currentName = trimmedLine.substring(commaIndex + 1).trim()
                } else {
                    // Edge case: No comma found, name might be missing or format is different.
                    currentName = "Unknown Channel"
                }

                // 2. Extract the logo URL (if present).
                // Looking for tvg-logo="...url..."
                currentLogo = extractAttribute(trimmedLine, "tvg-logo")

            } else if (trimmedLine.isNotEmpty() && !trimmedLine.startsWith("#")) {
                // This is the URL line.
                // We add a channel only if we found a name in the previous line (or used a default)
                // and if the URL looks valid (starts with http/https).
                if (currentName != null && (trimmedLine.startsWith("http") || trimmedLine.startsWith("https"))) {
                    channels.add(
                        Channel(
                            id = idCounter.toString(),
                            name = currentName ?: "Channel $idCounter",
                            logoUrl = currentLogo ?: "",
                            url = trimmedLine,
                            referer = null,
                            origin = null
                        )
                    )
                    idCounter++
                    
                    // Reset variables for the next channel.
                    currentName = null
                    currentLogo = null
                }
            }

            line = reader.readLine()
        }

        return channels
    }

    /**
     * Helper function to extract an attribute value from an EXTINF line.
     * Example: extractAttribute(line, "tvg-logo") returns the logo URL.
     */
    private fun extractAttribute(line: String, attributeName: String): String? {
        val key = "$attributeName=\""
        val startIndex = line.indexOf(key)
        if (startIndex != -1) {
            val valueStart = startIndex + key.length
            val valueEnd = line.indexOf("\"", valueStart)
            if (valueEnd != -1) {
                return line.substring(valueStart, valueEnd)
            }
        }
        return null
    }
}
