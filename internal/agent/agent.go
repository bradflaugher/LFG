// Package agent assembles a Fantasy agent for LFG: one bash tool plus
// the skills layer. The LLM provider is selected from the model spec
// ("provider:model-id"); every Fantasy provider is supported.
package agent

import (
	"context"
	"fmt"
	"io"
	"time"

	"charm.land/fantasy"
)

// Options configures Run.
type Options struct {
	// Model is the spec passed to the agent in "provider:model-id" form,
	// e.g. "anthropic:claude-opus-4-7" or "openrouter:moonshotai/kimi-k2".
	// API credentials come from the corresponding provider env vars
	// (see provider.go).
	Model       string
	SkillsPaths []string
	BashTimeout time.Duration
	MaxSteps    int

	// Verbose, when non-nil, receives a stream of human-readable events
	// (text deltas, tool calls, tool results). Pass os.Stderr for `-v`.
	Verbose io.Writer
}

// Run builds the agent and executes the given prompt to completion.
// Returns the agent's final text response (or "" if no text was produced).
func Run(ctx context.Context, opts Options, prompt string) (string, error) {
	skills, err := LoadSkills(opts.SkillsPaths)
	if err != nil {
		return "", fmt.Errorf("load skills: %w", err)
	}

	model, err := resolveModel(ctx, opts.Model)
	if err != nil {
		return "", err
	}

	tools := []fantasy.AgentTool{BashTool(opts.BashTimeout)}
	tools = append(tools, SkillsTools(skills)...)

	maxSteps := opts.MaxSteps
	if maxSteps <= 0 {
		maxSteps = 200
	}

	a := fantasy.NewAgent(model,
		fantasy.WithSystemPrompt(SystemPrompt(skills)),
		fantasy.WithTools(tools...),
		fantasy.WithStopConditions(fantasy.StepCountIs(maxSteps)),
	)

	// Stream so we can spool tool calls/results into verbose output as they happen.
	call := fantasy.AgentStreamCall{Prompt: prompt}
	if opts.Verbose != nil {
		w := opts.Verbose
		call.OnTextDelta = func(_, text string) error {
			_, err := io.WriteString(w, text)
			return err
		}
		call.OnToolCall = func(t fantasy.ToolCallContent) error {
			_, err := fmt.Fprintf(w, "\n→ %s %s\n", t.ToolName, compactInput(t.Input))
			return err
		}
		call.OnToolResult = func(r fantasy.ToolResultContent) error {
			text, ok := fantasy.AsToolResultOutputType[fantasy.ToolResultOutputContentText](r.Result)
			if !ok {
				return nil
			}
			_, err := fmt.Fprintf(w, "← %s\n", firstLine(text.Text, 200))
			return err
		}
		call.OnStepFinish = func(_ fantasy.StepResult) error {
			_, err := io.WriteString(w, "\n")
			return err
		}
	}

	res, err := a.Stream(ctx, call)
	if err != nil {
		return "", fmt.Errorf("agent stream: %w", err)
	}
	if res == nil {
		return "", nil
	}
	return res.Response.Content.Text(), nil
}

func compactInput(s string) string {
	if len(s) > 256 {
		return s[:253] + "..."
	}
	return s
}

func firstLine(s string, max int) string {
	for i, r := range s {
		if r == '\n' {
			s = s[:i]
			break
		}
	}
	if len(s) > max {
		return s[:max-1] + "…"
	}
	return s
}
