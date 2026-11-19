package com.dandi.nyummy.common.data.analytics

import android.app.Application
import co.ab180.airbridge.Airbridge
import co.ab180.airbridge.AirbridgeOptionBuilder

/**
 * Airbridge(광고 성과 측정) SDK 를 켜는 곳. SDK 를 직접 부르는 코드는 이 파일과 [AirbridgeAnalyticsHelper]뿐이다.
 */
object AirbridgeSdk {

    /** Airbridge 대시보드의 앱 이름. */
    private const val APP_NAME = "nyummy"

    /** SDK 를 켰는지. 켜지 않은 빌드(debug 기본, 토큰 없음)에서는 이벤트를 보내지 않는다. */
    @Volatile
    var isInitialized: Boolean = false
        private set

    /**
     * SDK 를 켠다. 켜는 순간부터 설치와 실행을 스스로 기록한다.
     * SDK 규칙상 Application.onCreate 의 메인 스레드에서 불러야 한다. [sdkToken]이 비어 있으면 켜지 않는다.
     */
    fun initialize(application: Application, sdkToken: String) {
        if (sdkToken.isBlank() || isInitialized) return
        Airbridge.initializeSDK(application, AirbridgeOptionBuilder(APP_NAME, sdkToken).build())
        isInitialized = true
    }
}
