# LFE

A minimal, privacy-first AI agent for Android. Skills-first and low-feature.

## What it does

LFE runs large language models on your phone — both locally and via OpenAI-compatible APIs — and lets them call **skills** that execute actions directly on your device.

## Skills

Browse the full list of built-in skills in the [skills directory](android/src/app/src/main/assets/skills).

See **[docs/SKILLS.md](docs/SKILLS.md)** for details on adding your own skills.

## Models

The app includes four recommended **Gemma 4** variants:

- **E2B** and **E4B** — Stock multi-modal models (text + image + audio)
- **E2B-Abliterated** and **E4B-Abliterated** — Uncensored versions (text-only)

### Want a different model?

1. Download any [LiteRT-LM](https://github.com/google-ai-edge/LiteRT-LM) compatible `.litertlm` or `.task` file from Hugging Face.
2. Copy the file to your device, then use **Import local model file** at the bottom of the model manager.

### Need a bigger model?

LFE supports inference through any OpenAI-compatible API — perfect for models too large to run locally.

## Install

Download the latest APK from the [releases page](https://github.com/bradflaugher/LFE/releases/latest) directly on your Android device.

Open the APK and follow the on-screen instructions to install.  
See Google’s guide on [installing apps from unknown sources](https://support.google.com/googleplay/answer/14669046) if needed.

**Note:** LFE targets the latest Android versions only.

## License

GPL-3.0-or-later. See [LICENSE](LICENSE).

Includes code adapted from [Google AI Edge Gallery](https://github.com/google-ai-edge/gallery) (Apache 2.0).