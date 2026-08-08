package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.auth.UserManager
import com.example.data.local.AppDatabase
import com.example.data.model.Pedagang
import com.example.data.repository.PedagangRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class FormState(
    val id: Long = 0,
    val timestamp: String = "",
    val emailAddress: String = "",
    val namaPedagang: String = "",
    val nik: String = "",
    val alamat: String = "",
    val nomorHp: String = "",
    val jenisRuangDagang: String = "",
    val nomorKiosLos: String = "",
    val komoditi: String = "",
    val lamaBerjualanStr: String = "1",
    val status: String = "Aktif",
    val keterangan: String = "",
    val fotoPedagangUri: String? = null,
    val fotoKtpUri: String? = null,
    val fotoSuratPernyataanUri: String? = null,
    // Validation Errors
    val namaError: String? = null,
    val nikError: String? = null,
    val hpError: String? = null,
    val jenisRuangError: String? = null,
    val nomorKiosError: String? = null,
    val komoditiError: String? = null,
    val lamaBerjualanError: String? = null,
    val statusError: String? = null
)

enum class GroupByMode {
    NONE, JENIS_RUANG, KOMODITI, STATUS, TIM_PENDATA
}

data class UiMessage(val message: String, val isError: Boolean = false)

class PedagangViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PedagangRepository

    init {
        val db = AppDatabase.getDatabase(application)
        repository = PedagangRepository(application, db.pedagangDao(), db.dropdownDao())
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
    }

    // Search Query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // Grouping Mode
    private val _groupByMode = MutableStateFlow(GroupByMode.NONE)
    val groupByMode = _groupByMode.asStateFlow()

    // Pedagang List with Search
    val pedagangList: StateFlow<List<Pedagang>> = searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) repository.allPedagang
            else repository.searchPedagang(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dropdown Options
    val jenisRuangOptions: StateFlow<List<String>> = repository.getOptionsByCategory("JENIS_RUANG")
        .map { list -> list.map { it.optionValue } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val komoditiOptions: StateFlow<List<String>> = repository.getOptionsByCategory("KOMODITI")
        .map { list -> list.map { it.optionValue } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val statusOptions: StateFlow<List<String>> = repository.getOptionsByCategory("STATUS")
        .map { list -> list.map { it.optionValue } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected Pedagang for Detail
    private val _selectedPedagang = MutableStateFlow<Pedagang?>(null)
    val selectedPedagang = _selectedPedagang.asStateFlow()

    // Lightbox image viewer URL
    private val _previewImageUrl = MutableStateFlow<String?>(null)
    val previewImageUrl = _previewImageUrl.asStateFlow()

    // Form State for Add / Edit
    private val _formState = MutableStateFlow(FormState())
    val formState = _formState.asStateFlow()

        private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()

    // Sync & UI Status
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    private val _lastSyncTime = MutableStateFlow("Belum disinkronkan")
    val lastSyncTime = _lastSyncTime.asStateFlow()

    private val _uiMessage = MutableStateFlow<UiMessage?>(null)
    val uiMessage = _uiMessage.asStateFlow()

    // User Activities State
    private val _userActivities = MutableStateFlow<List<com.example.data.model.UserActivity>>(emptyList())
    val userActivities = _userActivities.asStateFlow()

    private val _isSyncingActivities = MutableStateFlow(false)
    val isSyncingActivities = _isSyncingActivities.asStateFlow()

    fun fetchUserActivities() {
        viewModelScope.launch {
            _isSyncingActivities.value = true
            val syncService = com.example.data.remote.GoogleSheetSyncService()
            val result = syncService.fetchUserActivityFromSheet()
            if (result.isSuccess) {
                _userActivities.value = result.getOrDefault(syncService.getInitialActivityData())
            } else {
                _userActivities.value = syncService.getInitialActivityData()
            }
            _isSyncingActivities.value = false
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setGroupByMode(mode: GroupByMode) {
        _groupByMode.value = mode
    }

    fun selectPedagang(pedagang: Pedagang?) {
        _selectedPedagang.value = pedagang
    }

    fun setPreviewImageUrl(url: String?) {
        _previewImageUrl.value = url
    }

    fun clearUiMessage() {
        _uiMessage.value = null
    }

    // Prepare New Form
    fun initNewForm() {
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val userEmail = UserManager.currentUser.value?.email ?: "bidangpasar.indag@gmail.com"
        _formState.value = FormState(
            timestamp = now,
            emailAddress = userEmail,
            jenisRuangDagang = jenisRuangOptions.value.firstOrNull() ?: "Kios",
            komoditi = komoditiOptions.value.firstOrNull() ?: "Sembako",
            status = statusOptions.value.firstOrNull() ?: "Aktif"
        )
    }

    // Prepare Edit Form
    fun initEditForm(pedagang: Pedagang) {
        _formState.value = FormState(
            id = pedagang.id,
            timestamp = pedagang.timestamp,
            emailAddress = pedagang.emailAddress,
            namaPedagang = pedagang.namaPedagang,
            nik = pedagang.nik,
            alamat = pedagang.alamat,
            nomorHp = pedagang.nomorHp,
            jenisRuangDagang = pedagang.jenisRuangDagang,
            nomorKiosLos = pedagang.nomorKiosLos,
            komoditi = pedagang.komoditi,
            lamaBerjualanStr = pedagang.lamaBerjualan.toString(),
            status = pedagang.status,
            keterangan = pedagang.keterangan,
            fotoPedagangUri = pedagang.fotoPedagangUri,
            fotoKtpUri = pedagang.fotoKtpUri,
            fotoSuratPernyataanUri = pedagang.fotoSuratPernyataanUri
        )
    }

    fun updateForm(update: (FormState) -> FormState) {
        _formState.value = update(_formState.value)
    }

        fun rebuildDropdownOptions() {
        viewModelScope.launch {
            repository.rebuildDropdownOptions()
            _uiMessage.value = UiMessage("Opsi dropdown berhasil diperbarui sesuai data saat ini")
        }
    }

    fun addCustomOption(category: String, value: String) {
        viewModelScope.launch {
            repository.addDropdownOption(category, value)
            _uiMessage.value = UiMessage("Opsi dropdown baru '$value' berhasil ditambahkan")
        }
    }

    // Validate and Save Pedagang Form
    fun savePedagang(onSuccess: () -> Unit) {
        val f = _formState.value

        var isValid = true

        val namaErr = if (f.namaPedagang.isBlank()) "Nama pedagang wajib diisi" else null
        if (namaErr != null) isValid = false

        val nikErr = if (f.nik.isNotBlank() && (!f.nik.all { it.isDigit() } || f.nik.length != 16)) {
            "NIK harus berupa 16 digit angka"
        } else null
        if (nikErr != null) isValid = false

        val hpErr = if (f.nomorHp.isNotBlank()) {
            if (!f.nomorHp.startsWith("0")) "Nomor HP harus dimulai dari 0"
            else if (!f.nomorHp.all { it.isDigit() }) "Nomor HP harus berupa angka"
            else if (f.nomorHp.length < 9 || f.nomorHp.length > 13) "Nomor HP harus 9 - 13 digit"
            else null
        } else null
        if (hpErr != null) isValid = false

        val jenisRuangErr = if (f.jenisRuangDagang.isBlank()) "Jenis ruang dagang wajib dipilih" else null
        if (jenisRuangErr != null) isValid = false

        val nomorKiosErr = if (f.nomorKiosLos.isBlank()) "Nomor Kios/Los wajib diisi" else null
        if (nomorKiosErr != null) isValid = false

        val komoditiErr = if (f.komoditi.isBlank()) "Komoditi / Jenis Usaha wajib dipilih" else null
        if (komoditiErr != null) isValid = false

        val lamaInt = f.lamaBerjualanStr.toIntOrNull()
        val lamaErr = if (lamaInt == null || lamaInt < 0) "Lama berjualan harus berupa angka tahun (min 0)" else null
        if (lamaErr != null) isValid = false

        val statusErr = if (f.status.isBlank()) "Status wajib dipilih" else null
        if (statusErr != null) isValid = false

        _formState.value = f.copy(
            namaError = namaErr,
            nikError = nikErr,
            hpError = hpErr,
            jenisRuangError = jenisRuangErr,
            nomorKiosError = nomorKiosErr,
            komoditiError = komoditiErr,
            lamaBerjualanError = lamaErr,
            statusError = statusErr
        )

        if (!isValid) {
            _uiMessage.value = UiMessage("Mohon periksa input form yang wajib diisi", isError = true)
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            val pedagang = Pedagang(
                id = f.id,
                timestamp = if (f.timestamp.isBlank()) SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()) else f.timestamp,
                emailAddress = if (f.emailAddress.isBlank()) (UserManager.currentUser.value?.email ?: "bidangpasar.indag@gmail.com") else f.emailAddress,
                namaPedagang = f.namaPedagang.trim(),
                nik = f.nik.trim(),
                alamat = f.alamat.trim(),
                nomorHp = f.nomorHp.trim(),
                jenisRuangDagang = f.jenisRuangDagang.trim(),
                nomorKiosLos = f.nomorKiosLos.trim(),
                komoditi = f.komoditi.trim(),
                lamaBerjualan = lamaInt!!,
                status = f.status.trim(),
                keterangan = f.keterangan.trim(),
                fotoPedagangUri = f.fotoPedagangUri,
                fotoKtpUri = f.fotoKtpUri,
                fotoSuratPernyataanUri = f.fotoSuratPernyataanUri,
                syncStatus = "SYNCED"
            )

            if (f.id == 0L) {
                repository.insertPedagang(pedagang)
                val nowStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                _lastSyncTime.value = "Terakhir: $nowStr (Sync Otomatis)"
                _uiMessage.value = UiMessage("Data pedagang '${pedagang.namaPedagang}' berhasil disimpan & langsung disinkronkan ke Google Spreadsheet")
            } else {
                repository.updatePedagang(pedagang)
                val nowStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                _lastSyncTime.value = "Terakhir: $nowStr (Sync Otomatis)"
                _uiMessage.value = UiMessage("Data pedagang '${pedagang.namaPedagang}' berhasil diperbarui & disinkronkan ke Google Spreadsheet")
            }

            if (_selectedPedagang.value?.id == pedagang.id) {
                _selectedPedagang.value = pedagang
            }

            _isSaving.value = false
            onSuccess()
        }
    }

    fun deletePedagang(pedagang: Pedagang) {
        viewModelScope.launch {
            repository.deletePedagang(pedagang)
            if (_selectedPedagang.value?.id == pedagang.id) {
                _selectedPedagang.value = null
            }
            _uiMessage.value = UiMessage("Data pedagang '${pedagang.namaPedagang}' telah dihapus")
        }
    }

    fun syncWithSpreadsheet() {
        viewModelScope.launch {
            _isSyncing.value = true
            val res = repository.syncWithSpreadsheet()
            _isSyncing.value = false

            if (res.isSuccess) {
                val count = res.getOrDefault(0)
                val nowStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                _lastSyncTime.value = "Terakhir: $nowStr ($count data)"
                _uiMessage.value = UiMessage("Berhasil sinkronisasi $count data pedagang dari Spreadsheet")
            } else {
                _uiMessage.value = UiMessage("Gagal koneksi ke Spreadsheet, menggunakan cache lokal", isError = true)
            }
        }
    }
}
