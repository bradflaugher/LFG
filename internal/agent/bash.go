package agent

import (
	"bytes"
	"context"
	"errors"
	"fmt"
	"os"
	"os/exec"
	"strings"
	"time"

	"charm.land/fantasy"
)

// bashInput is the schema the LLM fills out for the bash tool.
type bashInput struct {
	Command     string `json:"command" description:"The shell command to run. Executed via /bin/bash -c."`
	Description string `json:"description,omitempty" description:"Brief one-line description of what this command does. Optional but helpful for verbose logs."`
}

// maxBashOutput is the per-tool-call cap on combined stdout+stderr bytes returned to the agent.
// Output beyond the cap is truncated with a [truncated …] marker.
const maxBashOutput = 64 * 1024

// BashTool returns a bash tool that runs commands in /workspace with the given timeout.
// Each call gets a fresh context with the timeout; the command runs as a subprocess of the agent process.
//
// The tool assumes it is running inside the per-task container — there is no
// further sandboxing. The host-side runner is responsible for the isolation
// boundary (rootless podman, bind-mounted workspace, allowlisted env vars).
func BashTool(timeout time.Duration) fantasy.AgentTool {
	return fantasy.NewAgentTool(
		"bash",
		"Run a shell command in /workspace via /bin/bash -c. Each call is independent (no persistent shell). Use this for everything: reading and writing files, running scripts, git, curl, python, etc.",
		func(ctx context.Context, in bashInput, _ fantasy.ToolCall) (fantasy.ToolResponse, error) {
			if strings.TrimSpace(in.Command) == "" {
				return fantasy.NewTextErrorResponse("command is empty"), nil
			}

			runCtx, cancel := context.WithTimeout(ctx, timeout)
			defer cancel()

			cmd := exec.CommandContext(runCtx, "/bin/bash", "-c", in.Command)
			// Inside the per-task container /workspace is the canonical cwd; outside
			// (notably under `go test`) it doesn't exist, so fall back to the
			// process's current directory.
			if _, err := os.Stat("/workspace"); err == nil {
				cmd.Dir = "/workspace"
			}
			// Combine stdout+stderr into a single stream — agents reason about
			// "what did this print" without caring which fd it came from.
			var buf bytes.Buffer
			cmd.Stdout = &buf
			cmd.Stderr = &buf

			err := cmd.Run()
			out := truncate(buf.Bytes(), maxBashOutput)

			var status string
			switch {
			case errors.Is(runCtx.Err(), context.DeadlineExceeded):
				status = fmt.Sprintf("[timed out after %s]", timeout)
			case err != nil:
				if ee, ok := err.(*exec.ExitError); ok {
					status = fmt.Sprintf("[exit %d]", ee.ExitCode())
				} else {
					status = fmt.Sprintf("[error: %v]", err)
				}
			default:
				status = "[exit 0]"
			}

			resp := strings.TrimRight(string(out), "\n") + "\n" + status
			// Treat non-zero exits as errors so the model can react.
			if err != nil || errors.Is(runCtx.Err(), context.DeadlineExceeded) {
				return fantasy.NewTextErrorResponse(resp), nil
			}
			return fantasy.NewTextResponse(resp), nil
		},
	)
}

// truncate caps b at max bytes, appending a marker line if it had to cut anything.
func truncate(b []byte, max int) []byte {
	if len(b) <= max {
		return b
	}
	out := make([]byte, 0, max+64)
	out = append(out, b[:max]...)
	out = append(out, []byte(fmt.Sprintf("\n[truncated %d bytes]\n", len(b)-max))...)
	return out
}
