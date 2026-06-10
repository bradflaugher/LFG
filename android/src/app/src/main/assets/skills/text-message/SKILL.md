---
name: text-message
description: Open the user's messaging app with a pre-filled SMS. Use when the user wants to text someone — "text Alex that I'm running 10 minutes late", "send a message to 555-1234".
metadata:
  homepage: https://github.com/bradflaugher/LFE
---

# Send a text message

Hand off to the phone's SMS app with the message already typed. The user still
taps send — nothing is sent automatically.

## When to use

The user asks to text or message someone. If they describe the message
loosely ("tell mom I'll be late"), draft a short, natural SMS for them.

## Instructions

Call the `run_intent` tool with these exact parameters:

- intent: send_sms
- parameters: A JSON string with these fields:
  - phone_number: String — the recipient's phone number. If the user gave a
    name instead of a number, leave this an empty string `""` so they can pick
    the contact in the messaging app.
  - sms_body: String — the message text. Keep it concise and SMS-appropriate.

After the intent fires, tell the user the draft is ready in their messaging app
to review and send.
