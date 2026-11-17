package io.github.trysac.android_a11y_checks

import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.constraints.IntRange
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

/** Property-based invariants for [Contrast] and [A11yChecker]. */
class A11yPropertiesTest {
    @Property
    fun contrastRatioIsSymmetricAndBounded(
        @ForAll fg: Int,
        @ForAll bg: Int,
    ) {
        val r1 = Contrast.ratio(fg, bg)
        val r2 = Contrast.ratio(bg, fg)
        assertEquals(r1, r2, 1e-12)
        assertTrue(r1 >= 1.0 && r1 <= 21.0 + 1e-9)
        assertEquals(1.0, Contrast.ratio(fg, fg), 1e-12)
    }

    @Property
    fun touchTargetPassesIfAndOnlyIfAtLeast48DpAtUnitDensity(
        @ForAll @IntRange(min = 1, max = 120) widthPx: Int,
        @ForAll @IntRange(min = 1, max = 120) heightPx: Int,
    ) {
        val checker = A11yChecker(density = 1.0f, rules = setOf("touch_target"))
        val node =
            SemanticsNode(
                path = "root/btn",
                role = Role.BUTTON,
                boundsPx = Rect(0, 0, widthPx, heightPx),
                text = "Action",
                clickable = true,
            )
        val issues = checker.check(node).issues
        if (widthPx >= 48 && heightPx >= 48) {
            assertTrue(issues.isEmpty())
        } else {
            assertEquals(1, issues.size)
            assertEquals("touch_target", issues.single().rule)
        }
    }
}
