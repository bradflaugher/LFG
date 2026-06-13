# LFG

**LFG. The on-device AI that actually says the quiet part out loud.**

For everything you wouldn't paste into ChatGPT. For everything the polite robots refuse, hedge, or lecture you about. Runs 100% on your phone (or your chosen API). No account. No cloud. No receipts.

LFG ships uncensored models on purpose and gives the model real **skills** that can touch your device, your data, and your life. Airplane mode friendly. Your data never leaves unless a skill you enabled does something.

## Why LFG when you already have the big ones?

The big cloud models are great at writing your TPS reports and summarizing meetings.

LFG is for the shit they're *structurally forbidden* from helping with:

- 🔒 **Private as fuck** — everything stays on your hardware. Perfect for medical letters, weird kinks, financial disasters, "is this text from my ex unhinged?", or anything that would be a GDPR incident if it left the building.
- 🙅 **Uncensored by design** — Abliterated models that don't clutch pearls. They answer the awkward, the illegal-but-not-immoral, the creative, the "I just need to know."
- ✈️ **Yours** — no login, no telemetry, works when your carrier is crying. Delete the app and it's gone.

Use LFG as the **unfiltered privacy layer** in front of your other tools. Scrub here, then paste the safe version into the cloud model.

## What it's actually good at

Open the **Skills** sheet in chat, toggle what you need, and talk like a normal (deranged) person.

**🛡️ Privacy power tools (mostly on by default)**
- **Redact** — turn your disaster of a text into something you can safely show the cloud.
- **Explain document** — photograph the lab results / lease / cease-and-desist and get the "what does this actually mean and am I fucked" version, all on-device.
- **Scam check** — paste the "your package is held, click here" text and find out it's fake before you click.
- **Privacy Lens** — paste a ToS or privacy policy, learn exactly which orifices they're renting out your data through.

**🔥 Won't say no (off by default — enable when you're ready)**
- **Straight answer** — health, sex, substances, money, family, the law's gray zones. No "I'm not a doctor" walls of text.
- **Unfiltered muse** — write the dark, horny, unhinged fiction the cloud models neuter.
- **Roleplay & rehearse** — practice the breakup / confrontation / coming out / salary ask with an AI that won't hold back.
- **Devil's advocate** — steelman the take you hate. Pressure test your own bullshit.

**🧠 Your data, your phone**
- Private journal, mood tracker, quick notes, budget — zero sync, zero cloud.

**🛠️ Random useful chaos**
- On-device translator, password generator, calendar reader/writer, set reminders, plus whatever custom skills you add.

**New punchy ones (try them):**
- **Brutal roast** — destroy your dating bio / group chat text / life choices with no mercy and some laughs.
- **Comeback king** — paste what they just said, walk away with ammo.
- **Alibi generator** — need a boring, consistent, defensible story? We got you (use responsibly).

👉 See the [recipe book](docs/RECIPES.md) for real examples of how people actually use this thing.

## Skills

Skills turn the LLM into an agent that can *do* stuff: personas, JS helpers that store data or draw charts, or firing real Android intents (calendar, flashlight, etc.).

All bundled skills live in [android/src/app/src/main/assets/skills](android/src/app/src/main/assets/skills). Writing your own is just a `SKILL.md` (see [docs/SKILLS.md](docs/SKILLS.md)).

The spicy skills (straight-answer, unfiltered-muse, roleplay, devil's advocate, brutal-roast, comeback-king, alibi-generator, etc.) and anything that touches other apps start **off**. Flip them on in the Skills sheet when you want the full unhinged experience.

## Models

Ships with four Gemma variants:

- **E2B / E4B** (stock) — multimodal. Use these for vision skills (photograph documents for Confidential Desk).
- **E2B-Abliterated / E4B-Abliterated** (text-only) — the ones that will actually answer your worst questions. Switch to these for the "won't say no" skills.

### Bring your own

Drop any LiteRT-LM compatible `.litertlm` or `.task` into the model manager import. Or point it at any OpenAI-compatible API for the big models when you need the extra brain.

## Install

Grab the latest APK from [Releases](https://github.com/bradflaugher/LFG/releases/latest) and sideload it.

(You'll need to allow "install from unknown sources". Google will complain because this app exists to do the things their policies don't like.)

**LFG targets recent Android only.**

## License

GPL-3.0-or-later. [LICENSE](LICENSE)

Forked/adapted from Google AI Edge Gallery (Apache 2.0).

---

LFG. Your phone. Your rules. Let's fucking go.
