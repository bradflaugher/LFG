---
name: read-webpage
description: Fetch and read the main text or links from a webpage URL (including paywalled sites if the user is signed in via Settings → Browser Session). Use this when the user asks to read, summarize, extract info, or list links from a URL.
---

# Web Reader

This skill lets you read content directly from the web using on-device web scraping. It uses two dedicated tools to interact with a page:

- `fetchArticle` to retrieve the main text and title of any page (cleaned up via Readability.js).
- `fetchLinks` to retrieve a list of links (hrefs and text) from a page.

Because these tools use a local WebView, cookies from your active browser session (configured via Settings → Browser Session) are automatically attached. This allows you to read articles from paid subscriptions (like NYT, WSJ, Bloomberg) if you are logged in.

## How to use

1. **Read or Summarize a specific page:**
   When the user provides a URL and asks to summarize it, read it, or answer questions about it, call the `fetchArticle` tool with the URL.
   * `fetchArticle` returns a JSON string under the `article` key: `{"title": "...", "text": "...", "url": "..."}` or `{"error": "..."}`.
   * Based on the content, summarize it or answer the user's specific questions.

2. **List links from a homepage or portal:**
   When the user wants to find stories/links on a site's homepage (e.g. "what's on the front page of news site X?"), call `fetchLinks` with the URL.
   * `fetchLinks` returns a JSON string under the `links` key containing the links on the page.
   * Present the relevant links to the user so they can select which page to read next using `fetchArticle`.

3. **Handle paywalls and errors:**
   If the tool returns an error, explain what happened in a single, helpful sentence. If the content appears to be blocked by a paywall or sign-in wall, suggest logging in via Settings → Browser Session first.

## Examples

- "Summarize this page: https://example.com/blog-post"
- "What are the latest headlines on https://news-portal.com?"
- "What does this article say about interest rates? https://wsj.com/..."
- "Extract the key figures from https://finance-report.org"

## Privacy note

All network fetches and rendering happen entirely on your phone via a local, hidden WebView. No third-party servers see the URL or your session cookies.
