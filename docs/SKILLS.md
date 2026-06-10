# Writing Skills for LFE

A **skill** is a small package that gives the on-device LLM a new capability
— a persona to adopt, a JavaScript helper to run, or a native intent to fire.
Skills are the only way to extend what LFE can do, and writing one is mostly
just authoring a `SKILL.md` file.

This doc walks through every kind of skill from the simplest case up, with copy-
pasteable templates you can hack on.

## Table of contents

- [Why skills?](#why-skills)
- [How a skill is loaded and triggered](#how-a-skill-is-loaded-and-triggered)
- [Type 1 — Text-only skills (the smallest possible skill)](#type-1--text-only-skills-the-smallest-possible-skill)
- [Type 2 — JavaScript skills](#type-2--javascript-skills)
  - [Returning an image](#returning-an-image)
  - [Returning a webview](#returning-a-webview)
  - [Asking the user for a secret (API keys, tokens)](#asking-the-user-for-a-secret-api-keys-tokens)
- [Type 3 — Native skills (Android intents)](#type-3--native-skills-android-intents)
- [Installing a skill in the app](#installing-a-skill-in-the-app)
  - [Load from a URL](#load-from-a-url)
  - [Import from a local folder](#import-from-a-local-folder)
  - [Bundle it into the build](#bundle-it-into-the-build)
- [Tips and gotchas](#tips-and-gotchas)

## Why skills?

A vanilla on-device LLM can only generate text. It can't open a webpage, hash
a string, draw a chart, send an email, or remember anything between sessions.
Skills bridge that gap without forcing every capability to be hard-coded into
the app:

- The bundled `private-journal` skill is a small JS file that stores entries in
  on-device storage and the model knows when to call it.
- The bundled `redact` skill is *zero* lines of code — just a `SKILL.md` persona
  that tells the model how to strip personal details out of text.
- The bundled `set-reminder` skill is also code-free — just a `SKILL.md` pointing
  at the built-in `run_intent` tool to add a calendar event.

Once you've written one, sharing it is just sharing a folder.

## How a skill is loaded and triggered

Every skill is a folder containing at minimum a `SKILL.md` file. The folder
name is in kebab-case and matches the skill name:

```
my-skill/
└── SKILL.md
```

When the user enables a skill in the chat's **Skills** sheet, the `SKILL.md`
metadata (`name` + `description`) is appended to the LLM's system prompt as a
catalog entry. When the user types something, the model checks that catalog
and — if the request looks like a fit — loads the skill's full instructions
and follows them.

On-device LLMs can't spawn shells, run Python, or read arbitrary files. So
"executing" a skill means one of three things:

1. **Pure text** — the LLM just adopts a persona / set of rules. No code.
2. **JavaScript in a hidden webview** — the LLM calls the built-in `run_js`
   tool, which loads your `index.html` off-screen, runs `await
   ai_edge_gallery_get_result(data)`, and feeds the return value back to the
   model.
3. **Android intent** — the LLM calls the built-in `run_intent` tool with an
   intent name and parameters, and the app fires it on the OS.

That's the whole runtime. Now let's build one of each.

## Type 1 — Text-only skills (the smallest possible skill)

Use this when all you want is a persona, a scenario, or a fixed prompt the
user can summon. No code.

**Structure:**

```
fitness-coach/
└── SKILL.md
```

**`SKILL.md`:**

```markdown
---
name: fitness-coach
description: A cheerful, high-energy fitness coach that gives motivational workout routines.
---

# Fitness Coach

## Persona
You are an enthusiastic, supportive fitness coach. Use upbeat language and lots
of encouragement.

## Instructions
When the user asks for a workout:
1. Open with a high-energy greeting.
2. Give a 15-minute routine they can do anywhere.
3. End with a "virtual high-five".
```

The frontmatter between the `---` lines is the part the model sees up front
when deciding whether to call your skill — keep `description` tight and
trigger-y ("when the user asks for a workout…"). Everything below the
frontmatter is loaded into context only after the skill is triggered, so feel
free to be verbose there.

> **Tip:** The model only reads `description` until it decides to use the
> skill, so put your trigger conditions there in plain English: *"when the
> user mentions food", "when the user wants to plan a trip"*, etc.

## Type 2 — JavaScript skills

Use this when you need real code: hashing, network calls, drawing, parsing,
date math, anything `fetch`-able.

**Structure:**

```
my-js-skill/
├── SKILL.md
└── scripts/
    └── index.html
```

**`SKILL.md` — tell the model to call `run_js`:**

```markdown
---
name: calculate-hash
description: Calculate a SHA hash of any text the user provides.
---

# Calculate hash

## Instructions

Call the `run_js` tool with these exact parameters:
- script name: index.html
- data: A JSON string with the following field:
  - text: String. The text to hash.
```

If your entry file is named `index.html`, the `script name` line is optional
— the runtime defaults to `scripts/index.html`.

**`scripts/index.html` — define `ai_edge_gallery_get_result` on `window`:**

```html
<!DOCTYPE html>
<html lang="en">
<head></head>
<body>
  <script>
    window['ai_edge_gallery_get_result'] = async (data) => {
      try {
        const { text } = JSON.parse(data);
        const buf = await crypto.subtle.digest(
          'SHA-256',
          new TextEncoder().encode(text),
        );
        const hex = Array.from(new Uint8Array(buf))
          .map((b) => b.toString(16).padStart(2, '0'))
          .join('');
        return JSON.stringify({ result: hex });
      } catch (e) {
        return JSON.stringify({ error: e.message });
      }
    };
  </script>
</body>
</html>
```

A few things to internalize:

- **`ai_edge_gallery_get_result` must be async** and attached to `window`. The
  name is fixed (kept for compatibility with the upstream skill ecosystem).
- **Always return a stringified JSON object** with either a `result` field on
  success or an `error` field on failure. Plain strings won't be parsed back.
- **Treat `scripts/index.html` as a headless execution environment.** You have
  `fetch`, `crypto`, `WebAssembly`, IndexedDB, the lot. You can pull in a CDN
  library with a `<script src="...">` tag, or split your logic across multiple
  `.js` files inside `scripts/` and import them.

### Returning an image

To render an image in the chat, base64-encode it and set `image.base64`:

```javascript
return JSON.stringify({
  result: 'Generated a QR code.',
  image: { base64: pngBase64String },
});
```

### Returning a webview

To show an interactive UI inline in the chat, return a `webview` block. The
URL is resolved relative to an `assets/` folder next to your script.

```javascript
return JSON.stringify({
  result: 'Here is the map.',
  webview: { url: 'webview.html', aspectRatio: 1.0 },
});
```

Layout for a skill that returns a webview:

```
my-interactive-skill/
├── SKILL.md
├── scripts/
│   └── index.html      ← the hidden runner
└── assets/
    └── webview.html    ← the UI rendered in the chat
```

> **Tip:** Pass data from your hidden runner to the rendered webview via
> query params (`webview.html?lat=37.7&lon=-122.4`). Parse them in the
> webview with `URLSearchParams`.

### Asking the user for a secret (API keys, tokens)

If your skill needs an API key, **never** put it in the prompt — the model
will paste it back, save it to history, or log it. Instead, declare that you
need a secret and the app will pop a native dialog the first time the skill
runs.

In `SKILL.md`:

```markdown
---
name: github-stars
description: Look up a GitHub repo's star count.
metadata:
  require-secret: true
  require-secret-description: A GitHub personal access token (Settings → Developer settings).
---
```

In `index.html` — accept a second parameter:

```javascript
window['ai_edge_gallery_get_result'] = async (data, secret) => {
  const { repo } = JSON.parse(data);
  const res = await fetch(`https://api.github.com/repos/${repo}`, {
    headers: { Authorization: `Bearer ${secret}` },
  });
  const json = await res.json();
  return JSON.stringify({ result: `${json.stargazers_count} stars` });
};
```

The secret is held in app-private storage and re-used on subsequent calls;
the user can rotate or clear it from the Skills sheet.

## Type 3 — Native skills (Android intents)

Use this when the right answer is "add it to the calendar" or "open another
app" rather than "generate text". You're handing off to Android. This is the
pattern the bundled `set-reminder` skill uses.

```markdown
---
name: set-reminder
description: Add a reminder or event to the user's calendar.
---

# Set a reminder

## Instructions

Call the `run_intent` tool with these exact parameters:
- intent: create_calendar_event
- parameters: A JSON string with these fields:
  - title: a short title. String.
  - description: any extra detail, or "". String.
  - begin_time: start time as yyyy-MM-dd'T'HH:mm:ss. String.
  - end_time: end time, same format. String.
```

The built-in intents today are:

| Intent                    | Parameters (JSON fields)                                  | Does |
| ------------------------- | --------------------------------------------------------- | ---- |
| `send_email`              | `extra_email`, `extra_subject`, `extra_text`              | Opens the email app with a draft. |
| `send_sms`                | `phone_number`, `sms_body`                                | Opens the messaging app with a draft. |
| `create_calendar_event`   | `title`, `description`, `begin_time`, `end_time` (`yyyy-MM-dd'T'HH:mm:ss`) | Opens the calendar app to add an event. |
| `read_calendar_events`    | `date` (`yyyy-MM-dd`)                                     | Reads that day's events (asks calendar permission). |
| `get_current_date_and_time` | *(none)*                                                | Returns the device's current date/time. |

See `IntentHandler.kt` in the app source for the exact handlers. Adding a new
intent (open camera, set alarm, etc.) means a small patch to the app itself.
PRs welcome.

## Installing a skill in the app

Three ways, in increasing order of permanence.

### Load from a URL

If your skill is hosted on the web, paste its folder URL into the **Skills**
sheet → **+** → **Load skill from URL**. The URL should be the folder
itself, not the `SKILL.md` inside it.

A few hosting gotchas:

- **GitHub raw URLs don't work.** `raw.githubusercontent.com` serves files as
  `text/plain`, which breaks JS execution inside the webview. Use real
  hosting: GitHub Pages, Cloudflare Pages, Netlify, etc.
- **GitHub Pages converts `.md` files to HTML by default.** The app needs the
  raw `SKILL.md`, so add an empty `.nojekyll` at the repo root to disable
  Jekyll processing.

To sanity-check your URL: paste it into a browser with `/SKILL.md` appended.
If you see raw Markdown, you're good. If you see rendered HTML or a download
prompt, fix the hosting first.

### Import from a local folder

For iterating on a skill without hosting it:

```bash
adb push my-skill/ /sdcard/Download/
```

Then in the **Skills** sheet → **+** → **Import local skill** → pick the
folder. The app copies it into app-private storage.

### Bundle it into the build

For skills that should ship with the APK, drop the folder into
`android/src/app/src/main/assets/skills/` and rebuild. The bundled skills in
this repo (`redact`, `scam-check`, `private-journal`, etc.) live there and
double as copy-paste templates.

## Tips and gotchas

- **The `description` field is the only thing the model sees by default.** Make
  it specific. *"Convert text to a QR code"* triggers reliably; *"Helper for
  links"* doesn't.
- **Add a `homepage` to the frontmatter** to make your skill name clickable in
  the Skills list:
  ```markdown
  metadata:
    homepage: https://github.com/you/my-skill
  ```
- **Skills can stack.** Enabling many skills bloats the system prompt and
  slows the first reply — keep the active set small for the model size you're
  using.
- **For JS debugging,** open the chat's execution panel after the skill runs
  — it shows the data the model passed in, the value your script returned,
  and live `console.log` output.
- **Skills can run offline** as long as your JS doesn't `fetch` external APIs.
  The `private-journal`, `mood-tracker`, `quick-note`, and `password-generator`
  skills all work with airplane mode on.

That's the whole API. Browse `android/src/app/src/main/assets/skills/` for
working examples of every pattern above.
