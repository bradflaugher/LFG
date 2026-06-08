## 2025-02-23 - Extract Content Descriptions to Resources
**Learning:** Hardcoded content descriptions in Android Compose files can negatively affect i18n localization of accessibility features. Even if English works, screen readers won't announce the string properly for non-English speakers.
**Action:** When finding hardcoded `contentDescription` properties in Android Jetpack Compose elements, extract them to `strings.xml` and use `stringResource(R.string.key_name)`.

## 2025-02-12 - Jetpack Compose Linting
**Learning:** `LocalContextGetResourceValueCall` lint error happens when calling `context.getString()` inside `remember` blocks in Jetpack Compose, as changes to Configuration objects won't trigger invalidation.
**Action:** Always extract string resources using `stringResource()` outside the block, then pass the resolved string as a dependency to the block or closure.

## 2026-06-02 - Redundant Semantics Node
**Learning:** Some compose elements like `Row` or `Modifier.clickable` can have `onClickLabel` properties or custom semantics that apply to its children, making nested `contentDescription`s on components like `Icon` redundant. Setting `contentDescription = null` for icons wrapped in parents with accurate semantic labels reduces screen reader double-reads.
**Action:** Always check the parent composable elements for accessibility semantics before assigning an explicit string to a purely decorative or wrapped inner element.

## 2026-06-06 - Improve Form Usability with Keyboard Actions
**Learning:** Adding IME actions (`ImeAction.Next` and `ImeAction.Done`) significantly improves the UX of forms, allowing users to jump between fields and submit forms directly from the keyboard without having to tap on the UI buttons.
**Action:** Next time when modifying or implementing Jetpack Compose forms with multiple text inputs, ensure `keyboardOptions(imeAction = ...)` is properly set for each text input, along with associated `KeyboardActions`.
