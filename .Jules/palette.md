## 2025-02-23 - Extract Content Descriptions to Resources
**Learning:** Hardcoded content descriptions in Android Compose files can negatively affect i18n localization of accessibility features. Even if English works, screen readers won't announce the string properly for non-English speakers.
**Action:** When finding hardcoded `contentDescription` properties in Android Jetpack Compose elements, extract them to `strings.xml` and use `stringResource(R.string.key_name)`.

## 2025-02-12 - Jetpack Compose Linting
**Learning:** `LocalContextGetResourceValueCall` lint error happens when calling `context.getString()` inside `remember` blocks in Jetpack Compose, as changes to Configuration objects won't trigger invalidation.
**Action:** Always extract string resources using `stringResource()` outside the block, then pass the resolved string as a dependency to the block or closure.
