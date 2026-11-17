package io.github.trysac.android_a11y_checks

/**
 * Single accessibility issue reported on a [SemanticsNode].
 *
 * @property rule Rule identifier (for example `touch_target` or `contrast`).
 * @property wcag WCAG 2.2 success criterion number (for example `1.4.3`).
 * @property path Node path in the semantics tree.
 * @property message Formatted diagnostic message (`<rule> at <path>: <detail>`, max 200 characters).
 */
public data class Issue(
    public val rule: String,
    public val wcag: String,
    public val path: String,
    public val message: String,
)

/**
 * Immutable result of running [A11yChecker.check] over a semantics snapshot.
 *
 * @property issues Detected accessibility issues, sorted by [Issue.rule] then [Issue.path].
 */
public data class A11yReport(
    public val issues: List<Issue>,
) {
    /**
     * Serializes the report as a deterministic JSON string with issues sorted by `rule` then `path`.
     */
    public fun toJson(): String {
        val sorted = issues.sortedWith(compareBy<Issue> { it.rule }.thenBy { it.path }.thenBy { it.message })
        if (sorted.isEmpty()) {
            return "{\"issues\":[]}"
        }
        val sb = StringBuilder("{\"issues\":[")
        sorted.forEachIndexed { index, issue ->
            if (index > 0) {
                sb.append(',')
            }
            sb.append("{\"rule\":\"")
            sb.append(escapeJson(issue.rule))
            sb.append("\",\"wcag\":\"")
            sb.append(escapeJson(issue.wcag))
            sb.append("\",\"path\":\"")
            sb.append(escapeJson(issue.path))
            sb.append("\",\"message\":\"")
            sb.append(escapeJson(issue.message))
            sb.append("\"}")
        }
        sb.append("]}")
        return sb.toString()
    }

    private companion object {
        private const val CONTROL_CHAR_LIMIT = 0x20
        private const val HEX_RADIX = 16
        private const val HEX_PAD_LEN = 4

        private fun escapeJson(value: String): String {
            val out = StringBuilder(value.length)
            for (ch in value) {
                when (ch) {
                    '"' -> {
                        out.append("\\\"")
                    }

                    '\\' -> {
                        out.append("\\\\")
                    }

                    '\b' -> {
                        out.append("\\b")
                    }

                    '\u000C' -> {
                        out.append("\\f")
                    }

                    '\n' -> {
                        out.append("\\n")
                    }

                    '\r' -> {
                        out.append("\\r")
                    }

                    '\t' -> {
                        out.append("\\t")
                    }

                    else -> {
                        if (ch.code < CONTROL_CHAR_LIMIT) {
                            val hex = ch.code.toString(HEX_RADIX).padStart(HEX_PAD_LEN, '0')
                            out.append("\\u").append(hex)
                        } else {
                            out.append(ch)
                        }
                    }
                }
            }
            return out.toString()
        }
    }
}
