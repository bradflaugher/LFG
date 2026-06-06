## 2026-06-06 - Improve Form Usability with Keyboard Actions
**Learning:** Adding IME actions (`ImeAction.Next` and `ImeAction.Done`) significantly improves the UX of forms, allowing users to jump between fields and submit forms directly from the keyboard without having to tap on the UI buttons.
**Action:** Next time when modifying or implementing Jetpack Compose forms with multiple text inputs, ensure `keyboardOptions(imeAction = ...)` is properly set for each text input, along with associated `KeyboardActions`.
