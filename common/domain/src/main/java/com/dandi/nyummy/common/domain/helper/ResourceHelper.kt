package com.dandi.nyummy.common.domain.helper

/**
 * 도메인 레이어에서 Android StringRes 의존 없이 다국어 문자열을 조회하기 위한 추상화.
 * 구현체([ResourceHelperImpl])는 presentation 레이어에서 Context.getString 으로 해석한다.
 * 도메인이 화면에 띄우는 문구(오류 안내, 버튼 글자)는 한국어를 코드에 적지 않고 [StringResource]로 고른다.
 */
interface ResourceHelper {
    fun getString(resource: StringResource): String

    /** `%1$s` 같은 자리 표시자가 있는 문구. 테스트 대역은 [getString]만 구현해도 된다. */
    fun getString(resource: StringResource, vararg formatArgs: Any): String =
        getString(resource).format(*formatArgs)
}
