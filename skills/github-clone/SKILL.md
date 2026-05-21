---
name: github-clone
description: Clone a GitHub repository into the workspace using the `gh` CLI (authenticated when GITHUB_TOKEN is forwarded). Use when the task involves working with code from a specific GitHub repo.
---

# github-clone

Clone a repository into `/workspace/<repo>` and `cd` into it.

## How

```bash
# If GITHUB_TOKEN is set, gh picks it up automatically.
gh repo clone OWNER/NAME
cd NAME
```

If `gh` is unavailable for some reason, fall back to:

```bash
git clone https://github.com/OWNER/NAME.git
cd NAME
```

Private repos require `GITHUB_TOKEN` to be forwarded into the container via `LFG_PASS_ENV=GITHUB_TOKEN`.
