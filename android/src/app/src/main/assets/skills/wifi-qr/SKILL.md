---
name: wifi-qr
description: Generate a QR code that lets guests join a Wi-Fi network by scanning it with their phone camera. Use when the user wants to share their Wi-Fi — "make a QR code for my wifi", "let guests connect to my network".
metadata:
  homepage: https://github.com/bradflaugher/LFE
---

# Wi-Fi QR code

Turn a Wi-Fi name and password into a scannable QR code. Anyone who points their
phone camera at it gets a "join network" prompt — no typing the password out
loud. Generated entirely on-device.

## Instructions

Call the `run_js` tool with these parameters:

- script name: index.html
- data: A JSON string with these fields:
  - ssid: String — the network name (required).
  - password: String — the Wi-Fi password. Omit or leave empty for an open
    network.
  - encryption: String (optional) — "WPA" (default, also covers WPA2/WPA3),
    "WEP", or "nopass" for open networks.
  - hidden: Boolean (optional) — true if the network doesn't broadcast its name.

The script returns a `result` message and an `image` (the QR code) that renders
in the chat. Tell the user to scan it with their phone's camera to join.

> Tip: this works offline, and the password stays on the device — nothing is
> uploaded.
