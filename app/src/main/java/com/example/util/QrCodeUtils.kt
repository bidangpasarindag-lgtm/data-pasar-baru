package com.example.util

import android.graphics.Bitmap
import android.graphics.Color
import com.example.data.model.Pedagang
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import java.util.EnumMap

object QrCodeUtils {

    private const val QR_PREFIX = "PASAR_PAMEKASAN:PEDAGANG:"

    /**
     * Format payload QR Code yang unik untuk setiap pedagang
     */
    fun getMerchantQrPayload(pedagang: Pedagang): String {
        return "PEDAGANG_PASAR_WARU:${pedagang.id}:${pedagang.nik}"
    }

    /**
     * Membuat Bitmap QR Code dari string konten
     */
    fun generateQrCodeBitmap(content: String, size: Int = 512): Bitmap? {
        if (content.isBlank()) return null
        return try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
                }
            }
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Membaca string dari Bitmap QR Code (ZXing)
     */
    fun decodeQrFromBitmap(bitmap: Bitmap): String? {
        return try {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            val source = RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

            val hints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java).apply {
                put(DecodeHintType.TRY_HARDER, true)
            }

            val reader = MultiFormatReader()
            val result = reader.decode(binaryBitmap, hints)
            result.text
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Mengekstrak ID Pedagang atau NIK dari isi QR Code.
     * Mengembalikan ID Pedagang (Long) jika valid, atau null jika tidak dapat dicocokkan secara langsung.
     */
    fun extractPedagangIdFromQr(qrContent: String): Long? {
        val trimmed = qrContent.trim()
        if (trimmed.isEmpty()) return null

        if (trimmed.startsWith("PEDAGANG_PASAR_WARU:")) {
            val parts = trimmed.split(":")
            if (parts.size >= 2) {
                return parts[1].toLongOrNull()
            }
        }

        if (trimmed.startsWith(QR_PREFIX)) {
            val idStr = trimmed.removePrefix(QR_PREFIX)
            return idStr.toLongOrNull()
        }

        if (trimmed.contains("PEDAGANG:", ignoreCase = true)) {
            val parts = trimmed.split(":")
            val lastPart = parts.lastOrNull()?.trim()
            if (lastPart != null && lastPart.toLongOrNull() != null) {
                return lastPart.toLong()
            }
        }

        return trimmed.toLongOrNull()
    }

    /**
     * Mencari objek Pedagang dari daftar berdasarkan hasil scan QR Code
     */
    fun findPedagangFromQrContent(qrContent: String, pedagangList: List<Pedagang>): Pedagang? {
        val extractedId = extractPedagangIdFromQr(qrContent)
        if (extractedId != null) {
            val matchedById = pedagangList.find { it.id == extractedId }
            if (matchedById != null) return matchedById
        }

        val trimmed = qrContent.trim()
        // Coba cocokan berdasarkan NIK jika QR berisi NIK
        if (trimmed.isNotBlank()) {
            val matchedByNik = pedagangList.find { it.nik.isNotBlank() && it.nik.equals(trimmed, ignoreCase = true) }
            if (matchedByNik != null) return matchedByNik
        }

        return null
    }
}
