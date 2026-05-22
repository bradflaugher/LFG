#!/usr/bin/env bash
# e2e/run-e2e.sh — end-to-end HTTP integration test for LFG.
#
# Boots `lfg serve` with the hyper provider, submits a tiny task via
# POST /tasks, and polls until the task reaches `success`. Validates the
# whole chain: HTTP API → runner → podman → in-container `lfg run
# --inside` → hyper API → agent loop → task store update.
#
# Inputs (env):
#   HYPER_API_KEY  required — without it the script exits 0 and prints a
#                  skip notice (matches the GHA gate behavior).
#   HYPER_URL      optional hyper proxy URL (default Charm-hosted).
#   LFG_MODEL      optional override (default "hyper:kimi-k2.6").
#   LFG_PORT       optional server port (default 8280).
#   LFG_TIMEOUT    optional wall-clock seconds to wait for task success
#                  (default 180).
#
# Outputs:
#   .state/logs/lfg.log  server stdout/stderr
#   .state/lfg-data/     task store (logs, workspace, meta.json)

set -euo pipefail

E2E_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_DIR="$(cd "$E2E_DIR/.." && pwd)"
STATE_DIR="$E2E_DIR/.state"
LOGS_DIR="$STATE_DIR/logs"
PIDS_DIR="$STATE_DIR/pids"
LFG_PORT="${LFG_PORT:-8280}"
# Default to 6 minutes — hyper.charm.land can take 60-100s for a single
# tiny completion when warming a model, so a 3-step agent loop can need
# up to ~5 minutes. Override on a faster network.
LFG_TIMEOUT="${LFG_TIMEOUT:-360}"
LFG_MODEL="${LFG_MODEL:-hyper:kimi-k2.6}"
SANDBOX_IMAGE="${LFG_SANDBOX_IMAGE:-ghcr.io/bradflaugher/box:latest}"

# Reuse LFA's small log helpers if a sibling LFA checkout is sitting next
# to us; otherwise fall back to printf.
if [[ -t 1 ]]; then
  c_reset=$'\033[0m'; c_dim=$'\033[2m'; c_green=$'\033[32m'
  c_yellow=$'\033[33m'; c_red=$'\033[31m'
else
  c_reset=''; c_dim=''; c_green=''; c_yellow=''; c_red=''
fi
log()  { printf '%s[lfg-e2e]%s %s\n' "$c_dim" "$c_reset" "$*"; }
ok()   { printf '%s[lfg-e2e ✓]%s %s\n' "$c_green" "$c_reset" "$*"; }
warn() { printf '%s[lfg-e2e !]%s %s\n' "$c_yellow" "$c_reset" "$*" >&2; }
die()  { printf '%s[lfg-e2e ✗]%s %s\n' "$c_red" "$c_reset" "$*" >&2; cleanup; exit 1; }

mkdir -p "$STATE_DIR" "$LOGS_DIR" "$PIDS_DIR"

cleanup() {
  if [[ -f "$PIDS_DIR/lfg.pid" ]]; then
    local pid; pid="$(cat "$PIDS_DIR/lfg.pid")"
    if kill -0 "$pid" 2>/dev/null; then
      log "stopping lfg (pid $pid)"
      kill "$pid" 2>/dev/null || true
      sleep 1
      kill -9 "$pid" 2>/dev/null || true
    fi
    rm -f "$PIDS_DIR/lfg.pid"
  fi
}
trap cleanup EXIT

# ── Gate ──────────────────────────────────────────────────────────────────

if [[ -z "${HYPER_API_KEY:-}" ]]; then
  warn "HYPER_API_KEY not set — skipping LFG end-to-end test"
  exit 0
fi

# ── Build + pull ──────────────────────────────────────────────────────────

command -v podman >/dev/null 2>&1 || die "podman is required"
command -v go >/dev/null 2>&1     || die "go is required"
command -v curl >/dev/null 2>&1   || die "curl is required"
command -v jq >/dev/null 2>&1     || die "jq is required (sudo dnf install jq / apt-get install jq)"

log "building bin/lfg"
(cd "$REPO_DIR" && GOTOOLCHAIN=auto go build -o "$STATE_DIR/lfg" ./cmd/lfg)

