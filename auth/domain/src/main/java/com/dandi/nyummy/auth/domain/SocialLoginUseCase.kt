package com.dandi.nyummy.auth.domain

import com.dandi.nyummy.auth.entity.SocialCredentialVO
import com.dandi.nyummy.auth.entity.SocialLoginType
import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.tti.TTIHelper
import java.io.IOException
import java.util.concurrent.CancellationException
import javax.inject.Inject

/**
 * 소셜 로그인. 제공자 SDK 로 얻은 자격 증명을 서버에서 검증하고 결과에 따라 이동한다.
 *
 * 실패 안내는 상태 코드로만 구분한다. 공통 처리([executeCommonErrorHanding])를 쓰면 401 이
 * "세션 만료"로, 404 가 루트 화면에서의 뒤로가기로 처리되고, 이메일 로그인용 도메인 문구
 * (이메일·비밀번호 확인)가 소셜 로그인에 뜰 수 있어서다.
 */
class SocialLoginUseCase @Inject constructor(
    private val repository: AuthRepository,
    private val socialSignUpSession: SocialSignUpSession,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {

    /**
     * 기존 회원은 redirectUrl 이 가리키는 화면(온보딩 또는 홈)으로, 신규 회원은 검증 완료 토큰을 [SocialSignUpSession] 에 보관한 뒤
     * 프로필 입력 화면([SocialSignUpPage])으로 이동한다. 실패는 안내 후 [Result.failure] 로 돌려준다.
     */
    suspend fun login(credential: SocialCredentialVO): Result<Unit> {
        // 이전 시도의 가입 대기 토큰이 다른 계정의 가입에 섞이지 않도록 먼저 비운다.
        socialSignUpSession.clear()
        return try {
            val result = repository.socialLogin(credential)
            when {
                result.isLoggedIn -> navigationHelper.navigateToAsRoot(PostLoginDestination.from(result.token.redirectUrl))
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
            showError(NETWORK_ERROR_MESSAGE)
            Result.failure(e)
        } catch (e: Exception) {
            showError(TEMPORARY_ERROR_MESSAGE)
            Result.failure(e)
        }
    }

    /** 제공자 SDK 로그인 자체가 실패해(토큰을 받지 못함) 서버 검증까지 가지 못했을 때 안내한다. */
    fun onProviderFailed(type: SocialLoginType) {
        showError(providerFailedMessage(type))
    }

    /** 아직 연동되지 않았거나 앱 키 없이 빌드돼 쓸 수 없는 제공자를 눌렀을 때 안내한다. */
    fun onProviderUnavailable(type: SocialLoginType) {
        messageHelper.showSnackBar(
            iconType = IconType.WARNING,
            messageText = "${type.displayName} 로그인은 준비 중이에요.",
        )
    }

    private fun httpErrorMessage(e: HttpResponseException, type: SocialLoginType): String =
        when (e.rawCode) {
            400 -> BAD_REQUEST_MESSAGE
            401 -> providerFailedMessage(type)
            503 -> "${type.displayName} 서버에 연결할 수 없어요. 잠시 후 다시 시도해주세요."
            else -> TEMPORARY_ERROR_MESSAGE
        }

    private fun providerFailedMessage(type: SocialLoginType): String =
        "${type.displayName} 로그인에 실패했어요. 다시 시도해주세요."

    private fun showError(message: String) {
        messageHelper.showOneButtonDialog(descText = message, buttonText = CONFIRM_BUTTON_TEXT)
    }

    private companion object {
        const val BAD_REQUEST_MESSAGE = "로그인 요청이 올바르지 않아요. 앱을 최신 버전으로 업데이트한 뒤 다시 시도해주세요."
        const val NETWORK_ERROR_MESSAGE = "네트워크 연결을 확인한 뒤 다시 시도해주세요."
        const val TEMPORARY_ERROR_MESSAGE = "일시적인 오류가 발생했어요. 잠시 후 다시 시도해주세요."
        const val CONFIRM_BUTTON_TEXT = "확인"
    }
}

/** 사용자 안내 문구에 쓰는 제공자 이름. */
internal val SocialLoginType.displayName: String
    get() = when (this) {
        SocialLoginType.KAKAO -> "카카오"
        SocialLoginType.GOOGLE -> "구글"
        SocialLoginType.NAVER -> "네이버"
    }
