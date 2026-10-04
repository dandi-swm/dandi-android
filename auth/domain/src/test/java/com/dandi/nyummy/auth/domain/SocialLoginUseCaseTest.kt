package com.dandi.nyummy.auth.domain

import com.dandi.nyummy.auth.entity.AuthTokenVO
import com.dandi.nyummy.auth.entity.SocialCredentialVO
import com.dandi.nyummy.auth.entity.SocialLoginType
import com.dandi.nyummy.auth.entity.SocialLoginVO
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.home.domain.HomePage
import com.dandi.nyummy.onboarding.domain.OnboardingPage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.concurrent.CancellationException

class SocialLoginUseCaseTest {

    private val repository = FakeAuthRepository()
    private val session = SocialSignUpSession()
    private val navigationHelper = RecordingNavigationHelper()
    private val messageHelper = RecordingMessageHelper()
    private val useCase = SocialLoginUseCase(
        repository = repository,
        socialSignUpSession = session,
        resourceHelper = FakeResourceHelper(),
        messageHelper = messageHelper,
        navigationHelper = navigationHelper,
        ttiHelper = FakeTTIHelper(),
    )

    private val credential = SocialCredentialVO(SocialLoginType.KAKAO, token = "id-token", nonce = "nonce-1")

    @Test
    fun `기존 회원이면 홈을 루트로 이동하고 가입 세션을 남기지 않는다`() = runBlocking {
        repository.socialLoginResult = SocialLoginVO(token = AuthTokenVO("access", "refresh"))

        val result = useCase.login(credential)

        assertTrue(result.isSuccess)
        assertEquals(listOf(credential), repository.socialLoginCalls)
        assertEquals(listOf<Any>(HomePage), navigationHelper.rootPages)
        assertTrue(navigationHelper.pages.isEmpty())
        assertNull(session.pendingToken)
        assertTrue(messageHelper.dialogs.isEmpty())
    }

    @Test
    fun `기존 회원이라도 redirectUrl 이 온보딩이면 온보딩을 루트로 이동한다`() = runBlocking {
        repository.socialLoginResult = SocialLoginVO(
            token = AuthTokenVO("access", "refresh", redirectUrl = "/onboarding"),
        )

        useCase.login(credential)

        assertEquals(listOf<Any>(OnboardingPage), navigationHelper.rootPages)
    }

    @Test
    fun `신규 회원이면 검증 완료 토큰을 세션에 보관하고 소셜 가입 화면으로 이동한다`() = runBlocking {
        repository.socialLoginResult = SocialLoginVO(verifiedToken = "social-verified")

        val result = useCase.login(credential)

        assertTrue(result.isSuccess)
        assertEquals("social-verified", session.pendingToken)
        assertEquals(listOf<Any>(SocialSignUpPage), navigationHelper.pages)
        assertTrue(navigationHelper.rootPages.isEmpty())
    }

    @Test
    fun `새 로그인을 시작하면 이전 시도의 가입 대기 토큰을 먼저 버린다`() = runBlocking {
        session.start("stale-token")
        repository.socialLoginResult = SocialLoginVO(token = AuthTokenVO("access", "refresh"))

        useCase.login(credential)

        assertNull(session.pendingToken)
    }

    @Test
    fun `토큰도 검증 완료 토큰도 없는 응답이면 홈으로 보내지 않고 실패를 안내한다`() = runBlocking {
        repository.socialLoginResult = SocialLoginVO.empty

        val result = useCase.login(credential)

        assertTrue(result.isFailure)
        assertTrue(navigationHelper.rootPages.isEmpty())
        assertTrue(navigationHelper.pages.isEmpty())
        assertEquals("일시적인 오류가 발생했어요. 잠시 후 다시 시도해주세요.", messageHelper.dialogs.single().descText)
    }

    @Test
    fun `401 이면 세션 만료가 아니라 카카오 로그인 실패로 안내하고 로그인 화면에 머문다`() = runBlocking {
        repository.socialLoginError = httpException(401, "api.auth.invalidCredentials")

        val result = useCase.login(credential)

        assertTrue(result.isFailure)
        val dialog = messageHelper.dialogs.single()
        assertEquals("카카오 로그인에 실패했어요. 다시 시도해주세요.", dialog.descText)
        dialog.onClickButton?.invoke()
        assertEquals(0, navigationHelper.initialCount)
        assertEquals(0, navigationHelper.backCount)
    }

    @Test
    fun `503 이면 제공자 서버 연결 불가를 안내한다`() = runBlocking {
        repository.socialLoginError = httpException(503)

        useCase.login(credential)

        assertEquals("카카오 서버에 연결할 수 없어요. 잠시 후 다시 시도해주세요.", messageHelper.dialogs.single().descText)
    }

    @Test
    fun `404 여도 공통 처리처럼 뒤로 보내지 않고 일시 오류로 안내한다`() = runBlocking {
        repository.socialLoginError = httpException(404)

        useCase.login(credential)

        val dialog = messageHelper.dialogs.single()
        assertEquals("일시적인 오류가 발생했어요. 잠시 후 다시 시도해주세요.", dialog.descText)
        dialog.onClickButton?.invoke()
        assertEquals(0, navigationHelper.backCount)
    }

    @Test
    fun `네트워크 오류면 크래시 없이 연결 확인을 안내한다`() = runBlocking {
        repository.socialLoginError = IOException("timeout")

        val result = useCase.login(credential)

        assertTrue(result.isFailure)
        assertEquals("네트워크 연결을 확인한 뒤 다시 시도해주세요.", messageHelper.dialogs.single().descText)
    }

    @Test
    fun `코루틴 취소는 삼키지 않고 다시 던진다`() {
        repository.socialLoginError = CancellationException("cancelled")

        assertThrows(CancellationException::class.java) {
            runBlocking { useCase.login(credential) }
        }
        assertTrue(messageHelper.dialogs.isEmpty())
    }

    @Test
    fun `SDK 로그인 실패와 미연동 제공자는 각각 다이얼로그와 경고 스낵바로 안내한다`() {
        useCase.onProviderFailed(SocialLoginType.KAKAO)
        useCase.onProviderUnavailable(SocialLoginType.NAVER)

        assertEquals("카카오 로그인에 실패했어요. 다시 시도해주세요.", messageHelper.dialogs.single().descText)
        val snackBar = messageHelper.snackBars.single()
        assertEquals(IconType.WARNING, snackBar.iconType)
        assertEquals("네이버 로그인은 준비 중이에요.", snackBar.messageText)
    }
}
