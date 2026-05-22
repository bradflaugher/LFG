#!/usr/bin/env bash
# scripts/bootstrap.sh — one-shot installer for LFG.
#
# Targets Fedora 39+ / RHEL 9+ / AlmaLinux 9+.
#
# Usage:
#   sudo bash scripts/bootstrap.sh
#
# Idempotent — re-runs reuse the .env.local and skip already-completed steps.
# Pre-answer prompts non-interactively by exporting:
#   LFG_BOOTSTRAP_OPENROUTER_KEY, LFG_BOOTSTRAP_HOSTNAME,
#   LFG_BOOTSTRAP_SETUP_CADDY=y/n, LFG_BOOTSTRAP_USE_LETSENCRYPT=y/n,
#   LFG_BOOTSTRAP_LE_EMAIL, LFG_BOOTSTRAP_NON_INTERACTIVE=1.

set -euo pipefail

SRC_DIR="$(cd "$(dirname "$0")/.." && pwd)"
. "$SRC_DIR/scripts/lib/envfile.sh"

# Re-open /dev/tty when stdin is piped (so `curl | sudo bash` still prompts).
# Non-interactive mode skips the TTY requirement; the caller must supply
# every answer via LFG_BOOTSTRAP_* env vars.
if [[ "${LFG_BOOTSTRAP_NON_INTERACTIVE:-0}" != "1" ]]; then
  if [[ ! -t 0 ]]; then
    if [[ -t 1 ]]; then exec </dev/tty
    else die "bootstrap.sh needs a TTY (or set LFG_BOOTSTRAP_NON_INTERACTIVE=1). Re-run locally: sudo bash scripts/bootstrap.sh"
    fi
  fi
fi

APP_DIR="${APP_DIR:-/opt/lfg}"
APP_USER="${APP_USER:-lfg}"
SRC_TARGET="${SRC_TARGET:-/opt/lfg-src}"
CLI_PATH="/usr/local/bin/lfg"
ENV_FILE="$APP_DIR/.env.local"
SANDBOX_IMAGE="${LFG_SANDBOX_IMAGE:-ghcr.io/bradflaugher/box:latest}"
NON_INTERACTIVE="${LFG_BOOTSTRAP_NON_INTERACTIVE:-0}"

prompt() {
  local envvar="$1" label="$2" default="${3:-}" answer=""
  if [[ -n "${!envvar:-}" ]]; then printf '%s' "${!envvar}"; return; fi
  if [[ "$NON_INTERACTIVE" == "1" ]]; then
    [[ -n "$default" ]] && { printf '%s' "$default"; return; }
    die "non-interactive mode + missing answer: set $envvar"
  fi
  if [[ -n "$default" ]]; then ask "${label} ${c_dim}[${default}]${c_reset}:"
  else ask "${label}:"; fi
  read -r answer; [[ -z "$answer" ]] && answer="$default"
  printf '%s' "$answer"
}

prompt_secret() {
  local envvar="$1" label="$2" answer=""
  if [[ -n "${!envvar:-}" ]]; then printf '%s' "${!envvar}"; return; fi
  if [[ "$NON_INTERACTIVE" == "1" ]]; then die "non-interactive mode + missing secret: set $envvar"; fi
  ask "${label}:"; read -r answer; echo >&2; printf '%s' "$answer"
}

confirm() {
  local envvar="$1" q="$2" default="${3:-y}" answer=""
  if [[ -n "${!envvar:-}" ]]; then answer="${!envvar}"
  elif [[ "$NON_INTERACTIVE" == "1" ]]; then answer="$default"
  else
    local hint="y/N"; [[ "$default" == "y" ]] && hint="Y/n"
    ask "${q} ${c_dim}(${hint})${c_reset}"
    read -r answer; answer="${answer:-$default}"
  fi
  [[ "${answer,,}" =~ ^(y|yes|1|true)$ ]]
}

clear || true
cat <<EOF
${c_bold}LFG — minimal AI agent install${c_reset}
${c_dim}Fedora / RHEL 9+  •  systemd  •  rootless podman  •  optional Caddy${c_reset}

