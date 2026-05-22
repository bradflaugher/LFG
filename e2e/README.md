# LFG E2E test

Single shell script that exercises the full live path:
`POST /tasks` → runner → podman → in-container `lfg run --inside` →
hyper API → agent loop → task store `success`.

## Run locally

```sh
HYPER_API_KEY=sk-hyper-... bash e2e/run-e2e.sh
```

That's it. The script builds `bin/lfg`, pulls the sandbox image (if not
already present), starts `lfg serve` on `127.0.0.1:8280`, submits a
trivial `echo` task, and polls until the task reaches `success` (or
fails). Logs and state land in `e2e/.state/`.

Without `HYPER_API_KEY` set, the script exits 0 with a skip notice — the
GHA workflow follows the same convention.

## Tuning knobs

| Env var             | Default                          | Purpose                                                  |
| ------------------- | -------------------------------- | -------------------------------------------------------- |
| `HYPER_API_KEY`     | *(required)*                     | Gate. Without it, the script skips.                      |
| `HYPER_URL`         | `https://hyper.charm.land`       | Override the hyper proxy URL.                            |
| `LFG_MODEL`         | `hyper:kimi-k2.6`                | Provider:model the agent runs under.                     |
| `LFG_PORT`          | `8280`                           | Loopback port the test server binds.                     |
| `LFG_TIMEOUT`       | `180`                            | Max seconds to wait for task success.                    |
| `LFG_SANDBOX_IMAGE` | `ghcr.io/bradflaugher/box:latest` | Sandbox image. Pulled if not already present.            |
| `LFG_MAX_STEPS`     | `5`                              | Cap the agent's tool-call loop so the test stays bounded.|

## CI

`.github/workflows/e2e.yml` runs this on every push and PR. The job is
skipped (not failed) on forks or branches where the `HYPER_API_KEY`
secret is unavailable.
