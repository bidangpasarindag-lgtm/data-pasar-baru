package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AddOptionDialog
import com.example.ui.components.ImagePickerField
import com.example.ui.theme.DisperindagAccentGold
import com.example.ui.theme.DisperindagGreenPrimary
import com.example.ui.viewmodel.FormState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormScreen(
    formState: FormState,
    jenisRuangOptions: List<String>,
    komoditiOptions: List<String>,
    statusOptions: List<String>,
    onUpdateForm: ((FormState) -> FormState) -> Unit,
    onAddOption: (category: String, newValue: String) -> Unit,
    onSaveClick: () -> Unit,
    onViewPhoto: (String) -> Unit,
    config: com.example.data.config.AgencyConfig = com.example.data.config.AgencyConfig()
) {
    var activeAddOptionCategory by remember { mutableStateOf<String?>(null) }

    val isEditing = formState.id != 0L

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Form Title Card
        Card(
            colors = CardDefaults.cardColors(containerColor = DisperindagGreenPrimary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isEditing) "EDIT DATA PEDAGANG" else "FORM PENDATAAN PEDAGANG BARU",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "${config.namaPasar} - Disperindag ${config.namaPemerintah}",
                    fontSize = 12.sp,
                    color = DisperindagAccentGold
                )
            }
        }

        // Section 1: System Auto Fields
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("INFORMASI PETUGAS & WAKTU", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DisperindagGreenPrimary)

                OutlinedTextField(
                    value = formState.timestamp,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Timestamp (Waktu Input)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = formState.emailAddress,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Email Address (Akun Logged In)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Section 2: Personal Info
        if (config.formShowNik || config.formShowAlamat || config.formShowHp || true) { // Nama is always shown
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("DATA DIRI PEDAGANG", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DisperindagGreenPrimary)

                    // NAMA PEDAGANG (Wajib)
                    OutlinedTextField(
                        value = formState.namaPedagang,
                        onValueChange = { newName -> onUpdateForm { it.copy(namaPedagang = newName, namaError = null) } },
                        label = { Text("NAMA PEDAGANG *") },
                        isError = formState.namaError != null,
                        supportingText = formState.namaError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("form_nama_pedagang_input")
                    )

                    // NIK (16 digit, Boleh Kosong)
                    if (config.formShowNik) {
                        OutlinedTextField(
                            value = formState.nik,
                            onValueChange = { newNik ->
                                if (newNik.length <= 16) {
                                    onUpdateForm { it.copy(nik = newNik, nikError = null) }
                                }
                            },
                            label = { Text("NIK (16 Digit, Boleh tidak diisi)") },
                            isError = formState.nikError != null,
                            supportingText = formState.nikError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("form_nik_input")
                        )
                    }

                    // ALAMAT (Boleh Kosong)
                    if (config.formShowAlamat) {
                        OutlinedTextField(
                            value = formState.alamat,
                            onValueChange = { newAlamat -> onUpdateForm { it.copy(alamat = newAlamat) } },
                            label = { Text("ALAMAT (Boleh tidak diisi)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("form_alamat_input")
                        )
                    }

                    // NOMOR HP (Mulai 0, 9-13 digit, Boleh Kosong)
                    if (config.formShowHp) {
                        OutlinedTextField(
                            value = formState.nomorHp,
                            onValueChange = { newHp ->
                                if (newHp.length <= 13) {
                                    onUpdateForm { it.copy(nomorHp = newHp, hpError = null) }
                                }
                            },
                            label = { Text("NOMOR HP (Mulai 0, 9-13 digit, Boleh tidak diisi)") },
                            isError = formState.hpError != null,
                            supportingText = formState.hpError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("form_nomor_hp_input")
                        )
                    }
                }
            }
        }

        // Section 3: Space & Business Info
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("INFORMASI RUANG DAGANG & USAHA", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DisperindagGreenPrimary)

                // JENIS RUANG DAGANG (Dropdown + Custom option)
                if (config.formShowJenisRuang) {
                    DynamicDropdownField(
                        label = "JENIS RUANG DAGANG *",
                        selectedValue = formState.jenisRuangDagang,
                        options = jenisRuangOptions,
                        isError = formState.jenisRuangError != null,
                        errorMessage = formState.jenisRuangError,
                        onSelect = { selected -> onUpdateForm { it.copy(jenisRuangDagang = selected, jenisRuangError = null) } },
                        onAddCustom = { activeAddOptionCategory = "JENIS_RUANG" },
                        testTag = "dropdown_jenis_ruang"
                    )
                }

                // NOMOR KIOS/LOS (Wajib)
                if (config.formShowNomorKios) {
                    OutlinedTextField(
                        value = formState.nomorKiosLos,
                        onValueChange = { newKios -> onUpdateForm { it.copy(nomorKiosLos = newKios, nomorKiosError = null) } },
                        label = { Text("NOMOR KIOS/LOS *") },
                        isError = formState.nomorKiosError != null,
                        supportingText = formState.nomorKiosError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("form_nomor_kios_input")
                    )
                }

                // KOMODITI / JENIS USAHA (Dropdown + Custom option)
                if (config.formShowKomoditi) {
                    DynamicDropdownField(
                        label = "KOMODITI / JENIS USAHA *",
                        selectedValue = formState.komoditi,
                        options = komoditiOptions,
                        isError = formState.komoditiError != null,
                        errorMessage = formState.komoditiError,
                        onSelect = { selected -> onUpdateForm { it.copy(komoditi = selected, komoditiError = null) } },
                        onAddCustom = { activeAddOptionCategory = "KOMODITI" },
                        testTag = "dropdown_komoditi"
                    )
                }

                // LAMA BERJUALAN (Angka dalam tahun, Wajib)
                if (config.formShowLamaBerjualan) {
                    OutlinedTextField(
                        value = formState.lamaBerjualanStr,
                        onValueChange = { newLama -> onUpdateForm { it.copy(lamaBerjualanStr = newLama, lamaBerjualanError = null) } },
                        label = { Text("LAMA BERJUALAN (Tahun) *") },
                        isError = formState.lamaBerjualanError != null,
                        supportingText = formState.lamaBerjualanError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("form_lama_berjualan_input")
                    )
                }

                // STATUS (Dropdown + Custom option)
                if (config.formShowStatus) {
                    DynamicDropdownField(
                        label = "STATUS *",
                        selectedValue = formState.status,
                        options = statusOptions,
                        isError = formState.statusError != null,
                        errorMessage = formState.statusError,
                        onSelect = { selected -> onUpdateForm { it.copy(status = selected, statusError = null) } },
                        onAddCustom = { activeAddOptionCategory = "STATUS" },
                        testTag = "dropdown_status"
                    )
                }

                // KETERANGAN (Boleh Kosong)
                if (config.formShowKeterangan) {
                    OutlinedTextField(
                        value = formState.keterangan,
                        onValueChange = { newKet -> onUpdateForm { it.copy(keterangan = newKet) } },
                        label = { Text("KETERANGAN (Boleh tidak diisi)") },
                        minLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("form_keterangan_input")
                    )
                }
            }
        }

        // Section 4: Photo Documents
        if (config.formShowFotoPedagang || config.formShowFotoKtp || config.formShowFotoSurat) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("DOKUMEN FOTO (Kamera / Gallery)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DisperindagGreenPrimary)

                    if (config.formShowFotoPedagang) {
                        ImagePickerField(
                            label = "FOTO PEDAGANG",
                            imageUri = formState.fotoPedagangUri,
                            onImageSelected = { uri -> onUpdateForm { it.copy(fotoPedagangUri = uri) } },
                            onViewImage = onViewPhoto,
                            testTagPrefix = "foto_pedagang"
                        )
                    }

                    if (config.formShowFotoKtp) {
                        ImagePickerField(
                            label = "FOTO KTP",
                            imageUri = formState.fotoKtpUri,
                            onImageSelected = { uri -> onUpdateForm { it.copy(fotoKtpUri = uri) } },
                            onViewImage = onViewPhoto,
                            testTagPrefix = "foto_ktp"
                        )
                    }

                    if (config.formShowFotoSurat) {
                        ImagePickerField(
                            label = "FOTO SURAT PERNYATAAN",
                            imageUri = formState.fotoSuratPernyataanUri,
                            onImageSelected = { uri -> onUpdateForm { it.copy(fotoSuratPernyataanUri = uri) } },
                            onViewImage = onViewPhoto,
                            testTagPrefix = "foto_surat"
                        )
                    }
                }
            }
        }

        // Save Action Button
        Button(
            onClick = onSaveClick,
            colors = ButtonDefaults.buttonColors(containerColor = DisperindagGreenPrimary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("save_form_button")
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isEditing) "SIMPAN PERUBAHAN PEDAGANG" else "SIMPAN DATA PEDAGANG",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }

        Spacer(modifier = Modifier.height(40.dp))
    }

    // Add Dynamic Option Dialog
    if (activeAddOptionCategory != null) {
        val title = when (activeAddOptionCategory) {
            "JENIS_RUANG" -> "Jenis Ruang Dagang"
            "KOMODITI" -> "Komoditi / Jenis Usaha"
            "STATUS" -> "Status Pedagang"
            else -> "Opsi"
        }
        AddOptionDialog(
            categoryTitle = title,
            onDismiss = { activeAddOptionCategory = null },
            onAddOption = { newValue ->
                onAddOption(activeAddOptionCategory!!, newValue)
                when (activeAddOptionCategory) {
                    "JENIS_RUANG" -> onUpdateForm { it.copy(jenisRuangDagang = newValue, jenisRuangError = null) }
                    "KOMODITI" -> onUpdateForm { it.copy(komoditi = newValue, komoditiError = null) }
                    "STATUS" -> onUpdateForm { it.copy(status = newValue, statusError = null) }
                }
            }
        )
    }
}

@Composable
fun DynamicDropdownField(
    label: String,
    selectedValue: String,
    options: List<String>,
    isError: Boolean,
    errorMessage: String?,
    onSelect: (String) -> Unit,
    onAddCustom: () -> Unit,
    testTag: String
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            TextButton(
                onClick = onAddCustom,
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text("+ Tambah Opsi Baru", fontSize = 11.sp, color = DisperindagGreenPrimary)
            }
        }

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = selectedValue,
                onValueChange = {},
                readOnly = true,
                isError = isError,
                supportingText = errorMessage?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                trailingIcon = {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(testTag)
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
                HorizontalDivider()
                DropdownMenuItem(
                    text = {
                        Text(
                            "+ Tambah Opsi Opsi Lain...",
                            color = DisperindagGreenPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    onClick = {
                        expanded = false
                        onAddCustom()
                    }
                )
            }
        }
    }
}
