import Foundation
import Security

/// Where Fox Desk keeps its files: ~/Library/Application Support/FoxDesk, readable by this user only.
/// Never /tmp, which every user on the Mac can read.
public enum FoxPaths {
    public static func appSupport() throws -> URL {
        if let custom = ProcessInfo.processInfo.environment["FOX_DATA_DIR"] ?? ProcessInfo.processInfo.environment["FOX_APP_SUPPORT_DIR"], !custom.isEmpty {
            let dir = URL(fileURLWithPath: custom)
            try FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true, attributes: [.posixPermissions: 0o700])
            return dir
        }
        do {
            let base = try FileManager.default.url(for: .applicationSupportDirectory, in: .userDomainMask, appropriateFor: nil, create: true)
            let dir = base.appendingPathComponent("FoxDesk", isDirectory: true)
            try FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true, attributes: [.posixPermissions: 0o700])
            let probe = dir.appendingPathComponent(".probe_\(ProcessInfo.processInfo.processIdentifier)")
            try "probe".write(to: probe, atomically: true, encoding: .utf8)
            try? FileManager.default.removeItem(at: probe)
            try? FileManager.default.setAttributes([.posixPermissions: 0o700], ofItemAtPath: dir.path)
            return dir
        } catch {
            let fallback = URL(fileURLWithPath: "/tmp/FoxDesk", isDirectory: true)
            try FileManager.default.createDirectory(at: fallback, withIntermediateDirectories: true, attributes: [.posixPermissions: 0o700])
            return fallback
        }
    }

    public static func database() throws -> String { try appSupport().appendingPathComponent("fox-desk.sqlite3").path }
    public static func socket() throws -> String { try appSupport().appendingPathComponent("desk.sock").path }

    /// The dev token for the Show rung: random, created once, readable by this user only.
    public static func devToken() throws -> String {
        let url = try appSupport().appendingPathComponent("dev-token")
        if let existing = try? String(contentsOf: url, encoding: .utf8).trimmingCharacters(in: .whitespacesAndNewlines), existing.count >= 24 {
            return existing
        }
        var bytes = [UInt8](repeating: 0, count: 24)
        guard SecRandomCopyBytes(kSecRandomDefault, bytes.count, &bytes) == errSecSuccess else {
            throw ProtocolError(path: "", reason: "could not create a random dev token")
        }
        let token = bytes.map { String(format: "%02x", $0) }.joined()
        try token.write(to: url, atomically: true, encoding: .utf8)
        try FileManager.default.setAttributes([.posixPermissions: 0o600], ofItemAtPath: url.path)
        return token
    }

    /// Folders Fox may look at, one per line. Created with the repo root on first run.
    public static func allowlist(defaultRoot: String) throws -> [String] {
        let url = try appSupport().appendingPathComponent("allowlist.txt")
        if !FileManager.default.fileExists(atPath: url.path) {
            try (defaultRoot + "\n").write(to: url, atomically: true, encoding: .utf8)
            try FileManager.default.setAttributes([.posixPermissions: 0o600], ofItemAtPath: url.path)
        }
        return try String(contentsOf: url, encoding: .utf8)
            .split(separator: "\n")
            .map { $0.trimmingCharacters(in: .whitespaces) }
            .filter { !$0.isEmpty && !$0.hasPrefix("#") }
            .map { NSString(string: $0).expandingTildeInPath }
    }

    /// A fresh epoch for every launch, always higher than the last one, so a phone never mistakes
    /// a restarted Fox Desk for a replay and never accepts an older session again.
    public static func nextEpoch(now: Int64 = Int64(Date().timeIntervalSince1970 * 1000)) throws -> Int64 {
        let url = try appSupport().appendingPathComponent("epoch")
        let last = (try? String(contentsOf: url, encoding: .utf8)).flatMap { Int64($0.trimmingCharacters(in: .whitespacesAndNewlines)) } ?? 0
        let next = max(now, last + 1)
        try String(next).write(to: url, atomically: true, encoding: .utf8)
        return next
    }
}
