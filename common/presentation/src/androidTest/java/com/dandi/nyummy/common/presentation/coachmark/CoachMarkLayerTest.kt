package com.dandi.nyummy.common.presentation.coachmark

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dandi.nyummy.common.presentation.R
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTutorialExpression
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CoachMarkLayerTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var targetClicks = 0
    private var underneathClicks = 0
    private var markTargetClicks = 0
    private var advances = 0
    private var skips = 0

    /** [targetAlignment] 자리에 대상 하나, 그 밖을 덮는 "아래 화면" 버튼 하나를 두고 [mark]를 띄운다. */
    private fun setLayer(mark: CoachMark, targetAlignment: Alignment = Alignment.BottomCenter) {
        composeRule.setContent {
            NyummyTheme {
                val host = remember { CoachMarkHost() }
                CompositionLocalProvider(LocalCoachMarkHost provides host) {
                    Box(Modifier.fillMaxSize()) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .testTag(UnderneathTag)
                                .clickable { underneathClicks++ },
                        )
                        Box(
                            Modifier
                                .align(targetAlignment)
                                .offset(y = if (targetAlignment == Alignment.TopEnd) 0.dp else (-40).dp)
                                .size(60.dp)
                                .testTag(TargetTag)
                                .coachMarkTarget(TargetKey) { targetClicks++ }
                                // 실제 버튼처럼 원래 클릭도 있다. 화면을 막는 코치마크에서는 층이 탭을 받아 등록한 동작을 대신 부른다.
                                .clickable { targetClicks++ },
                        )
                        ShowCoachMark(mark)
                        CoachMarkLayer(host)
                    }
                }
            }
        }
    }

    private fun pointing(waitsForTarget: Boolean = true) = CoachMark(
        title = PointTitle,
        expression = NyummyTutorialExpression.Point,
        target = TargetKey,
        waitsForTarget = waitsForTarget,
        onAdvance = { advances++ },
        onTargetClick = { markTargetClicks++ },
        onSkip = { skips++ },
    )

    @Test
    fun 비운_곳을_누르면_대상이_등록한_동작과_코치마크_콜백을_부른다() {
        setLayer(pointing())

        composeRule.onNodeWithTag(TargetTag).performTouchInput { click() }

        assertEquals(1, targetClicks)
        assertEquals(1, markTargetClicks)
    }

    @Test
    fun 덮인_곳을_누르면_아래_화면에_닿지_않는다() {
        setLayer(pointing())

        composeRule.onNodeWithTag(UnderneathTag).performTouchInput { click(center.copy(y = height * 0.3f)) }

        assertEquals(0, underneathClicks)
        assertEquals(0, targetClicks)
    }

    @Test
    fun 대상을_눌러야_하는_단계는_카드를_눌러도_넘어가지_않는다() {
        setLayer(pointing(waitsForTarget = true))

        composeRule.onNodeWithText(PointTitle).performClick()

        assertEquals(0, advances)
    }

    @Test
    fun 소개_단계는_카드를_누르면_넘어간다() {
        setLayer(pointing(waitsForTarget = false))

        composeRule.onNodeWithText(PointTitle).performClick()

        assertEquals(1, advances)
    }

    @Test
    fun 건너뛰기를_누르면_건너뛰기_콜백을_부른다() {
        setLayer(pointing())
        val skip = InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.nyummy_tutorial_skip)

        composeRule.onNodeWithText(skip).performClick()

        assertEquals(1, skips)
    }

    @Test
    fun 건너뛸_수_없는_코치마크는_건너뛰기를_그리지_않는다() {
        setLayer(pointing().copy(onSkip = null))
        val skip = InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.nyummy_tutorial_skip)

        composeRule.onNodeWithText(skip).assertDoesNotExist()
    }

    @Test
    fun 대상이_건너뛰기_자리와_겹치면_건너뛰기를_그리지_않는다() {
        setLayer(pointing(), targetAlignment = Alignment.TopEnd)
        val skip = InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.nyummy_tutorial_skip)

        composeRule.onNodeWithText(skip).assertDoesNotExist()
    }

    @Test
    fun 대상이_건너뛰기_자리와_겹치면_그_자리를_눌러도_건너뛰지_않고_대상을_누른다() {
        setLayer(pointing(), targetAlignment = Alignment.TopEnd)

        composeRule.onNodeWithTag(TargetTag).performTouchInput { click() }

        assertEquals(0, skips)
        assertEquals(1, targetClicks)
    }

    @Test
    fun 화면을_막지_않는_코치마크는_아래_화면과_대상을_그대로_누르게_한다() {
        setLayer(pointing().copy(blocksTouches = false))

        composeRule.onNodeWithTag(UnderneathTag).performTouchInput { click(center.copy(y = height * 0.3f)) }
        composeRule.onNodeWithTag(TargetTag).performTouchInput { click() }

        assertEquals(1, underneathClicks)
        assertEquals(1, targetClicks)
        assertEquals(0, markTargetClicks)
    }

    private companion object {
        val TargetKey = CoachMarkKey("camera")
        const val TargetTag = "target"
        const val UnderneathTag = "underneath"
        const val PointTitle = "여기를 눌러 첫 기록을 해 보자!"
    }
}
