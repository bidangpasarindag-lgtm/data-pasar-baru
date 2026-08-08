import re

with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'r') as f:
    text = f.read()

# Declare isSavingSettings at the top of SettingsScreen
state_decl = """    val currentConfig by AgencyConfigManager.config.collectAsState()
    var stateConfig by androidx.compose.runtime.remember(currentConfig) { androidx.compose.runtime.mutableStateOf(currentConfig) }
    var isSavingSettings by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }"""

text = re.sub(r'val currentConfig by AgencyConfigManager\.config\.collectAsState\(\)\s*var stateConfig by remember\(currentConfig\) \{ mutableStateOf\(currentConfig\) \}', state_decl, text)

# Remove all ProgressDialog instances that use isSavingSettings to clean up
text = re.sub(r'\s*ProgressDialog\(\s*showDialog = isSavingSettings,\s*title = "Menyimpan Pengaturan",\s*message = "Sedang menerapkan dan menyimpan konfigurasi aplikasi\.\.\."\s*\)\s*', '', text)

# Then add just one correct ProgressDialog inside SettingsScreen
# SettingsScreen starts with Scaffold
scaffold_pos = text.find('    Scaffold(')
if scaffold_pos != -1:
    dialog_insert = """
    ProgressDialog(
        showDialog = isSavingSettings,
        title = "Menyimpan Pengaturan",
        message = "Sedang menerapkan dan menyimpan konfigurasi aplikasi..."
    )
"""
    text = text[:scaffold_pos] + dialog_insert + text[scaffold_pos:]

with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'w') as f:
    f.write(text)

