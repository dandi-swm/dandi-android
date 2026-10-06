package com.dandi.nyummy.auth.presentation

import android.content.Context
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dandi.nyummy.auth.domain.EmailLoginFieldError
import com.dandi.nyummy.auth.domain.SignUpFieldError
import com.dandi.nyummy.auth.entity.SocialLoginType
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthScreensTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private fun text(id: Int, vararg args: Any): String = context.getString(id, *args)

    @Test
    fun 로그인_카드의_버튼은_각_로그인_인텐트를_보낸다() {
        val intents = mutableListOf<LoginIntent>()
        composeRule.setContent {
            NyummyTheme { LoginPageContent(uiState = LoginUIState.empty, onIntent = { intents += it }) }
        }

        composeRule.onNodeWithText(text(R.string.auth_login_kakao)).performClick()
        composeRule.onNodeWithContentDescription(text(R.string.auth_login_naver_description)).performClick()
        composeRule.onNodeWithContentDescription(text(R.string.auth_login_google_description)).performClick()
        composeRule.onNodeWithContentDescription(text(R.string.auth_login_email_description)).performClick()

        assertEquals(
            listOf(
                LoginIntent.ClickSocialLogin(SocialLoginType.KAKAO),
                LoginIntent.ClickSocialLogin(SocialLoginType.NAVER),
                LoginIntent.ClickSocialLogin(SocialLoginType.GOOGLE),
                LoginIntent.ClickEmailLogin,
            ),
            intents,
        )
    }

    @Test
    fun 테스트_계정_버튼은_사용할_수_있을_때만_보인다() {
        composeRule.setContent {
            NyummyTheme { LoginPageContent(uiState = LoginUIState.empty, onIntent = {}) }
        }

        composeRule.onNodeWithText(text(R.string.auth_login_test_account)).assertDoesNotExist()
    }

    @Test
    fun 소셜_로그인_확인_중에는_버튼이_막히고_로딩_문구가_보인다() {
        composeRule.setContent {
            NyummyTheme {
                LoginPageContent(
                    uiState = LoginUIState(
                        isLoading = true,
                        verifyingSocialLogin = SocialLoginAttempt(id = 1, socialType = SocialLoginType.KAKAO),
                    ),
                    onIntent = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription(text(R.string.auth_login_email_description)).assertIsNotEnabled()
        val message = text(R.string.auth_login_social_verifying, text(R.string.auth_social_provider_kakao))
        composeRule.onNodeWithText(message).assertIsDisplayed()
    }

    @Test
    fun 이메일_로그인은_입력이_없으면_로그인을_막고_뒤로_가기를_보낸다() {
        val intents = mutableListOf<EmailLoginIntent>()
        composeRule.setContent {
            NyummyTheme { EmailLoginContent(uiState = EmailLoginUIState.empty, onIntent = { intents += it }) }
        }

        composeRule.onNodeWithText(text(R.string.auth_login_button)).assertIsNotEnabled()
        composeRule.onNodeWithContentDescription(BackDescription).performClick()
        composeRule.onNodeWithText(text(R.string.auth_signup_button)).performClick()

        assertEquals(listOf(EmailLoginIntent.ClickBack, EmailLoginIntent.ClickSignUp), intents)
    }

    @Test
    fun 이메일_로그인_중에는_버튼_문구가_바뀌고_오류는_입력칸_아래에_보인다() {
        composeRule.setContent {
            NyummyTheme {
                EmailLoginContent(
                    uiState = EmailLoginUIState(
                        email = "nyummy@cat",
                        password = "password",
                        isLoading = true,
                        emailError = EmailLoginFieldError.EMAIL_FORMAT,
                    ),
                    onIntent = {},
                )
            }
        }

        composeRule.onNodeWithText(text(R.string.auth_login_button_loading)).assertIsNotEnabled()
        composeRule.onNodeWithText(text(R.string.auth_email_error_format)).assertIsDisplayed()
    }

    @Test
    fun 회원가입_첫_단계는_단계_표시와_인증_코드_버튼을_보여준다() {
        val intents = mutableListOf<SignUpIntent>()
        composeRule.setContent {
            NyummyTheme {
                SignUpContent(
                    uiState = SignUpUIState.empty.copy(email = "a@b.co", password = "abcd1234", passwordConfirm = "abcd1234"),
                    onIntent = { intents += it },
                )
            }
        }

        composeRule.onNodeWithText("1 / 3").assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.auth_signup_cta)).assertIsEnabled().performClick()
        composeRule.onNodeWithContentDescription(BackDescription).performClick()

        assertEquals(listOf(SignUpIntent.ClickSendCode, SignUpIntent.ClickBack), intents)
    }

    @Test
    fun 인증_코드_단계는_남은_시간_동안_다시_보내기를_숨긴다() {
        composeRule.setContent {
            NyummyTheme {
                SignUpContent(
                    uiState = SignUpUIState.empty.copy(step = SignUpStep.CODE, email = "a@b.co", resendRemainingSeconds = 272),
                    onIntent = {},
                )
            }
        }

        composeRule.onNodeWithText(text(R.string.auth_signup_code_resend_countdown, "04:32")).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.auth_signup_code_resend)).assertDoesNotExist()
        composeRule.onNodeWithText(text(R.string.auth_signup_code_cta)).assertIsNotEnabled()
    }

    @Test
    fun 인증_코드_단계는_시간이_지나면_다시_보내기를_보낸다() {
        val intents = mutableListOf<SignUpIntent>()
        composeRule.setContent {
            NyummyTheme {
                SignUpContent(
                    uiState = SignUpUIState.empty.copy(step = SignUpStep.CODE, email = "a@b.co", resendRemainingSeconds = 0),
                    onIntent = { intents += it },
                )
            }
        }

        composeRule.onNodeWithText(text(R.string.auth_signup_code_resend)).performClick()

        assertEquals(listOf(SignUpIntent.ClickResendCode), intents)
    }

    @Test
    fun 프로필_단계에서_이름이_비면_오류_카드가_안내_카드를_대신한다() {
        composeRule.setContent {
            NyummyTheme {
                SignUpContent(
                    uiState = SignUpUIState.empty.copy(
                        step = SignUpStep.PROFILE,
                        nicknameError = SignUpFieldError.NICKNAME_EMPTY,
                    ),
                    onIntent = {},
                )
            }
        }

        composeRule.onNodeWithText(text(R.string.auth_signup_error_nickname_empty_title))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.auth_signup_privacy_notice_title)).assertDoesNotExist()
        composeRule.onNode(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion),
            useUnmergedTree = false,
        ).assertExists()
    }

    @Test
    fun 소셜_가입은_단계_표시_없이_프로필만_보인다() {
        composeRule.setContent {
            NyummyTheme {
                SignUpContent(
                    uiState = SignUpUIState.initial(isSocialSignUp = true),
                    onIntent = {},
                )
            }
        }

        composeRule.onNodeWithText(text(R.string.auth_signup_profile_title)).assertIsDisplayed()
        composeRule.onNodeWithText("3 / 3").assertDoesNotExist()
    }

    private companion object {
        const val BackDescription = "뒤로 가기"
    }
}
