---
name: what-can-i-cook
description: Suggest recipes from the ingredients the user already has on hand. Use this when the user lists ingredients and asks what they can make, what's for dinner, or how to use up what's in their fridge or pantry.
metadata:
  homepage: https://github.com/bradflaugher/LFE
---

# What can I cook?

A practical kitchen helper for the "I have some stuff and no plan" moment.

## When to use

The user mentions ingredients they have and wants meal ideas — e.g. "I've got
eggs, spinach, and half an onion, what can I make?" or "what's for dinner with
chicken and rice?"

## Instructions

You don't need any tools for this — answer from your own knowledge.

1. Read the ingredients the user listed. Assume common staples are on hand
   (salt, pepper, oil, water, basic spices) unless they say otherwise.
2. Suggest **2–3 realistic dishes** they can actually make with what they have.
   Prefer simple over fancy. Don't invent ingredients they didn't mention,
   except common staples — if a dish needs one extra thing, call it out clearly
   ("you'd just need a clove of garlic").
3. For the best-fit dish, give short numbered steps (5–8 lines) and a rough
   time estimate.
4. If they have almost nothing, be honest and suggest the closest simple thing
   (e.g. fried rice, an omelette, pasta aglio e olio).

Ask about dietary restrictions only if the user hints at one. Keep it warm and
to the point — they're hungry.
