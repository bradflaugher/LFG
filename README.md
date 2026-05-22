# LFG

A minimal terminal AI agent. Give it a model and a task; it spawns a
rootless podman container per task, runs the agent inside with one
`bash` tool plus a skills layer ([agentskills.io](https://agentskills.io)
`SKILL.md` format), and exits.

Works with any provider [Fantasy](https://github.com/charmbracelet/fantasy)
supports — OpenRouter, Anthropic, OpenAI, Google, Azure, Bedrock, Vercel
AI, or any OpenAI-compatible endpoint. Optionally exposes a
fire-and-forget HTTP API so other tools (e.g.
[LFA](https://github.com/bradflaugher/LFA)) can kick off jobs remotely.

## Install

On a fresh Fedora / RHEL 9+ box, one line:

```sh
curl -fsSL https://raw.githubusercontent.com/bradflaugher/LFG/main/scripts/install.sh | sudo bash
```

That installs `git`, clones the repo into `/opt/lfg-src`, and runs the
bootstrap. The bootstrap asks three things: a provider key (OpenRouter
by default; pick `hyper` / `anthropic` / `openai` / ... at the prompt),
the hostname people will reach the server at, and whether to set up
Caddy + TLS. Everything else (secrets, systemd, podman, the LFG API
key) is generated.

Prefer to inspect first? Clone and run by hand:

```sh
git clone https://github.com/bradflaugher/LFG.git
cd LFG
sudo bash scripts/bootstrap.sh
```

## Run

Once installed, the binary is at `/usr/local/bin/lfg`.

```sh
# One-shot, local. Streams agent steps to stderr.
lfg run --verbose "list everything in /workspace and write a summary to /workspace/out.md"

# Fire a task at the server (bootstrap.sh prints the API key once):
curl -H "Authorization: Bearer $LFG_API_KEY" \
     -d '{"prompt":"clone bradflaugher/LFG and count the Go files"}' \
     http://localhost:8080/tasks
```

## Skills

Drop `SKILL.md` directories under `LFG_SKILLS_PATH` (colon-separated).
See [agentskills.io/specification](https://agentskills.io/specification)
for the schema. Bundled examples live in [`skills/`](./skills).

## Configuration

| Env var               | Default                                  | Notes                                                              |
| --------------------- | ---------------------------------------- | ------------------------------------------------------------------ |
| `LFG_MODEL`           | `openrouter:moonshotai/kimi-k2`          | Format: `provider:model-id`. See providers below.                  |
| `LFG_API_KEY`         | *(unset)*                                | If set, server requires Bearer auth and binds `0.0.0.0`.           |
| `LFG_DATA_DIR`        | `/opt/lfg/data`                          | Per-task workspaces + logs.                                        |
| `LFG_SANDBOX_IMAGE`   | `ghcr.io/bradflaugher/box:latest`        | Container image. See [bradflaugher/box](https://github.com/bradflaugher/box). |
| `LFG_SKILLS_PATH`     | *(none)*                                 | Colon-separated dirs scanned for SKILL.md.                         |
| `LFG_PASS_ENV`        | *(none)*                                 | Comma-separated extra env vars to forward into the container.      |
| `LFG_BASH_TIMEOUT`    | `5m`                                     | Per-call timeout for the bash tool.                                |
| `LFG_MAX_STEPS`       | `200`                                    | Cap on agent tool-call loop.                                       |

### Providers

`LFG_MODEL` is `provider:model-id`. Set the matching env var(s) in
`.env.local`; LFG forwards whichever are present into each per-task
container automatically.

| Provider        | Example `LFG_MODEL`                    | Required env vars                                        |
| --------------- | -------------------------------------- | -------------------------------------------------------- |
| `openrouter`    | `openrouter:moonshotai/kimi-k2`        | `OPENROUTER_API_KEY`                                     |
| `anthropic`     | `anthropic:claude-opus-4-7`            | `ANTHROPIC_API_KEY`                                      |
| `openai`        | `openai:gpt-5`                         | `OPENAI_API_KEY` (+ optional `OPENAI_BASE_URL`)          |
| `google`        | `google:gemini-2.5-pro`                | `GEMINI_API_KEY` *or* `GOOGLE_API_KEY`                   |
| `hyper`         | `hyper:kimi-k2.6`                      | `HYPER_API_KEY` (+ optional `HYPER_URL`)                 |
| `azure`         | `azure:gpt-5-mini`                     | `AZURE_OPENAI_API_KEY` + `AZURE_OPENAI_ENDPOINT`         |
| `bedrock`       | `bedrock:anthropic.claude-opus-4-7`    | Standard AWS chain (or `BEDROCK_API_KEY`)                |
| `openai-compat` | `openai-compat:llama-3.1-70b`          | `OPENAI_COMPAT_API_KEY` + `OPENAI_COMPAT_BASE_URL`       |
| `vercel`        | `vercel:openai/gpt-5`                  | `VERCEL_AI_API_KEY` (+ optional `VERCEL_AI_BASE_URL`)    |

[`hyper`](https://hyper.charm.land) is Charm's managed proxy — runs
Kimi, DeepSeek, GLM, Qwen, etc. The endpoint defaults to
`https://hyper.charm.land/v1`; set `HYPER_URL` to a base host
(no path) to override.

## Operating

`lfg <update|restart|logs|status|env>` — see `lfg help`.
Operational details in [DEVOPS.md](./DEVOPS.md).

