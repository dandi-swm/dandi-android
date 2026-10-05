package com.dandi.nyummy.auth.presentation.social

import android.app.Activity
import android.content.Context
import android.util.Log
import com.dandi.nyummy.auth.entity.SocialCredentialVO
import com.dandi.nyummy.auth.entity.SocialLoginType
import com.kakao.sdk.auth.TokenManagerProvider
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.KakaoSdk
import com.kakao.sdk.common.model.AuthError
import com.kakao.sdk.common.model.AuthErrorCause
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 카카오 로그인. 카카오톡이 설치돼 있으면 카카오톡으로, 아니면(또는 카카오톡 로그인이 실패하면)
 * 카카오계정 웹 로그인으로 진행해 OIDC ID 토큰을 얻는다.
 *
 * 서버는 ID 토큰과, 그 토큰에 박힌 nonce 를 대조해 재전송 공격을 막으므로 시도마다 새 nonce 를 만든다.
 * ID 토큰은 카카오 디벨로퍼스 콘솔에서 OpenID Connect 를 켜야 발급된다.
 */
internal object KakaoLoginClient : SocialLoginClient() {

    private const val TAG = "KakaoLogin"

    override val type: SocialLoginType = SocialLoginType.KAKAO

    // 초기화 전에 UserApiClient 에 접근하면 예외가 나므로 SDK 호출 전에 반드시 확인한다.
    override val isAvailable: Boolean
        get() = KakaoSdk.isInitialized

    override fun launch(activity: Activity, onResult: (SocialLoginResult) -> Unit) {
        if (!isAvailable) {
            onResult(SocialLoginResult.Unavailable)
            return
        }
        val nonce = newNonce()
        val delivered = AtomicBoolean(false)
        val deliver: (SocialLoginResult) -> Unit = { result ->
            if (delivered.compareAndSet(false, true)) onResult(result)
        }
        // 콜백은 로그인 창이 닫힌 뒤에 오므로 Activity 대신 애플리케이션 컨텍스트만 붙잡는다.
        val appContext = activity.applicationContext
        val userApi = UserApiClient.instance

        if (userApi.isKakaoTalkLoginAvailable(activity)) {
            userApi.loginWithKakaoTalk(activity, nonce = nonce) { token, error ->
                if (error != null && !error.isCancellation()) {
                    // 카카오톡에 로그인돼 있지 않은 경우 등 — 카카오계정 로그인으로 이어서 시도한다.
                    Log.w(TAG, "카카오톡 로그인 실패, 카카오계정 로그인으로 전환", error)
                    loginWithKakaoAccount(appContext, nonce, deliver)
                } else {
                    deliver(resultOf(token, error, nonce))
                }
            }
        } else {
            loginWithKakaoAccount(activity, nonce, deliver)
        }
    }

    /** 카카오계정 웹 로그인. SDK 가 새 태스크로 커스텀 탭을 띄우므로 애플리케이션 컨텍스트로도 동작한다. */
    private fun loginWithKakaoAccount(
        context: Context,
        nonce: String,
        deliver: (SocialLoginResult) -> Unit,
    ) {
        UserApiClient.instance.loginWithKakaoAccount(context, nonce = nonce) { token, error ->
            deliver(resultOf(token, error, nonce))
        }
    }

    private fun resultOf(token: OAuthToken?, error: Throwable?, nonce: String): SocialLoginResult {
        val idToken = token?.idToken
        val result = when {
            error != null && error.isCancellation() -> SocialLoginResult.Cancelled
            error != null -> {
                Log.w(TAG, "카카오 로그인 실패", error)
                SocialLoginResult.Failed
            }
            idToken.isNullOrBlank() -> {
                Log.w(TAG, "ID 토큰 없음 — 카카오 디벨로퍼스 콘솔에서 OpenID Connect 활성화가 필요하다")
                SocialLoginResult.Failed
            }
            else -> SocialLoginResult.Success(
                SocialCredentialVO(type = type, token = idToken, nonce = nonce),
            )
        }
        // 서버 검증에는 ID 토큰만 필요하다. SDK 가 저장한 카카오 토큰은 남겨 두면 주기적으로
        // 토큰 확인 요청을 보내므로 바로 지운다.
        if (token != null) TokenManagerProvider.instance.manager.clear()
        return result
    }

    /** 사용자가 창을 닫았거나 동의를 거부한 경우 — 실패 안내 없이 조용히 돌아간다. */
    private fun Throwable.isCancellation(): Boolean =
        (this is ClientError && reason == ClientErrorCause.Cancelled) ||
            (this is AuthError && reason == AuthErrorCause.AccessDenied)
}
