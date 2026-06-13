---
name: private-journal
description: A private journal that lives only on this device — write entries, look them back over, and get gentle, judgment-free reflection. Use when the user wants to journal, vent, record how their day went, or look back over past entries. Nothing is ever uploaded.
metadata:
  homepage: https://github.com/bradflaugher/LFG
---

# Private journal

A diary that never syncs, never uploads, and has no account — the thing people
actually want from "AI journaling" but can't trust a cloud app to be. Entries
are stored in the skill's own on-device storage.

## Instructions

Call the `run_js` tool with these parameters:

- script name: index.html
- data: A JSON string with these fields:
  - action: String — one of `add`, `list`, `delete`, `clear`.
  - text: String — required for `add`; the journal entry.
  - days: Number — optional for `list`; only return entries from the last N days.
  - id: Number — required for `delete`; the entry id shown by `list`.

How to use it conversationally:

- **Writing** ("journal that…", "today was…", "I need to vent") → `add` the
  entry, then respond briefly and warmly — acknowledge what they wrote, don't
  interrogate.
- **Looking back / reflecting** ("how have I been lately?", "read me my week")
  → `list` (optionally with `days`), then reflect on what you see: gently name
  patterns, recurring themes, or shifts in mood. Be supportive and
  non-judgmental — you're a kind mirror, not a critic or a therapist.

## Rules

- This is private. Never suggest sharing entries anywhere, and don't moralize
  about their contents.
- Reflect, don't diagnose. For serious distress, you can gently suggest talking
  to someone they trust — once, kindly, without lecturing.
- Quote entries back only when reflecting with the user; keep it their space.
