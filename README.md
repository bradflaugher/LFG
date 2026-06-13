<div align="center">
  <img src="docs/app_icon_512.png" alt="LFG App Icon" width="128" height="128">
  <p><strong>Uncensored AI agents on our phones! LFG!</strong></p>

  <a href="https://github.com/bradflaugher/LFG/releases/latest"><img src="https://img.shields.io/github/v/release/bradflaugher/LFG?style=for-the-badge&color=orange&label=Latest%20Release" alt="Latest Release"></a>
  <a href="https://github.com/bradflaugher/LFG/blob/main/LICENSE"><img src="https://img.shields.io/github/license/bradflaugher/LFG?style=for-the-badge&color=green" alt="License"></a>
</div>

For everything you wouldn't type into a corporate cloud model. For everything the polite robots refuse, hedge, or lecture you about. Runs 100% on your phone with no accounts, no telemetry, and no paper trail.

LFG is built to run uncensored models locally, giving them direct access to **skills** that interact with your device. It works entirely in airplane mode. Your data never leaves your silicon.

## Why LFG?

Cloud AI is built for corporate productivity. It's safe, sterile, and structurally forbidden from helping with anything remotely sensitive or raw. LFG is for everything else:

* 🔒 **Absolute Privacy:** Everything stays on your hardware. Perfect for sensitive documents, financial panic, relationship drafts, or anything you wouldn't trust to a third-party server.
* 🙅 **Zero Censorship:** Abliterated models that answer awkward, creative, or direct questions without moralizing or lecturing you.
* ✈️ **100% Local:** No sign-up, no internet connection required. Works when your network is dead. Delete the app, and your data disappears with it.

Use LFG as a **privacy filter**: draft and scrub your text here in complete privacy, then clean it up before pasting it to the cloud.

## What It Can Do

Toggle **Skills** inside the chat sheet to give your local model real-world utility:

* **🛡️ Privacy Triage (Default):**
  * **Redact:** Scrub identifying details from text before sharing it elsewhere.
  * **Confidential Desk:** Decode leases, medical results, or legalese without exposing them to cloud trackers.
  * **Privacy Lens:** Parse a terms-of-service document to find exactly where your data is commercialized.
* **🔥 Uncensored (Opt-In):**
  * **Straight Answer:** Direct guidance on health, finance, and legal gray zones without the standard walls of text.
  * **Devil's Advocate:** Steelman arguments you disagree with to pressure-test your own logic.
  * **Confrontation Rehearsal:** Practice breakups, salary negotiations, or difficult conversations with zero judgment.
  * **Chaos Engine:** Generate alibis, craft sharp comebacks, or roast your writing.

👉 Check out the [Recipe Book](docs/RECIPES.md) for real-world examples.

## Skills

Skills allow the LLM to execute tasks: custom personas, JS helper scripts that save data or render graphs, or native Android intents (like calendar and system toggles).

* All bundled skills are located in [assets/skills](android/src/app/src/main/assets/skills).
* Creating your own is as simple as writing a markdown file—see the [Skills Guide](docs/SKILLS.md).
* Utility and agentic skills start **off** by default. Toggle them on in the chat sheet to unlock the full experience.

## Models

Ships pre-configured with four Gemma variants:
* **E2B / E4B (Stock):** Multimodal. Best for vision skills (scanning documents or images locally).
* **E2B-Abliterated / E4B-Abliterated (Text-only):** Designed to answer unfiltered questions. Use these for opt-in skills.

### Bring Your Own
Import any LiteRT-LM compatible `.litertlm` or `.task` file directly, or configure LFG to target any external OpenAI-compatible API when you need larger model capabilities.

## Install

<a href="https://github.com/bradflaugher/LFG/releases/latest"><img src="docs/badges/get-it-on-github.png" alt="Get it on GitHub" height="60"></a>

1. Download the latest APK from the [Releases](https://github.com/bradflaugher/LFG/releases/latest) page.
2. Enable "Install from unknown sources" on your device to sideload the APK. 
   *(Google will flag the app because it bypasses cloud telemetry and corporate guidelines).*

**LFG requires recent Android versions.**

## License

GPL-3.0-or-later. [LICENSE](LICENSE)

Includes code adapted from the Google AI Edge Gallery project (Apache 2.0).
