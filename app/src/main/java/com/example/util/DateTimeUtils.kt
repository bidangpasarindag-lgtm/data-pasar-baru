package com.example.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateTimeUtils {
    private val dateFormats = listOf(
        SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()),
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()),
        SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()),
        SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()),
        SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()),
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    )

    /**
     * Mengkonversi string timestamp (seperti "08/08/2026 09:22:00" atau "2026-08-08 09:22:00")
     * menjadi epoch milliseconds (Long) untuk pengurutan tanggal dan jam secara presisi
     * (Tahun -> Bulan -> Tanggal -> Jam -> Menit -> Detik).
     */
    fun parseTimestampToMillis(timestampStr: String?): Long {
        if (timestampStr.isNullOrBlank()) return 0L
        val cleanStr = timestampStr.trim()

        // Jika string berupa angka murni (epoch millis)
        cleanStr.toLongOrNull()?.let { return it }

        for (sdf in dateFormats) {
            try {
                val date = sdf.parse(cleanStr)
                if (date != null) {
                    return date.time
                }
            } catch (_: Exception) {
                // Lanjut ke pola berikutnya
            }
        }
        return 0L
    }
}
