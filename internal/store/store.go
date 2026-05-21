// Package store is a filesystem-backed task store. Each task gets:
//
//	<DataDir>/tasks/<id>/
//	  meta.json            — Task metadata (status, prompt, timestamps)
//	  workspace/           — bind-mounted into the per-task container as /workspace
//	  logs/run.jsonl       — one JSON object per line, written by the runner
package store

import (
	"encoding/json"
	"errors"
	"fmt"
	"os"
	"path/filepath"
	"sort"
	"sync"
	"time"

	"github.com/google/uuid"
)

// Status enumerates the lifecycle of a task.
type Status string

const (
	StatusPending Status = "pending"
	StatusRunning Status = "running"
	StatusSuccess Status = "success"
	StatusFailed  Status = "failed"
	StatusKilled  Status = "killed"
)

// Task is the metadata persisted at meta.json.
type Task struct {
	ID         string    `json:"id"`
	Prompt     string    `json:"prompt"`
	Model      string    `json:"model"`
	Status     Status    `json:"status"`
	CreatedAt  time.Time `json:"created_at"`
	StartedAt  time.Time `json:"started_at,omitempty"`
	FinishedAt time.Time `json:"finished_at,omitempty"`
	ExitCode   int       `json:"exit_code"`
	Error      string    `json:"error,omitempty"`
}

// Store is safe for concurrent use.
type Store struct {
	root string
	mu   sync.Mutex
}

// New returns a Store rooted at <dataDir>/tasks/, creating the directory if needed.
func New(dataDir string) (*Store, error) {
	root := filepath.Join(dataDir, "tasks")
	if err := os.MkdirAll(root, 0o755); err != nil {
		return nil, fmt.Errorf("mkdir %s: %w", root, err)
	}
	return &Store{root: root}, nil
}

// Create allocates a new task, lays out its directory tree, and persists meta.json.
func (s *Store) Create(prompt, model string) (*Task, error) {
	id := uuid.NewString()
	t := &Task{
		ID:        id,
		Prompt:    prompt,
		Model:     model,
		Status:    StatusPending,
		CreatedAt: time.Now().UTC(),
	}

	dir := s.dir(id)
	for _, sub := range []string{"workspace", "logs"} {
		if err := os.MkdirAll(filepath.Join(dir, sub), 0o755); err != nil {
			return nil, fmt.Errorf("mkdir %s: %w", sub, err)
		}
	}
	if err := s.writeMeta(t); err != nil {
		return nil, err
	}
	return t, nil
}

// Get returns a task by id or os.ErrNotExist if missing.
func (s *Store) Get(id string) (*Task, error) {
	if !validID(id) {
		return nil, os.ErrNotExist
	}
	return s.readMeta(id)
}

// Update applies the mutator to a task's metadata and writes it back atomically.
func (s *Store) Update(id string, mut func(*Task)) (*Task, error) {
	s.mu.Lock()
	defer s.mu.Unlock()
	t, err := s.readMeta(id)
	if err != nil {
		return nil, err
	}
	mut(t)
	if err := s.writeMeta(t); err != nil {
		return nil, err
	}
	return t, nil
}

// List returns all tasks, newest-first.
func (s *Store) List() ([]*Task, error) {
	entries, err := os.ReadDir(s.root)
	if err != nil {
		return nil, err
	}
	var out []*Task
	for _, e := range entries {
		if !e.IsDir() || !validID(e.Name()) {
			continue
		}
		t, err := s.readMeta(e.Name())
		if err != nil {
			continue
		}
		out = append(out, t)
	}
	sort.Slice(out, func(i, j int) bool { return out[i].CreatedAt.After(out[j].CreatedAt) })
	return out, nil
}

// WorkspaceDir returns the absolute path of <id>/workspace.
func (s *Store) WorkspaceDir(id string) string { return filepath.Join(s.dir(id), "workspace") }

// LogPath returns the JSONL log file path.
func (s *Store) LogPath(id string) string { return filepath.Join(s.dir(id), "logs", "run.jsonl") }

func (s *Store) dir(id string) string { return filepath.Join(s.root, id) }

func (s *Store) writeMeta(t *Task) error {
	p := filepath.Join(s.dir(t.ID), "meta.json")
	tmp := p + ".tmp"
	f, err := os.OpenFile(tmp, os.O_CREATE|os.O_WRONLY|os.O_TRUNC, 0o644)
	if err != nil {
		return err
	}
	enc := json.NewEncoder(f)
	enc.SetIndent("", "  ")
	if err := enc.Encode(t); err != nil {
		f.Close()
		return err
	}
	if err := f.Close(); err != nil {
		return err
	}
	return os.Rename(tmp, p)
}

func (s *Store) readMeta(id string) (*Task, error) {
	p := filepath.Join(s.dir(id), "meta.json")
	data, err := os.ReadFile(p)
	if err != nil {
		if errors.Is(err, os.ErrNotExist) {
			return nil, os.ErrNotExist
		}
		return nil, err
	}
	var t Task
	if err := json.Unmarshal(data, &t); err != nil {
		return nil, fmt.Errorf("decode meta.json: %w", err)
	}
	return &t, nil
}

// validID guards against path traversal on the URL :id param.
func validID(s string) bool {
	if s == "" {
		return false
	}
	_, err := uuid.Parse(s)
	return err == nil
}
