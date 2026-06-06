## 2024-05-24 - [Path Traversal in WebView]
**Vulnerability:** WebView allowed arbitrary local file access via `allowFileAccess = true`.
**Learning:** Even when `WebViewAssetLoader` is used, if `allowFileAccess` is true, WebViews can be vulnerable to local file inclusion (LFI) and path traversal attacks.
**Prevention:** Always set `allowFileAccess = false` unless strictly necessary, and rely exclusively on `WebViewAssetLoader` for local asset serving.

## 2024-11-20 - [Explicitly Disable WebView File Access]
**Vulnerability:** WebView instances in `ArticleFetcher.kt` and `WebLoginDialog.kt` implicitly relied on default `allowFileAccess` settings.
**Learning:** Default behaviors vary across API levels. Relying on defaults leaves the application potentially vulnerable on older API levels to path traversal and local file inclusion (LFI) attacks.
**Prevention:** Always explicitly set `settings.allowFileAccess = false` on all WebViews for consistent security configuration across all supported API levels.

## 2024-05-18 - Prevent Local File Inclusion (LFI) via WebViews
**Vulnerability:** Android WebViews allowed local file access and local content access by default or via configuration. `allowContentAccess` wasn't explicitly disabled, allowing arbitrary file reading via `content://` URIs if exposed to untrusted inputs.
**Learning:** Even if `allowFileAccess` is disabled, Android WebViews might still permit loading local files via content providers. For absolute security against path traversal and LFI in untrusted WebView contexts, both `allowFileAccess` and `allowContentAccess` must be explicitly set to `false`.
**Prevention:** Whenever configuring an Android WebView, always explicitly set both `settings.allowFileAccess = false` and `settings.allowContentAccess = false` unless loading local files is an explicitly required feature, in which case `WebViewAssetLoader` is the safer alternative.
