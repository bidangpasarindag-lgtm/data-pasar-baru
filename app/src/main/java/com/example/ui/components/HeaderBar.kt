package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.ui.graphics.Brush
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.auth.GoogleUser
import com.example.data.auth.UserManager
import com.example.ui.theme.DisperindagAccentGold
import com.example.ui.theme.DisperindagGreenPrimary

import androidx.compose.material.icons.filled.Settings
import coil.compose.AsyncImage
import com.example.data.config.AgencyConfigManager
import com.example.util.DriveImageUtils

import androidx.compose.material.icons.filled.QrCodeScanner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeaderBar(
    onSyncClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onScanQrClick: () -> Unit = {},
    isSyncing: Boolean = false
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val currentUser by UserManager.currentUser.collectAsState()
    val isLoggedIn by UserManager.isLoggedIn.collectAsState()
    val agencyConfig by AgencyConfigManager.config.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var isLoggingOut by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    var isHeaderExpanded by remember { mutableStateOf(false) }
    var isTestingConnection by remember { mutableStateOf(false) }
    var connectionTestResult by remember { mutableStateOf<String?>(null) }

    Surface(
        color = Color.Transparent,
        contentColor = Color.White,
        shadowElevation = 6.dp,
        modifier = Modifier
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        DisperindagGreenPrimary,
                        Color(0xFF1B5E20), // Darker green
                        Color(0xFF0D5330)  // Premium emerald green
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Clickable Logo + App Title section to toggle the quick-insights drawer
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isHeaderExpanded = !isHeaderExpanded }
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        if (agencyConfig.customLogoUri.isNotBlank()) {
                            AsyncImage(
                                model = DriveImageUtils.convertToDirectUrl(agencyConfig.customLogoUri) ?: agencyConfig.customLogoUri,
                                contentDescription = "Logo Instansi",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(32.dp)
                                    .padding(2.dp)
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.ic_pamekasan_logo),
                                contentDescription = "Logo Kabupaten Pamekasan",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = agencyConfig.appTitleHeader.ifBlank { "DISPERINDAG PAMEKASAN" },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = DisperindagAccentGold,
                                letterSpacing = 0.5.sp,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown, // Pointing Indicator
                                contentDescription = null,
                                tint = DisperindagAccentGold,
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(start = 2.dp)
                            )
                        }
                        Text(
                            text = "Pendataan Pasar Waru",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = onSyncClick,
                        enabled = !isSyncing,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .testTag("header_sync_button")
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Sinkronisasi Spreadsheet",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = onScanQrClick,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .testTag("header_scan_qr_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scan QR Code Pedagang",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = { showLogoutDialog = true },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .testTag("header_logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Keluar",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Quick Insights / Profile Drawer
            AnimatedVisibility(
                visible = isHeaderExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    color = Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Integrasi & Konektivitas Webhook",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = DisperindagAccentGold
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF4CAF50))
                                )
                                Text(
                                    text = "Ready",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF4CAF50)
                                )
                            }
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.15f))
                        // Config and operator summary details
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Spreadsheet Terhubung:",
                                    fontSize = 9.sp,
                                    color = Color.LightGray
                                )
                                Text(
                                    text = "Pasar Waru Pamekasan (Shared)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Operator Aktif:",
                                    fontSize = 9.sp,
                                    color = Color.LightGray
                                )
                                Text(
                                    text = currentUser?.displayName ?: "Petugas Default",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DisperindagAccentGold
                                )
                            }
                        }
                        // Webhook Testing Trigger within the expanded topbar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = connectionTestResult ?: "Status Webhook: Belum diuji",
                                fontSize = 9.5.sp,
                                color = if (connectionTestResult?.contains("Berhasil") == true) Color(0xFF81C784) else Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        isTestingConnection = true
                                        connectionTestResult = "Sedang menghubungkan..."
                                        val service = com.example.data.remote.GoogleSheetSyncService()
                                        val result = service.testWebhookConnection()
                                        connectionTestResult = if (result.isSuccess) {
                                            "Koneksi Webhook Berhasil!"
                                        } else {
                                            "Koneksi Gagal: Terputus"
                                        }
                                        isTestingConnection = false
                                    }
                                },
                                enabled = !isTestingConnection,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White.copy(alpha = 0.2f),
                                    disabledContainerColor = Color.White.copy(alpha = 0.1f)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                if (isTestingConnection) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(10.dp),
                                        color = Color.White,
                                        strokeWidth = 1.5.dp
                                    )
                                } else {
                                    Text("Test Koneksi", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmation Logout Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { if (!isLoggingOut) showLogoutDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = if (isLoggingOut) "Proses Keluar..." else "Keluar Aplikasi",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (isLoggingOut) {
                        Text(
                            text = "Sedang mencatat aktivitas logout dan memproses keluar...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.error,
                            trackColor = MaterialTheme.colorScheme.errorContainer
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.error,
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "Menghubungkan ke Google Spreadsheet...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text(
                            text = "Apakah Anda yakin ingin keluar dari aplikasi pendataan ini?",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isLoggingOut) return@Button
                        isLoggingOut = true
                        val user = UserManager.currentUser.value
                        val email = user?.email ?: "Unknown"
                        val name = user?.displayName ?: "Petugas"
                        
                        coroutineScope.launch {
                            val service = com.example.data.remote.GoogleSheetSyncService()
                            service.logActivityToSheet("Logout", "User keluar dari aplikasi", email, name)
                            UserManager.logout(context)
                            isLoggingOut = false
                            showLogoutDialog = false
                        }
                    },
                    enabled = !isLoggingOut,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isLoggingOut) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Keluar", color = Color.White)
                    }
                }
            },
            dismissButton = {
                if (!isLoggingOut) {
                    TextButton(
                        onClick = { showLogoutDialog = false }
                    ) {
                        Text("Batal", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        )
    }
}
