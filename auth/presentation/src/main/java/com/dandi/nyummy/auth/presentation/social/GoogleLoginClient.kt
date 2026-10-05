package com.dandi.nyummy.auth.presentation.social

import android.app.Activity
import android.os.CancellationSignal
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.credentials.CredentialManagerCallback
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.dandi.nyummy.auth.entity.SocialCredentialVO
import com.dandi.nyummy.auth.entity.SocialLoginType
import com.dandi.nyummy.auth.presentation.BuildConfig
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

/**
 * 구글 로그인. Credential Manager 의 "Google 계정으로 로그인" 바텀시트로 OIDC ID 토큰을 얻는다.
 *
 * ID 토큰은 웹 애플리케이션 클라이언트 ID([BuildConfig.GOOGLE_WEB_CLIENT_ID])를 aud 로 발급되며,
 * 서버는 aud 와 nonce 를 대조해 검증하므로 시도마다 새 nonce 를 만든다.
 * 구글 콘솔에 이 앱의 패키지명·서명 SHA-1 로 Android 클라이언트도 등록돼 있어야 토큰이 발급된다.
 */
internal object GoogleLoginClient : SocialLoginClient() {

    private const val TAG = "GoogleLogin"

    override val type: SocialLoginType = SocialLoginType.GOOGLE

    override val isAvailable: Boolean
        get() = BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()

    override fun launch(activity: Activity, onResult: (SocialLoginResult) -> Unit) {
        if (!isAvailable) {
            onResult(SocialLoginResult.Unavailable)
            return
        }
        val nonce = newNonce()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(
                GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                    .setNonce(nonce)
                    .build(),
            )
            .build()

        // 바텀시트는 Activity 위에 떠야 하므로 요청에는 Activity 를 넘기되,
        // 콜백에는 캡처하지 않는다(그사이 화면이 재생성될 수 있다).
        CredentialManager.create(activity).getCredentialAsync(
            activity,
            request,
            CancellationSignal(),
            ContextCompat.getMainExecutor(activity.applicationContext),
            object : CredentialManagerCallback<GetCredentialResponse, GetCredentialException> {
                override fun onResult(result: GetCredentialResponse) {
                    onResult(resultOf(result, nonce))
                }

                override fun onError(e: GetCredentialException) {
                    onResult(
                        if (e is GetCredentialCancellationException) {
                            SocialLoginResult.Cancelled
                        } else {
                            Log.w(TAG, "구글 로그인 실패", e)
                            SocialLoginResult.Failed
                        },
                    )
                }
            },
        )
    }

    private fun resultOf(response: GetCredentialResponse, nonce: String): SocialLoginResult {
        val credential = response.credential
        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            Log.w(TAG, "예상하지 못한 자격 증명 유형: ${credential.type}")
            return SocialLoginResult.Failed
        }
        return try {
            val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
            SocialLoginResult.Success(
                SocialCredentialVO(type = type, token = idToken, nonce = nonce),
            )
        } catch (e: GoogleIdTokenParsingException) {
            Log.w(TAG, "구글 ID 토큰 파싱 실패", e)
            SocialLoginResult.Failed
        }
    }
}
