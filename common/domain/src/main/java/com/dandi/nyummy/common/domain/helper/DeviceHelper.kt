package com.dandi.nyummy.common.domain.helper

/**
 * 현재 기기/앱 환경 정보 제공자.
 *
 * 라이브러리 모듈에는 app 의 `BuildConfig.VERSION_*` 가 없으므로 PackageInfo 로 읽는다.
 * domain 이 이 인터페이스를 주입받아 순수하게 버전 비교 등을 수행할 수 있다.
 * (필요 시 OS 버전·기기 모델 등 기기 정보를 여기에 확장한다.)
 */
interface DeviceHelper {
    /** 현재 앱 versionCode. */
    val appVersionCode: Long

    /** 현재 앱 versionName (예: "1.0"). */
    val appVersionName: String
}
