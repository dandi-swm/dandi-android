package com.dandi.nyummy

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.dandi.nyummy.auth.presentation.social.initializeSocialLoginSdks
import com.dandi.nyummy.common.domain.helper.PushTokenHelper
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

    @Inject
    lateinit var pushTokenHelper: PushTokenHelper

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

        // 푸시 알림 채널을 미리 등록한다. 앱이 백그라운드일 때 OS가 서버 알림을 이 채널(manifest 기본 채널)로 띄운다.
        PushNotifier.ensureChannel(this)

        // 로그인한 상태면 이 기기의 푸시 토큰을 서버에 등록한다. 서버가 멱등이라 켤 때마다 보내도 된다.
        pushTokenHelper.register()
    }
}
