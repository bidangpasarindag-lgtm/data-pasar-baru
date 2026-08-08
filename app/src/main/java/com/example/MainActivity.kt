package com.example

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.HeaderBar
import com.example.ui.components.LightboxDialog
import com.example.ui.screens.*
import com.example.ui.components.ProgressDialog
import com.example.ui.theme.DisperindagGreenPrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.GroupByMode
import com.example.ui.viewmodel.PedagangViewModel

import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.History
import com.example.data.config.AgencyConfigManager
import com.example.ui.components.QrScannerDialog
import com.example.util.QrCodeUtils

enum class MainTab {
    LIST, GROUPED, CHARTS, ACTIVITY, SETTINGS
}

class MainActivity : ComponentActivity() {

    private val viewModel: PedagangViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AgencyConfigManager.init(this)
        com.example.data.auth.UserManager.init(this)

        setContent {
            MyApplicationTheme {
                MainAppContent(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(viewModel: PedagangViewModel) {
    val context = LocalContext.current

    val isLoggedIn by com.example.data.auth.UserManager.isLoggedIn.collectAsStateWithLifecycle()

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            viewModel.syncWithSpreadsheet()
        }
    }

    if (!isLoggedIn) {
        LoginScreen(
            onLoginSuccess = { email, displayName ->
                com.example.data.auth.UserManager.loginWithUser(context, com.example.data.auth.GoogleUser(email, displayName))
            }
        )
        return
    }

    var activeTab by remember { mutableStateOf(MainTab.LIST) }
    var showQrScanner by remember { mutableStateOf(false) }
    var invalidQrCodeText by remember { mutableStateOf<String?>(null) }
    var isFormVisible by remember { mutableStateOf(false) }

    val pedagangList by viewModel.pedagangList.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val groupByMode by viewModel.groupByMode.collectAsStateWithLifecycle()
    val selectedPedagang by viewModel.selectedPedagang.collectAsStateWithLifecycle()
    val previewImageUrl by viewModel.previewImageUrl.collectAsStateWithLifecycle()
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val lastSyncTime by viewModel.lastSyncTime.collectAsStateWithLifecycle()
    val uiMessage by viewModel.uiMessage.collectAsStateWithLifecycle()

    val userActivities by viewModel.userActivities.collectAsStateWithLifecycle()
    val isSyncingActivities by viewModel.isSyncingActivities.collectAsStateWithLifecycle()

    val jenisRuangOptions by viewModel.jenisRuangOptions.collectAsStateWithLifecycle()
    val komoditiOptions by viewModel.komoditiOptions.collectAsStateWithLifecycle()
    val statusOptions by viewModel.statusOptions.collectAsStateWithLifecycle()

    val syncProgress by viewModel.syncProgress.collectAsStateWithLifecycle()
    val syncProcessName by viewModel.syncProcessName.collectAsStateWithLifecycle()
    val syncEstimatedTime by viewModel.syncEstimatedTime.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler(enabled = isFormVisible || selectedPedagang != null || activeTab != MainTab.LIST) {
        if (isFormVisible) {
            isFormVisible = false
        } else if (selectedPedagang != null) {
            viewModel.selectPedagang(null)
        } else if (activeTab != MainTab.LIST) {
            activeTab = MainTab.LIST
        }
    }

    LaunchedEffect(uiMessage) {
        uiMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg.message)
            viewModel.clearUiMessage()
        }
    }

    ProgressDialog(
        showDialog = isSaving || isSyncing || isSyncingActivities,
        title = when {
            isSaving -> "Menyimpan Data"
            isSyncing -> "Sinkronisasi Data"
            isSyncingActivities -> "Sinkronisasi Aktivitas"
            else -> "Memuat"
        },
        message = when {
            isSaving -> "Sedang menyimpan dan menyinkronkan data pedagang ke Google Spreadsheet..."
            isSyncing -> "Mengambil data terbaru dari Google Spreadsheet..."
            isSyncingActivities -> "Mengambil log aktivitas terbaru..."
            else -> "Mohon tunggu sebentar..."
        },
        progress = syncProgress,
        processName = syncProcessName,
        estimatedTime = syncEstimatedTime
    )

