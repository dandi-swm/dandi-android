package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NyummyComingSoonPageTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun 탭_이름을_헤딩으로_알리고_준비_중_문구를_보여준다() {
        composeRule.setContent {
            NyummyTheme { NyummyComingSoonPage(title = "퀘스트", message = "퀘스트를 준비하고 있어요") }
        }

        composeRule.onNodeWithText("퀘스트").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        composeRule.onNodeWithText("퀘스트를 준비하고 있어요").assertIsDisplayed()
        composeRule.onNodeWithText("조금만 기다려 주세요").assertIsDisplayed()
    }
}
