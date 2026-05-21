package agent

import (
	"context"
	"strings"
	"testing"
	"time"

	"charm.land/fantasy"
)

func TestBashTool_Success(t *testing.T) {
	tool := BashTool(5 * time.Second)
	resp, err := tool.Run(context.Background(), fantasy.ToolCall{
		Input: `{"command":"echo hello"}`,
	})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	text := toolText(t, resp)
	if !strings.Contains(text, "hello") {
		t.Errorf("expected output to contain 'hello', got %q", text)
	}
	if !strings.Contains(text, "[exit 0]") {
		t.Errorf("expected exit-status marker, got %q", text)
	}
}

func TestBashTool_NonZeroExit(t *testing.T) {
	tool := BashTool(5 * time.Second)
	resp, err := tool.Run(context.Background(), fantasy.ToolCall{
		Input: `{"command":"exit 7"}`,
	})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	text := toolText(t, resp)
	if !strings.Contains(text, "[exit 7]") {
		t.Errorf("expected exit-7 marker, got %q", text)
	}
}

func TestBashTool_Timeout(t *testing.T) {
	tool := BashTool(100 * time.Millisecond)
	resp, err := tool.Run(context.Background(), fantasy.ToolCall{
		Input: `{"command":"sleep 5"}`,
	})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	text := toolText(t, resp)
	if !strings.Contains(text, "timed out") {
		t.Errorf("expected timeout marker, got %q", text)
	}
}

func TestBashTool_OutputTruncation(t *testing.T) {
	tool := BashTool(5 * time.Second)
	// 200KB of zeros — well over the 64KB cap.
	resp, err := tool.Run(context.Background(), fantasy.ToolCall{
		Input: `{"command":"yes | head -c 204800"}`,
	})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	text := toolText(t, resp)
	if !strings.Contains(text, "[truncated") {
		t.Errorf("expected truncation marker for large output, got %d bytes", len(text))
	}
}

func toolText(t *testing.T, resp fantasy.ToolResponse) string {
	t.Helper()
	if resp.Type != "text" {
		t.Fatalf("response type = %q, want text", resp.Type)
	}
	return resp.Content
}
