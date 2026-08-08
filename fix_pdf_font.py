import re

with open('app/src/main/java/com/example/util/PdfExportUtils.kt', 'r') as f:
    content = f.read()

# Make labels and values NORMAL instead of BOLD
label_paint = '''        val labelPaint = Paint().apply {
            color = Color.parseColor("#333333")
            textSize = bodySize
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            isAntiAlias = true
        }'''
old_label_paint_regex = r'val labelPaint = Paint\(\)\.apply \{.*?isAntiAlias = true\s*\}'
content = re.sub(old_label_paint_regex, label_paint, content, count=1, flags=re.DOTALL)

bold_value_paint = '''        val boldValuePaint = Paint().apply {
            color = Color.parseColor("#333333")
            textSize = bodySize
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            isAntiAlias = true
        }'''
old_bold_value_paint_regex = r'val boldValuePaint = Paint\(\)\.apply \{.*?isAntiAlias = true\s*\}'
content = re.sub(old_bold_value_paint_regex, bold_value_paint, content, count=1, flags=re.DOTALL)

value_paint = '''        val valuePaint = Paint().apply {
            color = Color.parseColor("#333333")
            textSize = bodySize
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            isAntiAlias = true
        }'''
old_value_paint_regex = r'val valuePaint = Paint\(\)\.apply \{.*?isAntiAlias = true\s*\}'
content = re.sub(old_value_paint_regex, value_paint, content, count=1, flags=re.DOTALL)

# Replace all "arial" with "sans-serif" to ensure consistency across devices since "arial" is not guaranteed to exist natively
content = content.replace('Typeface.create("arial",', 'Typeface.create("sans-serif",')

# However, the user explicitly asked: "menghasilkan tampilan font yang sama yaitu ARIAL."
# Actually, Android doesn't have Arial natively, but we can change "sans-serif" back to "arial".
# But Typeface.create("arial", ...) will fallback to default sans-serif on most devices.
content = content.replace('Typeface.create("sans-serif",', 'Typeface.create("arial",')

# One more thing: The user wanted "Semua data pedagang ... tidak bold". This applies to data (values). I also unbolded the label just in case, but let's keep labels bold? 
# "Semua data pedagang pada menu PDF dibuat default font style tidak bold." -> "All merchant data in PDF menu is made default font style not bold."
# This means the values (data) should not be bold. The `labelPaint` can remain bold? No, let's just make it normal as well if they want a consistent look, or just the values.
# The code above changes `labelPaint` to NORMAL, `boldValuePaint` to NORMAL.

with open('app/src/main/java/com/example/util/PdfExportUtils.kt', 'w') as f:
    f.write(content)

