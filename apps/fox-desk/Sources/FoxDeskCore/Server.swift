import Foundation
import Network

/// Builds outgoing envelopes and checks incoming ones for one Fox Desk run.
public final class FoxDeskNode {
    public let epoch: Int64
    public private(set) var seq: Int64 = 0
    public let store: FoxStore
    public let collector: FoxCollector
    public let catalog: FoxProtocolCatalog
    private let lock = NSLock()

    /// `epoch` must be fresh for every launch (see `FoxPaths.nextEpoch`); a fixed epoch makes
    /// a phone reject a restarted Fox Desk as a replay.
    public init(store: FoxStore, collector: FoxCollector, catalog: FoxProtocolCatalog, epoch: Int64) {
        self.store = store
        self.collector = collector
        self.catalog = catalog
        self.epoch = epoch
    }

    public func nextEnvelope(
        type: String,
        idPrefix: String,
        ts: Int64 = Int64(Date().timeIntervalSince1970 * 1000),
        payload: [String: Any]
    ) -> [String: Any] {
        lock.lock(); seq += 1; let s = seq; lock.unlock()
        var cleanPrefix = idPrefix
        if cleanPrefix.hasPrefix("msg_") { cleanPrefix = String(cleanPrefix.dropFirst(4)) }
        cleanPrefix = cleanPrefix.replacingOccurrences(of: "[^A-Za-z0-9]", with: "", options: .regularExpression)
        if cleanPrefix.isEmpty { cleanPrefix = "live" }
        let msgId = "msg_\(cleanPrefix.prefix(20))\(String(format: "%06lld", s))"
        return ["v": 1, "type": type, "id": msgId, "epoch": epoch, "seq": s, "ts": ts, "payload": payload]
    }

    /// Checks an incoming envelope: schema first, then the caller's per-connection replay guard.
    /// The schema's type list is the Slice S blast-radius rule: no type exists that makes the Mac act.
    public func receiveEnvelope(
        _ env: [String: Any],
        replayGuard: ReplayGuard,
        nowMs: Int64 = Int64(Date().timeIntervalSince1970 * 1000)
    ) -> Result<String, FoxValidationError> {
        if case .failure(let err) = FoxSchemaValidator.validateEnvelope(env, catalog: catalog) { return .failure(err) }
        guard let epoch = (env["epoch"] as? NSNumber)?.int64Value,
              let seq = (env["seq"] as? NSNumber)?.int64Value,
              let ts = (env["ts"] as? NSNumber)?.int64Value,
              let type = env["type"] as? String else {
            return .failure(FoxValidationError(path: "", reason: "invalid envelope structure"))
        }
        if case .failure(let err) = replayGuard.check(epoch: epoch, seq: seq, ts: ts, nowMs: nowMs) { return .failure(err) }
        return .success(type)
    }

    public func encode(_ env: [String: Any]) throws -> Data {
        let validated = try catalog.validateMessage(env)   // never send what we would reject
        return try JSONSerialization.data(withJSONObject: validated, options: [.sortedKeys])
    }

    public func helloEnvelope(deviceName: String) -> [String: Any] {
        nextEnvelope(type: "hello", idPrefix: "hello", payload: [
            "role": "desk", "device_name": String(deviceName.prefix(64)), "app_version": "0.1.0", "protocol": 1
        ])
    }

    public func taskListEnvelope(limit: Int = 20) throws -> [String: Any] {
        let tasks = try store.listTasks(limit: limit).map { $0.toEnvelopePayload() }
        return nextEnvelope(type: "task.list", idPrefix: "list", payload: ["tasks": tasks])
    }

    public func taskUpdateEnvelope(_ task: FoxTaskRecord) -> [String: Any] {
        nextEnvelope(type: "task.update", idPrefix: "task", payload: ["task": task.toEnvelopePayload()])
    }
}

/// FoxLink over a LAN WebSocket. Slice S: Show rung only, guarded by the dev token.
/// The phone must send the token in the `X-Fox-Token` header; anything else is refused before upgrade.
public final class FoxLinkServer {
    public let node: FoxDeskNode
    public private(set) var port: UInt16 = 0
    private let token: String
    private let deviceName: String
    private let queue = DispatchQueue(label: "fox.link")
    private var listener: NWListener?
    private var clients: [ObjectIdentifier: (conn: NWConnection, guard: ReplayGuard)] = [:]

    public init(node: FoxDeskNode, token: String, deviceName: String = Host.current().localizedName ?? "Mac") {
        self.node = node
        self.token = token
        self.deviceName = deviceName
    }

    public var clientCount: Int { queue.sync { clients.count } }

    /// Starts listening; port 0 picks a free port. Calls `ready` with the bound port.
    public func start(port: UInt16, ready: @escaping (UInt16) -> Void = { _ in }) throws {
        let ws = NWProtocolWebSocket.Options()
        ws.autoReplyPing = true
        ws.maximumMessageSize = 64 * 1024
        let token = self.token
        ws.setClientRequestHandler(queue) { _, headers in
            let sent = headers.first { $0.name.lowercased() == "x-fox-token" }?.value ?? ""
            return NWProtocolWebSocket.Response(status: constantTimeEquals(sent, token) ? .accept : .reject, subprotocol: nil)
        }
        let params = NWParameters.tcp
        params.defaultProtocolStack.applicationProtocols.insert(ws, at: 0)
        let l = try NWListener(using: params, on: NWEndpoint.Port(rawValue: port) ?? .any)
        l.newConnectionHandler = { [weak self] conn in self?.accept(conn) }
        l.stateUpdateHandler = { [weak self] state in
            if case .ready = state, let p = l.port?.rawValue { self?.port = p; ready(p) }
        }
        listener = l
        l.start(queue: queue)
    }

