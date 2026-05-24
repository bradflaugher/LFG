---
name: summarize-article
description: Fetch an article from a URL and summarize it. Works on any publicly readable page. Use this whenever the user provides a news article URL or asks "summarize this article".
---

# Summarize article

This skill uses the `fetchArticle` **tool** (not `run_js`) to retrieve an
article's title and main text via a hidden WebView, then summarizes it.

## Workflow for every article summarization request

1. **Identify the URL.** If the user message contains an `http://` or
   `https://` link, that's it. If the user only describes the article without
   a URL, ask them for the URL — do not guess.

2. **Call the `fetchArticle` tool** with the URL.

   Use the tool name `fetchArticle` literally — you **MUST NOT** call this
   with `run_js`, `runMcpTool`, or any other tool. The result will be a JSON
   string under the `article` key: either
   `{"title": "...", "text": "...", "url": "..."}` on success, or
   `{"error": "..."}` on failure.

3. **Handle errors:**
   - If the result contains `"error"`, tell the user what failed in one
     sentence. If it looks like a paywall or login wall, say so plainly — the
     skill can only read pages the device can reach without signing in.
   - Otherwise proceed to step 4.

4. **Summarize:** produce
   - A one-line headline (use the article's title verbatim).
   - 3–5 bullet points of the key facts, dates, names, and numbers.
   - One closing line summarizing the takeaway or the article's stance.

5. **Stop.** Do not run any other tool or skill afterwards. Output only the
   summary — no preambles, no "Here's the summary", no chain-of-thought.

## Examples

- "Summarize this for me: https://en.wikipedia.org/wiki/Foo_Bar"
- "What does this piece say about XYZ? https://example.com/2026/05/…"
- "tl;dr https://example.com/news/long-piece"

## Privacy note

The fetch happens entirely on-device: a hidden WebView in the LFE process
loads the URL, runs Mozilla Readability.js on the DOM, and returns text. No
intermediate server sees the URL.
