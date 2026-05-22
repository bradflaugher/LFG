// Command lfg is the entry point for both the host-side runner and the
// in-container agent loop. The same binary handles both — `lfg run` spawns
// a per-task podman container that re-execs this same binary with
// --inside, which then loads Fantasy and runs the agent.
package main

import (
	"context"
	"errors"
	"flag"
	"fmt"
	"io"
	"os"
	"os/signal"
	"syscall"

	"github.com/bradflaugher/lfg/internal/agent"
	"github.com/bradflaugher/lfg/internal/config"
	"github.com/bradflaugher/lfg/internal/runner"
	"github.com/bradflaugher/lfg/internal/server"
	"github.com/bradflaugher/lfg/internal/store"
)

// Version is the build version, set via -ldflags "-X main.Version=…" by the Makefile.
var Version = "dev"

func main() {
	if len(os.Args) < 2 {
		usage()
		os.Exit(2)
	}

	switch os.Args[1] {
	case "run":
		os.Exit(cmdRun(os.Args[2:]))
	case "serve":
		os.Exit(cmdServe(os.Args[2:]))
	case "version", "--version", "-v":
		fmt.Println(Version)
	case "help", "--help", "-h":
		usage()
	default:
		fmt.Fprintf(os.Stderr, "lfg: unknown command %q\n", os.Args[1])
		usage()
		os.Exit(2)
	}
}

func usage() {
	fmt.Fprint(os.Stderr, `lfg — minimal AI agent

USAGE
  lfg run [flags] "<task description>"
  lfg serve [flags]
  lfg version

RUN FLAGS
  --workspace DIR    Bind-mount DIR into the container as /workspace (default: $LFG_DATA_DIR/tasks/<id>/workspace)
  --task-id ID       Reuse an existing task id (default: generate new uuid)
  --model SLUG       Override LFG_MODEL for this run
  --verbose          Stream tool calls + text to stderr
  --inside           Internal: skip the container spawn; run the agent in-process

SERVE FLAGS
  --addr ADDR        Listen address (default $LFG_ADDR or :8080)

ENV
  OPENROUTER_API_KEY  Required.
  LFG_MODEL           Default model slug (e.g. moonshotai/kimi-k2).
  LFG_API_KEY         If set, the server requires Bearer auth and binds 0.0.0.0.
  LFG_DATA_DIR        Where tasks land. Default: ./data (or /opt/lfg/data on a provisioned box).
  LFG_SKILLS_PATH     Colon-separated SKILL.md dirs.
  LFG_SANDBOX_IMAGE   Container image (default: ghcr.io/bradflaugher/box:latest).
  LFG_PASS_ENV        Comma-separated env vars to forward into the container.
`)
}

func cmdRun(args []string) int {
	fs := flag.NewFlagSet("run", flag.ContinueOnError)
	workspace := fs.String("workspace", "", "host workspace dir to bind-mount into the container")
	taskID := fs.String("task-id", "", "reuse an existing task id")
	model := fs.String("model", "", "override LFG_MODEL for this run")
	verbose := fs.Bool("verbose", false, "stream agent steps to stderr")
	inside := fs.Bool("inside", false, "internal: agent-only mode, skip the container spawn")
	if err := fs.Parse(args); err != nil {
		return 2
	}

	if fs.NArg() == 0 {
		fmt.Fprintln(os.Stderr, "lfg run: a task description is required")
		return 2
	}
	prompt := fs.Arg(0)

	cfg, err := config.Load()
	if err != nil {
		fmt.Fprintf(os.Stderr, "lfg: %v\n", err)
		return 1
	}
	if *model != "" {
		cfg.Model = *model
	}

	ctx, cancel := signalContext()
	defer cancel()

	if *inside {
		return runInside(ctx, cfg, prompt, *verbose)
	}

	if err := cfg.RequireForRun(); err != nil {
		fmt.Fprintf(os.Stderr, "lfg: %v\n", err)
		return 1
	}

	st, err := store.New(cfg.DataDir)
	if err != nil {
		fmt.Fprintf(os.Stderr, "lfg: %v\n", err)
		return 1
	}

	var task *store.Task
	if *taskID != "" {
		task, err = st.Get(*taskID)
		if err != nil {
			fmt.Fprintf(os.Stderr, "lfg: task %s not found\n", *taskID)
			return 1
		}
	} else {
		task, err = st.Create(prompt, cfg.Model)
		if err != nil {
			fmt.Fprintf(os.Stderr, "lfg: %v\n", err)
			return 1
		}
	}

	// If --workspace was passed, symlink it as <id>/workspace so callers can
	// seed their own files (e.g. a checked-out git repo).
	if *workspace != "" {
		ws := st.WorkspaceDir(task.ID)
		_ = os.RemoveAll(ws)
		if err := os.Symlink(*workspace, ws); err != nil {
			fmt.Fprintf(os.Stderr, "lfg: symlink workspace: %v\n", err)
			return 1
		}
	}

	r := runner.New(cfg, st)

	// io.Writer (not *os.File) so a non-verbose run passes a true-nil
	// interface to r.Run, not a typed-nil that masquerades as non-nil.
	var sink io.Writer
	if *verbose {
		sink = os.Stderr
	}
	if err := r.Run(ctx, task.ID, prompt, sink); err != nil {
		// Non-zero is the agent's exit, not a hard failure of the runner.
		var exitErr *exitCodeError
		if errors.As(err, &exitErr) {
			return exitErr.code
		}
		fmt.Fprintf(os.Stderr, "lfg: %v\n", err)
		return 1
	}
	return 0
}

func runInside(ctx context.Context, cfg *config.Config, prompt string, verbose bool) int {
	if err := cfg.RequireForRun(); err != nil {
		fmt.Fprintf(os.Stderr, "lfg: %v\n", err)
		return 1
	}
	var w io.Writer
	if verbose {
		w = os.Stderr
	}
	text, err := agent.Run(ctx, agent.Options{
		Model:       cfg.Model,
		SkillsPaths: cfg.SkillsPath,
		BashTimeout: cfg.BashTimeout,
		MaxSteps:    cfg.MaxSteps,
		Verbose:     w,
	}, prompt)
	if err != nil {
		fmt.Fprintf(os.Stderr, "lfg: %v\n", err)
		return 1
	}
	if text != "" {
		fmt.Println(text)
	}
	return 0
}

func cmdServe(args []string) int {
	fs := flag.NewFlagSet("serve", flag.ContinueOnError)
	addr := fs.String("addr", "", "listen address (overrides $LFG_ADDR)")
	if err := fs.Parse(args); err != nil {
		return 2
	}

	cfg, err := config.Load()
	if err != nil {
		fmt.Fprintf(os.Stderr, "lfg: %v\n", err)
		return 1
	}
	if err := cfg.RequireForRun(); err != nil {
		fmt.Fprintf(os.Stderr, "lfg: %v\n", err)
		return 1
	}
	if *addr != "" {
		cfg.Addr = *addr
	}

	st, err := store.New(cfg.DataDir)
	if err != nil {
		fmt.Fprintf(os.Stderr, "lfg: %v\n", err)
		return 1
	}
	r := runner.New(cfg, st)
	srv := server.New(cfg, st, r)

	ctx, cancel := signalContext()
	defer cancel()

	if err := srv.ListenAndServe(ctx); err != nil {
		fmt.Fprintf(os.Stderr, "lfg: %v\n", err)
		return 1
	}
	return 0
}

type exitCodeError struct{ code int }

func (e *exitCodeError) Error() string { return fmt.Sprintf("exit %d", e.code) }

func signalContext() (context.Context, context.CancelFunc) {
	return signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
}
