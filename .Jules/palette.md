## 2025-02-12 - Jetpack Compose Linting
**Learning:** `LocalContextGetResourceValueCall` lint error happens when calling `context.getString()` inside `remember` blocks in Jetpack Compose, as changes to Configuration objects won't trigger invalidation.
**Action:** Always extract string resources using `stringResource()` outside the block, then pass the resolved string as a dependency to the block or closure.
