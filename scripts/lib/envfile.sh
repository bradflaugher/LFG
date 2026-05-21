# scripts/lib/envfile.sh — small helpers shared between bootstrap.sh,
# update.sh, and the CLI dispatcher.

# Print an env file with secrets redacted. Keys whose names match
# TOKEN|KEY|SECRET|PASSWORD are shown as [REDACTED].
env_show_redacted() {
  awk '
    /^[[:space:]]*#/ { next } /^[[:space:]]*$/ { next }
    { eq = index($0, "="); if (eq == 0) { print; next }
      key = substr($0, 1, eq - 1); sub(/^[[:space:]]+/, "", key)
      if (key ~ /TOKEN|KEY|SECRET|PASSWORD|PASSWD/) printf "%s=[REDACTED]\n", key; else print }
  ' "$1"
}

# Helpers for TTY-gated colored output. Sourced by bootstrap.sh + update.sh.
if [[ -t 1 && "${TERM:-}" != "dumb" ]]; then
  c_reset=$'\033[0m'; c_dim=$'\033[2m'; c_red=$'\033[0;31m'
  c_green=$'\033[0;32m'; c_yellow=$'\033[0;33m'; c_cyan=$'\033[0;36m'; c_bold=$'\033[1m'
else
  c_reset=''; c_dim=''; c_red=''; c_green=''; c_yellow=''; c_cyan=''; c_bold=''
fi

say()  { printf '%s\n' "$*"; }
info() { printf '%s» %s%s\n' "$c_dim" "$*" "$c_reset"; }
step() { printf '\n%s▸ %s%s\n' "$c_bold" "$*" "$c_reset"; }
ok()   { printf '%s✓ %s%s\n' "$c_green" "$*" "$c_reset"; }
warn() { printf '%s! %s%s\n' "$c_yellow" "$*" "$c_reset" >&2; }
die()  { printf '%s✗ %s%s\n' "$c_red" "$*" "$c_reset" >&2; exit 1; }
ask()  { printf '%s?%s %s ' "$c_cyan" "$c_reset" "$*" >&2; }

genbase64() { openssl rand -base64 "$1" | tr -d '=\n' | tr '/+' '_-'; }
genhex()    { openssl rand -hex "$1"; }
