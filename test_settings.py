with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'r') as f:
    text = f.read()

lines = text.split('\n')
for i, line in enumerate(lines[:145]):
    print(f"{i+1}: {line}")
