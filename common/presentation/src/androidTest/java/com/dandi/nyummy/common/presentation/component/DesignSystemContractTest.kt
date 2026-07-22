package com.dandi.nyummy.common.presentation.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.focusable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasRequestFocusAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isEditable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.click
import androidx.compose.ui.test.down
import androidx.compose.ui.test.up
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemTheme
import androidx.test.espresso.Espresso.closeSoftKeyboard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlinx.coroutines.awaitCancellation

class DesignSystemContractTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun bottomNavigation_hasCanonicalFiveDestinations_and370By76FloatingFixture() {
        val selected = mutableStateOf(NyummyNavigationDestination.Home)

        composeRule.setContent {
            DesignSystemTheme {
                NyummyBottomNavigation(
                    selectedDestination = selected.value,
                    modifier = Modifier.testTag(NavigationTag),
                    style = NyummyBottomNavigationStyle.Floating,
                    onDestinationSelected = { selected.value = it },
                )
            }
        }

        composeRule.onNodeWithTag(NavigationTag)
            .assertWidthIsEqualTo(370.dp)
            .assertHeightIsEqualTo(76.dp)

        composeRule.onAllNodes(
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab),
        ).assertCountEquals(5)
        NyummyNavigationDestination.entries.forEach { destination ->
            composeRule.onNodeWithContentDescription(destination.label).assertHasClickAction()
        }
        composeRule.onNodeWithContentDescription("홈").assertIsSelected()
        composeRule.onNodeWithContentDescription("히스토리").assertIsNotSelected().performClick()
        composeRule.onNodeWithContentDescription("히스토리").assertIsSelected()
        composeRule.onNodeWithContentDescription("홈").assertIsNotSelected()
    }

    @Test
    fun bottomNavigation_limitsPressedFeedbackToIndicator_andSlidesToSelectedTab() {
        val selected = mutableStateOf(NyummyNavigationDestination.Home)
        composeRule.mainClock.autoAdvance = false

        composeRule.setContent {
            DesignSystemTheme {
                NyummyBottomNavigation(
                    selectedDestination = selected.value,
                    style = NyummyBottomNavigationStyle.Floating,
                    onDestinationSelected = { selected.value = it },
                )
            }
        }

        val indicator = composeRule.onNodeWithTag(
            NavigationIndicatorTag,
            useUnmergedTree = true,
        )
        val historyItem = composeRule.onNodeWithTag(
            HistoryNavigationItemTag,
            useUnmergedTree = true,
        )
        val startX = indicator.getUnclippedBoundsInRoot().left.value
        val restingItem = historyItem.captureToImage()

        historyItem.performTouchInput { down(center) }
        composeRule.mainClock.advanceTimeBy(NavigationPressedFeedbackMillis)
        composeRule.waitForIdle()
        val pressedItem = historyItem.captureToImage()
        assertImagesDiffer(
            restingItem,
            pressedItem,
            "navigation press did not render feedback",
        )
        assertImageCornersUnchanged(
            restingItem,
            pressedItem,
            "navigation press feedback escaped the rounded item clip",
        )
        assertImageChangesContainedWithinIndicator(
            restingItem,
            pressedItem,
            "navigation press feedback escaped the icon indicator bounds",
        )

        historyItem.performTouchInput { up() }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("히스토리").assertIsSelected()

        composeRule.mainClock.advanceTimeBy(NavigationIndicatorMidpointMillis)
        composeRule.waitForIdle()
        val midpointX = indicator.getUnclippedBoundsInRoot().left.value
        composeRule.mainClock.advanceTimeBy(NavigationIndicatorSettleMillis)
        composeRule.waitForIdle()
        val endX = indicator.getUnclippedBoundsInRoot().left.value

        assertTrue("indicator did not move after selection", midpointX > startX)
        assertTrue("indicator jumped without an intermediate frame", midpointX < endX)
        assertEquals(
            "indicator did not settle one item width away",
            startX + NavigationItemWidthDp,
            endX,
            NavigationIndicatorPositionTolerance,
        )
    }

    @Test
    fun chip_withoutLeadingIcon_centersLabelInCanonical72By48Bounds() {
        composeRule.setContent {
            DesignSystemTheme {
                NyummyChip(
                    label = ChipLabel,
                    selected = false,
                    onClick = {},
                    modifier = Modifier.testTag(ChipTag),
                )
            }
        }

        val chip = composeRule.onNodeWithTag(ChipTag)
            .assertWidthIsEqualTo(72.dp)
            .assertHeightIsEqualTo(48.dp)
        val chipBounds = chip.getUnclippedBoundsInRoot()
        val labelBounds = composeRule
            .onNodeWithText(ChipLabel, useUnmergedTree = true)
            .getUnclippedBoundsInRoot()

        assertEquals(
            "chip label horizontal center",
            (chipBounds.left.value + chipBounds.right.value) / 2f,
            (labelBounds.left.value + labelBounds.right.value) / 2f,
            ChipCenterTolerance,
        )
        assertEquals(
            "chip label vertical center",
            (chipBounds.top.value + chipBounds.bottom.value) / 2f,
            (labelBounds.top.value + labelBounds.bottom.value) / 2f,
            ChipCenterTolerance,
        )
    }

    @Test
    @OptIn(ExperimentalComposeUiApi::class)
    fun textField_all12TypeStateContracts_haveCanonicalBoundsFocusErrorAndEditability() {
        val nonFocusedStates = InputState.entries.filterNot { it == InputState.Focused }
        val fixtures = listOf(InputType.Multiline, InputType.SingleLine).flatMap { type ->
            nonFocusedStates.map { state -> InputFixture(type, state) }
        } + listOf(
            InputFixture(InputType.Multiline, InputState.Focused),
            FinalInputInteractionFixture,
        )
        val fixture = mutableStateOf(fixtures.first())
        var editCount = 0
        var inputLayoutSize = IntSize.Zero
        lateinit var density: Density
        lateinit var focusParking: FocusRequester
        lateinit var inputModeManager: InputModeManager
        var softwareKeyboardController: SoftwareKeyboardController? = null

        composeRule.setContent {
            InterceptPlatformTextInput(
                interceptor = { _, _ -> awaitCancellation() },
            ) {
                DesignSystemTheme {
                    density = LocalDensity.current
                    inputModeManager = LocalInputModeManager.current
                    softwareKeyboardController = LocalSoftwareKeyboardController.current
                    focusParking = remember { FocusRequester() }
                    val current = fixture.value
                    Column {
                        FocusParking(FocusParkingInputTag, focusParking)
                        Row(Modifier.horizontalScroll(rememberScrollState())) {
                            key(current.inventoryKey) {
                                var value by remember { mutableStateOf(current.initialValue) }
                                NyummyTextField(
                                    value = value,
                                    onValueChange = {
                                        editCount++
                                        value = it
                                    },
                                    modifier = Modifier
                                        .width(InputFixtureWidth)
                                        .onGloballyPositioned { inputLayoutSize = it.size }
                                        .testTag(InputTag),
                                    placeholder = "식사 이름을 입력해 주세요",
                                    label = InputLabel,
                                    helperText = current.helperText,
                                    isError = current.state == InputState.Error,
                                    enabled = current.state != InputState.Disabled,
                                    readOnly = current.state == InputState.ReadOnly,
                                    singleLine = current.type == InputType.SingleLine,
                                )
                            }
                        }
                    }
                }
            }
        }

        assertEquals(12, fixtures.size)
        assertEquals(12, fixtures.map { it.inventoryKey }.toSet().size)

        fixtures.forEach { current ->
            withFixtureContext("Input/${current.inventoryKey}") {
                composeRule.runOnIdle {
                    editCount = 0
                    fixture.value = current
                }
                composeRule.waitForIdle()
                composeRule.runOnIdle {
                    assertTrue(
                        "could not enter hardware-keyboard input mode",
                        inputModeManager.requestInputMode(InputMode.Keyboard),
                    )
                    focusParking.requestFocus()
                }
                composeRule.waitForIdle()
                composeRule.onNodeWithTag(FocusParkingInputTag).assertIsFocused()
                composeRule.runOnIdle { softwareKeyboardController?.hide() }
                closeSoftKeyboard()
                composeRule.waitUntil(timeoutMillis = KeyboardDismissTimeoutMillis) {
                    inputLayoutSize.height >= with(density) {
                        current.minimumFixtureHeight.roundToPx()
                    }
                }

                composeRule.runOnIdle {
                    assertEquals(
                        "${current.inventoryKey} layout width px",
                        with(density) { InputFixtureWidth.roundToPx() },
                        inputLayoutSize.width,
                    )
                }

                composeRule.onNodeWithTag(InputTag)
                    .assertWidthIsAtLeast(InputVisibleMinimumWidth)
                    .assertHeightIsAtLeast(current.minimumFixtureHeight)

                val field = composeRule.onNodeWithContentDescription(InputLabel)
                    // BasicTextField semantics sit inside canonical content padding. The outer
                    // 360x56/120 field is proven by inputLayoutSize/fixture height above.
                    .assertWidthIsEqualTo(InputSemanticContentWidth)
                    .assertHeightIsEqualTo(current.semanticContentHeight)

                if (current.state == InputState.Focused) {
                    composeRule.runOnIdle {
                        inputModeManager.requestInputMode(InputMode.Keyboard)
                    }
                    field.requestFocus()
                    composeRule.waitForIdle()
                    field.assertIsFocused()
                }
                field.assertStateDescription(current.expectedStateDescription)

                if (current.state == InputState.Error) {
                    field.assert(
                        SemanticsMatcher.expectValue(
                            SemanticsProperties.Error,
                            InputErrorMessage,
                        ),
                    )
                } else {
                    field.assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Error))
                }

                when (current.state) {
                    InputState.Disabled -> field
                        .assertIsNotEnabled()
                        .assert(!hasRequestFocusAction())
                        .assert(!hasSetTextAction())
                        .assert(!isEditable())

                    InputState.ReadOnly -> {
                        composeRule.runOnIdle {
                            inputModeManager.requestInputMode(InputMode.Keyboard)
                        }
                        field
                            .assertIsEnabled()
                            .assert(hasRequestFocusAction())
                            .assert(!hasSetTextAction())
                            .assert(!isEditable())
                            .assertStateDescription("읽기 전용")
                    }

                    else -> {
                        field
                            .assertIsEnabled()
                            .assert(hasRequestFocusAction())
                            .assert(hasSetTextAction())
                            .assert(isEditable())
                        when {
                            current == FinalInputInteractionFixture -> {
                                field.performTextReplacement(InputReplacement)
                                composeRule.waitForIdle()
                                composeRule.onNodeWithContentDescription(InputLabel)
                                    .assertTextEquals(InputReplacement)
                                composeRule.runOnIdle { assertEquals(1, editCount) }
                            }

                            current.state != InputState.Focused -> {
                                field.performSemanticsAction(SemanticsActions.SetText) { setText ->
                                    assertTrue(
                                        "${current.inventoryKey} rejected SetText",
                                        setText(AnnotatedString(InputReplacement)),
                                    )
                                }
                                composeRule.waitForIdle()
                                composeRule.onNodeWithContentDescription(InputLabel)
                                    .assertTextEquals(InputReplacement)
                                composeRule.runOnIdle { assertEquals(1, editCount) }
                            }

                            else -> composeRule.runOnIdle { assertEquals(0, editCount) }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun stateSurface_all14Types_keepSizeStateAndActionSemantics() {
        val currentType = mutableStateOf(StateSurfaceContracts.first().type)

        composeRule.setContent {
            DesignSystemTheme {
                NyummyStateSurface(
                    type = currentType.value,
                    modifier = Modifier.testTag(StateSurfaceTag),
                    onAction = {},
                    onSecondaryAction = {},
                )
            }
        }

        assertEquals(14, StateSurfaceContracts.size)
        assertEquals(NyummyStateSurfaceType.entries.toSet(), StateSurfaceContracts.map { it.type }.toSet())

        StateSurfaceContracts.forEach { contract ->
            composeRule.runOnIdle { currentType.value = contract.type }
            composeRule.waitForIdle()

            composeRule.onNodeWithTag(StateSurfaceTag)
                .assertWidthIsEqualTo(190.dp)
                .assertHeightIsEqualTo(220.dp)
                .assertStateDescription(contract.title)

            contract.actions.forEach { action ->
                composeRule.onNodeWithText(action).assertHasClickAction()
            }

            val expectedClickActions = contract.actions.size
            composeRule.onAllNodes(hasClickAction(), useUnmergedTree = true)
                .assertCountEquals(expectedClickActions)

            val progressNodes = composeRule.onAllNodes(
                SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo),
                useUnmergedTree = true,
            )
            progressNodes.assertCountEquals(if (contract.progressLabel == null) 0 else 1)
            contract.progressLabel?.let { label ->
                composeRule.onNode(
                    SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo),
                    useUnmergedTree = true,
                )
                    .assertStateDescription(label)
                    .assert(
                        SemanticsMatcher.expectValue(
                            SemanticsProperties.ProgressBarRangeInfo,
                            ProgressBarRangeInfo.Indeterminate,
                        ),
                    )
            }
        }
    }

    @Test
    fun calendarDay_rendersAll24SelectionIconNutritionCombinations() {
        val fixture = mutableStateOf(
            CalendarFixture(
                selected = false,
                iconCount = 0,
                nutritionStatus = NyummyCalendarNutritionStatus.Positive,
            ),
        )

        composeRule.setContent {
            DesignSystemTheme {
                val current = fixture.value
                NyummyCalendarDay(
                    day = "22",
                    modifier = Modifier.testTag(CalendarDayTag),
                    onClick = {},
                    selected = current.selected,
                    nutritionStatus = current.nutritionStatus,
                    firstFoodIcon = foodIconOrNull(current.iconCount >= 1),
                    secondFoodIcon = foodIconOrNull(current.iconCount >= 2),
                )
            }
        }

        val fixtures = listOf(false, true).flatMap { selected ->
            (0..2).flatMap { iconCount ->
                NyummyCalendarNutritionStatus.entries.map { nutritionStatus ->
                    CalendarFixture(selected, iconCount, nutritionStatus)
                }
            }
        }
        assertEquals(24, fixtures.size)

        fixtures.forEach { current ->
            composeRule.runOnIdle { fixture.value = current }
            composeRule.waitForIdle()

            val dayNode = composeRule.onNodeWithTag(CalendarDayTag)
            if (current.selected) dayNode.assertIsSelected() else dayNode.assertIsNotSelected()

            val expectedDescription = buildList {
                if (current.selected) add("선택됨")
                when (current.nutritionStatus) {
                    NyummyCalendarNutritionStatus.Positive -> add("영양 균형 양호")
                    NyummyCalendarNutritionStatus.OutOfRange -> add("영양 범위 초과")
                    NyummyCalendarNutritionStatus.NoRecord -> add("영양 기록 없음")
                    NyummyCalendarNutritionStatus.None -> Unit
                }
                if (current.iconCount > 0) add("식사 ${current.iconCount}개")
            }.joinToString()

            if (expectedDescription.isEmpty()) {
                dayNode.assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription))
            } else {
                dayNode.assertStateDescription(expectedDescription)
            }
        }
    }

    @Test
    fun button_all80StyleStateSizeWidthContracts_renderAndUseRealInteractions() {
        val fixtures = NyummyButtonStyle.entries.flatMap { style ->
            ButtonState.entries.flatMap { state ->
                NyummyButtonSize.entries.flatMap { size ->
                    ButtonWidth.entries.map { width ->
                        ButtonFixture(style, state, size, width)
                    }
                }
            }
        }
        val fixture = mutableStateOf(fixtures.first())
        var clickCount = 0
        lateinit var inputModeManager: InputModeManager
        lateinit var focusParking: FocusRequester

        composeRule.setContent {
            DesignSystemTheme {
                inputModeManager = LocalInputModeManager.current
                focusParking = remember { FocusRequester() }
                val current = fixture.value
                Column {
                    FocusParking(FocusParkingButtonTag, focusParking)
                    Box(Modifier.width(ButtonFullWidth)) {
                        key(current.inventoryKey) {
                            NyummyButton(
                                label = ButtonLabel,
                                modifier = Modifier
                                    .testTag(ButtonTag)
                                    .then(
                                        if (current.width == ButtonWidth.Full) {
                                            Modifier.fillMaxWidth()
                                        } else {
                                            Modifier
                                        },
                                    ),
                                style = current.style,
                                size = current.size,
                                enabled = current.state != ButtonState.Disabled,
                                loading = current.state == ButtonState.Loading,
                                onClick = { clickCount++ },
                            )
                        }
                    }
                }
            }
        }

        assertEquals(80, fixtures.size)
        assertEquals(80, fixtures.map { it.inventoryKey }.toSet().size)

        fixtures.forEach { current ->
            withFixtureContext("Button/${current.inventoryKey}") {
                composeRule.runOnIdle {
                    clickCount = 0
                    fixture.value = current
                }
                composeRule.waitForIdle()
                composeRule.runOnIdle {
                    assertTrue(
                        "could not enter hardware-keyboard input mode",
                        inputModeManager.requestInputMode(InputMode.Keyboard),
                    )
                    focusParking.requestFocus()
                }
                composeRule.waitForIdle()
                composeRule.onNodeWithTag(FocusParkingButtonTag).assertIsFocused()

                val button = composeRule.onNodeWithTag(ButtonTag)
                    .assertHeightIsEqualTo(current.expectedHeight)
                    .assertWidthIsAtLeast(ButtonMinimumWidth)
                    .assertHasClickAction()
                    .assert(
                        SemanticsMatcher.expectValue(
                            SemanticsProperties.Role,
                            Role.Button,
                        ),
                    )
                if (current.width == ButtonWidth.Full) {
                    button.assertWidthIsEqualTo(ButtonFullWidth)
                }

                when (current.state) {
                    ButtonState.Default -> {
                        button
                            .assertIsEnabled()
                            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription))
                            .performClick()
                        composeRule.runOnIdle { assertEquals(1, clickCount) }
                    }

                    ButtonState.Pressed -> {
                        button.assertIsEnabled()
                        val defaultImage = button.captureToImage()
                        button.performTouchInput { down(center) }
                        composeRule.waitForIdle()
                        composeRule.runOnIdle { assertEquals(0, clickCount) }
                        assertImagesDiffer(
                            defaultImage,
                            button.captureToImage(),
                            "pressed interaction did not change rendering",
                        )
                        button.performTouchInput { up() }
                        composeRule.waitForIdle()
                        composeRule.runOnIdle { assertEquals(1, clickCount) }
                    }

                    ButtonState.Focused -> {
                        button
                            .assertIsEnabled()
                            .assertIsNotFocused()
                        val defaultImage = button.captureToImage()
                        composeRule.runOnIdle {
                            assertTrue(
                                "could not enter hardware-keyboard input mode",
                                inputModeManager.requestInputMode(InputMode.Keyboard),
                            )
                        }
                        button.requestFocus()
                        composeRule.waitForIdle()
                        button.assertIsFocused()
                        assertImagesDiffer(
                            defaultImage,
                            button.captureToImage(),
                            "focus interaction did not change rendering",
                        )
                        button.performKeyInput { pressKey(Key.Enter) }
                        composeRule.waitForIdle()
                        composeRule.runOnIdle { assertEquals(1, clickCount) }
                    }

                    ButtonState.Disabled -> {
                        button
                            .assertIsNotEnabled()
                            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription))
                            .performTouchInput { click() }
                        composeRule.runOnIdle { assertEquals(0, clickCount) }
                    }

                    ButtonState.Loading -> {
                        button
                            .assertIsNotEnabled()
                            .assertStateDescription("로딩 중")
                            .performTouchInput { click() }
                        composeRule.runOnIdle { assertEquals(0, clickCount) }
                    }
                }
            }
        }
    }

    private companion object {
        const val NavigationTag = "design_system_navigation"
        const val NavigationIndicatorTag = "nyummy_bottom_navigation_indicator"
        const val HistoryNavigationItemTag = "nyummy_bottom_navigation_item_History"
        const val ChipTag = "design_system_chip"
        const val StateSurfaceTag = "design_system_state_surface"
        const val CalendarDayTag = "design_system_calendar_day"
        const val ButtonTag = "design_system_button"
        const val InputTag = "design_system_input"
        const val FocusParkingButtonTag = "design_system_button_focus_parking"
        const val FocusParkingInputTag = "design_system_input_focus_parking"
        const val ButtonLabel = "가"
        const val ChipLabel = "전체"
        const val InputLabel = "음식 이름"
        const val InputErrorMessage = "입력 내용을 확인해 주세요"
        const val InputReplacement = "새 값"
        const val KeyboardDismissTimeoutMillis = 5_000L
        const val ChipCenterTolerance = 0.5f
        const val NavigationPressedFeedbackMillis = 120L
        const val NavigationIndicatorMidpointMillis = 140L
        const val NavigationIndicatorSettleMillis = 240L
        const val NavigationItemWidthDp = 70f
        const val NavigationIndicatorPositionTolerance = 0.5f

        val ButtonMinimumWidth = 96.dp
        val ButtonFullWidth = 320.dp
        val InputFixtureWidth = 360.dp
        val InputVisibleMinimumWidth = 320.dp
        val InputSemanticContentWidth = 328.dp
        val FinalInputInteractionFixture = InputFixture(
            InputType.SingleLine,
            InputState.Focused,
        )

        val StateSurfaceContracts = listOf(
            StateSurfaceContract(NyummyStateSurfaceType.Empty, "아직 기록이 없어요", listOf("기록 시작")),
            StateSurfaceContract(NyummyStateSurfaceType.Loading, "기록을 불러오는 중", progressLabel = "진행 표시"),
            StateSurfaceContract(NyummyStateSurfaceType.Offline, "연결을 확인해 주세요", listOf("다시 시도")),
            StateSurfaceContract(NyummyStateSurfaceType.AnalysisFailed, "분석하지 못했어요", listOf("다시 분석")),
            StateSurfaceContract(NyummyStateSurfaceType.PermissionDenied, "사진 접근이 필요해요", listOf("설정 열기")),
            StateSurfaceContract(NyummyStateSurfaceType.Ended, "이벤트가 끝났어요", listOf("다른 소식")),
            StateSurfaceContract(NyummyStateSurfaceType.Destructive, "기록을 삭제할까요?", listOf("취소", "삭제")),
            StateSurfaceContract(NyummyStateSurfaceType.Partial, "일부만 분석됐어요", listOf("실패 식사 보기")),
            StateSurfaceContract(NyummyStateSurfaceType.Retrying, "다시 분석하는 중", progressLabel = "진행 표시"),
            StateSurfaceContract(NyummyStateSurfaceType.RewardPending, "보상 처리 중", progressLabel = "처리 중"),
            StateSurfaceContract(NyummyStateSurfaceType.RewardCompleted, "코인 80개 받았어요", listOf("확인")),
            StateSurfaceContract(NyummyStateSurfaceType.AlreadyClaimed, "이미 받은 보상이에요", listOf("지갑 보기")),
            StateSurfaceContract(NyummyStateSurfaceType.ReconcileFailed, "보상 확인이 필요해요", listOf("다시 확인")),
            StateSurfaceContract(NyummyStateSurfaceType.RateLimited, "잠시 후 다시 해주세요", listOf("10초 후 다시 시도")),
        )
    }
}

