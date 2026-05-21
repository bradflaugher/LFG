// Package runner spawns one rootless podman container per task,
// streams its logs, and updates the task store.
package runner

import (
	"context"
	"errors"
	"fmt"
	"io"
	"os"
	"os/exec"
	"strings"
	"time"

	"github.com/bradflaugher/lfg/internal/agent"
	"github.com/bradflaugher/lfg/internal/config"
	"github.com/bradflaugher/lfg/internal/store"
)

// Runner spawns containers and writes their output into the task store.
type Runner struct {
	cfg   *config.Config
	store *store.Store
}

// New returns a Runner bound to the given config + task store.
func New(cfg *config.Config, st *store.Store) *Runner {
	return &Runner{cfg: cfg, store: st}
}

// PodmanArgs assembles the full `podman run` arg list for a task.
// Split out for testability — runner_test.go golden-tests the slice
// rather than spawning real containers.
func (r *Runner) PodmanArgs(taskID, prompt string) []string {
	args := []string{
		"run", "--rm",
		"--name", "lfg-" + taskID,
		"--workdir", "/workspace",
		"-v", fmt.Sprintf("%s:/workspace:rw,Z", r.store.WorkspaceDir(taskID)),
	}

	// Bind-mount each skills dir under /skills/<idx> so multiple dirs can coexist.
	// Inside the container, LFG_SKILLS_PATH is rewritten to point at all of them.
	for i, dir := range r.cfg.SkillsPath {
		args = append(args, "-v", fmt.Sprintf("%s:/skills/%d:ro,Z", dir, i))
	}

	// Bind-mount the host's lfg binary so the container always runs the same
	// build as the host. Avoids needing to rebuild the image on every change.
	if r.cfg.SelfBinPath != "" {
		args = append(args, "-v", fmt.Sprintf("%s:/usr/local/bin/lfg:ro,Z", r.cfg.SelfBinPath))
	}

	// Env vars: LFG-controlled values first, then provider credentials,
	// then operator-allowlisted extras.
	args = append(args,
		"-e", "LFG_INSIDE=1",
		"-e", fmt.Sprintf("LFG_MODEL=%s", r.cfg.Model),
		"-e", fmt.Sprintf("LFG_BASH_TIMEOUT=%s", r.cfg.BashTimeout),
		"-e", fmt.Sprintf("LFG_MAX_STEPS=%d", r.cfg.MaxSteps),
	)
	if n := len(r.cfg.SkillsPath); n > 0 {
		inside := make([]string, n)
		for i := range r.cfg.SkillsPath {
			inside[i] = fmt.Sprintf("/skills/%d", i)
		}
		args = append(args, "-e", "LFG_SKILLS_PATH="+strings.Join(inside, ":"))
	}
	// Forward whichever provider env vars are set (and non-empty) on the host.
	// `-e KEY` (no =VALUE) tells podman to copy the value from the parent
	// process at exec time. Skipping empty values prevents an operator's
	// half-filled .env.local from shadowing an env var the agent actually
	// inherits from systemd.
	seen := map[string]bool{}
	for _, k := range agent.ProviderEnv {
		if os.Getenv(k) != "" {
			args = append(args, "-e", k)
			seen[k] = true
		}
	}
	for _, k := range r.cfg.PassThroughEnv {
		if seen[k] {
			continue
		}
		args = append(args, "-e", k)
	}

	args = append(args, r.cfg.SandboxImage,
		"lfg", "run", "--inside", "--task-id", taskID, prompt,
	)
	return args
}

// Run executes a task synchronously: spawns the container, streams its
// combined stdout+stderr to the task's log file (and optionally to `extra`),
// updates the task status, and returns when the container exits.
//
// extra may be nil for fully detached server-mode invocations.
func (r *Runner) Run(ctx context.Context, taskID, prompt string, extra io.Writer) error {
	if _, err := r.store.Update(taskID, func(t *store.Task) {
		t.Status = store.StatusRunning
		t.StartedAt = time.Now().UTC()
	}); err != nil {
		return fmt.Errorf("mark task running: %w", err)
	}

	logPath := r.store.LogPath(taskID)
	logFile, err := os.OpenFile(logPath, os.O_CREATE|os.O_WRONLY|os.O_APPEND, 0o644)
	if err != nil {
		return fmt.Errorf("open log %s: %w", logPath, err)
	}
	defer logFile.Close()

	var sink io.Writer = logFile
	if extra != nil {
		sink = io.MultiWriter(logFile, extra)
	}

	cmd := exec.CommandContext(ctx, "podman", r.PodmanArgs(taskID, prompt)...)
	cmd.Stdout = sink
	cmd.Stderr = sink

	runErr := cmd.Run()

	exit := 0
	errStr := ""
	if runErr != nil {
		if ee := (*exec.ExitError)(nil); errors.As(runErr, &ee) {
			exit = ee.ExitCode()
		} else {
			exit = -1
			errStr = runErr.Error()
		}
	}

	status := store.StatusSuccess
	switch {
	case errors.Is(ctx.Err(), context.Canceled):
		status = store.StatusKilled
	case runErr != nil:
		status = store.StatusFailed
	}

	if _, uerr := r.store.Update(taskID, func(t *store.Task) {
		t.Status = status
		t.FinishedAt = time.Now().UTC()
		t.ExitCode = exit
		t.Error = errStr
	}); uerr != nil {
		// Don't shadow the original error.
		fmt.Fprintf(os.Stderr, "lfg: failed to update task %s: %v\n", taskID, uerr)
	}

	return runErr
}

// Kill best-effort terminates a running container by task id.
func (r *Runner) Kill(taskID string) error {
	return exec.Command("podman", "kill", "lfg-"+taskID).Run()
}
