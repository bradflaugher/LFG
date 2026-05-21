package agent

import (
	"context"
	"strings"
	"testing"
)

func TestResolveModel_InvalidSpec(t *testing.T) {
	cases := []struct {
		spec string
		want string
	}{
		{"", "provider:model-id"},
		{"openrouter", "provider:model-id"},
		{":no-provider", "provider:model-id"},
		{"openrouter:", "provider:model-id"},
	}
	for _, tc := range cases {
		t.Run(tc.spec, func(t *testing.T) {
			_, err := resolveModel(context.Background(), tc.spec)
			if err == nil {
				t.Fatalf("expected error, got nil")
			}
			if !strings.Contains(err.Error(), tc.want) {
				t.Errorf("err = %q, want substring %q", err.Error(), tc.want)
			}
		})
	}
}

func TestResolveModel_UnknownProvider(t *testing.T) {
	_, err := resolveModel(context.Background(), "magic:foo-bar")
	if err == nil || !strings.Contains(err.Error(), "unknown provider") {
		t.Errorf("err = %v, want unknown-provider message", err)
	}
}

func TestResolveModel_MissingEnvHasHelpfulMessage(t *testing.T) {
	// Make sure every known provider env is empty.
	for _, k := range []string{
		"OPENROUTER_API_KEY", "ANTHROPIC_API_KEY", "OPENAI_API_KEY",
		"GEMINI_API_KEY", "GOOGLE_API_KEY",
		"AZURE_OPENAI_API_KEY", "AZURE_OPENAI_ENDPOINT",
		"OPENAI_COMPAT_API_KEY", "OPENAI_COMPAT_BASE_URL",
		"VERCEL_AI_API_KEY", "HYPER_API_KEY",
	} {
		t.Setenv(k, "")
	}

	cases := []struct {
		spec    string
		envvars string
	}{
		{"openrouter:m", "OPENROUTER_API_KEY"},
		{"anthropic:m", "ANTHROPIC_API_KEY"},
		{"openai:m", "OPENAI_API_KEY"},
		{"google:m", "GEMINI_API_KEY or GOOGLE_API_KEY"},
		{"azure:m", "AZURE_OPENAI_API_KEY and AZURE_OPENAI_ENDPOINT"},
		{"openai-compat:m", "OPENAI_COMPAT_API_KEY and OPENAI_COMPAT_BASE_URL"},
		{"vercel:m", "VERCEL_AI_API_KEY"},
		{"hyper:m", "HYPER_API_KEY"},
	}
	for _, tc := range cases {
		t.Run(tc.spec, func(t *testing.T) {
			_, err := resolveModel(context.Background(), tc.spec)
			if err == nil {
				t.Fatalf("expected error for missing env, got nil")
			}
			if !strings.Contains(err.Error(), tc.envvars) {
				t.Errorf("err = %q, want substring %q", err.Error(), tc.envvars)
			}
		})
	}
}

func TestSupportedProviders_Stable(t *testing.T) {
	a := SupportedProviders()
	b := SupportedProviders()
	if strings.Join(a, ",") != strings.Join(b, ",") {
		t.Errorf("SupportedProviders is not stable: %v vs %v", a, b)
	}
	for _, want := range []string{"openrouter", "anthropic", "openai", "google", "azure", "bedrock", "openai-compat", "vercel", "hyper"} {
		found := false
		for _, got := range a {
			if got == want {
				found = true
				break
			}
		}
		if !found {
			t.Errorf("missing provider %q in %v", want, a)
		}
	}
}
