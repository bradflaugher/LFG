# LFG — build, test, lint.
.PHONY: all build test lint fmt vet install clean

VERSION ?= $(shell git describe --tags --always --dirty 2>/dev/null || echo dev)
LDFLAGS := -X main.Version=$(VERSION)

all: build

build:
	mkdir -p bin
	GOTOOLCHAIN=auto go build -ldflags "$(LDFLAGS)" -o bin/lfg ./cmd/lfg

test:
	GOTOOLCHAIN=auto go test -race ./...

lint:
	@command -v golangci-lint >/dev/null || { echo "golangci-lint not installed (https://golangci-lint.run/welcome/install/)"; exit 1; }
	GOTOOLCHAIN=auto golangci-lint run ./...

fmt:
	gofmt -w .
	@test -z "$$(gofmt -l . 2>&1)" || { echo "gofmt suggests changes"; exit 1; }

vet:
	GOTOOLCHAIN=auto go vet ./...

install: build
	install -m 0755 bin/lfg /usr/local/bin/lfg

clean:
	rm -rf bin
