# Third-Party Notices

LFG is [Apache License 2.0](LICENSE) software. It is a fork of Google AI Edge
Gallery, substantially modified by Brad Flaugher.

## Google AI Edge Gallery

- **Copyright:** 2025 Google LLC
- **License:** Apache License 2.0
- **Source:** https://github.com/google-ai-edge/gallery

Most of the Android app scaffold (UI, model management, chat infrastructure,
skill runtime) derives from this project.

## Mozilla Readability.js

- **Copyright:** Mozilla Foundation and contributors
- **License:** Apache License 2.0
- **Bundled at:** `android/src/app/src/main/assets/js/readability.js`
- **Source:** https://github.com/mozilla/readability

Used by the Article Fetcher skill to extract article text from web pages.

## Gallery skill HTML (mood-tracker, translator)

Some skill `scripts/*.html` and `assets/*.html` files retain their original
Google Apache 2.0 headers from AI Edge Gallery.

## Other dependencies

Runtime libraries (LiteRT-LM, Jetpack Compose, Hilt, etc.) are declared in
`android/src/gradle/libs.versions.toml` and carry their own licenses via Gradle
dependencies. Consult each package for terms.