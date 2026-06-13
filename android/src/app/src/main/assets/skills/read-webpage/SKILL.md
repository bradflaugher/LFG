---
name: read-webpage
description: Fetch and read main text or links, click links/buttons, or display an interactive WebView of a webpage/URL to the user. Use this when the user asks to read, summarize, extract info, list links, click links/buttons, or view/interact with a webpage.
---

# Web Reader

This skill lets you read content directly from the web or show interactive web pages to the user. It uses four dedicated tools:

- `fetchArticle` to retrieve the main text and title of any page (cleaned up via Readability.js).
- `fetchLinks` to retrieve a list of links (hrefs and text) from a page.
- `clickAndReadWebpage` to load a URL, click a specific button or link (via CSS selector), and return the updated article text.
- `showWebpage` to display an interactive WebView of a webpage inline in the chat so the user can scroll, click, or log in.

Because these tools use a local WebView, cookies from your active browser session (configured via the Web Browser icon in the chat) are automatically attached. This allows you to read articles from paid subscriptions (like NYT, WSJ, Bloomberg) if you are logged in.

## How to use

1. **Read or Summarize a specific page:**
   When the user provides a URL and asks to summarize it, read it, or answer questions about it, call the `fetchArticle` tool with the URL.
   * `fetchArticle` returns a JSON string under the `article` key: `{"title": "...", "text": "...", "url": "..."}` or `{"error": "..."}`.
   * Based on the content, summarize it or answer the user's specific questions.

2. **List links from a homepage or portal:**
   When the user wants to find stories/links on a site's homepage (e.g. "what's on the front page of news site X?"), call `fetchLinks` with the URL.
   * `fetchLinks` returns a JSON string under the `links` key containing the links on the page.
   * Present the relevant links to the user so they can select which page to read next using `fetchArticle`.

3. **Click links or buttons dynamically:**
   When the user asks to click a button, link, expander, or tab on a webpage (e.g., "click 'Read More'", "expand comments", "click the next page button"), call `clickAndReadWebpage` with the URL and the CSS selector of the element to click. This loads the page, clicks the element, waits for dynamic updates, and returns the updated article text.

4. **Display or Interact with a webpage:**
   When the user wants to view/browse a webpage directly, or if they need to manually interact with/sign in to a page, call the `showWebpage` tool with the URL. This embeds an interactive WebView inline in the chat.

5. **Handle paywalls and errors:**
   If the tool returns an error, explain what happened in a single, helpful sentence. If the content appears to be blocked by a paywall or sign-in wall, suggest logging in via the Web Browser button (top right of the chat) first.

## Examples

- "Summarize this page: https://example.com/blog-post"
- "What are the latest headlines on https://news-portal.com?"
- "What does this article say about interest rates? https://wsj.com/..."
- "Extract the key figures from https://finance-report.org"
- "Open https://github.com so I can browse it"
- "Show me https://nytimes.com"
- "Click on the button with class 'load-more' on https://example.com/comments"
- "Expand the first article on https://news-site.com"

## Privacy note

All network fetches, rendering, and interactions happen entirely on your phone via a local WebView. No third-party servers see the URL or your session cookies.


