## 2024-05-24 - [Path Traversal in WebView]
**Vulnerability:** WebView allowed arbitrary local file access via `allowFileAccess = true`.
**Learning:** Even when `WebViewAssetLoader` is used, if `allowFileAccess` is true, WebViews can be vulnerable to local file inclusion (LFI) and path traversal attacks.
**Prevention:** Always set `allowFileAccess = false` unless strictly necessary, and rely exclusively on `WebViewAssetLoader` for local asset serving.

## 2024-11-20 - [Explicitly Disable WebView File Access]
**Vulnerability:** WebView instances in `ArticleFetcher.kt` and `WebLoginDialog.kt` implicitly relied on default `allowFileAccess` settings.
**Learning:** Default behaviors vary across API levels. Relying on defaults leaves the application potentially vulnerable on older API levels to path traversal and local file inclusion (LFI) attacks.
**Prevention:** Always explicitly set `settings.allowFileAccess = false` on all WebViews for consistent security configuration across all supported API levels.
