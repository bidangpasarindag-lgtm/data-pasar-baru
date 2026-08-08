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
import androidx.compose.ui.text.TextStyle
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
import com.example.util.AppStorageUtils
import com.example.util.DriveImageUtils
import com.example.util.FileExportUtils
import com.example.util.QrCodeUtils
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    lastSyncTime: String,
    isSyncing: Boolean,
    totalPedagangCount: Int,
    allJenisRuangOptions: List<com.example.data.model.DropdownOption> = emptyList(),
    allKomoditiOptions: List<com.example.data.model.DropdownOption> = emptyList(),
    allStatusOptions: List<com.example.data.model.DropdownOption> = emptyList(),
    onSyncClick: () -> Unit,
    onExportCsvClick: () -> Unit,
    onRebuildDropdownClick: () -> Unit,
    onToggleDropdownVisibility: (com.example.data.model.DropdownOption) -> Unit = {},
    onDeleteDropdownOption: (Long) -> Unit = {},
    onAddDropdownOption: (String, String) -> Unit = { _, _ -> },
    onSettingsSaved: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentConfig by AgencyConfigManager.config.collectAsState()
    var stateConfig by androidx.compose.runtime.remember(currentConfig) { androidx.compose.runtime.mutableStateOf(currentConfig) }
    var isSavingSettings by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var activeTab by remember { mutableStateOf("cloud") } // "cloud", "pdf", "form", "storage", "sistem"

    var showResetDialog by remember { mutableStateOf(false) }
    var isTestingConnection by remember { mutableStateOf(false) }
    var connectionResult by remember { mutableStateOf<String?>(null) }
    var isCheckingVersion by remember { mutableStateOf(false) }
    var versionCheckResult by remember { mutableStateOf<String?>(null) }
    var showGuide by remember { mutableStateOf(false) }

    var lastBackupFile by remember { mutableStateOf<File?>(null) }
    var showBackupSuccessDialog by remember { mutableStateOf(false) }

    // Helper for Styled Headers
    @Composable
    fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector? = null, color: Color = DisperindagGreenPrimary) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(color.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                .padding(vertical = 8.dp, horizontal = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                }
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = color,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }

    @Composable
    fun SubSectionHeader(title: String, color: Color = DisperindagGreenPrimary) {
        Surface(
            color = color.copy(alpha = 0.05f),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 2.dp)
        ) {
            Text(
                text = title,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Black,
                color = color,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }

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
                if (jsonString.isNotBlank()) {
                    val result = AgencyConfigManager.importConfigFromJson(context, jsonString)
                    if (result.isSuccess) {
                        val imported = result.getOrNull()
                        if (imported != null) {
                            stateConfig = imported
                        }
                        Toast.makeText(context, "✓ Konfigurasi aplikasi berhasil dipulihkan!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "❌ Format JSON konfigurasi tidak valid", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context, "❌ File JSON kosong", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error restore: ${e.message}", Toast.LENGTH_SHORT).show()
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
                    "form" -> 2
                    "storage" -> 3
                    else -> 4
                },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = DisperindagGreenPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[when (activeTab) {
                            "cloud" -> 0
                            "pdf" -> 1
                            "form" -> 2
                            "storage" -> 3
                            else -> 4
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
                            Text("Cloud", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = activeTab == "pdf",
                    onClick = { activeTab = "pdf" },
                    text = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, sizeModifier(activeTab == "pdf"))
                            Text("PDF", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = activeTab == "form",
                    onClick = { activeTab = "form" },
                    text = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Assignment, contentDescription = null, sizeModifier(activeTab == "form"))
                            Text("Form", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = activeTab == "storage",
                    onClick = { activeTab = "storage" },
                    text = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.FolderSpecial, contentDescription = null, sizeModifier(activeTab == "storage"))
                            Text("Storage", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = activeTab == "sistem",
                    onClick = { activeTab = "sistem" },
                    text = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Tune, contentDescription = null, sizeModifier(activeTab == "sistem"))
                            Text("Sistem", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                    .verticalScroll(rememberScrollState())
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (activeTab) {
                    "cloud" -> {
                        // TAB 1: CLOUD & GOOGLE SHEET SYNCHRONIZATION
                        SectionHeader("KONEKTIVITAS CLOUD & GOOGLE SPREADSHEET", Icons.Default.CloudSync, color = Color(0xFF2E7D32))

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                SubSectionHeader("Integrasi Google Spreadsheet", color = Color(0xFF2E7D32))
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

                                // Interactive Webhook & AppsScript Version Tester
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        if (isTestingConnection) {
                                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                        } else {
                                            Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Uji Webhook", fontSize = 11.sp)
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            scope.launch {
                                                isCheckingVersion = true
                                                versionCheckResult = "Memeriksa versi Apps Script..."
                                                val service = GoogleSheetSyncService()
                                                val res = service.checkAppsScriptVersion()
                                                if (res.isSuccess) {
                                                    versionCheckResult = res.getOrNull()?.third
                                                } else {
                                                    versionCheckResult = "❌ Gagal mengecek versi: ${res.exceptionOrNull()?.message}"
                                                }
                                                isCheckingVersion = false
                                            }
                                        },
                                        enabled = !isCheckingVersion,
                                        modifier = Modifier.weight(1.2f),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = DisperindagAccentGold, contentColor = Color.Black),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        if (isCheckingVersion) {
                                            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.Black, strokeWidth = 2.dp)
                                        } else {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Uji Versi Script", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Button(
                                        onClick = onSyncClick,
                                        enabled = !isSyncing,
                                        modifier = Modifier.weight(1.1f),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = DisperindagGreenPrimary),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        if (isSyncing) {
                                            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                                        } else {
                                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Tarik Data", fontSize = 11.sp)
                                        }
                                    }
                                }

                                connectionResult?.let {
                                    Text(
                                        text = it,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (it.startsWith("✓")) Color(0xFF2E7D32) else Color.Red,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                versionCheckResult?.let { text ->
                                    val isSuccess = text.startsWith("✓")
                                    Surface(
                                        color = if (isSuccess) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                                        shape = RoundedCornerShape(8.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSuccess) Color(0xFF4CAF50) else Color(0xFFFF9800)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = text,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isSuccess) Color(0xFF1B5E20) else Color(0xFFE65100),
                                            lineHeight = 15.sp,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
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
                                    val fullAppsScriptCode = com.example.util.AppsScriptUtils.LATEST_CODE
                                    val changelogText = com.example.util.AppsScriptUtils.CHANGELOG

                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        // Changelog Section
                                        Surface(
                                            color = Color(0xFFEFF6FF),
                                            shape = RoundedCornerShape(8.dp),
                                            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFBFDBFE)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(Icons.Default.History, contentDescription = null, tint = DisperindagGreenPrimary, modifier = Modifier.size(16.dp))
                                                    Text("Changelog Apps Script (${com.example.util.AppsScriptUtils.VERSION})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DisperindagGreenPrimary)
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = changelogText.trim(),
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = Color.DarkGray
                                                )
                                            }
                                        }

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

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    clipboard.setPrimaryClip(ClipData.newPlainText("Apps Script", fullAppsScriptCode))
                                                    Toast.makeText(context, "✓ Berhasil menyalin kode Apps Script!", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = DisperindagGreenPrimary),
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Salin Kode", fontSize = 11.sp)
                                            }

                                            Button(
                                                onClick = {
                                                    val file = com.example.util.FileExportUtils.saveAppsScriptAsTxt(context, fullAppsScriptCode)
                                                    if (file != null) {
                                                        Toast.makeText(context, "Script disimpan di: ${file.absolutePath}", Toast.LENGTH_LONG).show()
                                                        com.example.util.FileExportUtils.openFile(context, file)
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Download .TXT", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "pdf" -> {
                        // TAB 2: BRANDING & PDF LAYOUT DESIGN
                        SectionHeader("DESAIN KARTU PDF & BRANDING INSTANSI", Icons.Default.PictureAsPdf, color = Color(0xFF1976D2))

                        // LIVE CARD PREVIEW
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                SubSectionHeader("Real-Time Preview Kartu PDF", color = Color(0xFF1976D2))
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
                                SubSectionHeader("Identitas Instansi & Header Aplikasi", color = Color(0xFF1976D2))
                                
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
                                SubSectionHeader("Penyimpanan File PDF & Dokumen", color = Color(0xFF1976D2))

                                Surface(
                                    color = Color(0xFFE8F5E9),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = DisperindagGreenPrimary, modifier = Modifier.size(20.dp))
                                        Column {
                                            Text("Lokasi Penyimpanan Default Aplikasi:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DisperindagGreenPrimary)
                                            Text("Internal Storage / SI-PENDATAPASAR / (PDF / EXCEL / APPS_SCRIPT / FOTO)", fontSize = 10.sp, color = Color.DarkGray)
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = stateConfig.pdfStorageSubfolder,
                                    onValueChange = { stateConfig = stateConfig.copy(pdfStorageSubfolder = it) },
                                    label = { Text("Custom Subfolder PDF (Opsional)") },
                                    placeholder = { Text("Contoh: KARTU_PEDAGANG") },
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
                                SubSectionHeader("Teks Dinamis Kop Surat & Kartu PDF", color = Color(0xFF1976D2))
 
                                OutlinedTextField(
                                    value = stateConfig.pdfTitleText,
                                    onValueChange = { stateConfig = stateConfig.copy(pdfTitleText = it) },
                                    label = { Text("Judul Kartu PDF (Kustom)") },
                                    placeholder = { Text("Contoh: PASAR WARU - KARTU BUKTI\nPENDATAAN PEDAGANG") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 2
                                )

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
                                SubSectionHeader("Kustomisasi Label / Teks PDF (Dinamis)", color = Color(0xFF1976D2))

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

                                OutlinedTextField(
                                    value = stateConfig.pdfLabelAlamat,
                                    onValueChange = { stateConfig = stateConfig.copy(pdfLabelAlamat = it) },
                                    label = { Text("Label Alamat") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = stateConfig.pdfLabelHp,
                                    onValueChange = { stateConfig = stateConfig.copy(pdfLabelHp = it) },
                                    label = { Text("Label Nomor HP/WA") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = stateConfig.pdfLabelKios,
                                    onValueChange = { stateConfig = stateConfig.copy(pdfLabelKios = it) },
                                    label = { Text("Label Nomor Kios/Los") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = stateConfig.pdfLabelKomoditi,
                                    onValueChange = { stateConfig = stateConfig.copy(pdfLabelKomoditi = it) },
                                    label = { Text("Label Komoditi") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = stateConfig.pdfLabelStatus,
                                    onValueChange = { stateConfig = stateConfig.copy(pdfLabelStatus = it) },
                                    label = { Text("Label Status Pedagang") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = stateConfig.pdfLabelWaktu,
                                    onValueChange = { stateConfig = stateConfig.copy(pdfLabelWaktu = it) },
                                    label = { Text("Label Waktu Pendataan") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = stateConfig.pdfLabelKeteranganHeader,
                                    onValueChange = { stateConfig = stateConfig.copy(pdfLabelKeteranganHeader = it) },
                                    label = { Text("Label Keterangan") },
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
                                SubSectionHeader("Visibilitas Elemen Kartu PDF", color = Color(0xFF1976D2))
                                Spacer(modifier = Modifier.height(4.dp))
                                CheckboxSettingRow("Tampilkan Kop Surat Resmi", stateConfig.pdfShowKopSurat) { stateConfig = stateConfig.copy(pdfShowKopSurat = it) }
                                CheckboxSettingRow("Tampilkan Logo Instansi", stateConfig.pdfShowLogo) { stateConfig = stateConfig.copy(pdfShowLogo = it) }
                                CheckboxSettingRow("Tampilkan Kode QR Identifikasi", stateConfig.pdfShowQrCode) { stateConfig = stateConfig.copy(pdfShowQrCode = it) }
                                CheckboxSettingRow("Tampilkan Foto Pedagang", stateConfig.pdfShowFotoPedagang) { stateConfig = stateConfig.copy(pdfShowFotoPedagang = it) }
                                CheckboxSettingRow("Tampilkan Nama Petugas Pendata (Footer)", stateConfig.pdfShowPetugas) { stateConfig = stateConfig.copy(pdfShowPetugas = it) }
                            }
                        }
                    }

                    "form" -> {
                        // TAB 3: FORM DATA MANAGEMENT
                        SectionHeader("MANAJEMEN DATA FORM TAMBAH/EDIT", Icons.Default.Assignment, color = Color(0xFFE65100))

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                SubSectionHeader("Visibilitas Field Form", color = Color(0xFFE65100))
                                
                                CheckboxSettingRow("Tampilkan NIK", stateConfig.formShowNik) { stateConfig = stateConfig.copy(formShowNik = it) }
                                CheckboxSettingRow("Tampilkan Alamat", stateConfig.formShowAlamat) { stateConfig = stateConfig.copy(formShowAlamat = it) }
                                CheckboxSettingRow("Tampilkan Nomor HP", stateConfig.formShowHp) { stateConfig = stateConfig.copy(formShowHp = it) }
                                CheckboxSettingRow("Tampilkan Jenis Ruang", stateConfig.formShowJenisRuang) { stateConfig = stateConfig.copy(formShowJenisRuang = it) }
                                CheckboxSettingRow("Tampilkan Nomor Kios/Los", stateConfig.formShowNomorKios) { stateConfig = stateConfig.copy(formShowNomorKios = it) }
                                CheckboxSettingRow("Tampilkan Komoditi", stateConfig.formShowKomoditi) { stateConfig = stateConfig.copy(formShowKomoditi = it) }
                                CheckboxSettingRow("Tampilkan Lama Berjualan", stateConfig.formShowLamaBerjualan) { stateConfig = stateConfig.copy(formShowLamaBerjualan = it) }
                                CheckboxSettingRow("Tampilkan Status", stateConfig.formShowStatus) { stateConfig = stateConfig.copy(formShowStatus = it) }
                                CheckboxSettingRow("Tampilkan Keterangan", stateConfig.formShowKeterangan) { stateConfig = stateConfig.copy(formShowKeterangan = it) }
                                
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                SubSectionHeader("Syarat Kelengkapan Berkas (Mandatori)", color = Color(0xFFE65100))
                                
                                CheckboxSettingRow("Wajib Foto Pedagang", stateConfig.formShowFotoPedagang) { stateConfig = stateConfig.copy(formShowFotoPedagang = it) }
                                CheckboxSettingRow("Wajib Foto KTP", stateConfig.formShowFotoKtp) { stateConfig = stateConfig.copy(formShowFotoKtp = it) }
                                CheckboxSettingRow("Wajib Foto Surat Pernyataan", stateConfig.formShowFotoSurat) { stateConfig = stateConfig.copy(formShowFotoSurat = it) }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        SectionHeader("MANAJEMEN OPSI DROPDOWN DINAMIS", Icons.Default.List, color = Color(0xFFE65100))
                        
                        // Jenis Ruang Management
                        DropdownManagementSection(
                            title = "JENIS RUANG DAGANG",
                            options = allJenisRuangOptions,
                            onToggle = onToggleDropdownVisibility,
                            onDelete = onDeleteDropdownOption,
                            onAdd = { onAddDropdownOption("JENIS_RUANG", it) }
                        )

                        // Komoditi Management
                        DropdownManagementSection(
                            title = "KOMODITI / JENIS USAHA",
                            options = allKomoditiOptions,
                            onToggle = onToggleDropdownVisibility,
                            onDelete = onDeleteDropdownOption,
                            onAdd = { onAddDropdownOption("KOMODITI", it) }
                        )

                        // Status Management
                        DropdownManagementSection(
                            title = "STATUS PEDAGANG",
                            options = allStatusOptions,
                            onToggle = onToggleDropdownVisibility,
                            onDelete = onDeleteDropdownOption,
                            onAdd = { onAddDropdownOption("STATUS", it) }
                        )

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                SubSectionHeader("Opsi Dropdown Otomatis", color = Color(0xFFE65100))
                                Text("Klik tombol di bawah untuk menyegarkan opsi dropdown berdasarkan data unik yang ada di database saat ini.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Button(
                                    onClick = onRebuildDropdownClick,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Perbarui Semua Opsi Dropdown", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    "storage" -> {
                        // TAB 4: SUB-PENGATURAN STORAGE & SUB-FOLDER PENYIMPANAN DATA DOWNLOAD
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                SectionHeader("Penyimpanan & Folder Download Aplikasi", Icons.Default.FolderSpecial, Color(0xFF1E88E5))

                                Text(
                                    "Sub-pengaturan kustomisasi lokasi folder penyimpanan default untuk seluruh data hasil download dari aplikasi. Lokasi default berada di Internal Storage / SI-PENDATAPASAR / dengan sub-folder otomatis.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp
                                )

                                // Status Izin Akses Storage
                                val hasPerm = AppStorageUtils.hasStoragePermission(context)
                                Surface(
                                    color = if (hasPerm) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (hasPerm) Color(0xFF4CAF50) else Color(0xFFFF9800)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            if (hasPerm) Icons.Default.CheckCircle else Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = if (hasPerm) Color(0xFF2E7D32) else Color(0xFFE65100),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (hasPerm) "Izin Akses Storage: DIBERIKAN ✓" else "Izin Akses Storage: PERLU DIKONFIRMASI ⚠️",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (hasPerm) Color(0xFF2E7D32) else Color(0xFFE65100)
                                            )
                                            Text(
                                                text = if (hasPerm) "Aplikasi siap membaca & menyimpan file ke folder Internal Storage." else "Klik tombol di kanan untuk mengaktifkan izin akses storage.",
                                                fontSize = 11.sp,
                                                color = Color.DarkGray
                                            )
                                        }
                                        Button(
                                            onClick = { AppStorageUtils.openStoragePermissionSettings(context) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (hasPerm) Color(0xFF2E7D32) else Color(0xFFE65100)
                                            ),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(if (hasPerm) "Cek Izin" else "Minta Izin", fontSize = 11.sp)
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            stateConfig = stateConfig.copy(
                                                storageMainFolder = "SI-PENDATAPASAR",
                                                storagePdfFolder = "PDF",
                                                storageExcelFolder = "EXCEL",
                                                storageAppsScriptFolder = "APPS_SCRIPT",
                                                storageFotoFolder = "FOTO",
                                                storageBackupConfigFolder = "BACKUP-KONFIGURASI"
                                            )
                                            Toast.makeText(context, "Nama folder direset ke default!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Reset Path Default", fontSize = 11.sp)
                                    }

                                    Button(
                                        onClick = {
                                            val dirs = AppStorageUtils.ensureDirectoriesExist(context)
                                            Toast.makeText(context, "✓ Berhasil membuat/verifikasi ${dirs.size} folder di Internal Storage!", Toast.LENGTH_LONG).show()
                                        },
                                        modifier = Modifier.weight(1.2f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Buat / Verifikasi Folder", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                SubSectionHeader("Kustomisasi Sub-Folder Penyimpanan Data", Color(0xFF1E88E5))

                                OutlinedTextField(
                                    value = stateConfig.storageMainFolder,
                                    onValueChange = { stateConfig = stateConfig.copy(storageMainFolder = it) },
                                    label = { Text("Folder Utama (Root) Internal Storage") },
                                    placeholder = { Text("SI-PENDATAPASAR") },
                                    leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFF1E88E5)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = stateConfig.storagePdfFolder,
                                    onValueChange = { stateConfig = stateConfig.copy(storagePdfFolder = it) },
                                    label = { Text("Subfolder PDF Kartu & Laporan") },
                                    placeholder = { Text("PDF") },
                                    leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFD32F2F)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = stateConfig.storageExcelFolder,
                                    onValueChange = { stateConfig = stateConfig.copy(storageExcelFolder = it) },
                                    label = { Text("Subfolder Excel (.xlsx) & CSV") },
                                    placeholder = { Text("EXCEL") },
                                    leadingIcon = { Icon(Icons.Default.TableChart, contentDescription = null, tint = Color(0xFF388E3C)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = stateConfig.storageAppsScriptFolder,
                                    onValueChange = { stateConfig = stateConfig.copy(storageAppsScriptFolder = it) },
                                    label = { Text("Subfolder File Apps Script (.txt)") },
                                    placeholder = { Text("APPS_SCRIPT") },
                                    leadingIcon = { Icon(Icons.Default.Code, contentDescription = null, tint = Color(0xFFF57C00)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = stateConfig.storageFotoFolder,
                                    onValueChange = { stateConfig = stateConfig.copy(storageFotoFolder = it) },
                                    label = { Text("Subfolder Foto Pedagang & Lampiran") },
                                    placeholder = { Text("FOTO") },
                                    leadingIcon = { Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Color(0xFF7B1FA2)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = stateConfig.storageBackupConfigFolder,
                                    onValueChange = { stateConfig = stateConfig.copy(storageBackupConfigFolder = it) },
                                    label = { Text("Subfolder Backup Konfigurasi System JSON") },
                                    placeholder = { Text("BACKUP-KONFIGURASI") },
                                    leadingIcon = { Icon(Icons.Default.Backup, contentDescription = null, tint = Color(0xFF0097A7)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                SubSectionHeader("Struktur & Path Absolut Folder di HP", Color(0xFF1E88E5))

                                val mainName = stateConfig.storageMainFolder.ifBlank { "SI-PENDATAPASAR" }
                                val pdfSub = stateConfig.storagePdfFolder.ifBlank { "PDF" }
                                val excelSub = stateConfig.storageExcelFolder.ifBlank { "EXCEL" }
                                val scriptSub = stateConfig.storageAppsScriptFolder.ifBlank { "APPS_SCRIPT" }
                                val fotoSub = stateConfig.storageFotoFolder.ifBlank { "FOTO" }
                                val backupSub = stateConfig.storageBackupConfigFolder.ifBlank { "BACKUP-KONFIGURASI" }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFE1F5FE), RoundedCornerShape(8.dp))
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("📁 Internal Storage / $mainName /", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0277BD))
                                    Text(" ├─ 📁 $pdfSub/ (Dokumen Kartu & Laporan PDF)", fontSize = 11.sp, color = Color.DarkGray)
                                    Text(" ├─ 📁 $excelSub/ (Export Spreadsheet XLSX & CSV)", fontSize = 11.sp, color = Color.DarkGray)
                                    Text(" ├─ 📁 $scriptSub/ (Script Kode Google Apps Script)", fontSize = 11.sp, color = Color.DarkGray)
                                    Text(" ├─ 📁 $fotoSub/ (Foto Hasil Tangkapan Kamera / Download)", fontSize = 11.sp, color = Color.DarkGray)
                                    Text(" └─ 📁 $backupSub/ (File JSON Cadangan Konfigurasi)", fontSize = 11.sp, color = Color.DarkGray)
                                }
                            }
                        }
                    }

                    else -> {
                        // TAB 5: SYSTEM RULES & CRUD ACCESS CONTROL
                        SectionHeader("ATURAN VALIDASI DATA & HAK AKSES", Icons.Default.Tune, color = Color(0xFF6A1B9A))

                        // Completeness rules
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                SubSectionHeader("Aturan Kelengkapan Data Pedagang", color = Color(0xFF6A1B9A))

                                SwitchSettingRow(
                                    label = "Tampilkan Warning Data Belum Lengkap",
                                    checked = stateConfig.enableCompletenessWarning,
                                    onCheckedChange = { stateConfig = stateConfig.copy(enableCompletenessWarning = it) }
                                )

                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                SubSectionHeader("Syarat Wajib Isi Untuk Berkas Lengkap", color = Color(0xFF6A1B9A))

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
                                SubSectionHeader("Atribut Daftar Pedagang", color = Color(0xFF6A1B9A))
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
                                SubSectionHeader("Kontrol Hak Akses Operasi Data", color = Color(0xFF6A1B9A))
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
                                SubSectionHeader("Backup & Restore Konfigurasi System", color = Color(0xFF6A1B9A))

                                Text(
                                    text = "Seluruh konfigurasi instansi, spreadsheet, webhook, syarat kelengkapan form, serta visibilitas PDF akan dicadangkan secara menyeluruh.",
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    lineHeight = 15.sp
                                )

                                Surface(
                                    color = Color(0xFF6A1B9A).copy(alpha = 0.06f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFF6A1B9A), modifier = Modifier.size(20.dp))
                                        Column {
                                            Text("Folder Backup Target:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6A1B9A))
                                            Text("${stateConfig.storageMainFolder}/${stateConfig.storageBackupConfigFolder}/", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                                        }
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            try {
                                                val jsonStr = AgencyConfigManager.exportConfigToJson(stateConfig)
                                                val backupDir = AppStorageUtils.getBackupConfigDirectory(
                                                    context = context,
                                                    customMainFolder = stateConfig.storageMainFolder,
                                                    customSubFolder = stateConfig.storageBackupConfigFolder
                                                )
                                                if (!backupDir.exists()) {
                                                    backupDir.mkdirs()
                                                }

                                                val dateStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
                                                val timeStr = SimpleDateFormat("HHmm", Locale.getDefault()).format(Date())
                                                val randomCode = String.format("%04d", (0..9999).random())
                                                val fileName = "${dateStr}-${timeStr}-BACKUPKONFIGURASI-${randomCode}.json"

                                                val backupFile = File(backupDir, fileName)
                                                FileOutputStream(backupFile).use { fos ->
                                                    fos.write(jsonStr.toByteArray(Charsets.UTF_8))
                                                }

                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("Backup JSON", jsonStr))

                                                lastBackupFile = backupFile
                                                showBackupSuccessDialog = true
                                                AppStorageUtils.scanFile(context, backupFile)
                                                Toast.makeText(context, "✓ File backup disimpan di:\n${backupFile.absolutePath}", Toast.LENGTH_LONG).show()
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Gagal backup: ${e.message}", Toast.LENGTH_LONG).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A1B9A)),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Backup Konfigurasi (Ke Storage)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { jsonRestoreLauncher.launch("*/*") },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Restore File JSON", fontSize = 11.sp)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                pasteFromClipboard(context) { pastedText ->
                                                    try {
                                                        val res = AgencyConfigManager.importConfigFromJson(context, pastedText)
                                                        if (res.isSuccess) {
                                                            stateConfig = res.getOrThrow()
                                                            Toast.makeText(context, "✓ Konfigurasi dari Clipboard berhasil dipulihkan!", Toast.LENGTH_LONG).show()
                                                        } else {
                                                            Toast.makeText(context, "❌ Format JSON dari clipboard tidak valid", Toast.LENGTH_SHORT).show()
                                                        }
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "Error restore: ${e.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF6A1B9A)),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Restore Clipboard", fontSize = 11.sp)
                                        }
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

    if (showBackupSuccessDialog && lastBackupFile != null) {
        val backupFile = lastBackupFile!!
        AlertDialog(
            onDismissRequest = { showBackupSuccessDialog = false },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(36.dp)) },
            title = { Text("Backup Konfigurasi Berhasil", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("File backup konfigurasi berhasil disimpan ke internal storage HP:", fontSize = 12.sp)
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Nama File:", fontSize = 10.sp, color = Color.Gray)
                            Text(backupFile.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6A1B9A))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Folder Penyimpanan:", fontSize = 10.sp, color = Color.Gray)
                            Text("${stateConfig.storageMainFolder}/${stateConfig.storageBackupConfigFolder}/", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Path Lengkap:", fontSize = 10.sp, color = Color.Gray)
                            Text(backupFile.absolutePath, fontSize = 10.sp, color = Color.DarkGray)
                        }
                    }
                    Text("✓ Data JSON konfigurasi juga disalin ke Clipboard.", fontSize = 11.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.SemiBold)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        FileExportUtils.openFile(context, backupFile)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DisperindagGreenPrimary)
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Buka File Backup", fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupSuccessDialog = false }) {
                    Text("Tutup", fontSize = 12.sp)
                }
            }
        )
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
                Column(
                    modifier = Modifier.padding(vertical = 3.dp, horizontal = 4.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val previewTitle = config.pdfTitleText.ifBlank { "${config.namaPasar} - KARTU BUKTI PENDATAAN" }
                    previewTitle.split("\n").forEach { line ->
                        Text(
                            text = line,
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
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
fun DropdownManagementSection(
    title: String,
    options: List<com.example.data.model.DropdownOption>,
    onToggle: (com.example.data.model.DropdownOption) -> Unit,
    onDelete: (Long) -> Unit,
    onAdd: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var newValue by remember { mutableStateOf("") }
    
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.List, contentDescription = null, tint = DisperindagGreenPrimary, modifier = Modifier.size(18.dp))
                    Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Surface(
                        color = DisperindagGreenPrimary.copy(alpha = 0.1f),
                        shape = CircleShape
                    ) {
                        Text(
                            text = options.size.toString(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = DisperindagGreenPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Icon(if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
            }
            
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Centang data di bawah untuk memilih opsi yang ingin dimunculkan di form pendataan:",
                        fontSize = 11.sp,
                        color = DisperindagGreenPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    if (options.isEmpty()) {
                        Text("Belum ada data opsi.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    
                    options.forEach { option ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Checkbox(
                                    checked = option.isVisible,
                                    onCheckedChange = { onToggle(option) },
                                    colors = CheckboxDefaults.colors(checkedColor = DisperindagGreenPrimary)
                                )
                                Text(
                                    text = option.optionValue,
                                    fontSize = 12.sp,
                                    color = if (option.isVisible) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    fontWeight = if (option.isVisible) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                            IconButton(onClick = { onDelete(option.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Add New Option Input
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newValue,
                            onValueChange = { newValue = it },
                            placeholder = { Text("Tambah opsi baru...", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 12.sp)
                        )
                        IconButton(
                            onClick = {
                                if (newValue.isNotBlank()) {
                                    onAdd(newValue.trim())
                                    newValue = ""
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = DisperindagGreenPrimary.copy(alpha = 0.1f))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Tambah", tint = DisperindagGreenPrimary)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

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
            .padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 11.5.sp, modifier = Modifier.weight(1f))
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
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = DisperindagGreenPrimary)
        )
    }
}
