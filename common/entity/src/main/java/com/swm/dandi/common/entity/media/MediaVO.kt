package com.swm.dandi.common.entity.media

import kotlinx.serialization.Serializable

/**
 * 이미지/동영상 검색 결과 1건.
 *
 * 카카오 다음 검색 API 의 이미지/동영상 응답을 단일 모델로 합쳐 표현한다.
 * - [title]      : display_sitename(이미지) / title(동영상)
 * - [urlKey]     : 항목 고유 식별 키(이미지 표시용이 아님). image_url(이미지) / url(동영상).
 *                  한 문서(doc_url)에 여러 이미지가 내려오므로 식별자는 doc_url 이 아닌 image_url 을 쓴다.
 * - [pageLinkUrl]    : doc_url(이미지) / url(동영상). 리스트에 표시하는 주소 겸 문서 링크.
 * - [thumbnailImageUrl]: 썸네일(목록/그리드 표시). thumbnail_url(이미지) / thumbnail(동영상)
 * - [contentsImageUrl] : 풀스크린에 띄울 원본 이미지. image_url(이미지) / thumbnail(동영상, 원본이 없어 썸네일 사용)
 * - [collection] : 이미지 전용 카테고리. 동영상은 빈 문자열.
 * - [dateTime]   : datetime(ISO 8601 원본 문자열)
 * - [page]       : 이 항목이 속한 검색 페이지(1-based). 페이지 구분자 렌더링에 사용.
 */
@Serializable
data class MediaItemVO(
    val type: MediaType,
    val title: String,
    val urlKey: String,
    val thumbnailImageUrl: String = "",
    val contentsImageUrl: String = "",
    val pageLinkUrl: String = "",
    val collection: String = "",
    val dateTime: String = "",
    val page: Int = 1,
) {
    companion object {
        val empty: MediaItemVO = MediaItemVO(type = MediaType.UNKNOWN, title = "", urlKey = "")
    }

    val isEmpty: Boolean get() = this == empty
}

/**
 * 검색 결과 한 페이지.
 * - [items] : 이미지+동영상을 합쳐 datetime 최신순으로 정렬한 항목들.
 * - [isEnd] : 더 불러올 페이지가 없으면 true. 이미지/동영상 양쪽 응답의 meta.is_end 가
 *             모두 true 일 때만 true 다(한쪽이라도 더 있으면 다음 페이지를 계속 불러온다).
 */
data class MediaSearchResultVO(
    val items: List<MediaItemVO>,
    val isEnd: Boolean,
)

@Serializable
enum class MediaType(val rawValue: Int) {
    UNKNOWN(0),
    IMAGE(1),
    VIDEO(3);

    companion object {
        fun fromRawValue(value: Int?): MediaType =
            entries.firstOrNull { it.rawValue == value } ?: UNKNOWN
    }
}
