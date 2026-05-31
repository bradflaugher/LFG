## 2024-05-31 - Constant-Time String Comparison Length Leak

**Vulnerability:** The custom `secureEqual` function in `internal/server/auth.go` returned early if the lengths of the provided token and the expected API key were different. This could leak the length of the secret API key through a timing side channel.
**Learning:** Returning early based on string length in a constant-time comparison defeats the purpose of the constant-time check, as it allows attackers to determine the exact length of the secret.
**Prevention:** Hash the inputs (e.g., using SHA-256) before performing the constant-time comparison using `crypto/subtle.ConstantTimeCompare`. This ensures both inputs are always the same length (the hash size) before comparison, masking the true secret's length while providing a robust constant-time check.
