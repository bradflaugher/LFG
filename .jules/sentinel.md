## 2025-02-28 - Local File Access enabled in WebViews
**Vulnerability:** Path Traversal / Unauthorized File Access in WebView
**Learning:** `allowFileAccess = true` combined with `javaScriptEnabled = true` can allow a malicious site loaded in the webview to read arbitrary local files from the app's directory.
**Prevention:** `WebViewAssetLoader` is the recommended way to load local assets without needing `allowFileAccess = true`. `allowFileAccess` should be set to `false`.
