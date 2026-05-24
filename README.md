# LFE

A low-feature, on-device AI agent for Android. Skills-first, fully offline, minimalistic.

## What it does

LFE runs a small LLM on your phone and lets it call **skills** — bundled JavaScript
helpers that do focused jobs like summarizing articles, generating QR codes, tracking
mood, or translating text. The model never talks to a server.

Two screens, that's it: the agent chat, and a model manager behind a gear icon.

## Skills bundled today

| Skill | What it does |
|---|---|
| `budget-tracker` | Log expenses and income, see a running summary. |
| `calculate-hash` | SHA-1 / 256 / 384 / 512 of any text. |
| `interactive-map` | Drop a location pin on a Leaflet map. |
| `mood-tracker` | Daily mood log with history. |
| `password-generator` | Secure random passwords. |
| `qr-code` | Make a QR code from any URL (works offline). |
| `query-wikipedia` | Pull a summary on any topic. |
| `recommend-articles` | Scan a news homepage and pick stories that match your stored interests. Defaults to NYT; configurable. |
| `send-email` | Hand off a draft to your mail app. |
| `summarize-article` | Fetch and summarize a news article. Works on paywalled sites once you've signed in via *Browser Session*. |
| `translator` | Translate between languages. |

Add your own by dropping a `SKILL.md` directory under
`android/src/app/src/main/assets/skills/` — the [agentskills.io](https://agentskills.io/specification)
spec is the schema.

## Models

Every LiteRT-LM model from the Edge AI allowlist works out of the box: Gemma 4 E2B/E4B,
Gemma 3n E2B/E4B, Gemma 3 1B, Qwen 2.5 1.5B, DeepSeek-R1-Distill-Qwen 1.5B. Tap the gear
icon → "Browse HF" to search HuggingFace for more LiteRT-LM-compatible models and
download directly to the phone.

For HuggingFace-gated models, set up an OAuth app at
<https://huggingface.co/settings/applications/new> with redirect URL
`com.bradflaugher.lfe://oauth`, then fill in `clientId` and `redirectUri` in
`common/ProjectConfig.kt`.

## Install

Grab the latest APK from
[**releases/latest**](https://github.com/bradflaugher/LFE/releases/latest) and sideload it.

```sh
adb install -r lfe-build-<date>-<sha>.apk
```

Android 12+ required. On first launch you'll be in the agent chat — tap the gear to
download a model.

## Build it yourself

JDK 21 is the only prereq.

```sh
cd android/src
./gradlew assembleDebug    # debug APK
./gradlew test             # unit tests
```

## License

GPL-3.0-or-later. See [LICENSE](LICENSE).

Includes code adapted from
[Google AI Edge Gallery](https://github.com/google-ai-edge/gallery) (Apache License 2.0,
© 2025 Google LLC) — much credit to that project — under Apache 2.0 § 4. Built on
[LiteRT-LM](https://github.com/google-ai-edge/LiteRT-LM) and Mozilla
[Readability.js](https://github.com/mozilla/readability). Inspired by
[LFG](https://github.com/bradflaugher/LFG), a terminal sibling with the same skills layer.
