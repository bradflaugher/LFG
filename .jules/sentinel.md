## 2024-05-24 - [Path Traversal in WebView]
**Vulnerability:** WebView allowed arbitrary local file access via `allowFileAccess = true`.
**Learning:** Even when `WebViewAssetLoader` is used, if `allowFileAccess` is true, WebViews can be vulnerable to local file inclusion (LFI) and path traversal attacks.
**Prevention:** Always set `allowFileAccess = false` unless strictly necessary, and rely exclusively on `WebViewAssetLoader` for local asset serving.

## 2026-06-02 - [Path Traversal in Multiple WebViews]
**Vulnerability:** Additional WebViews in WebLoginDialog.kt and ArticleFetcher.kt allowed arbitrary local file access because `allowFileAccess` defaults to true on older API levels or was not explicitly disabled.
**Learning:** The default behavior of `allowFileAccess` can lead to path traversal vulnerabilities if an attacker can control the loaded URL or injected content in any WebView in the app.
**Prevention:** Explicitly set `settings.allowFileAccess = false` on all instantiated WebViews across the codebase, not just the primary ones.
