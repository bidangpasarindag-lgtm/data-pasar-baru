package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ProgressDialog
import com.example.data.config.AgencyConfigManager
import com.example.data.model.Pedagang
import com.example.ui.theme.DisperindagAccentGold
import com.example.ui.theme.DisperindagGreenPrimary
import com.example.ui.viewmodel.GroupByMode
import com.example.util.PdfExportUtils

@Composable
fun GroupedDataScreen(
    pedagangList: List<Pedagang>,
    groupByMode: GroupByMode,
    onGroupByModeChange: (GroupByMode) -> Unit,
    onPedagangClick: (Pedagang) -> Unit,
    onEditPedagang: (Pedagang) -> Unit = {},
    onDeletePedagang: (Pedagang) -> Unit = {},
    onViewPhoto: (String) -> Unit
) {
    val context = LocalContext.current
    val agencyConfig by AgencyConfigManager.config.collectAsState()

    var selectedSortOption by remember { mutableStateOf("Nama (A-Z)") }
    var isSortMenuExpanded by remember { mutableStateOf(false) }
    var isGeneratingPdf by remember { mutableStateOf(false) }
    var pdfProgress by remember { mutableFloatStateOf(-1f) }
    var pdfProcessName by remember { mutableStateOf("") }
    var pdfEstimatedTime by remember { mutableStateOf("") }

    val sortOptions = listOf(
        "Nama (A-Z)",
        "Nama (Z-A)",
        "Nomor Kios/Los",
        "Tanggal (Terbaru)",
        "Tanggal (Terlama)",
        "Data Belum Lengkap First"
    )

    // Sort items first
    val sortedList = remember(pedagangList, selectedSortOption, agencyConfig) {
        val sorted = when (selectedSortOption) {
            "Nama (A-Z)" -> pedagangList.sortedBy { it.namaPedagang.lowercase() }
            "Nama (Z-A)" -> pedagangList.sortedByDescending { it.namaPedagang.lowercase() }
            "Nomor Kios/Los" -> pedagangList.sortedBy { it.nomorKiosLos.lowercase() }
            "Tanggal (Terbaru)" -> pedagangList.sortedByDescending { it.timestamp }
            "Tanggal (Terlama)" -> pedagangList.sortedBy { it.timestamp }
            "Data Belum Lengkap First" -> pedagangList.sortedBy {
                val (isComplete, _) = agencyConfig.checkCompleteness(it)
                if (isComplete) 1 else 0
            }
            else -> pedagangList
        }
        sorted
    }

    // Group items according to selected mode
    val groupedMap = remember(sortedList, groupByMode) {
        when (groupByMode) {
            GroupByMode.JENIS_RUANG -> sortedList.groupBy { it.jenisRuangDagang.ifBlank { "Lainnya" } }
            GroupByMode.KOMODITI -> sortedList.groupBy { it.komoditi.ifBlank { "Lainnya" } }
            GroupByMode.STATUS -> sortedList.groupBy { it.status.ifBlank { "Lainnya" } }
            GroupByMode.TIM_PENDATA -> sortedList.groupBy { it.emailAddress.ifBlank { "Unassigned" } }
            else -> sortedList.groupBy { it.jenisRuangDagang.ifBlank { "Semua" } }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Mode Selector Chips
        Text(
            text = "Pengelompokan Data Pedagang:",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = DisperindagGreenPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = groupByMode == GroupByMode.JENIS_RUANG || groupByMode == GroupByMode.NONE,
                    onClick = { onGroupByModeChange(GroupByMode.JENIS_RUANG) },
                    label = { Text("Jenis Ruang") },
                    modifier = Modifier.testTag("group_by_jenis_ruang")
                )
            }
            item {
                FilterChip(
                    selected = groupByMode == GroupByMode.KOMODITI,
                    onClick = { onGroupByModeChange(GroupByMode.KOMODITI) },
                    label = { Text("Komoditi") },
                    modifier = Modifier.testTag("group_by_komoditi")
                )
            }
            item {
                FilterChip(
                    selected = groupByMode == GroupByMode.STATUS,
                    onClick = { onGroupByModeChange(GroupByMode.STATUS) },
                    label = { Text("Status") },
                    modifier = Modifier.testTag("group_by_status")
                )
            }
            item {
                FilterChip(
                    selected = groupByMode == GroupByMode.TIM_PENDATA,
                    onClick = { onGroupByModeChange(GroupByMode.TIM_PENDATA) },
                    label = { Text("Tim Pendata (Email)") },
                    modifier = Modifier.testTag("group_by_pendata")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Sorting Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Urutan Pedagang di Kelompok:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Box {
                OutlinedButton(
                    onClick = { isSortMenuExpanded = true },
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                    modifier = Modifier.testTag("group_sort_menu_button")
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

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            groupedMap.forEach { (groupTitle, itemsInGroup) ->
                item(key = groupTitle) {
                    GroupExpandableCard(
                        groupTitle = groupTitle,
                        items = itemsInGroup,
                        onPedagangClick = onPedagangClick,
                        onEditPedagang = onEditPedagang,
                        onDeletePedagang = onDeletePedagang,
                        onViewPhoto = onViewPhoto,
                        onPdfExport = { items ->
                            PdfExportUtils.generateAndOpenPdf(
                                context = context,
                                pedagangList = items,
                                fileNamePrefix = "Batch_${groupTitle}",
                                onStart = { 
                                    isGeneratingPdf = true
                                    pdfProgress = 0f
                                    pdfProcessName = "Memulai Cetak Batch Kelompok..."
                                },
                                onProgress = { progress, name, time ->
                                    pdfProgress = progress
                                    pdfProcessName = name
                                    pdfEstimatedTime = time
                                },
                                onComplete = { isGeneratingPdf = false }
                            )
                        }
                    )
                }
            }
        }
    }

    ProgressDialog(
        showDialog = isGeneratingPdf,
        title = "Membuat PDF Kelompok",
        message = "Sedang merender dokumen untuk kelompok ini...",
        progress = pdfProgress,
        processName = pdfProcessName,
        estimatedTime = pdfEstimatedTime
    )
}

@Composable
fun GroupExpandableCard(
    groupTitle: String,
    items: List<Pedagang>,
    onPedagangClick: (Pedagang) -> Unit,
    onEditPedagang: (Pedagang) -> Unit,
    onDeletePedagang: (Pedagang) -> Unit,
    onViewPhoto: (String) -> Unit,
    onPdfExport: (List<Pedagang>) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Group Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DisperindagGreenPrimary.copy(alpha = 0.08f))
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null, tint = DisperindagGreenPrimary)
                    Text(
                        text = groupTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { onPdfExport(items) },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("batch_pdf_button_${groupTitle}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Cetak PDF Batch",
                            tint = DisperindagGreenPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Surface(
                        color = DisperindagGreenPrimary,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "${items.size} Pedagang",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = DisperindagGreenPrimary
                    )
                }
            }

            if (expanded) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items.forEach { pedagang ->
                        PedagangCard(
                            pedagang = pedagang,
                            onClick = { onPedagangClick(pedagang) },
                            onEdit = { onEditPedagang(pedagang) },
                            onDelete = { onDeletePedagang(pedagang) },
                            onViewPhoto = onViewPhoto
                        )
                    }
                }
            }
        }
    }
}
