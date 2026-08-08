package com.example.data.config

import android.content.Context
import com.example.data.model.Pedagang
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

data class AgencyConfig(
    // Identitas Dinas & Kop Surat
    val namaPemerintah: String = "PEMERINTAH KABUPATEN PAMEKASAN",
    val namaDinas: String = "DINAS PERINDUSTRIAN DAN PERDAGANGAN",
    val appTitleHeader: String = "DISPERINDAG PAMEKASAN",
    val appLoginTitle: String = "SI-PENDATA PASAR",
    val namaPasar: String = "PASAR WARU KABUPATEN PAMEKASAN",
    val alamatDinas: String = "Jalan Jokotole Nomor 199 Pamekasan 69322",
    val teleponFax: String = "Telepon/Fax (0324) 321497",
    val websiteLaman: String = "https://disperindag.pamekasankab.go.id/",
    val posElEmail: String = "disperindag@pamekasankab.go.id",
    val customLogoUri: String = "",

    // Pengaturan Spreadsheet, Drive & Webhook
    val spreadsheetId: String = "1Q7OtJ1fuEwkycAtnAjRNrSNrAEJ5SxjGRn-ge9YcWlU",
    val sheetGid: String = "1751220302",
    val driveFolderId: String = "1G81CN0555Gst93hIosHG0lrUf4pP0ELO",
    val webhookUrl: String = "https://script.google.com/macros/s/AKfycbzyIKCqNpmbhAxgbTWDPXwzZ1CyTgl8C_28CBtJkaoTQwXGHa7v2rdDFiLzkBrW7kjQ/exec",

    // Syarat Kelengkapan Data Pedagang (Dinamis)
    val enableCompletenessWarning: Boolean = true,
    val requireNik: Boolean = true,
    val requireFotoPedagang: Boolean = true,
    val requireFotoKtp: Boolean = true,
    val requireFotoSurat: Boolean = true,
    val requireNomorHp: Boolean = true,
    val requireAlamat: Boolean = true,
    val requireKomoditi: Boolean = true,

    // Pengaturan PDF (Tampilan, Font, Visibilitas)
    val pdfPaperSize: String = "F5 Landscape",
    val pdfFontFamily: String = "Sans-Serif",
    val pdfHeaderFontSize: Int = 12,
    val pdfBodyFontSize: Int = 10,
    val pdfHeaderTitle: String = "PEMERINTAH KABUPATEN PAMEKASAN",
    val pdfHeaderSubtitle: String = "DINAS PERINDUSTRIAN DAN PERDAGANGAN",
    val pdfHeaderAddress: String = "Jalan Jokotole Nomor 199 Pamekasan 69322",
    val pdfFontSize: Float = 12f,
    val pdfShowLogo: Boolean = true,
    val pdfShowKopSurat: Boolean = true,
    val pdfShowQrCode: Boolean = true,
    val pdfShowFotoPedagang: Boolean = true,
    val pdfShowFotoKtp: Boolean = true,
    val pdfShowFotoSurat: Boolean = true,
    val pdfShowSignatureBlock: Boolean = true,
    val pdfShowPetugas: Boolean = true,
    val pdfTitleText: String = "",
    val pdfFooterNote: String = "Bukti Pendataan Resmi - Dinas Perindustrian dan Perdagangan Kabupaten Pamekasan",
    val pdfShowQr: Boolean = true,
    val pdfShowVerificationStatus: Boolean = true,
    val pdfFooterText: String = "Bukti Pendataan Resmi - Dinas Perindustrian dan Perdagangan Kabupaten Pamekasan",
    val pdfPejabatNama: String = "Kepala Dinas Perindustrian dan Perdagangan",
    val pdfPejabatNip: String = "19780512 200312 1 002",
    val pdfShowNik: Boolean = true,
    val pdfShowHp: Boolean = true,
    val pdfShowAlamat: Boolean = true,
    val pdfShowKomoditi: Boolean = true,
    val pdfShowStatus: Boolean = true,
    val pdfShowLama: Boolean = true,

    // Visibilitas Atribut Form Tambah/Edit Pedagang
    val formShowNik: Boolean = true,
    val formShowAlamat: Boolean = true,
    val formShowHp: Boolean = true,
    val formShowJenisRuang: Boolean = true,
    val formShowNomorKios: Boolean = true,
    val formShowKomoditi: Boolean = true,
    val formShowLamaBerjualan: Boolean = true,
    val formShowStatus: Boolean = true,
    val formShowKeterangan: Boolean = true,
    val formShowFotoPedagang: Boolean = true,
    val formShowFotoKtp: Boolean = true,
    val formShowFotoSurat: Boolean = true,

    // Visibilitas Atribut Kartu Pendataan di Aplikasi
    val cardShowNik: Boolean = true,
    val cardShowHp: Boolean = true,
    val cardShowAlamat: Boolean = true,
    val cardShowPhotos: Boolean = true,
    val cardShowStatus: Boolean = true,
    val cardShowQr: Boolean = true,

    // Penyimpanan & Subfolder Download Data Aplikasi
    val pdfStorageSubfolder: String = "si-pendata pasar",
    val pdfFileNameFormat: String = "KARTU_PEDAGANG_{NAMA}_{NOMOR_KIOS}",
    val pdfStorageDirectory: String = "DOCUMENTS", // DOCUMENTS atau DOWNLOADS
    val storageMainFolder: String = "SI-PENDATAPASAR",
    val storagePdfFolder: String = "PDF",
    val storageExcelFolder: String = "EXCEL",
    val storageAppsScriptFolder: String = "APPS_SCRIPT",
    val storageFotoFolder: String = "FOTO",
    val storageBackupConfigFolder: String = "BACKUP-KONFIGURASI",

    // Custom Label PDF
    val pdfLabelNama: String = "Nama Pedagang",
    val pdfLabelNik: String = "NIK / No. KTP",
    val pdfLabelAlamat: String = "Alamat Domisili",
    val pdfLabelHp: String = "Nomor HP / WA",
    val pdfLabelRuang: String = "Jenis Ruang Dagang",
    val pdfLabelKios: String = "Nomor Kios / Los",
    val pdfLabelKomoditi: String = "Komoditi / Usaha",
    val pdfLabelStatus: String = "Status Pedagang",
    val pdfLabelWaktu: String = "Waktu Pendataan",
    val pdfLabelFotoPlaceholder: String = "FOTO PEDAGANG",
    val pdfLabelLampiranPlaceholder: String = "(Lampiran)",
    val pdfLabelScanQr: String = "SCAN QR UNTUK VERIFIKASI",
    val pdfLabelKeteranganHeader: String = "Keterangan",
    val pdfLabelTerdataResmi: String = "✓ Pedagang dengan data di atas telah TERDATA secara resmi di Sistem Informasi Pendataan Pasar Waru.",
    val pdfLabelDiterbitkan: String = "Diterbitkan oleh Dinas Perindustrian dan Perdagangan Kabupaten Pamekasan pada {TANGGAL} ",

    // Hak Akses Fitur CRUD
    val allowCreate: Boolean = true,
    val allowUpdate: Boolean = true,
    val allowDelete: Boolean = true
) {
    val reqNik: Boolean get() = requireNik
    val reqNomorHp: Boolean get() = requireNomorHp
    val reqAlamat: Boolean get() = requireAlamat
    val reqFotoPedagang: Boolean get() = requireFotoPedagang
    val reqFotoKtp: Boolean get() = requireFotoKtp
    val reqFotoSuratPernyataan: Boolean get() = requireFotoSurat

    fun checkCompleteness(pedagang: Pedagang): Pair<Boolean, List<String>> {
        val missing = mutableListOf<String>()
        if (requireNik && (pedagang.nik.isBlank() || pedagang.nik.length < 16)) {
            missing.add("NIK (16 Digit)")
        }
        if (requireNomorHp && pedagang.nomorHp.isBlank()) {
            missing.add("Nomor HP/WA")
        }
        if (requireAlamat && pedagang.alamat.isBlank()) {
            missing.add("Alamat Domisili")
        }
        if (requireKomoditi && pedagang.komoditi.isBlank()) {
            missing.add("Komoditi / Jenis Usaha")
        }
        if (requireFotoPedagang && pedagang.fotoPedagangUri.isNullOrBlank()) {
            missing.add("Foto Pedagang")
        }
        if (requireFotoKtp && pedagang.fotoKtpUri.isNullOrBlank()) {
            missing.add("Foto KTP")
        }
        if (requireFotoSurat && pedagang.fotoSuratPernyataanUri.isNullOrBlank()) {
            missing.add("Foto Surat Pernyataan")
        }
        return Pair(missing.isEmpty(), missing)
    }
}

