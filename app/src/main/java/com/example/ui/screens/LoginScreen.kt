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

                    Spacer(modifier = Modifier.height(24.dp))

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

            // Icon-only Webhook settings button
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
                text = "Pengaturan Webhook & Spreadsheet",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 8.dp)
            )

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
