package com.swm.dandi.common.data.mediaSearch.dto

import com.swm.dandi.common.entity.UNKNOWN
import com.swm.dandi.common.entity.media.MediaItemVO
import com.swm.dandi.common.entity.media.MediaType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchMetaDTO(
    // true 면 현재 검색어로 더 불러올 페이지가 없음을 의미한다.
    @SerialName("is_end") val isEnd: Boolean? = null,
)

@Serializable
data class ImageSearchResponse(
    val documents: List<ImageDocumentDTO>? = null,
    val meta: SearchMetaDTO? = null,
)

@Serializable
data class ImageDocumentDTO(
    val collection: String? = null,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("display_sitename") val displaySitename: String? = null,
    @SerialName("doc_url") val docUrl: String? = null,
    val datetime: String? = null,
)

@Serializable
data class VideoSearchResponse(
    val documents: List<VideoDocumentDTO>? = null,
    val meta: SearchMetaDTO? = null,
)

@Serializable
data class VideoDocumentDTO(
    val title: String? = null,
    val url: String? = null,
    val thumbnail: String? = null,
    val datetime: String? = null,
)

// https://developers.kakao.com/docs/ko/daum-search/dev-guide#search-image
fun ImageDocumentDTO.toVO(): MediaItemVO = MediaItemVO(
    type = MediaType.IMAGE,
    title = displaySitename ?: UNKNOWN,
    urlKey = imageUrl.orEmpty(),
    thumbnailImageUrl = thumbnailUrl.orEmpty(),
    contentsImageUrl = imageUrl.orEmpty(),
    pageLinkUrl = docUrl.orEmpty(),
    collection = collection.orEmpty(),
    dateTime = datetime.orEmpty(),
)

// https://developers.kakao.com/docs/ko/daum-search/dev-guide#search-video
fun VideoDocumentDTO.toVO(): MediaItemVO = MediaItemVO(
    type = MediaType.VIDEO,
    title = title ?: UNKNOWN,
    urlKey = url.orEmpty(),
    thumbnailImageUrl = thumbnail.orEmpty(),
    contentsImageUrl = thumbnail.orEmpty(),
    pageLinkUrl = url.orEmpty(),
    collection = "",
    dateTime = datetime.orEmpty(),
)
