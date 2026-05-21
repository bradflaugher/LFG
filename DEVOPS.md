# LFG operations

## Layout

```
/opt/lfg/
├── bin/lfg                 the binary (replaced atomically by `lfg update`)
├── .env.local              env vars (mode 0640, owner lfg:lfg) — secrets here
├── data/tasks/<uuid>/      one per task
│   ├── meta.json           status + timestamps
│   ├── workspace/          bind-mounted into the container as /workspace
│   └── logs/run.jsonl      container stdout+stderr
└── .local/.config/         rootless podman storage (don't touch)
/opt/lfg-src/               git checkout — `lfg update` pulls + rebuilds from here
/usr/local/bin/lfg          dispatcher
/etc/systemd/system/lfg.service
```

The agent inside the container is the same binary, bind-mounted in
read-only as `/usr/local/bin/lfg`. Changing the host binary changes the
in-container behavior on the next task.

## Lifecycle

```
sudo lfg start | stop | restart | status
sudo lfg logs                 # follow journal for lfg.service
sudo lfg logs -n 200          # last 200 lines (passes args through)
sudo lfg update               # git pull + rebuild + restart (atomic swap)
sudo lfg rebuild              # rebuild current checkout (no fetch)
sudo lfg env edit             # open .env.local in $EDITOR
sudo lfg env show             # print .env.local with secrets redacted
```

`lfg update` builds in a `mktemp` staging dir, refreshes the sandbox
image during the planned downtime, then installs the new binary
atomically with `install`. If the build fails nothing on the live box
changes.

## Authoring skills

A skill is just a directory under one of the paths in `LFG_SKILLS_PATH`:

```
my-skill/
├── SKILL.md          # required; YAML frontmatter + markdown body
├── scripts/          # optional; bash/python/anything the agent can `bash`-exec
└── references/       # optional; docs the agent reads via `bash cat …`
```

Frontmatter rules (full spec: [agentskills.io/specification](https://agentskills.io/specification)):

```yaml
---
name: my-skill                  # must match the directory name; [a-z0-9-]; 1-64 chars
description: One-liner …        # ≤ 1024 chars
license: MIT                    # optional
metadata:                       # optional
  author: example
---
```

At startup the agent lists every skill's name + description in the
system prompt; when it picks one, it calls `read_skill(name)` to load
the full body and a file listing, then `bash`-executes whatever it
wants.

## Container model

Each task = one rootless podman container. The container has:

- `/workspace` — bind-mounted from `<data>/tasks/<id>/workspace`, the only
  thing that survives the container.
- `/skills/0`, `/skills/1`, … — read-only bind mounts of every entry in
  `LFG_SKILLS_PATH`.
- `/usr/local/bin/lfg` — bind-mounted host binary.
- Environment: `OPENROUTER_API_KEY`, `LFG_MODEL`, `LFG_SKILLS_PATH` (rewritten),
  plus anything you list in `LFG_PASS_ENV` (e.g. `GITHUB_TOKEN`).

The agent is **root inside the container** with no extra capability
drops or `--read-only`. Isolation comes from rootless podman + the
operator-controlled allowlist of env vars + the workspace being the
only persistent mount.

For a hardened, network-isolated image, point `LFG_SANDBOX_IMAGE` at a
different image — the default [`box`](https://github.com/bradflaugher/box)
is deliberately permissive.

## Troubleshooting

| Symptom                          | Where to look                                                      |
| -------------------------------- | ------------------------------------------------------------------ |
| `podman pull` failing            | `journalctl -u lfg -n 100`; check subuid/subgid + `enable-linger`. |
| `lfg-server: bind: permission`   | Port 8080 in use; change `LFG_ADDR` in `.env.local` and `restart`. |
| Tasks stuck "running"            | `podman ps`. `podman kill lfg-<id>` if needed.                     |
| 401 on `/tasks`                  | Verify `Authorization: Bearer $LFG_API_KEY`.                       |
| Server only listens on loopback  | `LFG_API_KEY` is unset → local-only mode. Set it and `restart`.    |
| Skill not appearing              | `lfg help` doesn't list it; check `LFG_SKILLS_PATH` + `SKILL.md` name vs directory. |

## CI

GitHub Actions in `.github/workflows/ci.yml` runs `gofmt`, `go vet`,
`go test -race`, and `go build` on every push/PR.