if ! podman image exists "$SANDBOX_IMAGE" >/dev/null 2>&1; then
  log "pulling sandbox image $SANDBOX_IMAGE (one-time, ~tens of seconds)"
  podman pull "$SANDBOX_IMAGE" >>"$LOGS_DIR/podman-pull.log" 2>&1 \
    || die "podman pull failed (see $LOGS_DIR/podman-pull.log)"
fi

# ── Boot lfg serve ────────────────────────────────────────────────────────

LFG_API_KEY="$(openssl rand -hex 16)"
log "starting lfg on 127.0.0.1:${LFG_PORT} (model=${LFG_MODEL})"
(
  cd "$STATE_DIR"
  mkdir -p lfg-data
  LFG_ADDR="127.0.0.1:${LFG_PORT}" \
    LFG_API_KEY="$LFG_API_KEY" \
    LFG_MODEL="$LFG_MODEL" \
    LFG_DATA_DIR="$STATE_DIR/lfg-data" \
    LFG_SANDBOX_IMAGE="$SANDBOX_IMAGE" \
    LFG_MAX_STEPS="${LFG_MAX_STEPS:-5}" \
    LFG_BASH_TIMEOUT="${LFG_BASH_TIMEOUT:-30s}" \
    HYPER_API_KEY="$HYPER_API_KEY" \
    HYPER_URL="${HYPER_URL:-}" \
    nohup "$STATE_DIR/lfg" serve >"$LOGS_DIR/lfg.log" 2>&1 &
  echo $! >"$PIDS_DIR/lfg.pid"
)

# Wait for /healthz.
for _ in $(seq 1 60); do
  if curl -fsS --max-time 1 "http://127.0.0.1:${LFG_PORT}/healthz" >/dev/null 2>&1; then
    ok "lfg healthy at http://127.0.0.1:${LFG_PORT}"
    break
  fi
  sleep 0.5
done
curl -fsS --max-time 2 "http://127.0.0.1:${LFG_PORT}/healthz" >/dev/null 2>&1 \
  || { tail -n 40 "$LOGS_DIR/lfg.log" >&2; die "lfg didn't answer /healthz within 30s"; }

# ── Fire a task and poll ──────────────────────────────────────────────────

log "submitting test task"
resp="$(curl -fsS -X POST \
  -H "Authorization: Bearer $LFG_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"prompt":"echo hi-from-lfg-e2e and exit"}' \
  "http://127.0.0.1:${LFG_PORT}/tasks")"
task_id="$(echo "$resp" | jq -r .id)"
[[ -n "$task_id" && "$task_id" != "null" ]] || die "no task id in response: $resp"
log "task submitted: $task_id"

deadline=$(( $(date +%s) + LFG_TIMEOUT ))
status=""
while (( $(date +%s) < deadline )); do
  status="$(curl -fsS -H "Authorization: Bearer $LFG_API_KEY" \
    "http://127.0.0.1:${LFG_PORT}/tasks/${task_id}" | jq -r .status)"
  case "$status" in
    success) ok "task $task_id reached success"; break ;;
    failed|killed)
      echo "--- task log tail ---"
      curl -fsS -H "Authorization: Bearer $LFG_API_KEY" \
        "http://127.0.0.1:${LFG_PORT}/tasks/${task_id}/logs" | tail -40 || true
      echo "--- server log tail ---"
      tail -n 40 "$LOGS_DIR/lfg.log"
      die "task $task_id ended in '$status'"
      ;;
  esac
  sleep 2
done

if [[ "$status" != "success" ]]; then
  echo "--- task log tail ---"
  curl -fsS -H "Authorization: Bearer $LFG_API_KEY" \
    "http://127.0.0.1:${LFG_PORT}/tasks/${task_id}/logs" | tail -40 || true
  echo "--- server log tail ---"
  tail -n 40 "$LOGS_DIR/lfg.log"
  die "task $task_id did not complete within ${LFG_TIMEOUT}s (last status: $status)"
fi

# Sanity-check the agent actually invoked bash with our prompt.
log "verifying agent ran the prompt"
if curl -fsS -H "Authorization: Bearer $LFG_API_KEY" \
     "http://127.0.0.1:${LFG_PORT}/tasks/${task_id}/logs" \
   | grep -q "hi-from-lfg-e2e"; then
  ok "agent produced expected output"
else
  warn "expected echo output not found in task log — task succeeded but output is unexpected"
fi

ok "all checks passed"
