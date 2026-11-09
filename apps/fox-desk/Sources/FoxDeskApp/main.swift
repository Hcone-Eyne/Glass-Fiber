import Foundation
#if canImport(FoxDeskCore)
import FoxDeskCore
#endif

// Fox Desk, Slice S: shell hook -> collector -> SQLite -> WebSocket -> phone.
//   swift run fox-desk            run the server (port 8787, or FOX_PORT)
//   swift run fox-desk --once     sense the repo once, print the task.update and exit

func resolveProtocolDir() -> URL {
    var dir = URL(fileURLWithPath: FileManager.default.currentDirectoryPath)
    for _ in 0..<6 {
        let candidate = dir.appendingPathComponent("packages/fox-protocol")
        if FileManager.default.fileExists(atPath: candidate.path) { return candidate }
        dir = dir.deletingLastPathComponent()
    }
    return URL(fileURLWithPath: FileManager.default.currentDirectoryPath).appendingPathComponent("packages/fox-protocol")
}

let protocolDir = resolveProtocolDir()
let repoRoot = protocolDir.deletingLastPathComponent().deletingLastPathComponent().path

do {
    let store = try FoxStore(path: try FoxPaths.database())
    let collector = FoxCollector(allowlistedRoots: try FoxPaths.allowlist(defaultRoot: repoRoot))
    let catalog = try FoxProtocolCatalog(protocolRoot: protocolDir)
    let node = FoxDeskNode(store: store, collector: collector, catalog: catalog, epoch: try FoxPaths.nextEpoch())

    if CommandLine.arguments.contains("--once") {
        switch collector.senseWorkspace(at: repoRoot) {
        case .task(let task):
            try store.upsertTask(task)
            let data = try node.encode(node.taskUpdateEnvelope(task))
            print(String(decoding: data, as: UTF8.self))
        case .state(let key, let detail):
            print("state \(key): \(detail)")
        }
        exit(0)
    }

    let token = try FoxPaths.devToken()
    let server = FoxLinkServer(node: node, token: token)
    let port = UInt16(ProcessInfo.processInfo.environment["FOX_PORT"] ?? "") ?? 8787

    // The hook fires on every prompt; sense a folder at most once a second.
    var lastSensed: [String: Date] = [:]
    let hook = HookListener(path: try FoxPaths.socket()) { event in
        guard let cwd = event["cwd"] as? String else { return }
        if let last = lastSensed[cwd], Date().timeIntervalSince(last) < 1 { return }
        lastSensed[cwd] = Date()
        switch collector.senseWorkspace(at: cwd) {
        case .task(let task):
            do {
                try store.upsertTask(task)
                server.broadcast(node.taskUpdateEnvelope(task))
            } catch {
                try? store.logFailure(stateKey: "corrupt", groupName: "Data and system", severity: "warn", detail: "Could not save task: \(error.localizedDescription)", ts: Int64(Date().timeIntervalSince1970 * 1000))
            }
        case .state(let key, let detail):
            // Silent states (outside the allowlist) are only logged; nothing is sent to the phone.
            let rec = catalog.statesByKey[key]
            try? store.logFailure(stateKey: key, groupName: rec?.group ?? "Task", severity: rec?.level ?? "silent", detail: detail, ts: Int64(Date().timeIntervalSince1970 * 1000))
            if let rec, rec.level != "silent" {
                server.broadcast(node.nextEnvelope(type: "state.event", idPrefix: "state", payload: ["state_id": key, "detail": detail]))
            }
        }
    }
    try hook.start()
    try server.start(port: port) { bound in
        print("Fox Desk is listening on port \(bound), epoch \(node.epoch).")
        print("Phone: connect to ws://<this Mac's IP>:\(bound) with header X-Fox-Token (token is in \(protocolDirHint())).")
    }
    signal(SIGINT) { _ in exit(0) }
    dispatchMain()
} catch {
    FileHandle.standardError.write(Data("Fox Desk could not start: \(error)\n".utf8))
    exit(1)
}

func protocolDirHint() -> String { (try? FoxPaths.appSupport().appendingPathComponent("dev-token").path) ?? "Application Support/FoxDesk/dev-token" }
