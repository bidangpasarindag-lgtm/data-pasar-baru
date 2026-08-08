package com.example.util

object AppsScriptUtils {
    const val VERSION = "2026.08.08 - v2.6 (Apps Script Compatibility Verification & Version Check)"

    const val CHANGELOG = """
=== CHANGELOG APPS SCRIPT INTEGRASI PASAR WARU ===

[v2.6 - 08/08/2026 09:40:00 WIB]
• Fitur pengujian kesesuaian/pemeriksaan versi Apps Script aktif di spreadsheet via tombol 'Uji Versi Script'.
• Respons JSON berstandar pada action PING/CHECK_VERSION/GET_VERSION yang mencakup data versi, versionCode, dan timestamp update.
• Otomatisasi deteksi versi lama vs versi terbaru pada aplikasi pendataan Android.

[v2.5 - 08/08/2026 09:22:00 WIB]
• Penyesuaian standar format timestamp pada sheet 'username' (last login) dan sheet 'aktivitas user' menjadi dd/MM/yyyy HH:mm:ss.
• Integrasi otomatisasi sync log aktivitas dan login dengan format tanggal dan jam Indonesia (WIB).
• Peningkatan stabilitas dan kompatibilitas parsing tanggal antara Google Apps Script, Excel, dan Android.

[v2.4 - 08/08/2026 09:00:00 WIB]
• Penyesuaian penuh parameter HTTP POST dari aplikasi Android.
• Otomatisasi pembersihan foto lama di Google Drive saat foto diperbarui atau dihapus (isDeleteFoto = true).
• Pembersihan file Drive secara otomatis saat data pedagang dihapus (DELETE action).
• Pencatatan log aktivitas petugas secara real-time ke sheet 'aktivitas user' (LOGIN, LOGOUT, TAMBAH, EDIT, HAPUS).
• Peningkatan penanganan format NIK & Nomor HP dengan apostrof (') agar tersimpan sebagai teks murni di Google Spreadsheet.
• Dukungan PING/PONG_OK untuk pengujian cepat status koneksi Webhook.

[v2.3 - 01/08/2026 09:00:00 WIB]
• Integrasi autentikasi sheet 'username' dengan timestamp last login.
• Penambahan folder Drive khusus foto pedagang, KTP, dan surat pernyataan.
• Format nama file otomatis berdasar tanggal, jenis foto, jam, dan nama pedagang.
"""

