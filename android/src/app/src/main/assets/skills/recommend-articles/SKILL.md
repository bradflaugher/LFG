---
name: recommend-articles
description: Find articles the user would actually want to read. Pulls a homepage (NYT by default, or whatever sources the user has configured), scans the headline links, and ranks them against the user's stored interest profile. Use this when the user asks "what's interesting today?", "find me some good articles", "what should I read on NYT?", or similar.
---

# Recommend articles

This skill combines two tools and a tiny local store:

- **`fetchLinks(url)`** — pulls the headline anchors off a news homepage.
- **`fetchArticle(url)`** — pulls full body text once a candidate is picked.
- **`run_js` with `index.html`** — reads / writes the user's persona and
  preferred feed sources in localStorage so the user only sets them once.

The data stored is:

```json
{
  "persona": "free-form text describing interests",
  "feeds": ["https://www.nytimes.com", "https://www.wsj.com", "..."]
}
```

## Workflow

Pick the path based on what the user said.

### Path A — first-time setup, or user wants to update their persona

Trigger phrases: "set my interests to ...", "update my persona", "I'm
interested in ...".

1. Call `run_js` with action `set_persona` and the user's interest text.
2. Acknowledge in one sentence ("Got it — I'll use that for recommendations.").
3. Stop.

### Path B — add or list feed sources

Trigger phrases: "add nytimes to my feeds", "list my feeds", "use WSJ
homepage", "remove FT".

1. Call `run_js` with the appropriate action (`add_feed`, `remove_feed`,
   `list_feeds`).
2. Echo back the result in one short sentence.
3. Stop.

### Path C — recommend articles (the main path)

Trigger phrases: "what's interesting today?", "find me articles",
"recommend something to read", "anything good on NYT?".

1. Call `run_js` with action `get_state`. The result will be
   `{persona: "...", feeds: [...]}`.

2. If `persona` is empty: ask the user one sentence — "What are you
   interested in? I'll remember it for next time." — and STOP. Don't make
   recommendations yet.

3. If `feeds` is empty: use the default `["https://www.nytimes.com"]`.

4. For each feed URL, call `fetchLinks(url)`. The result will be JSON
   `{url: "...", links: [{text: "...", href: "..."}]}`. Combine all link
   lists into one candidate pool.

5. **Pick the top 5 candidates** that match the persona. Use your own
   judgement — read the link text, score it against the interest
   description. Skip obvious nav links ("Subscribe", "Log In", section
   headers like "U.S.", "Opinion").

6. Output the picks as a markdown list:

   ```
   1. **<title>** — one-sentence reason this matches the persona
      <url>
   ```

7. End with: "Want me to summarize any of these? Reply with the number."

8. Stop. Do NOT pre-emptively call `fetchArticle` — wait for the user to
   pick.

### Path D — user picks a number from Path C

1. Call `fetchArticle(url)` with the URL the user picked.
2. Summarize per the rules in the `summarize-article` skill: title, 3-5
   bullets, one-line takeaway.
3. Stop.

## Rules

- Never invent URLs. Only use ones returned by `fetchLinks`.
- Never call any tool other than `run_js`, `fetchLinks`, `fetchArticle`.
- Output ONLY the user-facing result — no preambles, no chain-of-thought,
  no "Here's what I found".
- All persona/feed data lives on-device. The skill writes via `run_js` →
  the in-app WebView's localStorage.

## Privacy

Homepage fetches use the same hidden WebView as `summarize-article`,
which means any persisted publisher cookies are attached. NYT delivers a
different (more relevant) homepage to signed-in users than to anonymous
ones, so signing in via Settings → Browser Session generally gives better
recommendations.
