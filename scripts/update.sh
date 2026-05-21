#!/usr/bin/env bash
# scripts/update.sh — git pull + rebuild + restart, atomic-swap pattern.
#
# Build in a staging dir; only swap binaries into place once the build
# succeeds. Preserves .env.local and data/. Refreshes the sandbox image
# during the planned downtime.

set -euo pipefail

SRC_DIR="${SRC_DIR:-/opt/lfg-src}"
APP_DIR="${APP_DIR:-/opt/lfg}"
APP_USER="${APP_USER:-lfg}"

. "$APP_DIR/scripts/lib/envfile.sh" 2>/dev/null || . "$SRC_DIR/scripts/lib/envfile.sh"

[[ $EUID -eq 0 ]] || die "run as root: sudo lfg update"
[[ -d "$SRC_DIR/.git" ]] || die "no git checkout at $SRC_DIR"
[[ -d "$APP_DIR" ]]      || die "no install at $APP_DIR (skip bootstrap?)"

# Fetch
step "1/4  Fetching latest from $SRC_DIR"
cd "$SRC_DIR"
git config --global --add safe.directory "$SRC_DIR" 2>/dev/null || true
before_sha="$(git rev-parse HEAD)"

if [[ "${LFG_UPDATE_NO_PULL:-0}" == "1" ]]; then
  after_sha="$before_sha"
  ok "rebuild-only mode — building ${after_sha:0:12}"
else
  git fetch --quiet origin
  current_branch="$(git rev-parse --abbrev-ref HEAD)"
  if [[ -n "${LFG_UPDATE_BRANCH:-}" ]]; then target_branch="$LFG_UPDATE_BRANCH"
  elif [[ "$current_branch" != "HEAD" ]]; then target_branch="$current_branch"
  else target_branch="$(git rev-parse --abbrev-ref origin/HEAD | sed 's|^origin/||')"; warn "HEAD detached — using '$target_branch'"; fi
  target_ref="origin/$target_branch"
  after_sha="$(git rev-parse "$target_ref")"

  if [[ "$before_sha" == "$after_sha" ]]; then ok "already on ${after_sha:0:12}"; exit 0; fi

  printf '%s  incoming commits:%s\n' "$c_dim" "$c_reset"
  git --no-pager log --oneline --no-decorate "${before_sha}..${after_sha}" | sed 's/^/    /'

  if [[ "${LFG_UPDATE_YES:-0}" != "1" ]]; then
    count="$(git rev-list --count "${before_sha}..${after_sha}")"
    printf '%s?%s Apply %s%d%s commits? %s(y/N)%s ' "$c_cyan" "$c_reset" "$c_bold" "$count" "$c_reset" "$c_dim" "$c_reset"
    read -r ans; [[ "${ans,,}" =~ ^(y|yes)$ ]] || { warn "cancelled"; exit 1; }
  fi

  if git show-ref --quiet --verify "refs/heads/$target_branch"; then
    git checkout --quiet "$target_branch"
    git merge --ff-only "$target_ref" || die "$target_branch diverged from $target_ref"
  else
    git checkout --quiet -b "$target_branch" "$target_ref"
  fi
fi

# Build in a staging dir.
step "2/4  Building staged binary"
STAGING="$(mktemp -d)"
trap 'rm -rf "$STAGING"' EXIT
rsync -a --delete --exclude='/.git' "$SRC_DIR/" "$STAGING/"
chown -R "$APP_USER:$APP_USER" "$STAGING"
sudo -u "$APP_USER" -H bash -c "
  set -euo pipefail
  cd '$STAGING'
  GOTOOLCHAIN=auto go build -ldflags '-X main.Version=$after_sha' -o '$STAGING/bin/lfg' ./cmd/lfg
"
ok "build complete"

# Stop service, refresh image, swap, restart.
step "3/4  Swapping in"
systemctl stop lfg.service || true

SANDBOX_IMAGE="$(grep -E '^LFG_SANDBOX_IMAGE=' "$APP_DIR/.env.local" 2>/dev/null | tail -n1 | cut -d= -f2- | tr -d '"' || true)"
[[ -n "$SANDBOX_IMAGE" ]] || SANDBOX_IMAGE="ghcr.io/bradflaugher/box:latest"

# Reset rootless podman pause so the next pull doesn't inherit the
# stopped service's hardened namespace.
sudo -u "$APP_USER" -H podman system migrate >/dev/null 2>&1 || true

info "refreshing sandbox image $SANDBOX_IMAGE"
if ! ( cd "$APP_DIR" && sudo -u "$APP_USER" -H podman pull "$SANDBOX_IMAGE" >/dev/null ); then
  warn "podman pull failed — continuing with whatever's cached"
fi

# Sync source mirror, install binary, install unit + CLI.
rsync -a --delete --exclude='/.git' --exclude='/data' "$SRC_DIR/" "$APP_DIR/source/"  >/dev/null 2>&1 || true
install -o "$APP_USER" -g "$APP_USER" -m 0755 "$STAGING/bin/lfg" "$APP_DIR/bin/lfg"
install -m 0644 "$STAGING/deploy/lfg.service" /etc/systemd/system/
install -m 0755 "$STAGING/deploy/lfg-cli"     /usr/local/bin/lfg
# Refresh scripts/ so future updates pick up bug fixes.
rsync -a --delete "$STAGING/scripts/" "$APP_DIR/scripts/"
systemctl daemon-reload
systemctl start lfg.service
ok "service restarted"

# Health
step "4/4  Health check"
for i in $(seq 1 15); do
  curl -fsS http://127.0.0.1:8080/healthz >/dev/null 2>&1 && { ok "healthy"; break; }
  sleep 1
  [[ "$i" == "15" ]] && die "lfg didn't come back — check: journalctl -u lfg -n 50"
done

say
printf '%s═══════════════════════════════════════════════%s\n' "$c_green" "$c_reset"
printf '%s ✓ Updated %s → %s%s\n' "$c_bold" "${before_sha:0:12}" "${after_sha:0:12}" "$c_reset"
printf '%s═══════════════════════════════════════════════%s\n' "$c_green" "$c_reset"
say
say "  Logs: lfg logs   Roll back: cd $SRC_DIR && sudo git checkout $before_sha && sudo lfg update"