    const val LATEST_CODE = """/**
 * GOOGLE APPS SCRIPT INTEGRASI PENDATAAN PASAR WARU
 * Dinas Perindustrian dan Perdagangan Kabupaten Pamekasan
 * Versi: 2026.08.08 - v2.6 (Apps Script Compatibility Verification & Version Check)
 *
 * CHANGELOG:
 * - v2.6 (08/08/2026 09:40:00 WIB):
 *   1. Fitur pengujian kesesuaian/pemeriksaan versi Apps Script aktif di spreadsheet via tombol 'Uji Versi Script'.
 *   2. Respons JSON berstandar pada action PING/CHECK_VERSION/GET_VERSION.
 *   3. Otomatisasi deteksi versi lama vs versi terbaru pada aplikasi pendataan Android.
 * - v2.5 (08/08/2026 09:22:00 WIB):
 *   1. Penyesuaian standar format timestamp pada sheet 'username' (last login) dan sheet 'aktivitas user' (dd/MM/yyyy HH:mm:ss).
 *   2. Integrasi otomatisasi sync log aktivitas dengan format tanggal & jam Indonesia (WIB).
 * - v2.4 (08/08/2026 09:00:00 WIB):
 *   1. Penyesuaian penuh parameter HTTP POST dari aplikasi Android.
 *   2. Otomatisasi hapus foto lama di Google Drive saat diperbarui / dihapus.
 *   3. Pembersihan file Drive otomatis saat data pedagang dihapus.
 *   4. Log real-time ke sheet 'aktivitas user' (LOGIN, LOGOUT, TAMBAH, EDIT, HAPUS).
 *   5. Penanganan NIK & HP dengan apostrof (') agar tersimpan sebagai teks murni.
 *   6. Dukungan PING / PONG_OK untuk pengujian cepat status Webhook.
 */

function doPost(e) {
  try {
    var params = e.parameter;
    var action = params.action || "CREATE";
    var ssId = params.spreadsheet_id || "1Q7OtJ1fuEwkycAtnAjRNrSNrAEJ5SxjGRn-ge9YcWlU";
    var gid = params.sheetGid || "1751220302";
    
    if (action === "PING" || action === "CHECK_VERSION" || action === "GET_VERSION") {
      return ContentService.createTextOutput(JSON.stringify({
        status: "PONG_OK",
        version: "v2.6",
        versionCode: 2026080803,
        versionName: "2026.08.08 - v2.6 (Apps Script Compatibility Verification & Version Check)",
        updatedAt: "08/08/2026 09:40:00 WIB"
      })).setMimeType(ContentService.MimeType.JSON);
    }
    
    var ss = SpreadsheetApp.openById(ssId);
    
    // LOGIN ACTION
    if (action === "LOGIN") {
      var username = (params.username || "").toString().trim().toLowerCase();
      var password = (params.password || "").toString().trim();
      
      var userSheet = ss.getSheetByName("username");
      if (!userSheet) {
        return ContentService.createTextOutput(JSON.stringify({
          status: "ERROR",
          message: "Sheet 'username' tidak ditemukan di Google Spreadsheet!"
        })).setMimeType(ContentService.MimeType.JSON);
      }
      
      var userData = userSheet.getDataRange().getValues();
      for (var i = 1; i < userData.length; i++) {
        var rowUser = (userData[i][0] || "").toString().trim().toLowerCase();
        var rowPass = (userData[i][1] || "").toString().trim();
        var displayName = (userData[i][2] || "").toString().trim();
        
        if (rowUser === username && rowPass === password) {
          // Update last login
          var now = Utilities.formatDate(new Date(), "GMT+7", "dd/MM/yyyy HH:mm:ss");
          userSheet.getRange(i + 1, 4).setValue(now);
          
          // Log activity
          logActivity(ss, username, displayName, "LOGIN", "Login berhasil ke aplikasi");
          
          return ContentService.createTextOutput(JSON.stringify({
            "status": "SUCCESS",
            "username": username,
            "displayName": displayName
          })).setMimeType(ContentService.MimeType.JSON);
        }
      }
      
      return ContentService.createTextOutput(JSON.stringify({
        status: "ERROR",
        message: "Username atau Password salah."
      })).setMimeType(ContentService.MimeType.JSON);
    }
    
    // LOG ACTIVITY ACTION
    if (action === "LOG_ACTIVITY") {
      var username = (params.operatorUsername || "bidangpasar.indag@gmail.com").toString().trim();
      var name = (params.operatorName || "Petugas").toString().trim();
      var aktivitas = (params.aktivitas || "Unknown").toString().trim();
      var keterangan = (params.keterangan || "").toString().trim();
      
      logActivity(ss, username, name, aktivitas, keterangan);
      return ContentService.createTextOutput(JSON.stringify({status: "SUCCESS"}))
        .setMimeType(ContentService.MimeType.JSON);
    }
    
    // Other actions (CREATE, UPDATE, DELETE)
    var sheet = getSheetByGid(ss, gid) || ss.getSheets()[0];
    var driveFolderId = params.driveFolderId || "1G81CN0555Gst93hIosHG0lrUf4pP0ELO";
    var folder = DriveApp.getFolderById(driveFolderId);
    
    var dateObj = new Date();
    var year = dateObj.getFullYear();
    var month = ("0" + (dateObj.getMonth() + 1)).slice(-2);
    var date = ("0" + dateObj.getDate()).slice(-2);
    var yyyymmdd = "" + year + month + date;
    var hours = ("0" + dateObj.getHours()).slice(-2);
    var minutes = ("0" + dateObj.getMinutes()).slice(-2);
    var seconds = ("0" + dateObj.getSeconds()).slice(-2);
    var jam = "" + hours + minutes + seconds;
    var sanitizedNama = (params.namaPedagang || "PEDAGANG").replace(/[^a-zA-Z0-9\s-_]/g, "").trim();
    
    var fotoPedagangName = "";
    var fotoKtpName = "";
    var fotoSuratName = "";
    var fotoPedagangUrl = params.fotoPedagangUri || "";
    var fotoKtpUrl = params.fotoKtpUri || "";
    var fotoSuratUrl = params.fotoSuratPernyataanUri || "";
    
    if (action === "UPDATE") {
      var targetRow = findRowByNameOrNik(sheet, params.namaPedagang, params.nik);
      if (targetRow > 0) {
        var existingRowData = sheet.getRange(targetRow, 1, 1, 18).getValues()[0];
        if (existingRowData.length >= 18) {
          fotoPedagangName = existingRowData[12] || "";
          fotoKtpName = existingRowData[13] || "";
          fotoSuratName = existingRowData[14] || "";
          fotoPedagangUrl = existingRowData[15] || "";
          fotoKtpUrl = existingRowData[16] || "";
          fotoSuratUrl = existingRowData[17] || "";
        }
      }
    }
    
    if (params.fotoPedagangBase64) {
      if (fotoPedagangUrl) deleteFileByUrl(fotoPedagangUrl);
      var fName = yyyymmdd + "-FOTO PEDAGANG-" + jam + "-" + sanitizedNama + ".jpg";
      var file = folder.createFile(Utilities.newBlob(Utilities.base64Decode(params.fotoPedagangBase64), "image/jpeg", fName));
      file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
      fotoPedagangUrl = file.getUrl();
      fotoPedagangName = "Form Responses 1_Images/" + fName;
    } else if (params.isDeleteFotoPedagang === "true") {
      if (fotoPedagangUrl) deleteFileByUrl(fotoPedagangUrl);
      fotoPedagangUrl = "";
      fotoPedagangName = "";
    }
    
    if (params.fotoKtpBase64) {
      if (fotoKtpUrl) deleteFileByUrl(fotoKtpUrl);
      var fNameKtp = yyyymmdd + "-FOTO KTP-" + jam + "-" + sanitizedNama + ".jpg";
      var fileKtp = folder.createFile(Utilities.newBlob(Utilities.base64Decode(params.fotoKtpBase64), "image/jpeg", fNameKtp));
      fileKtp.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
      fotoKtpUrl = fileKtp.getUrl();
      fotoKtpName = "Form Responses 1_Images/" + fNameKtp;
    } else if (params.isDeleteFotoKtp === "true") {
      if (fotoKtpUrl) deleteFileByUrl(fotoKtpUrl);
      fotoKtpUrl = "";
      fotoKtpName = "";
    }
    
    if (params.fotoSuratBase64) {
      if (fotoSuratUrl) deleteFileByUrl(fotoSuratUrl);
      var fNameSurat = yyyymmdd + "-FOTO SURAT PERNYATAAN-" + jam + "-" + sanitizedNama + ".jpg";
      var fileSurat = folder.createFile(Utilities.newBlob(Utilities.base64Decode(params.fotoSuratBase64), "image/jpeg", fNameSurat));
      fileSurat.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
      fotoSuratUrl = fileSurat.getUrl();
      fotoSuratName = "Form Responses 1_Images/" + fNameSurat;
    } else if (params.isDeleteFotoSurat === "true") {
      if (fotoSuratUrl) deleteFileByUrl(fotoSuratUrl);
      fotoSuratUrl = "";
      fotoSuratName = "";
    }
    
    var rowData = [
      params.timestamp || new Date(),
      params.emailAddress || "bidangpasar.indag@gmail.com",
      params.namaPedagang,
      "'" + params.nik,
      params.alamat,
      "'" + params.nomorHp,
      params.jenisRuangDagang,
      params.nomorKiosLos,
      params.komoditi,
      params.lamaBerjualan,
      params.status,
      params.keterangan,
      fotoPedagangName,
      fotoKtpName,
      fotoSuratName,
      fotoPedagangUrl,
      fotoKtpUrl,
      fotoSuratUrl
    ];
    
    var operatorEmail = params.emailAddress || "bidangpasar.indag@gmail.com";
    var operatorName = params.operatorName || "Petugas";
    
    if (action === "CREATE") {
      sheet.appendRow(rowData);
      logActivity(ss, operatorEmail, operatorName, "TAMBAH_PEDAGANG", "Menambah pedagang baru: " + params.namaPedagang);
    } else if (action === "UPDATE") {
      var targetRow = findRowByNameOrNik(sheet, params.namaPedagang, params.nik);
      if (targetRow > 0) {
        sheet.getRange(targetRow, 1, 1, rowData.length).setValues([rowData]);
        logActivity(ss, operatorEmail, operatorName, "EDIT_PEDAGANG", "Mengubah data pedagang: " + params.namaPedagang);
      } else {
        sheet.appendRow(rowData);
        logActivity(ss, operatorEmail, operatorName, "TAMBAH_PEDAGANG", "Menambah pedagang baru (update target tidak ditemukan): " + params.namaPedagang);
      }
    } else if (action === "DELETE") {
      var targetRowDel = findRowByNameOrNik(sheet, params.namaPedagang, params.nik);
      if (targetRowDel > 0) {
        var existingRowData = sheet.getRange(targetRowDel, 1, 1, 18).getValues()[0];
        if (existingRowData.length >= 18) {
          if (existingRowData[15]) deleteFileByUrl(existingRowData[15]);
          if (existingRowData[16]) deleteFileByUrl(existingRowData[16]);
          if (existingRowData[17]) deleteFileByUrl(existingRowData[17]);
        }
        sheet.deleteRow(targetRowDel);
        logActivity(ss, operatorEmail, operatorName, "HAPUS_PEDAGANG", "Menghapus data pedagang: " + params.namaPedagang);
      }
    }
    
    return ContentService.createTextOutput(JSON.stringify({status: "SUCCESS", action: action}))
      .setMimeType(ContentService.MimeType.JSON);
  } catch (err) {
    return ContentService.createTextOutput(JSON.stringify({status: "ERROR", message: err.toString()}))
      .setMimeType(ContentService.MimeType.JSON);
  }
}

function logActivity(ss, email, name, action, details) {
  try {
    var actSheet = ss.getSheetByName("aktivitas user");
    if (!actSheet) {
      actSheet = ss.insertSheet("aktivitas user");
      actSheet.appendRow(["Timestamp", "Petugas", "Aktivitas (Log)"]);
      actSheet.getRange(1, 1, 1, 3).setFontWeight("bold").setBackground("#E2E8F0");
    }
    var now = Utilities.formatDate(new Date(), "GMT+7", "dd/MM/yyyy HH:mm:ss");
    var fullActivity = "[" + action.toUpperCase() + "] " + details;
    actSheet.appendRow([now, name + " (" + email + ")", fullActivity]);
  } catch (e) {
    console.error("Log error: " + e.toString());
  }
}

function getSheetByGid(ss, gid) {
  var sheets = ss.getSheets();
  for (var i = 0; i < sheets.length; i++) {
    if (sheets[i].getSheetId().toString() === gid.toString()) return sheets[i];
  }
  return null;
}

function findRowByNameOrNik(sheet, nama, nik) {
  var data = sheet.getDataRange().getValues();
  for (var i = 1; i < data.length; i++) {
    if ((data[i][2] && data[i][2].toString().toLowerCase() === nama.toLowerCase()) ||
        (data[i][3] && data[i][3].toString().replace("'", "") === nik.toString().replace("'", ""))) {
      return i + 1;
    }
  }
  return -1;
}

function deleteFileByUrl(url) {
  try {
    var match = url.match(/[-\w]{25,}/);
    if (match) DriveApp.getFileById(match[0]).setTrashed(true);
  } catch (e) {}
}
"""
}

