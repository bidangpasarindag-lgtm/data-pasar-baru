import re

with open('app/src/main/java/com/example/util/PdfExportUtils.kt', 'r') as f:
    content = f.read()

# Make the big box instead of separate boxes.
content = content.replace('canvas.drawRoundRect(photoLeft, photoTop, photoRight, photoBottom, 6f, 6f, photoBoxBg)', 'canvas.drawRoundRect(photoLeft, photoTop, photoRight, 325f, 6f, 6f, photoBoxBg)')
content = content.replace('canvas.drawRoundRect(photoLeft, photoTop, photoRight, photoBottom, 6f, 6f, photoBoxBorder)', 'canvas.drawRoundRect(photoLeft, photoTop, photoRight, 325f, 6f, 6f, photoBoxBorder)')

# Remove qrBoxBg and qrBoxBorder drawing
content = re.sub(r'val qrBoxBg = Paint\(\)\.apply \{.*?canvas\.drawRoundRect\(photoLeft, qrBoxTop, photoRight, qrBoxBottom, 6f, 6f, qrBoxBg\)', '', content, flags=re.DOTALL)
content = re.sub(r'val qrBoxBorder = Paint\(\)\.apply \{.*?canvas\.drawRoundRect\(photoLeft, qrBoxTop, photoRight, qrBoxBottom, 6f, 6f, qrBoxBorder\)', '', content, flags=re.DOTALL)

with open('app/src/main/java/com/example/util/PdfExportUtils.kt', 'w') as f:
    f.write(content)
