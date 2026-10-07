package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NyummyTypewriterTextTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun 한_글자씩_찍다가_다_찍으면_알리고_읽기_도구에는_처음부터_문장_전체가_보인다() {
        val line = "냠냠 맛있다"
        var revealed = 0
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            NyummyTheme {
                NyummyTypewriterText(text = line, style = NyummyTheme.typography.voiceM, onRevealed = { revealed++ })
            }
        }

        composeRule.onNodeWithText(line).assertIsDisplayed()
        composeRule.mainClock.advanceTimeBy(NyummyTypewriterCharDelayMillis * (line.length - 1))
        assertEquals(0, revealed)

        composeRule.mainClock.advanceTimeBy(NyummyTypewriterCharDelayMillis * 2)
        assertEquals(1, revealed)
    }

    @Test
    fun 대사가_바뀌면_처음부터_다시_찍는다() {
        var line by mutableStateOf("냠냠")
        var revealed = 0
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            NyummyTheme {
                NyummyTypewriterText(text = line, style = NyummyTheme.typography.voiceM, onRevealed = { revealed++ })
            }
        }
        composeRule.mainClock.advanceTimeBy(NyummyTypewriterCharDelayMillis * 3)
        assertEquals(1, revealed)

        line = "골골골 행복해"
        composeRule.mainClock.advanceTimeBy(NyummyTypewriterCharDelayMillis * 3)
        assertEquals(1, revealed)

        composeRule.mainClock.advanceTimeBy(NyummyTypewriterCharDelayMillis * line.length)
        assertEquals(2, revealed)
        composeRule.onNodeWithText(line).assertIsDisplayed()
    }

    @Test
    fun 건너뛰면_바로_전부_보여주고_따로_알리지_않는다() {
        var revealed = 0
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            NyummyTheme {
                NyummyTypewriterText(
                    text = "바로 보여 줘",
                    style = NyummyTheme.typography.voiceM,
                    revealed = true,
                    onRevealed = { revealed++ },
                )
            }
        }

        composeRule.mainClock.advanceTimeBy(5_000L)
        composeRule.onNodeWithText("바로 보여 줘").assertIsDisplayed()
        assertEquals(0, revealed)
    }
}
