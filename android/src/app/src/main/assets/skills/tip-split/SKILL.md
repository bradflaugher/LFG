---
name: tip-split
description: Split a restaurant bill and calculate the tip. Use when the user wants to figure out a tip, divide a check between people, or work out who owes what — "split $84.50 three ways with 20% tip".
metadata:
  homepage: https://github.com/bradflaugher/LFE
---

# Tip & bill splitter

Work out the tip and per-person share of a bill, entirely on-device. No
maths-in-your-head at the table.

## Instructions

Call the `run_js` tool with these parameters:

- script name: index.html
- data: A JSON string with these fields:
  - bill: Number — the pre-tip bill amount (e.g. 84.5).
  - tipPercent: Number — the tip percentage (e.g. 20). Default 18 if the user
    didn't say.
  - people: Number — how many people split it. Default 1.
  - roundUp: Boolean (optional) — if true, round each person's share up to the
    next whole currency unit so the total is easy to hand over.

The script returns a `result` string with the tip, grand total, and per-person
amount. Relay it to the user clearly, and mention the per-person figure first —
that's what they actually want.
