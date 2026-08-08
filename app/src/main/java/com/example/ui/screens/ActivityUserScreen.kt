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

    // Grouped and Filtered list
    val sortedActivities = remember(filteredActivities) {
        filteredActivities.sortedByDescending { com.example.util.DateTimeUtils.parseTimestampToMillis(it.timestamp) }
    }
    val groupedActivities = sortedActivities.groupBy { 
        it.timestamp.substringBefore(" ") 
    }
    
    val sortedDates = groupedActivities.keys.toList() // Already sorted descending because of sortedByDescending above
    var collapsedDates by remember { mutableStateOf(setOf<String>()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ... (Header remains the same)
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
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh data")
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
                sortedDates.forEach { date ->
                    val isCollapsed = collapsedDates.contains(date)
                    val activitiesOnDate = groupedActivities[date] ?: emptyList()

                    item(key = date) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    collapsedDates = if (isCollapsed) collapsedDates - date
                                    else collapsedDates + date
                                }
                                .padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = DisperindagGreenPrimary, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = date,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = DisperindagGreenPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = DisperindagGreenPrimary.copy(alpha = 0.1f),
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            text = activitiesOnDate.size.toString(),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DisperindagGreenPrimary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = if (isCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                    contentDescription = null,
                                    tint = DisperindagGreenPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    if (!isCollapsed) {
                        items(activitiesOnDate.mapIndexed { idx, act -> "${date}_$idx" to act }, key = { it.first }) { (_, activity) ->
                            val activityIndex = activities.indexOf(activity)
                            val isExpanded = expandedActivityId == activityIndex

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
                                activity.aktivitas.contains("Logout", ignoreCase = true) || activity.aktivitas.contains("Keluar", ignoreCase = true) ->
                                    Icons.Default.Logout to Color(0xFF607D8B)
                                else ->
                                    Icons.Default.SyncAlt to Color(0xFFF9A825)
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expandedActivityId = if (isExpanded) null else activityIndex }
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
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(tint.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = tint,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    // Connecting timeline line
                                    Box(
                                        modifier = Modifier
                                            .width(1.5.dp)
                                            .height(if (isExpanded) 120.dp else 60.dp)
                                            .background(Color.LightGray.copy(alpha = 0.2f))
                                    )
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
                                        .padding(bottom = 8.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Text(
                                                text = activity.aktivitas,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = DisperindagGreenPrimary
                                            )
                                            Text(
                                                text = activity.timestamp.substringAfter(" "),
                                                fontSize = 10.sp,
                                                color = Color.Gray,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))

                                        Text(
                                            text = activity.namaPetugas,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = activity.keterangan,
                                            fontSize = 10.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = if (isExpanded) 10 else 2,
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
                                                
                                                InfoRow("Username", activity.email, tint)
                                                InfoRow("Waktu Log", activity.timestamp, tint)
                                                InfoRow("Metode", "Cloud Sync GSheet", tint)
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
}

@Composable
fun InfoRow(label: String, value: String, tint: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            fontSize = 9.sp,
            color = Color.Gray
        )
        Text(
            text = value,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
