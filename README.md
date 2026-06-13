# LFG

**LFG! Let's have uncensored agents with data that never leaves your phone (unless we tell it to).** For everything you wouldn't paste into
ChatGPT — and everything the big assistants won't do.

LFE runs language models entirely on-device (or, if you choose, via an
OpenAI-compatible API). It ships **uncensored** model options on purpose, and it
lets the model call **skills** that act on your device. No account, no telemetry,
works in airplane mode.

## Why LFE, when you already have Gemini / Claude / ChatGPT?

You should keep those — they're more powerful, and great for everyday work. LFE
is for the things they're *structurally* bad at:

- 🔒 **Private** — it runs on the device, so nothing you give it leaves your
  pocket. It's the right tool for anything you'd never paste into a cloud chatbot.
- 🙅 **Uncensored** — it ships [Abliterated](#models) models that don't refuse or
  lecture, so it answers the frank, sensitive, or creative stuff the cloud models
  sanitize.
- ✈️ **Offline & yours** — no account, no signal required, no data harvesting.

Think of LFE as the **privacy front-end to your other AI tools**: sanitize, read,
and reason about sensitive things here, then take the safe result to a bigger
model if you want.

## What it's best at

Toggle skills on in the chat's **Skills** sheet, then just talk to it. The
bundled set is built around the niche:

**🔒 Privacy power-tools**
- 🛡️ **Redact** — *"Scrub the names and account numbers out of this so I can safely paste it into ChatGPT."*
- 📄 **Confidential Desk** — *"What does this lab result / lease / legal letter actually mean?"* — reads it on-device, nothing uploaded.
- 🎣 **Scam check** — *"Is this text from 'my bank' a scam?"* — spot phishing without forwarding it anywhere.
- 👁️ **Privacy Lens** — *"What is this app's privacy policy actually taking?"*

**🙅 Won't say no** *(best with an Abliterated model)*
- 💬 **Straight answer** — frank, judgment-free answers to sensitive personal questions.
- ✍️ **Unfiltered muse** — an uncensored partner for mature/dark fiction.
- 🎭 **Roleplay & rehearse** — practice a hard conversation, or immersive character chat — privately.
- ⚖️ **Devil's advocate** — argue any side, steelman the uncomfortable position, pressure-test your thinking.

**📓 Your data stays yours**
- Private journal, mood tracker, notes, and budget — all on-device, never synced.

**✈️ Offline & personal**
- On-device translation, local password generation, and your calendar.

👉 **[See the recipe book → docs/RECIPES.md](docs/RECIPES.md)** for concrete walkthroughs.

## Skills

Skills are how LFE does anything beyond plain chat — a persona, a bit of
JavaScript, or an Android intent. Browse them in the
[skills directory](android/src/app/src/main/assets/skills), and see
**[docs/SKILLS.md](docs/SKILLS.md)** to write your own — it's mostly just one
`SKILL.md` file.

The four "won't say no" skills and the calendar skills are **off by default**
(they reshape the assistant's voice or touch other apps) — flip them on in the
Skills sheet when you want them.

## Models

The app includes four recommended **Gemma 4** variants — pick based on the job:

- **E2B** and **E4B** — Stock multi-modal models (text + image + audio). Use
  these for the vision skills like **Confidential Desk** (photograph a document).
- **E2B-Abliterated** and **E4B-Abliterated** — **Uncensored** (text-only). Switch
  to these for the "won't say no" skills — frank answers, mature fiction, roleplay,
  devil's advocate.

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
