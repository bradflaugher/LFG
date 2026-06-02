with open('android/src/app/src/main/java/com/bradflaugher/lfe/ui/common/chat/ChatPanel.kt', 'r') as f:
    content = f.read()

old_column = """        Column(
          modifier =
            Modifier.fillMaxSize()
              .nestedScroll(nestedScrollConnection)
              .verticalScroll(state = listState)
              .semantics { contentDescription = cdChatPanel },
          verticalArrangement = Arrangement.Top,
        ) {
          messages.forEachIndexed { index, message ->"""

new_column = """        LazyColumn(
          state = listState,
          modifier =
            Modifier.fillMaxSize()
              .nestedScroll(nestedScrollConnection)
              .semantics { contentDescription = cdChatPanel },
          verticalArrangement = Arrangement.Top,
        ) {
          itemsIndexed(messages) { index, message ->"""

content = content.replace(old_column, new_column)

old_spacer = """          // The spacer at the bottom to push the content up so that the last user message will be
          // positioned at the top edge of the view when the list is scrolled to the bottom.
          //
          // See how `dynamicBottomPadding` is calculated above.
          Spacer(modifier = Modifier.height(dynamicBottomPadding).fillMaxWidth())
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.padding(vertical = 4.dp))"""

new_spacer = """          // The spacer at the bottom to push the content up so that the last user message will be
          // positioned at the top edge of the view when the list is scrolled to the bottom.
          //
          // See how `dynamicBottomPadding` is calculated above.
          item {
            Spacer(modifier = Modifier.height(dynamicBottomPadding).fillMaxWidth())
          }
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.padding(vertical = 4.dp))"""

content = content.replace(old_spacer, new_spacer)

with open('android/src/app/src/main/java/com/bradflaugher/lfe/ui/common/chat/ChatPanel.kt', 'w') as f:
    f.write(content)
