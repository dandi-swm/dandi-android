package com.dandi.nyummy.intro.presentation

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IntroScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun 스플래시는_로고와_진행_문구를_보여주고_완료되면_완료_문구로_바뀐다() {
        var complete by mutableStateOf(false)
        composeRule.setContent {
            NyummyTheme { IntroSplashContent(isComplete = complete, isPermissionNoticeVisible = false) }
        }

        composeRule.onNodeWithContentDescription(context.getString(R.string.intro_splash_logo_description))
            .assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.intro_splash_progress_label)).assertIsDisplayed()

        complete = true
        composeRule.onNodeWithText(context.getString(R.string.intro_splash_progress_complete)).assertIsDisplayed()
    }

    @Test
    fun 권한_안내는_보일_때만_나타나고_확인을_누르면_콜백을_호출한다() {
        var visible by mutableStateOf(false)
        var confirmed = 0
        composeRule.setContent {
            NyummyTheme {
                Box { PermissionNoticeBottomSheet(visible = visible, onConfirm = { confirmed++ }) }
            }
        }
        val title = context.getString(R.string.intro_permission_title)

        composeRule.onNodeWithText(title).assertDoesNotExist()

        visible = true
        composeRule.onNodeWithText(title).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.intro_permission_camera)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.intro_permission_notification)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.intro_permission_confirm)).performClick()

        assertEquals(1, confirmed)
    }
}
