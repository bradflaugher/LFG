## 2024-05-30 - Compose Recomposition Memory Leak
**Learning:** In Jetpack Compose `remember` blocks that process large lists (like chat histories), using operations that allocate new collections (like `.reversed()`) causes unnecessary heap allocations and GC pressure on every recomposition.
**Action:** Use `.asReversed()` instead of `.reversed()` when iterating backwards to create an O(1) view rather than an O(N) copy, especially in frequently recomposed UI layers.

## 2024-05-24 - Optimizing ChatPanel List Rendering
**Learning:** Replaced an eagerly rendered `Column` with `verticalScroll` using a `LazyColumn` for displaying chat messages. The previous implementation degraded performance significantly when the chat history grew large, causing noticeable frame drops and unresponsiveness because every message was rendered immediately.
**Action:** When working with large lists of dynamically created components in Compose, always use `LazyColumn` or `LazyRow` to implement virtualization. Furthermore, state tied to elements within a `LazyColumn` must use `rememberSaveable` rather than `remember` to ensure state is not lost when elements scroll off-screen and are recreated.
