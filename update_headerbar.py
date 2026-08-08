import re

with open('app/src/main/java/com/example/ui/components/HeaderBar.kt', 'r') as f:
    content = f.read()

# Change spacing
content = content.replace('horizontalArrangement = Arrangement.spacedBy(8.dp)', 'horizontalArrangement = Arrangement.spacedBy(12.dp)')

# Remove settings button
settings_button_regex = r'IconButton\(\s*onClick = onSettingsClick,.*?Icon\(\s*imageVector = Icons\.Default\.Settings,.*?\}\s*\)\s*\}'
content = re.sub(settings_button_regex, '', content, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/components/HeaderBar.kt', 'w') as f:
    f.write(content)
