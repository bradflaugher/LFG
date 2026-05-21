package runner

import (
	"strings"
	"testing"
	"time"

	"github.com/bradflaugher/lfg/internal/config"
	"github.com/bradflaugher/lfg/internal/store"
)

func TestPodmanArgs_Layout(t *testing.T) {
	// Pretend only OPENROUTER_API_KEY is set on the host so the test is
	// deterministic regardless of the developer's shell.
	for _, k := range []string{
		"OPENROUTER_API_KEY", "ANTHROPIC_API_KEY", "OPENAI_API_KEY",
		"OPENAI_BASE_URL", "GEMINI_API_KEY", "GOOGLE_API_KEY",
		"AZURE_OPENAI_API_KEY", "AZURE_OPENAI_ENDPOINT", "AZURE_OPENAI_API_VERSION",
		"BEDROCK_API_KEY", "AWS_ACCESS_KEY_ID", "AWS_SECRET_ACCESS_KEY",
		"AWS_SESSION_TOKEN", "AWS_REGION", "AWS_DEFAULT_REGION", "AWS_PROFILE",
		"OPENAI_COMPAT_API_KEY", "OPENAI_COMPAT_BASE_URL",
		"VERCEL_AI_API_KEY", "VERCEL_AI_BASE_URL",
	} {
		t.Setenv(k, "")
	}
	t.Setenv("OPENROUTER_API_KEY", "fake")
	t.Setenv("GITHUB_TOKEN", "fake")
	t.Setenv("SLACK_WEBHOOK", "fake")

	dataDir := t.TempDir()
	st, err := store.New(dataDir)
	if err != nil {
		t.Fatalf("store.New: %v", err)
	}
	task, err := st.Create("hello task", "openrouter:moonshotai/kimi-k2")
	if err != nil {
		t.Fatalf("store.Create: %v", err)
	}

	cfg := &config.Config{
		Model:          "openrouter:moonshotai/kimi-k2",
		DataDir:        dataDir,
		SandboxImage:   "ghcr.io/bradflaugher/box:latest",
		SkillsPath:     []string{"/opt/lfg/skills", "/srv/team-skills"},
		SelfBinPath:    "/usr/local/bin/lfg",
		BashTimeout:    60 * time.Second,
		MaxSteps:       50,
		PassThroughEnv: []string{"GITHUB_TOKEN", "SLACK_WEBHOOK"},
	}

	r := New(cfg, st)
	args := r.PodmanArgs(task.ID, "do the thing")

	joined := strings.Join(args, " ")

	mustContain := []string{
		"run --rm",
		"--name lfg-" + task.ID,
		"--workdir /workspace",
		"-v " + st.WorkspaceDir(task.ID) + ":/workspace:rw,Z",
		"-v /opt/lfg/skills:/skills/0:ro,Z",
		"-v /srv/team-skills:/skills/1:ro,Z",
		"-v /usr/local/bin/lfg:/usr/local/bin/lfg:ro,Z",
		"-e LFG_INSIDE=1",
		"-e LFG_MODEL=openrouter:moonshotai/kimi-k2",
		"-e LFG_BASH_TIMEOUT=1m0s",
		"-e LFG_MAX_STEPS=50",
		"-e LFG_SKILLS_PATH=/skills/0:/skills/1",
		"-e OPENROUTER_API_KEY",
		"-e GITHUB_TOKEN",
		"-e SLACK_WEBHOOK",
		"ghcr.io/bradflaugher/box:latest",
		"lfg run --inside --task-id " + task.ID + " do the thing",
	}
	for _, want := range mustContain {
		if !strings.Contains(joined, want) {
			t.Errorf("args missing %q\nfull: %s", want, joined)
		}
	}

	// Sanity: the trailing positional args must be exactly the agent invocation,
	// ending with the prompt.
	expectedTail := []string{"lfg", "run", "--inside", "--task-id", task.ID, "do the thing"}
	tail := args[len(args)-len(expectedTail):]
	for i, v := range expectedTail {
		if tail[i] != v {
			t.Errorf("args[%d] = %q, want %q", len(args)-len(expectedTail)+i, tail[i], v)
		}
	}
}

func TestPodmanArgs_NoSkillsNoExtras(t *testing.T) {
	for _, k := range []string{
		"OPENROUTER_API_KEY", "ANTHROPIC_API_KEY", "OPENAI_API_KEY",
		"OPENAI_BASE_URL", "GEMINI_API_KEY", "GOOGLE_API_KEY",
		"AZURE_OPENAI_API_KEY", "AZURE_OPENAI_ENDPOINT", "AZURE_OPENAI_API_VERSION",
		"BEDROCK_API_KEY", "AWS_ACCESS_KEY_ID", "AWS_SECRET_ACCESS_KEY",
		"AWS_SESSION_TOKEN", "AWS_REGION", "AWS_DEFAULT_REGION", "AWS_PROFILE",
		"OPENAI_COMPAT_API_KEY", "OPENAI_COMPAT_BASE_URL",
		"VERCEL_AI_API_KEY", "VERCEL_AI_BASE_URL",
	} {
		t.Setenv(k, "")
	}

	dataDir := t.TempDir()
	st, _ := store.New(dataDir)
	task, _ := st.Create("p", "m")

	cfg := &config.Config{
		Model:        "m",
		SandboxImage: "img",
		BashTimeout:  30 * time.Second,
		MaxSteps:     10,
	}
	args := New(cfg, st).PodmanArgs(task.ID, "do it")
	joined := strings.Join(args, " ")
	if strings.Contains(joined, "/skills/") {
		t.Errorf("expected no skill mounts when SkillsPath empty; got %s", joined)
	}
	if strings.Contains(joined, "LFG_SKILLS_PATH=") {
		t.Errorf("expected no LFG_SKILLS_PATH env when no skill dirs; got %s", joined)
	}
}

func TestPodmanArgs_AnthropicProvider(t *testing.T) {
	// Clear known provider envs then set only the Anthropic one — verify the
	// runner forwards exactly what's set, nothing else.
	for _, k := range []string{
		"OPENROUTER_API_KEY", "ANTHROPIC_API_KEY", "OPENAI_API_KEY",
		"GEMINI_API_KEY", "AZURE_OPENAI_API_KEY", "AWS_ACCESS_KEY_ID",
		"OPENAI_COMPAT_API_KEY", "VERCEL_AI_API_KEY",
	} {
		t.Setenv(k, "")
	}
	t.Setenv("ANTHROPIC_API_KEY", "sk-ant-xyz")

	dataDir := t.TempDir()
	st, _ := store.New(dataDir)
	task, _ := st.Create("p", "anthropic:claude-opus-4-7")

	cfg := &config.Config{
		Model:        "anthropic:claude-opus-4-7",
		SandboxImage: "img",
		BashTimeout:  30 * time.Second,
		MaxSteps:     10,
	}
	joined := strings.Join(New(cfg, st).PodmanArgs(task.ID, "x"), " ")
	if !strings.Contains(joined, "-e ANTHROPIC_API_KEY") {
		t.Errorf("expected ANTHROPIC_API_KEY in args; got %s", joined)
	}
	if strings.Contains(joined, "-e OPENROUTER_API_KEY") {
		t.Errorf("did not expect OPENROUTER_API_KEY to be forwarded; got %s", joined)
	}
}