This will:
  • install system deps (git, go, podman, caddy?)
  • create a '${APP_USER}' system user + ${APP_DIR}
  • build bin/lfg
  • pull ${SANDBOX_IMAGE}
  • seed .env.local with your OpenRouter key + a fresh API key
  • install the systemd unit and (optionally) Caddy with automatic TLS

Safe to re-run.

EOF

[[ $EUID -eq 0 ]] || die "run as root: sudo bash scripts/bootstrap.sh"
[[ -f /etc/fedora-release || -f /etc/redhat-release ]] || warn "this installer targets Fedora/RHEL; dnf steps will fail elsewhere."
[[ -f "$SRC_DIR/go.mod" ]] || die "not a repo checkout (no go.mod). Clone first, then re-run."

# 1. Packages
step "1/7  Installing system dependencies"
PKGS=(git curl jq golang openssl podman bind-utils shadow-utils)
dnf install -y "${PKGS[@]}" >/dev/null
ok "installed: ${PKGS[*]}"

# 2. User + dirs
step "2/7  Preparing ${APP_DIR} + '${APP_USER}'"
if ! id -u "$APP_USER" >/dev/null 2>&1; then
  useradd --system --shell /usr/sbin/nologin --home-dir "$APP_DIR" --create-home "$APP_USER"
fi
mkdir -p "$APP_DIR/data" "$APP_DIR/bin" "$APP_DIR/.local/share/containers" "$APP_DIR/.config/containers"
chown -R "$APP_USER:$APP_USER" "$APP_DIR"

# Rootless podman prereqs.
grep -q "^${APP_USER}:" /etc/subuid 2>/dev/null || usermod --add-subuids 100000-165535 "$APP_USER"
grep -q "^${APP_USER}:" /etc/subgid 2>/dev/null || usermod --add-subgids 100000-165535 "$APP_USER"
loginctl enable-linger "$APP_USER" >/dev/null 2>&1 || true
ok "user '${APP_USER}' ready"

# 3. Source mirror so `lfg update` has a tree to git-pull.
step "3/7  Mirroring source to $SRC_TARGET"
if [[ ! -d "$SRC_TARGET/.git" ]]; then
  cp -a "$SRC_DIR" "$SRC_TARGET"
fi
chown -R root:root "$SRC_TARGET"

# 4. Config + secrets
step "4/7  Configuring the instance"
if [[ -f "$ENV_FILE" ]]; then
  info "found existing $ENV_FILE — reusing values, only asking for what's missing"
  set -a; . "$ENV_FILE"; set +a
fi

say
say "  ${c_bold}Provider${c_reset} — LFG works with any provider Fantasy supports:"
say "    openrouter • anthropic • openai • google • hyper • azure • bedrock • openai-compat • vercel"
say "  Pick one for the default; you can mix-and-match later by editing .env.local"
say "  and changing LFG_MODEL to e.g. ${c_dim}anthropic:claude-opus-4-7${c_reset} on a per-call basis."
say
PROVIDER="$(prompt LFG_BOOTSTRAP_PROVIDER "Provider" "openrouter")"

case "$PROVIDER" in
  openrouter)
    API_KEY_ENV="OPENROUTER_API_KEY"
    DEFAULT_MODEL="openrouter:moonshotai/kimi-k2"
    KEY_HINT="https://openrouter.ai/keys"
    ;;
  anthropic)
    API_KEY_ENV="ANTHROPIC_API_KEY"
    DEFAULT_MODEL="anthropic:claude-opus-4-7"
    KEY_HINT="https://console.anthropic.com/settings/keys"
    ;;
  openai)
    API_KEY_ENV="OPENAI_API_KEY"
    DEFAULT_MODEL="openai:gpt-5"
    KEY_HINT="https://platform.openai.com/api-keys"
    ;;
  google)
    API_KEY_ENV="GEMINI_API_KEY"
    DEFAULT_MODEL="google:gemini-2.5-pro"
    KEY_HINT="https://aistudio.google.com/apikey"
    ;;
  hyper)
    # Charm Hyper — managed proxy at hyper.charm.land. Get a key by
    # running `crush login` from the Crush TUI, then copy the token out
    # of crush's config, or from https://hyper.charm.land directly.
    API_KEY_ENV="HYPER_API_KEY"
    DEFAULT_MODEL="hyper:kimi-k2.6"
    KEY_HINT="https://hyper.charm.land"
    ;;
  *)
    warn "provider '$PROVIDER' isn't auto-prompted — set the matching env var in $ENV_FILE after install."
    API_KEY_ENV=""
    DEFAULT_MODEL="${PROVIDER}:CHANGE-ME"
    KEY_HINT=""
    ;;
