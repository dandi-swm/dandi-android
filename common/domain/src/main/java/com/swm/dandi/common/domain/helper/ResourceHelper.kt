package com.swm.dandi.common.domain.helper

/**
 * 도메인 레이어에서 Android StringRes 의존 없이 다국어 문자열을 조회하기 위한 추상화.
 * 구현체([ResourceHelperImpl])는 presentation 레이어에서 Context.getString 으로 해석한다.
 */
interface ResourceHelper {
    fun getString(resource: StringResource): String
}
