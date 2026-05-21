package agent

import (
	"context"
	"errors"
	"fmt"
	"os"
	"path/filepath"
	"regexp"
	"sort"
	"strings"

	"charm.land/fantasy"
	"gopkg.in/yaml.v3"
)

// Skill is a single discovered SKILL.md entry, following the agentskills.io spec.
type Skill struct {
	Name        string `yaml:"name"`
	Description string `yaml:"description"`

	// License, Compatibility, AllowedTools, Metadata are parsed but currently unused.
	License       string            `yaml:"license,omitempty"`
	Compatibility string            `yaml:"compatibility,omitempty"`
	AllowedTools  string            `yaml:"allowed-tools,omitempty"`
	Metadata      map[string]string `yaml:"metadata,omitempty"`

	// Dir is the absolute path to the directory containing SKILL.md.
	Dir string `yaml:"-"`
	// Body is the markdown content after the frontmatter.
	Body string `yaml:"-"`
}

// nameRE enforces the agentskills.io rules: lowercase a-z, 0-9, hyphens,
// not starting/ending with a hyphen, no consecutive hyphens, 1-64 chars.
var nameRE = regexp.MustCompile(`^[a-z0-9](?:[a-z0-9]|-(?:[a-z0-9]))*$`)

// LoadSkills walks every directory in paths, parses every SKILL.md it finds,
// and returns the validated entries sorted by name. Invalid skills are skipped
// with a logged warning rather than returning an error — a malformed skill
// shouldn't crash the agent.
func LoadSkills(paths []string) ([]Skill, error) {
	var out []Skill
	seen := map[string]string{} // name → first-seen dir, for duplicate detection

	for _, root := range paths {
		info, err := os.Stat(root)
		if err != nil || !info.IsDir() {
			continue
		}
		entries, err := os.ReadDir(root)
		if err != nil {
			return nil, fmt.Errorf("read skills dir %s: %w", root, err)
		}
		for _, e := range entries {
			if !e.IsDir() {
				continue
			}
			dir := filepath.Join(root, e.Name())
			skill, err := loadSkillFile(filepath.Join(dir, "SKILL.md"))
			if err != nil {
				if !errors.Is(err, os.ErrNotExist) {
					fmt.Fprintf(os.Stderr, "lfg: skipping skill at %s: %v\n", dir, err)
				}
				continue
			}
			if skill.Name != e.Name() {
				fmt.Fprintf(os.Stderr, "lfg: skipping skill at %s: name %q does not match directory %q\n", dir, skill.Name, e.Name())
				continue
			}
			skill.Dir = dir
			if prev, dup := seen[skill.Name]; dup {
				fmt.Fprintf(os.Stderr, "lfg: skipping duplicate skill %q at %s (first seen at %s)\n", skill.Name, dir, prev)
				continue
			}
			seen[skill.Name] = dir
			out = append(out, *skill)
		}
	}

	sort.Slice(out, func(i, j int) bool { return out[i].Name < out[j].Name })
	return out, nil
}

// ParseSKILL parses a SKILL.md byte buffer into a Skill (or returns a validation error).
// Exposed for tests.
func ParseSKILL(data []byte) (*Skill, error) {
	text := string(data)
	if !strings.HasPrefix(text, "---") {
		return nil, errors.New("missing YAML frontmatter")
	}
	rest := strings.TrimPrefix(text, "---")
	rest = strings.TrimLeft(rest, "\n")
	end := strings.Index(rest, "\n---")
	if end < 0 {
		return nil, errors.New("unterminated frontmatter")
	}
	front := rest[:end]
	body := rest[end+len("\n---"):]
	body = strings.TrimLeft(body, "\n")

	var s Skill
	if err := yaml.Unmarshal([]byte(front), &s); err != nil {
		return nil, fmt.Errorf("parse frontmatter: %w", err)
	}
	if s.Name == "" {
		return nil, errors.New("name is required")
	}
	if len(s.Name) > 64 {
		return nil, errors.New("name must be 1-64 chars")
	}
	if !nameRE.MatchString(s.Name) {
		return nil, fmt.Errorf("name %q violates agentskills.io rules (lowercase, alphanumeric, hyphens, no leading/trailing/consecutive hyphens)", s.Name)
	}
	if s.Description == "" {
		return nil, errors.New("description is required")
	}
	if len(s.Description) > 1024 {
		return nil, errors.New("description must be ≤ 1024 chars")
	}
	if len(s.Compatibility) > 500 {
		return nil, errors.New("compatibility must be ≤ 500 chars")
	}
	s.Body = body
	return &s, nil
}

func loadSkillFile(path string) (*Skill, error) {
	data, err := os.ReadFile(path)
	if err != nil {
		return nil, err
	}
	return ParseSKILL(data)
}

// listSkillsInput is intentionally empty — list_skills takes no args.
type listSkillsInput struct{}

// readSkillInput is the schema for the read_skill tool.
type readSkillInput struct {
	Name string `json:"name" description:"The skill name (matching its directory name)."`
}

// SkillsTools returns the two skill-related agent tools, closed over a snapshot of the loaded skills.
func SkillsTools(skills []Skill) []fantasy.AgentTool {
	byName := make(map[string]Skill, len(skills))
	for _, s := range skills {
		byName[s.Name] = s
	}

	listTool := fantasy.NewAgentTool(
		"list_skills",
		"List all available skills with their one-line descriptions. Skills are bundles of instructions and helper scripts you can activate with read_skill.",
		func(_ context.Context, _ listSkillsInput, _ fantasy.ToolCall) (fantasy.ToolResponse, error) {
			if len(skills) == 0 {
				return fantasy.NewTextResponse("No skills installed."), nil
			}
			var b strings.Builder
			for _, s := range skills {
				fmt.Fprintf(&b, "- %s: %s\n", s.Name, s.Description)
			}
			return fantasy.NewTextResponse(b.String()), nil
		},
	)

	readTool := fantasy.NewAgentTool(
		"read_skill",
		"Read the full SKILL.md body and file listing for a named skill. Activate this when a task matches the skill's description.",
		func(_ context.Context, input readSkillInput, _ fantasy.ToolCall) (fantasy.ToolResponse, error) {
			s, ok := byName[input.Name]
			if !ok {
				return fantasy.NewTextErrorResponse(fmt.Sprintf("no skill named %q", input.Name)), nil
			}
			listing, err := listSkillFiles(s.Dir)
			if err != nil {
				return fantasy.NewTextErrorResponse(fmt.Sprintf("list skill files: %v", err)), nil
			}
			var b strings.Builder
			fmt.Fprintf(&b, "# Skill: %s\n\n%s\n\n## Files (rooted at %s)\n\n%s",
				s.Name, strings.TrimSpace(s.Body), s.Dir, listing)
			return fantasy.NewTextResponse(b.String()), nil
		},
	)

	return []fantasy.AgentTool{listTool, readTool}
}

func listSkillFiles(dir string) (string, error) {
	var lines []string
	err := filepath.WalkDir(dir, func(path string, d os.DirEntry, err error) error {
		if err != nil {
			return err
		}
		if path == dir {
			return nil
		}
		rel, _ := filepath.Rel(dir, path)
		if d.IsDir() {
			lines = append(lines, rel+"/")
		} else {
			lines = append(lines, rel)
		}
		return nil
	})
	if err != nil {
		return "", err
	}
	sort.Strings(lines)
	return strings.Join(lines, "\n"), nil
}
