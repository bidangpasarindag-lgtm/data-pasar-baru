import re

with open('app/src/main/java/com/example/ui/screens/DataListScreen.kt', 'r') as f:
    content = f.read()

content = content.replace('''PdfExportUtils.generateAndOpenPdf(
                            context = context,
                            pedagangList = filteredList,
                            fileNamePrefix = "Kartu_Bukti_Pendataan_Pedagang"
                        )''', '''PdfExportUtils.generateAndOpenPdf(
                            context = context,
                            pedagangList = filteredList,
                            fileNamePrefix = "Kartu_Bukti_Pendataan_Pedagang",
                            onStart = { isGeneratingPdf = true },
                            onComplete = { isGeneratingPdf = false }
                        )''')

content = content.replace('''PdfExportUtils.generateAndOpenPdf(
                                context = cardContext,
                                pedagangList = listOf(pedagang),
                                fileNamePrefix = "Kartu_${pedagang.namaPedagang}"
                            )''', '''PdfExportUtils.generateAndOpenPdf(
                                context = cardContext,
                                pedagangList = listOf(pedagang),
                                fileNamePrefix = "Kartu_${pedagang.namaPedagang}",
                                onStart = { isGeneratingPdf = true },
                                onComplete = { isGeneratingPdf = false }
                            )''')

with open('app/src/main/java/com/example/ui/screens/DataListScreen.kt', 'w') as f:
    f.write(content)
