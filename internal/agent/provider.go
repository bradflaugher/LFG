package agent

import (
	"context"
	"fmt"
	"os"
	"sort"
	"strings"

	"charm.land/fantasy"
	"charm.land/fantasy/providers/anthropic"
	"charm.land/fantasy/providers/azure"
	"charm.land/fantasy/providers/bedrock"
	"charm.land/fantasy/providers/google"
	"charm.land/fantasy/providers/openai"
	"charm.land/fantasy/providers/openaicompat"
	"charm.land/fantasy/providers/openrouter"
	"charm.land/fantasy/providers/vercel"
)

// ProviderEnv lists the API-key env vars LFG knows about, one per
// supported Fantasy provider. The runner forwards any of these that
// are set on the host into the per-task container — so the agent
// can pick the right one for whichever provider was selected.
//
// Operators who want to add new ones (custom openai-compat deployments
// with a different env-var name, say) should add them to LFG_PASS_ENV.
var ProviderEnv = []string{
	"OPENROUTER_API_KEY",
	"ANTHROPIC_API_KEY",
	"OPENAI_API_KEY",
	"OPENAI_BASE_URL",
	"GEMINI_API_KEY",
	"GOOGLE_API_KEY",
	"AZURE_OPENAI_API_KEY",
	"AZURE_OPENAI_ENDPOINT",
	"AZURE_OPENAI_API_VERSION",
	"BEDROCK_API_KEY",
	"AWS_ACCESS_KEY_ID",
	"AWS_SECRET_ACCESS_KEY",
	"AWS_SESSION_TOKEN",
	"AWS_REGION",
	"AWS_DEFAULT_REGION",
	"AWS_PROFILE",
	"OPENAI_COMPAT_API_KEY",
	"OPENAI_COMPAT_BASE_URL",
	"VERCEL_AI_API_KEY",
	"VERCEL_AI_BASE_URL",
	"HYPER_API_KEY",
	"HYPER_URL",
}

// SupportedProviders lists the provider prefixes the model spec accepts.
// Sorted alphabetically for stable help output.
func SupportedProviders() []string {
	out := []string{"anthropic", "azure", "bedrock", "google", "hyper", "openai", "openai-compat", "openrouter", "vercel"}
	sort.Strings(out)
	return out
}

// resolveModel parses spec ("provider:model-id") and returns the matching
// Fantasy LanguageModel. Each provider reads its own env vars; an empty
// API-key env var triggers a helpful error rather than letting the model
// call fail later with an opaque 401.
func resolveModel(ctx context.Context, spec string) (fantasy.LanguageModel, error) {
	providerName, modelID, ok := strings.Cut(spec, ":")
	if !ok || providerName == "" || modelID == "" {
		return nil, fmt.Errorf("model %q must be %q (try %q)",
			spec, "provider:model-id", "openrouter:moonshotai/kimi-k2")
	}
	provider, err := newProvider(providerName)
	if err != nil {
		return nil, err
	}
	return provider.LanguageModel(ctx, modelID)
}