private data class StateSurfaceContract(
    val type: NyummyStateSurfaceType,
    val title: String,
    val actions: List<String> = emptyList(),
    val progressLabel: String? = null,
)

private data class CalendarFixture(
    val selected: Boolean,
    val iconCount: Int,
    val nutritionStatus: NyummyCalendarNutritionStatus,
)

private enum class ButtonState {
    Default,
    Pressed,
    Focused,
    Disabled,
    Loading,
}

private enum class ButtonWidth {
    Hug,
    Full,
}

private data class ButtonFixture(
    val style: NyummyButtonStyle,
    val state: ButtonState,
    val size: NyummyButtonSize,
    val width: ButtonWidth,
) {
    val inventoryKey = "${style.name}/${state.name}/${size.name}/${width.name}"
    val expectedHeight = when (size) {
        NyummyButtonSize.Medium -> 48.dp
        NyummyButtonSize.Large -> 56.dp
    }
}

private enum class InputType {
    SingleLine,
    Multiline,
}

private enum class InputState {
    Empty,
    Focused,
    Filled,
    Error,
    Disabled,
    ReadOnly,
}

private data class InputFixture(
    val type: InputType,
    val state: InputState,
) {
    val inventoryKey = "${type.name}/${state.name}"
    val initialValue = when (state) {
        InputState.Empty,
        InputState.Focused,
        -> ""

        InputState.Filled -> "닭가슴살 포케"
        InputState.Error -> "확인 필요"
        InputState.Disabled -> "처리 중"
        InputState.ReadOnly -> "완료된 기록"
    }
    val helperText = if (state == InputState.Error) {
        "입력 내용을 확인해 주세요"
    } else {
        "사진과 함께 음식 이름을 입력해 주세요"
    }
    val fieldHeight = when (type) {
        InputType.SingleLine -> 56.dp
        InputType.Multiline -> 120.dp
    }
    val semanticContentHeight = when (type) {
        InputType.SingleLine -> fieldHeight
        InputType.Multiline -> 92.dp
    }
    // Text line-height to physical-pixel conversion can round the 106/170dp reference fixture
    // down by less than 1dp on fractional-density devices. The field itself remains exact above.
    val minimumFixtureHeight = when (type) {
        InputType.SingleLine -> 105.dp
        InputType.Multiline -> 169.dp
    }
    val expectedStateDescription = when (state) {
        InputState.Empty -> "비어 있음"
        InputState.Focused -> "입력 중"
        InputState.Filled -> "입력됨"
        InputState.Error -> "오류"
        InputState.Disabled -> "사용 안 함"
        InputState.ReadOnly -> "읽기 전용"
    }
}

