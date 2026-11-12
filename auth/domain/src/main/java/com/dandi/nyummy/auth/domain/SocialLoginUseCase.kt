package com.dandi.nyummy.auth.domain

import com.dandi.nyummy.auth.entity.SocialCredentialVO
import com.dandi.nyummy.auth.entity.SocialLoginType
import com.dandi.nyummy.common.domain.analytics.AnalyticsEvent
import com.dandi.nyummy.common.domain.analytics.AnalyticsHelper
import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.error.handlingErrorOnUseCase
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.helper.StringResource
import com.dandi.nyummy.common.domain.helper.WidgetHelper
import com.dandi.nyummy.tti.TTIHelper
import java.io.IOException
import java.util.concurrent.CancellationException
import javax.inject.Inject

/**
 * 소셜 로그인. 제공자 SDK 로 얻은 자격 증명을 서버에서 검증하고 결과에 따라 이동한다.
 *
 * 실패 안내는 서버 code로 먼저 구분하고, code가 없으면 상태 코드로 구분한다. 공통 처리([executeCommonErrorHanding])를 쓰면 401 이
 * "세션 만료"로, 404 가 루트 화면에서의 뒤로가기로 처리되고, 이메일 로그인용 도메인 문구
 * (이메일·비밀번호 확인)가 소셜 로그인에 뜰 수 있어서다.
 */
class SocialLoginUseCase @Inject constructor(
    private val repository: AuthRepository,
    private val socialSignUpSession: SocialSignUpSession,
    private val analyticsHelper: AnalyticsHelper,
    private val widgetHelper: WidgetHelper,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {

    /**
     * 기존 회원은 기기 홈 화면 위젯에 남은 앞 계정의 기록을 지우고 redirectUrl 이 가리키는 화면(온보딩 또는 홈)으로,
     * 신규 회원은 검증 완료 토큰을 [SocialSignUpSession] 에 보관한 뒤
     * 프로필 입력 화면([SocialSignUpPage])으로 이동한다. 실패는 안내 후 [Result.failure] 로 돌려준다.
     */
    suspend fun login(credential: SocialCredentialVO): Result<Unit> {
        // 이전 시도의 가입 대기 토큰이 다른 계정의 가입에 섞이지 않도록 먼저 비운다.
        socialSignUpSession.clear()
        return try {
            val result = repository.socialLogin(credential)
            when {
                result.isLoggedIn -> {
                    repository.setOnboardingIncomplete(result.token.redirectUrl.isOnboardingRedirect())
                    widgetHelper.clear()
                    analyticsHelper.logEvent(AnalyticsEvent.Login(method = credential.type.name.lowercase()))
                    navigationHelper.openAfterLogin(result.token.redirectUrl)
                }
                result.isSignUpRequired -> {
                    socialSignUpSession.start(result.verifiedToken)
                    navigationHelper.navigateTo(SocialSignUpPage)
                }
                // 토큰 없이 200 이 오면 홈으로 보내도 인증이 안 된 상태라 실패로 처리한다.
                else -> throw IllegalStateException("Social login response has no token")
            }
            Result.success(Unit)
        } catch (e: HttpResponseException) {
            showError(httpErrorMessage(e, credential.type))
            Result.failure(e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            showError(resourceHelper.getString(StringResource.NETWORK_ERROR))
            Result.failure(e)
        } catch (e: Exception) {
            showError(resourceHelper.getString(StringResource.TEMPORARY_ERROR))
            Result.failure(e)
        }
    }

    /** 제공자 SDK 로그인 자체가 실패해(토큰을 받지 못함) 서버 검증까지 가지 못했을 때 안내한다. */
    fun onProviderFailed(type: SocialLoginType) {
        showError(providerFailedMessage(type))
    }

    /** 아직 연동되지 않았거나 앱 키 없이 빌드돼 쓸 수 없는 제공자를 눌렀을 때 안내한다. */
    fun onProviderUnavailable(type: SocialLoginType) {
        messageHelper.showToast(resourceHelper.getString(StringResource.AUTH_PROVIDER_NOT_READY, displayName(type)))
    }

    /** 서버 code로 먼저 구분하고, code가 없을 때만 상태 코드로 판단한다. 문구에 제공자 이름을 넣는다. */
    private fun httpErrorMessage(e: HttpResponseException, type: SocialLoginType): String =
        when (e.handlingErrorOnUseCase<AuthErrorType>()) {
            AuthErrorType.INVALID_OAUTH_TOKEN -> providerFailedMessage(type)
            AuthErrorType.OAUTH_PROVIDER_UNAVAILABLE -> providerUnavailableMessage(type)
            else -> when (e.rawCode) {
                400 -> resourceHelper.getString(StringResource.AUTH_BAD_REQUEST)
                401 -> providerFailedMessage(type)
                503 -> providerUnavailableMessage(type)
                else -> resourceHelper.getString(StringResource.TEMPORARY_ERROR)
            }
        }

    private fun providerUnavailableMessage(type: SocialLoginType): String =
        resourceHelper.getString(StringResource.AUTH_PROVIDER_UNAVAILABLE, displayName(type))

    private fun providerFailedMessage(type: SocialLoginType): String =
        resourceHelper.getString(StringResource.AUTH_PROVIDER_FAILED, displayName(type))

    /** 사용자 안내 문구에 쓰는 제공자 이름. */
    private fun displayName(type: SocialLoginType): String = resourceHelper.getString(
        when (type) {
            SocialLoginType.KAKAO -> StringResource.AUTH_PROVIDER_KAKAO
            SocialLoginType.GOOGLE -> StringResource.AUTH_PROVIDER_GOOGLE
            SocialLoginType.NAVER -> StringResource.AUTH_PROVIDER_NAVER
        },
    )

    private fun showError(message: String) {
        messageHelper.showOneButtonDialog(descText = message)
    }
}