esac

PROVIDER_KEY=""
if [[ -n "$API_KEY_ENV" ]] && [[ -z "${!API_KEY_ENV:-}" ]]; then
  say
  [[ -n "$KEY_HINT" ]] && say "  Grab a key at ${c_cyan}${KEY_HINT}${c_reset}."
  PROVIDER_KEY="$(prompt_secret LFG_BOOTSTRAP_PROVIDER_KEY "$API_KEY_ENV")"
  [[ -n "$PROVIDER_KEY" ]] || die "$API_KEY_ENV is required for provider '$PROVIDER'"
elif [[ -n "$API_KEY_ENV" ]]; then
  PROVIDER_KEY="${!API_KEY_ENV}"
fi

LFG_API_KEY="${LFG_API_KEY:-$(genhex 32)}"
LFG_MODEL="${LFG_MODEL:-$DEFAULT_MODEL}"

HOSTNAME_ANSWER="$(prompt LFG_BOOTSTRAP_HOSTNAME "Hostname or URL (use 'localhost' for a private box)" "localhost")"

SETUP_CADDY="n"; USE_LE="n"; LE_EMAIL=""
if [[ "$HOSTNAME_ANSWER" != "localhost" && "$HOSTNAME_ANSWER" != 127.* ]]; then
  if confirm LFG_BOOTSTRAP_SETUP_CADDY "Set up Caddy + auto-TLS for ${HOSTNAME_ANSWER}?" y; then
    SETUP_CADDY="y"
    if confirm LFG_BOOTSTRAP_USE_LETSENCRYPT "Use Let's Encrypt (host must be publicly reachable on 80/443)?" y; then
      USE_LE="y"
      LE_EMAIL="$(prompt LFG_BOOTSTRAP_LE_EMAIL "LE contact email (blank to skip)" "")"
    fi
  fi
fi

# 5. Build
step "5/7  Building bin/lfg"
# -buildvcs=false: the .git dir is owned by root from step 3's copy, but
# we run go build as $APP_USER. Same fix as LFA's bootstrap.
sudo -u "$APP_USER" -H bash -c "
  cd '$SRC_TARGET'
  GOTOOLCHAIN=auto go build -buildvcs=false -ldflags '-X main.Version=\$(git -C $SRC_TARGET describe --tags --always --dirty 2>/dev/null || echo dev)' \
    -o '$APP_DIR/bin/lfg' ./cmd/lfg
"
ok "built $APP_DIR/bin/lfg"

# 6. Pull sandbox image
step "6/7  Pulling sandbox image $SANDBOX_IMAGE"
pull_log="$(mktemp)"
if ! sudo -u "$APP_USER" -H bash -c "cd '$APP_DIR' && podman pull '$SANDBOX_IMAGE'" >"$pull_log" 2>&1; then
  err="$(tail -n 20 "$pull_log")"; rm -f "$pull_log"
  die "podman pull failed:
$err"
fi
rm -f "$pull_log"
ok "image ready"

