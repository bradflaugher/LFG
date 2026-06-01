## 2024-05-30 - Compose Recomposition Memory Leak
**Learning:** In Jetpack Compose `remember` blocks that process large lists (like chat histories), using operations that allocate new collections (like `.reversed()`) causes unnecessary heap allocations and GC pressure on every recomposition.
**Action:** Use `.asReversed()` instead of `.reversed()` when iterating backwards to create an O(1) view rather than an O(N) copy, especially in frequently recomposed UI layers.
