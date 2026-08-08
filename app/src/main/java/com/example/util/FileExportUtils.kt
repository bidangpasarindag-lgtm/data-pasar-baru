package com.example.util

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook

object FileExportUtils {

    fun saveAppsScriptAsTxt(context: Context, code: String): File? {
        try {
            val dateStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
            val hourStr = SimpleDateFormat("HHmm", Locale.getDefault()).format(Date())
            val uniqueCode = (1000..9999).random().toString()
            val fileName = "${dateStr}-${hourStr}-Apps Script-${uniqueCode}.txt"

            val downloadFolder = AppStorageUtils.getCategoryDirectory(
                category = AppStorageUtils.CategoryFolder.APPS_SCRIPT,
                context = context
            )

            val file = File(downloadFolder, fileName)
            FileOutputStream(file).use { 
                it.write(code.toByteArray())
            }
            
            Log.d("FileExport", "Apps Script saved to: ${file.absolutePath}")
            return file
        } catch (e: Exception) {
            Log.e("FileExport", "Failed to save Apps Script as txt", e)
            return null
        }
    }

    fun downloadTemplateSpreadsheetXlsx(context: Context): File? {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.getDefault()).format(Date())
            val fileName = "${timestamp}-TemplateSpreadsheet.xlsx"
            
            val downloadFolder = AppStorageUtils.getCategoryDirectory(
                category = AppStorageUtils.CategoryFolder.EXCEL,
                context = context
            )
            
            val file = File(downloadFolder, fileName)
            
            val workbook = XSSFWorkbook()
            
            // Sheet 1: Form Responses 1 (Data Pedagang)
            val sheet1 = workbook.createSheet("Form Responses 1")
            val header1 = listOf("Timestamp", "Email Address", "NAMA PEDAGANG", "NIK", "ALAMAT", "NOMOR HP", "JENIS RUANG DAGANG", "NOMOR KIOS/LOS", "KOMODITI/JENIS USAHA", "LAMA BERJUALAN", "STATUS", "KETERANGAN", "FOTO PEDAGANG", "FOTO KTP", "FOTO SURAT PERNYATAAN", "FOTO PEDAGANG GDRIVE", "FOTO KTP GDRIVE", "FOTO SURAT PERNYATAAN GDRIVE")
            val headerRow1 = sheet1.createRow(0)
            header1.forEachIndexed { i, s -> headerRow1.createCell(i).setCellValue(s) }
            
            val sampleRow1 = sheet1.createRow(1)
            val sampleData1 = listOf("01/08/2026 09:00:00", "bidangpasar.indag@gmail.com", "CONTOH PEDAGANG", "'3528014502800001", "Jl. Raya Pasar Waru", "'081234567890", "Kios", "A-01", "Sembako", "10", "Aktif", "Contoh data template pendataan", "", "", "", "https://drive.google.com/file/d/sample1/view", "https://drive.google.com/file/d/sample2/view", "https://drive.google.com/file/d/sample3/view")
            sampleData1.forEachIndexed { i, s -> sampleRow1.createCell(i).setCellValue(s) }

            // Sheet 2: username
            val sheet2 = workbook.createSheet("username")
            val header2 = listOf("Email", "Password", "DisplayName", "LastLogin")
            val headerRow2 = sheet2.createRow(0)
            header2.forEachIndexed { i, s -> headerRow2.createCell(i).setCellValue(s) }
            val sampleRow2 = sheet2.createRow(1)
            listOf("bidangpasar.indag@gmail.com", ".", "Admin Disperindag", "01/08/2026 09:00:00").forEachIndexed { i, s -> sampleRow2.createCell(i).setCellValue(s) }
            val sampleRow3 = sheet2.createRow(2)
            listOf("tim.pendata@ubed.com", ".", "Tim Pendata Ubed", "01/08/2026 09:15:00").forEachIndexed { i, s -> sampleRow3.createCell(i).setCellValue(s) }

            // Sheet 3: aktivitas user
            val sheet3 = workbook.createSheet("aktivitas user")
            val header3 = listOf("Timestamp", "Petugas", "Aktivitas (Log)")
            val headerRow3 = sheet3.createRow(0)
            header3.forEachIndexed { i, s -> headerRow3.createCell(i).setCellValue(s) }
            val sampleRow4 = sheet3.createRow(1)
            listOf("01/08/2026 09:00:00", "Admin Disperindag (bidangpasar.indag@gmail.com)", "[LOGIN] Login berhasil ke aplikasi").forEachIndexed { i, s -> sampleRow4.createCell(i).setCellValue(s) }

            FileOutputStream(file).use { 
                workbook.write(it)
            }
            workbook.close()
            
            Log.d("FileExport", "Template XLSX downloaded to: ${file.absolutePath}")
            return file
        } catch (e: Exception) {
            Log.e("FileExport", "Failed to download XLSX template", e)
            return null
        }
    }

    fun downloadTemplateSpreadsheet(context: Context, csvContent: String): File? {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.getDefault()).format(Date())
            val fileName = "${timestamp}-TemplateSpreadsheet.csv"
            
            val downloadFolder = AppStorageUtils.getCategoryDirectory(
                category = AppStorageUtils.CategoryFolder.EXCEL,
                context = context
            )
            
            val file = File(downloadFolder, fileName)
            FileOutputStream(file).use { 
                it.write(csvContent.toByteArray())
            }
            
            Log.d("FileExport", "Template downloaded to: ${file.absolutePath}")
            return file
        } catch (e: Exception) {
            Log.e("FileExport", "Failed to download template", e)
            return null
        }
    }

    fun openFile(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val mimeType = when {
                file.name.endsWith(".xlsx") -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                file.name.endsWith(".txt") -> "text/plain"
                else -> "text/csv"
            }
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Buka File Dengan Application...").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e("FileExport", "Failed to open file", e)
        }
    }
}
