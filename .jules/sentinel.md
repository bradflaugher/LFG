## 2023-10-27 - Path Traversal Bypass in WebView Asset Loader
**Vulnerability:** Path traversal bypass in `GalleryWebView.kt`. The path validation check used `!localFile.canonicalPath.startsWith(context.filesDir.canonicalPath)` which only validates prefixes. This would allow an attacker to bypass the directory restriction by supplying paths like `../files_backup` if the intended base directory is `files`.
**Learning:** Checking string prefix is insufficient for validating canonical paths due to sibling directories sharing the same prefix.
**Prevention:** Always append a trailing `File.separator` to the allowed base directory's canonical path before using `startsWith()`, or use proper standard library path resolution methods.
