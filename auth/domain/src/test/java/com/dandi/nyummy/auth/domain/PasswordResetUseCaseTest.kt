package com.dandi.nyummy.auth.domain

import com.dandi.nyummy.auth.entity.EmailVerificationPurpose
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class PasswordResetUseCaseTest {

    private val repository = FakeAuthRepository()
    private val navigationHelper = RecordingNavigationHelper()
    private val messageHelper = RecordingMessageHelper()
    private val useCase = PasswordResetUseCase(
        repository = repository,
        resourceHelper = FakeResourceHelper(),
        messageHelper = messageHelper,
        navigationHelper = navigationHelper,
        ttiHelper = FakeTTIHelper(),
    )
    private val emailVerificationUseCase = EmailVerificationUseCase(
        repository = repository,
        resourceHelper = FakeResourceHelper(),
        messageHelper = messageHelper,
        navigationHelper = navigationHelper,
        ttiHelper = FakeTTIHelper(),
    )

    @Test
    fun `인증 완료 토큰으로 재설정을 부르고 성공을 돌려준다`() = runBlocking {
        val result = useCase.resetPassword("verified")

        assertTrue(result.isSuccess)
        assertEquals(listOf("verified"), repository.resetPasswordCalls)
        assertTrue(messageHelper.dialogs.isEmpty())
    }

    @Test
    fun `인증이 만료됐거나 무효하면 안내 후 이메일 입력부터 다시 하게 한다`() = runBlocking {
        listOf(AuthErrorType.VERIFICATION_EXPIRED, AuthErrorType.INVALID_VERIFIED_TOKEN).forEach { errorType ->
            repository.resetPasswordError = httpException(401, errorType.type)

            val result = useCase.resetPassword("verified")

            assertTrue(result.exceptionOrNull() is EmailVerificationRestartRequiredException)
        }
        assertEquals(
            listOf(AuthErrorType.VERIFICATION_EXPIRED.errorMsg, AuthErrorType.INVALID_VERIFIED_TOKEN.errorMsg),
            messageHelper.dialogs.map { it.descText },
        )
        assertEquals(0, navigationHelper.initialCount)
    }

    @Test
    fun `가입되지 않은 이메일 404 는 공통 준비 중 안내 대신 code 문구로 알리고 이메일 단계로 돌려보낸다`() = runBlocking {
        repository.resetPasswordError = httpException(404, AuthErrorType.EMAIL_NOT_FOUND.type)

        val result = useCase.resetPassword("verified")

        assertTrue(result.exceptionOrNull() is EmailVerificationRestartRequiredException)
        assertEquals("가입되지 않은 이메일이에요.", messageHelper.dialogs.single().descText)
        assertEquals(0, navigationHelper.backCount)
    }

    @Test
    fun `임시 비밀번호 메일 발송 실패는 따로 안내하고 같은 토큰으로 다시 시도할 수 있게 둔다`() = runBlocking {
        repository.resetPasswordError = httpException(500, AuthErrorType.EMAIL_SEND_FAILED.type)

        val result = useCase.resetPassword("verified")

        assertFalse(result.exceptionOrNull() is EmailVerificationRestartRequiredException)
        assertEquals("임시 비밀번호 메일을 보내지 못했어요. 잠시 후 다시 시도해 주세요.", messageHelper.dialogs.single().descText)
    }

    @Test
    fun `code 없는 400 은 재설정 실패로 안내한다`() = runBlocking {
        repository.resetPasswordError = httpException(400)

        useCase.resetPassword("verified")

        assertEquals("비밀번호를 재설정하지 못했어요. 잠시 후 다시 시도해 주세요.", messageHelper.dialogs.single().descText)
    }

    @Test
    fun `네트워크 오류면 연결 확인을 안내한다`() = runBlocking {
        repository.resetPasswordError = IOException("offline")

        val result = useCase.resetPassword("verified")

        assertTrue(result.isFailure)
        assertEquals("네트워크 연결을 확인한 뒤 다시 시도해주세요.", messageHelper.dialogs.single().descText)
    }

    @Test
    fun `비밀번호 찾기 코드 요청에서 가입되지 않은 이메일 404 는 공통 준비 중 안내로 가지 않는다`() = runBlocking {
        repository.requestEmailVerificationError = httpException(404, AuthErrorType.EMAIL_NOT_FOUND.type)

        val result = emailVerificationUseCase.sendCode("nobody@nyummy.com", EmailVerificationPurpose.RESET_PASSWORD)

        assertTrue(result.isFailure)
        assertEquals("가입되지 않은 이메일이에요.", messageHelper.dialogs.single().descText)
        assertEquals(0, navigationHelper.backCount)
    }
}
