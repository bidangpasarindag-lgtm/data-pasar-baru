import re

with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'r') as f:
    content = f.read()

card_block = """
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Manajemen Data Form (Dropdown):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DisperindagGreenPrimary)
                                
                                Text("Tombol di bawah ini akan mengatur ulang opsi pilihan untuk JENIS RUANG, KOMODITI, dan STATUS ke setelan bawaan terbaru, dan menambahkan semua nilai unik dari data pedagang saat ini.", fontSize = 11.sp, color = Color.Gray)
                                
                                Button(
                                    onClick = { 
                                        viewModel.rebuildDropdownOptions() 
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Perbarui Opsi Dropdown")
                                }
                            }
                        }

                        Card(
"""

content = content.replace('                        Card(\n                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),\n                            shape = RoundedCornerShape(12.dp)\n                        ) {\n                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {\n                                Text("Manajemen File Konfigurasi (JSON Backup):",', card_block + '                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),\n                            shape = RoundedCornerShape(12.dp)\n                        ) {\n                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {\n                                Text("Manajemen File Konfigurasi (JSON Backup):",')

with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'w') as f:
    f.write(content)
