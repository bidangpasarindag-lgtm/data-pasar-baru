import re

with open('app/src/main/java/com/example/data/local/DropdownDao.kt', 'r') as f:
    content = f.read()

content = content.replace('suspend fun deleteOption(category: String, value: String)', 'suspend fun deleteOption(category: String, value: String)\n\n    @Query("DELETE FROM dropdown_options")\n    suspend fun clearAll()')

with open('app/src/main/java/com/example/data/local/DropdownDao.kt', 'w') as f:
    f.write(content)
