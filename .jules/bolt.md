## 2024-06-25 - Avoid UTF-8 decoding overhead in tight text parsing loops
**Learning:** `for i, r := range s` performs UTF-8 decoding on every rune. When parsing raw text streams (like bash output chunks up to 64KB) for simple ASCII characters like newline (`\n`), this adds unnecessary overhead compared to checking bytes directly.
**Action:** Use `strings.IndexByte(s, '\n')` instead of range loops over strings when searching for single ASCII characters to dramatically improve performance by skipping UTF-8 decoding.
