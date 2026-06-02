import re

with open('android/src/app/src/main/java/com/bradflaugher/lfe/ui/common/chat/ChatPanel.kt', 'r') as f:
    content = f.read()

content = content.replace('import androidx.compose.foundation.rememberScrollState\n', '')
content = content.replace('import androidx.compose.foundation.verticalScroll\n', '')

imports_to_add = """import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
"""

# Find a good place to add the new imports (e.g., after the other foundation imports)
content = re.sub(r'(import androidx.compose.foundation.layout.size\n)', r'\1' + imports_to_add, content)

content = content.replace('val listState = rememberScrollState()', 'val listState = rememberLazyListState()')

with open('android/src/app/src/main/java/com/bradflaugher/lfe/ui/common/chat/ChatPanel.kt', 'w') as f:
    f.write(content)
