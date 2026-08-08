import re

with open('app/src/main/java/com/example/data/repository/PedagangRepository.kt', 'r') as f:
    content = f.read()

# Replace seedDefaultDropdownOptions
old_seed = """    private suspend fun seedDefaultDropdownOptions() {
        val defaultJenisRuang = listOf("Kios", "Los", "Lesehan", "Tenda", "Lapak", "Portable")
        val defaultKomoditi = listOf(
            "Sembako",
            "Pakaian & Tekstil",
            "Sayur & Buah",
            "Daging & Ikan",
            "Perabotan & Alat Dapur",
            "Kuliner / Makanan-Minuman",
            "Jasa & Kerajinan",
            "Lainnya"
        )
        val defaultStatus = listOf("Aktif", "Tutup Sementara", "Pemilik", "Penyewa", "Hak Pakai")

        defaultJenisRuang.forEach { addDropdownOption("JENIS_RUANG", it) }
        defaultKomoditi.forEach { addDropdownOption("KOMODITI", it) }
        defaultStatus.forEach { addDropdownOption("STATUS", it) }
    }"""

new_seed = """    suspend fun rebuildDropdownOptions() {
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
            addDropdownOption("JENIS_RUANG", pedagang.jenisRuang)
            addDropdownOption("KOMODITI", pedagang.komoditi)
            addDropdownOption("STATUS", pedagang.status)
        }
    }
    
    private suspend fun seedDefaultDropdownOptions() {
        // Re-use rebuild method here to ensure consistency
        rebuildDropdownOptions()
    }"""

content = content.replace(old_seed, new_seed)

# In syncWithSpreadsheet, after insertAll(remoteList), call rebuildDropdownOptions
sync_block = """            if (remoteList.isNotEmpty()) {
                pedagangDao.clearAll()
                pedagangDao.insertAll(remoteList)
            }
            rebuildDropdownOptions()
            Result.success(remoteList.size)"""
content = re.sub(r'if \(remoteList\.isNotEmpty\(\)\) \{\s*pedagangDao\.clearAll\(\)\s*pedagangDao\.insertAll\(remoteList\)\s*\}\s*Result\.success\(remoteList\.size\)', sync_block, content)

with open('app/src/main/java/com/example/data/repository/PedagangRepository.kt', 'w') as f:
    f.write(content)
