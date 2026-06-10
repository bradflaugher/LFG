---
name: set-reminder
description: Add a reminder or event to the user's calendar. Use when the user wants to be reminded of something at a time — "remind me to call the dentist tomorrow at 9am", "add a lunch with Sam on Friday at noon".
metadata:
  homepage: https://github.com/bradflaugher/LFE
---

# Set a reminder / calendar event

Drop a reminder onto the phone's calendar. The calendar app opens pre-filled so
the user confirms and saves.

## When to use

The user wants to remember to do something at a specific time, or wants to add
an appointment/event.

## Working out the time

If you don't already know the current date, call `run_intent` with intent
`get_current_date_and_time` first (no parameters) so you can resolve relative
times like "tomorrow", "next Tuesday", or "in 2 hours" correctly.

## Instructions

Call the `run_intent` tool with these exact parameters:

- intent: create_calendar_event
- parameters: A JSON string with these fields:
  - title: String — a short title (e.g. "Call the dentist").
  - description: String — any extra detail, or an empty string `""`.
  - begin_time: String — start time as `yyyy-MM-dd'T'HH:mm:ss`
    (e.g. `2026-06-11T09:00:00`). Use 24-hour time.
  - end_time: String — end time in the same format. If the user didn't give a
    duration, default to **30 minutes** after begin_time.

After the intent fires, confirm what you scheduled in plain language ("Added
'Call the dentist' for tomorrow at 9:00 AM — just hit save").
