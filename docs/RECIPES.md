# LFG Recipes — what it's actually for

LFG isn't here to write your slide decks better than Claude. It's the AI you open when the thing you're dealing with **cannot leave your phone**, or when every cloud model starts with "As a helpful assistant, I must decline..."

Everything here runs fully on-device. Open **Skills**, toggle the ones the job needs, talk to it like a person who owes you nothing.

> **Two models, two jobs.** Stock Gemma for vision (Confidential Desk / photo docs). Abliterated models for anything you wouldn't say in front of HR. Both in the model manager.

---

## 🛡️ Sanitize it before you trust the cloud

**Skill:** `redact` *(on by default)*

The killer move: LFG as the "oh god don't let them see my name" filter before you hand anything to the cloud.

> "Redact this so I can paste it into ChatGPT: *Hi, it's Jane Doe
> (jane@acme.com, 555-0199), my account 4012-8899 was double-charged…*"

You get back `Hi, it's [NAME] ([EMAIL], [PHONE]), my account [ID] was
double-charged…` plus a map of what it pulled. Now a powerful cloud model can
help with the *problem* without ever seeing *who you are*.

---

## 📄 Understand sensitive paperwork — nothing uploaded

**Skill:** `explain-document` *(stock model, can read a photo)*

Medical results, a lease, a legal notice, an insurance EOB, a tax letter — the
stuff you most want explained and least want to upload anywhere.

> *(photograph the document)* "What does this actually mean, and is there
> anything I need to do?"

Plain-language summary, what stands out, deadlines/amounts, and what to ask your
doctor/lawyer/accountant — entirely on the device.

---

## 🎣 "Is this a scam?"

**Skill:** `scam-check` *(on by default)*

Stop forwarding suspicious texts to the family group chat. Check them privately.

> "Is this legit? *USPS: your package is held, pay a $1.99 fee at
> usps-trackingsupport.com within 12h.*"

Verdict, the exact tells (look-alike domain, fake urgency, the ask), and what to
do — without sending the phish anywhere.

---

## 👁️ What is this app *actually* taking?

**Skill:** `privacy-lens` *(on by default)*

> *(paste a privacy policy / ToS)* "What do they collect and what are the red
> flags?"

Ten pages of legalese → what they collect, who they share it with, the 🚩 clauses
(broad content licenses, data-broker sharing, no real opt-out), and a bottom
line. The policy never leaves your phone.

---

## 💬 The answer you won't get from a cloud bot

**Skill:** `straight-answer` *(off by default — turn it on; best with an Abliterated model)*

Frank, judgment-free help with the sensitive, awkward, or taboo-but-legitimate —
health, sexuality, mental health, harm reduction, money, the law's grey areas —
no lectures, no reflexive "consult a professional", no refusals.

> "Give it to me straight, no disclaimers: …"

This is why the fucking thing exists.

---

## 🎭 Rehearse the conversation you're dreading

**Skill:** `roleplay` *(off by default; best with an Abliterated model)*

> "Play my manager and push back hard — I want to practice asking for a raise."
> *(…)* "Pause — how did that land, and what's a stronger opening line?"

Practice a breakup, a negotiation, coming out, a tough confrontation, with a
partner that reacts realistically and coaches you — and stays on your phone, not
in a cloud history tied to your name. Also does immersive character roleplay.

---

## ✍️ Write the fiction the cloud models neuter on sight

**Skill:** `unfiltered-muse` *(off by default; best with an Abliterated model)*

> "Co-write a noir scene — morally grey narrator, real violence, no punches
> pulled."

A craft-focused collaborator that doesn't sanitize dark themes or break
character, and gives honest editing instead of flattery.

---

## ⚖️ Stress-test your own thinking

**Skill:** `devils-advocate` *(off by default; best with an Abliterated model)*

> "Steelman the strongest case *against* the decision I just made."

A sparring partner that will genuinely argue any side — including ones a cloud
model refuses to take — to find the holes before reality does.

---

## 📓 A diary that truly never leaves the device

**Skills:** `private-journal`, `mood-tracker`

> "Journal that today was long but I'm proud I went for a run."
> *(later)* "How have I been this week?"

On-device entries, gentle non-judgmental reflection, no account, no sync. The
thing "AI journaling" apps promise but route through someone's cloud.

---

## 💸 Your numbers, crunched locally

**Skill:** `budget-tracker`

> "I spent $42 on groceries." … "Show me this month's spending."

A private ledger with an inline chart — your finances never touch a server.

---

## ✈️ Off the grid

**Skills:** `translator`, `password-generator`, `set-reminder`, `whats-on-my-calendar`

> "How do I ask for the train station in Portuguese?" *(offline)*

> "Generate three strong 20-character passwords." *(never ask a cloud AI for a
> password — make them on the device)*

> "Remind me to call the clinic tomorrow at 9." *(calendar skills are off by
> default — enable them in the Skills sheet)*

---

## Putting it together

Because the model picks skills on its own, you can chain them:

- **Safe escalation:** *"Redact this support thread, then explain what my options
  are."* → `redact` + `explain-document`, and you leave with a sanitized version
  ready for a bigger model.
- **Don't get got:** *"Is this email a scam, and what does its 'unsubscribe'
  link's privacy policy actually do?"* → `scam-check` + `privacy-lens`.

---

Want to build your own? A skill is just a folder with a `SKILL.md` — see
[SKILLS.md](./SKILLS.md). The bundled skills are copy-paste templates for every
pattern.
