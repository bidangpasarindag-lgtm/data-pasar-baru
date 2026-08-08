import re

with open('app/src/main/java/com/example/ui/screens/DataDetailScreen.kt', 'r') as f:
    content = f.read()

old_call = '''PdfExportUtils.generateAndOpenPdf(
                                context = context,
                                pedagangList = listOf(pedagang),
                                fileNamePrefix = "Kartu_${pedagang.namaPedagang}"
                            )'''

new_call = '''PdfExportUtils.generateAndOpenPdf(
                                context = context,
                                pedagangList = listOf(pedagang),
                                fileNamePrefix = "Kartu_${pedagang.namaPedagang}",
                                onStart = { isGeneratingPdf = true },
                                onComplete = { isGeneratingPdf = false }
                            )'''

content = content.replace(old_call, new_call)

with open('app/src/main/java/com/example/ui/screens/DataDetailScreen.kt', 'w') as f:
    f.write(content)
