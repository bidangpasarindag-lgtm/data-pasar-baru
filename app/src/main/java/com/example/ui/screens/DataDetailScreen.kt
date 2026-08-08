package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ProgressDialog
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.example.data.config.AgencyConfigManager
import com.example.data.model.Pedagang
import com.example.ui.theme.DisperindagAccentGold
import com.example.ui.theme.DisperindagGreenPrimary
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.filled.QrCodeScanner
import com.example.util.QrCodeUtils
import com.example.util.DriveImageUtils
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import com.example.util.PdfExportUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataDetailScreen(
    pedagang: Pedagang,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onViewPhoto: (String) -> Unit
) {
    val context = LocalContext.current
    val agencyConfig by AgencyConfigManager.config.collectAsState()
    val (isComplete, missingFields) = agencyConfig.checkCompleteness(pedagang)
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isGeneratingPdf by remember { mutableStateOf(false) }
    var pdfProgress by remember { mutableFloatStateOf(-1f) }
    var pdfProcessName by remember { mutableStateOf("") }
    var pdfEstimatedTime by remember { mutableStateOf("") }
    val fotoPedagangDirect = DriveImageUtils.convertToDirectUrl(pedagang.fotoPedagangUri)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail Data Pedagang", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("detail_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            PdfExportUtils.generateAndOpenPdf(
                                context = context,
                                pedagangList = listOf(pedagang),
                                fileNamePrefix = "Kartu_${pedagang.namaPedagang}",
                                onStart = { 
                                    isGeneratingPdf = true
                                    pdfProgress = 0f
                                    pdfProcessName = "Memulai..."
                                },
                                onProgress = { progress, name, time ->
                                    pdfProgress = progress
                                    pdfProcessName = name
                                    pdfEstimatedTime = time
                                },
                                onComplete = { isGeneratingPdf = false }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DisperindagGreenPrimary),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.padding(end = 8.dp).testTag("cetak_pdf_single_button")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cetak Kartu PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Card Trader Header
            Card(
                colors = CardDefaults.cardColors(containerColor = DisperindagGreenPrimary),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable { fotoPedagangDirect?.let { onViewPhoto(it) } },
                        contentAlignment = Alignment.Center
                    ) {
                        if (fotoPedagangDirect != null) {
                            AsyncImage(
                                model = fotoPedagangDirect,
                                contentDescription = pedagang.namaPedagang,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = DisperindagGreenPrimary,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = pedagang.namaPedagang,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${pedagang.jenisRuangDagang} No. ${pedagang.nomorKiosLos}",
                            style = MaterialTheme.typography.titleMedium,
                            color = DisperindagAccentGold,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Komoditi: ${pedagang.komoditi}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            // Incomplete Data Warning Banner (Detail Page Warning)
            if (agencyConfig.enableCompletenessWarning && !isComplete) {
                Surface(
                    color = Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFFFCDD2)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Data Belum Lengkap",
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Data Pedagang Belum Lengkap",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD32F2F)
                            )
                            Text(
                                text = "Field belum diisi/lengkap: ${missingFields.joinToString(", ")}",
                                fontSize = 11.sp,
                                color = Color(0xFFB71C1C)
                            )
                        }
                    }
                }
            }

            // Section QR Code Identifikasi Pedagang Unik (DITAMPILKAN SEBELUM INFORMASI UTAMA)
            val qrPayload = remember(pedagang) { QrCodeUtils.getMerchantQrPayload(pedagang) }
            val qrBitmap = remember(qrPayload) { QrCodeUtils.generateQrCodeBitmap(qrPayload, 512) }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("detail_qr_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = DisperindagGreenPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "QR CODE IDENTIFIKASI UNIK",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = DisperindagGreenPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (qrBitmap != null) {
                        Box(
                            modifier = Modifier
                                .size(170.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .border(2.dp, DisperindagGreenPrimary, RoundedCornerShape(12.dp))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "QR Code ${pedagang.namaPedagang}",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Kode Identifikasi Unik Pedagang:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = qrPayload,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Dapat discan melalui fitur Scan QR Code di aplikasi ini untuk verifikasi langsung identitas pedagang.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Section 1: Dynamic Data List (INFORMASI UTAMA PEDAGANG)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "INFORMASI UTAMA PEDAGANG",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = DisperindagGreenPrimary
                    )

                    DetailRow(icon = Icons.Default.CalendarToday, label = "Timestamp (Waktu Input)", value = pedagang.timestamp)
                    DetailRow(icon = Icons.Default.Email, label = "Email Petugas (Akun Google)", value = pedagang.emailAddress)
                    DetailRow(icon = Icons.Default.Person, label = "NAMA PEDAGANG", value = pedagang.namaPedagang, isBold = true)
                    DetailRow(icon = Icons.Default.Badge, label = "NIK", value = pedagang.nik.ifBlank { "(Tidak diisi)" })
                    DetailRow(icon = Icons.Default.LocationOn, label = "ALAMAT", value = pedagang.alamat.ifBlank { "(Tidak diisi)" })
                    DetailRow(icon = Icons.Default.Phone, label = "NOMOR HP", value = pedagang.nomorHp.ifBlank { "(Tidak diisi)" })
                    DetailRow(icon = Icons.Default.Storefront, label = "JENIS RUANG DAGANG", value = pedagang.jenisRuangDagang)
                    DetailRow(icon = Icons.Default.Storefront, label = "NOMOR KIOS/LOS", value = pedagang.nomorKiosLos, isBold = true)
                    DetailRow(icon = Icons.Default.ShoppingBag, label = "KOMODITI/JENIS USAHA", value = pedagang.komoditi)
                    DetailRow(icon = Icons.Default.CalendarToday, label = "LAMA BERJUALAN", value = "${pedagang.lamaBerjualan} Tahun")
                    DetailRow(icon = Icons.Default.CheckCircle, label = "STATUS", value = pedagang.status)
                    DetailRow(icon = Icons.Default.Notes, label = "KETERANGAN", value = pedagang.keterangan.ifBlank { "(Tidak ada keterangan)" })
                }
            }

            // Section 2: Photo Documents Preview & Drive Links
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "DOKUMEN FOTO PEDAGANG (KOLOM P, Q, R)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = DisperindagGreenPrimary
                    )

                    PhotoPreviewCard(
                        title = "1. FOTO PEDAGANG (Kolom P: FOTO PEDAGANG GDRIVE)",
                        imageUri = pedagang.fotoPedagangUri,
                        onViewClick = onViewPhoto,
                        testTag = "detail_foto_pedagang"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    PhotoPreviewCard(
                        title = "2. FOTO KTP (Kolom Q: FOTO KTP GDRIVE)",
                        imageUri = pedagang.fotoKtpUri,
                        onViewClick = onViewPhoto,
                        testTag = "detail_foto_ktp"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    PhotoPreviewCard(
                        title = "3. FOTO SURAT PERNYATAAN (Kolom R: FOTO SURAT PERNYATAAN GDRIVE)",
                        imageUri = pedagang.fotoSuratPernyataanUri,
                        onViewClick = onViewPhoto,
                        testTag = "detail_foto_surat"
                    )
                }
            }

            // Section Action Buttons (Edit & Delete)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onEditClick,
                        colors = ButtonDefaults.buttonColors(containerColor = DisperindagGreenPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("detail_edit_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit Data", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = { showDeleteDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("detail_delete_button")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Hapus Data", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        ProgressDialog(
        showDialog = isGeneratingPdf,
        title = "Membuat PDF",
        message = "Sedang membuat kartu bukti pendataan dan merender dokumen...",
        progress = pdfProgress,
        processName = pdfProcessName,
        estimatedTime = pdfEstimatedTime
    )


    if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                title = { Text("Konfirmasi Hapus Data") },
                text = { Text("Apakah Anda yakin ingin menghapus data pedagang '${pedagang.namaPedagang}' (${pedagang.jenisRuangDagang} No. ${pedagang.nomorKiosLos})?") },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteDialog = false
                            onDeleteClick()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Hapus", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showDeleteDialog = false }) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}

@Composable
fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = DisperindagGreenPrimary,
            modifier = Modifier
                .size(20.dp)
                .padding(top = 2.dp)
        )
        Column {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun PhotoPreviewCard(
    title: String,
    imageUri: String?,
    onViewClick: (String) -> Unit,
    testTag: String
) {
    val uriHandler = LocalUriHandler.current
    val primaryDisplayUrl = DriveImageUtils.convertToDirectUrl(imageUri)
    val fallbackDisplayUrl = DriveImageUtils.getFallbackUrl(imageUri)
    val driveWebUrl = DriveImageUtils.getDriveWebUrl(imageUri)
    val fileId = DriveImageUtils.extractFileId(imageUri)

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(6.dp))

        if (!imageUri.isNullOrBlank()) {
            var currentUrl by remember(imageUri) { mutableStateOf(primaryDisplayUrl) }

            // Photo Preview Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, DisperindagGreenPrimary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .background(Color(0xFFF8F9FA))
                    .clickable {
                        val urlToView = currentUrl ?: driveWebUrl ?: imageUri
                        onViewClick(urlToView)
                    }
                    .testTag(testTag)
            ) {
                SubcomposeAsyncImage(
                    model = currentUrl,
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter,
                    loading = {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = DisperindagGreenPrimary, modifier = Modifier.size(28.dp))
                        }
                    },
                    error = {
                        if (currentUrl == primaryDisplayUrl && fallbackDisplayUrl != null) {
                            LaunchedEffect(Unit) {
                                currentUrl = fallbackDisplayUrl
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "Gunakan tombol di bawah untuk membuka foto di Google Drive",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                )

                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Perbesar", color = Color.White, fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Google Drive Link Section directly below the photo
            Surface(
                color = DisperindagGreenPrimary.copy(alpha = 0.08f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, DisperindagGreenPrimary.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            tint = DisperindagGreenPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Link Google Drive Foto:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = DisperindagGreenPrimary
                        )
                    }

                    val targetLink = driveWebUrl ?: imageUri
                    Text(
                        text = targetLink,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .clickable {
                                try {
                                    uriHandler.openUri(targetLink)
                                } catch (_: Exception) {}
                            }
                    )

                    if (fileId != null) {
                        Text(
                            text = "ID File Drive: $fileId",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedButton(
                        onClick = {
                            try {
                                uriHandler.openUri(targetLink)
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(vertical = 4.dp, horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Buka Foto di Google Drive / Browser", fontSize = 11.sp)
                    }
                }
            }
        } else {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("Foto belum diunggah (Kolom ini kosong di Spreadsheet)", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }
}
