---
name: redact
description: Strip personal and identifying details out of text so it's safe to paste into a cloud AI, post publicly, or share. Use when the user wants to redact, anonymize, sanitize, or "scrub" something before sending it somewhere else — names, emails, phone numbers, addresses, account numbers, employer, etc.
metadata:
  homepage: https://github.com/bradflaugher/LFG
---

# Redact — scrub it before you share it

This is LFG's job as the **privacy front-end to your other AI tools**. The user
has a chunk of text — an email thread, a log, a medical note, a contract, a
message — that they want help with from a bigger model (ChatGPT, Gemini,
Claude) or want to post somewhere, but it's full of personal details. You strip
those out *on-device* so what leaves the phone is safe.

## When to use

"Redact this before I paste it into ChatGPT", "anonymize this", "take my name
and address out of this", "make this safe to share".

## Instructions

Work entirely from your own reasoning — no tools needed. Everything stays on the
device.

1. Find and replace every piece of personally identifying information with a
   stable, generic placeholder so the text still makes sense:
   - Names of people → `[NAME]`, `[NAME 2]`, … (reuse the same tag for the same
     person so relationships are preserved).
   - Organizations / employers / schools → `[ORG]`.
   - Email addresses → `[EMAIL]`; phone numbers → `[PHONE]`.
   - Street addresses, cities precise enough to locate someone → `[ADDRESS]`.
   - Account numbers, card numbers, IDs, SSNs, policy/case numbers → `[ID]`.
   - Dates of birth, exact ages → `[DOB]`. URLs/usernames that identify someone
     → `[HANDLE]`.
   - Anything else that could single the person out (rare job title at a named
     company, license plate, etc.) → a sensible `[TAG]`.
2. **Preserve everything else verbatim** — the structure, the meaning, the
   non-identifying content. The goal is text a stranger could read without
   learning who it's about.
3. Output the redacted text **first**, ready to copy. Then add a short list of
   what you removed and the placeholder you used for each, so the user can map
   it back themselves if they need to.

## Rules

- When unsure whether something is identifying, redact it — err toward privacy.
- Never echo the original sensitive values back except in the brief mapping
  list. Don't "summarize" the personal details.
- Don't add commentary about the content itself; just sanitize it.
