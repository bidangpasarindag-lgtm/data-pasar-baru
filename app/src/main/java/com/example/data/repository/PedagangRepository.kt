package com.example.data.repository

import com.example.data.local.DropdownDao
import com.example.data.local.PedagangDao
import com.example.data.model.DropdownOption
import com.example.data.model.Pedagang
import com.example.data.remote.GoogleSheetSyncService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class PedagangRepository(
    private val context: android.content.Context,
    private val pedagangDao: PedagangDao,
    private val dropdownDao: DropdownDao,
    private val syncService: GoogleSheetSyncService = GoogleSheetSyncService()
) {
    val allPedagang: Flow<List<Pedagang>> = pedagangDao.getAllPedagang()

    fun searchPedagang(query: String): Flow<List<Pedagang>> = pedagangDao.searchPedagang(query)

    suspend fun getPedagangById(id: Long): Pedagang? = pedagangDao.getPedagangById(id)

    private fun uriToBase64(uriString: String?): String? {
        if (uriString.isNullOrBlank()) return null
        if (uriString.startsWith("http://") || uriString.startsWith("https://")) return null
        try {
            val uri = android.net.Uri.parse(uriString)
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bytes = inputStream.readBytes()
            inputStream.close()
            return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            android.util.Log.e("PedagangRepository", "Gagal konversi URI ke Base64: $uriString", e)
            return null
        }
    }

    suspend fun insertPedagang(pedagang: Pedagang): Long {
        val id = pedagangDao.insertPedagang(pedagang)
        val updatedPedagang = pedagang.copy(id = id)
        
        val fotoPedagangBase64 = uriToBase64(updatedPedagang.fotoPedagangUri)
        val fotoKtpBase64 = uriToBase64(updatedPedagang.fotoKtpUri)
        val fotoSuratBase64 = uriToBase64(updatedPedagang.fotoSuratPernyataanUri)

        syncService.postActionToSheet(
            action = "CREATE",
            pedagang = updatedPedagang,
            fotoPedagangBase64 = fotoPedagangBase64,
            fotoKtpBase64 = fotoKtpBase64,
            fotoSuratBase64 = fotoSuratBase64
        )
        return id
    }

    suspend fun updatePedagang(pedagang: Pedagang) {
        val oldPedagang = pedagangDao.getPedagangById(pedagang.id)
        pedagangDao.updatePedagang(pedagang)
        
        val isDeleteFotoPedagang = !oldPedagang?.fotoPedagangUri.isNullOrBlank() && pedagang.fotoPedagangUri.isNullOrBlank()
        val isDeleteFotoKtp = !oldPedagang?.fotoKtpUri.isNullOrBlank() && pedagang.fotoKtpUri.isNullOrBlank()
        val isDeleteFotoSurat = !oldPedagang?.fotoSuratPernyataanUri.isNullOrBlank() && pedagang.fotoSuratPernyataanUri.isNullOrBlank()

        val fotoPedagangBase64 = uriToBase64(pedagang.fotoPedagangUri)
        val fotoKtpBase64 = uriToBase64(pedagang.fotoKtpUri)
        val fotoSuratBase64 = uriToBase64(pedagang.fotoSuratPernyataanUri)

        syncService.postActionToSheet(
            action = "UPDATE",
            pedagang = pedagang,
            fotoPedagangBase64 = fotoPedagangBase64,
            fotoKtpBase64 = fotoKtpBase64,
            fotoSuratBase64 = fotoSuratBase64,
            isDeleteFotoPedagang = isDeleteFotoPedagang,
            isDeleteFotoKtp = isDeleteFotoKtp,
            isDeleteFotoSurat = isDeleteFotoSurat
        )
    }

    suspend fun sendPedagangToSpreadsheet(pedagang: Pedagang): Result<Boolean> {
        val fotoPedagangBase64 = uriToBase64(pedagang.fotoPedagangUri)
        val fotoKtpBase64 = uriToBase64(pedagang.fotoKtpUri)
        val fotoSuratBase64 = uriToBase64(pedagang.fotoSuratPernyataanUri)

        return syncService.postActionToSheet(
            action = "CREATE",
            pedagang = pedagang,
            fotoPedagangBase64 = fotoPedagangBase64,
            fotoKtpBase64 = fotoKtpBase64,
            fotoSuratBase64 = fotoSuratBase64
        )
    }

    suspend fun deletePedagang(pedagang: Pedagang) {
        pedagangDao.deletePedagang(pedagang)
        syncService.postActionToSheet(
            action = "DELETE",
            pedagang = pedagang,
            isDeleteFotoPedagang = !pedagang.fotoPedagangUri.isNullOrBlank(),
            isDeleteFotoKtp = !pedagang.fotoKtpUri.isNullOrBlank(),
            isDeleteFotoSurat = !pedagang.fotoSuratPernyataanUri.isNullOrBlank()
        )
    }

    suspend fun deleteById(id: Long) {
        val p = pedagangDao.getPedagangById(id)
        if (p != null) {
            deletePedagang(p)
        } else {
            pedagangDao.deleteById(id)
        }
    }

    // Dynamic Dropdowns
    fun getOptionsByCategory(category: String): Flow<List<DropdownOption>> =
        dropdownDao.getOptionsByCategory(category)

    suspend fun addDropdownOption(category: String, value: String) {
        if (value.isNotBlank()) {
            dropdownDao.insertOption(DropdownOption(category = category, optionValue = value.trim()))
        }
    }

    suspend fun seedInitialDataIfEmpty() {
        val currentPedagang = pedagangDao.getAllPedagang().first()
        if (currentPedagang.isEmpty()) {
            val sampleData = syncService.getInitialSampleData()
            pedagangDao.insertAll(sampleData)
        }

        seedDefaultDropdownOptions()
    }

    suspend fun rebuildDropdownOptions() {
        // Clear all options
        dropdownDao.clearAll()
        
        // Add new defaults
        val defaultJenisRuang = listOf("TOKO", "KIOS", "SWADAYA", "LOS", "HAMPARAN")
        val defaultKomoditi = listOf(
            "AKSESORIS", "ARLOGI", "BUAH", "DAGING AYAM", "DAGING KAMBING/SAPI",
            "ELEKTRONIK", "BUKU / KITAB", "IKAN LAUT", "JAMU", "KONVEKSI", "MERACANG",
            "MAMIN", "MAINAN ANAK", "PECAH BELAH", "PLASTIK", "SELIP DAGING", "SEPATU / SANDAL", "SEPEDA", "SNACK", "SONGKOK"
        )
        val defaultStatus = listOf("PEMILIK HAK PAKAI", "SEWA", "BELI", "MILIK KELUARGA", "PENJAGA", "TUTUP", "TAMBAHAN")

        defaultJenisRuang.forEach { addDropdownOption("JENIS_RUANG", it) }
        defaultKomoditi.forEach { addDropdownOption("KOMODITI", it) }
        defaultStatus.forEach { addDropdownOption("STATUS", it) }
        
        // Add existing options from pedagang data
        val allPedagang = pedagangDao.getAllPedagang().first()
        allPedagang.forEach { pedagang ->
            addDropdownOption("JENIS_RUANG", pedagang.jenisRuangDagang)
            addDropdownOption("KOMODITI", pedagang.komoditi)
            addDropdownOption("STATUS", pedagang.status)
        }
    }
    
    private suspend fun seedDefaultDropdownOptions() {
        // Re-use rebuild method here to ensure consistency
        rebuildDropdownOptions()
    }

    suspend fun syncWithSpreadsheet(): Result<Int> {
        val result = syncService.fetchPedagangFromSheet()
        return if (result.isSuccess) {
            val remoteList = result.getOrDefault(emptyList())
                        if (remoteList.isNotEmpty()) {
                pedagangDao.clearAll()
                pedagangDao.insertAll(remoteList)
            }
            rebuildDropdownOptions()
            Result.success(remoteList.size)
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
        }
    }

    suspend fun exportToCsvString(): String {
        val list = pedagangDao.getAllPedagang().first()
        val sb = StringBuilder()
        sb.append("Timestamp,Email Address,NAMA PEDAGANG,NIK,ALAMAT,NOMOR HP,JENIS RUANG DAGANG,NOMOR KIOS/LOS,KOMODITI/JENIS USAHA,LAMA BERJUALAN,STATUS,KETERANGAN,FOTO PEDAGANG,FOTO KTP,FOTO SURAT PERNYATAAN\n")

        for (p in list) {
            sb.append("\"${p.timestamp}\",")
            sb.append("\"${p.emailAddress}\",")
            sb.append("\"${p.namaPedagang.replace("\"", "\"\"")}\",")
            sb.append("\"${p.nik}\",")
            sb.append("\"${p.alamat.replace("\"", "\"\"")}\",")
            sb.append("\"${p.nomorHp}\",")
            sb.append("\"${p.jenisRuangDagang}\",")
            sb.append("\"${p.nomorKiosLos}\",")
            sb.append("\"${p.komoditi}\",")
            sb.append("${p.lamaBerjualan},")
            sb.append("\"${p.status}\",")
            sb.append("\"${p.keterangan.replace("\"", "\"\"")}\",")
            sb.append("\"${p.fotoPedagangUri ?: ""}\",")
            sb.append("\"${p.fotoKtpUri ?: ""}\",")
            sb.append("\"${p.fotoSuratPernyataanUri ?: ""}\"\n")
        }
        return sb.toString()
    }
}