# 7. Write env, install unit + CLI, start
step "7/7  Writing env, installing systemd unit + ${CLI_PATH}"
umask 077
cat > "$ENV_FILE" <<EOF
# Auto-generated by bootstrap.sh on $(date -Iseconds)
#
# LFG_MODEL is "provider:model-id". Supported providers:
#   openrouter • anthropic • openai • google • azure • bedrock • openai-compat • vercel
# Add the matching API-key env var(s) below — only whichever providers
# you intend to call. LFG forwards any of them that are set into each
# per-task container.
LFG_MODEL="$LFG_MODEL"
LFG_API_KEY="$LFG_API_KEY"
LFG_DATA_DIR="$APP_DIR/data"
LFG_SANDBOX_IMAGE="$SANDBOX_IMAGE"
LFG_PUBLIC_HOSTNAME="$HOSTNAME_ANSWER"
LFG_ADDR=":8080"
EOF
if [[ -n "$API_KEY_ENV" && -n "$PROVIDER_KEY" ]]; then
  printf '%s="%s"\n' "$API_KEY_ENV" "$PROVIDER_KEY" >> "$ENV_FILE"
fi
chown "$APP_USER:$APP_USER" "$ENV_FILE"
chmod 0640 "$ENV_FILE"

install -m 0644 "$SRC_TARGET/deploy/lfg.service" /etc/systemd/system/
install -m 0755 "$SRC_TARGET/deploy/lfg-cli"    "$CLI_PATH"
systemctl daemon-reload
systemctl enable --now lfg.service

# Health check
for _ in $(seq 1 20); do
  curl -fsS --max-time 1 http://127.0.0.1:8080/healthz >/dev/null 2>&1 && break
  sleep 0.5
done
curl -fsS --max-time 2 http://127.0.0.1:8080/healthz >/dev/null 2>&1 \
  && ok "lfg-server is healthy" \
  || warn "lfg-server didn't answer /healthz — check 'lfg logs'"

# Optional Caddy
if [[ "$SETUP_CADDY" == "y" ]]; then
  step "Caddy"
  dnf install -y caddy >/dev/null
  tmp=$(mktemp)
  if [[ -n "$LE_EMAIL" ]]; then printf '{\n\temail %s\n}\n\n' "$LE_EMAIL" > "$tmp"; fi
  sed "s/lfg\.example\.com/$HOSTNAME_ANSWER/" "$SRC_TARGET/deploy/Caddyfile" >> "$tmp"
  if [[ "$USE_LE" != "y" ]]; then
    sed -i '/^'"${HOSTNAME_ANSWER//./\\.}"' {/a\\ttls internal' "$tmp"
  fi
  install -m 0644 "$tmp" /etc/caddy/Caddyfile
  rm -f "$tmp"
  if systemctl is-active --quiet firewalld; then
    firewall-cmd --add-service=http --permanent >/dev/null
    firewall-cmd --add-service=https --permanent >/dev/null
    firewall-cmd --reload >/dev/null
  fi
  systemctl enable --now caddy
  ok "Caddy running"
fi

say
printf '%s═══════════════════════════════════════════════%s\n' "$c_green" "$c_reset"
printf '%s ✓ LFG installed%s\n' "$c_bold" "$c_reset"
printf '%s═══════════════════════════════════════════════%s\n' "$c_green" "$c_reset"
say
if [[ "$SETUP_CADDY" == "y" ]]; then say "  URL         ${c_bold}https://${HOSTNAME_ANSWER}${c_reset}"
else say "  URL         ${c_bold}http://${HOSTNAME_ANSWER}:8080${c_reset}"; fi
say "  API key     ${c_bold}$LFG_API_KEY${c_reset}"
say "              (also stored in $ENV_FILE)"
say "  Data dir    $APP_DIR/data"
say "  Logs        ${c_dim}journalctl -fu lfg${c_reset}"
say "  CLI         ${c_dim}lfg update  •  lfg restart  •  lfg logs  •  lfg env${c_reset}"
say
say "  Try it:"
say "    ${c_dim}curl -H 'Authorization: Bearer $LFG_API_KEY' \\\\${c_reset}"
say "    ${c_dim}     -d '{\"prompt\":\"echo hello\"}' \\\\${c_reset}"
say "    ${c_dim}     http://${HOSTNAME_ANSWER}:8080/tasks${c_reset}"
say
