package io.github.trysac.android_a11y_checks

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Locale

/** Unit and acceptance tests for [Contrast], [A11yChecker], and [A11yReport]. */
class A11yChecksTest {
    @Test
    fun contrastRatioAcceptanceAndRules() {
        val ratio77 = Contrast.ratio(0xFF777777.toInt(), 0xFFFFFFFF.toInt())
        assertEquals("4.48", String.format(Locale.ROOT, "%.2f", ratio77))

        val ratio76 = Contrast.ratio(0xFF767676.toInt(), 0xFFFFFFFF.toInt())
        assertEquals("4.54", String.format(Locale.ROOT, "%.2f", ratio76))

        val checker = A11yChecker(density = 1.0f, rules = setOf("contrast"))

        val failingNormalNode =
            SemanticsNode(
                path = "root/text_fail",
                role = Role.TEXT,
                boundsPx = Rect(0, 0, 100, 50),
                text = "Low contrast",
                textColor = 0xFF777777.toInt(),
                backgroundColor = 0xFFFFFFFF.toInt(),
                textSizeSp = 14f,
            )
        val reportFail = checker.check(failingNormalNode)
        assertEquals(1, reportFail.issues.size)
        assertEquals("contrast", reportFail.issues.single().rule)
        assertEquals("1.4.3", reportFail.issues.single().wcag)

        val passingNormalNode =
            failingNormalNode.copy(
                path = "root/text_pass",
                textColor = 0xFF767676.toInt(),
            )
        assertTrue(checker.check(passingNormalNode).issues.isEmpty())

        // 0xFF949494 on white has contrast ~3.03:1 -> passes for 18sp or 14sp bold, fails for 14sp normal
        val largeTextNode =
            failingNormalNode.copy(
                path = "root/text_18sp",
                textColor = 0xFF949494.toInt(),
                textSizeSp = 18f,
            )
        assertTrue(checker.check(largeTextNode).issues.isEmpty())

        val largeBoldNode =
            failingNormalNode.copy(
                path = "root/text_14sp_bold",
                textColor = 0xFF949494.toInt(),
                textSizeSp = 14f,
                bold = true,
            )
        assertTrue(checker.check(largeBoldNode).issues.isEmpty())

        val failingLargeNode =
            failingNormalNode.copy(
                path = "root/text_18sp_fail",
                textColor = 0xFF959595.toInt(),
                textSizeSp = 18f,
            )
        assertEquals(1, checker.check(failingLargeNode).issues.size)
    }

    @Test
    fun touchTargetAcceptanceAt2625Density() {
        val checker = A11yChecker(density = 2.625f)
        val smallButton =
            SemanticsNode(
                path = "root/small_btn",
                role = Role.BUTTON,
                boundsPx = Rect(0, 0, 105, 105),
                text = "Save",
                clickable = true,
            )
        val report = checker.check(smallButton)
        assertEquals(1, report.issues.size)
        val issue = report.issues.single()
        assertEquals("touch_target", issue.rule)
        assertEquals("2.5.8", issue.wcag)
        assertTrue(issue.message.contains("40x40dp"), "Expected '40x40dp' in: ${issue.message}")

        // 48 * 2.625 = 126 px
        val validButton = smallButton.copy(boundsPx = Rect(0, 0, 126, 126))
        assertTrue(checker.check(validButton).issues.isEmpty())
    }

    @Test
    fun seededSampleScreenReportsSevenIssuesAndFixedScreenReportsZero() {
        val checker = A11yChecker(density = 2.625f)
        val brokenScreen = buildSeededBrokenScreen()
        val brokenReport = checker.check(brokenScreen)

        val expectedRules =
            listOf(
                "contrast",
                "duplicate_label",
                "editable_no_label",
                "image_no_label",
                "missing_label",
                "redundant_role",
                "touch_target",
            )
        assertEquals(7, brokenReport.issues.size)
        assertEquals(expectedRules, brokenReport.issues.map { it.rule })

        val fixedScreen = buildSeededFixedScreen()
        val fixedReport = checker.check(fixedScreen)
        assertEquals(0, fixedReport.issues.size)
    }

