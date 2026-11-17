package io.github.trysac.android_a11y_checks

/** The input is empty or holds only whitespace; the message is `input is blank`. */
public class BlankInputException : IllegalArgumentException("input is blank")
