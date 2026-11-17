package io.github.trysac.android_a11y_checks

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * WCAG 2.2 relative luminance and contrast ratio calculator for sRGB colors.
 */
public object Contrast {
    private const val CHANNEL_MAX = 255.0
    private const val LINEAR_THRESHOLD = 0.04045
    private const val LINEAR_DIVISOR = 12.92
    private const val GAMMA_OFFSET = 0.055
    private const val GAMMA_DIVISOR = 1.055
    private const val GAMMA_EXPONENT = 2.4
    private const val RED_WEIGHT = 0.2126
    private const val GREEN_WEIGHT = 0.7152
    private const val BLUE_WEIGHT = 0.0722
    private const val LUMINANCE_OFFSET = 0.05
    private const val RED_SHIFT = 16
    private const val GREEN_SHIFT = 8
    private const val CHANNEL_MASK = 0xFF

    /**
     * Computes the WCAG 2.2 contrast ratio `(L1 + 0.05) / (L2 + 0.05)` between [foreground] and
     * [background] 32-bit ARGB integers. The alpha channel is ignored.
     */
    public fun ratio(
        foreground: Int,
        background: Int,
    ): Double {
        val lFg = relativeLuminance(foreground)
        val lBg = relativeLuminance(background)
        val lighter = max(lFg, lBg)
        val darker = min(lFg, lBg)
        return (lighter + LUMINANCE_OFFSET) / (darker + LUMINANCE_OFFSET)
    }

    private fun relativeLuminance(color: Int): Double {
        val r = linearize((color shr RED_SHIFT) and CHANNEL_MASK)
        val g = linearize((color shr GREEN_SHIFT) and CHANNEL_MASK)
        val b = linearize(color and CHANNEL_MASK)
        return RED_WEIGHT * r + GREEN_WEIGHT * g + BLUE_WEIGHT * b
    }

    private fun linearize(channel: Int): Double {
        val sRgb = channel / CHANNEL_MAX
        return if (sRgb <= LINEAR_THRESHOLD) {
            sRgb / LINEAR_DIVISOR
        } else {
            ((sRgb + GAMMA_OFFSET) / GAMMA_DIVISOR).pow(GAMMA_EXPONENT)
        }
    }
}