    @Test
    fun skipsUnimportantSubtreesAndMergesDescendantsForClickableParent() {
        val checker = A11yChecker(density = 1.0f)
        val root =
            SemanticsNode(
                path = "screen",
                role = Role.OTHER,
                boundsPx = Rect(0, 0, 400, 800),
                children =
                    listOf(
                        // Unimportant subtree containing multiple broken nodes that must be skipped
                        SemanticsNode(
                            path = "screen/hidden_container",
                            role = Role.OTHER,
                            boundsPx = Rect(0, 0, 200, 200),
                            importantForAccessibility = false,
                            children =
                                listOf(
                                    SemanticsNode(
                                        path = "screen/hidden_container/tiny_unlabelled",
                                        role = Role.BUTTON,
                                        boundsPx = Rect(0, 0, 10, 10),
                                        clickable = true,
                                    ),
                                    SemanticsNode(
                                        path = "screen/hidden_container/bad_image",
                                        role = Role.IMAGE,
                                        boundsPx = Rect(0, 0, 50, 50),
                                    ),
                                ),
                        ),
                        // Clickable parent with mergeDescendants = true taking label from child text
                        SemanticsNode(
                            path = "screen/merged_card",
                            role = Role.BUTTON,
                            boundsPx = Rect(0, 200, 200, 260),
                            clickable = true,
                            mergeDescendants = true,
                            children =
                                listOf(
                                    SemanticsNode(
                                        path = "screen/merged_card/title",
                                        role = Role.TEXT,
                                        boundsPx = Rect(10, 210, 150, 240),
                                        text = "Account settings",
                                    ),
                                    SemanticsNode(
                                        path = "screen/merged_card/ignored_child",
                                        role = Role.TEXT,
                                        boundsPx = Rect(10, 240, 150, 255),
                                        text = "Hidden",
                                        importantForAccessibility = false,
                                    ),
                                ),
                        ),
                    ),
            )

        val report = checker.check(root)
        assertTrue(report.issues.isEmpty(), "Expected 0 issues, got: ${report.issues}")
    }

    @Test
    fun jsonReportIsSortedAndByteIdenticalAcrossRuns() {
        val checker = A11yChecker(density = 1.0f)
        val screen = buildSeededBrokenScreen()
        val firstRun = checker.check(screen).toJson()
        val secondRun = checker.check(screen).toJson()
        assertEquals(firstRun, secondRun)
        assertTrue(firstRun.startsWith("{\"issues\":[{\"rule\":\"contrast\",\"wcag\":\"1.4.3\","))
        assertTrue(firstRun.contains("\"path\":\""))
        assertTrue(firstRun.contains("\"message\":\""))

        val emptyReportJson = A11yReport(emptyList()).toJson()
        assertEquals("{\"issues\":[]}", emptyReportJson)
    }

    @Test
    fun enforcesMaxNodesAndMaxDepthBounds() {
        val checker = A11yChecker(density = 1.0f)

        // 10,001 nodes: 1 root + 10,000 children
        val children =
            (1..10_000).map { idx ->
                SemanticsNode(
                    path = "root/c$idx",
                    role = Role.OTHER,
                    boundsPx = Rect(0, 0, 50, 50),
                )
            }
        val hugeRoot =
            SemanticsNode(
                path = "root",
                role = Role.OTHER,
                boundsPx = Rect(0, 0, 500, 500),
                children = children,
            )
        val largeError = assertThrows<A11yException> { checker.check(hugeRoot) }
        assertEquals("tree_too_large", largeError.code)

        // Depth 129 linear chain
        var current =
            SemanticsNode(
                path = "n129",
                role = Role.OTHER,
                boundsPx = Rect(0, 0, 50, 50),
            )
        for (d in 128 downTo 1) {
            current =
                SemanticsNode(
                    path = "n$d",
                    role = Role.OTHER,
                    boundsPx = Rect(0, 0, 50, 50),
                    children = listOf(current),
                )
        }
        val deepError = assertThrows<A11yException> { checker.check(current) }
        assertEquals("too_deep", deepError.code)
    }

