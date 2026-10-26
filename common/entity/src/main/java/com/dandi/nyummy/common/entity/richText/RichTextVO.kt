package com.dandi.nyummy.common.entity.richText

/**
 * 꾸민 글자 조각 하나. 서버가 보낸 조각을 순서대로 이어 붙여 그린다.
 * 띄어쓰기와 줄바꿈은 [text]에 들어 있고, 아이콘 옆 간격은 앱이 넣는다.
 *
 * @property typeScale 글자 토큰 이름(displayM, titleS 등). 모르는 값이면 bodyM으로 그린다.
 * @property color 색 토큰 이름(contentPrimary 등). 모르는 값이면 contentPrimary로 그린다.
 * @property icon 아이콘 조각 이름(coin, clock, chevronRight). 있으면 [text]는 비어 있고, 모르는 값이면 빼고 그린다.
 */
data class RichTextVO(
    val text: String = "",
    val typeScale: String = "",
    val color: String = "",
    val icon: String = "",
    val strike: Boolean = false,
) {
    val isIcon: Boolean get() = icon.isNotEmpty()

    companion object {
        val empty: List<RichTextVO> = emptyList()
    }
}

typealias RichText = List<RichTextVO>
