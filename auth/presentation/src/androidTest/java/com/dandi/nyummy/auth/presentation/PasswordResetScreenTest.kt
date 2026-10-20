package com.dandi.nyummy.auth.presentation

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PasswordResetScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private fun text(id: Int, vararg args: Any): String = context.getString(id, *args)

    @Test
    fun 이메일을_입력해야_인증_코드_받기를_누를_수_있다() {
        val intents = mutableListOf<PasswordResetIntent>()
        var uiState by mutableStateOf(PasswordResetUIState.empty)
        composeRule.setContent {
            NyummyTheme { PasswordResetContent(uiState = uiState, onIntent = { intents += it }) }
        }
        composeRule.onNodeWithText(text(R.string.auth_password_reset_cta)).assertIsNotEnabled()

        uiState = PasswordResetUIState(email = "nyummy@cat.com")
        composeRule.onNodeWithText(text(R.string.auth_password_reset_cta)).assertIsEnabled().performClick()

        assertEquals(listOf<PasswordResetIntent>(PasswordResetIntent.ClickSendCode), intents)
    }

    @Test
    fun 코드_단계는_보낸_주소를_보여_주고_6자리를_채워야_인증할_수_있다() {
        composeRule.setContent {
            NyummyTheme {
                PasswordResetContent(
                    uiState = PasswordResetUIState(step = PasswordResetStep.CODE, email = "nyummy@cat.com", code = "482"),
                    onIntent = {},
                )
            }
        }

        composeRule.onNodeWithText(text(R.string.auth_signup_code_subtitle, "nyummy@cat.com")).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.auth_signup_code_cta)).assertIsNotEnabled()
    }

    @Test
    fun 완료_단계는_임시_비밀번호를_보낸_주소를_알리고_로그인하기를_보낸다() {
        val intents = mutableListOf<PasswordResetIntent>()
        composeRule.setContent {
            NyummyTheme {
                PasswordResetContent(
                    uiState = PasswordResetUIState(step = PasswordResetStep.DONE, email = "nyummy@cat.com"),
                    onIntent = { intents += it },
                )
            }
        }

        composeRule.onNodeWithText(text(R.string.auth_password_reset_done_subtitle, "nyummy@cat.com")).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.auth_password_reset_done_cta)).performClick()

        assertEquals(listOf<PasswordResetIntent>(PasswordResetIntent.ClickLogin), intents)
    }
}
