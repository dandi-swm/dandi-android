package com.dandi.nyummy

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.dandi.nyummy.auth.presentation.social.initializeSocialLoginSdks
import com.dandi.nyummy.common.data.analytics.AirbridgeSdk
import com.dandi.nyummy.common.presentation.image.ImageLoadReport
import com.dandi.nyummy.common.presentation.image.NoOpImageLoadReport
import com.dandi.nyummy.common.presentation.image.NyummyImageLoaderFactory
import com.dandi.nyummy.push.PushNotifier
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

        // Airbridge(광고 성과 측정). 수집을 끈 빌드(debug 기본)나 토큰 없이 빌드된 앱에서는 켜지 않는다.
        if (BuildConfig.AIRBRIDGE_ENABLED) AirbridgeSdk.initialize(this, BuildConfig.AIRBRIDGE_SDK_TOKEN)

        // 카카오 등 소셜 로그인 SDK. 앱 키 없이 빌드되면 건너뛴다.
        initializeSocialLoginSdks(this)

        // 푸시 알림 채널을 미리 등록한다. 앱이 백그라운드일 때 OS가 서버 알림을 이 채널(manifest 기본 채널)로 띄운다.
        PushNotifier.ensureChannel(this)

        // 푸시 토큰은 여기서 등록하지 않는다. 앱이 꺼져 있을 때 푸시가 오면 화면 없이 이 onCreate만 돌기 때문에,
        // 여기서 보내면 푸시 배치 때마다 받은 기기 전부가 등록과 토큰 재발급을 부른다. 화면을 열 때(MainActivity) 보낸다.
    }
}
