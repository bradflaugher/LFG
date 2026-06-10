# LFE

A minimal, privacy-first AI agent for Android. Skills-first and low-feature.

## What it does

LFE runs large language models on your phone (both locally and via OpenAI-compatible APIs) and lets them call **skills** that execute actions directly on your device.

## What can I actually do with it?

LFE *does things*, not just chats — all on-device. A few one-liners to try:

- 📶 **"Make a Wi-Fi QR code for *Cabin-5G*, password *trailmix2026*."** — guests scan to join.
- 🧾 **"Split $128.40 four ways with a 20% tip."** — instant per-person amount, works offline.
- ✈️ **"How much is 7,500 yen in dollars?"** — live currency conversion.
- 💬 **"Rewrite this politely and text it to 555-0142: …"** — polishes the message, opens your SMS app pre-filled.
- ⏰ **"Remind me to take the chicken out at 5pm."** — adds it to your calendar.
- 📝 **"Note that I parked on level 3, section D."** … later: **"What were my notes?"**
- 🍳 **"I've got eggs, onion, and cheddar — dinner ideas?"**
- 📰 **"Summarize this article: <url>"** — fetched and condensed privately on the phone.

👉 **[See the full recipe book → docs/RECIPES.md](docs/RECIPES.md)** for these and more, including multi-skill combos.

## Skills

Skills are how LFE does anything beyond text — a persona, a bit of JavaScript, or
an Android intent. Toggle them on in the chat's **Skills** sheet. The bundled set
covers everyday phone tasks (Wi-Fi QR, tip splitting, currency conversion, quick
notes, reminders, message proofreading, article summaries, and more).

Browse them in the [skills directory](android/src/app/src/main/assets/skills), and
see **[docs/SKILLS.md](docs/SKILLS.md)** to write your own — it's mostly just one
`SKILL.md` file.

## Models

The app includes four recommended **Gemma 4** variants:

- **E2B** and **E4B** — Stock multi-modal models (text + image + audio)
- **E2B-Abliterated** and **E4B-Abliterated** — Uncensored versions (text-only)

### Want a different model?

1. Download any [LiteRT-LM](https://github.com/google-ai-edge/LiteRT-LM) compatible `.litertlm` or `.task` file from Hugging Face.
2. Copy the file to your device, then use **Import local model file** at the bottom of the model manager.

### Need a bigger model?

LFE supports inference through any OpenAI-compatible API. This is perfect for models too large to run locally.

## Install

Download the latest APK from the [releases page](https://github.com/bradflaugher/LFE/releases/latest) directly on your Android device.

Open the APK and follow the on-screen instructions to install.  
See Google’s guide on [installing apps from unknown sources](https://support.google.com/googleplay/answer/14669046) if needed.

**Note:** LFE targets the latest Android versions only.

## License

GPL-3.0-or-later. See [LICENSE](LICENSE).

Includes code adapted from [Google AI Edge Gallery](https://github.com/google-ai-edge/gallery) (Apache 2.0).