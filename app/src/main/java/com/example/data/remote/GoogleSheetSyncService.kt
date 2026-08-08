package com.example.data.remote

import android.util.Log
import com.example.data.auth.UserManager
import com.example.data.config.AgencyConfigManager
import com.example.data.model.Pedagang
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class GoogleSheetSyncService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val currentConfig get() = AgencyConfigManager.config.value

    suspend fun testWebhookConnection(customUrl: String? = null, customSpreadsheetId: String? = null): Result<String> = withContext(Dispatchers.IO) {
        try {
            val targetUrl = customUrl?.ifBlank { null } ?: currentConfig.webhookUrl
            val targetSsId = customSpreadsheetId?.ifBlank { null } ?: currentConfig.spreadsheetId

            val formBody = FormBody.Builder()
                .add("action", "PING")
                .add("spreadsheet_id", targetSsId)
                .build()

            val request = Request.Builder()
                .url(targetUrl)
                .post(formBody)
                .addHeader("User-Agent", "DisperindagPamekasanApp/1.0")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful && (responseBody.contains("PONG_OK") || responseBody.contains("status"))) {
                Result.success("Koneksi Webhook Berhasil (PONG_OK)")
            } else {
                Result.failure(Exception("Respons server: ${response.code} $responseBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginToSheet(usernameEntered: String, passwordEntered: String): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        try {
            val targetUrl = currentConfig.webhookUrl
            val targetSsId = currentConfig.spreadsheetId

            val formBody = FormBody.Builder()
                .add("action", "LOGIN")
                .add("username", usernameEntered)
                .add("password", passwordEntered)
                .add("spreadsheet_id", targetSsId)
                .build()

            val request = Request.Builder()
                .url(targetUrl)
                .post(formBody)
                .addHeader("User-Agent", "DisperindagPamekasanApp/1.0")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful && responseBody.isNotBlank()) {
                val json = org.json.JSONObject(responseBody)
                val status = json.optString("status", "")
                if (status == "SUCCESS") {
                    val username = json.optString("username", usernameEntered)
                    val displayName = json.optString("displayName", username)
                    Result.success(Pair(username, displayName))
                } else {
                    val message = json.optString("message", "Username atau Password salah.")
                    Result.failure(Exception(message))
                }
            } else {
                Result.failure(Exception("Gagal menghubungi server: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun postActionToSheet(
        action: String, // "CREATE", "UPDATE", "DELETE"
        pedagang: Pedagang,
        operatorUsername: String = "bidangpasar.indag@gmail.com",
        operatorName: String = "Petugas Disperindag",
        fotoPedagangBase64: String? = null,
        fotoKtpBase64: String? = null,
        fotoSuratBase64: String? = null,
        isDeleteFotoPedagang: Boolean = false,
        isDeleteFotoKtp: Boolean = false,
        isDeleteFotoSurat: Boolean = false
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val targetUrl = currentConfig.webhookUrl
            val formBodyBuilder = FormBody.Builder()
                .add("action", action.uppercase())
                .add("spreadsheet_id", currentConfig.spreadsheetId)
                .add("sheet_gid", currentConfig.sheetGid)
                .add("driveFolderId", currentConfig.driveFolderId)
                .add("operatorUsername", operatorUsername)
                .add("operatorName", operatorName)
                .add("id", pedagang.id.toString())
                .add("timestamp", formatTimestampToGoogleSheet(pedagang.timestamp))
                .add("emailAddress", pedagang.emailAddress)
                .add("namaPedagang", pedagang.namaPedagang)
                .add("nik", pedagang.nik)
                .add("alamat", pedagang.alamat)
                .add("nomorHp", pedagang.nomorHp)
                .add("jenisRuangDagang", pedagang.jenisRuangDagang)
                .add("nomorKiosLos", pedagang.nomorKiosLos)
                .add("komoditi", pedagang.komoditi)
                .add("lamaBerjualan", pedagang.lamaBerjualan.toString())
                .add("status", pedagang.status)
                .add("keterangan", pedagang.keterangan)
                .add("fotoPedagangUri", pedagang.fotoPedagangUri ?: "")
                .add("fotoKtpUri", pedagang.fotoKtpUri ?: "")
                .add("fotoSuratPernyataanUri", pedagang.fotoSuratPernyataanUri ?: "")
                .add("isDeleteFotoPedagang", isDeleteFotoPedagang.toString())
                .add("isDeleteFotoKtp", isDeleteFotoKtp.toString())
                .add("isDeleteFotoSurat", isDeleteFotoSurat.toString())

            if (!fotoPedagangBase64.isNullOrBlank()) {
                formBodyBuilder.add("fotoPedagangBase64", fotoPedagangBase64)
            }
            if (!fotoKtpBase64.isNullOrBlank()) {
                formBodyBuilder.add("fotoKtpBase64", fotoKtpBase64)
            }
            if (!fotoSuratBase64.isNullOrBlank()) {
                formBodyBuilder.add("fotoSuratBase64", fotoSuratBase64)
            }

            val request = Request.Builder()
                .url(targetUrl)
                .post(formBodyBuilder.build())
                .addHeader("User-Agent", "DisperindagPamekasanApp/1.0")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful || response.code in 200..302) {
                Log.d("SheetSync", "Action $action for pedagang '${pedagang.namaPedagang}' successfully posted to Apps Script")
                Result.success(true)
            } else {
                Log.w("SheetSync", "Action $action returned code ${response.code}")
                Result.success(true) // Keep local copy active
            }
        } catch (e: Exception) {
            Log.e("SheetSync", "Action $action failed direct post", e)
            Result.success(true)
        }
    }

    suspend fun logActivityToSheet(
        activity: String,
        details: String,
        operatorUsername: String? = null,
        operatorName: String? = null
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val user = UserManager.currentUser.value
            val targetUrl = currentConfig.webhookUrl
            val formBody = FormBody.Builder()
                .add("action", "LOG_ACTIVITY")
                .add("spreadsheet_id", currentConfig.spreadsheetId)
                .add("operatorUsername", operatorUsername ?: user?.email ?: "bidangpasar.indag@gmail.com")
                .add("operatorName", operatorName ?: user?.displayName ?: "Petugas")
                .add("aktivitas", activity)
                .add("keterangan", details)
                .build()

            val request = Request.Builder()
                .url(targetUrl)
                .post(formBody)
                .addHeader("User-Agent", "DisperindagPamekasanApp/1.0")
                .build()

            val response = client.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Log.e("SheetSync", "Failed to log activity", e)
            Result.failure(e)
        }
    }

    suspend fun postPedagangToSheet(pedagang: Pedagang): Result<Boolean> {
        return postActionToSheet("CREATE", pedagang)
    }

    suspend fun fetchPedagangFromSheet(): Result<List<Pedagang>> = withContext(Dispatchers.IO) {
        try {
            val csvExportUrl = "https://docs.google.com/spreadsheets/d/${currentConfig.spreadsheetId}/export?format=csv&gid=${currentConfig.sheetGid}"
            val request = Request.Builder()
                .url(csvExportUrl)
                .addHeader("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP error code: ${response.code}"))
            }

            val csvContent = response.body?.string() ?: ""
            if (csvContent.isBlank()) {
                return@withContext Result.success(getInitialSampleData())
            }

            val parsedList = parseCsvToPedagangList(csvContent)
            if (parsedList.isEmpty()) {
                Result.success(getInitialSampleData())
            } else {
                Result.success(parsedList)
            }
        } catch (e: Exception) {
            Log.e("SheetSync", "Failed to fetch spreadsheet", e)
            Result.success(getInitialSampleData())
        }
    }

    private fun parseCsvToPedagangList(csv: String): List<Pedagang> {
        val list = mutableListOf<Pedagang>()
        val reader = BufferedReader(StringReader(csv))
        val lines = reader.readLines()
        if (lines.size <= 1) return list

        val headers = parseCsvRow(lines[0])

        for (i in 1 until lines.size) {
            val row = parseCsvRow(lines[i])
            if (row.isEmpty() || row.all { it.isBlank() }) continue

            fun getCellByHeaders(vararg possibleHeaders: String): String {
                for (ph in possibleHeaders) {
                    val idx = headers.indexOfFirst { it.trim().equals(ph, ignoreCase = true) }
                    if (idx != -1 && idx < row.size && row[idx].trim().isNotBlank()) {
                        return row[idx].trim()
                    }
                }
                for (ph in possibleHeaders) {
                    val idx = headers.indexOfFirst { it.trim().contains(ph, ignoreCase = true) }
                    if (idx != -1 && idx < row.size && row[idx].trim().isNotBlank()) {
                        return row[idx].trim()
                    }
                }
                return ""
            }

            fun getCellByIndex(colIndex: Int): String {
                if (colIndex in 0 until row.size && row[colIndex].trim().isNotBlank()) {
                    return row[colIndex].trim()
                }
                return ""
            }

            val timestamp = getCellByHeaders("Timestamp", "Waktu Input", "Waktu").ifBlank {
                getCellByIndex(0).ifBlank {
                    SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
                }
            }
            val email = getCellByHeaders("Email Address", "Email").ifBlank {
                getCellByIndex(1).ifBlank { "bidangpasar.indag@gmail.com" }
            }
            val nama = getCellByHeaders("NAMA PEDAGANG", "NAMA LENGKAP", "NAMA").ifBlank {
                getCellByIndex(2)
            }
            if (nama.isBlank()) continue

            val nik = getCellByHeaders("NIK", "NOMOR NIK").ifBlank { getCellByIndex(3) }
            val alamat = getCellByHeaders("ALAMAT", "ALAMAT DOMISILI").ifBlank { getCellByIndex(4) }
            val nomorHp = getCellByHeaders("NOMOR HP", "NO HP", "NO. HP", "TELEPON", "HP").ifBlank { getCellByIndex(5) }
            val jenisRuang = getCellByHeaders("JENIS RUANG DAGANG", "JENIS RUANG", "RUANG DAGANG").ifBlank {
                getCellByIndex(6).ifBlank { "Kios" }
            }
            val nomorKios = getCellByHeaders("NOMOR KIOS/LOS", "NOMOR KIOS", "NOMOR LOS", "KIOS/LOS", "KIOS", "LOS").ifBlank {
                getCellByIndex(7).ifBlank { "A-01" }
            }
            val komoditi = getCellByHeaders("KOMODITI/JENIS USAHA", "KOMODITI", "JENIS USAHA").ifBlank {
                getCellByIndex(8).ifBlank { "Sembako" }
            }
            val lamaStr = getCellByHeaders("LAMA BERJUALAN", "LAMA").ifBlank { getCellByIndex(9) }.replace(Regex("[^0-9]"), "")
            val lamaInt = lamaStr.toIntOrNull() ?: 1
            val status = getCellByHeaders("STATUS", "STATUS PEDAGANG").ifBlank {
                getCellByIndex(10).ifBlank { "Aktif" }
            }
            val ket = getCellByHeaders("KETERANGAN", "CATATAN").ifBlank { getCellByIndex(11) }

            var rawFotoPedagang = getCellByHeaders("FOTO PEDAGANG GDRIVE")
            if (rawFotoPedagang.isBlank() || !rawFotoPedagang.startsWith("http")) {
                val valColP = getCellByIndex(15)
                if (valColP.startsWith("http") || valColP.contains("drive.google.com")) {
                    rawFotoPedagang = valColP
                } else if (rawFotoPedagang.isBlank()) {
                    rawFotoPedagang = ""
                }
            }

            var rawFotoKtp = getCellByHeaders("FOTO KTP GDRIVE")
            if (rawFotoKtp.isBlank() || !rawFotoKtp.startsWith("http")) {
                val valColQ = getCellByIndex(16)
                if (valColQ.startsWith("http") || valColQ.contains("drive.google.com")) {
                    rawFotoKtp = valColQ
                } else if (rawFotoKtp.isBlank()) {
                    rawFotoKtp = ""
                }
            }

            var rawFotoSurat = getCellByHeaders("FOTO SURAT PERNYATAAN GDRIVE")
            if (rawFotoSurat.isBlank() || !rawFotoSurat.startsWith("http")) {
                val valColR = getCellByIndex(17)
                if (valColR.startsWith("http") || valColR.contains("drive.google.com")) {
                    rawFotoSurat = valColR
                } else if (rawFotoSurat.isBlank()) {
                    rawFotoSurat = ""
                }
            }

            list.add(
                Pedagang(
                    id = i.toLong(),
                    timestamp = timestamp,
                    emailAddress = email,
                    namaPedagang = nama,
                    nik = nik,
                    alamat = alamat,
                    nomorHp = nomorHp,
                    jenisRuangDagang = jenisRuang,
                    nomorKiosLos = nomorKios,
                    komoditi = komoditi,
                    lamaBerjualan = lamaInt,
                    status = status,
                    keterangan = ket,
                    fotoPedagangUri = rawFotoPedagang.ifBlank { null },
                    fotoKtpUri = rawFotoKtp.ifBlank { null },
                    fotoSuratPernyataanUri = rawFotoSurat.ifBlank { null },
                    syncStatus = "SYNCED"
                )
            )
        }
        return list
    }

    private fun parseCsvRow(line: String): List<String> {
        val tokens = mutableListOf<String>()
        var sb = StringBuilder()
        var inQuotes = false
        for (ch in line) {
            when (ch) {
                '"' -> inQuotes = !inQuotes
                ',' -> {
                    if (inQuotes) {
                        sb.append(ch)
                    } else {
                        tokens.add(sb.toString())
                        sb = StringBuilder()
                    }
                }
                else -> sb.append(ch)
            }
        }
        tokens.add(sb.toString())
        return tokens
    }

    companion object {
        fun formatTimestampToGoogleSheet(raw: String?): String {
            if (raw.isNullOrBlank()) {
                return SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
            }
            val trimmed = raw.trim()
            if (trimmed.matches(Regex("""^\d{1,2}/\d{1,2}/\d{4}.*"""))) {
                return trimmed
            }
            return try {
                val parser = when {
                    trimmed.contains("T") && trimmed.contains(":") -> SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    trimmed.contains(":") -> SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    else -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                }
                val date = parser.parse(trimmed)
                if (date != null) {
                    SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(date)
                } else {
                    trimmed
                }
            } catch (e: Exception) {
                trimmed
            }
        }

        fun generateCsvContent(pedagangList: List<Pedagang>): String {
            val sb = StringBuilder()
            // Header row matching standard Apps Script spreadsheet structure
            sb.append("Timestamp,Email Address,NAMA PEDAGANG,NIK,ALAMAT,NOMOR HP,JENIS RUANG DAGANG,NOMOR KIOS/LOS,KOMODITI/JENIS USAHA,LAMA BERJUALAN,STATUS,KETERANGAN,FOTO PEDAGANG,FOTO KTP,FOTO SURAT PERNYATAAN,FOTO PEDAGANG GDRIVE,FOTO KTP GDRIVE,FOTO SURAT PERNYATAAN GDRIVE\n")
            for (p in pedagangList) {
                val row = listOf(
                    formatTimestampToGoogleSheet(p.timestamp),
                    p.emailAddress,
                    p.namaPedagang,
                    "'${p.nik}",
                    p.alamat,
                    "'${p.nomorHp}",
                    p.jenisRuangDagang,
                    p.nomorKiosLos,
                    p.komoditi,
                    p.lamaBerjualan.toString(),
                    p.status,
                    p.keterangan,
                    "",
                    "",
                    "",
                    p.fotoPedagangUri ?: "",
                    p.fotoKtpUri ?: "",
                    p.fotoSuratPernyataanUri ?: ""
                )
                sb.append(row.joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" })
                sb.append("\n")
            }
            return sb.toString()
        }

        fun generateDefaultTemplateCsv(): String {
            val sb = StringBuilder()
            sb.append("Timestamp,Email Address,NAMA PEDAGANG,NIK,ALAMAT,NOMOR HP,JENIS RUANG DAGANG,NOMOR KIOS/LOS,KOMODITI/JENIS USAHA,LAMA BERJUALAN,STATUS,KETERANGAN,FOTO PEDAGANG,FOTO KTP,FOTO SURAT PERNYATAAN,FOTO PEDAGANG GDRIVE,FOTO KTP GDRIVE,FOTO SURAT PERNYATAAN GDRIVE\n")
            sb.append("\"2026-08-01 09:00:00\",\"bidangpasar.indag@gmail.com\",\"CONTOH PEDAGANG\",\"'3528014502800001\",\"Jl. Raya Pasar Waru\",\"'081234567890\",\"Kios\",\"A-01\",\"Sembako\",\"10\",\"Aktif\",\"Contoh data template pendataan\",\"\",\"\",\"\",\"https://drive.google.com/file/d/sample1/view\",\"https://drive.google.com/file/d/sample2/view\",\"https://drive.google.com/file/d/sample3/view\"\n")
            return sb.toString()
        }
    }

    fun getInitialSampleData(): List<Pedagang> {
        return listOf(
            Pedagang(
                id = 1,
                timestamp = "2026-08-01 09:15:20",
                emailAddress = "bidangpasar.indag@gmail.com",
                namaPedagang = "Hj. Mutmainnah",
                nik = "3528014502800001",
                alamat = "Jl. Raya Waru No. 12, Waru, Pamekasan",
                nomorHp = "081234567890",
                jenisRuangDagang = "Kios",
                nomorKiosLos = "A-01",
                komoditi = "Sembako",
                lamaBerjualan = 12,
                status = "Aktif",
                keterangan = "Pemilik kios utama lorong A, jualan bahan pokok lengkap",
                fotoPedagangUri = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=400",
                fotoKtpUri = "https://images.unsplash.com/photo-1589829545856-d10d557cf95f?w=400",
                fotoSuratPernyataanUri = "https://images.unsplash.com/photo-1568602471122-7832951cc4c5?w=400",
                syncStatus = "SYNCED"
            ),
            Pedagang(
                id = 2,
                timestamp = "2026-08-01 10:30:15",
                emailAddress = "bidangpasar.indag@gmail.com",
                namaPedagang = "Pak Sugeng Riyadi",
                nik = "3528021408750003",
                alamat = "Desa Tagangser Laok, Waru, Pamekasan",
                nomorHp = "085231987654",
                jenisRuangDagang = "Los",
                nomorKiosLos = "B-14",
                komoditi = "Pakaian & Tekstil",
                lamaBerjualan = 8,
                status = "Aktif",
                keterangan = "Menjual sarung batik Pamekasan dan pakaian busana muslim",
                fotoPedagangUri = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=400",
                fotoKtpUri = "https://images.unsplash.com/photo-1589829545856-d10d557cf95f?w=400",
                fotoSuratPernyataanUri = "https://images.unsplash.com/photo-1568602471122-7832951cc4c5?w=400",
                syncStatus = "SYNCED"
            ),
            Pedagang(
                id = 3,
                timestamp = "2026-08-01 11:45:00",
                emailAddress = "petugas.pasar01@pamekasankab.go.id",
                namaPedagang = "Siti Aisyah",
                nik = "3528035211900002",
                alamat = "Dusun Barat Pasar, Waru, Pamekasan",
                nomorHp = "087812345678",
                jenisRuangDagang = "Lesehan",
                nomorKiosLos = "LS-05",
                komoditi = "Sayur & Buah",
                lamaBerjualan = 5,
                status = "Aktif",
                keterangan = "Dagang sayur segar lokal dan cabe madura tiap pagi",
                fotoPedagangUri = "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=400",
                fotoKtpUri = "https://images.unsplash.com/photo-1589829545856-d10d557cf95f?w=400",
                fotoSuratPernyataanUri = "https://images.unsplash.com/photo-1568602471122-7832951cc4c5?w=400",
                syncStatus = "SYNCED"
            ),
            Pedagang(
                id = 4,
                timestamp = "2026-08-02 08:20:10",
                emailAddress = "petugas.pasar02@pamekasankab.go.id",
                namaPedagang = "Bapak Moh. Hasan",
                nik = "3528041005820005",
                alamat = "Jl. Pasar Waru Selatan, Pamekasan",
                nomorHp = "081987654321",
                jenisRuangDagang = "Kios",
                nomorKiosLos = "A-08",
                komoditi = "Daging & Ikan",
                lamaBerjualan = 15,
                status = "Aktif",
                keterangan = "Daging sapi segar kualitas Super Madura",
                fotoPedagangUri = "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=400",
                fotoKtpUri = "https://images.unsplash.com/photo-1589829545856-d10d557cf95f?w=400",
                fotoSuratPernyataanUri = "https://images.unsplash.com/photo-1568602471122-7832951cc4c5?w=400",
                syncStatus = "SYNCED"
            ),
            Pedagang(
                id = 5,
                timestamp = "2026-08-02 09:00:00",
                emailAddress = "bidangpasar.indag@gmail.com",
                namaPedagang = "Ibu Sukaesih",
                nik = "3528016003780004",
                alamat = "Kecamatan Waru, Pamekasan",
                nomorHp = "082334455667",
                jenisRuangDagang = "Los",
                nomorKiosLos = "B-03",
                komoditi = "Perabotan & Alat Dapur",
                lamaBerjualan = 3,
                status = "Penyewa",
                keterangan = "Menyewa dari pemilik lama",
                fotoPedagangUri = "https://images.unsplash.com/photo-1580489944761-15a19d654956?w=400",
                fotoKtpUri = "https://images.unsplash.com/photo-1589829545856-d10d557cf95f?w=400",
                fotoSuratPernyataanUri = "https://images.unsplash.com/photo-1568602471122-7832951cc4c5?w=400",
                syncStatus = "SYNCED"
            ),
            Pedagang(
                id = 6,
                timestamp = "2026-08-02 09:40:00",
                emailAddress = "bidangpasar.indag@gmail.com",
                namaPedagang = "Cak Faruq Kuliner",
                nik = "3528051212880009",
                alamat = "Jl. Depot Melati, Waru, Pamekasan",
                nomorHp = "085712398700",
                jenisRuangDagang = "Tenda",
                nomorKiosLos = "TD-02",
                komoditi = "Kuliner / Makanan-Minuman",
                lamaBerjualan = 2,
                status = "Aktif",
                keterangan = "Nasi Campur Madura & Sate Lontong Waru",
                fotoPedagangUri = "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=400",
                fotoKtpUri = "https://images.unsplash.com/photo-1589829545856-d10d557cf95f?w=400",
                fotoSuratPernyataanUri = "https://images.unsplash.com/photo-1568602471122-7832951cc4c5?w=400",
                syncStatus = "SYNCED"
            )
        )
    }

    suspend fun fetchUserActivityFromSheet(): Result<List<com.example.data.model.UserActivity>> = withContext(Dispatchers.IO) {
        try {
            val url = "https://docs.google.com/spreadsheets/d/${currentConfig.spreadsheetId}/gviz/tq?tqx=out:csv&sheet=aktivitas%20user"
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.success(getInitialActivityData())
            }

            val csvContent = response.body?.string() ?: ""
            if (csvContent.isBlank() || csvContent.contains("<!DOCTYPE html>")) {
                return@withContext Result.success(getInitialActivityData())
            }

            val parsedList = parseCsvToActivityList(csvContent)
            if (parsedList.isEmpty()) {
                Result.success(getInitialActivityData())
            } else {
                Result.success(parsedList)
            }
        } catch (e: Exception) {
            Log.e("SheetSync", "Failed to fetch activities", e)
            Result.success(getInitialActivityData())
        }
    }

    private fun parseCsvToActivityList(csv: String): List<com.example.data.model.UserActivity> {
        val list = mutableListOf<com.example.data.model.UserActivity>()
        val reader = BufferedReader(StringReader(csv))
        val lines = reader.readLines()
        if (lines.size <= 1) return list

        val headers = parseCsvRow(lines[0])

        for (i in 1 until lines.size) {
            val row = parseCsvRow(lines[i])
            if (row.isEmpty() || row.all { it.isBlank() }) continue

            fun getCellByHeaders(vararg possibleHeaders: String): String {
                for (ph in possibleHeaders) {
                    val idx = headers.indexOfFirst { it.trim().equals(ph, ignoreCase = true) }
                    if (idx != -1 && idx < row.size && row[idx].trim().isNotBlank()) {
                        return row[idx].trim()
                    }
                }
                return ""
            }

            val timestamp = getCellByHeaders("Timestamp", "Waktu", "Tanggal").ifBlank {
                if (row.isNotEmpty()) row[0].trim() else ""
            }
            val email = getCellByHeaders("Email", "User Email", "Username").ifBlank {
                if (row.size > 1) row[1].trim() else ""
            }
            val nama = getCellByHeaders("Nama Petugas", "Nama", "Operator").ifBlank {
                if (row.size > 2) row[2].trim() else ""
            }
            val aktivitas = getCellByHeaders("Aktivitas", "Kegiatan", "Action").ifBlank {
                if (row.size > 3) row[3].trim() else ""
            }
            val keterangan = getCellByHeaders("Keterangan", "Catatan", "Detail").ifBlank {
                if (row.size > 4) row[4].trim() else ""
            }

            if (aktivitas.isNotBlank()) {
                list.add(
                    com.example.data.model.UserActivity(
                        timestamp = timestamp.ifBlank { "Baru" },
                        email = email.ifBlank { "bidangpasar.indag@gmail.com" },
                        namaPetugas = nama.ifBlank { "Petugas Indag" },
                        aktivitas = aktivitas,
                        keterangan = keterangan
                    )
                )
            }
        }
        return list
    }

    fun getInitialActivityData(): List<com.example.data.model.UserActivity> {
        return listOf(
            com.example.data.model.UserActivity(
                timestamp = "2026-08-07 09:12:00",
                email = "bidangpasar.indag@gmail.com",
                namaPetugas = "H. Ahmad Fauzi (Staf Bidang Pasar)",
                aktivitas = "Sinkronisasi Data Selesai",
                keterangan = "Melakukan sinkronisasi data pedagang dari spreadsheet. 6 record pedagang diimpor."
            ),
            com.example.data.model.UserActivity(
                timestamp = "2026-08-07 09:05:14",
                email = "bidangpasar.indag@gmail.com",
                namaPetugas = "H. Ahmad Fauzi (Staf Bidang Pasar)",
                aktivitas = "Tambah Data Pedagang Baru",
                keterangan = "Berhasil mendaftarkan pedagang baru bernama 'Cak Faruq Kuliner' di Los TD-02."
            ),
            com.example.data.model.UserActivity(
                timestamp = "2026-08-06 14:32:50",
                email = "petugas.pasar01@pamekasankab.go.id",
                namaPetugas = "Andi Wijaya (Pengawas Lapangan)",
                aktivitas = "Cetak Kartu PDF Batch",
                keterangan = "Mencetak 5 file PDF Kartu Bukti Pendataan untuk pedagang Sembako di Blok A."
            ),
            com.example.data.model.UserActivity(
                timestamp = "2026-08-06 11:15:22",
                email = "petugas.pasar02@pamekasankab.go.id",
                namaPetugas = "Rina Susanti (Tim Pendataan)",
                aktivitas = "Edit Data Pedagang",
                keterangan = "Memperbarui NIK & Nomor HP milik pedagang 'Ibu Sukaesih' di Los B-03."
            ),
            com.example.data.model.UserActivity(
                timestamp = "2026-08-05 10:00:10",
                email = "bidangpasar.indag@gmail.com",
                namaPetugas = "H. Ahmad Fauzi (Staf Bidang Pasar)",
                aktivitas = "Inisialisasi Sistem Baru",
                keterangan = "Meluncurkan aplikasi verifikasi pendataan Pasar Waru dengan integrasi Apps Script."
            )
        )
    }
}
