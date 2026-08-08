import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

# Add imports
imports = """import com.example.ui.components.ProgressDialog
import com.example.ui.screens.FormScreen"""
content = content.replace('import com.example.ui.screens.FormScreen', imports)

# Add state collection
collect = """    val formState by viewModel.formState.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()"""
content = content.replace('val formState by viewModel.formState.collectAsState()', collect)

# Add ProgressDialog in MainActivity
dialog = """        }
    }

    ProgressDialog(
        showDialog = isSaving,
        title = "Menyimpan Data",
        message = "Sedang menyimpan dan menyinkronkan data pedagang ke Google Spreadsheet..."
    )

    if (isFormVisible) {"""
content = content.replace('        }\n    }\n\n    if (isFormVisible) {', dialog)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
