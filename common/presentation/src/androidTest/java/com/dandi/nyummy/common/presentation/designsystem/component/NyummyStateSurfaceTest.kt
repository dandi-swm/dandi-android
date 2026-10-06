package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NyummyStateSurfaceTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun 빈_상태는_말풍선과_문구를_보여주고_행동이_없으면_버튼이_없다() {
        composeRule.setContent {
            NyummyTheme { NyummyStateSurface.Empty(voice = "이날은 쉬어 갔어", message = "기록이 없는 날이에요") }
        }

        composeRule.onNodeWithText("이날은 쉬어 갔어").assertIsDisplayed()
        composeRule.onNodeWithText("기록이 없는 날이에요").assertIsDisplayed()
        composeRule.onNode(hasClickAction()).assertDoesNotExist()
    }

    @Test
    fun 빈_상태에_행동을_주면_버튼이_생긴다() {
        var clicks = 0
        composeRule.setContent {
            NyummyTheme {
                NyummyStateSurface.Empty(
                    voice = "오늘 첫 끼를 기다려",
                    message = "사진 한 장이면 기록 끝",
                    action = NyummyStateAction("기록하기") { clicks++ },
                )
            }
        }

        composeRule.onNodeWithText("기록하기").performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun 오류는_다시_시도를_호출한다() {
        var retries = 0
        composeRule.setContent {
            NyummyTheme {
                NyummyStateSurface.Error(
                    voice = "앗, 전송이 안 됐어",
                    message = "네트워크를 확인하고 다시 시도해 주세요",
                    retry = NyummyStateAction("다시 시도") { retries++ },
                )
            }
        }

        composeRule.onNodeWithText("다시 시도").performClick()

        assertEquals(1, retries)
    }

    @Test
    fun 권한은_설정_이동을_호출한다() {
        var opened = 0
        composeRule.setContent {
            NyummyTheme {
                NyummyStateSurface.Permission(
                    voice = "카메라를 빌려줄래?",
                    message = "사진을 찍어야 냐미가 밥을 먹을 수 있어요",
                    openSettings = NyummyStateAction("설정으로 이동") { opened++ },
                )
            }
        }

        composeRule.onNodeWithText("설정으로 이동").performClick()

        assertEquals(1, opened)
    }

    @Test
    fun 로딩은_끝을_알_수_없는_진행으로_알린다() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            NyummyTheme { NyummyStateSurface.Loading(voice = "냐미가 맛보는 중", message = "보통 10초 안에 끝나요") }
        }

        composeRule.onNodeWithText("보통 10초 안에 끝나요").assertExists()
        composeRule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo.Indeterminate))
            .assertExists()
    }
}
