package agent

import (
	"os"
	"path/filepath"
	"strings"
	"testing"
)

func TestParseSKILL_Valid(t *testing.T) {
	body := `---
name: pdf-processing
description: Extract text and tables from PDFs.
license: Apache-2.0
metadata:
  author: example-org
  version: "1.0"
---

# PDF processing

Use ` + "`scripts/extract.py`" + ` to pull text out of a PDF.
`
	s, err := ParseSKILL([]byte(body))
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if s.Name != "pdf-processing" {
		t.Errorf("name = %q, want pdf-processing", s.Name)
	}
	if !strings.Contains(s.Description, "PDF") {
		t.Errorf("description = %q, want it to mention PDFs", s.Description)
	}
	if !strings.Contains(s.Body, "extract.py") {
		t.Errorf("body did not include the marker text; got %q", s.Body)
	}
	if s.Metadata["author"] != "example-org" {
		t.Errorf("metadata.author = %q, want example-org", s.Metadata["author"])
	}
}

func TestParseSKILL_Invalid(t *testing.T) {
	cases := []struct {
		name string
		body string
		want string
	}{
		{"no frontmatter", "# just a heading\n", "missing YAML frontmatter"},
		{"unterminated", "---\nname: foo\ndescription: bar\n", "unterminated frontmatter"},
		{"missing name", "---\ndescription: x\n---\nbody\n", "name is required"},
		{"missing description", "---\nname: foo\n---\nbody\n", "description is required"},
		{"uppercase name", "---\nname: FOO\ndescription: x\n---\n", "agentskills.io rules"},
		{"leading hyphen", "---\nname: -foo\ndescription: x\n---\n", "agentskills.io rules"},
		{"trailing hyphen", "---\nname: foo-\ndescription: x\n---\n", "agentskills.io rules"},
		{"consecutive hyphens", "---\nname: foo--bar\ndescription: x\n---\n", "agentskills.io rules"},
		{"name too long", "---\nname: " + strings.Repeat("a", 65) + "\ndescription: x\n---\n", "1-64"},
	}
	for _, tc := range cases {
		t.Run(tc.name, func(t *testing.T) {
			_, err := ParseSKILL([]byte(tc.body))
			if err == nil {
				t.Fatalf("expected error, got nil")
			}
			if !strings.Contains(err.Error(), tc.want) {
				t.Errorf("error = %q, want substring %q", err.Error(), tc.want)
			}
		})
	}
}

func TestLoadSkills_DirectoryNameMustMatch(t *testing.T) {
	root := t.TempDir()
	// Skill name says "foo" but parent dir is "bar" — should be skipped, not an error.
	mustWrite(t, filepath.Join(root, "bar", "SKILL.md"), `---
name: foo
description: x
---
`)
	skills, err := LoadSkills([]string{root})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if len(skills) != 0 {
		t.Errorf("expected 0 skills (dir/name mismatch), got %d", len(skills))
	}
}

func TestLoadSkills_DiscoversValid(t *testing.T) {
	root := t.TempDir()
	mustWrite(t, filepath.Join(root, "alpha", "SKILL.md"), `---
name: alpha
description: First skill.
---
`)
	mustWrite(t, filepath.Join(root, "beta", "SKILL.md"), `---
name: beta
description: Second skill.
---
`)
	skills, err := LoadSkills([]string{root})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if len(skills) != 2 {
		t.Fatalf("got %d skills, want 2", len(skills))
	}
	if skills[0].Name != "alpha" || skills[1].Name != "beta" {
		t.Errorf("expected sorted [alpha beta], got %s %s", skills[0].Name, skills[1].Name)
	}
}

func mustWrite(t *testing.T, path, body string) {
	t.Helper()
	if err := os.MkdirAll(filepath.Dir(path), 0o755); err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(path, []byte(body), 0o644); err != nil {
		t.Fatal(err)
	}
}
