package server

import (
	"crypto/sha256"
	"crypto/subtle"
	"net/http"
	"strings"
)

// authMiddleware enforces a bearer-token check when LFG_API_KEY is configured.
// When the key is unset, the server is in local-only mode and skips the check entirely
// (see ListenAndServe — it also refuses to bind anything but loopback in that case).
func (s *Server) authMiddleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if s.cfg.APIKey == "" {
			next.ServeHTTP(w, r)
			return
		}
		header := r.Header.Get("Authorization")
		const prefix = "Bearer "
		if !strings.HasPrefix(header, prefix) {
			writeJSONError(w, http.StatusUnauthorized, "missing Bearer token")
			return
		}
		token := strings.TrimPrefix(header, prefix)
		// constant-time compare in case the secret length varies
		if !secureEqual(token, s.cfg.APIKey) {
			writeJSONError(w, http.StatusUnauthorized, "invalid token")
			return
		}
		next.ServeHTTP(w, r)
	})
}

// secureEqual is a constant-time string comparison to avoid timing-based token leaks.
// It hashes both strings to avoid leaking the token length via early returns.
func secureEqual(a, b string) bool {
	hashA := sha256.Sum256([]byte(a))
	hashB := sha256.Sum256([]byte(b))
	return subtle.ConstantTimeCompare(hashA[:], hashB[:]) == 1
}
