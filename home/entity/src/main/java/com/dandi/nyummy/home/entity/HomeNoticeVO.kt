package com.dandi.nyummy.home.entity

/**
 * 홈 첫 진입 때 띄우는 새 소식 공지 한 장(Remote Config `android_home_notice`).
 *
 * @property id 공지 구분값. 화면에 쓰지 않고 운영자가 공지를 알아보는 데 쓴다.
 * @property badge 사진 위 왼쪽에 얹는 배지 글자(업데이트, 이벤트 등). 비어 있으면 배지를 그리지 않는다.
 * @property startAtMillis 보이기 시작하는 순간. null이면 처음부터 보인다.
 * @property endAtMillis 그만 보이는 순간. null이면 끝없이 보인다.
 * @property minVersionCode 이 versionCode 이상인 앱에만 보인다. null이면 모든 앱에 보인다.
 * @property imageUrl 10:9 사진 주소. null이면 사진 칸을 연초록 바탕으로 둔다.
 * @property meta 제목 위 작은 글(이벤트 기간 등). null이면 그리지 않는다.
 * @property title 제목. 비어 있는 공지는 만들지 않는다.
 * @property body 설명
 * @property ctaText 버튼 글자. null이면 버튼이 없다.
 * @property ctaLink 버튼이 여는 앱 경로(`/shop` 등). 버튼이 없으면 null이다.
 */
data class HomeNoticeVO(
    val id: String = "",
    val badge: String = "",
    val startAtMillis: Long? = null,
    val endAtMillis: Long? = null,
    val minVersionCode: Long? = null,
    val imageUrl: String? = null,
    val meta: String? = null,
    val title: String = "",
    val body: String = "",
    val ctaText: String? = null,
    val ctaLink: String? = null,
)
