.PHONY: all test-all test-protocol test-desk run-desk test-mobile clean help

SWIFTC ?= swiftc
PYTHON ?= python3

all: test-all

help:
	@echo "Fox — Zero-Lag Native Monorepo Build & Test Suite"
	@echo ""
	@echo "Available targets:"
	@echo "  make test-protocol  Verify wire protocol schema & 53-state catalog"
	@echo "  make test-desk      Compile and run native Swift + C SQLite3 contract tests"
	@echo "  make run-desk       Build and execute live native FoxDesk daemon binary"
	@echo "  make test-mobile    Compile and run pure Kotlin mobile contract & spring suite"
	@echo "  make test-all       Run full end-to-end verification (protocol + desk + mobile)"
	@echo "  make clean          Clean temporary build and test caches"

test-protocol:
	@echo "==> [1/3] Verifying Fox Protocol (53 States & Wire Contract)..."
	$(PYTHON) packages/fox-protocol/tools/validate_protocol.py

test-desk:
	@echo "==> [2/3] Compiling and running native Swift FoxDesk contract tests..."
	@mkdir -p /tmp/fox-clang-cache
	$(SWIFTC) -O -module-cache-path /tmp/fox-clang-cache -parse-as-library \
		apps/fox-desk/Sources/FoxDeskCore/*.swift \
		apps/fox-desk/Tests/FoxDeskTests/ContractTests.swift \
		-lsqlite3 -o /tmp/fox-contract-tests
	/tmp/fox-contract-tests

run-desk:
	@echo "==> Building and running live FoxDesk native daemon..."
	@mkdir -p /tmp/fox-clang-cache
	$(SWIFTC) -O -module-cache-path /tmp/fox-clang-cache \
		apps/fox-desk/Sources/FoxDeskCore/*.swift \
		apps/fox-desk/Sources/FoxDeskApp/main.swift \
		-lsqlite3 -o /tmp/fox-desk-bin
	/tmp/fox-desk-bin --once

test-mobile:
	@echo "==> [3/3] Running pure Kotlin mobile unit & contract tests via Gradle..."
	cd apps/fox-mobile && JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home ./gradlew testDebugUnitTest

test-all: test-protocol test-desk test-mobile
	@echo ""
	@echo "========================================================"
	@echo "✓ ALL CONTRACT & ZERO-LAG NATIVE VERIFICATION GATES PASS"
	@echo "========================================================"

clean:
	rm -rf /tmp/fox-clang-cache /tmp/fox-contract-tests /tmp/fox-desk-bin /tmp/fox-test-*.sqlite3 /tmp/fox-desk-live.sqlite3 /tmp/fox-mobile-contract-classes
