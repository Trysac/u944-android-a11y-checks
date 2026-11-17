package io.github.trysac.android_a11y_checks

/**
 * Exception thrown when accessibility checker arguments or semantics tree bounds are invalid.
 *
 * @property code Stable machine-readable error code (for example `tree_too_large` or `too_deep`).
 * @param message Human-readable description of the failure (capped at 200 characters).
 */
public class A11yException(
    public val code: String,
    message: String = code,
) : IllegalArgumentException(message.take(MAX_MESSAGE_LENGTH)) {
    private companion object {
        private const val MAX_MESSAGE_LENGTH = 200
    }
}
