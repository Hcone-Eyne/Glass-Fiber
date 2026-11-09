import Foundation
import SQLite3

public struct FoxTaskRecord: Equatable, Codable {
    public var id: String
    public var title: String
    public var source: String
    public var status: String
    public var ts: Int64
    public var project: String?
    public var branch: String?
    public var primaryFile: String?
    public var summary: String?
    public var pinned: Bool
    public var uncommittedCount: Int?

    public init(
        id: String,
        title: String,
        source: String,
        status: String,
        ts: Int64,
        project: String? = nil,
        branch: String? = nil,
        primaryFile: String? = nil,
        summary: String? = nil,
        pinned: Bool = false,
        uncommittedCount: Int? = nil
    ) {
        self.id = id
        self.title = title
        self.source = source
        self.status = status
        self.ts = ts
        self.project = project
        self.branch = branch
        self.primaryFile = primaryFile
        self.summary = summary
        self.pinned = pinned
        self.uncommittedCount = uncommittedCount
    }

    public func toEnvelopePayload() -> [String: Any] {
        var taskId = id
        if !taskId.hasPrefix("tsk_") {
            if taskId.hasPrefix("task_") {
                taskId = "tsk_" + String(taskId.dropFirst(5))
            } else {
                taskId = "tsk_" + taskId
            }
        }
        // Match ^tsk_[A-Za-z0-9_-]{1,64}$
        let suffix = String(taskId.dropFirst(4)).replacingOccurrences(of: "[^A-Za-z0-9_-]", with: "_", options: .regularExpression)
        taskId = "tsk_" + String(suffix.prefix(60))

        let p = (project?.isEmpty == false ? project! : "FoxDesk").prefix(128).description
        let s = (summary?.isEmpty == false ? summary! : title).prefix(280).description

        let dict: [String: Any] = [
            "task_id": taskId,
            "project": p,
            "branch": branch.map { String($0.prefix(128)) } ?? NSNull(),
            "active_file": primaryFile.map { String($0.prefix(256)) } ?? NSNull(),
            "summary": s,
            "updated_at": ts,
            "uncommitted_count": uncommittedCount != nil ? NSNumber(value: max(0, uncommittedCount!)) : NSNull()
        ]
        return dict
    }
}

public final class FoxStore {
    private var db: OpaquePointer?
    public let path: String

    public init(path: String) throws {
        self.path = path
        var handle: OpaquePointer?
        let flags = SQLITE_OPEN_CREATE | SQLITE_OPEN_READWRITE | SQLITE_OPEN_FULLMUTEX
        if sqlite3_open_v2(path, &handle, flags, nil) != SQLITE_OK {
            let msg = handle.flatMap { String(cString: sqlite3_errmsg($0)) } ?? "sqlite3_open_v2 failed"
            throw NSError(domain: "FoxStore", code: 1, userInfo: [NSLocalizedDescriptionKey: msg])
        }
        self.db = handle
        try migrate()
    }

    deinit {
        if let db = db {
            sqlite3_close(db)
        }
    }

    private func exec(_ sql: String) throws {
        var errMsg: UnsafeMutablePointer<CChar>?
        if sqlite3_exec(db, sql, nil, nil, &errMsg) != SQLITE_OK {
            let str = errMsg.map { String(cString: $0) } ?? "SQLite error"
            sqlite3_free(errMsg)
            throw NSError(domain: "FoxStore", code: 2, userInfo: [NSLocalizedDescriptionKey: str])
        }
    }

