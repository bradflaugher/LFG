// Package server exposes LFG's fire-and-forget HTTP API.
package server

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"net/http"
	"os"
	"time"

	"github.com/go-chi/chi/v5"
	"github.com/go-chi/chi/v5/middleware"

	"github.com/bradflaugher/lfg/internal/config"
	"github.com/bradflaugher/lfg/internal/store"
)

// Runner is the subset of *runner.Runner that the server depends on.
// Defining it here lets tests inject a noop runner without spawning podman.
type Runner interface {
	Run(ctx context.Context, taskID, prompt string, extra io.Writer) error
	Kill(taskID string) error
}

// Server wires the HTTP router to the runner + store.
type Server struct {
	cfg    *config.Config
	store  *store.Store
	runner Runner
	mux    http.Handler
}

// New builds a Server. The router is constructed eagerly so it can be tested via httptest.
func New(cfg *config.Config, st *store.Store, r Runner) *Server {
	s := &Server{cfg: cfg, store: st, runner: r}
	s.mux = s.routes()
	return s
}

// Handler returns the underlying http.Handler (for tests).
func (s *Server) Handler() http.Handler { return s.mux }

// ListenAndServe binds the configured address and serves until ctx is done or an error occurs.
func (s *Server) ListenAndServe(ctx context.Context) error {
	addr := s.cfg.Addr
	// Local-only mode: if no API key is configured, refuse to bind anything but loopback.
	if s.cfg.APIKey == "" && !hasLoopback(addr) {
		addr = "127.0.0.1" + portOnly(addr)
		fmt.Fprintf(os.Stderr, "lfg: LFG_API_KEY unset — binding %s only\n", addr)
	}

	srv := &http.Server{
		Addr:              addr,
		Handler:           s.mux,
		ReadHeaderTimeout: 10 * time.Second,
		IdleTimeout:       60 * time.Second,
	}

	go func() {
		<-ctx.Done()
		shutCtx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
		defer cancel()
		_ = srv.Shutdown(shutCtx)
	}()

	fmt.Fprintf(os.Stderr, "lfg: listening on %s\n", addr)
	if err := srv.ListenAndServe(); err != nil && !errors.Is(err, http.ErrServerClosed) {
		return err
	}
	return nil
}

func (s *Server) routes() http.Handler {
	r := chi.NewRouter()
	r.Use(middleware.Recoverer)
	r.Use(middleware.RequestID)

	r.Get("/healthz", func(w http.ResponseWriter, _ *http.Request) {
		w.WriteHeader(http.StatusOK)
		_, _ = io.WriteString(w, "ok\n")
	})

	r.Group(func(r chi.Router) {
		r.Use(s.authMiddleware)
		r.Post("/tasks", s.createTask)
		r.Get("/tasks", s.listTasks)
		r.Get("/tasks/{id}", s.getTask)
		r.Get("/tasks/{id}/logs", s.taskLogs)
		r.Delete("/tasks/{id}", s.deleteTask)
	})

	return r
}

type createTaskReq struct {
	Prompt string `json:"prompt"`
	// Model is optional — falls back to the server's default.
	Model string `json:"model,omitempty"`
}

type createTaskResp struct {
	ID     string       `json:"id"`
	Status store.Status `json:"status"`
}

func (s *Server) createTask(w http.ResponseWriter, r *http.Request) {
	var req createTaskReq
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 1<<20)).Decode(&req); err != nil {
		writeJSONError(w, http.StatusBadRequest, "invalid JSON body")
		return
	}
	if req.Prompt == "" {
		writeJSONError(w, http.StatusBadRequest, "prompt is required")
		return
	}

	model := req.Model
	if model == "" {
		model = s.cfg.Model
	}
	t, err := s.store.Create(req.Prompt, model)
	if err != nil {
		writeJSONError(w, http.StatusInternalServerError, fmt.Sprintf("create task: %v", err))
		return
	}

	// Fire-and-forget. Use a fresh context so a client disconnect doesn't kill the agent.
	go func() {
		runCtx, cancel := context.WithCancel(context.Background())
		defer cancel()
		_ = s.runner.Run(runCtx, t.ID, t.Prompt, nil)
	}()

	writeJSON(w, http.StatusAccepted, createTaskResp{ID: t.ID, Status: store.StatusRunning})
}

func (s *Server) listTasks(w http.ResponseWriter, _ *http.Request) {
	tasks, err := s.store.List()
	if err != nil {
		writeJSONError(w, http.StatusInternalServerError, err.Error())
		return
	}
	writeJSON(w, http.StatusOK, map[string]any{"tasks": tasks})
}

func (s *Server) getTask(w http.ResponseWriter, r *http.Request) {
	id := chi.URLParam(r, "id")
	t, err := s.store.Get(id)
	if err != nil {
		writeJSONError(w, http.StatusNotFound, "task not found")
		return
	}
	writeJSON(w, http.StatusOK, t)
}

func (s *Server) deleteTask(w http.ResponseWriter, r *http.Request) {
	id := chi.URLParam(r, "id")
	if _, err := s.store.Get(id); err != nil {
		writeJSONError(w, http.StatusNotFound, "task not found")
		return
	}
	_ = s.runner.Kill(id)
	w.WriteHeader(http.StatusNoContent)
}

// taskLogs streams the JSONL log. With ?follow=1, holds the connection open
// and pushes new lines as they arrive (Server-Sent-Events-ish but plain text).
func (s *Server) taskLogs(w http.ResponseWriter, r *http.Request) {
	id := chi.URLParam(r, "id")
	if _, err := s.store.Get(id); err != nil {
		writeJSONError(w, http.StatusNotFound, "task not found")
		return
	}
	path := s.store.LogPath(id)

	f, err := os.Open(path)
	if err != nil {
		writeJSONError(w, http.StatusNotFound, "log not available yet")
		return
	}
	defer f.Close()

	w.Header().Set("Content-Type", "text/plain; charset=utf-8")
	if _, err := io.Copy(w, f); err != nil {
		return
	}

	if r.URL.Query().Get("follow") != "1" {
		return
	}

	flusher, _ := w.(http.Flusher)
	buf := make([]byte, 4096)
	for {
		select {
		case <-r.Context().Done():
			return
		default:
		}
		n, err := f.Read(buf)
		if n > 0 {
			if _, werr := w.Write(buf[:n]); werr != nil {
				return
			}
			if flusher != nil {
				flusher.Flush()
			}
			continue
		}
		if err == io.EOF {
			// Done if the task has finished; otherwise back off and poll.
			t, _ := s.store.Get(id)
			if t != nil && (t.Status == store.StatusSuccess || t.Status == store.StatusFailed || t.Status == store.StatusKilled) {
				return
			}
			time.Sleep(500 * time.Millisecond)
			continue
		}
		if err != nil {
			return
		}
	}
}

func writeJSON(w http.ResponseWriter, code int, body any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(code)
	_ = json.NewEncoder(w).Encode(body)
}

func writeJSONError(w http.ResponseWriter, code int, msg string) {
	writeJSON(w, code, map[string]string{"error": msg})
}

func hasLoopback(addr string) bool {
	if len(addr) > 0 && addr[0] == ':' {
		return false
	}
	return len(addr) >= 9 && (addr[:9] == "127.0.0.1" || addr[:10] == "localhost:")
}

func portOnly(addr string) string {
	for i := len(addr) - 1; i >= 0; i-- {
		if addr[i] == ':' {
			return addr[i:]
		}
	}
	return ":8080"
}
