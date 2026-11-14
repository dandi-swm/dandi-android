package com.dandi.nyummy.auth.presentation

import com.dandi.nyummy.auth.domain.EmailVerificationUseCase
import com.dandi.nyummy.auth.domain.SignUpFieldError
import com.dandi.nyummy.auth.domain.SignUpUseCase
import com.dandi.nyummy.auth.domain.SocialSignUpSession
import com.dandi.nyummy.common.entity.time.DeviceTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/** 회원가입 프로필 단계의 입력 제한(집사 이름 8자, 미래 생년월일 막기). */
@OptIn(ExperimentalCoroutinesApi::class)
class SignUpProfileInputTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository = FakeAuthRepository()
    private val navigationHelper = RecordingNavigationHelper()
    private val messageHelper = RecordingMessageHelper()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `집사 이름이 8자를 넘으면 가입하지 않고 글자 수 오류를 보여 준다`() {
        val viewModel = createViewModel()
        viewModel.onIntent(SignUpIntent.InputNickname("가나다라마바사아자"))

        viewModel.onIntent(SignUpIntent.ClickSubmit)

        assertEquals(SignUpFieldError.NICKNAME_TOO_LONG, viewModel.uiState.value.nicknameError)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `생년월일은 오늘 뒤로 고르면 오늘로 맞춘다`() {
        val today = DeviceTime.today()
        val viewModel = createViewModel()

        viewModel.onIntent(SignUpIntent.SelectBirthYear(today.year))
        viewModel.onIntent(SignUpIntent.SelectBirthMonth(12))
        viewModel.onIntent(SignUpIntent.SelectBirthDay(31))

        val state = viewModel.uiState.value
        val chosen = LocalDate.of(state.birthYear, state.birthMonth, state.birthDayOfMonth)
        val picked = LocalDate.of(today.year, 12, 31)
        assertEquals(if (picked.isAfter(today)) today else picked, chosen)
    }

    private fun createViewModel() = SignUpViewModel(
        isSocialSignUp = true,
        emailVerificationUseCase = EmailVerificationUseCase(repository, FakeResourceHelper, messageHelper, navigationHelper, FakeTTIHelper),
        signUpUseCase = SignUpUseCase(
            repository,
            SocialSignUpSession(),
            RecordingAnalyticsHelper(),
            NoOpWidgetHelper,
            FakeResourceHelper,
            messageHelper,
            navigationHelper,
            FakeTTIHelper,
        ),
        messageHelper = messageHelper,
        strings = { id, _ -> "string:$id" },
        navigationHelper = navigationHelper,
        analyticsHelper = RecordingAnalyticsHelper(),
    )
}
