// Package config loads runtime configuration from environment variables.
package config

import (
	"errors"
	"fmt"
	"os"
	"path/filepath"
	"strconv"
	"strings"
	"time"
)

// Config is the resolved runtime configuration for both `lfg run` and `lfg serve`.
type Config struct {
	// Model is the Fantasy model spec, "provider:model-id".
	// E.g. "openrouter:moonshotai/kimi-k2", "anthropic:claude-opus-4-7".
	// API credentials come from the corresponding provider env vars
	// (OPENROUTER_API_KEY, ANTHROPIC_API_KEY, OPENAI_API_KEY, …).
	Model string

	// DataDir is where per-task workspaces + logs land. Default: ./data or /opt/lfg/data when running as the lfg user.
	DataDir string

	// SkillsPath is colon-separated dirs scanned for SKILL.md.
	SkillsPath []string

	// SandboxImage is the container image LFG runs each task inside.
	SandboxImage string

	// SelfBinPath is the absolute path to the lfg binary on disk, bind-mounted into the container.
	SelfBinPath string

	// PassThroughEnv lists env var names the runner copies into the per-task container.
	// Always-on: OPENROUTER_API_KEY, LFG_MODEL, LFG_SKILLS_PATH (rewritten to /skills).
	PassThroughEnv []string

	// BashTimeout is the per-call timeout for the bash tool inside the container.
	BashTimeout time.Duration

	// MaxSteps caps the agent's tool-call loop.
	MaxSteps int

	// Server-only fields below.

	// APIKey enables bearer-token auth on the HTTP server. Empty = local-only mode (binds 127.0.0.1, no auth).
	APIKey string

	// Addr is the listen address for `lfg serve`. Default :8080.
	Addr string
}

// Load reads the environment into a Config and validates required fields based on mode.
func Load() (*Config, error) {
	cfg := &Config{
		Model:        envOr("LFG_MODEL", "openrouter:moonshotai/kimi-k2"),
		DataDir:      envOr("LFG_DATA_DIR", defaultDataDir()),
		SandboxImage: envOr("LFG_SANDBOX_IMAGE", "ghcr.io/bradflaugher/box:latest"),
		BashTimeout:  envDuration("LFG_BASH_TIMEOUT", 300*time.Second),
		MaxSteps:     envInt("LFG_MAX_STEPS", 200),
		APIKey:       os.Getenv("LFG_API_KEY"),
		Addr:         envOr("LFG_ADDR", ":8080"),
	}

	if p := os.Getenv("LFG_SKILLS_PATH"); p != "" {
		cfg.SkillsPath = splitNonEmpty(p, ":")
	}

	if p := os.Getenv("LFG_PASS_ENV"); p != "" {
		cfg.PassThroughEnv = splitNonEmpty(p, ",")
	}

	if b, err := os.Executable(); err == nil {
		// Resolve symlinks so the bind-mount target is the real binary.
		if resolved, rerr := filepath.EvalSymlinks(b); rerr == nil {
			cfg.SelfBinPath = resolved
		} else {
			cfg.SelfBinPath = b
		}
	}

	return cfg, nil
}

// RequireForRun validates fields needed to actually run an agent.
// Provider credentials are checked at resolve time so we can give a
// provider-specific error message; here we only confirm the model spec
// has the required "provider:model-id" shape.
func (c *Config) RequireForRun() error {
	if c.Model == "" {
		return errors.New("LFG_MODEL is required (format: provider:model-id)")
	}
	if !strings.Contains(c.Model, ":") {
		return fmt.Errorf("LFG_MODEL %q is missing a provider prefix; try %q",
			c.Model, "openrouter:"+c.Model)
	}
	return nil
}

func defaultDataDir() string {
	if _, err := os.Stat("/opt/lfg"); err == nil {
		return "/opt/lfg/data"
	}
	wd, err := os.Getwd()
	if err != nil {
		return "./data"
	}
	return filepath.Join(wd, "data")
}

func envOr(key, def string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return def
}

func envInt(key string, def int) int {
	v := os.Getenv(key)
	if v == "" {
		return def
	}
	n, err := strconv.Atoi(v)
	if err != nil {
		return def
	}
	return n
}

func envDuration(key string, def time.Duration) time.Duration {
	v := os.Getenv(key)
	if v == "" {
		return def
	}
	d, err := time.ParseDuration(v)
	if err != nil {
		return def
	}
	return d
}

func splitNonEmpty(s, sep string) []string {
	parts := strings.Split(s, sep)
	out := make([]string, 0, len(parts))
	for _, p := range parts {
		p = strings.TrimSpace(p)
		if p != "" {
			out = append(out, p)
		}
	}
	return out
}

// String returns a redacted summary, safe to log.
func (c *Config) String() string {
	redact := func(s string) string {
		if s == "" {
			return ""
		}
		if len(s) <= 8 {
			return "***"
		}
		return s[:4] + "…" + s[len(s)-2:]
	}
	return fmt.Sprintf("Config{model=%s data=%s image=%s skills=%v api_key=%s addr=%s}",
		c.Model, c.DataDir, c.SandboxImage, c.SkillsPath,
		redact(c.APIKey), c.Addr)
}
