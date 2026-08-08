import re

with open('app/src/main/java/com/example/ui/screens/DataListScreen.kt', 'r') as f:
    content = f.read()

# I know there's a floatingActionButton = {
content = content.replace('floatingActionButton = {', '''
    ProgressDialog(
        showDialog = isGeneratingPdf,
        title = "Membuat PDF",
        message = "Sedang membuat kartu bukti pendataan dan merender dokumen..."
    )

    ProgressDialog(
        showDialog = isSyncing,
        title = "Sinkronisasi Data",
        message = "Sedang mengambil data terbaru dari Google Spreadsheet..."
    )

    floatingActionButton = {''')

with open('app/src/main/java/com/example/ui/screens/DataListScreen.kt', 'w') as f:
    f.write(content)

