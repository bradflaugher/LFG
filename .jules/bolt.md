## 2024-05-18 - Jetpack Compose LazyColumn Keys
**Learning:** In Jetpack Compose, using the same variable (like `it.name`) for the `key` parameter in multiple `items()` blocks within the *same* `LazyColumn` will cause a fatal runtime crash (`java.lang.IllegalArgumentException: Key X was already used`). Keys must be unique across the **entire** `LazyColumn`.
**Action:** When mapping items in a single `LazyColumn` that contains multiple lists (like `builtInModels` and `importedModels`), always namespace the keys (e.g., `key = { "builtin_${it.name}" }` and `key = { "imported_${it.name}" }`) to prevent collision and ensure uniqueness.

## 2024-06-14 - Jetpack Compose LazyColumn Keys for Filterable Lists
**Learning:** For `LazyColumn` lists that display dynamically filtered content (such as a search box that changes a `filteredSkills` list state), Jetpack Compose by default tracks list items by index. When filtering reduces the list size, items shift positions, causing Compose to unnecessarily recompose remaining items.
**Action:** Always add a stable unique `key` parameter (like `key = { it.skillUrl }`) to the `items()` block for lists that can be filtered or sorted. This provides structural identity, enabling Compose to just animate or re-order items instead of deeply recomposing them.