    @Test
    fun edgeCasesZeroSizeAlphaSingleRuleAndLongTextTruncation() {
        // Zero-size nodes are skipped as invisible
        val checker = A11yChecker(density = 1.0f)
        val zeroWidthNode =
            SemanticsNode(
                path = "root/zero_w",
                role = Role.BUTTON,
                boundsPx = Rect(10, 10, 10, 60),
                clickable = true,
            )
        val zeroHeightNode =
            SemanticsNode(
                path = "root/zero_h",
                role = Role.IMAGE,
                boundsPx = Rect(10, 10, 60, 10),
            )
        val rootWithInvisible =
            SemanticsNode(
                path = "root",
                role = Role.OTHER,
                boundsPx = Rect(0, 0, 100, 100),
                children = listOf(zeroWidthNode, zeroHeightNode),
            )
        assertTrue(checker.check(rootWithInvisible).issues.isEmpty())

        // Alpha channel is ignored in Contrast.ratio
        val opaqueRatio = Contrast.ratio(0xFF777777.toInt(), 0xFFFFFFFF.toInt())
        val translucentRatio = Contrast.ratio(0x12777777, 0x80FFFFFF.toInt())
        assertEquals(opaqueRatio, translucentRatio, 1e-12)

        // Rule set limited to a single rule ignores other problems
        val singleRuleChecker = A11yChecker(density = 1.0f, rules = setOf("image_no_label"))
        val multiIssueNode =
            SemanticsNode(
                path = "root/btn",
                role = Role.BUTTON,
                boundsPx = Rect(0, 0, 20, 20),
                clickable = true,
            )
        assertTrue(singleRuleChecker.check(multiIssueNode).issues.isEmpty())

        // Long text in redundant_role / duplicate_label is truncated to <= 40 chars and total message <= 200 chars
        val veryLongSecretLikeText = "A".repeat(80) + " button"
        val redundantNode =
            SemanticsNode(
                path = "root/" + "p".repeat(90),
                role = Role.BUTTON,
                boundsPx = Rect(0, 0, 60, 60),
                text = veryLongSecretLikeText,
                clickable = true,
            )
        val issue = checker.check(redundantNode).issues.single()
        assertEquals("redundant_role", issue.rule)
        assertTrue(issue.message.length <= 200)
        assertTrue(!issue.message.contains("A".repeat(41)))
    }

    @Test
    fun siblingTextLabelsEditableFieldAndJsonEscapesSpecialCharacters() {
        val checker = A11yChecker(density = 1.0f)
        val formWithSiblingLabel =
            SemanticsNode(
                path = "form",
                role = Role.OTHER,
                boundsPx = Rect(0, 0, 300, 200),
                children =
                    listOf(
                        SemanticsNode(
                            path = "form/email_label",
                            role = Role.TEXT,
                            boundsPx = Rect(0, 0, 100, 30),
                            text = "Username",
                        ),
                        SemanticsNode(
                            path = "form/email_input",
                            role = Role.TEXT_FIELD,
                            boundsPx = Rect(0, 35, 200, 90),
                            editable = true,
                            focusable = true,
                        ),
                    ),
            )
        assertTrue(checker.check(formWithSiblingLabel).issues.isEmpty())

        // JSON escaping for quotes, backslashes, and control characters
        val specialReport =
            A11yReport(
                listOf(
                    Issue(
                        rule = "redundant_role",
                        wcag = "4.1.2",
                        path = "root/\"quote\"\\slash\n\r\t\b\u000C\u0001",
                        message = "redundant_role at root: detail",
                    ),
                ),
            )
        val json = specialReport.toJson()
        assertTrue(json.contains("\\\"quote\\\"\\\\slash\\n\\r\\t\\b\\f\\u0001"))
    }

    @Test
    fun invalidCheckerConfigurationThrowsA11yException() {
        assertEquals(
            "invalid_density",
            assertThrows<A11yException> { A11yChecker(density = 0f) }.code,
        )
        assertEquals(
            "invalid_density",
            assertThrows<A11yException> { A11yChecker(density = Float.NaN) }.code,
        )
        assertEquals(
            "invalid_max_nodes",
            assertThrows<A11yException> { A11yChecker(density = 1f, maxNodes = 0) }.code,
        )
        assertEquals(
            "invalid_max_depth",
            assertThrows<A11yException> { A11yChecker(density = 1f, maxDepth = 0) }.code,
        )
        assertEquals(
            "unknown_rule",
            assertThrows<A11yException> { A11yChecker(density = 1f, rules = setOf("not_a_rule")) }.code,
        )
    }

    private fun buildSeededBrokenScreen(): SemanticsNode {
        val brokenChildren =
            listOf(
                SemanticsNode(
                    path = "screen/small_action",
                    role = Role.BUTTON,
                    boundsPx = Rect(0, 0, 105, 105),
                    text = "Apply",
                    clickable = true,
                ),
                SemanticsNode(
                    path = "screen/subtitle",
                    role = Role.TEXT,
                    boundsPx = Rect(0, 120, 400, 180),
                    text = "Secondary caption",
                    textColor = 0xFF777777.toInt(),
                    backgroundColor = 0xFFFFFFFF.toInt(),
                    textSizeSp = 14f,
                ),
                SemanticsNode(
                    path = "screen/icon_btn",
                    role = Role.BUTTON,
                    boundsPx = Rect(0, 200, 140, 340),
                    clickable = true,
                ),
                SemanticsNode(
                    path = "screen/hero_banner",
                    role = Role.IMAGE,
                    boundsPx = Rect(0, 360, 500, 560),
                ),
                buildBrokenSearchBox(),
                buildBrokenRowActions(),
                SemanticsNode(
                    path = "screen/submit_cta",
                    role = Role.BUTTON,
                    boundsPx = Rect(0, 920, 300, 1060),
                    contentDescription = "Submit button",
                    clickable = true,
                ),
            )
        return SemanticsNode("screen", Role.OTHER, Rect(0, 0, 1080, 2400), children = brokenChildren)
    }