    public func stop() {
        queue.sync {
            for c in clients.values { c.conn.cancel() }
            clients.removeAll()
            listener?.cancel()
        }
    }

    /// Sends one envelope to every connected phone.
    public func broadcast(_ env: [String: Any]) {
        guard let data = try? node.encode(env) else { return }
        queue.async { for c in self.clients.values { self.send(data, on: c.conn) } }
    }

    private func accept(_ conn: NWConnection) {
        let key = ObjectIdentifier(conn)
        clients[key] = (conn, ReplayGuard())
        conn.stateUpdateHandler = { [weak self] state in
            switch state {
            case .ready: self?.greet(conn)
            case .failed, .cancelled: self?.clients[key] = nil
            default: break
            }
        }
        conn.start(queue: queue)
        receive(on: conn)
    }

    private func greet(_ conn: NWConnection) {
        for env in [node.helloEnvelope(deviceName: deviceName), (try? node.taskListEnvelope()) ?? [:]] where !env.isEmpty {
            if let data = try? node.encode(env) { send(data, on: conn) }
        }
    }

    private func send(_ data: Data, on conn: NWConnection) {
        let meta = NWProtocolWebSocket.Metadata(opcode: .text)
        let ctx = NWConnection.ContentContext(identifier: "fox", metadata: [meta])
        conn.send(content: data, contentContext: ctx, isComplete: true, completion: .contentProcessed { _ in })
    }

    private func receive(on conn: NWConnection) {
        conn.receiveMessage { [weak self] data, _, _, error in
            guard let self else { return }
            if let data, let guardEngine = self.clients[ObjectIdentifier(conn)]?.guard {
                self.handle(data, guardEngine: guardEngine)
            }
            if error == nil { self.receive(on: conn) } else { conn.cancel() }
        }
    }

    private func handle(_ data: Data, guardEngine: ReplayGuard) {
        let now = Int64(Date().timeIntervalSince1970 * 1000)
        guard let obj = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            try? node.store.logFailure(stateKey: "replay", groupName: "Pairing and security", severity: "silent", detail: "Unreadable message dropped", ts: now)
            return
        }
        switch node.receiveEnvelope(obj, replayGuard: guardEngine, nowMs: now) {
        case .failure(let err):
            let key = err.path == "ts" ? "skew" : "replay"
            try? node.store.logFailure(stateKey: key, groupName: "Pairing and security", severity: "silent", detail: "Dropped at \(err.path): \(err.reason)", ts: now)
        case .success(let type):
            if type == "state.event", let p = obj["payload"] as? [String: Any], let sid = p["state_id"] as? String,
               let rec = node.catalog.statesByKey[sid] {
                try? node.store.logFailure(stateKey: sid, groupName: rec.group, severity: rec.level, detail: (p["detail"] as? String) ?? rec.title, ts: now)
            }
        }
    }
}

private func constantTimeEquals(_ a: String, _ b: String) -> Bool {
    let x = Array(a.utf8), y = Array(b.utf8)
    guard x.count == y.count, !y.isEmpty else { return false }
    var diff: UInt8 = 0
    for i in 0..<x.count { diff |= x[i] ^ y[i] }
    return diff == 0
}

/// Listens on a Unix socket for the zsh hook. The socket lives in Fox Desk's private folder
/// and is readable and writable by this user only. Each connection carries one small JSON object.
public final class HookListener {
    private let path: String
    private var fd: Int32 = -1
    private let onEvent: ([String: Any]) -> Void

    public init(path: String, onEvent: @escaping ([String: Any]) -> Void) {
        self.path = path
        self.onEvent = onEvent
    }

    public func start() throws {
        unlink(path)
        fd = socket(AF_UNIX, SOCK_STREAM, 0)
        guard fd >= 0 else { throw ProtocolError(path: "", reason: "socket() failed") }
        var addr = sockaddr_un()
        addr.sun_family = sa_family_t(AF_UNIX)
        let bytes = Array(path.utf8)
        guard bytes.count < MemoryLayout.size(ofValue: addr.sun_path) else { throw ProtocolError(path: "", reason: "socket path too long") }
        withUnsafeMutableBytes(of: &addr.sun_path) { buf in
            buf.copyBytes(from: bytes)
            buf[bytes.count] = 0
        }
        let len = socklen_t(MemoryLayout<sockaddr_un>.size)
        let rc = withUnsafePointer(to: &addr) { $0.withMemoryRebound(to: sockaddr.self, capacity: 1) { bind(fd, $0, len) } }
        guard rc == 0 else { throw ProtocolError(path: "", reason: "bind() failed for \(path)") }
        chmod(path, 0o600)
        guard listen(fd, 16) == 0 else { throw ProtocolError(path: "", reason: "listen() failed") }
        let serverFd = fd
        Thread.detachNewThread { [weak self] in
            while true {
                let c = accept(serverFd, nil, nil)
                if c < 0 { break }
                var buf = [UInt8](repeating: 0, count: 4096), data = Data()
                while data.count < 4096 {
                    let n = read(c, &buf, buf.count)
                    if n <= 0 { break }
                    data.append(buf, count: n)
                }
                close(c)
                if let obj = try? JSONSerialization.jsonObject(with: data) as? [String: Any] {
                    DispatchQueue.main.async { self?.onEvent(obj) }
                }
            }
        }
    }

    public func stop() {
        if fd >= 0 { close(fd); fd = -1 }
        unlink(path)
    }
}
