## 2024-05-24 - [Path Traversal in WebView]
**Vulnerability:** WebView allowed arbitrary local file access via `allowFileAccess = true`.
**Learning:** Even when `WebViewAssetLoader` is used, if `allowFileAccess` is true, WebViews can be vulnerable to local file inclusion (LFI) and path traversal attacks.
**Prevention:** Always set `allowFileAccess = false` unless strictly necessary, and rely exclusively on `WebViewAssetLoader` for local asset serving.

## 2024-11-20 - [Explicitly Disable WebView File Access]
**Vulnerability:** WebView instances in `ArticleFetcher.kt` and `WebLoginDialog.kt` implicitly relied on default `allowFileAccess` settings.
**Learning:** Default behaviors vary across API levels. Relying on defaults leaves the application potentially vulnerable on older API levels to path traversal and local file inclusion (LFI) attacks.
**Prevention:** Always explicitly set `settings.allowFileAccess = false` on all WebViews for consistent security configuration across all supported API levels.

## 2026-06-08 - [WebView Content Provider Access Vulnerability]
**Vulnerability:** WebView allowed arbitrary local file access via content:// URIs when `allowContentAccess` was implicitly true.
**Learning:** Explicitly setting `allowFileAccess = false` is not sufficient; `allowContentAccess = false` must also be set to fully prevent LFI and path traversal via content providers.
**Prevention:** Always explicitly set `settings.allowContentAccess = false` alongside `settings.allowFileAccess = false` on all WebViews.
