import re

with open('app/src/main/java/com/example/ui/screens/DataListScreen.kt', 'r') as f:
    content = f.read()

# Add state to DataListScreen
state_block = """    var showSortSheet by remember { mutableStateOf(false) }
    var isGeneratingPdf by remember { mutableStateOf(false) }"""
content = content.replace('    var showSortSheet by remember { mutableStateOf(false) }', state_block)

# Add ProgressDialog before "if (showFilterSheet) {"
dialog_block = """
    ProgressDialog(
        showDialog = isGeneratingPdf,
        title = "Membuat PDF",
        message = "Sedang membuat kartu bukti pendataan dan merender dokumen..."
    )

    ProgressDialog(
        showDialog = false, // Sync is in main VM, so maybe not here
        title = "Sinkronisasi Data",
        message = "Sedang mengambil data terbaru dari Google Spreadsheet..."
    )

    if (showFilterSheet) {"""

content = content.replace('if (showFilterSheet) {', dialog_block.strip() + '\n\n    if (showFilterSheet) {')

with open('app/src/main/java/com/example/ui/screens/DataListScreen.kt', 'w') as f:
    f.write(content)
