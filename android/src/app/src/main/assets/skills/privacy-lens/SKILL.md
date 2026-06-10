---
name: privacy-lens
description: Decode a privacy policy, terms of service, or app-permission list and tell the user what data is collected, who it's shared with, and the red flags. Use when the user pastes a privacy policy / ToS / EULA, or asks "what is this app/site actually taking?"
metadata:
  homepage: https://github.com/bradflaugher/LFE
---

# Privacy Lens — what are they actually taking?

Nobody reads the privacy policy. This skill does, on the device, and turns ten
pages of legalese into the three things you actually care about — without
sending the document (or your interest in it) to anyone.

## When to use

The user pastes a privacy policy, terms of service, EULA, cookie notice, or a
list of app permissions, or asks what a given service collects.

## Instructions

Analyze the pasted text directly — no tools, no network.

1. **What they collect** — bullet the categories of data (identity, contacts,
   location, browsing/usage, device IDs, payment, biometrics, content you
   create…). Quote the giveaway phrases briefly.
2. **What they do with it** — sold? shared with "partners"/advertisers/data
   brokers? used to train models? combined across services? retained how long?
3. **🚩 Red flags** — the parts that should give pause: broad/irrevocable
   licenses to your content, sharing with unnamed third parties, vague
   "legitimate interest", no real delete/opt-out, arbitration/class-action
   waivers, "we may change this at any time".
4. **✅ Reassuring bits**, if any (data minimization, local processing, clear
   deletion, no sale).
5. **Bottom line** — one or two sentences: how privacy-invasive is this, and is
   there anything the user should do (decline a permission, opt out, avoid)?

## Rules

- Ground every claim in the actual text — quote or point to the clause. Don't
  invent terms that aren't there.
- This is a plain-language reading, not legal advice; say so once, briefly, if
  it matters.
- If the pasted text is partial, say what you can and flag what's missing.
