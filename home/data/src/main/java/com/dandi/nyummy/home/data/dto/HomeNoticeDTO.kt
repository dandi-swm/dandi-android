package com.dandi.nyummy.home.data.dto

import com.dandi.nyummy.common.entity.time.DeviceTime
import com.dandi.nyummy.home.entity.HomeNoticeVO
import kotlinx.serialization.Serializable

/**
 * Remote Config `android_home_notice` 값(JSON 문자열). 작성법은 docs/remote-config-home-notice.md를 본다.
 *
 * @property notices 공지 목록. 이 순서대로 넘겨 본다.
 */
@Serializable
data class HomeNoticeListDTO(
    val notices: List<HomeNoticeDTO>? = null,
) {
    /** 보일 수 있는 공지만 순서대로 바꾼다. 형식이 맞지 않는 공지는 그 공지만 버린다. */
    fun toVO(): List<HomeNoticeVO> = notices.orEmpty().mapNotNull { it.toVO() }
}

/**
 * 공지 한 장.
 *
 * @property id 공지 구분값(운영용)
 * @property badge 사진 위 배지 글자
 * @property startAt 보이기 시작하는 순간(ISO 8601, 오프셋 포함). 없으면 처음부터 보인다.
 * @property endAt 그만 보이는 순간(ISO 8601, 오프셋 포함). 없으면 끝없이 보인다.
 * @property minVersionCode 이 versionCode 이상인 앱에만 보인다
 * @property imageUrl 10:9 사진 주소(https)
 * @property meta 제목 위 작은 글(이벤트 기간 등)
 * @property title 제목. 없으면 공지를 버린다.
 * @property body 설명
 * @property cta 버튼. 없으면 버튼 없는 공지다.
 */
@Serializable
data class HomeNoticeDTO(
    val id: String? = null,
    val badge: String? = null,
    val startAt: String? = null,
    val endAt: String? = null,
    val minVersionCode: Long? = null,
    val imageUrl: String? = null,
    val meta: String? = null,
    val title: String? = null,
    val body: String? = null,
    val cta: HomeNoticeCtaDTO? = null,
) {
    /**
     * 제목이 없거나, 적어 둔 시각을 읽을 수 없으면 null(공지를 버린다). 끝 시각을 잘못 적은 공지가 끝없이 뜨지 않게 한다.
     * 버튼은 글자와 경로가 모두 있을 때만 둔다.
     */
    fun toVO(): HomeNoticeVO? {
        val title = title?.trim().orEmpty()
        if (title.isEmpty()) return null
        val start = startAt?.takeIf { it.isNotBlank() }?.let { DeviceTime.parseIsoToEpochMillisOrNull(it) ?: return null }
        val end = endAt?.takeIf { it.isNotBlank() }?.let { DeviceTime.parseIsoToEpochMillisOrNull(it) ?: return null }
        val ctaText = cta?.text?.trim()?.takeIf { it.isNotEmpty() }
        val ctaLink = cta?.link?.trim()?.takeIf { it.isNotEmpty() }
        val hasCta = ctaText != null && ctaLink != null
        return HomeNoticeVO(
            id = id.orEmpty(),
            badge = badge?.trim().orEmpty(),
            startAtMillis = start,
            endAtMillis = end,
            minVersionCode = minVersionCode,
            imageUrl = imageUrl?.trim()?.takeIf { it.isNotEmpty() },
            meta = meta?.trim()?.takeIf { it.isNotEmpty() },
            title = title,
            body = body?.trim().orEmpty(),
            ctaText = ctaText.takeIf { hasCta },
            ctaLink = ctaLink.takeIf { hasCta },
        )
    }
}

/**
 * 공지 버튼.
 *
 * @property text 버튼 글자
 * @property link 누르면 여는 앱 경로(`/shop` 등)
 */
@Serializable
data class HomeNoticeCtaDTO(
    val text: String? = null,
    val link: String? = null,
)
