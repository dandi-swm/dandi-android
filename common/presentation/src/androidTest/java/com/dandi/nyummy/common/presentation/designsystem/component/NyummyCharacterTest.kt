package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NyummyCharacterTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun 포즈는_주어진_크기로_그리고_설명을_전달한다() {
        composeRule.setContent {
            NyummyTheme {
                NyummyPoseImage(pose = NyummyPose.Sleep, size = NyummyTheme.size.characterL, contentDescription = "잠든 냐미")
            }
        }

        composeRule.onNodeWithContentDescription("잠든 냐미").assertWidthIsEqualTo(160.dp)
    }

    @Test
    fun 모든_포즈_리소스를_불러올_수_있다() {
        composeRule.setContent {
            NyummyTheme {
                androidx.compose.foundation.layout.Column {
                    NyummyPose.entries.forEach { pose ->
                        NyummyPoseImage(pose = pose, size = NyummyTheme.size.characterXs, contentDescription = pose.name)
                    }
                }
            }
        }

        NyummyPose.entries.forEach { composeRule.onNodeWithContentDescription(it.name).assertIsDisplayed() }
    }

    @Test
    fun 말풍선은_꼬리_방향과_상관없이_문구를_보여준다() {
        composeRule.setContent {
            NyummyTheme {
                androidx.compose.foundation.layout.Column {
                    NyummyBubbleTail.entries.forEach { tail -> NyummyVoiceBubble(text = "꼬리 ${tail.name}", tail = tail) }
                }
            }
        }

        NyummyBubbleTail.entries.forEach { composeRule.onNodeWithText("꼬리 ${it.name}").assertIsDisplayed() }
    }

    @Test
    fun 짧은_코멘트는_더보기가_없고_누를_수_없다() {
        composeRule.setContent {
            NyummyTheme { NyummyCoachCard(text = "오늘 첫 끼 최고였어", modifier = Modifier.width(350.dp)) }
        }

        composeRule.onNodeWithText("더보기").assertDoesNotExist()
        composeRule.onNodeWithText("오늘 첫 끼 최고였어").assertHasNoClickAction()
    }

    @Test
    fun 세_줄을_넘는_코멘트는_더보기로_펼치고_접기로_접는다() {
        val longText = List(8) { "채소 가득한 비빔밥이네! 다음 끼니도 같이 먹자." }.joinToString(" ")
        composeRule.setContent {
            NyummyTheme { NyummyCoachCard(text = longText, modifier = Modifier.width(350.dp)) }
        }

        composeRule.onNodeWithText("더보기").assertIsDisplayed()
        composeRule.onNodeWithText(longText).performClick()
        composeRule.onNodeWithText("접기").assertIsDisplayed()
        composeRule.onNodeWithText(longText).performClick()
        composeRule.onNodeWithText("더보기").assertIsDisplayed()
    }

    @Test
    fun 보이스_토스트는_문구를_보여준다() {
        composeRule.setContent {
            NyummyTheme { NyummyVoiceToast(text = "기록 완료! 냐미가 맛있게 먹었어") }
        }

        composeRule.onNodeWithText("기록 완료! 냐미가 맛있게 먹었어").assertIsDisplayed()
    }
}
