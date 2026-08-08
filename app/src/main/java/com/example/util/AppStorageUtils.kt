package com.example.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.config.AgencyConfigManager
import java.io.File

object AppStorageUtils {

    const val DEFAULT_MAIN_FOLDER = "SI-PENDATAPASAR"

    enum class CategoryFolder(val defaultSubFolderName: String) {
        PDF("PDF"),
        EXCEL("EXCEL"),
        APPS_SCRIPT("APPS_SCRIPT"),
        FOTO("FOTO"),
        BACKUP("BACKUP"),
        BACKUP_KONFIGURASI("BACKUP-KONFIGURASI")
    }

    /**
     * Memeriksa apakah aplikasi memiliki izin akses penyimpanan file/storage.
     */
    fun hasStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            val readPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
            val writePerm = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            readPerm == PackageManager.PERMISSION_GRANTED && writePerm == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Membuka pengaturan sistem untuk meminta/mengaktifkan izin akses storage.
     */
    fun openStoragePermissionSettings(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } else {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            Log.e("AppStorageUtils", "Error launching permission settings: ${e.message}")
            try {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    /**
     * Mengambil nama folder utama kustom dari AgencyConfigManager (default: SI-PENDATAPASAR).
     */
    fun getMainFolderName(): String {
        val configName = AgencyConfigManager.config.value.storageMainFolder.trim()
        return if (configName.isBlank()) DEFAULT_MAIN_FOLDER else configName
    }

    /**
     * Mengambil direktori folder utama SI-PENDATAPASAR pada internal storage (/storage/emulated/0/SI-PENDATAPASAR/)
     * dengan multi-strategy fallback (Root -> Downloads -> Documents -> App External Dir).
     */
    fun getMainDirectory(context: Context? = null, customMainName: String? = null): File {
        val mainName = customMainName?.trim()?.ifBlank { null } ?: getMainFolderName()

        // Strategy 1: Direct Root External Storage (/storage/emulated/0/{mainName})
        try {
            val rootDir = Environment.getExternalStorageDirectory()
            val mainDir = File(rootDir, mainName)
            if (!mainDir.exists()) mainDir.mkdirs()
            if (mainDir.exists() && mainDir.canWrite()) return mainDir
        } catch (e: Exception) {
            Log.e("AppStorageUtils", "Root dir strategy failed: ${e.message}")
        }

        // Strategy 2: Public Downloads Directory (/storage/emulated/0/Download/{mainName})
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val dlMainDir = File(downloadsDir, mainName)
            if (!dlMainDir.exists()) dlMainDir.mkdirs()
            if (dlMainDir.exists()) return dlMainDir
        } catch (e: Exception) {
            Log.e("AppStorageUtils", "Downloads dir strategy failed: ${e.message}")
        }

        // Strategy 3: Public Documents Directory (/storage/emulated/0/Documents/{mainName})
        try {
            val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val docsMainDir = File(docsDir, mainName)
            if (!docsMainDir.exists()) docsMainDir.mkdirs()
            if (docsMainDir.exists()) return docsMainDir
        } catch (e: Exception) {
            Log.e("AppStorageUtils", "Documents dir strategy failed: ${e.message}")
        }

        // Strategy 4: App External Files Dir (always guaranteed writable)
        if (context != null) {
            try {
                val appExtDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.getExternalFilesDir(null)
                if (appExtDir != null) {
                    val fallbackDir = File(appExtDir, mainName)
                    if (!fallbackDir.exists()) fallbackDir.mkdirs()
                    return fallbackDir
                }
            } catch (e: Exception) {
                Log.e("AppStorageUtils", "App ext dir strategy failed: ${e.message}")
            }
        }

        // Ultimate fallback
        val defaultDocs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val fallback = File(defaultDocs, mainName)
        if (!fallback.exists()) fallback.mkdirs()
        return fallback
    }

    /**
     * Mengambil sub-folder kustom berdasarkan kategori (misal: PDF, EXCEL, APPS_SCRIPT, FOTO, BACKUP-KONFIGURASI).
     */
    fun getCategoryDirectory(
        category: CategoryFolder,
        customSubFolder: String? = null,
        customMainFolder: String? = null,
        context: Context? = null
    ): File {
        val config = AgencyConfigManager.config.value
        val mainName = customMainFolder?.trim()?.ifBlank { null } ?: config.storageMainFolder
        val mainDir = getMainDirectory(context, mainName)

        val subFolderConfigName = when (category) {
            CategoryFolder.PDF -> config.storagePdfFolder.ifBlank { "PDF" }
            CategoryFolder.EXCEL -> config.storageExcelFolder.ifBlank { "EXCEL" }
            CategoryFolder.APPS_SCRIPT -> config.storageAppsScriptFolder.ifBlank { "APPS_SCRIPT" }
            CategoryFolder.FOTO -> config.storageFotoFolder.ifBlank { "FOTO" }
            CategoryFolder.BACKUP_KONFIGURASI -> config.storageBackupConfigFolder.ifBlank { "BACKUP-KONFIGURASI" }
            CategoryFolder.BACKUP -> config.storageBackupConfigFolder.ifBlank { "BACKUP" }
        }.trim()

        val subName = customSubFolder?.trim()?.ifBlank { null } ?: subFolderConfigName

        val categoryDir = File(mainDir, subName)
        if (!categoryDir.exists()) {
            categoryDir.mkdirs()
        }

        return categoryDir
    }

    /**
     * Mengambil direktori folder khusus Backup Konfigurasi:
     * Internal Storage/SI-PENDATAPASAR/BACKUP-KONFIGURASI/
     */
    fun getBackupConfigDirectory(
        context: Context? = null,
        customMainFolder: String? = null,
        customSubFolder: String? = null
    ): File {
        return getCategoryDirectory(
            category = CategoryFolder.BACKUP_KONFIGURASI,
            customSubFolder = customSubFolder,
            customMainFolder = customMainFolder,
            context = context
        )
    }

    /**
     * Memicu MediaScanner agar file baru tersinkron dan terbaca oleh File Manager HP.
     */
    fun scanFile(context: Context?, file: File) {
        if (context == null) return
        try {
            android.media.MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                null
            ) { path, uri ->
                Log.d("AppStorageUtils", "MediaScanner indexed file: $path -> $uri")
            }
        } catch (e: Exception) {
            Log.e("AppStorageUtils", "Error scanning file: ${e.message}")
        }
    }

    /**
     * Memastikan seluruh struktur folder & sub-folder default aplikasi telah dibuat di Internal Storage.
     * Subfolder: PDF, EXCEL, APPS_SCRIPT, FOTO, BACKUP-KONFIGURASI
     */
    fun ensureDirectoriesExist(context: Context? = null): Map<String, File> {
        val created = mutableMapOf<String, File>()
        val mainDir = getMainDirectory(context)
        created["MAIN"] = mainDir

        CategoryFolder.values().forEach { cat ->
            val dir = getCategoryDirectory(cat, context = context)
            created[cat.name] = dir
        }
        return created
    }
}