    private fun buildBrokenSearchBox(): SemanticsNode =
        SemanticsNode(
            path = "screen/search_box",
            role = Role.OTHER,
            boundsPx = Rect(0, 580, 500, 720),
            children =
                listOf(
                    SemanticsNode(
                        path = "screen/search_box/input",
                        role = Role.TEXT_FIELD,
                        boundsPx = Rect(0, 580, 500, 720),
                        editable = true,
                        focusable = true,
                    ),
                ),
        )

    private fun buildBrokenRowActions(): SemanticsNode =
        SemanticsNode(
            path = "screen/row_actions",
            role = Role.OTHER,
            boundsPx = Rect(0, 740, 500, 900),
            children =
                listOf(
                    SemanticsNode(
                        path = "screen/row_actions/edit_primary",
                        role = Role.BUTTON,
                        boundsPx = Rect(0, 740, 140, 880),
                        text = "Edit",
                        clickable = true,
                    ),
                    SemanticsNode(
                        path = "screen/row_actions/edit_secondary",
                        role = Role.BUTTON,
                        boundsPx = Rect(160, 740, 300, 880),
                        text = "Edit",
                        clickable = true,
                    ),
                ),
        )

    private fun buildSeededFixedScreen(): SemanticsNode {
        val fixedChildren =
            listOf(
                SemanticsNode(
                    path = "screen/small_action",
                    role = Role.BUTTON,
                    boundsPx = Rect(0, 0, 126, 126),
                    text = "Apply",
                    clickable = true,
                ),
                SemanticsNode(
                    path = "screen/subtitle",
                    role = Role.TEXT,
                    boundsPx = Rect(0, 120, 400, 180),
                    text = "Secondary caption",
                    textColor = 0xFF767676.toInt(),
                    backgroundColor = 0xFFFFFFFF.toInt(),
                    textSizeSp = 14f,
                ),
                SemanticsNode(
                    path = "screen/icon_btn",
                    role = Role.BUTTON,
                    boundsPx = Rect(0, 200, 140, 340),
                    contentDescription = "Close",
                    clickable = true,
                ),
                SemanticsNode(
                    path = "screen/hero_banner",
                    role = Role.IMAGE,
                    boundsPx = Rect(0, 360, 500, 560),
                    contentDescription = "Spring collection overview",
                ),
                buildFixedSearchBox(),
                buildFixedRowActions(),
                SemanticsNode(
                    path = "screen/submit_cta",
                    role = Role.BUTTON,
                    boundsPx = Rect(0, 920, 300, 1060),
                    contentDescription = "Submit",
                    clickable = true,
                ),
            )
        return SemanticsNode("screen", Role.OTHER, Rect(0, 0, 1080, 2400), children = fixedChildren)
    }

    private fun buildFixedSearchBox(): SemanticsNode =
        SemanticsNode(
            path = "screen/search_box",
            role = Role.OTHER,
            boundsPx = Rect(0, 580, 500, 720),
            children =
                listOf(
                    SemanticsNode(
                        path = "screen/search_box/input",
                        role = Role.TEXT_FIELD,
                        boundsPx = Rect(0, 580, 500, 720),
                        hint = "Search orders",
                        editable = true,
                        focusable = true,
                    ),
                ),
        )

    private fun buildFixedRowActions(): SemanticsNode =
        SemanticsNode(
            path = "screen/row_actions",
            role = Role.OTHER,
            boundsPx = Rect(0, 740, 500, 900),
            children =
                listOf(
                    SemanticsNode(
                        path = "screen/row_actions/edit_primary",
                        role = Role.BUTTON,
                        boundsPx = Rect(0, 740, 140, 880),
                        text = "Edit billing address",
                        clickable = true,
                    ),
                    SemanticsNode(
                        path = "screen/row_actions/edit_secondary",
                        role = Role.BUTTON,
                        boundsPx = Rect(160, 740, 300, 880),
                        text = "Edit shipping address",
                        clickable = true,
                    ),
                ),
        )
}
