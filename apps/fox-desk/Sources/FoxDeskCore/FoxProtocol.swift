import Foundation

/// FoxLink v1 native Swift protocol validator, envelope builder, and replay guard.
/// Compiled with `-O` to native ARM64 machine code. Matches Kotlin `FoxSchemaValidator`
/// line-for-line on `packages/fox-protocol` golden fixtures.
public struct ProtocolError: Error, Equatable, CustomStringConvertible {
    public let path: String
    public let reason: String

    public init(path: String, reason: String) {
        self.path = path
        self.reason = reason
    }

    public var description: String {
        "\(path.isEmpty ? "<root>" : path): \(reason)"
    }
}

public typealias FoxValidationError = ProtocolError

public struct FoxStateRecord: Equatable, Codable {
    public let id: String
    public let group: String
    public let level: String
    public let title: String
    public let detail: String?
    public let action: String?
    public let alt: String?

    public init(
        id: String,
        group: String,
        level: String,
        title: String,
        detail: String? = nil,
        action: String? = nil,
        alt: String? = nil
    ) {
        self.id = id
        self.group = group
        self.level = level
        self.title = title
        self.detail = detail
        self.action = action
        self.alt = alt
    }
}

public struct FoxStateCatalogMap: Collection {
    public typealias Index = Dictionary<String, FoxStateRecord>.Index
    public typealias Element = (key: String, value: FoxStateRecord)

    public let storage: [String: FoxStateRecord]

    public init(states: [FoxStateRecord]) {
        var dict: [String: FoxStateRecord] = [:]
        for s in states {
            dict[s.id] = s
        }
        self.storage = dict
    }

    public var startIndex: Index { storage.startIndex }
    public var endIndex: Index { storage.endIndex }
    public func index(after i: Index) -> Index { storage.index(after: i) }
    public subscript(position: Index) -> Element { storage[position] }
    public var count: Int { storage.count }

    public subscript(key: String) -> FoxStateRecord? { storage[key] }

    public var keys: Dictionary<String, FoxStateRecord>.Keys { storage.keys }
    public var values: Dictionary<String, FoxStateRecord>.Values { storage.values }
}

public enum FoxSchemaValidator {
    private static let allowedKeywords: Set<String> = [
        "$schema", "$id", "title", "description",
        "type", "required", "properties", "additionalProperties", "enum", "const",
        "minimum", "maxLength", "minLength", "pattern", "items", "maxItems", "$ref"
    ]

    private static func join(_ path: String, _ key: String) -> String {
        path.isEmpty ? key : "\(path).\(key)"
    }

    private static func isNull(_ v: Any) -> Bool {
        v is NSNull
    }

    private static func isBool(_ v: Any) -> Bool {
        guard let num = v as? NSNumber else { return false }
        return CFGetTypeID(num) == CFBooleanGetTypeID()
    }

    private static func isInt(_ v: Any) -> Bool {
        guard let num = v as? NSNumber, !isBool(v) else { return false }
        let d = num.doubleValue
        return floor(d) == d
    }

    private static func isNumber(_ v: Any) -> Bool {
        (v as? NSNumber) != nil && !isBool(v)
    }

    private static func typeOk(_ value: Any, _ t: String) throws -> Bool {
        switch t {
        case "null": return isNull(value)
        case "boolean": return isBool(value)
        case "integer": return isInt(value)
        case "number": return isNumber(value)
        case "string": return value is String
        case "array": return value is [Any]
        case "object": return value is [String: Any]
        default:
            throw ProtocolError(path: "", reason: "unknown schema type '\(t)'")
        }
    }

