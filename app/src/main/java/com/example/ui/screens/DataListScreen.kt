package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ProgressDialog
import coil.compose.AsyncImage
import com.example.data.config.AgencyConfigManager
import com.example.data.model.Pedagang
import com.example.ui.theme.CardBorderLight
import com.example.ui.theme.DisperindagAccentGold
import com.example.ui.theme.DisperindagGreenPrimary
import com.example.util.DriveImageUtils
import com.example.util.PdfExportUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataListScreen(
    pedagangList: List<Pedagang>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onPedagangClick: (Pedagang) -> Unit,
    onEditPedagang: (Pedagang) -> Unit = {},
    onDeletePedagang: (Pedagang) -> Unit = {},
    onAddNewClick: () -> Unit = {},
    onScanQrClick: () -> Unit = {},
    onViewPhoto: (String) -> Unit
) {
    val context = LocalContext.current
    val agencyConfig by AgencyConfigManager.config.collectAsState()

    var selectedFilterChip by remember { mutableStateOf<String?>(null) }
    var selectedSortOption by remember { mutableStateOf("Nama (A-Z)") }
    var isSortMenuExpanded by remember { mutableStateOf(false) }
    var isGeneratingPdf by remember { mutableStateOf(false) }
    var pdfProgress by remember { mutableFloatStateOf(-1f) }
    var pdfProcessName by remember { mutableStateOf("") }
    var pdfEstimatedTime by remember { mutableStateOf("") }
    var pedagangToDelete by remember { mutableStateOf<Pedagang?>(null) }

    val sortOptions = listOf(
        "Nama (A-Z)",
        "Nama (Z-A)",
        "Nomor Kios/Los",
        "Tanggal (Terbaru)",
        "Tanggal (Terlama)",
        "Data Belum Lengkap First"
    )

    // Dynamic extraction of unique JENIS RUANG DAGANG values from data
    val dynamicRuangList = remember(pedagangList) {
        pedagangList.map { it.jenisRuangDagang.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    val filteredList = remember(pedagangList, searchQuery, selectedFilterChip, selectedSortOption, agencyConfig) {
        val filtered = pedagangList.filter { p ->
            val matchesSearch = searchQuery.isBlank() ||
                    p.namaPedagang.contains(searchQuery, ignoreCase = true) ||
                    p.nomorKiosLos.contains(searchQuery, ignoreCase = true) ||
                    p.nik.contains(searchQuery, ignoreCase = true) ||
                    p.alamat.contains(searchQuery, ignoreCase = true) ||
                    p.komoditi.contains(searchQuery, ignoreCase = true)

            val matchesFilter = selectedFilterChip == null ||
                    p.jenisRuangDagang.equals(selectedFilterChip, ignoreCase = true) ||
                    p.status.equals(selectedFilterChip, ignoreCase = true)

            matchesSearch && matchesFilter
        }

        when (selectedSortOption) {
            "Nama (A-Z)" -> filtered.sortedBy { it.namaPedagang.lowercase() }
            "Nama (Z-A)" -> filtered.sortedByDescending { it.namaPedagang.lowercase() }
            "Nomor Kios/Los" -> filtered.sortedBy { it.nomorKiosLos.lowercase() }
            "Tanggal (Terbaru)" -> filtered.sortedByDescending { it.timestamp }
            "Tanggal (Terlama)" -> filtered.sortedBy { it.timestamp }
            "Data Belum Lengkap First" -> filtered.sortedBy {
                val (isComplete, _) = agencyConfig.checkCompleteness(it)
                if (isComplete) 1 else 0
            }
            else -> filtered
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Search Bar & Scan QR Button Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Cari nama, NIK, Kios/Los...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = DisperindagGreenPrimary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Bersihkan")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DisperindagGreenPrimary,
                        unfocusedBorderColor = Color.LightGray
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("search_pedagang_input")
                )

                FilledTonalButton(
                    onClick = onScanQrClick,
                    shape = CircleShape,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = DisperindagGreenPrimary,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(12.dp),
                    modifier = Modifier.testTag("scan_qr_main_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Scan QR",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dynamic Quick Filter Chips (Otomatis menyesuaikan data di kolom JENIS RUANG DAGANG)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedFilterChip == null,
                        onClick = { selectedFilterChip = null },
                        label = { Text("Semua (${pedagangList.size})") },
                        modifier = Modifier.testTag("filter_chip_all")
                    )
                }
                
                // Dynamic Items from JENIS RUANG DAGANG
                items(dynamicRuangList) { ruang ->
                    val count = pedagangList.count { it.jenisRuangDagang.equals(ruang, ignoreCase = true) }
                    FilterChip(
                        selected = selectedFilterChip.equals(ruang, ignoreCase = true),
                        onClick = {
                            selectedFilterChip = if (selectedFilterChip.equals(ruang, ignoreCase = true)) null else ruang
                        },
                        label = { Text("$ruang ($count)") },
                        modifier = Modifier.testTag("filter_chip_$ruang")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Results count badge, Sort Dropdown & Batch Cetak PDF Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Pedagang (${filteredList.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = DisperindagGreenPrimary
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Sort Button & Menu
                    Box {
                        OutlinedButton(
                            onClick = { isSortMenuExpanded = true },
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("sort_menu_button")
                        ) {
                            Icon(Icons.Default.Sort, contentDescription = "Urutkan", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(selectedSortOption, fontSize = 11.sp)
                        }

                        DropdownMenu(
                            expanded = isSortMenuExpanded,
                            onDismissRequest = { isSortMenuExpanded = false }
                        ) {
                            sortOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option, fontSize = 13.sp) },
                                    onClick = {
                                        selectedSortOption = option
                                        isSortMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        PdfExportUtils.generateAndOpenPdf(
                            context = context,
                            pedagangList = filteredList,
                            fileNamePrefix = "Kartu_Bukti_Pendataan_Pedagang",
                            onStart = { 
                                isGeneratingPdf = true
                                pdfProgress = 0f
                                pdfProcessName = "Memulai Cetak Batch..."
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
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("cetak_pdf_semua_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "Cetak Semua Kartu PDF",
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cetak PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tidak ada data pedagang ditemukan",
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredList, key = { it.id }) { pedagang ->
                        PedagangCard(
                            pedagang = pedagang,
                            onClick = { onPedagangClick(pedagang) },
                            onEdit = { onEditPedagang(pedagang) },
                            onDelete = { pedagangToDelete = pedagang },
                            onViewPhoto = onViewPhoto
                        )
                    }
                }
            }
        }

        // Floating Action Button to Add New Pedagang (if allowed)
        if (agencyConfig.allowCreate) {
            ExtendedFloatingActionButton(
                onClick = onAddNewClick,
                icon = { Icon(Icons.Default.Add, contentDescription = null, tint = Color.White) },
                text = { Text("Tambah Pedagang", color = Color.White, fontWeight = FontWeight.Bold) },
                containerColor = DisperindagGreenPrimary,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 80.dp, end = 16.dp)
                    .testTag("add_new_pedagang_fab")
            )
        }
    }

    ProgressDialog(
        showDialog = isGeneratingPdf,
        title = "Membuat PDF",
        message = "Sedang membuat kartu bukti pendataan dan merender dokumen...",
        progress = pdfProgress,
        processName = pdfProcessName,
        estimatedTime = pdfEstimatedTime
    )

    // Delete Confirmation Dialog
    pedagangToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pedagangToDelete = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Hapus Data Pedagang?") },
            text = { Text("Apakah Anda yakin ingin menghapus data '${target.namaPedagang}' (Kios/Los: ${target.nomorKiosLos})? Data yang dihapus juga akan disinkronkan ke Spreadsheet dan Google Drive.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeletePedagang(target)
                        pedagangToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Ya, Hapus Data")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { pedagangToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun PedagangCard(
    pedagang: Pedagang,
    onClick: () -> Unit,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    onViewPhoto: (String) -> Unit
) {
    val agencyConfig by AgencyConfigManager.config.collectAsState()
    val (isComplete, missingFields) = agencyConfig.checkCompleteness(pedagang)
    val fotoDirect = DriveImageUtils.convertToDirectUrl(pedagang.fotoPedagangUri)

    var showMenu by remember { mutableStateOf(false) }
    var isGeneratingPdf by remember { mutableStateOf(false) }
    var pdfProgress by remember { mutableFloatStateOf(-1f) }
    var pdfProcessName by remember { mutableStateOf("") }
    var pdfEstimatedTime by remember { mutableStateOf("") }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pedagang_card_${pedagang.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Photo Thumbnail (Conditional based on cardShowPhotos)
                    if (agencyConfig.cardShowPhotos) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(DisperindagGreenPrimary.copy(alpha = 0.1f))
                                .clickable {
                                    fotoDirect?.let { onViewPhoto(it) }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (fotoDirect != null) {
                                AsyncImage(
                                    model = fotoDirect,
                                    contentDescription = pedagang.namaPedagang,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    alignment = Alignment.TopCenter
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = DisperindagGreenPrimary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }

                    Column {
                        Text(
                            text = pedagang.namaPedagang,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (agencyConfig.cardShowNik && pedagang.nik.isNotBlank()) {
                            Text(
                                text = "NIK: ${pedagang.nik}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Surface(
                                color = DisperindagGreenPrimary,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "${pedagang.jenisRuangDagang}: ${pedagang.nomorKiosLos}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = pedagang.komoditi,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Right Controls (Status Badge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (agencyConfig.cardShowStatus) {
                        Surface(
                            color = if (pedagang.status.equals("Aktif", ignoreCase = true)) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                            contentColor = if (pedagang.status.equals("Aktif", ignoreCase = true)) Color(0xFF2E7D32) else Color(0xFFE65100),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = pedagang.status,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Incomplete Data Warning Banner (Positioned BELOW the photo & header info with RED color)
            if (agencyConfig.enableCompletenessWarning && !isComplete) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFFFCDD2)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Data Belum Lengkap",
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Data Belum Lengkap: ${missingFields.joinToString(", ")}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD32F2F)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = CardBorderLight)
            Spacer(modifier = Modifier.height(8.dp))

            // Details Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (agencyConfig.cardShowHp && pedagang.nomorHp.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color.Gray)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = pedagang.nomorHp, fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                    if (agencyConfig.cardShowAlamat && pedagang.alamat.isNotBlank()) {
                        Text(
                            text = pedagang.alamat,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    Text(
                        text = "Lama Dagang: ${pedagang.lamaBerjualan} Thn | Waktu: ${pedagang.timestamp}",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (agencyConfig.allowUpdate) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(32.dp).testTag("card_edit_button_${pedagang.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Data Pedagang",
                                tint = DisperindagGreenPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (agencyConfig.allowDelete) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(32.dp).testTag("card_delete_button_${pedagang.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Hapus Data",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    val cardContext = LocalContext.current
                    IconButton(
                        onClick = {
                            PdfExportUtils.generateAndOpenPdf(
                                context = cardContext,
                                pedagangList = listOf(pedagang),
                                fileNamePrefix = "Kartu_${pedagang.namaPedagang}",
                                onStart = { 
                                    isGeneratingPdf = true
                                    pdfProgress = 0f
                                    pdfProcessName = "Memulai Cetak Kartu..."
                                },
                                onProgress = { progress, name, time ->
                                    pdfProgress = progress
                                    pdfProcessName = name
                                    pdfEstimatedTime = time
                                },
                                onComplete = { isGeneratingPdf = false }
                            )
                        },
                        modifier = Modifier.size(32.dp).testTag("card_pdf_button_${pedagang.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Cetak Kartu PDF",
                            tint = DisperindagGreenPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    ProgressDialog(
        showDialog = isGeneratingPdf,
        title = "Membuat PDF",
        message = "Sedang membuat kartu bukti pendataan...",
        progress = pdfProgress,
        processName = pdfProcessName,
        estimatedTime = pdfEstimatedTime
    )
}

