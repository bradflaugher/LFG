# LFE Recipes — what to actually do with it

LFE is a private AI agent that lives on your phone and can *do things*, not just
chat. Every example below runs on-device with the bundled skills — open the
**Skills** sheet (the ⚙️ in the chat), switch on the ones a recipe needs, and
just talk to it. Nothing leaves your phone unless a skill explicitly fetches
from the web.

> New here? The fastest "oh, neat" moment is the **Wi-Fi QR** or **tip splitter**
> below — both work offline and take one sentence.

---

## 📶 Share your Wi-Fi without reading the password aloud

**Skills:** `wifi-qr`

> "Make a Wi-Fi QR code for network *Cabin-5G*, password *trailmix2026*."

You get a QR code right in the chat. Guests point their camera at it and tap
"join" — no spelling out `trailmix2026` three times. Screenshot it and stick it
on the fridge.

---

## 🧾 Split the check at dinner

**Skills:** `tip-split`

> "Split $128.40 four ways with a 20% tip, round each person up."

> "What's 18% on $54?"

Instant per-person number while everyone's reaching for their phones. Works in
airplane mode.

---

## ✈️ Travel money on the fly

**Skills:** `currency-convert`

> "How much is 7,500 yen in dollars?"

> "I'm looking at a €45 menu — what's that in pounds?"

Live reference rates (needs a connection). Pair with **tip-split** abroad:
*"Split this 60-euro bill 3 ways and tell me each share in USD."*

---

## 📝 Pocket scratchpad that actually remembers

**Skills:** `quick-note`

> "Note that I parked on level 3, section D."

> "Remember: wine for Saturday, gift for Mom, call the bank."

> *(later)* "What were my notes?"

Stored privately on the device — no account, no cloud, works offline. Great for
parking spots, locker combos, the thing you'll definitely forget by the time you
reach the car.

---

## 💬 Write the awkward message for me, then send it

**Skills:** `proofread` + `text-message` (or `send-email`)

> "Rewrite this to my landlord politely: *the heating's been broken for a week
> and I want it fixed*. Then text it to 555-0142."

LFE polishes the tone on-device, then hands the finished message to your SMS app
with everything pre-filled. You read it and hit send. Swap in `send-email` for
the same trick over email.

---

## ⏰ Remind me, hands-free

**Skills:** `set-reminder` (+ `whats-on-my-calendar`)

> "Remind me to take the chicken out of the freezer at 5pm."

> "Add dentist Thursday at 9:30."

> "What's on my calendar tomorrow?"

It works out the date from "tomorrow"/"Thursday" and opens your calendar
pre-filled. The calendar reader can tell you if you're actually free at 2pm
before you say yes to something.

---

## 🍳 "What can I make with what's in the fridge?"

**Skills:** `what-can-i-cook`

> "I've got eggs, half an onion, some cheddar, and bread. Dinner ideas?"

Two or three realistic dishes and quick steps for the best one — entirely from
the model's own knowledge, no recipe site required.

---

## 📰 Read it for me

**Skills:** `summarize-article`

> "Summarize https://example.com/long-investigation"

The article is fetched and de-cluttered *on the phone* (no middleman server sees
what you read), then boiled down to the title, a few bullets, and the takeaway.

---

## 🔐 Strong passwords + quick utilities, offline

**Skills:** `password-generator`, `calculate-hash`, `unit-converter`, `qr-code`

> "Generate three 20-character passwords with symbols."

> "Convert 350°F to Celsius."

> "Make a QR code for https://my.link/rsvp"

The everyday calculator-drawer of the suite. All offline.

---

## 🌍 Quick facts without a browser

**Skills:** `query-wikipedia`, `translator`

> "Give me the one-paragraph version of the Treaty of Westphalia."

> "How do I say 'where's the train station?' in Portuguese?"

---

## 📊 Personal dashboards that stay on your phone

**Skills:** `mood-tracker`, `budget-tracker`

> "Log my mood as a 7 — slept well, busy day."

> "I spent $42 on groceries."  …  "Show me this month's spending."

These keep a private on-device history and draw a little chart inline. Your data
never leaves the phone.

---

## Putting it together (multi-skill combos)

Because the model picks skills on its own, you can chain them in one ask:

- **Night out:** *"Split this $96 bill 3 ways with 20% tip, and remind me to
  Venmo Sam tomorrow morning."* → `tip-split` + `set-reminder`
- **Trip prep:** *"Convert $200 to euros, and make a note of my hotel
  confirmation 8841-22."* → `currency-convert` + `quick-note`
- **Inbox zero:** *"Proofread this and email it to dana@work.com: …"* →
  `proofread` + `send-email`

---

Want to build your own? Skills are just a folder with a `SKILL.md` — see
[SKILLS.md](./SKILLS.md). If you make something good, the bundled skills in
`android/src/app/src/main/assets/skills/` are copy-paste templates for every
pattern.
