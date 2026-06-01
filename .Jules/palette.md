## 2025-02-23 - Extract Content Descriptions to Resources
**Learning:** Hardcoded content descriptions in Android Compose files can negatively affect i18n localization of accessibility features. Even if English works, screen readers won't announce the string properly for non-English speakers.
**Action:** When finding hardcoded `contentDescription` properties in Android Jetpack Compose elements, extract them to `strings.xml` and use `stringResource(R.string.key_name)`.
