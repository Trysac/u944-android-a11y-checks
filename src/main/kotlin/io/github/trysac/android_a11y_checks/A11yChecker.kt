package io.github.trysac.android_a11y_checks

import java.util.Locale
import kotlin.math.floor

/**
 * Rule engine that evaluates an Android View or Compose [SemanticsNode] tree for common
 * accessibility defects and reports WCAG 2.2 references.
 *
 * @property density Display density factor (`px / dp`). Must be strictly positive and finite.
 * @property rules Active rule identifiers to check. Defaults to [ALL_RULES].
 * @property maxNodes Maximum number of nodes allowed in a single tree before throwing [A11yException].
 * @property maxDepth Maximum tree depth allowed (root has depth 1) before throwing [A11yException].
 */
public class A11yChecker(
    public val density: Float,
    public val rules: Set<String> = ALL_RULES,
    public val maxNodes: Int = DEFAULT_MAX_NODES,
    public val maxDepth: Int = DEFAULT_MAX_DEPTH,
) {
    init {
        if (!density.isFinite() || density <= 0f) {
            throw A11yException("invalid_density", "density must be finite and positive")
        }
        if (maxNodes < 1) {
            throw A11yException("invalid_max_nodes", "maxNodes must be at least 1")
        }
        if (maxDepth < 1) {
            throw A11yException("invalid_max_depth", "maxDepth must be at least 1")
        }
        val unknown = rules - ALL_RULES
        if (unknown.isNotEmpty()) {
            throw A11yException("unknown_rule", "unknown rule: ${unknown.first().take(MAX_SNIPPET_LENGTH)}")
        }
    }

    /**
     * Evaluates the semantics tree rooted at [root] and returns an [A11yReport] whose issues are
     * sorted by `rule` then `path`.
     *
     * @throws A11yException with code `tree_too_large` if the tree exceeds [maxNodes], or
     *   `too_deep` if the hierarchy depth exceeds [maxDepth].
     */
    public fun check(root: SemanticsNode): A11yReport {
        validateBounds(root)
        val collected = mutableListOf<Issue>()
        if (isInspectable(root)) {
            evaluateSubtree(root, siblings = listOf(root), collected = collected)
        }
        val sorted =
            collected.sortedWith(
                compareBy<Issue> { it.rule }
                    .thenBy { it.path }
                    .thenBy { it.message },
            )
        return A11yReport(sorted)
    }

    private fun validateBounds(root: SemanticsNode) {
        val stack = ArrayDeque<Pair<SemanticsNode, Int>>()
        stack.addLast(root to 1)
        var count = 0
        while (stack.isNotEmpty()) {
            val (node, depth) = stack.removeLast()
            if (depth > maxDepth) {
                throw A11yException(
                    "too_deep",
                    "semantics tree depth $depth exceeds maxDepth $maxDepth",
                )
            }
            count++
            if (count > maxNodes) {
                throw A11yException(
                    "tree_too_large",
                    "semantics tree exceeds maxNodes $maxNodes",
                )
            }
            for (i in node.children.indices.reversed()) {
                stack.addLast(node.children[i] to (depth + 1))
            }
        }
    }

    private fun evaluateSubtree(
        node: SemanticsNode,
        siblings: List<SemanticsNode>,
        collected: MutableList<Issue>,
    ) {
        if (RULE_TOUCH_TARGET in rules) {
            checkTouchTarget(node, collected)
        }
        if (RULE_CONTRAST in rules) {
            checkContrast(node, collected)
        }
        checkMissingAndImageLabels(node, collected)
        checkEditableAndRedundantRole(node, siblings, collected)

        val inspectableChildren = node.children.filter { isInspectable(it) }
        checkDuplicateLabels(inspectableChildren, collected)
        for (child in inspectableChildren) {
            evaluateSubtree(child, inspectableChildren, collected)
        }
    }

    private fun checkDuplicateLabels(
        siblings: List<SemanticsNode>,
        collected: MutableList<Issue>,
    ) {
        if (RULE_DUPLICATE_LABEL !in rules) {
            return
        }
        val clickableSiblings = siblings.filter { it.clickable }
        val seen = mutableMapOf<String, SemanticsNode>()
        for (sibling in clickableSiblings) {
            val label = effectiveLabel(sibling)?.trim()?.lowercase(Locale.ROOT)
            if (!label.isNullOrEmpty()) {
                val previous = seen[label]
                if (previous != null) {
                    val snippet = truncateSnippet(label)
                    collected.add(
                        buildIssue(
                            rule = RULE_DUPLICATE_LABEL,
                            wcag = WCAG_HEADINGS_AND_LABELS,
                            path = sibling.path,
                            detail = "clickable sibling shares duplicate label '$snippet' with ${previous.path}",
                        ),
                    )
                } else {
                    seen[label] = sibling
                }
            }
        }
    }

    private fun checkTouchTarget(
        node: SemanticsNode,
        collected: MutableList<Issue>,
    ) {
        if (!node.clickable && !node.focusable) {
            return
        }
        val widthDp = pxToDp(node.boundsPx.width)
        val heightDp = pxToDp(node.boundsPx.height)
        if (widthDp < MIN_TOUCH_TARGET_DP || heightDp < MIN_TOUCH_TARGET_DP) {
            collected.add(
                buildIssue(
                    rule = RULE_TOUCH_TARGET,
                    wcag = WCAG_TOUCH_TARGET,
                    path = node.path,
                    detail =
                        "size ${widthDp}x${heightDp}dp is below minimum " +
                            "${MIN_TOUCH_TARGET_DP}x${MIN_TOUCH_TARGET_DP}dp",
                ),
            )
        }
    }

    private fun checkContrast(
        node: SemanticsNode,
        collected: MutableList<Issue>,
    ) {
        val fg = node.textColor
        val bg = node.backgroundColor
        val hasText = !node.text.isNullOrBlank() || node.role == Role.TEXT || node.role == Role.TEXT_FIELD
        if (fg != null && bg != null && hasText) {
            val ratio = Contrast.ratio(fg, bg)
            val isLargeText =
                (node.textSizeSp != null && node.textSizeSp >= LARGE_TEXT_SP) ||
                    (node.bold && node.textSizeSp != null && node.textSizeSp >= LARGE_BOLD_TEXT_SP)
            val threshold = if (isLargeText) MIN_CONTRAST_LARGE else MIN_CONTRAST_NORMAL
            if (ratio < threshold) {
                val formattedRatio = String.format(Locale.ROOT, "%.2f", ratio)
                val formattedThreshold = String.format(Locale.ROOT, "%.1f", threshold)
                collected.add(
                    buildIssue(
                        rule = RULE_CONTRAST,
                        wcag = WCAG_CONTRAST,
                        path = node.path,
                        detail = "contrast ratio $formattedRatio:1 is below minimum $formattedThreshold:1",
                    ),
                )
            }
        }
    }

    private fun checkMissingAndImageLabels(
        node: SemanticsNode,
        collected: MutableList<Issue>,
    ) {
        val label = effectiveLabel(node)
        if (RULE_MISSING_LABEL in rules && node.clickable && label == null) {
            collected.add(
                buildIssue(
                    rule = RULE_MISSING_LABEL,
                    wcag = WCAG_NAME_ROLE_VALUE,
                    path = node.path,
                    detail = "clickable node has no text or contentDescription",
                ),
            )
        }
        if (RULE_IMAGE_NO_LABEL in rules && node.role == Role.IMAGE && node.contentDescription.isNullOrBlank()) {
            collected.add(
                buildIssue(
                    rule = RULE_IMAGE_NO_LABEL,
                    wcag = WCAG_NON_TEXT_CONTENT,
                    path = node.path,
                    detail = "important image has no contentDescription",
                ),
            )
        }
    }

    private fun checkEditableAndRedundantRole(
        node: SemanticsNode,
        siblings: List<SemanticsNode>,
        collected: MutableList<Issue>,
    ) {
        if (RULE_EDITABLE_NO_LABEL in rules && (node.role == Role.TEXT_FIELD || node.editable)) {
            val hasOwn = !node.hint.isNullOrBlank() || !node.contentDescription.isNullOrBlank()
            val hasSibling =
                siblings.any {
                    it !== node && it.role == Role.TEXT && !it.clickable && !it.editable && !it.text.isNullOrBlank()
                }
            if (!hasOwn && !hasSibling) {
                collected.add(
                    buildIssue(
                        rule = RULE_EDITABLE_NO_LABEL,
                        wcag = WCAG_INFO_AND_RELATIONSHIPS,
                        path = node.path,
                        detail = "editable text field has no hint, contentDescription or sibling label",
                    ),
                )
            }
        }
        val label = effectiveLabel(node)
        if (RULE_REDUNDANT_ROLE in rules && label != null) {
            val roleWords = ROLE_WORDS[node.role].orEmpty()
            val words = WORD_SPLIT_REGEX.split(label.lowercase(Locale.ROOT)).filter { it.isNotEmpty() }
            val matchedWord = roleWords.firstOrNull { roleWord -> words.contains(roleWord) }
            if (matchedWord != null) {
                val snippet = truncateSnippet(label.trim())
                collected.add(
                    buildIssue(
                        rule = RULE_REDUNDANT_ROLE,
                        wcag = WCAG_NAME_ROLE_VALUE,
                        path = node.path,
                        detail = "label '$snippet' redundantly includes role word '$matchedWord'",
                    ),
                )
            }
        }
    }

    private fun pxToDp(px: Int): Int = floor(px.toDouble() / density.toDouble()).toInt()

    private fun truncateSnippet(text: String): String =
        if (text.length <= MAX_SNIPPET_LENGTH) {
            text
        } else {
            text.take(MAX_SNIPPET_LENGTH)
        }

    /**
     * Constants and rule names for [A11yChecker].
     */
    public companion object {
        /** Rule checking that clickable/focusable targets are at least 48x48 dp. */
        public const val RULE_TOUCH_TARGET: String = "touch_target"

        /** Rule checking WCAG 1.4.3 minimum text contrast (4.5:1 normal, 3.0:1 large). */
        public const val RULE_CONTRAST: String = "contrast"

        /** Rule checking that clickable elements expose a label. */
        public const val RULE_MISSING_LABEL: String = "missing_label"

        /** Rule checking that important images provide a contentDescription. */
        public const val RULE_IMAGE_NO_LABEL: String = "image_no_label"

        /** Rule checking that editable text fields have a hint, contentDescription, or sibling label. */
        public const val RULE_EDITABLE_NO_LABEL: String = "editable_no_label"

        /** Rule checking that clickable siblings do not share identical labels. */
        public const val RULE_DUPLICATE_LABEL: String = "duplicate_label"

        /** Rule checking that labels do not repeat their semantic role word (e.g. 'button'). */
        public const val RULE_REDUNDANT_ROLE: String = "redundant_role"

        /** Complete set of all 7 supported accessibility rules. */
        public val ALL_RULES: Set<String> =
            setOf(
                RULE_TOUCH_TARGET,
                RULE_CONTRAST,
                RULE_MISSING_LABEL,
                RULE_IMAGE_NO_LABEL,
                RULE_EDITABLE_NO_LABEL,
                RULE_DUPLICATE_LABEL,
                RULE_REDUNDANT_ROLE,
            )

        /** Default upper bound on the number of nodes in a checked semantics tree. */
        public const val DEFAULT_MAX_NODES: Int = 10_000

        /** Default upper bound on the depth of a checked semantics tree. */
        public const val DEFAULT_MAX_DEPTH: Int = 128

        private const val MIN_TOUCH_TARGET_DP = 48
        private const val LARGE_TEXT_SP = 18f
        private const val LARGE_BOLD_TEXT_SP = 14f
        private const val MIN_CONTRAST_NORMAL = 4.5
        private const val MIN_CONTRAST_LARGE = 3.0
        private const val MAX_SNIPPET_LENGTH = 40
        internal const val MAX_PATH_IN_MESSAGE = 80
        internal const val MAX_MESSAGE_LENGTH = 200

        private const val WCAG_NON_TEXT_CONTENT = "1.1.1"
        private const val WCAG_INFO_AND_RELATIONSHIPS = "1.3.1"
        private const val WCAG_CONTRAST = "1.4.3"
        private const val WCAG_HEADINGS_AND_LABELS = "2.4.6"
        private const val WCAG_TOUCH_TARGET = "2.5.8"
        private const val WCAG_NAME_ROLE_VALUE = "4.1.2"

        private val WORD_SPLIT_REGEX = Regex("[^a-z0-9]+")

        private val ROLE_WORDS: Map<Role, List<String>> =
            mapOf(
                Role.BUTTON to listOf("button"),
                Role.CHECKBOX to listOf("checkbox"),
                Role.IMAGE to listOf("image", "icon", "picture", "graphic"),
                Role.SWITCH to listOf("switch", "toggle"),
            )
    }
}

