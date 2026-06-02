with open('android/src/app/src/main/java/com/bradflaugher/lfe/ui/common/chat/ChatPanel.kt', 'r') as f:
    content = f.read()

old_func = """private suspend fun scrollToBottom(
  listState: ScrollState,
  animate: Boolean = false,
  animationDurationMs: Int = SCROLL_ANIMATION_DURATION_MS,
) {
  if (animate) {
    listState.animateScrollTo(
      listState.maxValue,
      animationSpec = tween(durationMillis = animationDurationMs, easing = FastOutSlowInEasing),
    )
  } else {
    listState.scrollTo(listState.maxValue)
  }
}"""

new_func = """private suspend fun scrollToBottom(
  listState: LazyListState,
  animate: Boolean = false,
  animationDurationMs: Int = SCROLL_ANIMATION_DURATION_MS,
) {
  if (listState.layoutInfo.totalItemsCount > 0) {
    if (animate) {
      listState.animateScrollToItem(listState.layoutInfo.totalItemsCount - 1)
    } else {
      listState.scrollToItem(listState.layoutInfo.totalItemsCount - 1)
    }
  }
}"""

content = content.replace(old_func, new_func)

with open('android/src/app/src/main/java/com/bradflaugher/lfe/ui/common/chat/ChatPanel.kt', 'w') as f:
    f.write(content)