@Composable
private fun foodIconOrNull(visible: Boolean): (@Composable () -> Unit)? =
    if (visible) {
        {
            Box(
                Modifier
                    .size(24.dp)
                    .semantics { },
            )
        }
    } else {
        null
    }

private fun androidx.compose.ui.test.SemanticsNodeInteraction.assertStateDescription(
    expected: String,
) = assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, expected))

@Composable
private fun FocusParking(
    tag: String,
    focusRequester: FocusRequester,
) {
    Box(
        Modifier
            .size(1.dp)
            .focusRequester(focusRequester)
            .focusable()
            .testTag(tag),
    )
}

private fun assertImagesDiffer(
    before: ImageBitmap,
    after: ImageBitmap,
    failureMessage: String,
) {
    assertEquals("image width", before.width, after.width)
    assertEquals("image height", before.height, after.height)
    val beforePixels = before.toPixelMap()
    val afterPixels = after.toPixelMap()
    val differs = (0 until before.height).any { y ->
        (0 until before.width).any { x -> beforePixels[x, y] != afterPixels[x, y] }
    }
    assertTrue(failureMessage, differs)
}

private fun assertImageCornersUnchanged(
    before: ImageBitmap,
    after: ImageBitmap,
    failureMessage: String,
) {
    assertEquals("image width", before.width, after.width)
    assertEquals("image height", before.height, after.height)
    val beforePixels = before.toPixelMap()
    val afterPixels = after.toPixelMap()
    val corners = listOf(
        0 to 0,
        before.width - 1 to 0,
        0 to before.height - 1,
        before.width - 1 to before.height - 1,
    )
    assertTrue(
        failureMessage,
        corners.all { (x, y) -> beforePixels[x, y] == afterPixels[x, y] },
    )
}

