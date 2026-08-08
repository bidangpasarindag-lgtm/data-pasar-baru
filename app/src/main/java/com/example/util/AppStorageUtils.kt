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
     * dengan fallback otomatis jika direktori utama tidak dapat dibuat.
     */
    fun getMainDirectory(context: Context? = null, customMainName: String? = null): File {
        val mainName = customMainName?.trim()?.ifBlank { null } ?: getMainFolderName()
        return try {
            val rootDir = Environment.getExternalStorageDirectory()
            val mainDir = File(rootDir, mainName)
            if (!mainDir.exists()) {
                mainDir.mkdirs()
            }
            if (!mainDir.exists()) {
                val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                val fallbackMainDir = File(docsDir, mainName)
                if (!fallbackMainDir.exists()) fallbackMainDir.mkdirs()
                fallbackMainDir
            } else {
                mainDir
            }
        } catch (e: Exception) {
            Log.e("AppStorageUtils", "Error creating main directory: ${e.message}")
            val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val fallbackMainDir = File(docsDir, mainName)
            if (!fallbackMainDir.exists()) fallbackMainDir.mkdirs()
            fallbackMainDir
        }
    }

    /**
     * Mengambil sub-folder kustom berdasarkan kategori (misal: PDF, EXCEL, APPS_SCRIPT, FOTO, BACKUP-KONFIGURASI).
     */
    fun getCategoryDirectory(
        category: CategoryFolder,
        customSubFolder: String? = null,
        context: Context? = null
    ): File {
        val config = AgencyConfigManager.config.value
        val mainDir = getMainDirectory(context, config.storageMainFolder)

        val subFolderConfigName = when (category) {
            CategoryFolder.PDF -> config.storagePdfFolder.ifBlank { "PDF" }
            CategoryFolder.EXCEL -> config.storageExcelFolder.ifBlank { "EXCEL" }
            CategoryFolder.APPS_SCRIPT -> config.storageAppsScriptFolder.ifBlank { "APPS_SCRIPT" }
            CategoryFolder.FOTO -> config.storageFotoFolder.ifBlank { "FOTO" }
            CategoryFolder.BACKUP_KONFIGURASI -> config.storageBackupConfigFolder.ifBlank { "BACKUP-KONFIGURASI" }
            CategoryFolder.BACKUP -> config.storageBackupConfigFolder.ifBlank { "BACKUP" }
        }.trim()

        val categoryDir = File(mainDir, subFolderConfigName)
        if (!categoryDir.exists()) {
            categoryDir.mkdirs()
        }

        val cleanCustom = customSubFolder?.trim()
        if (!cleanCustom.isNullOrBlank() && 
            !cleanCustom.equals("si-pendata pasar", ignoreCase = true) && 
            !cleanCustom.equals("SI-PENDATAPASAR", ignoreCase = true) &&
            !cleanCustom.equals(subFolderConfigName, ignoreCase = true)) {
            val customDir = File(categoryDir, cleanCustom)
            if (!customDir.exists()) customDir.mkdirs()
            return customDir
        }

        return categoryDir
    }

    /**
     * Mengambil direktori folder khusus Backup Konfigurasi:
     * Internal Storage/SI-PENDATAPASAR/BACKUP-KONFIGURASI/
     */
    fun getBackupConfigDirectory(context: Context? = null): File {
        return getCategoryDirectory(CategoryFolder.BACKUP_KONFIGURASI, context = context)
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
