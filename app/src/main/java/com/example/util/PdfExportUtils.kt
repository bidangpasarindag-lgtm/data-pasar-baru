package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import android.media.ExifInterface
import com.example.data.config.AgencyConfigManager
import com.example.data.model.Pedagang
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfExportUtils {

    // Ukuran Kertas F5 Landscape (215.9 mm x 165.1 mm) dalam satuan point (72 pt = 1 inch)
    // 215.9 mm ≈ 612 pt, 165.1 mm ≈ 468 pt
    private const val PAGE_WIDTH = 612
    private const val PAGE_HEIGHT = 468

    /**
     * Mencetak / mendownload PDF Kartu Bukti Pendataan Pedagang.
     * Dapat digunakan untuk 1 pedagang (single) atau semua pedagang (batch multi-halaman).
     */
    fun generateAndOpenPdf(
        context: Context,
        pedagangList: List<Pedagang>,
        fileNamePrefix: String = "Bukti_Pendataan_Pedagang",
        onStart: () -> Unit = {},
        onProgress: (Float, String, String) -> Unit = { _, _, _ -> },
        onComplete: () -> Unit = {}
    ) {
        if (pedagangList.isEmpty()) {
            Toast.makeText(context, "Tidak ada data pedagang untuk dicetak", Toast.LENGTH_SHORT).show()
            return
        }

        onStart()
        onProgress(0.01f, "Mempersiapkan data...", "Menghitung estimasi...")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val startTime = System.currentTimeMillis()
                val pdfDocument = PdfDocument()

                val agencyConfig = AgencyConfigManager.config.value

                // Load Logo Instansi (Custom Uri or default drawable)
                withContext(Dispatchers.Main) {
                    onProgress(0.05f, "Memuat logo instansi...", "")
                }
                var logoBitmap: Bitmap? = if (agencyConfig.customLogoUri.isNotBlank()) {
                    loadBitmapFromUriOrUrl(context, agencyConfig.customLogoUri)
                } else null

                if (logoBitmap == null) {
                    val logoNames = listOf("logo_default", "ic_pamekasan_logo", "ic_pamekasan_logo_default", "disperindag_header_banner_1785657536075")
                    for (name in logoNames) {
                        val resId = context.resources.getIdentifier(name, "drawable", context.packageName)
                        if (resId != 0) {
                            logoBitmap = BitmapFactory.decodeResource(context.resources, resId)
                            if (logoBitmap != null) break
                        }
                    }
                }

                pedagangList.forEachIndexed { index, pedagang ->
                    val currentProgress = 0.1f + (index.toFloat() / pedagangList.size.toFloat() * 0.8f)
                    val elapsed = System.currentTimeMillis() - startTime
                    val estTotal = if (index > 0) (elapsed / index) * pedagangList.size else 0L
                    val remaining = if (index > 0) estTotal - elapsed else 0L
                    
                    val remainingStr = if (remaining > 0) {
                        val seconds = (remaining / 1000) % 60
                        val minutes = (remaining / (1000 * 60)) % 60
                        String.format("%02d:%02d", minutes, seconds)
                    } else "Menghitung..."

                    withContext(Dispatchers.Main) {
                        onProgress(
                            currentProgress,
                            "Merender halaman ${index + 1} dari ${pedagangList.size}",
                            "Estimasi sisa: $remainingStr"
                        )
                    }

                    val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, index + 1).create()
                    val page = pdfDocument.startPage(pageInfo)
                    val canvas = page.canvas

                    // Load Foto Pedagang Bitmap if available
                    val photoBitmap = loadBitmapFromUriOrUrl(context, pedagang.fotoPedagangUri)

                    drawKartuBuktiPedagang(
                        canvas = canvas,
                        pedagang = pedagang,
                        photoBitmap = photoBitmap,
                        logoBitmap = logoBitmap,
                        currentPage = index + 1,
                        totalPages = pedagangList.size
                    )

                    pdfDocument.finishPage(page)
                }

                withContext(Dispatchers.Main) {
                    onProgress(0.95f, "Menyimpan file PDF...", "Hampir selesai")
                }
                val fileName = if (pedagangList.size == 1) {
                    val p = pedagangList[0]
                    var fmt = agencyConfig.pdfFileNameFormat
                    if (fmt.isBlank()) fmt = "KARTU_PEDAGANG_{NAMA}_{NOMOR_KIOS}"
                    val cleanNama = p.namaPedagang.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                    val cleanKios = p.nomorKiosLos.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                    val resolvedName = fmt
                        .replace("{NAMA}", cleanNama)
                        .replace("{NOMOR_KIOS}", cleanKios)
                    "${resolvedName}.pdf"
                } else {
                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                    val cleanPrefix = fileNamePrefix.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                    "${cleanPrefix}_$timestamp.pdf"
                }

                val finalDir = AppStorageUtils.getCategoryDirectory(
                    category = AppStorageUtils.CategoryFolder.PDF,
                    customSubFolder = agencyConfig.pdfStorageSubfolder,
                    context = context
                )

                val pdfFile = File(finalDir, fileName)
                val outputStream = FileOutputStream(pdfFile)
                pdfDocument.writeTo(outputStream)
                outputStream.close()
                pdfDocument.close()

                withContext(Dispatchers.Main) {
                    openPdfFile(context, pdfFile)
                    onComplete()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Gagal mencetak PDF: ${e.message}", Toast.LENGTH_LONG).show()
                    onComplete()
                }
            }
        }
    }

    private fun rotateBitmapIfNeeded(bitmap: Bitmap?, exif: ExifInterface): Bitmap? {
        if (bitmap == null) return null
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        val rotationDegrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
        if (rotationDegrees == 0) return bitmap
        val matrix = Matrix()
        matrix.postRotate(rotationDegrees.toFloat())
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated != bitmap) {
            bitmap.recycle()
        }
        return rotated
    }

    private fun loadBitmapFromUriOrUrl(context: Context, rawUri: String?): Bitmap? {
        if (rawUri.isNullOrBlank()) return null
        val directUrl = DriveImageUtils.convertToDirectUrl(rawUri) ?: rawUri
        return try {
            if (directUrl.startsWith("http://") || directUrl.startsWith("https://")) {
                val url = java.net.URL(directUrl)
                val connection = url.openConnection()
                connection.connectTimeout = 6000
                connection.readTimeout = 6000
                connection.doInput = true
                connection.connect()
                val bytes = connection.getInputStream().readBytes()
                val exif = ExifInterface(bytes.inputStream())
                val rawBmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                rotateBitmapIfNeeded(rawBmp, exif)
            } else if (directUrl.startsWith("content://") || directUrl.startsWith("file://")) {
                val uri = Uri.parse(directUrl)
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes != null) {
                    val exif = ExifInterface(bytes.inputStream())
                    val rawBmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    rotateBitmapIfNeeded(rawBmp, exif)
                } else null
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun drawKartuBuktiPedagang(
        canvas: Canvas,
        pedagang: Pedagang,
        photoBitmap: Bitmap?,
        logoBitmap: Bitmap?,
        currentPage: Int,
        totalPages: Int
    ) {
        // 1. Background Kartu Putih
        val bgPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), bgPaint)

        // 2. Transparan / Tanpa Frame Hijau (Frame hijau dihilangkan sesuai permintaan)

        val agencyConfig = AgencyConfigManager.config.value

        // Kop Surat Text Paints & Sizing based on AgencyConfig
        val headerSize = agencyConfig.pdfHeaderFontSize.toFloat().coerceAtLeast(8f)
        val bodySize = agencyConfig.pdfBodyFontSize.toFloat().coerceAtLeast(7f)

        val kop1Paint = Paint().apply {
            color = Color.BLACK
            textSize = headerSize
            typeface = Typeface.create("arial", Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        val kop2Paint = Paint().apply {
            color = Color.BLACK
            textSize = headerSize + 1.5f
            typeface = Typeface.create("arial", Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        val kop3Paint = Paint().apply {
            color = Color.parseColor("#222222")
            textSize = bodySize
            typeface = Typeface.create("arial", Typeface.NORMAL)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        val textPaintDark = Paint().apply {
            color = Color.parseColor("#222222")
            textSize = bodySize - 1f
            isAntiAlias = true
        }
        val textPaintBlue = Paint().apply {
            color = Color.parseColor("#1A73E8")
            textSize = bodySize - 1f
            isAntiAlias = true
            isUnderlineText = true
        }

        if (agencyConfig.pdfShowKopSurat) {
            val line4Prefix = "${agencyConfig.teleponFax} Laman : "
            val line4Url = agencyConfig.websiteLaman
            val line5Prefix = "Pos-el : "
            val line5Email = agencyConfig.posElEmail

            val widthKop1 = kop1Paint.measureText(agencyConfig.namaPemerintah)
            val widthKop2 = kop2Paint.measureText(agencyConfig.namaDinas)
            val widthKop3 = kop3Paint.measureText(agencyConfig.alamatDinas)
            val widthPrefix4 = textPaintDark.measureText(line4Prefix)
            val widthUrl4 = textPaintBlue.measureText(line4Url)
            val widthLine4 = widthPrefix4 + widthUrl4
            val widthPrefix5 = textPaintDark.measureText(line5Prefix)
            val widthEmail5 = textPaintBlue.measureText(line5Email)
            val widthLine5 = widthPrefix5 + widthEmail5

            val maxTextWidth = maxOf(widthKop1, widthKop2, widthKop3, widthLine4, widthLine5)

            val logoSize = if (agencyConfig.pdfShowLogo) 60f else 0f
            val logoGap = if (agencyConfig.pdfShowLogo) 12f else 0f
            val totalCombinedWidth = logoSize + logoGap + maxTextWidth

            val combinedStartX = maxOf(16f, (PAGE_WIDTH - totalCombinedWidth) / 2f)

            val logoLeft = combinedStartX
            val logoTop = 22f
            val logoRight = logoLeft + logoSize
            val logoBottom = logoTop + logoSize

            val textBlockLeft = if (agencyConfig.pdfShowLogo) logoRight + logoGap else combinedStartX
            val headerCenterX = textBlockLeft + (maxTextWidth / 2f)

            // Draw Kop Logo
            if (agencyConfig.pdfShowLogo) {
                if (logoBitmap != null) {
                    val srcRect = Rect(0, 0, logoBitmap.width, logoBitmap.height)
                    val destRect = RectF(logoLeft, logoTop, logoRight, logoBottom)
                    canvas.drawBitmap(logoBitmap, srcRect, destRect, Paint(Paint.FILTER_BITMAP_FLAG))
                } else {
                    val logoPaint = Paint().apply {
                        color = Color.parseColor("#1B5E20")
                        style = Paint.Style.FILL
                        isAntiAlias = true
                    }
                    val path = Path().apply {
                        moveTo((logoLeft + logoRight) / 2f, logoTop)
                        lineTo(logoRight, logoTop + 15f)
                        lineTo(logoRight, logoBottom - 15f)
                        lineTo((logoLeft + logoRight) / 2f, logoBottom)
                        lineTo(logoLeft, logoBottom - 15f)
                        lineTo(logoLeft, logoTop + 15f)
                        close()
                    }
                    canvas.drawPath(path, logoPaint)
                }
            }

            // Line 1: PEMERINTAH KABUPATEN PAMEKASAN
            canvas.drawText(agencyConfig.namaPemerintah, headerCenterX, 32f, kop1Paint)

            // Line 2: DINAS PERINDUSTRIAN DAN PERDAGANGAN
            canvas.drawText(agencyConfig.namaDinas, headerCenterX, 46f, kop2Paint)

            // Line 3: Jalan Jokotole Nomor 199 Pamekasan 69322
            canvas.drawText(agencyConfig.alamatDinas, headerCenterX, 58f, kop3Paint)

            // Line 4: Telepon/Fax
            val startX4 = headerCenterX - (widthLine4 / 2f)
            canvas.drawText(line4Prefix, startX4, 69f, textPaintDark)
            canvas.drawText(line4Url, startX4 + widthPrefix4, 69f, textPaintBlue)

            // Line 5: Email
            val startX5 = headerCenterX - (widthLine5 / 2f)
            canvas.drawText(line5Prefix, startX5, 79f, textPaintDark)
            canvas.drawText(line5Email, startX5 + widthPrefix5, 79f, textPaintBlue)

            // Garis Kop Surat Ganda
            val lineThick = Paint().apply {
                color = Color.BLACK
                strokeWidth = 2.5f
                style = Paint.Style.STROKE
            }
            val lineThin = Paint().apply {
                color = Color.BLACK
                strokeWidth = 0.8f
                style = Paint.Style.STROKE
            }

            canvas.drawLine(20f, 85f, (PAGE_WIDTH - 20).toFloat(), 85f, lineThick)
            canvas.drawLine(20f, 88f, (PAGE_WIDTH - 20).toFloat(), 88f, lineThin)
        }

        // Subtitle Banner
        val docTitleBg = Paint().apply {
            color = Color.parseColor("#E8F5E9")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(20f, 93f, (PAGE_WIDTH - 20).toFloat(), 113f, 4f, 4f, docTitleBg)

        val docTitlePaint = Paint().apply {
            color = Color.parseColor("#1B5E20")
            textSize = 10f
            typeface = Typeface.create("arial", Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        val cardBannerTitle = if (agencyConfig.pdfTitleText.isNotBlank()) 
            agencyConfig.pdfTitleText.uppercase() 
        else 
            "${agencyConfig.namaPasar.uppercase()} - KARTU BUKTI PENDATAAN PEDAGANG"
            
        // Handle Multi-line Title
        val titleLines = cardBannerTitle.split("\n")
        val titleLineHeight = 12f
        val totalTitleHeight = (titleLines.size * titleLineHeight).coerceAtLeast(20f)
        val bannerTop = 93f
        val bannerBottom = bannerTop + totalTitleHeight
        
        canvas.drawRoundRect(20f, bannerTop, (PAGE_WIDTH - 20).toFloat(), bannerBottom, 4f, 4f, docTitleBg)
        
        var currentTitleY = bannerTop + (totalTitleHeight / 2f) - ((titleLines.size - 1) * titleLineHeight / 2f) + 3f
        titleLines.forEach { line ->
            canvas.drawText(line, (PAGE_WIDTH / 2).toFloat(), currentTitleY, docTitlePaint)
            currentTitleY += titleLineHeight
        }

        // 4. Detail Pedagang (Sisi Kiri)
                val labelPaint = Paint().apply {
            color = Color.parseColor("#333333")
            textSize = bodySize
            typeface = Typeface.create("arial", Typeface.BOLD)
            isAntiAlias = true
        }

                val valuePaint = Paint().apply {
            color = Color.parseColor("#333333")
            textSize = bodySize
            typeface = Typeface.create("arial", Typeface.NORMAL)
            isAntiAlias = true
        }

                val boldValuePaint = Paint().apply {
            color = Color.parseColor("#333333")
            textSize = bodySize
            typeface = Typeface.create("arial", Typeface.BOLD)
            isAntiAlias = true
        }

        var startY = bannerBottom + 19f
        val lineSpacing = 18f
        val startXLabel = 30f
        val startXColon = 175f
        val startXValue = 185f

        fun drawField(label: String, value: String, isHighlight: Boolean = false, isUpperCase: Boolean = false) {
            canvas.drawText(label, startXLabel, startY, labelPaint)
            canvas.drawText(":", startXColon, startY, labelPaint)
            val p = if (isHighlight) boldValuePaint else valuePaint
            var finalValue = if (isUpperCase) value.uppercase() else value
            
            // Text Wrapping
            val maxWidth = 420f - startXValue - 10f
            val words = finalValue.split(" ")
            var currentLine = ""
            for (word in words) {
                val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                if (p.measureText(testLine) > maxWidth) {
                    canvas.drawText(currentLine, startXValue, startY, p)
                    startY += lineSpacing
                    currentLine = word
                } else {
                    currentLine = testLine
                }
            }
            if (currentLine.isNotEmpty()) {
                canvas.drawText(currentLine, startXValue, startY, p)
                startY += lineSpacing
            }
        }

        drawField(agencyConfig.pdfLabelNama, pedagang.namaPedagang, isHighlight = true, isUpperCase = true)
        drawField(agencyConfig.pdfLabelNik, pedagang.nik.ifBlank { "-" }, isHighlight = false)
        drawField(agencyConfig.pdfLabelAlamat, pedagang.alamat.ifBlank { "-" }, isHighlight = false)
        drawField(agencyConfig.pdfLabelHp, pedagang.nomorHp.ifBlank { "-" }, isHighlight = false)
        drawField(agencyConfig.pdfLabelRuang, pedagang.jenisRuangDagang, isHighlight = false)
        drawField(agencyConfig.pdfLabelKios, pedagang.nomorKiosLos, isHighlight = false)
        drawField(agencyConfig.pdfLabelKomoditi, pedagang.komoditi, isHighlight = false)
        drawField(agencyConfig.pdfLabelStatus, pedagang.status, isHighlight = false)
        drawField(agencyConfig.pdfLabelWaktu, pedagang.timestamp, isHighlight = false)
        
        if (pedagang.keterangan.trim().isNotBlank()) {
            drawField(agencyConfig.pdfLabelKeteranganHeader, pedagang.keterangan, isHighlight = false)
        }

        // 5. Bingkai Foto Pedagang (Sisi Kanan Atas)
        val photoLeft = 420f
        val photoTop = 122f
        val photoRight = 582f
        val photoBottom = 222f

        val photoBoxBg = Paint().apply {
            color = Color.parseColor("#F5F5F5")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(photoLeft, photoTop, photoRight, 325f, 6f, 6f, photoBoxBg)

        val photoBoxBorder = Paint().apply {
            color = Color.parseColor("#2E7D32")
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
        }
        canvas.drawRoundRect(photoLeft, photoTop, photoRight, 325f, 6f, 6f, photoBoxBorder)

        val imgFrameMargin = 4f
        val imgLeft = photoLeft + imgFrameMargin
        val imgTop = photoTop + imgFrameMargin
        val imgRight = photoRight - imgFrameMargin
        val imgBottom = photoBottom - imgFrameMargin

        if (photoBitmap != null && agencyConfig.pdfShowFotoPedagang) {
            val destWidth = imgRight - imgLeft
            val destHeight = imgBottom - imgTop

            // Fill clean white background inside photo box
            val bgPaint = Paint().apply { color = Color.WHITE }
            canvas.drawRect(imgLeft, imgTop, imgRight, imgBottom, bgPaint)

            val destRectF = RectF(imgLeft, imgTop, imgRight, imgBottom)

            // Smart crop source rectangle to fit target frame without distortion, centering face/head
            val srcRect = getSmartCropSrcRect(photoBitmap, destWidth, destHeight)

            canvas.save()
            canvas.clipRect(destRectF)
            canvas.drawBitmap(photoBitmap, srcRect, destRectF, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
            canvas.restore()
        } else {
            val placeholderBg = Paint().apply {
                color = Color.parseColor("#E0E0E0")
                style = Paint.Style.FILL
            }
            canvas.drawRect(imgLeft, imgTop, imgRight, imgBottom, placeholderBg)

            val placeholderTextPaint = Paint().apply {
                color = Color.parseColor("#757575")
                textSize = 8.5f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create("arial", Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText(agencyConfig.pdfLabelFotoPlaceholder, (imgLeft + imgRight) / 2f, (imgTop + imgBottom) / 2f - 3f, placeholderTextPaint)
            canvas.drawText(agencyConfig.pdfLabelLampiranPlaceholder, (imgLeft + imgRight) / 2f, (imgTop + imgBottom) / 2f + 8f, placeholderTextPaint)
        }

        // 5.5 Bingkai QR Code Identifikasi Pedagang (Pas Berada Di Bawah Foto)
        val qrBoxTop = 226f
        val qrBoxBottom = 325f
        

        

        val qrPayload = QrCodeUtils.getMerchantQrPayload(pedagang)
        val qrBitmap = QrCodeUtils.generateQrCodeBitmap(qrPayload, 256)
        if (qrBitmap != null && agencyConfig.pdfShowQrCode) {
            val qrSize = 70f
            val qrLeft = (photoLeft + photoRight) / 2f - (qrSize / 2f)
            val qrTop = qrBoxTop + 5f
            val srcRect = Rect(0, 0, qrBitmap.width, qrBitmap.height)
            val destRect = RectF(qrLeft, qrTop, qrLeft + qrSize, qrTop + qrSize)
            canvas.drawBitmap(qrBitmap, srcRect, destRect, Paint(Paint.FILTER_BITMAP_FLAG))
        }

        val qrTitlePaint = Paint().apply {
            color = Color.parseColor("#1B5E20")
            textSize = 7f
            typeface = Typeface.create("arial", Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(agencyConfig.pdfLabelScanQr, (photoLeft + photoRight) / 2f, qrBoxBottom - 12f, qrTitlePaint)

        // 6. Section Informasi Resmi Pendataan (Dipindah ke bawah pas di atas footer)
        val infoBoxLeft = 30f
        val infoBoxRight = 582f
        val infoBoxBottom = (PAGE_HEIGHT - 45).toFloat()
        val infoBoxTop = infoBoxBottom - 40f

        val infoBoxBg = Paint().apply {
            color = Color.parseColor("#E8F5E9")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(infoBoxLeft, infoBoxTop, infoBoxRight, infoBoxBottom, 6f, 6f, infoBoxBg)

        val infoBoxBorder = Paint().apply {
            color = Color.parseColor("#2E7D32")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRoundRect(infoBoxLeft, infoBoxTop, infoBoxRight, infoBoxBottom, 6f, 6f, infoBoxBorder)

        val infoTextPaint = Paint().apply {
            color = Color.parseColor("#2E7D32")
            textSize = 8f
            typeface = Typeface.create("arial", Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(
            agencyConfig.pdfLabelTerdataResmi,
            infoBoxLeft + 10f,
            infoBoxTop + 14f,
            infoTextPaint
        )

        val infoSubTextPaint = Paint().apply {
            color = Color.parseColor("#444444")
            textSize = 7.5f
            isAntiAlias = true
        }
        val dateStr = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(Date())
        val petugasEmail = pedagang.emailAddress.ifBlank { "Sistem Pasar Waru" }
        val customDiterbitkan = agencyConfig.pdfLabelDiterbitkan
            .replace("{TANGGAL}", dateStr)
            .replace("{PETUGAS}", petugasEmail)
            .replace("{DINAS}", agencyConfig.namaDinas)
            .replace("{PEMERINTAH}", agencyConfig.namaPemerintah)
        canvas.drawText(
            customDiterbitkan,
            infoBoxLeft + 10f,
            infoBoxTop + 26f,
            infoSubTextPaint
        )

        // 7. Footer Disclaimer
        val footerLinePaint = Paint().apply {
            color = Color.parseColor("#1B5E20")
            strokeWidth = 1f
        }
        canvas.drawLine(16f, (PAGE_HEIGHT - 18).toFloat(), (PAGE_WIDTH - 16).toFloat(), (PAGE_HEIGHT - 18).toFloat(), footerLinePaint)

        val footerTextPaint = Paint().apply {
            color = Color.parseColor("#666666")
            textSize = 7.5f
            isAntiAlias = true
            typeface = Typeface.create("arial", Typeface.NORMAL)
        }
        canvas.drawText(
            agencyConfig.pdfFooterText,
            20f,
            (PAGE_HEIGHT - 8).toFloat(),
            footerTextPaint
        )
        
        if (agencyConfig.pdfShowPetugas) {
            val petugasPaint = Paint().apply {
                color = Color.parseColor("#666666")
                textSize = 7.5f
                textAlign = Paint.Align.RIGHT
                isAntiAlias = true
                typeface = Typeface.create("arial", Typeface.NORMAL)
            }
            val petugasName = pedagang.emailAddress.ifBlank { "Admin" }
            canvas.drawText("Petugas: $petugasName", (PAGE_WIDTH - 20).toFloat(), (PAGE_HEIGHT - 8).toFloat(), petugasPaint)
        }
    }

    private fun openPdfFile(context: Context, pdfFile: File) {
        try {
            Toast.makeText(
                context,
                "PDF Berhasil Disimpan di:\n${pdfFile.absolutePath}",
                Toast.LENGTH_LONG
            ).show()

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }

            // Membuka dialog pilihan (chooser) aplikasi penampil PDF di HP pengguna
            val chooserIntent = Intent.createChooser(viewIntent, "Pilih Aplikasi untuk Membuka PDF...").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // Grant permissions to resolved intent handlers if any
            val resInfoList = context.packageManager.queryIntentActivities(
                chooserIntent,
                android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
            )
            for (resolveInfo in resInfoList) {
                val packageName = resolveInfo.activityInfo.packageName
                context.grantUriPermission(
                    packageName,
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

            context.startActivity(chooserIntent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "PDF Tersimpan di: ${pdfFile.absolutePath}\n(Gagal membuka aplikasi penampil PDF)", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Memotong bitmap secara presisi (CenterCrop dengan deteksi wajah/kepala)
     * agar dapat sepenuhnya mengisi frame target tanpa terjadi distorsi / foto penyet,
     * serta menempatkan posisi wajah/kepala di area tengah/3/4 frame.
     */
    private fun getSmartCropSrcRect(bitmap: Bitmap, targetWidth: Float, targetHeight: Float): Rect {
        val srcWidth = bitmap.width.toFloat()
        val srcHeight = bitmap.height.toFloat()
        if (srcWidth <= 0f || srcHeight <= 0f || targetWidth <= 0f || targetHeight <= 0f) {
            return Rect(0, 0, bitmap.width, bitmap.height)
        }

        val targetRatio = targetWidth / targetHeight
        val srcRatio = srcWidth / srcHeight

        val cropWidth: Float
        val cropHeight: Float

        if (srcRatio > targetRatio) {
            cropHeight = srcHeight
            cropWidth = srcHeight * targetRatio
        } else {
            cropWidth = srcWidth
            cropHeight = srcWidth / targetRatio
        }

        var focalX = srcWidth / 2f
        var focalY = srcHeight * 0.35f // Default area kepala/wajah di foto paspor/ID

        try {
            val maxDim = 400
            val scale = minOf(1f, maxDim.toFloat() / maxOf(srcWidth, srcHeight))
            val detectW = (srcWidth * scale).toInt().coerceAtLeast(1)
            val detectH = (srcHeight * scale).toInt().coerceAtLeast(1)

            val evenDetectW = if (detectW % 2 != 0) detectW - 1 else detectW
            if (evenDetectW > 10 && detectH > 10) {
                val scaledBmp = Bitmap.createScaledBitmap(bitmap, evenDetectW, detectH, true)
                val rgb565Bmp = scaledBmp.copy(Bitmap.Config.RGB_565, false)
                if (rgb565Bmp != null) {
                    val detector = android.media.FaceDetector(rgb565Bmp.width, rgb565Bmp.height, 1)
                    val faces = Array<android.media.FaceDetector.Face?>(1) { null }
                    val numFound = detector.findFaces(rgb565Bmp, faces)
                    if (numFound > 0 && faces[0] != null) {
                        val midPoint = PointF()
                        faces[0]!!.getMidPoint(midPoint)
                        focalX = midPoint.x / scale
                        focalY = midPoint.y / scale
                    }
                    rgb565Bmp.recycle()
                }
                if (scaledBmp != bitmap) scaledBmp.recycle()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val maxCropLeft = (srcWidth - cropWidth).coerceAtLeast(0f)
        val maxCropTop = (srcHeight - cropHeight).coerceAtLeast(0f)

        val cropLeft = (focalX - cropWidth / 2f).coerceIn(0f, maxCropLeft)
        val cropRight = cropLeft + cropWidth

        val cropTop = (focalY - cropHeight * 0.35f).coerceIn(0f, maxCropTop)
        val cropBottom = cropTop + cropHeight

        return Rect(
            cropLeft.toInt().coerceIn(0, bitmap.width),
            cropTop.toInt().coerceIn(0, bitmap.height),
            cropRight.toInt().coerceIn(0, bitmap.width),
            cropBottom.toInt().coerceIn(0, bitmap.height)
        )
    }
}
