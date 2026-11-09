import Foundation

public enum CollectorResult: Equatable {
    case task(FoxTaskRecord)
    case state(key: String, detail: String)
}

public final class FoxCollector {
    public var allowlistedRoots: [String]

    public init(allowlistedRoots: [String]) {
        self.allowlistedRoots = allowlistedRoots.map {
            URL(fileURLWithPath: $0).standardizedFileURL.resolvingSymlinksInPath().path
        }
    }

    public func isAllowlisted(path: String) -> Bool {
        let standardized = URL(fileURLWithPath: path).standardizedFileURL.resolvingSymlinksInPath().path
        return allowlistedRoots.contains { root in
            standardized == root || standardized.hasPrefix(root + "/")
        }
    }

    /// Senses the project around `directory`. Only folders inside the allowlist are looked at;
    /// everything that leaves this function has been through the scrubber, or nothing leaves at all.
    public func senseWorkspace(at directory: String, nowMs: Int64 = Int64(Date().timeIntervalSince1970 * 1000)) -> CollectorResult {
        let standardized = URL(fileURLWithPath: directory).standardizedFileURL.resolvingSymlinksInPath().path
        guard isAllowlisted(path: standardized) else {
            return .state(key: "allowlist", detail: "Outside allowlisted folders")
        }

        // Work from the repository root when there is one, so every folder in a repo is the same task.
        let repoRoot = runGit(["rev-parse", "--show-toplevel"], cwd: standardized)
        let root = repoRoot ?? standardized
        let projectName = URL(fileURLWithPath: root).lastPathComponent
        var branch: String? = nil
        var dirtyLines: [String] = []
        if repoRoot != nil {
            let head = runGit(["rev-parse", "--abbrev-ref", "HEAD"], cwd: root)
            branch = (head == nil || head == "HEAD") ? nil : head   // detached HEAD or no commits: no branch to show
            dirtyLines = (runGit(["status", "--porcelain"], cwd: root) ?? "")
                .split(separator: "\n").map(String.init)
                .filter { !$0.trimmingCharacters(in: .whitespaces).isEmpty }
        }

        let primaryFile = mostRecentFile(in: root, gitStatusLines: dirtyLines)
        var parts = [projectName]
        if let branch { parts.append(branch) }
        if repoRoot != nil { parts.append(dirtyLines.isEmpty ? "clean working tree" : "\(dirtyLines.count) uncommitted") }
        let rawSummary = parts.joined(separator: " · ")

        do {
            let cleanProject = try FoxScrubber.scrub(projectName).scrubbed
            let cleanBranch = try branch.map { try FoxScrubber.scrub($0).scrubbed }
            let cleanFile = try primaryFile.map { try FoxScrubber.scrub($0).scrubbed }
            let cleanSummary = try FoxScrubber.scrub(rawSummary).scrubbed
            let safeName = projectName.lowercased().replacingOccurrences(of: "[^a-z0-9_-]", with: "_", options: .regularExpression)
            return .task(FoxTaskRecord(
                id: "tsk_\(safeName)",
                title: cleanFile ?? cleanProject,
                source: "git",
                status: "active",
                ts: nowMs,
                project: cleanProject,
                branch: cleanBranch,
                primaryFile: cleanFile,
                summary: cleanSummary,
                pinned: true,
                uncommittedCount: repoRoot == nil ? nil : dirtyLines.count
            ))
        } catch {
            return .state(key: "redaction", detail: "Scrubber failed, so nothing was sent")
        }
    }

    private func runGit(_ args: [String], cwd: String) -> String? {
        let proc = Process()
        proc.executableURL = URL(fileURLWithPath: "/usr/bin/git")
        proc.arguments = args
        proc.currentDirectoryURL = URL(fileURLWithPath: cwd)
        let pipe = Pipe()
        proc.standardOutput = pipe
        proc.standardError = FileHandle.nullDevice
        do {
            try proc.run()
            // Read before waiting: git blocks once the pipe buffer is full, so waiting first can hang forever.
            let data = pipe.fileHandleForReading.readDataToEndOfFile()
            proc.waitUntilExit()
            guard proc.terminationStatus == 0 else { return nil }
            return String(data: data, encoding: .utf8)?.trimmingCharacters(in: .whitespacesAndNewlines)
        } catch {
            return nil
        }
    }

    /// The file the person most likely touched last: the first changed *file* in git status
    /// (folders, which git lists for untracked directories, are skipped), else the newest file at the root.
    private func mostRecentFile(in dir: String, gitStatusLines: [String]) -> String? {
        for line in gitStatusLines where line.count > 3 {
            var rel = String(line.dropFirst(3)).trimmingCharacters(in: .whitespaces)
            if let arrow = rel.range(of: " -> ") { rel = String(rel[arrow.upperBound...]) }   // renames: keep the new name
            if rel.hasPrefix("\""), rel.hasSuffix("\""), rel.count >= 2 { rel = String(rel.dropFirst().dropLast()) }
            if rel.hasSuffix("/") { continue }
            var isDir: ObjCBool = false
            let full = (dir as NSString).appendingPathComponent(rel)
            if FileManager.default.fileExists(atPath: full, isDirectory: &isDir), isDir.boolValue { continue }
            return URL(fileURLWithPath: rel).lastPathComponent
        }
        let fm = FileManager.default
        guard let items = try? fm.contentsOfDirectory(atPath: dir) else { return nil }
        var newest: (name: String, date: Date)?
        for item in items where !item.hasPrefix(".") {
            let full = (dir as NSString).appendingPathComponent(item)
            var isDir: ObjCBool = false
            guard fm.fileExists(atPath: full, isDirectory: &isDir), !isDir.boolValue,
                  let mod = (try? fm.attributesOfItem(atPath: full))?[.modificationDate] as? Date else { continue }
            if newest == nil || mod > newest!.date { newest = (item, mod) }
        }
        return newest?.name
    }
}
