package io.github.trysac.android_a11y_checks

/**
 * Semantic role of a UI element in an Android View or Compose hierarchy.
 */
public enum class Role {
    /** A button widget or clickable action element. */
    BUTTON,

    /** A two-state checkbox widget. */
    CHECKBOX,

    /** An image or icon element. */
    IMAGE,

    /** A static text label. */
    TEXT,

    /** An editable text input field. */
    TEXT_FIELD,

    /** A two-state toggle switch widget. */
    SWITCH,

    /** Any other layout or custom role. */
    OTHER,
}

/**
 * Axis-aligned bounding box in physical pixels.
 *
 * @property left Left X coordinate in pixels.
 * @property top Top Y coordinate in pixels.
 * @property right Right X coordinate in pixels.
 * @property bottom Bottom Y coordinate in pixels.
 */
public data class Rect(
    public val left: Int,
    public val top: Int,
    public val right: Int,
    public val bottom: Int,
) {
    /** Width in pixels. */
    public val width: Int
        get() = right - left

    /** Height in pixels. */
    public val height: Int
        get() = bottom - top
}

/**
 * Platform-neutral snapshot of an Android View or Jetpack Compose semantics node.
 *
 * @property path Deterministic path identifier of the node in the screen tree.
 * @property role Semantic [Role] of the node.
 * @property boundsPx Bounding box on screen in physical pixels.
 * @property text Visible text of the node, if any.
 * @property contentDescription Accessibility content description, if any.
 * @property hint Input hint text for editable fields, if any.
 * @property clickable Whether the node responds to click actions.
 * @property editable Whether the node accepts text editing.
 * @property focusable Whether the node can take accessibility/input focus.
 * @property importantForAccessibility Whether the node and its subtree are exposed to accessibility services.
 * @property mergeDescendants Whether descendant text/descriptions are merged into this node.
 * @property textColor Foreground ARGB color of the text, if known.
 * @property backgroundColor Background ARGB color behind the text, if known.
 * @property textSizeSp Font size in scale-independent pixels (sp), if known.
 * @property bold Whether the text uses a bold font weight.
 * @property children Child semantics nodes in hierarchy order.
 */
public data class SemanticsNode(
    public val path: String,
    public val role: Role,
    public val boundsPx: Rect,
    public val text: String? = null,
    public val contentDescription: String? = null,
    public val hint: String? = null,
    public val clickable: Boolean = false,
    public val editable: Boolean = false,
    public val focusable: Boolean = false,
    public val importantForAccessibility: Boolean = true,
    public val mergeDescendants: Boolean = false,
    public val textColor: Int? = null,
    public val backgroundColor: Int? = null,
    public val textSizeSp: Float? = null,
    public val bold: Boolean = false,
    public val children: List<SemanticsNode> = emptyList(),
)