    private func migrate() throws {
        try exec("PRAGMA journal_mode=WAL;")
        try exec("PRAGMA synchronous=NORMAL;")
        try exec("""
        CREATE TABLE IF NOT EXISTS meta (
            key TEXT PRIMARY KEY,
            value TEXT NOT NULL
        );
        CREATE TABLE IF NOT EXISTS tasks (
            id TEXT PRIMARY KEY,
            title TEXT NOT NULL,
            source TEXT NOT NULL,
            status TEXT NOT NULL,
            ts INTEGER NOT NULL,
            project TEXT,
            branch TEXT,
            primary_file TEXT,
            summary TEXT,
            pinned INTEGER NOT NULL DEFAULT 0,
            uncommitted_count INTEGER
        );
        CREATE TABLE IF NOT EXISTS failure_log (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            ts INTEGER NOT NULL,
            state_key TEXT NOT NULL,
            group_name TEXT NOT NULL,
            severity TEXT NOT NULL,
            detail TEXT NOT NULL
        );
        PRAGMA user_version = 1;
        """)
    }

    public func journalMode() throws -> String {
        var stmt: OpaquePointer?
        guard sqlite3_prepare_v2(db, "PRAGMA journal_mode;", -1, &stmt, nil) == SQLITE_OK else {
            return "unknown"
        }
        defer { sqlite3_finalize(stmt) }
        if sqlite3_step(stmt) == SQLITE_ROW {
            return String(cString: sqlite3_column_text(stmt, 0))
        }
        return "unknown"
    }

    public func upsertTask(_ task: FoxTaskRecord) throws {
        let sql = """
        INSERT INTO tasks (id, title, source, status, ts, project, branch, primary_file, summary, pinned, uncommitted_count)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT(id) DO UPDATE SET
            title = excluded.title,
            source = excluded.source,
            status = excluded.status,
            ts = excluded.ts,
            project = excluded.project,
            branch = excluded.branch,
            primary_file = excluded.primary_file,
            summary = excluded.summary,
            pinned = excluded.pinned,
            uncommitted_count = excluded.uncommitted_count;
        """
        var stmt: OpaquePointer?
        guard sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK else {
            throw NSError(domain: "FoxStore", code: 3, userInfo: [NSLocalizedDescriptionKey: "prepare upsertTask failed"])
        }
        defer { sqlite3_finalize(stmt) }

        let transient = unsafeBitCast(-1, to: sqlite3_destructor_type.self)
        sqlite3_bind_text(stmt, 1, task.id, -1, transient)
        sqlite3_bind_text(stmt, 2, task.title, -1, transient)
        sqlite3_bind_text(stmt, 3, task.source, -1, transient)
        sqlite3_bind_text(stmt, 4, task.status, -1, transient)
        sqlite3_bind_int64(stmt, 5, task.ts)
        if let v = task.project { sqlite3_bind_text(stmt, 6, v, -1, transient) } else { sqlite3_bind_null(stmt, 6) }
        if let v = task.branch { sqlite3_bind_text(stmt, 7, v, -1, transient) } else { sqlite3_bind_null(stmt, 7) }
        if let v = task.primaryFile { sqlite3_bind_text(stmt, 8, v, -1, transient) } else { sqlite3_bind_null(stmt, 8) }
        if let v = task.summary { sqlite3_bind_text(stmt, 9, v, -1, transient) } else { sqlite3_bind_null(stmt, 9) }
        sqlite3_bind_int(stmt, 10, task.pinned ? 1 : 0)
        if let v = task.uncommittedCount { sqlite3_bind_int(stmt, 11, Int32(v)) } else { sqlite3_bind_null(stmt, 11) }

        guard sqlite3_step(stmt) == SQLITE_DONE else {
            throw NSError(domain: "FoxStore", code: 4, userInfo: [NSLocalizedDescriptionKey: "step upsertTask failed"])
        }
    }

