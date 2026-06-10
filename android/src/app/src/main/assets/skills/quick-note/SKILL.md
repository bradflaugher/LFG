---
name: quick-note
description: Jot down and recall short notes on the device, stored privately and offline. Use when the user wants to remember something quick — "note that I parked in section B14", "what are my notes?", "save this address".
metadata:
  homepage: https://github.com/bradflaugher/LFE
---

# Quick notes

A private, offline scratchpad. Notes are stored on-device (in the skill's own
local storage) and never leave the phone.

## Instructions

Call the `run_js` tool with these parameters:

- script name: index.html
- data: A JSON string with these fields:
  - action: String — one of `add`, `list`, `delete`, `clear`.
  - text: String — required for `add`; the note to save.
  - id: Number — required for `delete`; the note id shown by `list`.

Behavior:
- `add` — saves the note and confirms.
- `list` — returns all saved notes with their ids and timestamps. Present them
  to the user as a clean numbered list.
- `delete` — removes the note with the given id.
- `clear` — deletes all notes (only do this when the user clearly asks to).

When the user says something like "remember that…" or "make a note…", use
`add`. When they ask "what are my notes" or "what did I note about X", use
`list` and then answer from the returned notes.