func newProvider(name string) (fantasy.Provider, error) {
	switch name {
	case "openrouter":
		key := os.Getenv("OPENROUTER_API_KEY")
		if key == "" {
			return nil, missingEnv("openrouter", "OPENROUTER_API_KEY")
		}
		return openrouter.New(openrouter.WithAPIKey(key))

	case "anthropic":
		key := os.Getenv("ANTHROPIC_API_KEY")
		if key == "" {
			return nil, missingEnv("anthropic", "ANTHROPIC_API_KEY")
		}
		return anthropic.New(anthropic.WithAPIKey(key))

	case "openai":
		key := os.Getenv("OPENAI_API_KEY")
		if key == "" {
			return nil, missingEnv("openai", "OPENAI_API_KEY")
		}
		opts := []openai.Option{openai.WithAPIKey(key)}
		if base := os.Getenv("OPENAI_BASE_URL"); base != "" {
			opts = append(opts, openai.WithBaseURL(base))
		}
		return openai.New(opts...)

	case "google":
		// Fantasy's Google provider uses Gemini API keys via WithGeminiAPIKey,
		// or auto-detects ADC. We honor either GEMINI_API_KEY or GOOGLE_API_KEY
		// — both names are in common use.
		key := firstNonEmpty("GEMINI_API_KEY", "GOOGLE_API_KEY")
		if key == "" {
			return nil, missingEnv("google", "GEMINI_API_KEY or GOOGLE_API_KEY")
		}
		return google.New(google.WithGeminiAPIKey(key))

	case "azure":
		key := os.Getenv("AZURE_OPENAI_API_KEY")
		endpoint := os.Getenv("AZURE_OPENAI_ENDPOINT")
		if key == "" || endpoint == "" {
			return nil, missingEnv("azure", "AZURE_OPENAI_API_KEY and AZURE_OPENAI_ENDPOINT")
		}
		opts := []azure.Option{azure.WithAPIKey(key), azure.WithBaseURL(endpoint)}
		if v := os.Getenv("AZURE_OPENAI_API_VERSION"); v != "" {
			opts = append(opts, azure.WithAPIVersion(v))
		}
		return azure.New(opts...)

	case "bedrock":
		// AWS Bedrock: prefer the new Bedrock API key when present, otherwise
		// fall back to the standard AWS credential chain (AWS_ACCESS_KEY_ID,
		// IAM role, AWS_PROFILE, etc.) — bedrock.New() with no opts picks that up.
		var opts []bedrock.Option
		if key := os.Getenv("BEDROCK_API_KEY"); key != "" {
			opts = append(opts, bedrock.WithAPIKey(key))
		}
		return bedrock.New(opts...)

	case "openai-compat":
		key := os.Getenv("OPENAI_COMPAT_API_KEY")
		base := os.Getenv("OPENAI_COMPAT_BASE_URL")
		if key == "" || base == "" {
			return nil, missingEnv("openai-compat", "OPENAI_COMPAT_API_KEY and OPENAI_COMPAT_BASE_URL")
		}
		return openaicompat.New(
			openaicompat.WithAPIKey(key),
			openaicompat.WithBaseURL(base),
		)

	case "hyper":
		// Charm Hyper: managed LLM proxy at https://hyper.charm.land.
		// Speaks the OpenAI-compatible Fantasy protocol on /v1, so we
		// build it via openaicompat with a fixed base URL.
		key := os.Getenv("HYPER_API_KEY")
		if key == "" {
			return nil, missingEnv("hyper", "HYPER_API_KEY")
		}
		host := os.Getenv("HYPER_URL")
		if host == "" {
			host = "https://hyper.charm.land"
		}
		return openaicompat.New(
			openaicompat.WithName("hyper"),
			openaicompat.WithAPIKey(key),
			openaicompat.WithBaseURL(host+"/v1"),
		)

	case "vercel":
		key := os.Getenv("VERCEL_AI_API_KEY")
		if key == "" {
			return nil, missingEnv("vercel", "VERCEL_AI_API_KEY")
		}
		opts := []vercel.Option{vercel.WithAPIKey(key)}
		if base := os.Getenv("VERCEL_AI_BASE_URL"); base != "" {
			opts = append(opts, vercel.WithBaseURL(base))
		}
		return vercel.New(opts...)

	default:
		return nil, fmt.Errorf("unknown provider %q (supported: %s)", name, strings.Join(SupportedProviders(), ", "))
	}
}

func missingEnv(provider, envvars string) error {
	return fmt.Errorf("%s provider needs %s to be set", provider, envvars)
}

func firstNonEmpty(keys ...string) string {
	for _, k := range keys {
		if v := os.Getenv(k); v != "" {
			return v
		}
	}
	return ""
}
