# LFE

A low-feature, edge-only AI agent for Android. Skills-first, on-device-only, minimalistic.

LFE is a standalone GPL-3.0 app that borrows the agent-skills runtime and chat scaffold from
[Google AI Edge Gallery](https://github.com/google-ai-edge/gallery) (Apache 2.0) — much credit to
that project — and distills it to a single feature: **Agent Skills mode** running on **LiteRT-LM**.
The rest (Chat, Prompt Lab, Mobile Actions, AICore, MLKit, Firebase, MCP, etc.) is intentionally
removed.

Inspired by [LFG](https://github.com/bradflaugher/LFG) — a terminal agent with the same skills
layer — LFE puts the agent in your pocket: no servers, no telemetry, no Play Services AICore.

## What's in

- **Agent Skills chat** as the only screen — launches straight into the agent.
- **Settings → Model Manager** via the gear icon — download/swap models. That's the only other screen.
- **LiteRT-LM runtime** for on-device inference. No MLKit, no AICore.
- **Bundled skills** (auto-discovered from `assets/skills/`):
  - `budget-tracker` — log expenses/income and view summaries
  - `calculate-hash` — SHA-1/256/384/512 via WebCrypto
  - `interactive-map` — show a location on a map
  - `mood-tracker` — daily mood + history
  - `password-generator` — secure random passwords
  - `qr-code` — generate QR codes (offline, no CDN)
  - `query-wikipedia` — summarize a topic
  - `send-email` — compose an email intent
  - `translator` — translate between languages
- **Models**: every model from the Edge AI Gallery allowlist (Gemma 4 E2B/E4B, Gemma 3n E2B/E4B,
  Gemma 3 1B, Qwen 2.5 1.5B, DeepSeek-R1-Distill-Qwen 1.5B), all targeted at the single agent
  task. Allowlist bundled in `assets/model_allowlist.json` and refreshed from GitHub at launch.

## What's deliberately out

| Removed                              | Why                                                |
|--------------------------------------|----------------------------------------------------|
| AICore / MLKit GenAI                 | LiteRT-LM only — keep the runtime story simple     |
| Firebase / FCM / Analytics           | No telemetry. On-device means on-device.           |
| MCP server plumbing                  | Agent skills carry the same payload, less surface  |
| AI Chat / Prompt Lab / Ask Image…    | Folded into the one agent screen                   |
| Mobile Actions / Tiny Garden         | Demo apps, not the point                           |
| Benchmark UI                         | Use `adb` if you need numbers                      |
| Notifications subsystem              | No background nagging                              |
| Home screen carousel                 | One screen, one purpose                            |
| OSS-licenses screen                  | Apache 2.0 — see LICENSE                           |

## Building

You do **not** need Android Studio. CI builds an APK on every push and a signed sideload-ready APK
on every tag. Locally you only need a JDK 21 and a network connection.

```sh
cd android/src
./gradlew assembleDebug          # debug APK in app/build/outputs/apk/debug/
./gradlew test                   # JVM unit tests
./gradlew assembleRelease        # debug-signed release APK
```

To install on a connected device:

```sh
./gradlew installDebug
```

## Releases

Every push to `main` triggers `release.yml`, which builds a debug-signed APK and publishes it as
a new GitHub Release named `build-YYYY-MM-DD-HHMM-<sha7>`. All builds stay forever — the
[Releases page](https://github.com/bradflaugher/LFE/releases) is the chronological archive, and
[`releases/latest`](https://github.com/bradflaugher/LFE/releases/latest) always points at the
newest build.

```
adb install -r lfe-build-2026-05-23-2230-7460dd0.apk
```

Sideload-ready, not Play-eligible (debug signing). Wiring a real keystore is a one-secrets-block
change in `release.yml` when you want that.

## Customizing skills

Drop a `SKILL.md` directory under `android/src/app/src/main/assets/skills/` and rebuild. The schema
is the [agentskills.io spec](https://agentskills.io/specification) — see any of the existing
skills for an example.

## HuggingFace gated models

Some allowlist models are gated on HF. To enable in-app login:

1. Create an OAuth app at <https://huggingface.co/settings/applications/new>.
2. Set its redirect URL to `com.bradflaugher.lfe://oauth` (or override `APP_AUTH_REDIRECT_SCHEME`
   via `-P` / `~/.gradle/gradle.properties`).
3. Fill in `clientId` and `redirectUri` in `common/ProjectConfig.kt`.

Most models in the bundled allowlist are not gated and work without this.

## License

GPL-3.0-or-later. See [LICENSE](LICENSE).

This project includes code adapted from
[Google AI Edge Gallery](https://github.com/google-ai-edge/gallery) (Apache License 2.0,
Copyright 2025 Google LLC). Apache 2.0 § 4 permits redistribution under GPL-3.0-or-later when
the original copyright notice is preserved — every adapted file keeps it in its header.
