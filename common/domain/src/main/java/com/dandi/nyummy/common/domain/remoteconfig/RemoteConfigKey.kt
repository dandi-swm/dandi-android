package com.dandi.nyummy.common.domain.remoteconfig

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer

/**
 * 원격 구성 키 정의. 키 하나가 이름, 값의 직렬화기, 기본값을 함께 가져서 읽는 쪽에서 형 변환과 기본값 처리가 필요 없다.
 *
 * @param key 콘솔의 키 이름(release 기준). 개발 빌드는 이름 뒤에 환경 접미사를 붙여 읽는다(`_debug`).
 * @param serializer 값의 직렬화기. 원시 타입(Boolean, Int, Long, Double, Float, String)은 Firebase의 타입별 조회로 읽고,
 *   그 밖의 타입(목록, VO)은 문자열 값을 JSON으로 파싱한다.
 * @param defaultValue 콘솔에 값이 없거나, 아직 받지 못했거나, 파싱에 실패했을 때 쓰는 값.
 *
 * 새 키는 `data object`를 하나 추가한다.
 */
sealed class RemoteConfigKey<T>(
    val key: String,
    val serializer: KSerializer<T>,
    val defaultValue: T,
) {
    // ---- 강제 업데이트(docs/remote-config-force-update.md) ----

    /** 최소 요구 버전 이름. 표시용이고 강제 업데이트 판단에는 쓰지 않는다. */
    data object MinimumVersion : RemoteConfigKey<String>(
        key = "android_minimum_version",
        serializer = String.serializer(),
        defaultValue = "",
    )

    /** 최소 요구 versionCode. 앱 versionCode가 이보다 작으면 강제 업데이트. 0이면 끈다(값이 없을 때도 끈 것과 같다). */
    data object MinimumVersionCode : RemoteConfigKey<Long>(
        key = "android_minimum_version_code",
        serializer = Long.serializer(),
        defaultValue = 0L,
    )

    /** 강제 업데이트 다이얼로그의 업데이트 버튼이 여는 링크. */
    data object LatestVersionUpdateLink : RemoteConfigKey<String>(
        key = "android_latest_version_update_link",
        serializer = String.serializer(),
        defaultValue = "",
    )

    /** 강제 업데이트 다이얼로그 문구. 비어 있으면 앱 기본 문구를 쓴다. */
    data object MinimumVersionReleaseNote : RemoteConfigKey<String>(
        key = "android_minimum_version_release_note",
        serializer = String.serializer(),
        defaultValue = "",
    )

    // ---- 홈 새 소식 공지(docs/remote-config-home-notice.md) ----

    /** 홈 첫 진입 때 띄울 새 소식 공지 JSON. 비어 있거나 형식이 맞지 않으면 띄우지 않는다. 파싱은 홈 데이터 계층이 한다. */
    data object HomeNotice : RemoteConfigKey<String>(
        key = "android_home_notice",
        serializer = String.serializer(),
        defaultValue = "",
    )

    // ---- 기능 플래그 ----

    /** 우편함 아이템 탭(보상, 코인, 아이템 받기). 백엔드가 완성될 때까지 운영에서는 끈다. */
    data object MailItemTabEnabled : RemoteConfigKey<Boolean>(
        key = "android_mail_item_tab_enabled",
        serializer = Boolean.serializer(),
        defaultValue = false,
    )

    /** 마이룸(방 꾸미기). 백엔드가 완성될 때까지 운영에서는 끄고, 하단 내비 마이룸 탭을 누르면 준비 중이라고 알린다. */
    data object MyRoomEnabled : RemoteConfigKey<Boolean>(
        key = "android_my_room_enabled",
        serializer = Boolean.serializer(),
        defaultValue = false,
    )

    /** 상점. 기능이 완성될 때까지 운영에서는 끄고, 하단 내비 상점 탭을 누르면 준비 중이라고 알린다. */
    data object ShopEnabled : RemoteConfigKey<Boolean>(
        key = "android_shop_enabled",
        serializer = Boolean.serializer(),
        defaultValue = false,
    )
}
