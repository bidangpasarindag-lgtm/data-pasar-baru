package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.config.AgencyConfigManager
import com.example.data.remote.GoogleSheetSyncService
import com.example.ui.theme.DisperindagGreenPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (username: String, displayName: String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val config by AgencyConfigManager.config.collectAsState()

    var showSettings by remember { mutableStateOf(false) }
    var tempWebhookUrl by remember { mutableStateOf(config.webhookUrl) }
    var tempSpreadsheetId by remember { mutableStateOf(config.spreadsheetId) }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(config) {
        tempWebhookUrl = config.webhookUrl
        tempSpreadsheetId = config.spreadsheetId
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DisperindagGreenPrimary.copy(alpha = 0.15f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeContent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Logo & Header
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = com.example.R.drawable.ic_pamekasan_logo),
                    contentDescription = "Logo",
                    modifier = Modifier.size(75.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = config.appLoginTitle,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = DisperindagGreenPrimary,
                textAlign = TextAlign.Center,
                letterSpacing = 1.sp
            )

            Text(
                text = config.namaDinas,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B), // High contrast slate-800
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp)
            )

            Text(
                text = config.namaPemerintah,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF475569), // High contrast slate-600
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Login Box
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Masuk ke Sistem",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Gunakan akun pendata yang terdaftar di Spreadsheet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                    )

                    if (errorMessage != null) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = "Error",
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = errorMessage ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Username Field
                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it
                            errorMessage = null
                        },
                        label = { Text("Username") },
                        placeholder = { Text("Masukkan username") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = DisperindagGreenPrimary)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_username_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DisperindagGreenPrimary,
                            focusedLabelColor = DisperindagGreenPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorMessage = null
                        },
                        label = { Text("Password") },
                        placeholder = { Text("Masukkan password") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = DisperindagGreenPrimary)
                        },
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (isPasswordVisible) "Sembunyikan password" else "Tampilkan password"
                                )
                            }
                        },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_password_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DisperindagGreenPrimary,
                            focusedLabelColor = DisperindagGreenPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isLoading) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = DisperindagGreenPrimary
                                    )
                                    Text(
                                        text = "Memproses Login...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = DisperindagGreenPrimary,
                                    trackColor = MaterialTheme.colorScheme.primaryContainer
                                )
                                Text(
                                    text = "Sedang memverifikasi akun ke Google Spreadsheet...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Sign In Button
                    Button(
                        onClick = {
                            if (username.isBlank() || password.isBlank()) {
                                errorMessage = "Username dan Password wajib diisi."
                                return@Button
                            }
                            isLoading = true
                            errorMessage = null
                            coroutineScope.launch {
                                val syncService = GoogleSheetSyncService()
                                val result = syncService.loginToSheet(username, password)
                                isLoading = false
                                if (result.isSuccess) {
                                    val (userEmail, userDispName) = result.getOrThrow()
                                    onLoginSuccess(userEmail, userDispName)
                                } else {
                                    errorMessage = result.exceptionOrNull()?.message ?: "Gagal terhubung ke server."
                                }
                            }
                        },
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DisperindagGreenPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("login_submit_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Text(
                                "MASUK",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Icons for Settings and Guide
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Webhook settings button
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { showSettings = true },
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = DisperindagGreenPrimary.copy(alpha = 0.1f),
                                shape = CircleShape
                            )
                            .testTag("login_settings_icon_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Pengaturan Webhook",
                            tint = DisperindagGreenPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = "Pengaturan",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                var showGuide by remember { mutableStateOf(false) }
                // User Guide button
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { showGuide = true },
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = Color(0xFFE2E8F0),
                                shape = CircleShape
                            )
                            .testTag("login_guide_icon_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpCenter,
                            contentDescription = "Panduan Penggunaan",
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = "Panduan",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (showGuide) {
                    AlertDialog(
                        onDismissRequest = { showGuide = false },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.MenuBook, contentDescription = null, tint = DisperindagGreenPrimary)
                                Text("Panduan Penggunaan Aplikasi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                        },
                        text = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 450.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                GuideSection(
                                    title = "1. Download Template Spreadsheet (.XLSX)",
                                    content = "Gunakan tombol di bawah untuk mengunduh template spreadsheet penuh dalam format Excel (.xlsx). File ini sudah mencakup semua sheet yang diperlukan: 'Form Responses 1', 'username', dan 'aktivitas user'."
                                )
                                
                                Button(
                                    onClick = {
                                        val file = com.example.util.FileExportUtils.downloadTemplateSpreadsheetXlsx(context)
                                        if (file != null) {
                                            android.widget.Toast.makeText(context, "Template XLSX berhasil diunduh ke: ${file.absolutePath}", android.widget.Toast.LENGTH_LONG).show()
                                            com.example.util.FileExportUtils.openFile(context, file)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Download Template (.XLSX)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF475569), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "User Default: tim.pendata@ubed.com\nPassword: .",
                                        fontSize = 10.sp,
                                        color = Color(0xFF475569),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    TextButton(
                                        onClick = {
                                            username = "tim.pendata@ubed.com"
                                            password = "."
                                            showGuide = false
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("Gunakan Login", fontSize = 10.sp, color = DisperindagGreenPrimary, fontWeight = FontWeight.ExtraBold)
                                    }
                                }

                                GuideSection(
                                    title = "2. Salin Kode Script Backend",
                                    content = "Buka 'Extensions' > 'Apps Script' di Spreadsheet Anda, hapus semua kode lama, lalu tempel kode berikut ini. Pastikan Anda klik 'Deploy' > 'New Deployment' setelahnya."
                                )

                                var isCodeExpanded by remember { mutableStateOf(false) }
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.LightGray),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().clickable { isCodeExpanded = !isCodeExpanded },
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Google Apps Script Code (doPostAndroid.gs)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            Icon(if (isCodeExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
                                        }
                                        
                                        if (isCodeExpanded) {
                                            val fullCode = com.example.util.AppsScriptUtils.LATEST_CODE
                                            val changelogText = com.example.util.AppsScriptUtils.CHANGELOG

                                            val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                                            val annotatedString = androidx.compose.ui.text.buildAnnotatedString { append(fullCode) }
                                            
                                            // Changelog Section
                                            Surface(
                                                color = Color(0xFFEFF6FF),
                                                shape = RoundedCornerShape(8.dp),
                                                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFBFDBFE)),
                                                modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 6.dp)
                                            ) {
                                                Column(modifier = Modifier.padding(8.dp)) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Icon(Icons.Default.History, contentDescription = null, tint = DisperindagGreenPrimary, modifier = Modifier.size(14.dp))
                                                        Text("Changelog Apps Script (${com.example.util.AppsScriptUtils.VERSION})", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DisperindagGreenPrimary)
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = changelogText.trim(),
                                                        fontSize = 8.5.sp,
                                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                        color = Color.DarkGray
                                                    )
                                                }
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = { clipboardManager.setText(annotatedString) },
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = DisperindagGreenPrimary)
                                                ) {
                                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Salin Kode", fontSize = 10.sp)
                                                }

                                                Button(
                                                    onClick = {
                                                        val file = com.example.util.FileExportUtils.saveAppsScriptAsTxt(context, fullCode)
                                                        if (file != null) {
                                                            android.widget.Toast.makeText(context, "Script disimpan di: ${file.absolutePath}", android.widget.Toast.LENGTH_LONG).show()
                                                            com.example.util.FileExportUtils.openFile(context, file)
                                                        }
                                                    },
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                                ) {
                                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Download .TXT", fontSize = 10.sp)
                                                }
                                            }
                                            
                                            Text(
                                                text = fullCode,
                                                fontSize = 9.sp,
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                color = Color.DarkGray,
                                                modifier = Modifier.padding(top = 8.dp).heightIn(max = 200.dp).verticalScroll(rememberScrollState())
                                            )
                                        }
                                    }
                                }

                                GuideSection(
                                    title = "3. Konfigurasi Aplikasi",
                                    content = "Di halaman Login ini, klik icon 'Pengaturan'. Masukkan URL Web App dari langkah 2 ke kolom 'URL Webhook' dan ID Spreadsheet Anda (ada di URL browser spreadsheet) ke kolom 'Spreadsheet ID'. Klik 'Tes Koneksi' untuk memastikan data terhubung."
                                )
                                GuideSection(
                                    title = "4. Manajemen User & Login",
                                    content = "Tambahkan data pengguna di sheet 'username' dengan kolom Username (Email), Password, dan DisplayName. Gunakan akun tersebut untuk masuk ke aplikasi ini."
                                )
                                GuideSection(
                                    title = "5. Pendataan Pedagang",
                                    content = "Setelah masuk, Anda dapat menambah data pedagang baru. Data akan disimpan secara lokal terlebih dahulu jika tidak ada internet, dan dapat disinkronkan ke Spreadsheet nanti."
                                )
                                GuideSection(
                                    title = "6. Export PDF & Cetak Kartu",
                                    content = "Data yang sudah masuk dapat diekspor menjadi PDF pendataan atau dicetak sebagai kartu identitas pedagang langsung dari aplikasi."
                                )
                                GuideSection(
                                    title = "7. Pengaturan Dinamis",
                                    content = "Gunakan menu Pengaturan di dalam aplikasi untuk mengubah judul aplikasi, logo, label PDF, hingga mengatur opsi pilihan dropdown agar sesuai dengan kebutuhan pasar Anda."
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = { showGuide = false },
                                colors = ButtonDefaults.buttonColors(containerColor = DisperindagGreenPrimary)
                            ) {
                                Text("Mengerti")
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Configuration & Test Connection Dialog
        if (showSettings) {
            var isTestingConnection by remember { mutableStateOf(false) }
            var testResult by remember { mutableStateOf<String?>(null) }
            var testSuccess by remember { mutableStateOf<Boolean?>(null) }

            AlertDialog(
                onDismissRequest = { showSettings = false },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = DisperindagGreenPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Pengaturan Webhook & Spreadsheet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Konfigurasi Google Apps Script Webhook & Spreadsheet ID untuk integrasi data.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Webhook URL Input
                        OutlinedTextField(
                            value = tempWebhookUrl,
                            onValueChange = { 
                                tempWebhookUrl = it 
                                testResult = null
                            },
                            label = { Text("URL Webhook Google Apps Script", fontSize = 12.sp) },
                            placeholder = { Text("https://script.google.com/...") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_webhook_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DisperindagGreenPrimary,
                                focusedLabelColor = DisperindagGreenPrimary
                            ),
                            textStyle = MaterialTheme.typography.bodySmall
                        )

                        // Spreadsheet ID Input
                        OutlinedTextField(
                            value = tempSpreadsheetId,
                            onValueChange = { 
                                tempSpreadsheetId = it 
                                testResult = null
                            },
                            label = { Text("Spreadsheet ID", fontSize = 12.sp) },
                            placeholder = { Text("ID unik dari spreadsheet...") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_spreadsheet_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DisperindagGreenPrimary,
                                focusedLabelColor = DisperindagGreenPrimary
                            ),
                            textStyle = MaterialTheme.typography.bodySmall
                        )

                        // Test Connection Button
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isTestingConnection) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = DisperindagGreenPrimary,
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Text(
                                        text = "Menguji koneksi...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = DisperindagGreenPrimary
                                    )
                                }
                            } else {
                                Button(
                                    onClick = {
                                        isTestingConnection = true
                                        testResult = null
                                        coroutineScope.launch {
                                            val syncService = GoogleSheetSyncService()
                                            val result = syncService.testWebhookConnection(tempWebhookUrl.trim(), tempSpreadsheetId.trim())
                                            isTestingConnection = false
                                            if (result.isSuccess) {
                                                testSuccess = true
                                                testResult = result.getOrNull() ?: "Koneksi Berhasil!"
                                            } else {
                                                testSuccess = false
                                                testResult = result.exceptionOrNull()?.message ?: "Koneksi Gagal"
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("login_test_connection_button")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CloudQueue,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text("Tes Koneksi Webhook", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Test Result Message
                        testResult?.let { msg ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (testSuccess == true) Color(0xFFE6F4EA) else MaterialTheme.colorScheme.errorContainer
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (testSuccess == true) Icons.Default.CheckCircle else Icons.Default.Error,
                                        contentDescription = if (testSuccess == true) "Sukses" else "Error",
                                        tint = if (testSuccess == true) Color(0xFF137333) else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = msg,
                                        fontSize = 11.sp,
                                        color = if (testSuccess == true) Color(0xFF137333) else MaterialTheme.colorScheme.onErrorContainer,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Reset Default Button
                        TextButton(
                            onClick = {
                                val defaultWebhook = "https://script.google.com/macros/s/AKfycbzyIKCqNpmbhAxgbTWDPXwzZ1CyTgl8C_28CBtJkaoTQwXGHa7v2rdDFiLzkBrW7kjQ/exec"
                                val defaultSpreadsheet = "1Q7OtJ1fuEwkycAtnAjRNrSNrAEJ5SxjGRn-ge9YcWlU"
                                tempWebhookUrl = defaultWebhook
                                tempSpreadsheetId = defaultSpreadsheet
                                testResult = null
                            }
                        ) {
                            Text("Reset Default", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        }

                        // Save Button
                        Button(
                            onClick = {
                                if (tempWebhookUrl.isNotBlank() && tempSpreadsheetId.isNotBlank()) {
                                    val newConfig = config.copy(
                                        webhookUrl = tempWebhookUrl.trim(),
                                        spreadsheetId = tempSpreadsheetId.trim()
                                    )
                                    AgencyConfigManager.updateConfig(context, newConfig)
                                    showSettings = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DisperindagGreenPrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Simpan", color = Color.White, fontSize = 12.sp)
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showSettings = false }
                    ) {
                        Text("Batal", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                }
            )
        }
    }
}

@Composable
fun GuideSection(title: String, content: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = DisperindagGreenPrimary
        )
        Text(
            text = content,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Justify,
            lineHeight = 16.sp
        )
    }
}
