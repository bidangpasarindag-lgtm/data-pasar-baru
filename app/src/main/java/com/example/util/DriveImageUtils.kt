package com.example.util

object DriveImageUtils {

    /**
     * Extracts Google Drive File ID from various link formats, formulas, or raw IDs.
     */
    fun extractFileId(rawInput: String?): String? {
        if (rawInput.isNullOrBlank()) return null
        var text = rawInput.trim()

        // Clean up Excel/Sheets formulas like =HYPERLINK("url", "label") or =IMAGE("url")
        if (text.startsWith("=") || text.contains("HYPERLINK", ignoreCase = true) || text.contains("IMAGE", ignoreCase = true)) {
            val urlInQuotes = Regex(""""(https?://[^"]+)"""").find(text)
            if (urlInQuotes != null) {
                text = urlInQuotes.groupValues[1]
            }
        }

        // Extract http/https URL if embedded in text
        val httpMatch = Regex("""https?://[^\s",]+""").find(text)
        if (httpMatch != null) {
            text = httpMatch.value
        }

        // 1. file/d/FILE_ID
        val fileDMatch = Regex("""drive\.google\.com/file/d/([a-zA-Z0-9_-]+)""").find(text)
        if (fileDMatch != null) return fileDMatch.groupValues[1]

        // 2. open?id=FILE_ID or uc?id=FILE_ID or &id=FILE_ID
        val openIdMatch = Regex("""[?&]id=([a-zA-Z0-9_-]+)""").find(text)
        if (openIdMatch != null && (text.contains("google.com", ignoreCase = true) || text.contains("googleusercontent.com", ignoreCase = true))) {
            return openIdMatch.groupValues[1]
        }

        // 3. googleusercontent.com/d/FILE_ID
        val lh3Match = Regex("""googleusercontent\.com/d/([a-zA-Z0-9_-]+)""").find(text)
        if (lh3Match != null) return lh3Match.groupValues[1]

        // 4. Raw file ID (alphanumeric string ~20-50 chars)
        if (text.length in 20..50 && text.matches(Regex("""[a-zA-Z0-9_-]+"""))) {
            return text
        }

        return null
    }

    /**
     * Get image thumbnail display URL for Coil / AsyncImage.
     * Uses Google Drive Thumbnail service as primary endpoint.
     */
    fun convertToDirectUrl(rawInput: String?): String? {
        if (rawInput.isNullOrBlank()) return null
        val fileId = extractFileId(rawInput)
        if (fileId != null) {
            return "https://drive.google.com/thumbnail?id=$fileId&sz=w1000"
        }

        var text = rawInput.trim()
        val httpMatch = Regex("""https?://[^\s",]+""").find(text)
        if (httpMatch != null) {
            text = httpMatch.value
        }
        return if (text.startsWith("http://") || text.startsWith("https://")) text else null
    }

    /**
     * Fallback direct image URL (lh3 endpoint).
     */
    fun getFallbackUrl(rawInput: String?): String? {
        val fileId = extractFileId(rawInput) ?: return null
        return "https://lh3.googleusercontent.com/d/$fileId"
    }

    /**
     * Get the shareable Google Drive Web URL for browser/app redirection.
     */
    fun getDriveWebUrl(rawInput: String?): String? {
        if (rawInput.isNullOrBlank()) return null
        val fileId = extractFileId(rawInput)
        if (fileId != null) {
            return "https://drive.google.com/file/d/$fileId/view?usp=sharing"
        }

        var text = rawInput.trim()
        val httpMatch = Regex("""https?://[^\s",]+""").find(text)
        if (httpMatch != null) {
            text = httpMatch.value
        }
        return if (text.startsWith("http://") || text.startsWith("https://")) text else null
    }
}
