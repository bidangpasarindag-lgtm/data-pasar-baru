import re

with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'r') as f:
    content = f.read()

# Add ProgressDialog import if missing
if 'import com.example.ui.components.ProgressDialog' not in content:
    content = content.replace('import androidx.compose.ui.unit.sp', 'import androidx.compose.ui.unit.sp\nimport com.example.ui.components.ProgressDialog')

# Add delay import
if 'import kotlinx.coroutines.delay' not in content:
    content = content.replace('import kotlinx.coroutines.launch', 'import kotlinx.coroutines.launch\nimport kotlinx.coroutines.delay')

state = """    val currentConfig by AgencyConfigManager.config.collectAsState()
    var stateConfig by remember(currentConfig) { mutableStateOf(currentConfig) }
    var isSavingSettings by remember { mutableStateOf(false) }"""
content = content.replace('    val currentConfig by AgencyConfigManager.config.collectAsState()\n    var stateConfig by remember(currentConfig) { mutableStateOf(currentConfig) }', state)

dialog = """        }
    }

    ProgressDialog(
        showDialog = isSavingSettings,
        title = "Menyimpan Pengaturan",
        message = "Sedang menerapkan dan menyimpan konfigurasi aplikasi..."
    )
}"""
content = content.replace('        }\n    }\n}', dialog)

save_btn = """                        onClick = {
                            scope.launch {
                                isSavingSettings = true
                                kotlinx.coroutines.delay(800) // Visual feedback
                                AgencyConfigManager.updateConfig(context, stateConfig)
                                isSavingSettings = false
                                Toast.makeText(context, "✓ Semua pengaturan berhasil disimpan!", Toast.LENGTH_SHORT).show()
                                onSettingsSaved()
                            }
                        },"""
old_save_btn_regex = r'onClick = \{\s*AgencyConfigManager.updateConfig.*?onSettingsSaved\(\)\s*\},'
content = re.sub(old_save_btn_regex, save_btn.strip(), content, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'w') as f:
    f.write(content)
