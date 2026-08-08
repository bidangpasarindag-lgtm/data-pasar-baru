import re

with open('app/src/main/java/com/example/ui/screens/DataDetailScreen.kt', 'r') as f:
    content = f.read()
    
# check if import ProgressDialog exists
if 'import com.example.ui.components.ProgressDialog' not in content:
    content = content.replace('import androidx.compose.ui.unit.sp', 'import androidx.compose.ui.unit.sp\nimport com.example.ui.components.ProgressDialog')

dialog_block = """
    ProgressDialog(
        showDialog = isGeneratingPdf,
        title = "Membuat PDF",
        message = "Sedang membuat kartu bukti pendataan dan merender dokumen..."
    )

    if (showDeleteDialog) {
"""
content = content.replace('if (showDeleteDialog) {', dialog_block.strip() + '\n\n    if (showDeleteDialog) {')
with open('app/src/main/java/com/example/ui/screens/DataDetailScreen.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/example/ui/screens/DataListScreen.kt', 'r') as f:
    content = f.read()
    
if 'import com.example.ui.components.ProgressDialog' not in content:
    content = content.replace('import androidx.compose.ui.unit.sp', 'import androidx.compose.ui.unit.sp\nimport com.example.ui.components.ProgressDialog')

dialog_block = """
    ProgressDialog(
        showDialog = isGeneratingPdf,
        title = "Membuat PDF",
        message = "Sedang membuat kartu bukti pendataan dan merender dokumen..."
    )
"""
# insert at end of Scaffold
content = content.replace('if (isSyncingActivities) {', dialog_block.strip() + '\n\n    if (isSyncingActivities) {')
with open('app/src/main/java/com/example/ui/screens/DataListScreen.kt', 'w') as f:
    f.write(content)

