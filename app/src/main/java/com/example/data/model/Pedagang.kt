package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pedagang")
data class Pedagang(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: String, // waktu proses input (ISO / YYYY-MM-DD HH:mm:ss)
    val emailAddress: String, // email pendata (Google Account)
    val namaPedagang: String, // Wajib diisi
    val nik: String = "", // 16 digit, optional
    val alamat: String = "", // optional
    val nomorHp: String = "", // angka mulai dari 0, 9-13 digit, optional
    val jenisRuangDagang: String, // Wajib diisi (Dropdown)
    val nomorKiosLos: String, // Text, Wajib diisi
    val komoditi: String, // Wajib diisi (Dropdown)
    val lamaBerjualan: Int, // Tahun, Wajib diisi
    val status: String, // Wajib diisi (Dropdown)
    val keterangan: String = "", // Text, optional
    val fotoPedagangUri: String? = null,
    val fotoKtpUri: String? = null,
    val fotoSuratPernyataanUri: String? = null,
    val syncStatus: String = "SYNCED" // "SYNCED" or "PENDING_SYNC"
)
