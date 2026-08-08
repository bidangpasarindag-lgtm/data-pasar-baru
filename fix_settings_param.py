import re

with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'r') as f:
    content = f.read()

# Add to function signature
content = content.replace('onExportCsvClick: () -> Unit,', 'onExportCsvClick: () -> Unit,\n    onRebuildDropdownClick: () -> Unit,')

# Replace in button onClick
content = content.replace('viewModel.rebuildDropdownOptions()', 'onRebuildDropdownClick()')

with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'w') as f:
    f.write(content)
