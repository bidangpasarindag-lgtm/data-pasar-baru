import re

with open('app/src/main/java/com/example/ui/viewmodel/PedagangViewModel.kt', 'r') as f:
    content = f.read()

rebuild_func = """    fun rebuildDropdownOptions() {
        viewModelScope.launch {
            repository.rebuildDropdownOptions()
            _uiMessage.value = UiMessage("Opsi dropdown berhasil diperbarui sesuai data saat ini")
        }
    }

    fun addCustomOption"""

content = content.replace('fun addCustomOption', rebuild_func)

with open('app/src/main/java/com/example/ui/viewmodel/PedagangViewModel.kt', 'w') as f:
    f.write(content)