private fun isInspectable(node: SemanticsNode): Boolean =
    node.importantForAccessibility && node.boundsPx.width > 0 && node.boundsPx.height > 0

private fun collectMergedDescendantText(node: SemanticsNode): String {
    val parts = mutableListOf<String>()

    fun visit(current: SemanticsNode) {
        for (child in current.children) {
            if (!isInspectable(child)) {
                continue
            }
            child.contentDescription
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.let { parts.add(it) }
            child.text
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.let { parts.add(it) }
            visit(child)
        }
    }

    visit(node)
    return parts.joinToString(" ")
}

private fun effectiveLabel(node: SemanticsNode): String? {
    val direct =
        listOfNotNull(
            node.contentDescription?.trim()?.takeIf { it.isNotEmpty() },
            node.text?.trim()?.takeIf { it.isNotEmpty() },
            node.hint?.trim()?.takeIf { it.isNotEmpty() },
        ).firstOrNull()
    if (direct != null) {
        return direct
    }
    return if (node.mergeDescendants) {
        collectMergedDescendantText(node).takeIf { it.isNotEmpty() }
    } else {
        null
    }
}

private fun buildIssue(
    rule: String,
    wcag: String,
    path: String,
    detail: String,
): Issue {
    val safePath = path.take(A11yChecker.MAX_PATH_IN_MESSAGE)
    val rawMessage = "$rule at $safePath: $detail"
    val boundedMessage = rawMessage.take(A11yChecker.MAX_MESSAGE_LENGTH)
    return Issue(
        rule = rule,
        wcag = wcag,
        path = path,
        message = boundedMessage,
    )
}
