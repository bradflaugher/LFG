## 2026-06-10 - [Path Traversal in GalleryWebView Custom URL Interception]
**Vulnerability:** Path traversal possible when intercepted URLs are converted to local file paths without checking if they escape the intended base directory.
**Learning:** Custom logic intended to verify file existence before passing to `WebViewAssetLoader` inadvertently allowed checking for files outside the intended directory via `../` sequences, bypassing `WebViewAssetLoader`'s own internal safety checks.
**Prevention:** Always verify that `file.canonicalPath` starts with the base directory's `canonicalPath` when constructing paths from untrusted input.
