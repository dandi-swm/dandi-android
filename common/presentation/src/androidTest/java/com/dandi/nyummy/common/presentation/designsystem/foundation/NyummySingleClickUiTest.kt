package com.dandi.nyummy.common.presentation.designsystem.foundation

import android.os.SystemClock
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButton
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NyummySingleClickUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    /**
     * 아무도 듣지 않는 통로를 내려 준다. 통로를 탄 누름은 영영 실행되지 않으므로,
     * "통로를 타지 않는다"를 실제 시각과 무관하게 실행 횟수로 확인할 수 있다.
     */
    @Composable
    private fun DeadChannel(content: @Composable () -> Unit) =
        CompositionLocalProvider(LocalNyummySingleClick provides nyummySingleClickFlow(), content = content)

    private fun clickTwice(first: String, second: String) {
        composeRule.onNodeWithText(first).performClick()
        composeRule.onNodeWithText(second).performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun 서로_다른_버튼_두_개를_쉬지_않고_누르면_먼저_누른_것만_실행된다() {
        val pressed = mutableListOf<String>()
        composeRule.setContent {
            NyummyTheme {
                Column {
                    NyummyButton(text = "저장", onClick = { pressed += "저장" })
                    NyummyButton(text = "삭제", onClick = { pressed += "삭제" })
                }
            }
        }
        composeRule.waitForIdle()

        val startedAt = SystemClock.uptimeMillis()
        clickTwice("저장", "삭제")
        // 걸러 내기는 실제 400ms에 기댄다. 느린 기기에서 두 누름 사이가 400ms를 넘으면
        // 두 번째도 실행되는 게 맞으니, 실패가 아니라 건너뛴다.
        assumeTrue(SystemClock.uptimeMillis() - startedAt < NyummySingleClickPeriodMillis)

        assertEquals(listOf("저장"), pressed)
    }

    @Test
    fun singleClick_기본값인_nyummyClickable은_두_번_누르면_두_번_실행된다() {
        var ran = 0
        composeRule.setContent {
            DeadChannel { Box(Modifier.testTag("수량").nyummyClickable(onClick = { ran++ })) }
        }

        composeRule.onNodeWithTag("수량").performClick()
        composeRule.onNodeWithTag("수량").performClick()
        composeRule.waitForIdle()

        assertEquals(2, ran)
    }

    @Test
    fun 테마의_singleClick이_꺼져_있으면_버튼_두_개가_둘_다_실행된다() {
        val pressed = mutableListOf<String>()
        composeRule.setContent {
            DeadChannel {
                NyummyTheme(singleClick = false) {
                    Column {
                        NyummyButton(text = "저장", onClick = { pressed += "저장" })
                        NyummyButton(text = "삭제", onClick = { pressed += "삭제" })
                    }
                }
            }
        }

        clickTwice("저장", "삭제")

        assertEquals(listOf("저장", "삭제"), pressed)
    }

    @Test
    fun singleClick이_켜진_nyummyClickable은_통로를_타서_듣는_곳이_없으면_실행되지_않는다() {
        var ran = 0
        composeRule.setContent {
            DeadChannel { Box(Modifier.testTag("구매").nyummyClickable(onClick = { ran++ }, singleClick = true)) }
        }

        composeRule.onNodeWithTag("구매").performClick()
        composeRule.waitForIdle()

        assertEquals(0, ran)
    }

    @Test
    fun 테마_밖의_NyummyButton은_누르면_바로_실행된다() {
        var ran = 0
        composeRule.setContent { NyummyButton(text = "저장", onClick = { ran++ }) }

        composeRule.onNodeWithText("저장").performClick()
        composeRule.waitForIdle()

        assertEquals(1, ran)
    }
}
