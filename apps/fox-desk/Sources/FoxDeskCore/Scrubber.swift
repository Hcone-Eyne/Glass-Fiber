import Foundation

public struct ScrubberError: Error, LocalizedError {
    public let reason: String
    public var errorDescription: String? { reason }
    public init(_ reason: String) { self.reason = reason }
}

public enum FoxScrubber {
    private static let hexChars = CharacterSet(charactersIn: "0123456789abcdefABCDEF")
    private static let base62Chars = CharacterSet(charactersIn: "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-_/+=")

    private static let prefixRegex: NSRegularExpression = {
        try! NSRegularExpression(
            pattern: "(?:sk-[A-Za-z0-9_-]{12,}|ghp_[A-Za-z0-9]{16,}|gho_[A-Za-z0-9]{16,}|xox[baprs]-[A-Za-z0-9-]{12,}|AIza[0-9A-Za-z_-]{20,}|AKIA[0-9A-Z]{12,}|eyJ[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{5,})",
            options: []
        )
    }()

    private static let kvSecretRegex: NSRegularExpression = {
        try! NSRegularExpression(
            pattern: "(?i)(api[_-]?key|secret|token|password|passwd|authorization|bearer)\\s*([:=])\\s*(['\"]?)([^\\s'\";,]{8,})\\3",
            options: []
        )
    }()

    private static let tokenScanRegex: NSRegularExpression = {
        try! NSRegularExpression(
            pattern: "[A-Za-z0-9_+=-]{20,}",
            options: []
        )
    }()

    @inlinable
    public static func shannonEntropy(_ token: String) -> Double {
        let utf8 = Array(token.utf8)
        let n = utf8.count
        guard n > 0 else { return 0.0 }
        var counts = [Int](repeating: 0, count: 256)
        for b in utf8 {
            counts[Int(b)] &+= 1
        }
        let invN = 1.0 / Double(n)
        var h = 0.0
        for c in counts where c > 0 {
            let p = Double(c) * invN
            h -= p * log2(p)
        }
        return h
    }

    public static func isHighEntropySecret(_ token: String) -> Bool {
        guard token.count >= 20 else { return false }
        // Exclude pure file paths or git commit hashes that are strictly 40 hex chars only when tagged,
        // but for raw strings in titles/summaries, 20+ hex chars with H >= 3.0 is treated as a secret/hash.
        let scalars = token.unicodeScalars
        let isHexOnly = scalars.allSatisfy { hexChars.contains($0) }
        let h = shannonEntropy(token)
        if isHexOnly {
            return h >= 3.0
        }
        let isBase62 = scalars.allSatisfy { base62Chars.contains($0) }
        if isBase62 {
            // Require at least one digit and one letter to avoid flagging long English camelCase identifiers
            let hasDigit = scalars.contains { CharacterSet.decimalDigits.contains($0) }
            let hasUpper = scalars.contains { CharacterSet.uppercaseLetters.contains($0) }
            let hasLower = scalars.contains { CharacterSet.lowercaseLetters.contains($0) }
            if hasDigit && (hasUpper || hasLower) && h >= 3.75 {
                return true
            }
        }
        return false
    }

    public static func scrub(_ input: String) throws -> (scrubbed: String, redactedCount: Int) {
        if input.utf8.contains(0) {
            throw ScrubberError("Null byte in payload blocked by fail-closed scrubber")
        }

        var working = input
        var redactions = 0

        // 1. Known token prefixes (sk-, ghp_, AIza, AKIA, JWT, etc.)
        let range1 = NSRange(working.startIndex..<working.endIndex, in: working)
        let matches1 = prefixRegex.matches(in: working, options: [], range: range1)
        if !matches1.isEmpty {
            redactions += matches1.count
            working = prefixRegex.stringByReplacingMatches(in: working, options: [], range: range1, withTemplate: "[REDACTED]")
        }

        // 2. Key-value secret assignments
        let range2 = NSRange(working.startIndex..<working.endIndex, in: working)
        let matches2 = kvSecretRegex.matches(in: working, options: [], range: range2)
        if !matches2.isEmpty {
            redactions += matches2.count
            working = kvSecretRegex.stringByReplacingMatches(in: working, options: [], range: range2, withTemplate: "$1$2$3[REDACTED]$3")
        }

        // 3. Charset-aware Shannon entropy scan (Hex >= 3.0 bits/char, Base62 >= 3.75 bits/char)
        let range3 = NSRange(working.startIndex..<working.endIndex, in: working)
        let matches3 = tokenScanRegex.matches(in: working, options: [], range: range3)
        if !matches3.isEmpty {
            var result = working
            for match in matches3.reversed() {
                guard let swiftRange = Range(match.range, in: result) else { continue }
                let candidate = String(result[swiftRange])
                if candidate == "REDACTED" { continue }
                if isHighEntropySecret(candidate) {
                    result.replaceSubrange(swiftRange, with: "[REDACTED]")
                    redactions += 1
                }
            }
            working = result
        }

        return (working, redactions)
    }
}
