import re

with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'r') as f:
    text = f.read()

# Fix "}Scaffold("
text = text.replace("}Scaffold(", "}\n\n    Scaffold(")

with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'w') as f:
    f.write(text)

