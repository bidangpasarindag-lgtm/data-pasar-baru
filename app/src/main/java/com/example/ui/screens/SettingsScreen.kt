package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ProgressDialog
import coil.compose.AsyncImage
import com.example.R
import com.example.data.config.AgencyConfig
import com.example.data.config.AgencyConfigManager
import com.example.data.model.Pedagang
import com.example.data.remote.GoogleSheetSyncService
import com.example.ui.theme.DisperindagAccentGold
import com.example.ui.theme.DisperindagGreenPrimary
import com.example.util.DriveImageUtils
import com.example.util.QrCodeUtils
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    lastSyncTime: String,
    isSyncing: Boolean,
    totalPedagangCount: Int,
    onSyncClick: () -> Unit,
    onExportCsvClick: () -> Unit,
    onRebuildDropdownClick: () -> Unit,
    onSettingsSaved: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
        val currentConfig by AgencyConfigManager.config.collectAsState()
    var stateConfig by androidx.compose.runtime.remember(currentConfig) { androidx.compose.runtime.mutableStateOf(currentConfig) }
    var isSavingSettings by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var activeTab by remember { mutableStateOf("cloud") } // "cloud", "pdf", "sistem"

    var showResetDialog by remember { mutableStateOf(false) }
    var isTestingConnection by remember { mutableStateOf(false) }
    var connectionResult by remember { mutableStateOf<String?>(null) }
    var showGuide by remember { mutableStateOf(false) }

    // Helper url parsing functions
    fun parseSpreadsheetUrl(input: String) {
        val trimmed = input.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            val ssIdMatch = Regex("/d/([a-zA-Z0-9-_]+)").find(trimmed)
            val gidMatch = Regex("[?#&]gid=([0-9]+)").find(trimmed)
            var updated = stateConfig
            if (ssIdMatch != null) {
                updated = updated.copy(spreadsheetId = ssIdMatch.groupValues[1])
            }
            if (gidMatch != null) {
                updated = updated.copy(sheetGid = gidMatch.groupValues[1])
            }
            stateConfig = updated
            Toast.makeText(context, "Spreadsheet ID & GID berhasil diekstrak!", Toast.LENGTH_SHORT).show()
        } else {
            stateConfig = stateConfig.copy(spreadsheetId = trimmed)
        }
    }

    fun parseDriveFolderUrl(input: String) {
        val trimmed = input.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            val folderMatch = Regex("/folders/([a-zA-Z0-9-_]+)").find(trimmed)
            if (folderMatch != null) {
                stateConfig = stateConfig.copy(driveFolderId = folderMatch.groupValues[1])
                Toast.makeText(context, "Drive Folder ID berhasil diekstrak!", Toast.LENGTH_SHORT).show()
            } else {
                stateConfig = stateConfig.copy(driveFolderId = trimmed)
            }
        } else {
            stateConfig = stateConfig.copy(driveFolderId = trimmed)
        }
    }

    // Logo Picker Launcher
    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            stateConfig = stateConfig.copy(customLogoUri = it.toString())
            Toast.makeText(context, "Logo kustom dipilih", Toast.LENGTH_SHORT).show()
        }
    }

    // JSON Restore Launcher
    val jsonRestoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val jsonString = inputStream?.bufferedReader().use { r -> r?.readText() } ?: ""
                val result = AgencyConfigManager.importConfigFromJson(context, jsonString)
                if (result.isSuccess) {
                    Toast.makeText(context, "✓ Konfigurasi aplikasi berhasil dipulihkan!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "❌ Gagal merestore JSON konfigurasi", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Pengaturan Sistem Integrasi", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DisperindagGreenPrimary)
                        Text("Konfigurasi tersinkronisasi langsung dengan cloud", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("reset_settings_button"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset Default", fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                isSavingSettings = true
                                kotlinx.coroutines.delay(800) // Visual feedback
                                AgencyConfigManager.updateConfig(context, stateConfig)
                                isSavingSettings = false
                                Toast.makeText(context, "✓ Semua pengaturan berhasil disimpan!", Toast.LENGTH_SHORT).show()
                                onSettingsSaved()
                            }
                        },
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("save_settings_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = DisperindagGreenPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Simpan Pengaturan", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // HIGHLY INTERACTIVE CATEGORY TABS
            TabRow(
                selectedTabIndex = when (activeTab) {
                    "cloud" -> 0
                    "pdf" -> 1
                    else -> 2
                },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = DisperindagGreenPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[when (activeTab) {
                            "cloud" -> 0
                            "pdf" -> 1
                            else -> 2
                        }]),
                        color = DisperindagGreenPrimary
                    )
                }
            ) {
                Tab(
                    selected = activeTab == "cloud",
                    onClick = { activeTab = "cloud" },
                    text = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, sizeModifier(activeTab == "cloud"))
                            Text("Cloud & Sync", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = activeTab == "pdf",
                    onClick = { activeTab = "pdf" },
                    text = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, sizeModifier(activeTab == "pdf"))
                            Text("Desain & PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = activeTab == "sistem",
                    onClick = { activeTab = "sistem" },
                    text = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Tune, contentDescription = null, sizeModifier(activeTab == "sistem"))
                            Text("Sistem & Aturan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (activeTab) {
                    "cloud" -> {
                        // TAB 1: CLOUD & GOOGLE SHEET SYNCHRONIZATION
                        Text(
                            text = "KONEKTIVITAS CLOUD & GOOGLE SPREADSHEET",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = DisperindagGreenPrimary
                        )

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Spreadsheet ID Input
                                OutlinedTextField(
                                    value = stateConfig.spreadsheetId,
                                    onValueChange = { parseSpreadsheetUrl(it) },
                                    label = { Text("Google Spreadsheet ID / Link Full") },
                                    placeholder = { Text("Masukkan ID atau tempel link spreadsheet") },
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            pasteFromClipboard(context) { parseSpreadsheetUrl(it) }
                                        }) {
                                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = DisperindagGreenPrimary)
                                        }
                                    },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("input_spreadsheet_id")
                                )

                                OutlinedTextField(
                                    value = stateConfig.sheetGid,
                                    onValueChange = { stateConfig = stateConfig.copy(sheetGid = it.trim()) },
                                    label = { Text("Sheet GID") },
                                    placeholder = { Text("1751220302") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("input_sheet_gid")
                                )

                                // Drive Folder ID Input
                                OutlinedTextField(
                                    value = stateConfig.driveFolderId,
                                    onValueChange = { parseDriveFolderUrl(it) },
                                    label = { Text("Google Drive Folder ID / Link Folder Foto") },
                                    placeholder = { Text("Masukkan ID atau tempel link folder Google Drive") },
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            pasteFromClipboard(context) { parseDriveFolderUrl(it) }
                                        }) {
                                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = DisperindagGreenPrimary)
                                        }
                                    },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("input_drive_folder_id")
                                )

                                Button(
                                    onClick = {
                                        val url = "https://drive.google.com/drive/folders/${stateConfig.driveFolderId.ifBlank { "1G81CN0555Gst93hIosHG0lrUf4pP0ELO" }}"
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Buka Folder Google Drive Foto", fontSize = 12.sp)
                                }

                                // Webhook Apps Script URL
                                OutlinedTextField(
                                    value = stateConfig.webhookUrl,
                                    onValueChange = { stateConfig = stateConfig.copy(webhookUrl = it.trim()) },
                                    label = { Text("URL Webhook Apps Script") },
                                    placeholder = { Text("https://script.google.com/macros/s/.../exec") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("input_webhook_url")
                                )

                                // Interactive Webhook Tester
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                isTestingConnection = true
                                                connectionResult = "Mengirim ping ke Apps Script..."
                                                val service = GoogleSheetSyncService()
                                                val res = service.testWebhookConnection()
                                                connectionResult = if (res.isSuccess) "✓ Webhook Terkoneksi Lancar!" else "❌ Koneksi Gagal, cek URL"
                                                isTestingConnection = false
                                            }
                                        },
                                        enabled = !isTestingConnection,
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                                    ) {
                                        if (isTestingConnection) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        } else {
                                            Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Uji Webhook", fontSize = 12.sp)
                                        }
                                    }

                                    Button(
                                        onClick = onSyncClick,
                                        enabled = !isSyncing,
                                        modifier = Modifier.weight(1.2f),
                                        colors = ButtonDefaults.buttonColors(containerColor = DisperindagGreenPrimary)
                                    ) {
                                        if (isSyncing) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                        } else {
                                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Tarik Data", fontSize = 12.sp)
                                        }
                                    }
                                }

                                connectionResult?.let {
                                    Text(
                                        text = it,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (it.startsWith("✓")) Color(0xFF2E7D32) else Color.Red,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        // Collapsible Apps Script Code Section
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            var isCodeExpanded by remember { mutableStateOf(false) }
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { isCodeExpanded = !isCodeExpanded }
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Default.Code, contentDescription = null, tint = DisperindagGreenPrimary)
                                        Text("Kode Google Apps Script Webhook (Utuh)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DisperindagGreenPrimary)
                                    }
                                    Icon(if (isCodeExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
                                }

                                AnimatedVisibility(visible = isCodeExpanded) {
                                    val fullAppsScriptCode = """
/**
 * GOOGLE APPS SCRIPT INTEGRASI PENDATAAN PASAR WARU
 * Dinas Perindustrian dan Perdagangan Kabupaten Pamekasan
 */

function doPost(e) {
  try {
    var params = e.parameter;
    var action = params.action || "CREATE";
    var ssId = params.spreadsheet_id || "1Q7OtJ1fuEwkycAtnAjRNrSNrAEJ5SxjGRn-ge9YcWlU";
    var gid = params.sheetGid || "1751220302";
    
    if (action === "PING") {
      return ContentService.createTextOutput("PONG_OK");
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
          var now = Utilities.formatDate(new Date(), "GMT+7", "yyyy-MM-dd HH:mm:ss");
          userSheet.getRange(i + 1, 4).setValue(now);
          
          // Log activity
          logActivity(ss, username, displayName, "LOGIN", "Login berhasil ke aplikasi");
          
          return ContentService.createTextOutput(JSON.stringify({
            status: "SUCCESS",
            username: username,
            displayName: displayName
          })).setMimeType(ContentService.MimeType.JSON);
        }
      }
      
      return ContentService.createTextOutput(JSON.stringify({
        status: "ERROR",
        message: "Username atau Password salah."
      })).setMimeType(ContentService.MimeType.JSON);
    }
    
    // Other actions (CREATE, UPDATE, DELETE)
    var sheet = getSheetByGid(ss, gid) || ss.getSheets()[0];
    
    // Auto Save Base64 Photos to Google Drive
    var driveFolderId = params.driveFolderId || "1G81CN0555Gst93hIosHG0lrUf4pP0ELO";
    var folder = DriveApp.getFolderById(driveFolderId);
    
    // Parse timestamp or get current time for YYYYMMDD and JAM (HHmmss)
    var dateObj = new Date();
    if (params.timestamp) {
      try {
        var tParts = params.timestamp.split(" ");
        if (tParts.length >= 2) {
          var dParts = tParts[0].split("-");
          var hParts = tParts[1].split(":");
          if (dParts.length === 3 && hParts.length === 3) {
            dateObj = new Date(
              parseInt(dParts[0], 10),
              parseInt(dParts[1], 10) - 1,
              parseInt(dParts[2], 10),
              parseInt(hParts[0], 10),
              parseInt(hParts[1], 10),
              parseInt(hParts[2], 10)
            );
          }
        }
      } catch (e) {
        // Fallback
      }
    }
    
    // Format YYYYMMDD
    var year = dateObj.getFullYear();
    var month = ("0" + (dateObj.getMonth() + 1)).slice(-2);
    var date = ("0" + dateObj.getDate()).slice(-2);
    var yyyymmdd = "" + year + month + date;
    
    // Format JAM as HHmmss
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
    
    // If UPDATE, pull existing values first to preserve old filenames/links if not updated
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
    
    // Delete existing photos if explicitly deleted or updated
    if (action === "UPDATE") {
      if (params.isDeleteFotoPedagang === "true" && fotoPedagangUrl) {
        deleteFileByUrl(fotoPedagangUrl);
        fotoPedagangName = "";
        fotoPedagangUrl = "";
      }
      if (params.isDeleteFotoKtp === "true" && fotoKtpUrl) {
        deleteFileByUrl(fotoKtpUrl);
        fotoKtpName = "";
        fotoKtpUrl = "";
      }
      if (params.isDeleteFotoSurat === "true" && fotoSuratUrl) {
        deleteFileByUrl(fotoSuratUrl);
        fotoSuratName = "";
        fotoSuratUrl = "";
      }
    }
    
    // Upload FOTO PEDAGANG
    if (params.fotoPedagangBase64) {
      if (fotoPedagangUrl) {
        deleteFileByUrl(fotoPedagangUrl);
      }
      var fName = yyyymmdd + "-FOTO PEDAGANG-" + jam + "-" + sanitizedNama + ".jpg";
      var file = folder.createFile(Utilities.newBlob(Utilities.base64Decode(params.fotoPedagangBase64), "image/jpeg", fName));
      file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
      fotoPedagangUrl = file.getUrl();
      fotoPedagangName = "Form Responses 1_Images/" + fName;
    }
    
    // Upload FOTO KTP
    if (params.fotoKtpBase64) {
      if (fotoKtpUrl) {
        deleteFileByUrl(fotoKtpUrl);
      }
      var fNameKtp = yyyymmdd + "-FOTO KTP-" + jam + "-" + sanitizedNama + ".jpg";
      var fileKtp = folder.createFile(Utilities.newBlob(Utilities.base64Decode(params.fotoKtpBase64), "image/jpeg", fNameKtp));
      fileKtp.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
      fotoKtpUrl = fileKtp.getUrl();
      fotoKtpName = "Form Responses 1_Images/" + fNameKtp;
    }
    
    // Upload FOTO SURAT PERNYATAAN
    if (params.fotoSuratBase64) {
      if (fotoSuratUrl) {
        deleteFileByUrl(fotoSuratUrl);
      }
      var fNameSurat = yyyymmdd + "-FOTO SURAT PERNYATAAN-" + jam + "-" + sanitizedNama + ".jpg";
      var fileSurat = folder.createFile(Utilities.newBlob(Utilities.base64Decode(params.fotoSuratBase64), "image/jpeg", fNameSurat));
      fileSurat.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
      fotoSuratUrl = fileSurat.getUrl();
      fotoSuratName = "Form Responses 1_Images/" + fNameSurat;
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
      fotoPedagangName, // Column M (13)
      fotoKtpName,      // Column N (14)
      fotoSuratName,    // Column O (15)
      fotoPedagangUrl,  // Column P (16)
      fotoKtpUrl,       // Column Q (17)
      fotoSuratUrl      // Column R (18)
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
        logActivity(ss, operatorEmail, operatorName, "TAMBAH_PEDAGANG", "Menambah pedagang baru (karena update tidak ditemukan): " + params.namaPedagang);
      }
    } else if (action === "DELETE") {
      var targetRowDel = findRowByNameOrNik(sheet, params.namaPedagang, params.nik);
      if (targetRowDel > 0) {
        var existingRowData = sheet.getRange(targetRowDel, 1, 1, 18).getValues()[0];
        if (existingRowData.length >= 18) {
          var delFotoPedagangUrl = existingRowData[15] || "";
          var delFotoKtpUrl = existingRowData[16] || "";
          var delFotoSuratUrl = existingRowData[17] || "";
          
          if (delFotoPedagangUrl) deleteFileByUrl(delFotoPedagangUrl);
          if (delFotoKtpUrl) deleteFileByUrl(delFotoKtpUrl);
          if (delFotoSuratUrl) deleteFileByUrl(delFotoSuratUrl);
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
      actSheet.appendRow(["Timestamp", "Email", "Nama Petugas", "Aktivitas", "Keterangan"]);
    }
    var now = Utilities.formatDate(new Date(), "GMT+7", "yyyy-MM-dd HH:mm:ss");
    actSheet.appendRow([now, email, name, action, details]);
  } catch (e) {
    // ignore logging error
  }
}

function getSheetByGid(ss, gid) {
  var sheets = ss.getSheets();
  for (var i = 0; i < sheets.length; i++) {
    if (sheets[i].getSheetId().toString() === gid.toString()) {
      return sheets[i];
    }
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

function extractFileId(url) {
  if (!url) return null;
  var match = url.match(/[-\w]{25,}/);
  return match ? match[0] : null;
}

function deleteFileByUrl(url) {
  try {
    var fileId = extractFileId(url);
    if (fileId) {
      DriveApp.getFileById(fileId).setTrashed(true);
    }
  } catch (e) {
    // ignore
  }
}
                                    """.trimIndent()

                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Surface(
                                            color = Color(0xFF1E272C),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth().height(200.dp)
                                        ) {
                                            SelectionContainer {
                                                Text(
                                                    text = fullAppsScriptCode,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 10.sp,
                                                    color = Color(0xFF80CBC4),
                                                    modifier = Modifier.padding(10.dp).verticalScroll(rememberScrollState())
                                                )
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("Apps Script", fullAppsScriptCode))
                                                Toast.makeText(context, "✓ Berhasil menyalin kode Apps Script!", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = DisperindagGreenPrimary),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Salin Kode Google Apps Script")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "pdf" -> {
                        // TAB 2: BRANDING & PDF LAYOUT DESIGN
                        Text(
                            text = "DESAIN KARTU PDF & BRANDING INSTANSI",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = DisperindagGreenPrimary
                        )

                        // LIVE CARD PREVIEW
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Real-Time Preview Kartu PDF Pendataan:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DisperindagGreenPrimary)
                                Spacer(modifier = Modifier.height(10.dp))
                                LivePdfCardPreview(config = stateConfig)
                            }
                        }

                        // Logo Instansi Picker
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .border(2.dp, DisperindagGreenPrimary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (stateConfig.customLogoUri.isNotBlank()) {
                                        AsyncImage(
                                            model = DriveImageUtils.convertToDirectUrl(stateConfig.customLogoUri) ?: stateConfig.customLogoUri,
                                            contentDescription = "Custom Logo",
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.size(48.dp)
                                        )
                                    } else {
                                        Image(
                                            painter = painterResource(id = R.drawable.ic_pamekasan_logo),
                                            contentDescription = "Default Logo",
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.size(48.dp)
                                        )
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(
                                        onClick = { logoPickerLauncher.launch("image/*") },
                                        colors = ButtonDefaults.buttonColors(containerColor = DisperindagGreenPrimary),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Pilih Logo Custom", fontSize = 11.sp)
                                    }
                                    if (stateConfig.customLogoUri.isNotBlank()) {
                                        OutlinedButton(
                                            onClick = { stateConfig = stateConfig.copy(customLogoUri = "") },
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Reset Logo Default", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // Identitas Instansi Fields
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Identitas Instansi & Header Aplikasi:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DisperindagGreenPrimary)
                                
                                OutlinedTextField(
                                    value = stateConfig.appTitleHeader,
                                    onValueChange = { stateConfig = stateConfig.copy(appTitleHeader = it) },
                                    label = { Text("Header Singkatan Aplikasi") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = stateConfig.appLoginTitle,
                                    onValueChange = { stateConfig = stateConfig.copy(appLoginTitle = it) },
                                    label = { Text("Judul Aplikasi Halaman Login") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = stateConfig.namaPemerintah,
                                    onValueChange = { stateConfig = stateConfig.copy(namaPemerintah = it) },
                                    label = { Text("Nama Pemerintah Daerah") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = stateConfig.namaDinas,
                                    onValueChange = { stateConfig = stateConfig.copy(namaDinas = it) },
                                    label = { Text("Nama Dinas / Instansi") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = stateConfig.namaPasar,
                                    onValueChange = { stateConfig = stateConfig.copy(namaPasar = it) },
                                    label = { Text("Nama Unit Pasar") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // PDF Storage & Layout configuration
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Pengaturan Penyimpanan File PDF:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DisperindagGreenPrimary)

                                OutlinedTextField(
                                    value = stateConfig.pdfStorageSubfolder,
                                    onValueChange = { stateConfig = stateConfig.copy(pdfStorageSubfolder = it) },
                                    label = { Text("Subfolder PDF") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = stateConfig.pdfFileNameFormat,
                                    onValueChange = { stateConfig = stateConfig.copy(pdfFileNameFormat = it) },
                                    label = { Text("Format Nama File ({NAMA}, {NOMOR_KIOS})") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Text("Direktori Penyimpanan Utama PDF:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val isDoc = stateConfig.pdfStorageDirectory == "DOCUMENTS"
                                    val isDl = stateConfig.pdfStorageDirectory == "DOWNLOADS"
                                    Button(
                                        onClick = { stateConfig = stateConfig.copy(pdfStorageDirectory = "DOCUMENTS") },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isDoc) DisperindagGreenPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (isDoc) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f).testTag("dir_documents_button")
                                    ) {
                                        Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Documents", fontSize = 12.sp)
                                    }
                                    Button(
                                        onClick = { stateConfig = stateConfig.copy(pdfStorageDirectory = "DOWNLOADS") },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isDl) DisperindagGreenPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (isDl) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f).testTag("dir_downloads_button")
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Downloads", fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        // Customize dynamic PDF text values
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Ubah Text Dinamis Kop Surat & Kartu PDF:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DisperindagGreenPrimary)

                                OutlinedTextField(
                                    value = stateConfig.pdfHeaderTitle,
                                    onValueChange = { stateConfig = stateConfig.copy(pdfHeaderTitle = it) },
                                    label = { Text("Nama Pemerintah Kop") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = stateConfig.pdfHeaderSubtitle,
                                    onValueChange = { stateConfig = stateConfig.copy(pdfHeaderSubtitle = it) },
                                    label = { Text("Nama Dinas Kop") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = stateConfig.pdfHeaderAddress,
                                    onValueChange = { stateConfig = stateConfig.copy(pdfHeaderAddress = it) },
                                    label = { Text("Alamat Kantor Kop") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = stateConfig.pdfLabelTerdataResmi,
                                    onValueChange = { stateConfig = stateConfig.copy(pdfLabelTerdataResmi = it) },
                                    label = { Text("Teks Custom: Pedagang dengan data di atas telah TERDATA...") },
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 3
                                )

                                OutlinedTextField(
                                    value = stateConfig.pdfLabelDiterbitkan,
                                    onValueChange = { stateConfig = stateConfig.copy(pdfLabelDiterbitkan = it) },
                                    label = { Text("Teks Custom: Diterbitkan oleh Dinas Perindustrian...") },
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 3
                                )
                            }
                        }

                        // Customize labels
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Kustomisasi Label / Teks PDF (Dinamis):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DisperindagGreenPrimary)

                                OutlinedTextField(
                                    value = stateConfig.pdfLabelNama,
                                    onValueChange = { stateConfig = stateConfig.copy(pdfLabelNama = it) },
                                    label = { Text("Label Nama Pedagang") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = stateConfig.pdfLabelNik,
                                    onValueChange = { stateConfig = stateConfig.copy(pdfLabelNik = it) },
                                    label = { Text("Label NIK / KTP") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = stateConfig.pdfLabelRuang,
                                    onValueChange = { stateConfig = stateConfig.copy(pdfLabelRuang = it) },
                                    label = { Text("Label Jenis Ruang") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // Visibilitas PDF
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Visibilitas Elemen Kartu PDF:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DisperindagGreenPrimary)
                                Spacer(modifier = Modifier.height(4.dp))
                                CheckboxSettingRow("Tampilkan Kop Surat Resmi", stateConfig.pdfShowKopSurat) { stateConfig = stateConfig.copy(pdfShowKopSurat = it) }
                                CheckboxSettingRow("Tampilkan Logo Instansi", stateConfig.pdfShowLogo) { stateConfig = stateConfig.copy(pdfShowLogo = it) }
                                CheckboxSettingRow("Tampilkan Kode QR Identifikasi", stateConfig.pdfShowQrCode) { stateConfig = stateConfig.copy(pdfShowQrCode = it) }
                                CheckboxSettingRow("Tampilkan Foto Pedagang", stateConfig.pdfShowFotoPedagang) { stateConfig = stateConfig.copy(pdfShowFotoPedagang = it) }
                                CheckboxSettingRow("Tampilkan Nama Petugas Pendata (Footer)", stateConfig.pdfShowPetugas) { stateConfig = stateConfig.copy(pdfShowPetugas = it) }
                            }
                        }
                    }

                    else -> {
                        // TAB 3: SYSTEM RULES & CRUD ACCESS CONTROL
                        Text(
                            text = "ATURAN VALIDASI DATA & HAK AKSES SISTEM",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = DisperindagGreenPrimary
                        )

                        // Completeness rules
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Aturan Kelengkapan Data Pedagang:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DisperindagGreenPrimary)

                                SwitchSettingRow(
                                    label = "Tampilkan Warning Data Belum Lengkap",
                                    checked = stateConfig.enableCompletenessWarning,
                                    onCheckedChange = { stateConfig = stateConfig.copy(enableCompletenessWarning = it) }
                                )

                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                Text("Syarat Wajib Isi Untuk Berkas Lengkap:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)

                                CheckboxSettingRow("NIK (Wajib 16 Digit)", stateConfig.requireNik) { stateConfig = stateConfig.copy(requireNik = it) }
                                CheckboxSettingRow("Nomor HP (Hubungi Pedagang)", stateConfig.requireNomorHp) { stateConfig = stateConfig.copy(requireNomorHp = it) }
                                CheckboxSettingRow("Alamat Domisili", stateConfig.requireAlamat) { stateConfig = stateConfig.copy(requireAlamat = it) }
                                CheckboxSettingRow("Foto Pedagang (Profil)", stateConfig.requireFotoPedagang) { stateConfig = stateConfig.copy(requireFotoPedagang = it) }
                                CheckboxSettingRow("Foto KTP Pedagang", stateConfig.requireFotoKtp) { stateConfig = stateConfig.copy(requireFotoKtp = it) }
                                CheckboxSettingRow("Foto Surat Pernyataan", stateConfig.requireFotoSurat) { stateConfig = stateConfig.copy(requireFotoSurat = it) }
                            }
                        }

                        // List view attributes visibility
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Atribut yang Tampil di Daftar Pedagang:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DisperindagGreenPrimary)
                                CheckboxSettingRow("Tampilkan NIK di Card", stateConfig.cardShowNik) { stateConfig = stateConfig.copy(cardShowNik = it) }
                                CheckboxSettingRow("Tampilkan Nomor HP di Card", stateConfig.cardShowHp) { stateConfig = stateConfig.copy(cardShowHp = it) }
                                CheckboxSettingRow("Tampilkan Alamat di Card", stateConfig.cardShowAlamat) { stateConfig = stateConfig.copy(cardShowAlamat = it) }
                                CheckboxSettingRow("Tampilkan Foto Thumbnail", stateConfig.cardShowPhotos) { stateConfig = stateConfig.copy(cardShowPhotos = it) }
                                CheckboxSettingRow("Tampilkan Badge Status", stateConfig.cardShowStatus) { stateConfig = stateConfig.copy(cardShowStatus = it) }
                            }
                        }

                        // Operational CRUD locks
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Kontrol Hak Akses Operasi Data (CRUD):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DisperindagGreenPrimary)
                                SwitchSettingRow("Izinkan Tambah Pedagang (CREATE)", stateConfig.allowCreate) { stateConfig = stateConfig.copy(allowCreate = it) }
                                SwitchSettingRow("Izinkan Ubah Data (UPDATE)", stateConfig.allowUpdate) { stateConfig = stateConfig.copy(allowUpdate = it) }
                                SwitchSettingRow("Izinkan Hapus Data (DELETE)", stateConfig.allowDelete) { stateConfig = stateConfig.copy(allowDelete = it) }
                            }
                        }

                        // Backup & Restore

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Manajemen Data Form (Dropdown):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DisperindagGreenPrimary)
                                
                                Text("Tombol di bawah ini akan mengatur ulang opsi pilihan untuk JENIS RUANG, KOMODITI, dan STATUS ke setelan bawaan terbaru, dan menambahkan semua nilai unik dari data pedagang saat ini.", fontSize = 11.sp, color = Color.Gray)
                                
                                Button(
                                    onClick = { 
                                        onRebuildDropdownClick() 
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Perbarui Opsi Dropdown")
                                }
                            }
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Manajemen File Konfigurasi (JSON Backup):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DisperindagGreenPrimary)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            try {
                                                val json = AgencyConfigManager.exportConfigToJson(stateConfig)
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("Backup JSON", json))
                                                Toast.makeText(context, "✓ JSON Cadangan disalin ke clipboard!", Toast.LENGTH_LONG).show()
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Gagal export: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Backup JSON", fontSize = 11.sp)
                                    }

                                    Button(
                                        onClick = { jsonRestoreLauncher.launch("application/json") },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.SettingsBackupRestore, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Restore JSON", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Pengaturan Default?") },
            text = { Text("Apakah Anda yakin ingin menyetel ulang identitas instansi, link webhook, serta aturan validasi kembali ke setelan pabrik?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        AgencyConfigManager.resetToDefault(context)
                        stateConfig = AgencyConfig()
                        showResetDialog = false
                        Toast.makeText(context, "Sistem dikembalikan ke setelan default", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Ya, Setel Ulang", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

// Inline small helpers to prevent sizing bloating
fun sizeModifier(isSelected: Boolean): Modifier = Modifier.size(if (isSelected) 22.dp else 18.dp)

private fun pasteFromClipboard(context: Context, onPasted: (String) -> Unit) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = clipboard.primaryClip
    if (clip != null && clip.itemCount > 0) {
        val text = clip.getItemAt(0).text?.toString() ?: ""
        if (text.isNotBlank()) {
            onPasted(text)
        }
    }}

@Composable
fun LivePdfCardPreview(config: AgencyConfig) {
    val dummyPedagang = remember {
        Pedagang(
            id = 12,
            timestamp = "2026-08-07 12:00:00",
            namaPedagang = "H. ACHMAD HASAN",
            nik = "3528051212720002",
            alamat = "Jl. Raya Waru No. 15, Pamekasan",
            nomorHp = "081234567890",
            jenisRuangDagang = "Kios Utama",
            nomorKiosLos = "B-04",
            komoditi = "Konveksi & Pakaian",
            lamaBerjualan = 12,
            status = "Aktif",
            keterangan = "Iuran lunas s.d Desember 2026",
            fotoPedagangUri = "",
            fotoKtpUri = "",
            fotoSuratPernyataanUri = "",
            emailAddress = "operator.waru@pamekasan.go.id"
        )
    }

    val context = LocalContext.current
    val qrBitmap = remember(config) {
        val payload = "PEDAGANG_PASAR_WARU:${dummyPedagang.id}:${dummyPedagang.nik}"
        try {
            QrCodeUtils.generateQrCodeBitmap(payload, 150)
        } catch (e: Exception) {
            null
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Kop Surat
            if (config.pdfShowKopSurat) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (config.pdfShowLogo) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_pamekasan_logo),
                            contentDescription = "Logo",
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = config.pdfHeaderTitle.ifBlank { config.namaPemerintah },
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = config.pdfHeaderSubtitle.ifBlank { config.namaDinas },
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = config.pdfHeaderAddress.ifBlank { config.alamatDinas },
                            fontSize = 7.sp,
                            color = Color.DarkGray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                HorizontalDivider(thickness = 1.5.dp, color = Color.Black)
            }

            // Title Box Banner
            Surface(
                color = DisperindagGreenPrimary,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = config.pdfTitleText.ifBlank { "${config.namaPasar} - KARTU BUKTI PENDATAAN" },
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 3.dp, horizontal = 4.dp)
                )
            }

            // Card Body content
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1.3f),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text("${config.pdfLabelNama}: ${dummyPedagang.namaPedagang}", fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                    if (config.pdfShowNik) Text("${config.pdfLabelNik}: ${dummyPedagang.nik}", fontSize = 7.5.sp)
                    if (config.pdfShowAlamat) Text("Alamat: ${dummyPedagang.alamat}", fontSize = 7.5.sp)
                    Text("${config.pdfLabelRuang}: ${dummyPedagang.jenisRuangDagang} ${dummyPedagang.nomorKiosLos}", fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
                    if (config.pdfShowKomoditi) Text("Komoditi: ${dummyPedagang.komoditi}", fontSize = 7.5.sp)
                    if (config.pdfShowStatus) Text("Status: ${dummyPedagang.status}", fontSize = 7.5.sp, color = DisperindagGreenPrimary, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(6.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (config.pdfShowFotoPedagang) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFEEEEEE), RoundedCornerShape(4.dp))
                                .border(0.5.dp, Color.Gray, RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                        }
                    }

                    if (config.pdfShowQrCode && qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "QR Code Preview",
                            modifier = Modifier
                                .size(40.dp)
                                .border(0.5.dp, Color.DarkGray)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = config.pdfLabelTerdataResmi,
                fontSize = 6.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(2.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = config.pdfLabelDiterbitkan,
                    fontSize = 6.sp,
                    color = Color.Gray,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    modifier = Modifier.weight(1f)
                )
                if (config.pdfShowPetugas) {
                    Text(
                        text = "Petugas: Budi Santoso",
                        fontSize = 6.sp,
                        color = Color.Gray,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        modifier = Modifier.padding(start = 4.dp),
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }}

@Composable
fun CheckboxSettingRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = DisperindagGreenPrimary)
        )
    }
}

@Composable
fun SwitchSettingRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = DisperindagGreenPrimary)
        )
    }
}
