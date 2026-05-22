#!/usr/bin/env bash
# scripts/install.sh — one-line installer entrypoint for LFG.
#
# Designed to be piped from curl:
#
#   curl -fsSL https://raw.githubusercontent.com/bradflaugher/LFG/main/scripts/install.sh | sudo bash
#
# Optional env (all forwarded into bootstrap.sh):
#   LFG_INSTALL_REPO   git URL (default https://github.com/bradflaugher/LFG.git)
#   LFG_INSTALL_REF    branch/tag/sha to install (default main)
#   LFG_INSTALL_SRC    local source dir (default /opt/lfg-src)
#   plus any LFG_BOOTSTRAP_* vars (see scripts/bootstrap.sh header).
#
# The actual install logic lives in scripts/bootstrap.sh — this file
# just makes sure the repo is on disk first.

set -euo pipefail

LFG_INSTALL_REPO="${LFG_INSTALL_REPO:-https://github.com/bradflaugher/LFG.git}"
LFG_INSTALL_REF="${LFG_INSTALL_REF:-main}"
LFG_INSTALL_SRC="${LFG_INSTALL_SRC:-/opt/lfg-src}"

if [[ $EUID -ne 0 ]]; then
  cat >&2 <<EOF
LFG's installer needs root (it provisions podman, systemd, and /opt/lfg).
Re-run with sudo:

  curl -fsSL https://raw.githubusercontent.com/bradflaugher/LFG/main/scripts/install.sh | sudo bash
EOF
  exit 1
fi

# Ensure git is available. Fedora/RHEL → dnf; Debian/Ubuntu → apt-get.
if ! command -v git >/dev/null 2>&1; then
  if command -v dnf >/dev/null 2>&1; then
    dnf install -y git >/dev/null
  elif command -v apt-get >/dev/null 2>&1; then
    apt-get update -y >/dev/null
    apt-get install -y git >/dev/null
  else
    echo "couldn't auto-install git — install it manually and re-run" >&2
    exit 1
  fi
fi

if [[ -d "$LFG_INSTALL_SRC/.git" ]]; then
  echo "[install] reusing existing checkout at $LFG_INSTALL_SRC"
  git -C "$LFG_INSTALL_SRC" config --global --add safe.directory "$LFG_INSTALL_SRC" 2>/dev/null || true
  git -C "$LFG_INSTALL_SRC" fetch --quiet origin
  git -C "$LFG_INSTALL_SRC" checkout --quiet "$LFG_INSTALL_REF" 2>/dev/null \
    || git -C "$LFG_INSTALL_SRC" checkout --quiet -B "$LFG_INSTALL_REF" "origin/$LFG_INSTALL_REF"
  git -C "$LFG_INSTALL_SRC" reset --hard --quiet "origin/$LFG_INSTALL_REF" 2>/dev/null || true
else
  echo "[install] cloning $LFG_INSTALL_REPO@$LFG_INSTALL_REF → $LFG_INSTALL_SRC"
  rm -rf "$LFG_INSTALL_SRC"
  git clone --quiet --depth 50 --branch "$LFG_INSTALL_REF" "$LFG_INSTALL_REPO" "$LFG_INSTALL_SRC" \
    || git clone --quiet "$LFG_INSTALL_REPO" "$LFG_INSTALL_SRC"
fi

exec bash "$LFG_INSTALL_SRC/scripts/bootstrap.sh" "$@"
