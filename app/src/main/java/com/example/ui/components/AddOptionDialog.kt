package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AddOptionDialog(
    categoryTitle: String,
    onDismiss: () -> Unit,
    onAddOption: (String) -> Unit
) {
    var textValue by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Tambah Opsi $categoryTitle",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Masukkan pilihan baru yang akan ditambahkan ke daftar dropdown:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = textValue,
                    onValueChange = {
                        textValue = it
                        if (errorText != null) errorText = null
                    },
                    label = { Text("Nama Opsi Baru") },
                    singleLine = true,
                    isError = errorText != null,
                    supportingText = errorText?.let { { Text(it) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_option_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (textValue.isBlank()) {
                        errorText = "Opsi tidak boleh kosong"
                    } else {
                        onAddOption(textValue.trim())
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("save_new_option_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Tambah")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_new_option_button")
            ) {
                Text("Batal")
            }
        }
    )
}
