package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserActivity
import com.example.ui.theme.DisperindagAccentGold
import com.example.ui.theme.DisperindagGreenPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityUserScreen(
    activities: List<UserActivity>,
    isSyncing: Boolean,
    onRefresh: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Semua") }
    var expandedActivityId by remember { mutableStateOf<Int?>(null) }

    val filters = listOf("Semua", "Sinkronisasi", "Tambah", "Cetak", "Edit")

    // Filtered list
    val filteredActivities = activities.filter { activity ->
        val matchesQuery = activity.namaPetugas.contains(searchQuery, ignoreCase = true) ||
                activity.aktivitas.contains(searchQuery, ignoreCase = true) ||
                activity.keterangan.contains(searchQuery, ignoreCase = true) ||
                activity.email.contains(searchQuery, ignoreCase = true)

        val matchesCategory = when (selectedFilter) {
            "Semua" -> true
            "Sinkronisasi" -> activity.aktivitas.contains("sinkronisasi", ignoreCase = true) || activity.aktivitas.contains("inisialisasi", ignoreCase = true)
            "Tambah" -> activity.aktivitas.contains("tambah", ignoreCase = true) || activity.aktivitas.contains("daftar", ignoreCase = true)
            "Cetak" -> activity.aktivitas.contains("cetak", ignoreCase = true) || activity.aktivitas.contains("print", ignoreCase = true) || activity.aktivitas.contains("pdf", ignoreCase = true)
            "Edit" -> activity.aktivitas.contains("edit", ignoreCase = true) || activity.aktivitas.contains("ubah", ignoreCase = true) || activity.aktivitas.contains("perbarui", ignoreCase = true)
            else -> true
        }

        matchesQuery && matchesCategory
    }

    LaunchedEffect(Unit) {
        onRefresh()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Banner Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(DisperindagGreenPrimary, DisperindagGreenPrimary.copy(alpha = 0.85f))
                    )
                )
                .padding(vertical = 20.dp, horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AKTIVITAS USER & LOG SYSTEM",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Jejak aktivitas dan sinkronisasi petugas terdata secara real-time",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }

                IconButton(
                    onClick = onRefresh,
                    enabled = !isSyncing,
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh data")
                    }
                }
            }
        }

        // Search Bar & Filter Chips
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Search Text Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Cari aktivitas, nama petugas, atau email...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = DisperindagGreenPrimary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DisperindagGreenPrimary,
                    focusedLabelColor = DisperindagGreenPrimary
                )
            )

            // Category Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filters.forEach { filter ->
                    val isSelected = selectedFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DisperindagGreenPrimary,
                            selectedLabelColor = Color.White,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = Color.Transparent,
                            selectedBorderColor = Color.Transparent
                        )
                    )
                }
            }
        }

        // Timeline list
        if (filteredActivities.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.HistoryToggleOff,
                        contentDescription = null,
                        tint = Color.Gray.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Tidak Ada Rekaman Aktivitas",
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Silakan segarkan halaman atau ubah kriteria filter pencarian Anda.",
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(filteredActivities.mapIndexed { idx, act -> idx to act }) { (index, activity) ->
                    val isExpanded = expandedActivityId == index

                    // Resolve Activity Color Theme
                    val (icon, tint) = when {
                        activity.aktivitas.contains("Tambah", ignoreCase = true) || activity.aktivitas.contains("Daftar", ignoreCase = true) ->
                            Icons.Default.AddCircle to Color(0xFF2E7D32)
                        activity.aktivitas.contains("Edit", ignoreCase = true) || activity.aktivitas.contains("Ubah", ignoreCase = true) || activity.aktivitas.contains("Perbarui", ignoreCase = true) ->
                            Icons.Default.EditCalendar to Color(0xFF1976D2)
                        activity.aktivitas.contains("Cetak", ignoreCase = true) || activity.aktivitas.contains("PDF", ignoreCase = true) ->
                            Icons.Default.PictureAsPdf to Color(0xFFC62828)
                        activity.aktivitas.contains("Hapus", ignoreCase = true) ->
                            Icons.Default.DeleteForever to Color(0xFFD84315)
                        else ->
                            Icons.Default.SyncAlt to Color(0xFFF9A825)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedActivityId = if (isExpanded) null else index }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Left timeline line & dot
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(36.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(tint.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = tint,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Connecting timeline line
                            if (index < filteredActivities.size - 1) {
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(70.dp)
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(tint.copy(alpha = 0.4f), Color.LightGray.copy(alpha = 0.2f))
                                            )
                                        )
                                )
                            }
                        }

                        // Right Card content
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isExpanded) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isExpanded) 1.dp else 0.5.dp),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .padding(bottom = 12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = activity.aktivitas,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DisperindagGreenPrimary
                                    )
                                    Text(
                                        text = activity.timestamp.substringAfter(" "),
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = activity.namaPetugas,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Text(
                                    text = activity.email,
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = activity.keterangan,
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = if (isExpanded) 8 else 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )

                                AnimatedVisibility(
                                    visible = isExpanded,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .padding(top = 8.dp)
                                            .fillMaxWidth()
                                    ) {
                                        HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Tanggal Lengkap:",
                                                fontSize = 10.sp,
                                                color = Color.Gray
                                            )
                                            Text(
                                                text = activity.timestamp,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            /* removed duplicate */
                                        ) {
                                            Text(
                                                text = "Metode Log:",
                                                fontSize = 10.sp,
                                                color = Color.Gray
                                            )
                                            Text(
                                                text = "Sinkronisasi Google Sheet Gv1",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = tint
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