    if (isFormVisible) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (formState.id != 0L) "Edit Data Pedagang" else "Tambah Pedagang Baru",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { isFormVisible = false },
                            modifier = Modifier.testTag("form_back_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Batal")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            modifier = Modifier.fillMaxSize()
        ) { formPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(formPadding)
            ) {
                val currentConfig by AgencyConfigManager.config.collectAsStateWithLifecycle()
                FormScreen(
                    formState = formState,
                    jenisRuangOptions = jenisRuangOptions,
                    komoditiOptions = komoditiOptions,
                    statusOptions = statusOptions,
                    onUpdateForm = { viewModel.updateForm(it) },
                    onAddOption = { category, value -> viewModel.addCustomOption(category, value) },
                    onSaveClick = {
                        viewModel.savePedagang {
                            isFormVisible = false
                        }
                    },
                    onViewPhoto = { url -> viewModel.setPreviewImageUrl(url) },
                    config = currentConfig
                )

                LightboxDialog(
                    imageUrl = previewImageUrl,
                    onDismiss = { viewModel.setPreviewImageUrl(null) }
                )
            }
        }
    } else {
        Scaffold(
            topBar = {
                HeaderBar(
                    onSyncClick = { viewModel.syncWithSpreadsheet() },
                    onSettingsClick = {
                        viewModel.selectPedagang(null)
                        activeTab = MainTab.SETTINGS
                    },
                    onScanQrClick = { showQrScanner = true },
                    isSyncing = isSyncing
                )
            },
            bottomBar = {
                if (selectedPedagang == null) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        NavigationBarItem(
                            selected = activeTab == MainTab.LIST,
                            onClick = { activeTab = MainTab.LIST },
                            icon = { Icon(Icons.Default.Storefront, contentDescription = "Pedagang") },
                            label = { Text("Pedagang", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.testTag("nav_tab_list")
                        )

                        NavigationBarItem(
                            selected = activeTab == MainTab.GROUPED,
                            onClick = { activeTab = MainTab.GROUPED },
                            icon = { Icon(Icons.Default.Folder, contentDescription = "Pengelompokan") },
                            label = { Text("Kelompok", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.testTag("nav_tab_grouped")
                        )

                        NavigationBarItem(
                            selected = activeTab == MainTab.CHARTS,
                            onClick = { activeTab = MainTab.CHARTS },
                            icon = { Icon(Icons.Default.BarChart, contentDescription = "Histogram") },
                            label = { Text("Chart", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.testTag("nav_tab_charts")
                        )

                        NavigationBarItem(
                            selected = activeTab == MainTab.ACTIVITY,
                            onClick = { activeTab = MainTab.ACTIVITY },
                            icon = { Icon(Icons.Default.History, contentDescription = "Aktivitas") },
                            label = { Text("Aktivitas", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.testTag("nav_tab_activity")
                        )

                        NavigationBarItem(
                            selected = activeTab == MainTab.SETTINGS,
                            onClick = { activeTab = MainTab.SETTINGS },
                            icon = { Icon(Icons.Default.Settings, contentDescription = "Pengaturan") },
                            label = { Text("Setting", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.testTag("nav_tab_settings")
                        )
                    }
                }
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (selectedPedagang != null) {
                    // Detail Screen view
                    DataDetailScreen(
                        pedagang = selectedPedagang!!,
                        onBackClick = { viewModel.selectPedagang(null) },
                        onEditClick = {
                            viewModel.initEditForm(selectedPedagang!!)
                            isFormVisible = true
                        },
                        onDeleteClick = {
                            val target = selectedPedagang!!
                            viewModel.deletePedagang(target)
                            viewModel.selectPedagang(null)
                        },
                        onViewPhoto = { url -> viewModel.setPreviewImageUrl(url) }
                    )
                } else {
                    when (activeTab) {
                        MainTab.LIST -> DataListScreen(
                            pedagangList = pedagangList,
                            searchQuery = searchQuery,
                            onSearchQueryChange = { viewModel.setSearchQuery(it) },
                            onPedagangClick = { viewModel.selectPedagang(it) },
                            onEditPedagang = {
                                viewModel.initEditForm(it)
                                isFormVisible = true
                            },
                            onDeletePedagang = {
                                viewModel.deletePedagang(it)
                            },
                            onAddNewClick = {
                                viewModel.initNewForm()
                                isFormVisible = true
                            },
                            onScanQrClick = { showQrScanner = true },
                            onViewPhoto = { url -> viewModel.setPreviewImageUrl(url) }
                        )

                        MainTab.GROUPED -> GroupedDataScreen(
                            pedagangList = pedagangList,
                            groupByMode = groupByMode,
                            onGroupByModeChange = { viewModel.setGroupByMode(it) },
                            onPedagangClick = { viewModel.selectPedagang(it) },
                            onEditPedagang = {
                                viewModel.initEditForm(it)
                                isFormVisible = true
                            },
                            onDeletePedagang = {
                                viewModel.deletePedagang(it)
                            },
                            onViewPhoto = { url -> viewModel.setPreviewImageUrl(url) }
                        )

                        MainTab.CHARTS -> ChartsScreen(
                            pedagangList = pedagangList
                        )

                        MainTab.ACTIVITY -> ActivityUserScreen(
                            activities = userActivities,
                            isSyncing = isSyncingActivities,
                            onRefresh = { viewModel.fetchUserActivities() }
                        )

                        MainTab.SETTINGS -> {
                            val allJenisRuangOptions by viewModel.allJenisRuangOptions.collectAsStateWithLifecycle()
                            val allKomoditiOptions by viewModel.allKomoditiOptions.collectAsStateWithLifecycle()
                            val allStatusOptions by viewModel.allStatusOptions.collectAsStateWithLifecycle()

                            SettingsScreen(
                                lastSyncTime = lastSyncTime,
                                isSyncing = isSyncing,
                                totalPedagangCount = pedagangList.size,
                                allJenisRuangOptions = allJenisRuangOptions,
                                allKomoditiOptions = allKomoditiOptions,
                                allStatusOptions = allStatusOptions,
                                onSyncClick = { viewModel.syncWithSpreadsheet() },
                                onRebuildDropdownClick = { viewModel.rebuildDropdownOptions() },
                                onToggleDropdownVisibility = { viewModel.toggleDropdownVisibility(it) },
                                onDeleteDropdownOption = { viewModel.deleteDropdownOption(it) },
                                onAddDropdownOption = { category, value -> viewModel.addCustomOption(category, value) },
                                onExportCsvClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "Data Pedagang Pasar Waru Pamekasan")
                                        putExtra(Intent.EXTRA_TEXT, "Data Pedagang Pasar Waru (${pedagangList.size} record) dari Disperindag Kabupaten Pamekasan")
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Bagikan / Export Data"))
                                },
                                onSettingsSaved = { activeTab = MainTab.LIST }
                            )
                        }
                    }
                }

                // Lightbox Modal for Photo viewing
                LightboxDialog(
                    imageUrl = previewImageUrl,
                    onDismiss = { viewModel.setPreviewImageUrl(null) }
                )
            }
        }
    }

    if (showQrScanner) {
        QrScannerDialog(
            onDismiss = { showQrScanner = false },
            onQrScanned = { qrContent ->
                showQrScanner = false
                val found = QrCodeUtils.findPedagangFromQrContent(qrContent, pedagangList)
                if (found != null) {
                    viewModel.selectPedagang(found)
                } else {
                    invalidQrCodeText = qrContent
                }
            }
        )
    }

    if (invalidQrCodeText != null) {
        AlertDialog(
            onDismissRequest = { invalidQrCodeText = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Data Tidak Ditemukan") },
            text = { Text("Hasil scan QR Code '$invalidQrCodeText' tidak cocok dengan data pedagang Pasar Waru.") },
            confirmButton = {
                TextButton(onClick = { invalidQrCodeText = null }) {
                    Text("OK")
                }
            }
        )
    }
}
