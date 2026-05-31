## 2025-02-27 - WebView Local File Access Vulnerability
**Vulnerability:** Android WebView in `GalleryWebView.kt` was configured with `allowFileAccess = true`.
**Learning:** This exposes the application to path traversal and local file inclusion (LFI) attacks. Since `WebViewAssetLoader` is already in place to serve local assets securely, explicitly granting raw file access is unnecessary and dangerous.
**Prevention:** Always set `allowFileAccess = false` in WebViews unless absolutely necessary. Rely on `WebViewAssetLoader` for secure, restricted loading of local web resources.
