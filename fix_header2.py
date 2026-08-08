import re

content = """                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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
            onDismissRequest = { showLogoutDialog = false },
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
                        text = "Keluar Aplikasi",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            },
            text = {
                Text(
                    text = "Apakah Anda yakin ingin keluar dari aplikasi pendataan ini?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        UserManager.logout(context)
                        showLogoutDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Keluar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutDialog = false }
                ) {
                    Text("Batal", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}
"""

with open('app/src/main/java/com/example/ui/components/HeaderBar.kt', 'r') as f:
    text = f.read()

# Replace from the Row horizontalArrangement = Arrangement.spacedBy(12.dp) to the end
idx = text.find('                Row(\n                    verticalAlignment = Alignment.CenterVertically,\n                    horizontalArrangement = Arrangement.spacedBy(12.dp)')
if idx != -1:
    text = text[:idx] + content
else:
    print("Could not find start of replacement")

with open('app/src/main/java/com/example/ui/components/HeaderBar.kt', 'w') as f:
    f.write(text)

