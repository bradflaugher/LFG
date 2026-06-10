---
name: currency-convert
description: Convert an amount between world currencies at the latest exchange rate. Use when the user wants to convert money — "how much is 50 euros in dollars", "convert 1000 yen to GBP". Needs an internet connection.
metadata:
  homepage: https://github.com/bradflaugher/LFE
---

# Currency converter

Convert money using up-to-date reference rates (European Central Bank data via
the free, key-less Frankfurter API). Requires a network connection.

## Instructions

Call the `run_js` tool with these parameters:

- script name: index.html
- data: A JSON string with these fields:
  - amount: Number — how much to convert (e.g. 50).
  - from: String — the source currency as a 3-letter ISO code (e.g. "EUR",
    "USD", "JPY", "GBP"). Map names like "euros" → "EUR" yourself.
  - to: String — the target currency, same format.

The script returns a `result` string with the converted amount and the rate
used. Relay it to the user. If it returns an `error` (e.g. unsupported currency
or no connection), explain that plainly — supported currencies are the major
ones tracked by the ECB.
