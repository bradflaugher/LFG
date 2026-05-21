package server

import (
	"bytes"
	"context"
	"encoding/json"
	"io"
	"net/http"
	"net/http/httptest"
	"strings"
	"sync"
	"testing"
	"time"

	"github.com/bradflaugher/lfg/internal/config"
	"github.com/bradflaugher/lfg/internal/store"
)

// fakeRunner records each Run call so the test can wait for the
// fire-and-forget goroutine to finish before TempDir cleanup runs.
type fakeRunner struct {
	mu   sync.Mutex
	wg   sync.WaitGroup
	runs []string
}

func (f *fakeRunner) Run(_ context.Context, taskID, _ string, _ io.Writer) error {
	defer f.wg.Done()
	f.mu.Lock()
	f.runs = append(f.runs, taskID)
	f.mu.Unlock()
	return nil
}
func (f *fakeRunner) Kill(string) error { return nil }
func (f *fakeRunner) expect(n int)      { f.wg.Add(n) }
func (f *fakeRunner) wait()             { f.wg.Wait() }

func newTestServer(t *testing.T, apiKey string) (*Server, *store.Store, *fakeRunner) {
	t.Helper()
	dataDir := t.TempDir()
	st, err := store.New(dataDir)
	if err != nil {
		t.Fatalf("store.New: %v", err)
	}
	cfg := &config.Config{
		Model:        "openrouter:moonshotai/kimi-k2",
		DataDir:      dataDir,
		SandboxImage: "ghcr.io/bradflaugher/box:latest",
		BashTimeout:  60 * time.Second,
		MaxSteps:     10,
		APIKey:       apiKey,
		Addr:         ":0",
	}
	r := &fakeRunner{}
	return New(cfg, st, r), st, r
}

func TestHealthz(t *testing.T) {
	srv, _, _ := newTestServer(t, "")
	rec := httptest.NewRecorder()
	srv.Handler().ServeHTTP(rec, httptest.NewRequest(http.MethodGet, "/healthz", nil))
	if rec.Code != http.StatusOK {
		t.Fatalf("status = %d, want 200", rec.Code)
	}
}

func TestAuth_RequiredWhenKeySet(t *testing.T) {
	srv, _, _ := newTestServer(t, "topsecret")
	rec := httptest.NewRecorder()
	srv.Handler().ServeHTTP(rec, httptest.NewRequest(http.MethodGet, "/tasks", nil))
	if rec.Code != http.StatusUnauthorized {
		t.Errorf("no auth header: status = %d, want 401", rec.Code)
	}

	rec = httptest.NewRecorder()
	req := httptest.NewRequest(http.MethodGet, "/tasks", nil)
	req.Header.Set("Authorization", "Bearer wrong")
	srv.Handler().ServeHTTP(rec, req)
	if rec.Code != http.StatusUnauthorized {
		t.Errorf("wrong key: status = %d, want 401", rec.Code)
	}

	rec = httptest.NewRecorder()
	req = httptest.NewRequest(http.MethodGet, "/tasks", nil)
	req.Header.Set("Authorization", "Bearer topsecret")
	srv.Handler().ServeHTTP(rec, req)
	if rec.Code != http.StatusOK {
		t.Errorf("correct key: status = %d, want 200", rec.Code)
	}
}

func TestAuth_SkippedWhenKeyEmpty(t *testing.T) {
	srv, _, _ := newTestServer(t, "")
	rec := httptest.NewRecorder()
	srv.Handler().ServeHTTP(rec, httptest.NewRequest(http.MethodGet, "/tasks", nil))
	if rec.Code != http.StatusOK {
		t.Errorf("status = %d, want 200 (local-only mode skips auth)", rec.Code)
	}
}

func TestCreateTask_ReturnsAccepted(t *testing.T) {
	srv, st, fake := newTestServer(t, "")
	fake.expect(1)

	body, _ := json.Marshal(map[string]string{"prompt": "echo hi"})
	rec := httptest.NewRecorder()
	req := httptest.NewRequest(http.MethodPost, "/tasks", bytes.NewReader(body))
	req.Header.Set("Content-Type", "application/json")
	srv.Handler().ServeHTTP(rec, req)

	if rec.Code != http.StatusAccepted {
		t.Fatalf("status = %d, want 202\nbody: %s", rec.Code, rec.Body.String())
	}
	var resp createTaskResp
	if err := json.NewDecoder(rec.Body).Decode(&resp); err != nil {
		t.Fatal(err)
	}
	if resp.ID == "" {
		t.Errorf("empty task id in response")
	}

	// Wait for the fire-and-forget goroutine so TempDir cleanup doesn't
	// race the runner's log writes.
	fake.wait()

	if _, err := st.Get(resp.ID); err != nil {
		t.Errorf("task %s not in store: %v", resp.ID, err)
	}
	if len(fake.runs) != 1 || fake.runs[0] != resp.ID {
		t.Errorf("fake runner saw %v, want [%s]", fake.runs, resp.ID)
	}
}

func TestCreateTask_RejectsEmptyPrompt(t *testing.T) {
	srv, _, _ := newTestServer(t, "")
	rec := httptest.NewRecorder()
	req := httptest.NewRequest(http.MethodPost, "/tasks", strings.NewReader(`{"prompt":""}`))
	req.Header.Set("Content-Type", "application/json")
	srv.Handler().ServeHTTP(rec, req)
	if rec.Code != http.StatusBadRequest {
		t.Errorf("status = %d, want 400", rec.Code)
	}
}

func TestGetTask_NotFound(t *testing.T) {
	srv, _, _ := newTestServer(t, "")
	rec := httptest.NewRecorder()
	srv.Handler().ServeHTTP(rec, httptest.NewRequest(http.MethodGet, "/tasks/not-a-uuid", nil))
	if rec.Code != http.StatusNotFound {
		t.Errorf("status = %d, want 404", rec.Code)
	}
}