    public func listTasks(limit: Int = 5) throws -> [FoxTaskRecord] {
        let sql = """
        SELECT id, title, source, status, ts, project, branch, primary_file, summary, pinned, uncommitted_count
        FROM tasks
        ORDER BY pinned DESC, ts DESC
        LIMIT ?;
        """
        var stmt: OpaquePointer?
        guard sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK else {
            throw NSError(domain: "FoxStore", code: 5, userInfo: [NSLocalizedDescriptionKey: "prepare listTasks failed"])
        }
        defer { sqlite3_finalize(stmt) }
        sqlite3_bind_int(stmt, 1, Int32(limit))

        var out: [FoxTaskRecord] = []
        while sqlite3_step(stmt) == SQLITE_ROW {
            let id = String(cString: sqlite3_column_text(stmt, 0))
            let title = String(cString: sqlite3_column_text(stmt, 1))
            let source = String(cString: sqlite3_column_text(stmt, 2))
            let status = String(cString: sqlite3_column_text(stmt, 3))
            let ts = sqlite3_column_int64(stmt, 4)
            let project = sqlite3_column_type(stmt, 5) == SQLITE_NULL ? nil : String(cString: sqlite3_column_text(stmt, 5))
            let branch = sqlite3_column_type(stmt, 6) == SQLITE_NULL ? nil : String(cString: sqlite3_column_text(stmt, 6))
            let primaryFile = sqlite3_column_type(stmt, 7) == SQLITE_NULL ? nil : String(cString: sqlite3_column_text(stmt, 7))
            let summary = sqlite3_column_type(stmt, 8) == SQLITE_NULL ? nil : String(cString: sqlite3_column_text(stmt, 8))
            let pinned = sqlite3_column_int(stmt, 9) != 0
            let uncommitted = sqlite3_column_type(stmt, 10) == SQLITE_NULL ? nil : Int(sqlite3_column_int(stmt, 10))
            out.append(FoxTaskRecord(
                id: id,
                title: title,
                source: source,
                status: status,
                ts: ts,
                project: project,
                branch: branch,
                primaryFile: primaryFile,
                summary: summary,
                pinned: pinned,
                uncommittedCount: uncommitted
            ))
        }
        return out
    }

    public func logFailure(stateKey: String, groupName: String, severity: String, detail: String, ts: Int64) throws {
        let sql = "INSERT INTO failure_log (ts, state_key, group_name, severity, detail) VALUES (?, ?, ?, ?, ?);"
        var stmt: OpaquePointer?
        guard sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK else {
            throw NSError(domain: "FoxStore", code: 6, userInfo: [NSLocalizedDescriptionKey: "prepare logFailure failed"])
        }
        defer { sqlite3_finalize(stmt) }
        // Logs never hold secrets: the detail goes through the scrubber, and if that fails nothing of it is kept.
        let safeDetail = (try? FoxScrubber.scrub(detail).scrubbed) ?? "[detail withheld: scrubber failed]"
        let transient = unsafeBitCast(-1, to: sqlite3_destructor_type.self)
        sqlite3_bind_int64(stmt, 1, ts)
        sqlite3_bind_text(stmt, 2, stateKey, -1, transient)
        sqlite3_bind_text(stmt, 3, groupName, -1, transient)
        sqlite3_bind_text(stmt, 4, severity, -1, transient)
        sqlite3_bind_text(stmt, 5, safeDetail, -1, transient)
        guard sqlite3_step(stmt) == SQLITE_DONE else {
            throw NSError(domain: "FoxStore", code: 7, userInfo: [NSLocalizedDescriptionKey: "step logFailure failed"])
        }

        // Enforce 500-row cap on failure_log
        try exec("""
        DELETE FROM failure_log WHERE id NOT IN (
            SELECT id FROM failure_log ORDER BY id DESC LIMIT 500
        );
        """)
    }

    public func failureLogCount() throws -> Int {
        var stmt: OpaquePointer?
        guard sqlite3_prepare_v2(db, "SELECT COUNT(*) FROM failure_log;", -1, &stmt, nil) == SQLITE_OK else { return 0 }
        defer { sqlite3_finalize(stmt) }
        if sqlite3_step(stmt) == SQLITE_ROW {
            return Int(sqlite3_column_int(stmt, 0))
        }
        return 0
    }
}
