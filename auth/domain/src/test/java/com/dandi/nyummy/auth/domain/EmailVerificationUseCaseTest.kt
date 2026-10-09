package com.dandi.nyummy.auth.domain

import com.dandi.nyummy.auth.entity.EmailVerificationPurpose
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class EmailVerificationUseCaseTest {

    private val repository = FakeAuthRepository()
    private val navigationHelper = RecordingNavigationHelper()
    private val messageHelper = RecordingMessageHelper()
    private val useCase = EmailVerificationUseCase(
        repository = repository,
        resourceHelper = FakeResourceHelper(),
        messageHelper = messageHelper,
        navigationHelper = navigationHelper,
        ttiHelper = FakeTTIHelper(),
    )

    private suspend fun sendCode() = useCase.sendCode("test@dandi.app", EmailVerificationPurpose.SIGNUP)

    @Test
    fun `메일 발송 실패 500 은 공통 오류 대신 메일을 못 보냈다고 안내한다`() = runBlocking {
        repository.requestEmailVerificationError = httpException(500, AuthErrorType.EMAIL_SEND_FAILED.type)

        val result = sendCode()

        assertTrue(result.isFailure)
        assertEquals("인증 메일을 보내지 못했어요. 잠시 후 다시 시도해 주세요.", messageHelper.dialogs.single().descText)
    }

    @Test
    fun `이전 SES 발송 실패 code 도 같은 문구로 안내한다`() = runBlocking {
        repository.requestEmailVerificationError = httpException(500, "api.ses.emailSendFailed")

        sendCode()

        assertEquals(AuthErrorType.EMAIL_SEND_FAILED.errorMsg, messageHelper.dialogs.single().descText)
    }

    @Test
    fun `이미 가입된 이메일이면 code 에 맞는 문구로 안내한다`() = runBlocking {
        repository.requestEmailVerificationError = httpException(409, "api.auth.emailAlreadyExists")

        sendCode()

        assertEquals("이미 가입된 이메일이에요. 로그인해 주세요.", messageHelper.dialogs.single().descText)
    }

    @Test
    fun `code 없는 500 은 공통 오류로 안내한다`() = runBlocking {
        repository.requestEmailVerificationError = httpException(500)

        sendCode()

        assertEquals("잠시 후 다시 시도해 주세요.", messageHelper.dialogs.single().descText)
    }

    @Test
    fun `발송 중 네트워크 오류면 앱이 죽지 않고 연결 확인을 안내한다`() = runBlocking {
        repository.requestEmailVerificationError = IOException("offline")

        val result = sendCode()

        assertTrue(result.isFailure)
        assertEquals("네트워크 연결을 확인한 뒤 다시 시도해주세요.", messageHelper.dialogs.single().descText)
    }

    @Test
    fun `인증 코드가 만료된 401 은 세션 만료가 아니라 입력란 아래 문구로 돌려준다`() = runBlocking {
        repository.confirmEmailVerificationError = httpException(401, AuthErrorType.EMAIL_CODE_EXPIRED.type)

        val result = useCase.confirmCode(authCode = "123456", emailChallengeToken = "challenge")

        val error = result.exceptionOrNull()
        assertTrue(error is CodeVerificationFailedException)
        assertEquals(AuthErrorType.EMAIL_CODE_EXPIRED.errorMsg, error?.message)
        assertTrue(messageHelper.dialogs.isEmpty())
        assertEquals(0, navigationHelper.initialCount)
    }

    @Test
    fun `인증 시도 횟수를 넘기면 입력란 아래 문구로 돌려준다`() = runBlocking {
        repository.confirmEmailVerificationError = httpException(429, "api.auth.emailCodeAttemptExceeded")

        val result = useCase.confirmCode(authCode = "123456", emailChallengeToken = "challenge")

        assertEquals("인증 코드를 여러 번 틀렸어요. 코드를 다시 받아 주세요.", result.exceptionOrNull()?.message)
    }

    @Test
    fun `챌린지 토큰이 무효한 401 은 로그인 만료가 아니라 코드를 다시 받으라고 입력란 아래에 돌려준다`() = runBlocking {
        repository.confirmEmailVerificationError =
            httpException(401, AuthErrorType.INVALID_EMAIL_CHALLENGE_TOKEN.type)

        val result = useCase.confirmCode(authCode = "123456", emailChallengeToken = "challenge")

        val error = result.exceptionOrNull()
        assertTrue(error is CodeVerificationFailedException)
        assertEquals("인증 요청이 올바르지 않아요. 인증 코드를 다시 받아 주세요.", error?.message)
        assertTrue(messageHelper.dialogs.isEmpty())
        assertEquals(0, navigationHelper.initialCount)
    }

    @Test
    fun `모르는 code 가 붙은 401 도 로그인 만료로 보내지 않고 입력란 아래에 돌려준다`() = runBlocking {
        repository.confirmEmailVerificationError = httpException(401, "api.auth.somethingNew")

        val result = useCase.confirmCode(authCode = "123456", emailChallengeToken = "challenge")

        assertTrue(result.exceptionOrNull() is CodeVerificationFailedException)
        assertTrue(messageHelper.dialogs.isEmpty())
        assertEquals(0, navigationHelper.initialCount)
    }
}