    public static func validate(
        value: Any,
        schema: [String: Any],
        resolveRef: (String) throws -> [String: Any],
        path: String = ""
    ) throws {
        let unknown = Set(schema.keys).subtracting(allowedKeywords)
        if !unknown.isEmpty {
            throw ProtocolError(path: path, reason: "schema uses unsupported keywords \(unknown.sorted())")
        }
        if let ref = schema["$ref"] as? String {
            if ref.contains("/") || ref.contains("\\") || ref.hasPrefix(".") {
                throw ProtocolError(path: "", reason: "illegal $ref '\(ref)'")
            }
            let target = try resolveRef(ref)
            try validate(value: value, schema: target, resolveRef: resolveRef, path: path)
            return
        }
        if let typeSpec = schema["type"] {
            let types: [String]
            if let s = typeSpec as? String {
                types = [s]
            } else if let arr = typeSpec as? [String] {
                types = arr
            } else {
                throw ProtocolError(path: path, reason: "invalid type spec")
            }
            var matched = false
            for t in types {
                if try typeOk(value, t) {
                    matched = true
                    break
                }
            }
            if !matched {
                throw ProtocolError(path: path, reason: "expected \(types.joined(separator: "/"))")
            }
        }
        if let constVal = schema["const"] {
            if !valuesEqual(value, constVal) {
                throw ProtocolError(path: path, reason: "must be \(constVal)")
            }
        }
        if let enumVals = schema["enum"] as? [Any] {
            if !enumVals.contains(where: { valuesEqual(value, $0) }) {
                throw ProtocolError(path: path, reason: "not an allowed value")
            }
        }
        if isNull(value) { return }

        if isNumber(value), let num = value as? NSNumber {
            if let min = (schema["minimum"] as? NSNumber)?.doubleValue {
                if num.doubleValue < min {
                    throw ProtocolError(path: path, reason: "below minimum \(min)")
                }
            }
        }
        if let str = value as? String {
            let count = str.count
            if let minLen = (schema["minLength"] as? NSNumber)?.intValue, count < minLen {
                throw ProtocolError(path: path, reason: "too short")
            }
            if let maxLen = (schema["maxLength"] as? NSNumber)?.intValue, count > maxLen {
                throw ProtocolError(path: path, reason: "too long")
            }
            if let pattern = schema["pattern"] as? String {
                var p = pattern
                if p.hasPrefix("^") { p.removeFirst() }
                if p.hasSuffix("$") { p.removeLast() }
                let anchored = "\\A(?:\(p))\\z"
                let regex = try NSRegularExpression(pattern: anchored)
                let range = NSRange(str.startIndex..<str.endIndex, in: str)
                if regex.firstMatch(in: str, range: range) == nil {
                    throw ProtocolError(path: path, reason: "does not match pattern")
                }
            }
        }
        if let arr = value as? [Any] {
            if let maxItems = (schema["maxItems"] as? NSNumber)?.intValue, arr.count > maxItems {
                throw ProtocolError(path: path, reason: "too many items")
            }
            if let itemSchema = schema["items"] as? [String: Any] {
                for (i, item) in arr.enumerated() {
                    try validate(value: item, schema: itemSchema, resolveRef: resolveRef, path: "\(path)[\(i)]")
                }
            }
        }
        if let obj = value as? [String: Any] {
            let props = (schema["properties"] as? [String: [String: Any]]) ?? [:]
            if let req = schema["required"] as? [String] {
                for k in req where obj[k] == nil {
                    throw ProtocolError(path: join(path, k), reason: "required")
                }
            }
            if let addl = schema["additionalProperties"] as? Bool, addl == false {
                for k in obj.keys.sorted() where props[k] == nil {
                    throw ProtocolError(path: join(path, k), reason: "not allowed")
                }
            }
            for k in props.keys.sorted() {
                if let v = obj[k], let sub = props[k] {
                    try validate(value: v, schema: sub, resolveRef: resolveRef, path: join(path, k))
                }
            }
        }
    }

    private static func valuesEqual(_ a: Any, _ b: Any) -> Bool {
        if isNull(a) && isNull(b) { return true }
        if isBool(a) && isBool(b) {
            return (a as? Bool) == (b as? Bool)
        }
        if isNumber(a) && isNumber(b), let na = a as? NSNumber, let nb = b as? NSNumber {
            return na.doubleValue == nb.doubleValue
        }
        if let sa = a as? String, let sb = b as? String {
            return sa == sb
        }
        return false
    }

    public static func validateEnvelope(_ msg: Any, catalog: FoxProtocolCatalog) -> Result<[String: Any], ProtocolError> {
        do {
            let dict = try catalog.validateMessage(msg)
            return .success(dict)
        } catch let err as ProtocolError {
            return .failure(err)
        } catch {
            return .failure(ProtocolError(path: "", reason: error.localizedDescription))
        }
    }
}

public final class FoxProtocolCatalog {
    public let protocolDir: URL
    public let envelopeSchema: [String: Any]
    public let statesByKey: FoxStateCatalogMap
    private var payloadCache: [String: [String: Any]] = [:]