private fun assertImageChangesContainedWithinIndicator(
    before: ImageBitmap,
    after: ImageBitmap,
    failureMessage: String,
) {
    assertEquals("image width", before.width, after.width)
    assertEquals("image height", before.height, after.height)
    val beforePixels = before.toPixelMap()
    val afterPixels = after.toPixelMap()
    val indicatorLeft = (before.width * NavigationIndicatorStartFraction).toInt()
    val indicatorRight = (before.width * NavigationIndicatorEndFraction).toInt()
    val indicatorBottom = (before.height * NavigationIndicatorBottomFraction).toInt()
    val changesStayInsideIndicator = (0 until before.height).all { y ->
        (0 until before.width).all { x ->
            val isInsideIndicator = x in indicatorLeft until indicatorRight && y < indicatorBottom
            isInsideIndicator || beforePixels[x, y] == afterPixels[x, y]
        }
    }
    assertTrue(failureMessage, changesStayInsideIndicator)
}

private const val NavigationIndicatorStartFraction = 13f / 70f
private const val NavigationIndicatorEndFraction = 57f / 70f
private const val NavigationIndicatorBottomFraction = 30f / 58f

private inline fun withFixtureContext(
    fixture: String,
    block: () -> Unit,
) {
    try {
        block()
    } catch (failure: AssertionError) {
        throw AssertionError("Contract fixture failed: $fixture\n${failure.message}", failure)
    }
}
