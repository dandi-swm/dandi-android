package com.dandi.nyummy

import android.app.Application
import android.util.Log
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.dandi.nyummy.auth.presentation.social.initializeSocialLoginSdks
import com.dandi.nyummy.common.presentation.image.ImageLoadReport
import com.dandi.nyummy.common.presentation.image.NoOpImageLoadReport
import com.dandi.nyummy.common.presentation.image.NyummyImageLoaderFactory
import com.dandi.nyummy.reminder.FcmTokenStore
import com.dandi.nyummy.reminder.ReminderNotifier
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class DandiApplication : Application(), SingletonImageLoader.Factory {

    @Inject
    lateinit var imageLoadReport: ImageLoadReport

    /**
     * AsyncImage 등 Coil 싱글턴이 쓰는 전역 ImageLoader. 캐시 설정은 [NyummyImageLoaderFactory] 참고.
     * 첫 이미지 요청 때 만들어지므로 그 전에 [imageLoadReport] 가 주입돼 있다(onCreate 에서 주입).
     */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        NyummyImageLoaderFactory.create(
            context,
            // 주입 전에 이미지가 먼저 요청되는 예외 상황에서도 앱이 죽지 않게 계측을 끈다.
            if (::imageLoadReport.isInitialized) imageLoadReport else NoOpImageLoadReport,
        )

    override fun onCreate() {
        super.onCreate()

        // 카카오 등 소셜 로그인 SDK. 앱 키 없이 빌드되면 건너뛴다.
        initializeSocialLoginSdks(this)

        // 냐미 리마인드 알림 채널을 미리 등록한다 (FCM 백그라운드 자동 표시 대비).
        ReminderNotifier.ensureChannel(this)

        // TODO 서버 push 토큰 등록 API 연동 전까지, 테스트 발송용 토큰을 로그로만 확인한다.
        // 토큰은 재설치·데이터 삭제 때만 바뀐다. 회전 시 로그에 ROTATED 로 표시된다.
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            val rotated = FcmTokenStore.saveAndCheckRotated(this, token)
            Log.d("[NyummyFcm]", "FCM token${if (rotated) " (ROTATED)" else ""}: $token")
        }
    }
}
