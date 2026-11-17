package io.github.trysac.android_a11y_checks

/**
 * Thrown when a text helper receives an empty or whitespace-only string.
 */
public class BlankInputException : IllegalArgumentException("input is blank")
