package com.dandi.nyummy.common.domain.remoteconfig

/**
 * 원격 구성으로 켜고 끄는 기능.
 *
 * [key]는 release 빌드가 읽는 키다. 그 밖의 빌드(debug, benchmark)는 같은 키에 `_debug`를 붙인 키를 읽는다.
 * 값을 받지 못했거나 콘솔에 키가 없으면 꺼진 것으로 본다.
 */
enum class FeatureFlag(val key: String) {
    /** 우편함 아이템 탭(보상, 코인, 아이템 받기). 백엔드가 완성될 때까지 운영에서는 끈다. */
    MAIL_ITEM_TAB("android_mail_item_tab_enabled"),
}
