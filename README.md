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
| `summarize-article` | Fetch and summarize a news article. Works on any publicly readable page. |
| `translator` | Translate between languages. |

Add your own by dropping a `SKILL.md` directory under
`android/src/app/src/main/assets/skills/`. See **[docs/SKILLS.md](docs/SKILLS.md)**
for the how-to — text personas, JavaScript skills, native intents, and
hosting tips. The in-app **Skills → + → How to write skills** menu opens
the same guide on your phone.

## Models

Four Gemma 4 variants ship in the recommended list: **E2B** / **E4B** (stock, from
litert-community — multi-modal text + image + audio) and **E2B-Abliterated** /
**E4B-Abliterated** (uncensored, from
[DuoNeural](https://huggingface.co/DuoNeural/Gemma-4-Abliterated-LiteRT) — text-only).
All 32K context. Tap the gear icon to download.

Want something else? Grab any LiteRT-LM-compatible `.litertlm` or `.task` file from
HuggingFace (or convert one yourself with [LiteRT-LM](https://github.com/google-ai-edge/LiteRT-LM)),
copy or download it onto your device, and use **Import local model file** at the bottom of the
model manager.

## Install

Grab the latest APK from
[**releases/latest**](https://github.com/bradflaugher/LFE/releases/latest) directly on your device.

Open the downloaded APK file and follow your device's instructions to install it. If you need help, check out the official Google guide on [how to install apps from other sources on Android](https://support.google.com/googleplay/answer/14669046).

Android 12+ required. On first launch you'll be in the agent chat — tap the gear (top
right) to download a model.

## License

GPL-3.0-or-later. See [LICENSE](LICENSE).

Includes code adapted from
[Google AI Edge Gallery](https://github.com/google-ai-edge/gallery) (Apache License 2.0,
© 2025 Google LLC) — much credit to that project — under Apache 2.0 § 4. Built on
[LiteRT-LM](https://github.com/google-ai-edge/LiteRT-LM) and Mozilla
[Readability.js](https://github.com/mozilla/readability). Inspired by
[LFG](https://github.com/bradflaugher/LFG), a terminal sibling with the same skills layer.
