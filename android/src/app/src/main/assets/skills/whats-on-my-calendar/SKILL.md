---
name: whats-on-my-calendar
description: Read the user's calendar events for a given day and summarize them. Use when the user asks what's on their schedule, what they have today or tomorrow, or whether they're free at a certain time.
metadata:
  homepage: https://github.com/bradflaugher/LFE
---

# What's on my calendar?

Look up the user's real calendar for a day and tell them what's coming. The app
asks for calendar permission the first time; everything stays on-device.

## When to use

The user asks about their schedule — "what do I have today?", "am I free
tomorrow afternoon?", "what's my Friday look like?"

## Working out the date

If you need today's date to resolve "today"/"tomorrow", call `run_intent` with
intent `get_current_date_and_time` first (no parameters).

## Instructions

Call the `run_intent` tool with these exact parameters:

- intent: read_calendar_events
- parameters: A JSON string with this field:
  - date: String — the day to read, formatted `yyyy-MM-dd` (e.g. `2026-06-11`).

The tool returns JSON like `{"events":[{"title":...,"description":...,
"begin_time":...,"end_time":...}]}`.

Then:
1. If there are no events, say the day looks clear.
2. Otherwise list them in time order, each as "9:00 AM – 9:30 AM — Title".
   Convert the 24-hour times to a friendly format.
3. If the user asked whether they're free at a time, answer that directly
   based on the events.

Don't invent events that aren't in the returned data.
