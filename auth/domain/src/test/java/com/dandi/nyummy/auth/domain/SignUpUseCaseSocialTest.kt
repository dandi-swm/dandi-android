package com.dandi.nyummy.auth.domain

import com.dandi.nyummy.auth.entity.Gender
import com.dandi.nyummy.home.domain.HomePage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class SignUpUseCaseSocialTest {

    private val repository = FakeAuthRepository()
    private val session = SocialSignUpSession()
    private val navigationHelper = RecordingNavigationHelper()
    private val messageHelper = RecordingMessageHelper()
    private val useCase = SignUpUseCase(
        repository = repository,
        socialSignUpSession = session,
        resourceHelper = FakeResourceHelper(),
        messageHelper = messageHelper,
        navigationHelper = navigationHelper,
        ttiHelper = FakeTTIHelper(),
    )

    @Test
    fun `소셜 가입은 세션 토큰으로 비밀번호 없이 요청하고 성공 시 세션을 비우고 홈으로 간다`() = runBlocking {
        session.start("social-verified")

        val result = signUpWithSocial()

        assertTrue(result.isSuccess)
        val call = repository.signUpCalls.single()
        assertEquals("social-verified", call.verifiedToken)
        assertNull(call.password)
        assertNull(call.confirmPassword)
        assertEquals("단디", call.nickname)
        assertNull(session.pendingToken)
        assertEquals(listOf<Any>(HomePage), navigationHelper.rootPages)
    }

    @Test
    fun `세션 토큰이 없으면 서버를 부르지 않고 안내 후 이전 화면으로 돌려보낸다`() = runBlocking {
        val result = signUpWithSocial()

        assertTrue(result.isFailure)
        assertTrue(repository.signUpCalls.isEmpty())
        val dialog = messageHelper.dialogs.single()
        assertTrue(dialog.cantIgnore)
        dialog.onClickButton?.invoke()
        assertEquals(1, navigationHelper.backCount)
        assertEquals(0, navigationHelper.initialCount)
    }

    @Test
    fun `가입 토큰이 만료되면(401) 세션을 비우고 다시 로그인하도록 초기 화면으로 보낸다`() = runBlocking {
        session.start("social-verified")
        repository.signUpError = httpException(401)

        signUpWithSocial()

        assertNull(session.pendingToken)
        val dialog = messageHelper.dialogs.single()
        assertEquals("가입 유효 시간이 지났어요. 다시 로그인해주세요.", dialog.descText)
        dialog.onClickButton?.invoke()
        assertEquals(1, navigationHelper.initialCount)
    }

    @Test
    fun `이미 가입된 소셜 계정이면(409) 세션을 비우고 다시 로그인하도록 안내한다`() = runBlocking {
        session.start("social-verified")
        repository.signUpError = httpException(409)

        signUpWithSocial()

        assertNull(session.pendingToken)
        assertEquals("이미 가입된 계정이에요. 다시 로그인해주세요.", messageHelper.dialogs.single().descText)
    }

    @Test
    fun `입력값 오류나 네트워크 오류는 세션을 남겨 다시 제출할 수 있게 한다`() = runBlocking {
        session.start("social-verified")
        repository.signUpError = httpException(400)
        signUpWithSocial()
        repository.signUpError = IOException("offline")
        signUpWithSocial()

        assertEquals("social-verified", session.pendingToken)
        assertEquals(
            listOf("입력한 정보를 확인한 뒤 다시 시도해주세요.", "네트워크 연결을 확인한 뒤 다시 시도해주세요."),
            messageHelper.dialogs.map { it.descText },
        )
        assertTrue(navigationHelper.rootPages.isEmpty())
    }

    @Test
    fun `가입 대기 여부와 포기는 세션 상태를 따른다`() {
        assertFalse(useCase.hasPendingSocialSignUp())
        session.start("social-verified")
        assertTrue(useCase.hasPendingSocialSignUp())

        useCase.abandonSocialSignUp()

        assertFalse(useCase.hasPendingSocialSignUp())
    }

    @Test
    fun `이메일 가입은 이메일 인증 토큰과 비밀번호를 그대로 보낸다`() = runBlocking {
        session.start("social-verified")

        useCase.signUp(
            emailVerifiedToken = "email-verified",
            password = "pw1234",
            confirmPassword = "pw1234",
            nickname = "단디",
        )

        val call = repository.signUpCalls.single()
        assertEquals("email-verified", call.verifiedToken)
        assertEquals("pw1234", call.password)
        // 이메일 가입은 소셜 세션을 건드리지 않는다.
        assertEquals("social-verified", session.pendingToken)
    }

    private suspend fun signUpWithSocial() = useCase.signUpWithSocial(
        nickname = "단디",
        gender = Gender.FEMALE,
        birth = "2000-01-15",
        height = 160,
        weight = 50,
    )
}
