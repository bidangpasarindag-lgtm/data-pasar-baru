import re

with open('app/src/main/java/com/example/ui/viewmodel/PedagangViewModel.kt', 'r') as f:
    content = f.read()

# Add isSaving state
state_block = """    private val _isSaving = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isSaving = _isSaving.kotlinx.coroutines.flow.asStateFlow()

    // Sync & UI Status"""
content = content.replace('// Sync & UI Status', state_block)

# Remove the `.kotlinx.coroutines.flow` because we already have import for them, Wait, MutableStateFlow is imported. 
# Let's fix the imports if needed. Actually `MutableStateFlow` and `asStateFlow` are already there.
content = content.replace('kotlinx.coroutines.flow.MutableStateFlow', 'MutableStateFlow')
content = content.replace('kotlinx.coroutines.flow.asStateFlow', 'asStateFlow')

# Inside savePedagang
save_start = '''        viewModelScope.launch {
            val pedagang = Pedagang('''
new_save_start = '''        viewModelScope.launch {
            _isSaving.value = true
            val pedagang = Pedagang('''
content = content.replace(save_start, new_save_start)

save_end = '''            }

            onSuccess()
        }'''
new_save_end = '''            }

            if (_selectedPedagang.value?.id == pedagang.id) {
                _selectedPedagang.value = pedagang
            }

            _isSaving.value = false
            onSuccess()
        }'''
content = content.replace(save_end, new_save_end)

with open('app/src/main/java/com/example/ui/viewmodel/PedagangViewModel.kt', 'w') as f:
    f.write(content)
