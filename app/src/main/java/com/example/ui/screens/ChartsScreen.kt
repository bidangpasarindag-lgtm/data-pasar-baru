package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.config.AgencyConfigManager
import com.example.data.model.Pedagang
import com.example.ui.theme.DisperindagAccentGold
import com.example.ui.theme.DisperindagGreenPrimary
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartsScreen(
    pedagangList: List<Pedagang>
) {
    val agencyConfig by AgencyConfigManager.config.collectAsState()

    var selectedChartTab by remember { mutableStateOf("Sebaran Kualitatif") }
    var selectedCategoryTab by remember { mutableStateOf("Jenis Ruang") }
    var selectedBarLabel by remember { mutableStateOf<String?>(null) }
    var selectedSliceIndex by remember { mutableStateOf(-1) }

    val totalCount = pedagangList.size
    val activeCount = pedagangList.count { it.status.equals("Aktif", ignoreCase = true) }
    val incompleteCount = pedagangList.count { !agencyConfig.checkCompleteness(it).first }
    val avgLama = if (totalCount > 0) pedagangList.map { it.lamaBerjualan }.average() else 0.0

    // Grouping
    val jenisRuangMap = pedagangList.groupingBy { it.jenisRuangDagang.ifBlank { "Lainnya" } }.eachCount()
    val komoditiMap = pedagangList.groupingBy { it.komoditi.ifBlank { "Lainnya" } }.eachCount()
    val statusMap = pedagangList.groupingBy { it.status.ifBlank { "Belum Ada Status" } }.eachCount()
    val pendataMap = pedagangList.groupingBy { it.emailAddress.substringBefore("@") }.eachCount()

    // Experiential categories for comparative breakdown
    val expNewcomers = pedagangList.count { it.lamaBerjualan <= 2 }
    val expIntermediate = pedagangList.count { it.lamaBerjualan in 3..5 }
    val expExperienced = pedagangList.count { it.lamaBerjualan in 6..10 }
    val expVeterans = pedagangList.count { it.lamaBerjualan > 10 }

    val topRuang = jenisRuangMap.entries.maxByOrNull { it.value }
    val topKomoditi = komoditiMap.entries.maxByOrNull { it.value }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Upper Executive Banner
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(DisperindagGreenPrimary, Color(0xFF1B5E20))
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Analytics,
                    contentDescription = null,
                    tint = DisperindagAccentGold,
                    modifier = Modifier.size(38.dp)
                )
                Column {
                    Text(
                        text = "EXECUTIVE ANALYTICS BOARD",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Dashboard visualisasi interaktif data verifikasi pedagang",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }

        // Key Performance Indicator (KPI) Metric Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KpiMetricCard(
                title = "Total Terdata",
                value = "$totalCount",
                unit = "Pedagang",
                icon = Icons.Default.Storefront,
                gradientColors = listOf(Color(0xFFE8F5E9), Color(0xFFC8E6C9)),
                textColor = Color(0xFF1B5E20),
                modifier = Modifier.weight(1f)
            )
            KpiMetricCard(
                title = "Status Aktif",
                value = "$activeCount",
                unit = "${if (totalCount > 0) (activeCount * 100 / totalCount) else 0}% Rasio",
                icon = Icons.Default.CheckCircle,
                gradientColors = listOf(Color(0xFFE3F2FD), Color(0xFFBBDEFB)),
                textColor = Color(0xFF0D47A1),
                modifier = Modifier.weight(1f)
            )
            KpiMetricCard(
                title = "Belum Lengkap",
                value = "$incompleteCount",
                unit = "Ditinjau",
                icon = Icons.Default.Warning,
                gradientColors = listOf(Color(0xFFFFF3E0), Color(0xFFFFE0B2)),
                textColor = Color(0xFFE65100),
                modifier = Modifier.weight(1f)
            )
        }

        // Analytical Mode Switch (High level navigation)
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            SegmentedButton(
                selected = selectedChartTab == "Sebaran Kualitatif",
                onClick = { selectedChartTab = "Sebaran Kualitatif" },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.PieChart, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("Donut Sebaran", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            }
            SegmentedButton(
                selected = selectedChartTab == "Tingkat Longevitas",
                onClick = { selectedChartTab = "Tingkat Longevitas" },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("Longevitas Usaha", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Section rendering based on current selected tab
        if (selectedChartTab == "Sebaran Kualitatif") {
            // Dropdown/Filter to select Category GID (Jenis Ruang, Komoditi, Status, etc)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Dimensi Filter Analisa:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = DisperindagGreenPrimary
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val catFilters = listOf("Jenis Ruang", "Komoditi Usaha", "Status Pedagang", "Tim Pendata")
                    catFilters.forEach { cat ->
                        val isSel = selectedCategoryTab == cat
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSel) DisperindagGreenPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(
                                        alpha = 0.5f
                                    )
                                )
                                .clickable {
                                    selectedCategoryTab = cat
                                    selectedBarLabel = null
                                    selectedSliceIndex = -1
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cat.split(" ")[0], // Get first word to prevent overflow
                                color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Get selected map
            val activeMap = when (selectedCategoryTab) {
                "Jenis Ruang" -> jenisRuangMap
                "Komoditi Usaha" -> komoditiMap
                "Status Pedagang" -> statusMap
                else -> pendataMap
            }

            // Canvas-based interactive donut chart
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Diagram Donut Interaktif Sebaran ($selectedCategoryTab)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = DisperindagGreenPrimary
                    )
                    Text(
                        text = "Sentuh salah satu potongan diagram atau legenda untuk detail data kualitatif",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    if (activeMap.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Tidak ada data untuk direpresentasikan", color = Color.Gray, fontSize = 12.sp)
                        }
                    } else {
                        DonutChartWithLegend(
                            dataMap = activeMap,
                            selectedSliceIndex = selectedSliceIndex,
                            onSliceSelected = { selectedSliceIndex = it }
                        )

                        // If slice selected, show details
                        if (selectedSliceIndex >= 0 && selectedSliceIndex < activeMap.size) {
                            val targetKey = activeMap.keys.toList()[selectedSliceIndex]
                            val targetVal = activeMap.values.toList()[selectedSliceIndex]
                            val percent = if (totalCount > 0) (targetVal * 100 / totalCount) else 0

                            Spacer(modifier = Modifier.height(16.dp))
                            Surface(
                                color = DisperindagGreenPrimary.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(getChartColor(selectedSliceIndex))
                                    )
                                    Column {
                                        Text(
                                            text = "Kategori: $targetKey",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp,
                                            color = DisperindagGreenPrimary
                                        )
                                        Text(
                                            text = "Terdiri dari $targetVal pedagang ($percent% dari total keseluruhan)",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // High Fidelity rounded interactive histogram
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Histogram Batang Komparatif",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = DisperindagGreenPrimary
                    )
                    Text(
                        text = "Klik pilar batang untuk memunculkan daftar instan nama pedagang",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    InteractiveHistogram(
                        dataMap = activeMap,
                        selectedLabel = selectedBarLabel,
                        onBarSelected = { selectedBarLabel = if (selectedBarLabel == it) null else it }
                    )

                    // Expansion list of matching merchants when a bar is selected
                    selectedBarLabel?.let { label ->
                        val matchingList = pedagangList.filter { p ->
                            when (selectedCategoryTab) {
                                "Jenis Ruang" -> p.jenisRuangDagang.equals(label, ignoreCase = true)
                                "Komoditi Usaha" -> p.komoditi.equals(label, ignoreCase = true)
                                "Status Pedagang" -> p.status.equals(label, ignoreCase = true)
                                else -> p.emailAddress.substringBefore("@").equals(label, ignoreCase = true)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "📍 Anggota Kategori '$label' (${matchingList.size} Pedagang):",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DisperindagGreenPrimary
                                    )
                                    IconButton(
                                        onClick = { selectedBarLabel = null },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Close details", modifier = Modifier.size(16.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                matchingList.take(6).forEach { p ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "• ${p.namaPedagang}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Kios/Los ${p.nomorKiosLos} | ${p.status}",
                                            fontSize = 10.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                                if (matchingList.size > 6) {
                                    Text(
                                        text = "... dan ${matchingList.size - 6} pedagang lainnya dalam kategori ini",
                                        fontSize = 9.5.sp,
                                        color = Color.Gray,
                                        modifier = Modifier.padding(top = 4.dp, start = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Longevitas (Years in trade distribution) Tab
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Distribusi Lama Berjualan Pedagang",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = DisperindagGreenPrimary
                    )
                    Text(
                        text = "Pengelompokan pengalaman berdagang dalam satuan tahun",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    ExperienceProgressRow(label = "Sangat Baru (≤ 2 Tahun)", count = expNewcomers, total = totalCount, color = Color(0xFFE53935))
                    ExperienceProgressRow(label = "Berkembang (3 - 5 Tahun)", count = expIntermediate, total = totalCount, color = Color(0xFFFB8C00))
                    ExperienceProgressRow(label = "Berpengalaman (6 - 10 Tahun)", count = expExperienced, total = totalCount, color = Color(0xFF1E88E5))
                    ExperienceProgressRow(label = "Veteran / Turun-Temurun (> 10 Tahun)", count = expVeterans, total = totalCount, color = Color(0xFF43A047))

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.LightGray.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = DisperindagGreenPrimary, modifier = Modifier.size(18.dp))
                        Text(
                            text = "Rata-rata lama berjualan pedagang Pasar Waru saat ini adalah ${String.format("%.1f", avgLama)} tahun, mencerminkan ekosistem pasar yang sangat stabil.",
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Executive Actionable Insights & Recommendations (Dynamic Decision Making)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = DisperindagAccentGold,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Rekomendasi Analitis & Insight",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = DisperindagGreenPrimary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                val insight1 = when {
                    (topRuang?.value ?: 0) > (totalCount * 0.5) ->
                        "Kategori tempat dagang '${topRuang?.key}' sangat mendominasi kapasitas pasar (${topRuang?.value} unit). Disarankan melakukan diversifikasi ruang dagang baru untuk memfasilitasi jenis lesehan dan los."
                    else ->
                        "Struktur tempat dagang tersebar cukup merata, mendukung keseimbangan sirkulasi area dagang utama."
                }

                val insight2 = when {
                    expVeterans > (totalCount * 0.4) ->
                        "Mayoritas pedagang adalah veteran berdurasi dagang > 10 tahun. Diperlukan program digitalisasi pembayaran digital (QRIS) terfokus untuk modernisasi bertahap."
                    expNewcomers > (totalCount * 0.3) ->
                        "Ada masuknya pedagang baru sebesar ${expNewcomers} orang. Disarankan pendampingan permodalan mikro UMKM bekerja sama dengan perbankan daerah."
                    else ->
                        "Komposisi umur usaha pedagang stabil dan seimbang antara pendatang baru dan pelaku usaha senior."
                }

                val insight3 = when {
                    incompleteCount > 0 ->
                        "Terdapat $incompleteCount berkas pedagang yang belum memenuhi kelengkapan data. Segera tugaskan tim pendata lapangan untuk memverifikasi NIK dan lampiran foto dokumen."
                    else ->
                        "Sempurna! Semua berkas pedagang telah terverifikasi lengkap 100% di Google Spreadsheet."
                }

                InsightItem(title = "Alokasi Ruang Dagang", desc = insight1)
                Spacer(modifier = Modifier.height(10.dp))
                InsightItem(title = "Strategi Pengembangan Pedagang", desc = insight2)
                Spacer(modifier = Modifier.height(10.dp))
                InsightItem(title = "Tindakan Kelengkapan Administrasi", desc = insight3)
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun KpiMetricCard(
    title: String,
    value: String,
    unit: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradientColors: List<Color>,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .background(
                Brush.verticalGradient(colors = gradientColors),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = textColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp,
                    color = textColor
                )
            }
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = textColor.copy(alpha = 0.8f)
            )
            Text(
                text = unit,
                fontSize = 10.sp,
                color = textColor.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun DonutChartWithLegend(
    dataMap: Map<String, Int>,
    selectedSliceIndex: Int,
    onSliceSelected: (Int) -> Unit
) {
    val totalSum = dataMap.values.sum().toFloat()
    val sliceAngles = dataMap.values.map { if (totalSum > 0) (it / totalSum) * 360f else 0f }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side: Donut Canvas
        Box(
            modifier = Modifier
                .size(150.dp)
                .pointerInput(dataMap) {
                    detectTapGestures { offset ->
                        // Determine which slice was clicked based on offset relative to center
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val angleRad = atan2(offset.y - center.y, offset.x - center.x)
                        var angleDeg = Math.toDegrees(angleRad.toDouble()).toFloat()
                        if (angleDeg < 0) angleDeg += 360f

                        // Find corresponding slice
                        var accumulatedAngle = 0f
                        var detectedIdx = -1
                        for (i in sliceAngles.indices) {
                            val nextAngle = accumulatedAngle + sliceAngles[i]
                            if (angleDeg >= accumulatedAngle && angleDeg < nextAngle) {
                                detectedIdx = i
                                break
                            }
                            accumulatedAngle = nextAngle
                        }
                        if (detectedIdx != -1) {
                            onSliceSelected(if (selectedSliceIndex == detectedIdx) -1 else detectedIdx)
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                var startAngle = 0f
                sliceAngles.forEachIndexed { index, angle ->
                    val isSelected = index == selectedSliceIndex
                    val strokeWidth = if (isSelected) 30f else 22f
                    val padAngle = 2f // padding gap between slices

                    drawArc(
                        color = getChartColor(index),
                        startAngle = startAngle + padAngle / 2f,
                        sweepAngle = (angle - padAngle).coerceAtLeast(0.1f),
                        useCenter = false,
                        topLeft = Offset(20f, 20f),
                        size = Size(size.width - 40f, size.height - 40f),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    startAngle += angle
                }
            }

            // Central Summary info inside Donut hole
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = totalSum.toInt().toString(),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DisperindagGreenPrimary
                )
                Text(
                    text = "Total Unit",
                    fontSize = 10.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Right side: Vertical Scrollable Legends
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            dataMap.keys.toList().forEachIndexed { index, label ->
                val isSelected = index == selectedSliceIndex
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) Color.LightGray.copy(alpha = 0.2f) else Color.Transparent)
                        .clickable { onSliceSelected(if (isSelected) -1 else index) }
                        .padding(vertical = 4.dp, horizontal = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(getChartColor(index))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) DisperindagGreenPrimary else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun InteractiveHistogram(
    dataMap: Map<String, Int>,
    selectedLabel: String?,
    onBarSelected: (String) -> Unit
) {
    val maxVal = (dataMap.values.maxOrNull() ?: 1).coerceAtLeast(1)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        dataMap.forEach { (label, count) ->
            val fraction = count.toFloat() / maxVal.toFloat()
            val isSelected = label == selectedLabel
            val barColor = if (isSelected) DisperindagAccentGold else DisperindagGreenPrimary

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onBarSelected(label) }
                    .padding(horizontal = 2.dp)
            ) {
                Text(
                    text = count.toString(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = barColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .fillMaxHeight(fraction.coerceAtLeast(0.08f))
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(barColor)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = label.substringBefore(" "),
                    fontSize = 9.5.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    color = if (isSelected) DisperindagGreenPrimary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun ExperienceProgressRow(
    label: String,
    count: Int,
    total: Int,
    color: Color
) {
    val pct = if (total > 0) count.toFloat() / total.toFloat() else 0f
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(text = "$count Pedagang (${(pct * 100).toInt()}%)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { pct },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
            color = color,
            trackColor = Color.LightGray.copy(alpha = 0.2f)
        )
    }
}

@Composable
fun InsightItem(title: String, desc: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.LightGray.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(DisperindagAccentGold)
            )
            Text(
                text = title,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = DisperindagGreenPrimary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = desc,
            fontSize = 10.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

fun getChartColor(index: Int): Color {
    val colors = listOf(
        Color(0xFF2E7D32), // Green
        Color(0xFF1565C0), // Blue
        Color(0xFFEF6C00), // Orange
        Color(0xFF6A1B9A), // Purple
        Color(0xFFC62828), // Red
        Color(0xFF00838F), // Cyan
        Color(0xFFAD1457), // Pink
        Color(0xFF00695C), // Teal
        Color(0xFF283593)  // Indigo
    )
    return colors[index % colors.size]
}
