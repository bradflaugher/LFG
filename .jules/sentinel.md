## 2026-06-10 - [Path Traversal in GalleryWebView Custom URL Interception]
**Vulnerability:** Path traversal possible when intercepted URLs are converted to local file paths without checking if they escape the intended base directory.
**Learning:** Custom logic intended to verify file existence before passing to `WebViewAssetLoader` inadvertently allowed checking for files outside the intended directory via `../` sequences, bypassing `WebViewAssetLoader`'s own internal safety checks.
**Prevention:** Always verify that `file.canonicalPath` starts with the base directory's `canonicalPath` when constructing paths from untrusted input.
## 2025-02-28 - Sibling Directory Path Traversal Vulnerability
**Vulnerability:** Path traversal vulnerability in `GalleryWebView.kt` where the security check `!localFile.canonicalPath.startsWith(context.filesDir.canonicalPath)` allowed access to sibling directories with the same prefix.
**Learning:** Checking `canonicalPath.startsWith()` without appending `File.separator` fails to prevent access to directories that share the same string prefix as the intended directory (e.g., `/data/.../files` and `/data/.../files_hacker`).
**Prevention:** Always append `File.separator` to the base path's canonical path when using `startsWith()` for path traversal checks, e.g., `localFile.canonicalPath.startsWith(baseDir.canonicalPath + File.separator)`.
