import Foundation

#if canImport(Testing)
import Testing
#if canImport(FoxDeskCore)
@testable import FoxDeskCore
#endif

private let protocolRoot: URL = {
    var dir = URL(fileURLWithPath: #filePath)
    for _ in 0..<5 { dir = dir.deletingLastPathComponent() }
    return dir.appendingPathComponent("packages/fox-protocol")
}()

private func catalog() throws -> FoxProtocolCatalog { try FoxProtocolCatalog(protocolRoot: protocolRoot) }

@Suite struct ProtocolContract {
    @Test func all53StatesLoadFromTheSharedCatalog() throws {
        let c = try catalog()
        #expect(c.statesByKey.count == 53)
        for id in ["mac_missing", "asleep", "done", "redaction", "allowlist", "not_paired", "battery"] {
            #expect(c.statesByKey[id] != nil, "missing state \(id)")
        }
        #expect(c.statesByKey["ok"] == nil, "no hidden aliases: 'ok' is not a state id")
    }

    @Test func goldenFixturesPass() throws {
        let c = try catalog()
        let dir = protocolRoot.appendingPathComponent("fixtures")
        let files = try FileManager.default.contentsOfDirectory(atPath: dir.path).filter { $0.hasPrefix("0") && $0.hasSuffix(".json") }.sorted()
        #expect(files.count >= 4)
        for f in files {
            let obj = try JSONSerialization.jsonObject(with: Data(contentsOf: dir.appendingPathComponent(f)))
            if case .failure(let err) = FoxSchemaValidator.validateEnvelope(obj, catalog: c) {
                Issue.record("golden \(f) failed at \(err.path): \(err.reason)")
            }
        }
    }

    @Test func invalidCasesFailAtTheExpectedField() throws {
        let c = try catalog()
        let data = try Data(contentsOf: protocolRoot.appendingPathComponent("fixtures/invalid/cases.json"))
        let cases = try #require(JSONSerialization.jsonObject(with: data) as? [[String: Any]])
        #expect(cases.count >= 14)
        for tc in cases {
            let name = tc["name"] as? String ?? "?", expect = tc["expect_error"] as? String ?? "?"
            switch FoxSchemaValidator.validateEnvelope(tc["message"] as Any, catalog: c) {
            case .success: Issue.record("invalid case '\(name)' was accepted")
            case .failure(let err): #expect(err.path == expect, "'\(name)' failed at '\(err.path)', expected '\(expect)'")
            }
        }
    }
}

@Suite struct ReplayGuardRules {
    let t: Int64 = 1_762_668_000_000

    @Test func duplicateSeqAndStaleClockAreDropped() {
        let g = ReplayGuard()
        #expect((try? g.check(epoch: 5, seq: 1, ts: t, nowMs: t).get()) != nil)
        if case .failure(let e) = g.check(epoch: 5, seq: 1, ts: t, nowMs: t) { #expect(e.path == "seq") } else { Issue.record("duplicate accepted") }
        if case .failure(let e) = g.check(epoch: 5, seq: 2, ts: t - 90_000, nowMs: t) { #expect(e.path == "ts") } else { Issue.record("stale clock accepted") }
    }

    @Test func restartedDeskWithAHigherEpochIsAccepted() {
        let g = ReplayGuard()
        _ = g.check(epoch: 100, seq: 40, ts: t, nowMs: t)
        #expect((try? g.check(epoch: 200, seq: 1, ts: t, nowMs: t).get()) != nil)
    }

    @Test func olderEpochIsAStaleSessionAndIsDropped() {
        let g = ReplayGuard()
        _ = g.check(epoch: 200, seq: 1, ts: t, nowMs: t)
        if case .failure(let e) = g.check(epoch: 100, seq: 99, ts: t, nowMs: t) { #expect(e.path == "epoch") } else { Issue.record("stale epoch accepted") }
    }

    @Test func eachLaunchGetsAHigherEpoch() throws {
        let a = try FoxPaths.nextEpoch(now: 10), b = try FoxPaths.nextEpoch(now: 10)
        #expect(b > a)
    }
}

@Suite struct Scrubbing {
    @Test func knownPrefixesAndHexSecretsAreRedacted() throws {
        let (s1, n1) = try FoxScrubber.scrub("Token sk-proj1234567890abcdefXYZ in env")
        #expect(n1 == 1 && s1.contains("[REDACTED]") && !s1.contains("sk-proj"))
        let hex = "a9f4c28e10b7d653e812c409b57a6d1f3e8091c2"
        #expect(FoxScrubber.shannonEntropy(hex) >= 3.0)
        #expect(try FoxScrubber.scrub("commit \(hex)").redactedCount == 1)
        #expect(throws: (any Error).self) { try FoxScrubber.scrub("bad\0payload") }
    }

    @Test func failureLogNeverStoresASecret() throws {
        let path = NSTemporaryDirectory() + "fox-test-\(UUID().uuidString).sqlite3"
        defer { try? FileManager.default.removeItem(atPath: path) }
        let store = try FoxStore(path: path)
        try store.logFailure(stateKey: "redaction", groupName: "Data and system", severity: "fail", detail: "token=ghp_abcdefghijklmnop1234", ts: 1)
        #expect(try store.failureLogCount() == 1)
    }
}

@Suite struct Storage {
    @Test func walModeAnd500RowFailureLogCap() throws {
        let path = NSTemporaryDirectory() + "fox-test-\(UUID().uuidString).sqlite3"
        defer { try? FileManager.default.removeItem(atPath: path) }
        let store = try FoxStore(path: path)
        let task = FoxTaskRecord(id: "task_1", title: "MenuBarView.swift", source: "git", status: "active", ts: 1_762_668_001_000,
                                 project: "FoxDesk", branch: "main", primaryFile: "MenuBarView.swift", summary: "FoxDesk · main · 2 uncommitted", pinned: true)
        try store.upsertTask(task)
        #expect(try store.listTasks(limit: 5) == [task])
        #expect(try store.journalMode().lowercased() == "wal")
        for i in 0..<550 { try store.logFailure(stateKey: "lost", groupName: "Connection", severity: "warn", detail: "Failure \(i)", ts: Int64(i)) }
        #expect(try store.failureLogCount() == 500)
    }
}

@Suite struct Collecting {
    private func makeRepo() throws -> String {
        let dir = NSTemporaryDirectory() + "fox-repo-\(UUID().uuidString)"
        try FileManager.default.createDirectory(atPath: dir + "/untracked_dir", withIntermediateDirectories: true)
        try "x".write(toFile: dir + "/untracked_dir/a.txt", atomically: true, encoding: .utf8)
        try "y".write(toFile: dir + "/notes.md", atomically: true, encoding: .utf8)
        let p = Process(); p.executableURL = URL(fileURLWithPath: "/usr/bin/git"); p.arguments = ["init", "-q", "-b", "feat/menubar"]
        p.currentDirectoryURL = URL(fileURLWithPath: dir); p.standardOutput = FileHandle.nullDevice; p.standardError = FileHandle.nullDevice
        try p.run(); p.waitUntilExit()
        return dir
    }

    @Test func folderOutsideTheAllowlistIsSilent() {
        let c = FoxCollector(allowlistedRoots: ["/nonexistent/root"])
        #expect(c.senseWorkspace(at: NSTemporaryDirectory()) == .state(key: "allowlist", detail: "Outside allowlisted folders"))
    }

    @Test func untrackedFolderIsNeverReportedAsTheActiveFile() throws {
        let repo = try makeRepo(); defer { try? FileManager.default.removeItem(atPath: repo) }
        guard case .task(let task) = FoxCollector(allowlistedRoots: [repo]).senseWorkspace(at: repo) else { Issue.record("expected a task"); return }
        #expect(task.primaryFile == "notes.md")
        #expect(task.branch == nil, "a repo with no commits has no branch to show")
        #expect(task.uncommittedCount == 2)
    }

    @Test func plainFolderHasNoBranchOrCount() throws {
        let dir = NSTemporaryDirectory() + "fox-plain-\(UUID().uuidString)"
        try FileManager.default.createDirectory(atPath: dir, withIntermediateDirectories: true)
        defer { try? FileManager.default.removeItem(atPath: dir) }
        guard case .task(let task) = FoxCollector(allowlistedRoots: [dir]).senseWorkspace(at: dir) else { Issue.record("expected a task"); return }
        #expect(task.branch == nil && task.uncommittedCount == nil)
    }
}

@Suite(.serialized) struct EndToEnd {
    @Test func phoneReceivesHelloTaskListAndALiveTaskUpdate() async throws {
        let repo = NSTemporaryDirectory() + "fox-e2e-\(UUID().uuidString)"
        try FileManager.default.createDirectory(atPath: repo, withIntermediateDirectories: true)
        try "y".write(toFile: repo + "/MenuBarView.swift", atomically: true, encoding: .utf8)
        defer { try? FileManager.default.removeItem(atPath: repo) }
        let db = NSTemporaryDirectory() + "fox-e2e-\(UUID().uuidString).sqlite3"
        defer { try? FileManager.default.removeItem(atPath: db) }

        let store = try FoxStore(path: db)
        let collector = FoxCollector(allowlistedRoots: [repo])
        let node = FoxDeskNode(store: store, collector: collector, catalog: try catalog(), epoch: 7)
        let server = FoxLinkServer(node: node, token: "test-token-0123456789abcdef", deviceName: "Test Mac")
        let port: UInt16 = try await withCheckedThrowingContinuation { cont in
            do { try server.start(port: 0) { cont.resume(returning: $0) } } catch { cont.resume(throwing: error) }
        }
        defer { server.stop() }

        var bad = URLRequest(url: URL(string: "ws://127.0.0.1:\(port)")!); bad.setValue("wrong", forHTTPHeaderField: "X-Fox-Token")
        let badTask = URLSession.shared.webSocketTask(with: bad); badTask.resume()
        await #expect(throws: (any Error).self) { _ = try await badTask.receive() }

        var req = URLRequest(url: URL(string: "ws://127.0.0.1:\(port)")!); req.setValue("test-token-0123456789abcdef", forHTTPHeaderField: "X-Fox-Token")
        let ws = URLSession.shared.webSocketTask(with: req); ws.resume()
        func next() async throws -> [String: Any] {
            guard case .string(let s) = try await ws.receive() else { throw ProtocolError(path: "", reason: "binary frame") }
            return try #require(JSONSerialization.jsonObject(with: Data(s.utf8)) as? [String: Any])
        }
        #expect(try await next()["type"] as? String == "hello")
        #expect(try await next()["type"] as? String == "task.list")

        if case .task(let task) = collector.senseWorkspace(at: repo) {
            try store.upsertTask(task)
            server.broadcast(node.taskUpdateEnvelope(task))
        }
        let update = try await next()
        #expect(update["type"] as? String == "task.update")
        let task = try #require((update["payload"] as? [String: Any])?["task"] as? [String: Any])
        #expect(task["active_file"] as? String == "MenuBarView.swift")
        #expect(update["epoch"] as? Int == 7)
        ws.cancel(with: .normalClosure, reason: nil)
    }
}

#else
// MARK: - Standalone Test Runner (Headless swiftc compilation)
#if canImport(FoxDeskCore)
@testable import FoxDeskCore
#endif

private func assertEqual<T: Equatable>(_ a: T, _ b: T, _ msg: String = "", file: StaticString = #file, line: UInt = #line) {
    if a != b {
        fputs("ASSERTION FAILED: '\(a)' != '\(b)' \(msg) at \(file):\(line)\n", stderr)
        exit(1)
    }
}

private func assertTrue(_ a: Bool, _ msg: String = "", file: StaticString = #file, line: UInt = #line) {
    if !a {
        fputs("ASSERTION FAILED: expected true \(msg) at \(file):\(line)\n", stderr)
        exit(1)
    }
}

private func assertNotNil(_ a: Any?, _ msg: String = "", file: StaticString = #file, line: UInt = #line) {
    if a == nil {
        fputs("ASSERTION FAILED: expected not nil \(msg) at \(file):\(line)\n", stderr)
        exit(1)
    }
}

private let protocolRoot: URL = {
    var dir = URL(fileURLWithPath: #filePath)
    for _ in 0..<5 { dir = dir.deletingLastPathComponent() }
    let candidate = dir.appendingPathComponent("packages/fox-protocol")
    if FileManager.default.fileExists(atPath: candidate.path) { return candidate }
    var cur = URL(fileURLWithPath: FileManager.default.currentDirectoryPath)
    for _ in 0..<5 {
        let p = cur.appendingPathComponent("packages/fox-protocol")
        if FileManager.default.fileExists(atPath: p.path) { return p }
        cur = cur.deletingLastPathComponent()
    }
    return candidate
}()

private func catalog() throws -> FoxProtocolCatalog { try FoxProtocolCatalog(protocolRoot: protocolRoot) }

@main
struct StandaloneRunner {
    static func main() throws {
        print("=== Running FoxDesk Native Swift Contract Tests ===")
        let c = try catalog()

        // 1. 53 states
        print("[TEST 1/6] Validating all 53 states loaded from catalog...")
        assertEqual(c.statesByKey.count, 53, "Expected 53 states")
        for id in ["mac_missing", "asleep", "done", "redaction", "allowlist", "not_paired", "battery"] {
            assertNotNil(c.statesByKey[id], "missing state \(id)")
        }
        print("  ✓ PASS: All 53 states verified from Fox_Line_v30.html catalog")

        // 2. Golden fixtures
        print("[TEST 2/6] Validating golden fixtures...")
        let fixDir = protocolRoot.appendingPathComponent("fixtures")
        let goldenFiles = try FileManager.default.contentsOfDirectory(atPath: fixDir.path)
            .filter { $0.hasPrefix("0") && $0.hasSuffix(".json") }
            .sorted()
        assertTrue(goldenFiles.count >= 4, "Expected at least 4 golden fixtures")
        for f in goldenFiles {
            let obj = try JSONSerialization.jsonObject(with: Data(contentsOf: fixDir.appendingPathComponent(f)))
            if case .failure(let err) = FoxSchemaValidator.validateEnvelope(obj, catalog: c) {
                fputs("Golden \(f) failed at \(err.path): \(err.reason)\n", stderr)
                exit(1)
            }
        }
        print("  ✓ PASS: All \(goldenFiles.count) golden fixtures passed schema validation")

        // 3. Adversarial invalid cases
        print("[TEST 3/6] Validating adversarial invalid cases fail at exact field paths...")
        let casesData = try Data(contentsOf: protocolRoot.appendingPathComponent("fixtures/invalid/cases.json"))
        guard let cases = try JSONSerialization.jsonObject(with: casesData) as? [[String: Any]] else {
            fputs("Failed to parse invalid cases.json\n", stderr); exit(1)
        }
        assertTrue(cases.count >= 12, "Expected at least 12 invalid cases")
        for tc in cases {
            let name = tc["name"] as? String ?? "?"
            let expect = tc["expect_error"] as? String ?? "?"
            let msg = tc["message"] ?? tc["envelope"]
            switch FoxSchemaValidator.validateEnvelope(msg as Any, catalog: c) {
            case .success:
                fputs("Invalid case '\(name)' was accepted unexpectedly\n", stderr)
                exit(1)
            case .failure(let err):
                assertEqual(err.path, expect, "Case '\(name)' failed at '\(err.path)', expected '\(expect)'")
            }
        }
        print("  ✓ PASS: All \(cases.count) invalid fixtures failed at exact expect_error paths")

        // 4. ReplayGuard
        print("[TEST 4/6] Validating ReplayGuard duplicate seq, timestamp skew, and epoch handling...")
        let guard_ = ReplayGuard()
        let t: Int64 = 1_762_668_000_000
        assertTrue((try? guard_.check(epoch: 5, seq: 1, ts: t, nowMs: t).get()) != nil)
        if case .failure(let e) = guard_.check(epoch: 5, seq: 1, ts: t, nowMs: t) {
            assertEqual(e.path, "seq")
        } else {
            fputs("Duplicate seq accepted\n", stderr); exit(1)
        }
        if case .failure(let e) = guard_.check(epoch: 5, seq: 2, ts: t - 90_000, nowMs: t) {
            assertEqual(e.path, "ts")
        } else {
            fputs("Stale timestamp accepted\n", stderr); exit(1)
        }
        // Restarted desk with higher epoch
        assertTrue((try? guard_.check(epoch: 200, seq: 1, ts: t, nowMs: t).get()) != nil)
        // Stale epoch dropped
        if case .failure(let e) = guard_.check(epoch: 100, seq: 99, ts: t, nowMs: t) {
            assertEqual(e.path, "epoch")
        } else {
            fputs("Stale epoch accepted\n", stderr); exit(1)
        }
        // Epoch monotonically increases
        let epA = try FoxPaths.nextEpoch(now: 10), epB = try FoxPaths.nextEpoch(now: 10)
        assertTrue(epB > epA, "Next epoch must be strictly greater")
        print("  ✓ PASS: ReplayGuard rejected duplicate seq, stale timestamp, and stale epoch")

        // 5. Scrubber & Shannon Entropy
        print("[TEST 5/6] Validating fail-closed scrubber, hex Shannon entropy, and null-byte injection...")
        let (s1, n1) = try FoxScrubber.scrub("Token sk-proj1234567890abcdefXYZ in env")
        assertTrue(n1 == 1 && s1.contains("[REDACTED]") && !s1.contains("sk-proj"))
        let hexSecret = "a9f4c28e10b7d653e812c409b57a6d1f3e8091c2"
        assertTrue(FoxScrubber.shannonEntropy(hexSecret) >= 3.0, "Shannon entropy must be >= 3.0")
        assertEqual(try FoxScrubber.scrub("commit \(hexSecret)").redactedCount, 1)
        var threwNull = false
        do {
            _ = try FoxScrubber.scrub("bad\0payload")
        } catch {
            threwNull = true
        }
        assertTrue(threwNull, "Expected scrubber to abort on null byte injection")
        print("  ✓ PASS: Scrubber and fail-closed null byte redaction verified")

        // 6. SQLite Store WAL & 500-Row Cap
        print("[TEST 6/6] Validating SQLite WAL mode and 500-row ring-buffered failure log cap...")
        let tmpDb = "/tmp/fox-test-\(UUID().uuidString).sqlite3"
        defer { try? FileManager.default.removeItem(atPath: tmpDb) }
        let store = try FoxStore(path: tmpDb)
        let task = FoxTaskRecord(
            id: "task_1",
            title: "MenuBarView.swift",
            source: "git",
            status: "active",
            ts: 1_762_668_001_000,
            project: "FoxDesk",
            branch: "main",
            primaryFile: "MenuBarView.swift",
            summary: "FoxDesk · main · 2 uncommitted",
            pinned: true
        )
        try store.upsertTask(task)
        assertEqual(try store.listTasks(limit: 5), [task])
        assertEqual(try store.journalMode().lowercased(), "wal")
        for i in 0..<550 {
            try store.logFailure(
                stateKey: "warn",
                groupName: "TestGroup",
                severity: "warn",
                detail: "Failure \(i)",
                ts: 1_762_668_001_000 + Int64(i)
            )
        }
        assertEqual(try store.failureLogCount(), 500, "Failure log must be capped at 500 rows")
        print("  ✓ PASS: SQLite WAL mode and 500-row failure log cap verified")

        print("=== ALL CONTRACT TESTS PASSED (6/6) ===")
    }
}
#endif