object AgencyConfigManager {
    private const val PREF_NAME = "disperindag_agency_config"

    private val _config = MutableStateFlow(AgencyConfig())
    val config: StateFlow<AgencyConfig> = _config.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        _config.value = AgencyConfig(
            namaPemerintah = prefs.getString("nama_pemerintah", "PEMERINTAH KABUPATEN PAMEKASAN") ?: "PEMERINTAH KABUPATEN PAMEKASAN",
            namaDinas = prefs.getString("nama_dinas", "DINAS PERINDUSTRIAN DAN PERDAGANGAN") ?: "DINAS PERINDUSTRIAN DAN PERDAGANGAN",
            appTitleHeader = prefs.getString("app_title_header", "DISPERINDAG PAMEKASAN") ?: "DISPERINDAG PAMEKASAN",
            appLoginTitle = prefs.getString("app_login_title", "SI-PENDATA PASAR") ?: "SI-PENDATA PASAR",
            namaPasar = prefs.getString("nama_pasar", "PASAR WARU KABUPATEN PAMEKASAN") ?: "PASAR WARU KABUPATEN PAMEKASAN",
            alamatDinas = prefs.getString("alamat_dinas", "Jalan Jokotole Nomor 199 Pamekasan 69322") ?: "Jalan Jokotole Nomor 199 Pamekasan 69322",
            teleponFax = prefs.getString("telepon_fax", "Telepon/Fax (0324) 321497") ?: "Telepon/Fax (0324) 321497",
            websiteLaman = prefs.getString("website_laman", "https://disperindag.pamekasankab.go.id/") ?: "https://disperindag.pamekasankab.go.id/",
            posElEmail = prefs.getString("pos_el_email", "disperindag@pamekasankab.go.id") ?: "disperindag@pamekasankab.go.id",
            customLogoUri = prefs.getString("custom_logo_uri", "") ?: "",

            spreadsheetId = prefs.getString("spreadsheet_id", "1Q7OtJ1fuEwkycAtnAjRNrSNrAEJ5SxjGRn-ge9YcWlU") ?: "1Q7OtJ1fuEwkycAtnAjRNrSNrAEJ5SxjGRn-ge9YcWlU",
            sheetGid = prefs.getString("sheet_gid", "1751220302") ?: "1751220302",
            driveFolderId = prefs.getString("drive_folder_id", "1G81CN0555Gst93hIosHG0lrUf4pP0ELO") ?: "1G81CN0555Gst93hIosHG0lrUf4pP0ELO",
            webhookUrl = prefs.getString("webhook_url", "https://script.google.com/macros/s/AKfycbzyIKCqNpmbhAxgbTWDPXwzZ1CyTgl8C_28CBtJkaoTQwXGHa7v2rdDFiLzkBrW7kjQ/exec") ?: "https://script.google.com/macros/s/AKfycbzyIKCqNpmbhAxgbTWDPXwzZ1CyTgl8C_28CBtJkaoTQwXGHa7v2rdDFiLzkBrW7kjQ/exec",

            enableCompletenessWarning = prefs.getBoolean("enable_completeness_warning", true),
            requireNik = prefs.getBoolean("require_nik", true),
            requireFotoPedagang = prefs.getBoolean("require_foto_pedagang", true),
            requireFotoKtp = prefs.getBoolean("require_foto_ktp", true),
            requireFotoSurat = prefs.getBoolean("require_foto_surat", true),
            requireNomorHp = prefs.getBoolean("require_nomor_hp", true),
            requireAlamat = prefs.getBoolean("require_alamat", true),
            requireKomoditi = prefs.getBoolean("require_komoditi", true),

            pdfPaperSize = prefs.getString("pdf_paper_size", "F5 Landscape") ?: "F5 Landscape",
            pdfFontFamily = prefs.getString("pdf_font_family", "Sans-Serif") ?: "Sans-Serif",
            pdfHeaderFontSize = prefs.getInt("pdf_header_font_size", 12),
            pdfBodyFontSize = prefs.getInt("pdf_body_font_size", 10),
            pdfHeaderTitle = prefs.getString("pdf_header_title", "PEMERINTAH KABUPATEN PAMEKASAN") ?: "PEMERINTAH KABUPATEN PAMEKASAN",
            pdfHeaderSubtitle = prefs.getString("pdf_header_subtitle", "DINAS PERINDUSTRIAN DAN PERDAGANGAN") ?: "DINAS PERINDUSTRIAN DAN PERDAGANGAN",
            pdfHeaderAddress = prefs.getString("pdf_header_address", "Jalan Jokotole Nomor 199 Pamekasan 69322") ?: "Jalan Jokotole Nomor 199 Pamekasan 69322",
            pdfFontSize = prefs.getFloat("pdf_font_size", 12f),
            pdfShowLogo = prefs.getBoolean("pdf_show_logo", true),
            pdfShowKopSurat = prefs.getBoolean("pdf_show_kop_surat", true),
            pdfShowQrCode = prefs.getBoolean("pdf_show_qr_code", true),
            pdfShowFotoPedagang = prefs.getBoolean("pdf_show_foto_pedagang", true),
            pdfShowFotoKtp = prefs.getBoolean("pdf_show_foto_ktp", true),
            pdfShowFotoSurat = prefs.getBoolean("pdf_show_foto_surat", true),
            pdfShowSignatureBlock = prefs.getBoolean("pdf_show_signature_block", true),
            pdfShowQr = prefs.getBoolean("pdf_show_qr", true),
            pdfShowVerificationStatus = prefs.getBoolean("pdf_show_verification_status", true),
            pdfFooterNote = prefs.getString("pdf_footer_note", "Bukti Pendataan Resmi - Dinas Perindustrian dan Perdagangan Kabupaten Pamekasan") ?: "Bukti Pendataan Resmi - Dinas Perindustrian dan Perdagangan Kabupaten Pamekasan",
            pdfFooterText = prefs.getString("pdf_footer_text", "Bukti Pendataan Resmi - Dinas Perindustrian dan Perdagangan Kabupaten Pamekasan") ?: "Bukti Pendataan Resmi - Dinas Perindustrian dan Perdagangan Kabupaten Pamekasan",
            pdfPejabatNama = prefs.getString("pdf_pejabat_nama", "Kepala Dinas Perindustrian dan Perdagangan") ?: "Kepala Dinas Perindustrian dan Perdagangan",
            pdfPejabatNip = prefs.getString("pdf_pejabat_nip", "19780512 200312 1 002") ?: "19780512 200312 1 002",
            pdfShowNik = prefs.getBoolean("pdf_show_nik", true),
            pdfShowHp = prefs.getBoolean("pdf_show_hp", true),
            pdfShowAlamat = prefs.getBoolean("pdf_show_alamat", true),
            pdfShowKomoditi = prefs.getBoolean("pdf_show_komoditi", true),
            pdfShowStatus = prefs.getBoolean("pdf_show_status", true),
            pdfShowLama = prefs.getBoolean("pdf_show_lama", true),
            pdfShowPetugas = prefs.getBoolean("pdf_show_petugas", true),
            pdfTitleText = prefs.getString("pdf_title_text", "") ?: "",

            formShowNik = prefs.getBoolean("form_show_nik", true),
            formShowAlamat = prefs.getBoolean("form_show_alamat", true),
            formShowHp = prefs.getBoolean("form_show_hp", true),
            formShowJenisRuang = prefs.getBoolean("form_show_jenis_ruang", true),
            formShowNomorKios = prefs.getBoolean("form_show_nomor_kios", true),
            formShowKomoditi = prefs.getBoolean("form_show_komoditi", true),
            formShowLamaBerjualan = prefs.getBoolean("form_show_lama_berjualan", true),
            formShowStatus = prefs.getBoolean("form_show_status", true),
            formShowKeterangan = prefs.getBoolean("form_show_keterangan", true),
            formShowFotoPedagang = prefs.getBoolean("form_show_foto_pedagang", true),
            formShowFotoKtp = prefs.getBoolean("form_show_foto_ktp", true),
            formShowFotoSurat = prefs.getBoolean("form_show_foto_surat", true),

            cardShowNik = prefs.getBoolean("card_show_nik", true),
            cardShowHp = prefs.getBoolean("card_show_hp", true),
            cardShowAlamat = prefs.getBoolean("card_show_alamat", true),
            cardShowPhotos = prefs.getBoolean("card_show_photos", true),
            cardShowStatus = prefs.getBoolean("card_show_status", true),
            cardShowQr = prefs.getBoolean("card_show_qr", true),

            pdfStorageSubfolder = prefs.getString("pdf_storage_subfolder", "si-pendata pasar") ?: "si-pendata pasar",
            pdfFileNameFormat = prefs.getString("pdf_file_name_format", "KARTU_PEDAGANG_{NAMA}_{NOMOR_KIOS}") ?: "KARTU_PEDAGANG_{NAMA}_{NOMOR_KIOS}",
            pdfStorageDirectory = prefs.getString("pdf_storage_directory", "DOCUMENTS") ?: "DOCUMENTS",
            storageMainFolder = prefs.getString("storage_main_folder", "SI-PENDATAPASAR") ?: "SI-PENDATAPASAR",
            storagePdfFolder = prefs.getString("storage_pdf_folder", "PDF") ?: "PDF",
            storageExcelFolder = prefs.getString("storage_excel_folder", "EXCEL") ?: "EXCEL",
            storageAppsScriptFolder = prefs.getString("storage_apps_script_folder", "APPS_SCRIPT") ?: "APPS_SCRIPT",
            storageFotoFolder = prefs.getString("storage_foto_folder", "FOTO") ?: "FOTO",
            storageBackupConfigFolder = prefs.getString("storage_backup_config_folder", "BACKUP-KONFIGURASI") ?: "BACKUP-KONFIGURASI",

            pdfLabelNama = prefs.getString("pdf_label_nama", "Nama Pedagang") ?: "Nama Pedagang",
            pdfLabelNik = prefs.getString("pdf_label_nik", "NIK / No. KTP") ?: "NIK / No. KTP",
            pdfLabelAlamat = prefs.getString("pdf_label_alamat", "Alamat Domisili") ?: "Alamat Domisili",
            pdfLabelHp = prefs.getString("pdf_label_hp", "Nomor HP / WA") ?: "Nomor HP / WA",
            pdfLabelRuang = prefs.getString("pdf_label_ruang", "Jenis Ruang Dagang") ?: "Jenis Ruang Dagang",
            pdfLabelKios = prefs.getString("pdf_label_kios", "Nomor Kios / Los") ?: "Nomor Kios / Los",
            pdfLabelKomoditi = prefs.getString("pdf_label_komoditi", "Komoditi / Usaha") ?: "Komoditi / Usaha",
            pdfLabelStatus = prefs.getString("pdf_label_status", "Status Pedagang") ?: "Status Pedagang",
            pdfLabelWaktu = prefs.getString("pdf_label_waktu", "Waktu Pendataan") ?: "Waktu Pendataan",
            pdfLabelFotoPlaceholder = prefs.getString("pdf_label_foto_placeholder", "FOTO PEDAGANG") ?: "FOTO PEDAGANG",
            pdfLabelLampiranPlaceholder = prefs.getString("pdf_label_lampiran_placeholder", "(Lampiran)") ?: "(Lampiran)",
            pdfLabelScanQr = prefs.getString("pdf_label_scan_qr", "SCAN QR UNTUK VERIFIKASI") ?: "SCAN QR UNTUK VERIFIKASI",
            pdfLabelKeteranganHeader = prefs.getString("pdf_label_keterangan_header", "Keterangan") ?: "Keterangan",
            pdfLabelTerdataResmi = prefs.getString("pdf_label_terdata_resmi", "✓ Pedagang dengan data di atas telah TERDATA secara resmi di Sistem Informasi Pendataan Pasar Waru.") ?: "✓ Pedagang dengan data di atas telah TERDATA secara resmi di Sistem Informasi Pendataan Pasar Waru.",
            pdfLabelDiterbitkan = prefs.getString("pdf_label_diterbitkan", "Diterbitkan oleh Dinas Perindustrian dan Perdagangan Kabupaten Pamekasan pada {TANGGAL} ") ?: "Diterbitkan oleh Dinas Perindustrian dan Perdagangan Kabupaten Pamekasan pada {TANGGAL} ",

            allowCreate = prefs.getBoolean("allow_create", true),
            allowUpdate = prefs.getBoolean("allow_update", true),
            allowDelete = prefs.getBoolean("allow_delete", true)
        )
    }

    fun updateConfig(context: Context, originalConfig: AgencyConfig) {
        val newConfig = originalConfig.copy(pdfStorageSubfolder = originalConfig.storagePdfFolder)
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString("nama_pemerintah", newConfig.namaPemerintah)
            .putString("nama_dinas", newConfig.namaDinas)
            .putString("app_title_header", newConfig.appTitleHeader)
            .putString("app_login_title", newConfig.appLoginTitle)
            .putString("nama_pasar", newConfig.namaPasar)
            .putString("alamat_dinas", newConfig.alamatDinas)
            .putString("telepon_fax", newConfig.teleponFax)
            .putString("website_laman", newConfig.websiteLaman)
            .putString("pos_el_email", newConfig.posElEmail)
            .putString("custom_logo_uri", newConfig.customLogoUri)

            .putString("spreadsheet_id", newConfig.spreadsheetId)
            .putString("sheet_gid", newConfig.sheetGid)
            .putString("drive_folder_id", newConfig.driveFolderId)
            .putString("webhook_url", newConfig.webhookUrl)

            .putBoolean("enable_completeness_warning", newConfig.enableCompletenessWarning)
            .putBoolean("require_nik", newConfig.requireNik)
            .putBoolean("require_foto_pedagang", newConfig.requireFotoPedagang)
            .putBoolean("require_foto_ktp", newConfig.requireFotoKtp)
            .putBoolean("require_foto_surat", newConfig.requireFotoSurat)
            .putBoolean("require_nomor_hp", newConfig.requireNomorHp)
            .putBoolean("require_alamat", newConfig.requireAlamat)
            .putBoolean("require_komoditi", newConfig.requireKomoditi)

            .putString("pdf_paper_size", newConfig.pdfPaperSize)
            .putString("pdf_font_family", newConfig.pdfFontFamily)
            .putInt("pdf_header_font_size", newConfig.pdfHeaderFontSize)
            .putInt("pdf_body_font_size", newConfig.pdfBodyFontSize)
            .putString("pdf_header_title", newConfig.pdfHeaderTitle)
            .putString("pdf_header_subtitle", newConfig.pdfHeaderSubtitle)
            .putString("pdf_header_address", newConfig.pdfHeaderAddress)
            .putFloat("pdf_font_size", newConfig.pdfFontSize)
            .putBoolean("pdf_show_logo", newConfig.pdfShowLogo)
            .putBoolean("pdf_show_kop_surat", newConfig.pdfShowKopSurat)
            .putBoolean("pdf_show_qr_code", newConfig.pdfShowQrCode)
            .putBoolean("pdf_show_foto_pedagang", newConfig.pdfShowFotoPedagang)
            .putBoolean("pdf_show_foto_ktp", newConfig.pdfShowFotoKtp)
            .putBoolean("pdf_show_foto_surat", newConfig.pdfShowFotoSurat)
            .putBoolean("pdf_show_signature_block", newConfig.pdfShowSignatureBlock)
            .putBoolean("pdf_show_qr", newConfig.pdfShowQr)
            .putBoolean("pdf_show_verification_status", newConfig.pdfShowVerificationStatus)
            .putString("pdf_footer_note", newConfig.pdfFooterNote)
            .putString("pdf_footer_text", newConfig.pdfFooterText)
            .putString("pdf_pejabat_nama", newConfig.pdfPejabatNama)
            .putString("pdf_pejabat_nip", newConfig.pdfPejabatNip)
            .putBoolean("pdf_show_nik", newConfig.pdfShowNik)
            .putBoolean("pdf_show_hp", newConfig.pdfShowHp)
            .putBoolean("pdf_show_alamat", newConfig.pdfShowAlamat)
            .putBoolean("pdf_show_komoditi", newConfig.pdfShowKomoditi)
            .putBoolean("pdf_show_status", newConfig.pdfShowStatus)
            .putBoolean("pdf_show_lama", newConfig.pdfShowLama)
            .putBoolean("pdf_show_petugas", newConfig.pdfShowPetugas)
            .putString("pdf_title_text", newConfig.pdfTitleText)

            .putBoolean("form_show_nik", newConfig.formShowNik)
            .putBoolean("form_show_alamat", newConfig.formShowAlamat)
            .putBoolean("form_show_hp", newConfig.formShowHp)
            .putBoolean("form_show_jenis_ruang", newConfig.formShowJenisRuang)
            .putBoolean("form_show_nomor_kios", newConfig.formShowNomorKios)
            .putBoolean("form_show_komoditi", newConfig.formShowKomoditi)
            .putBoolean("form_show_lama_berjualan", newConfig.formShowLamaBerjualan)
            .putBoolean("form_show_status", newConfig.formShowStatus)
            .putBoolean("form_show_keterangan", newConfig.formShowKeterangan)
            .putBoolean("form_show_foto_pedagang", newConfig.formShowFotoPedagang)
            .putBoolean("form_show_foto_ktp", newConfig.formShowFotoKtp)
            .putBoolean("form_show_foto_surat", newConfig.formShowFotoSurat)

            .putBoolean("card_show_nik", newConfig.cardShowNik)
            .putBoolean("card_show_hp", newConfig.cardShowHp)
            .putBoolean("card_show_alamat", newConfig.cardShowAlamat)
            .putBoolean("card_show_photos", newConfig.cardShowPhotos)
            .putBoolean("card_show_status", newConfig.cardShowStatus)
            .putBoolean("card_show_qr", newConfig.cardShowQr)

            .putString("pdf_storage_subfolder", newConfig.pdfStorageSubfolder)
            .putString("pdf_file_name_format", newConfig.pdfFileNameFormat)
            .putString("pdf_storage_directory", newConfig.pdfStorageDirectory)
            .putString("storage_main_folder", newConfig.storageMainFolder)
            .putString("storage_pdf_folder", newConfig.storagePdfFolder)
            .putString("storage_excel_folder", newConfig.storageExcelFolder)
            .putString("storage_apps_script_folder", newConfig.storageAppsScriptFolder)
            .putString("storage_foto_folder", newConfig.storageFotoFolder)
            .putString("storage_backup_config_folder", newConfig.storageBackupConfigFolder)

            .putString("pdf_label_nama", newConfig.pdfLabelNama)
            .putString("pdf_label_nik", newConfig.pdfLabelNik)
            .putString("pdf_label_alamat", newConfig.pdfLabelAlamat)
            .putString("pdf_label_hp", newConfig.pdfLabelHp)
            .putString("pdf_label_ruang", newConfig.pdfLabelRuang)
            .putString("pdf_label_kios", newConfig.pdfLabelKios)
            .putString("pdf_label_komoditi", newConfig.pdfLabelKomoditi)
            .putString("pdf_label_status", newConfig.pdfLabelStatus)
            .putString("pdf_label_waktu", newConfig.pdfLabelWaktu)
            .putString("pdf_label_foto_placeholder", newConfig.pdfLabelFotoPlaceholder)
            .putString("pdf_label_lampiran_placeholder", newConfig.pdfLabelLampiranPlaceholder)
            .putString("pdf_label_scan_qr", newConfig.pdfLabelScanQr)
            .putString("pdf_label_keterangan_header", newConfig.pdfLabelKeteranganHeader)
            .putString("pdf_label_terdata_resmi", newConfig.pdfLabelTerdataResmi)
            .putString("pdf_label_diterbitkan", newConfig.pdfLabelDiterbitkan)

            .putBoolean("allow_create", newConfig.allowCreate)
            .putBoolean("allow_update", newConfig.allowUpdate)
            .putBoolean("allow_delete", newConfig.allowDelete)
            .apply()

        _config.value = newConfig
    }

    fun resetToDefault(context: Context) {
        val defaultConfig = AgencyConfig()
        updateConfig(context, defaultConfig)
    }

    fun exportConfigToJson(config: AgencyConfig): String {
        val json = JSONObject()
        json.put("namaPemerintah", config.namaPemerintah)
        json.put("namaDinas", config.namaDinas)
        json.put("appTitleHeader", config.appTitleHeader)
        json.put("appLoginTitle", config.appLoginTitle)
        json.put("namaPasar", config.namaPasar)
        json.put("alamatDinas", config.alamatDinas)
        json.put("teleponFax", config.teleponFax)
        json.put("websiteLaman", config.websiteLaman)
        json.put("posElEmail", config.posElEmail)
        json.put("customLogoUri", config.customLogoUri)

        json.put("spreadsheetId", config.spreadsheetId)
        json.put("sheetGid", config.sheetGid)
        json.put("driveFolderId", config.driveFolderId)
        json.put("webhookUrl", config.webhookUrl)

        json.put("enableCompletenessWarning", config.enableCompletenessWarning)
        json.put("requireNik", config.requireNik)
        json.put("requireFotoPedagang", config.requireFotoPedagang)
        json.put("requireFotoKtp", config.requireFotoKtp)
        json.put("requireFotoSurat", config.requireFotoSurat)
        json.put("requireNomorHp", config.requireNomorHp)
        json.put("requireAlamat", config.requireAlamat)
        json.put("requireKomoditi", config.requireKomoditi)

        json.put("pdfPaperSize", config.pdfPaperSize)
        json.put("pdfFontFamily", config.pdfFontFamily)
        json.put("pdfHeaderFontSize", config.pdfHeaderFontSize)
        json.put("pdfBodyFontSize", config.pdfBodyFontSize)
        json.put("pdfHeaderTitle", config.pdfHeaderTitle)
        json.put("pdfHeaderSubtitle", config.pdfHeaderSubtitle)
        json.put("pdfHeaderAddress", config.pdfHeaderAddress)
        json.put("pdfFontSize", config.pdfFontSize.toDouble())
        json.put("pdfShowLogo", config.pdfShowLogo)
        json.put("pdfShowKopSurat", config.pdfShowKopSurat)
        json.put("pdfShowQrCode", config.pdfShowQrCode)
        json.put("pdfShowFotoPedagang", config.pdfShowFotoPedagang)
        json.put("pdfShowFotoKtp", config.pdfShowFotoKtp)
        json.put("pdfShowFotoSurat", config.pdfShowFotoSurat)
        json.put("pdfShowSignatureBlock", config.pdfShowSignatureBlock)
        json.put("pdfShowQr", config.pdfShowQr)
        json.put("pdfShowVerificationStatus", config.pdfShowVerificationStatus)
        json.put("pdfFooterNote", config.pdfFooterNote)
        json.put("pdfFooterText", config.pdfFooterText)
        json.put("pdfPejabatNama", config.pdfPejabatNama)
        json.put("pdfPejabatNip", config.pdfPejabatNip)
        json.put("pdfShowNik", config.pdfShowNik)
        json.put("pdfShowHp", config.pdfShowHp)
        json.put("pdfShowAlamat", config.pdfShowAlamat)
        json.put("pdfShowKomoditi", config.pdfShowKomoditi)
        json.put("pdfShowStatus", config.pdfShowStatus)
        json.put("pdfShowLama", config.pdfShowLama)
        json.put("pdfShowPetugas", config.pdfShowPetugas)
        json.put("pdfTitleText", config.pdfTitleText)

        json.put("formShowNik", config.formShowNik)
        json.put("formShowAlamat", config.formShowAlamat)
        json.put("formShowHp", config.formShowHp)
        json.put("formShowJenisRuang", config.formShowJenisRuang)
        json.put("formShowNomorKios", config.formShowNomorKios)
        json.put("formShowKomoditi", config.formShowKomoditi)
        json.put("formShowLamaBerjualan", config.formShowLamaBerjualan)
        json.put("formShowStatus", config.formShowStatus)
        json.put("formShowKeterangan", config.formShowKeterangan)
        json.put("formShowFotoPedagang", config.formShowFotoPedagang)
        json.put("formShowFotoKtp", config.formShowFotoKtp)
        json.put("formShowFotoSurat", config.formShowFotoSurat)

        json.put("cardShowNik", config.cardShowNik)
        json.put("cardShowHp", config.cardShowHp)
        json.put("cardShowAlamat", config.cardShowAlamat)
        json.put("cardShowPhotos", config.cardShowPhotos)
        json.put("cardShowStatus", config.cardShowStatus)
        json.put("cardShowQr", config.cardShowQr)

        json.put("pdfStorageSubfolder", config.pdfStorageSubfolder)
        json.put("pdfFileNameFormat", config.pdfFileNameFormat)
        json.put("pdfStorageDirectory", config.pdfStorageDirectory)
        json.put("storageMainFolder", config.storageMainFolder)
        json.put("storagePdfFolder", config.storagePdfFolder)
        json.put("storageExcelFolder", config.storageExcelFolder)
        json.put("storageAppsScriptFolder", config.storageAppsScriptFolder)
        json.put("storageFotoFolder", config.storageFotoFolder)
        json.put("storageBackupConfigFolder", config.storageBackupConfigFolder)

        json.put("pdfLabelNama", config.pdfLabelNama)
        json.put("pdfLabelNik", config.pdfLabelNik)
        json.put("pdfLabelAlamat", config.pdfLabelAlamat)
        json.put("pdfLabelHp", config.pdfLabelHp)
        json.put("pdfLabelRuang", config.pdfLabelRuang)
        json.put("pdfLabelKios", config.pdfLabelKios)
        json.put("pdfLabelKomoditi", config.pdfLabelKomoditi)
        json.put("pdfLabelStatus", config.pdfLabelStatus)
        json.put("pdfLabelWaktu", config.pdfLabelWaktu)
        json.put("pdfLabelFotoPlaceholder", config.pdfLabelFotoPlaceholder)
        json.put("pdfLabelLampiranPlaceholder", config.pdfLabelLampiranPlaceholder)
        json.put("pdfLabelScanQr", config.pdfLabelScanQr)
        json.put("pdfLabelKeteranganHeader", config.pdfLabelKeteranganHeader)
        json.put("pdfLabelTerdataResmi", config.pdfLabelTerdataResmi)
        json.put("pdfLabelDiterbitkan", config.pdfLabelDiterbitkan)

        json.put("allowCreate", config.allowCreate)
        json.put("allowUpdate", config.allowUpdate)
        json.put("allowDelete", config.allowDelete)

        return json.toString(2)
    }

    fun importConfigFromJson(context: Context, jsonStr: String): Result<AgencyConfig> {
        return try {
            val json = JSONObject(jsonStr)
            val curr = _config.value
            val importedConfig = AgencyConfig(
                namaPemerintah = json.optString("namaPemerintah", curr.namaPemerintah),
                namaDinas = json.optString("namaDinas", curr.namaDinas),
                appTitleHeader = json.optString("appTitleHeader", curr.appTitleHeader),
                appLoginTitle = json.optString("appLoginTitle", curr.appLoginTitle),
                namaPasar = json.optString("namaPasar", curr.namaPasar),
                alamatDinas = json.optString("alamatDinas", curr.alamatDinas),
                teleponFax = json.optString("teleponFax", curr.teleponFax),
                websiteLaman = json.optString("websiteLaman", curr.websiteLaman),
                posElEmail = json.optString("posElEmail", curr.posElEmail),
                customLogoUri = json.optString("customLogoUri", curr.customLogoUri),

                spreadsheetId = json.optString("spreadsheetId", curr.spreadsheetId),
                sheetGid = json.optString("sheetGid", curr.sheetGid),
                driveFolderId = json.optString("driveFolderId", curr.driveFolderId),
                webhookUrl = json.optString("webhookUrl", curr.webhookUrl),

                enableCompletenessWarning = json.optBoolean("enableCompletenessWarning", curr.enableCompletenessWarning),
                requireNik = json.optBoolean("requireNik", curr.requireNik),
                requireFotoPedagang = json.optBoolean("requireFotoPedagang", curr.requireFotoPedagang),
                requireFotoKtp = json.optBoolean("requireFotoKtp", curr.requireFotoKtp),
                requireFotoSurat = json.optBoolean("requireFotoSurat", curr.requireFotoSurat),
                requireNomorHp = json.optBoolean("requireNomorHp", curr.requireNomorHp),
                requireAlamat = json.optBoolean("requireAlamat", curr.requireAlamat),
                requireKomoditi = json.optBoolean("requireKomoditi", curr.requireKomoditi),

                pdfPaperSize = json.optString("pdfPaperSize", curr.pdfPaperSize),
                pdfFontFamily = json.optString("pdfFontFamily", curr.pdfFontFamily),
                pdfHeaderFontSize = json.optInt("pdfHeaderFontSize", curr.pdfHeaderFontSize),
                pdfBodyFontSize = json.optInt("pdfBodyFontSize", curr.pdfBodyFontSize),
                pdfHeaderTitle = json.optString("pdfHeaderTitle", curr.pdfHeaderTitle),
                pdfHeaderSubtitle = json.optString("pdfHeaderSubtitle", curr.pdfHeaderSubtitle),
                pdfHeaderAddress = json.optString("pdfHeaderAddress", curr.pdfHeaderAddress),
                pdfFontSize = json.optDouble("pdfFontSize", curr.pdfFontSize.toDouble()).toFloat(),
                pdfShowLogo = json.optBoolean("pdfShowLogo", curr.pdfShowLogo),
                pdfShowKopSurat = json.optBoolean("pdfShowKopSurat", curr.pdfShowKopSurat),
                pdfShowQrCode = json.optBoolean("pdfShowQrCode", curr.pdfShowQrCode),
                pdfShowFotoPedagang = json.optBoolean("pdfShowFotoPedagang", curr.pdfShowFotoPedagang),
                pdfShowFotoKtp = json.optBoolean("pdfShowFotoKtp", curr.pdfShowFotoKtp),
                pdfShowFotoSurat = json.optBoolean("pdfShowFotoSurat", curr.pdfShowFotoSurat),
                pdfShowSignatureBlock = json.optBoolean("pdfShowSignatureBlock", curr.pdfShowSignatureBlock),
                pdfShowQr = json.optBoolean("pdfShowQr", curr.pdfShowQr),
                pdfShowVerificationStatus = json.optBoolean("pdfShowVerificationStatus", curr.pdfShowVerificationStatus),
                pdfFooterNote = json.optString("pdfFooterNote", curr.pdfFooterNote),
                pdfFooterText = json.optString("pdfFooterText", curr.pdfFooterText),
                pdfPejabatNama = json.optString("pdfPejabatNama", curr.pdfPejabatNama),
                pdfPejabatNip = json.optString("pdfPejabatNip", curr.pdfPejabatNip),
                pdfShowNik = json.optBoolean("pdfShowNik", curr.pdfShowNik),
                pdfShowHp = json.optBoolean("pdfShowHp", curr.pdfShowHp),
                pdfShowAlamat = json.optBoolean("pdfShowAlamat", curr.pdfShowAlamat),
                pdfShowKomoditi = json.optBoolean("pdfShowKomoditi", curr.pdfShowKomoditi),
                pdfShowStatus = json.optBoolean("pdfShowStatus", curr.pdfShowStatus),
                pdfShowLama = json.optBoolean("pdfShowLama", curr.pdfShowLama),
                pdfShowPetugas = json.optBoolean("pdfShowPetugas", curr.pdfShowPetugas),
                pdfTitleText = json.optString("pdfTitleText", curr.pdfTitleText),

                formShowNik = json.optBoolean("formShowNik", curr.formShowNik),
                formShowAlamat = json.optBoolean("formShowAlamat", curr.formShowAlamat),
                formShowHp = json.optBoolean("formShowHp", curr.formShowHp),
                formShowJenisRuang = json.optBoolean("formShowJenisRuang", curr.formShowJenisRuang),
                formShowNomorKios = json.optBoolean("formShowNomorKios", curr.formShowNomorKios),
                formShowKomoditi = json.optBoolean("formShowKomoditi", curr.formShowKomoditi),
                formShowLamaBerjualan = json.optBoolean("formShowLamaBerjualan", curr.formShowLamaBerjualan),
                formShowStatus = json.optBoolean("formShowStatus", curr.formShowStatus),
                formShowKeterangan = json.optBoolean("formShowKeterangan", curr.formShowKeterangan),
                formShowFotoPedagang = json.optBoolean("formShowFotoPedagang", curr.formShowFotoPedagang),
                formShowFotoKtp = json.optBoolean("formShowFotoKtp", curr.formShowFotoKtp),
                formShowFotoSurat = json.optBoolean("formShowFotoSurat", curr.formShowFotoSurat),

                cardShowNik = json.optBoolean("cardShowNik", curr.cardShowNik),
                cardShowHp = json.optBoolean("cardShowHp", curr.cardShowHp),
                cardShowAlamat = json.optBoolean("cardShowAlamat", curr.cardShowAlamat),
                cardShowPhotos = json.optBoolean("cardShowPhotos", curr.cardShowPhotos),
                cardShowStatus = json.optBoolean("cardShowStatus", curr.cardShowStatus),
                cardShowQr = json.optBoolean("cardShowQr", curr.cardShowQr),

                pdfStorageSubfolder = json.optString("pdfStorageSubfolder", curr.pdfStorageSubfolder),
                pdfFileNameFormat = json.optString("pdfFileNameFormat", curr.pdfFileNameFormat),
                pdfStorageDirectory = json.optString("pdfStorageDirectory", curr.pdfStorageDirectory),
                storageMainFolder = json.optString("storageMainFolder", curr.storageMainFolder),
                storagePdfFolder = json.optString("storagePdfFolder", curr.storagePdfFolder),
                storageExcelFolder = json.optString("storageExcelFolder", curr.storageExcelFolder),
                storageAppsScriptFolder = json.optString("storageAppsScriptFolder", curr.storageAppsScriptFolder),
                storageFotoFolder = json.optString("storageFotoFolder", curr.storageFotoFolder),
                storageBackupConfigFolder = json.optString("storageBackupConfigFolder", curr.storageBackupConfigFolder),

                pdfLabelNama = json.optString("pdfLabelNama", curr.pdfLabelNama),
                pdfLabelNik = json.optString("pdfLabelNik", curr.pdfLabelNik),
                pdfLabelAlamat = json.optString("pdfLabelAlamat", curr.pdfLabelAlamat),
                pdfLabelHp = json.optString("pdfLabelHp", curr.pdfLabelHp),
                pdfLabelRuang = json.optString("pdfLabelRuang", curr.pdfLabelRuang),
                pdfLabelKios = json.optString("pdfLabelKios", curr.pdfLabelKios),
                pdfLabelKomoditi = json.optString("pdfLabelKomoditi", curr.pdfLabelKomoditi),
                pdfLabelStatus = json.optString("pdfLabelStatus", curr.pdfLabelStatus),
                pdfLabelWaktu = json.optString("pdfLabelWaktu", curr.pdfLabelWaktu),
                pdfLabelFotoPlaceholder = json.optString("pdfLabelFotoPlaceholder", curr.pdfLabelFotoPlaceholder),
                pdfLabelLampiranPlaceholder = json.optString("pdfLabelLampiranPlaceholder", curr.pdfLabelLampiranPlaceholder),
                pdfLabelScanQr = json.optString("pdfLabelScanQr", curr.pdfLabelScanQr),
                pdfLabelKeteranganHeader = json.optString("pdfLabelKeteranganHeader", curr.pdfLabelKeteranganHeader),
                pdfLabelTerdataResmi = json.optString("pdfLabelTerdataResmi", curr.pdfLabelTerdataResmi),
                pdfLabelDiterbitkan = json.optString("pdfLabelDiterbitkan", curr.pdfLabelDiterbitkan),

                allowCreate = json.optBoolean("allowCreate", curr.allowCreate),
                allowUpdate = json.optBoolean("allowUpdate", curr.allowUpdate),
                allowDelete = json.optBoolean("allowDelete", curr.allowDelete)
            )
            updateConfig(context, importedConfig)
            Result.success(importedConfig)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