    public init(protocolRoot: URL) throws {
        self.protocolDir = protocolRoot
        let envURL = protocolRoot.appendingPathComponent("schema/envelope.schema.json")
        let data = try Data(contentsOf: envURL)
        guard let obj = try JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            throw ProtocolError(path: "", reason: "invalid envelope schema")
        }
        self.envelopeSchema = obj

        let statesURL = protocolRoot.appendingPathComponent("schema/states.json")
        let statesData = try Data(contentsOf: statesURL)
        guard let statesObj = try JSONSerialization.jsonObject(with: statesData) as? [String: Any],
              let statesList = statesObj["states"] as? [[String: Any]] else {
            throw ProtocolError(path: "", reason: "invalid states.json")
        }
        var records: [FoxStateRecord] = []
        for s in statesList {
            guard let sid = s["id"] as? String else { continue }
            records.append(FoxStateRecord(
                id: sid,
                group: (s["group"] as? String) ?? "",
                level: (s["level"] as? String) ?? "",
                title: (s["title"] as? String) ?? "",
                detail: s["detail"] as? String,
                action: s["action"] as? String,
                alt: s["alt"] as? String
            ))
        }
        self.statesByKey = FoxStateCatalogMap(states: records)
    }

    public convenience init(protocolDir: URL) throws {
        try self.init(protocolRoot: protocolDir)
    }

    public func payloadSchema(named name: String) throws -> [String: Any] {
        let filename = name.hasSuffix(".json") ? name : "\(name).json"
        if let cached = payloadCache[filename] { return cached }
        let url = protocolDir.appendingPathComponent("schema/payloads/\(filename)")
        let data = try Data(contentsOf: url)
        guard let obj = try JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            throw ProtocolError(path: "", reason: "invalid payload schema \(filename)")
        }
        payloadCache[filename] = obj
        return obj
    }

    public func validateMessage(_ msg: Any) throws -> [String: Any] {
        guard let dict = msg as? [String: Any] else {
            throw ProtocolError(path: "", reason: "expected envelope object")
        }
        try FoxSchemaValidator.validate(
            value: dict,
            schema: envelopeSchema,
            resolveRef: { try self.payloadSchema(named: $0) },
            path: ""
        )
        guard let type = dict["type"] as? String,
              let payload = dict["payload"] else {
            throw ProtocolError(path: "", reason: "expected envelope object")
        }
        let pSchema = try payloadSchema(named: type)
        try FoxSchemaValidator.validate(
            value: payload,
            schema: pSchema,
            resolveRef: { try self.payloadSchema(named: $0) },
            path: "payload"
        )
        if type == "state.event", let p = payload as? [String: Any], let sid = p["state_id"] as? String, statesByKey[sid] == nil {
            throw ProtocolError(path: "payload.state_id", reason: "unknown state '\(sid)'")
        }
        return dict
    }

    public func parse(_ data: Data) throws -> [String: Any] {
        let obj: Any
        do {
            obj = try JSONSerialization.jsonObject(with: data, options: [.fragmentsAllowed])
        } catch {
            throw ProtocolError(path: "", reason: "not JSON: \(error.localizedDescription)")
        }
        return try validateMessage(obj)
    }
}

public final class ReplayGuard {
    public private(set) var epoch: Int64 = -1
    public private(set) var lastSeq: Int64 = -1
    public let maxSkewMs: Int64

    public init(maxSkewMs: Int64 = 60_000) {
        self.maxSkewMs = maxSkewMs
    }

    /// Accepts a message only if its clock is within `maxSkewMs` and it is newer than everything seen:
    /// a higher epoch starts a new session (the other side restarted), the same epoch needs a higher seq,
    /// and a lower epoch is a stale session, so it is dropped.
    public func check(epoch: Int64, seq: Int64, ts: Int64, nowMs: Int64) -> Result<Void, ProtocolError> {
        if abs(nowMs - ts) > maxSkewMs {
            return .failure(ProtocolError(path: "ts", reason: "stale timestamp (> \(maxSkewMs / 1000)s skew)"))
        }
        if epoch < self.epoch {
            return .failure(ProtocolError(path: "epoch", reason: "stale epoch \(epoch) < \(self.epoch)"))
        }
        if epoch > self.epoch {
            self.epoch = epoch
            self.lastSeq = seq
            return .success(())
        }
        if seq <= lastSeq {
            return .failure(ProtocolError(path: "seq", reason: "duplicate or out-of-order sequence \(seq) <= \(lastSeq)"))
        }
        lastSeq = seq
        return .success(())
    }
}
