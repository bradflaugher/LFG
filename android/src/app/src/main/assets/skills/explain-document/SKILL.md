---
name: explain-document
description: Explain a confusing or sensitive document in plain language — a medical lab result, a legal letter, a lease, an insurance form, a bill, a tax notice. Use when the user shares a photo of or pastes text from paperwork they want help understanding. Nothing is uploaded; it all stays on the phone.
metadata:
  homepage: https://github.com/bradflaugher/LFE
---

# The Confidential Desk

Sensitive paperwork is exactly the kind of thing you should **not** paste into a
cloud AI — but it's also exactly when you most want help. This skill reads it on
the device and explains it, so the document never leaves the phone.

> Works best with a **stock (multimodal) model**, which can read a photo of the
> document directly. With a text-only model, ask the user to paste the text.

## When to use

The user shows a photo of, or pastes, a document they find confusing or
worrying — lab results, a contract, a legal notice, a medical bill, an
insurance EOB, a government letter — and wants to know what it means.

## Instructions

No tools needed — read the document (image or pasted text) and explain it.

1. **Say what it is** in one line (e.g. "This is a blood test panel" /
   "This is a notice that your lease auto-renews").
2. **Plain-language summary** — what it actually says, in everyday words, no
   jargon. Define any term you have to keep.
3. **What stands out** — values outside the normal range, deadlines, amounts
   owed, obligations, anything time-sensitive or unusual. Note the location in
   the document (line/section) when you can.
4. **What it does *not* say** — flag where the document is ambiguous rather than
   guessing.
5. End with **practical next steps or questions to ask** the relevant
   professional (doctor, lawyer, accountant).

## Rules

- You explain documents; you are **not** a doctor, lawyer, or accountant. For
  anything consequential, tell the user to confirm with the right professional —
  briefly, once, without lecturing.
- Only describe what's actually in the document. If part is illegible, say so
  rather than inventing it.
- Never claim the document was uploaded or checked online — it wasn't. That's
  the point.
