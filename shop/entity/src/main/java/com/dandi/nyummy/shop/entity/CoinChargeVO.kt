package com.dandi.nyummy.shop.entity

/**
 * 코인 충전 이벤트(가짜 결제 A/B 테스트) 한 회차. 원격 구성 `android_coin_charge_event` 값이고, Firebase A/B Testing 실험이
 * 그룹마다 다른 값을 준다. 실제 결제는 하지 않고, "결제하기"를 누르면 이번 한 번만 [grantCoins]를 무료로 넣어 준다.
 *
 * @property eventId 회차 구분값. 분석 이벤트, "다시 보지 않기", 받음 기록이 이 값으로 회차를 나눈다.
 * @property group 이 기기가 받은 A/B 그룹(A, B 등). 분석 이벤트와 사용자 속성에 그대로 남는다.
 * @property grantCoins "결제하기"를 누르면 실제로 넣어 주는 코인 수. 고른 묶음과 그룹에 상관없이 같다.
 * @property notice 홈에 띄울 공지. null이면 공지 없이 충전 화면만 있다(링크로만 들어옴).
 * @property packages 충전 화면에 보일 묶음. 그룹마다 가격만 다르다. 비어 있는 이벤트는 만들지 않는다.
 */
data class CoinChargeEventVO(
    val eventId: String = "",
    val group: String = "",
    val grantCoins: Int = 0,
    val notice: CoinChargeNoticeVO? = null,
    val packages: List<CoinChargePackageVO> = emptyList(),
)

/**
 * 코인 충전 이벤트의 홈 공지 한 장. 홈 새 소식 공지와 같은 다이얼로그에 맨 앞 장으로 뜬다.
 *
 * @property badge 사진 위 배지 글자. 비어 있으면 배지를 그리지 않는다.
 * @property imageUrl 10:9 사진 주소. null이면 사진 칸을 연초록 바탕으로 둔다.
 * @property title 제목
 * @property body 설명
 * @property ctaText 버튼 글자. 버튼은 늘 코인 충전 화면을 연다.
 */
data class CoinChargeNoticeVO(
    val badge: String = "",
    val imageUrl: String? = null,
    val title: String = "",
    val body: String = "",
    val ctaText: String = "",
)

/**
 * 충전 화면의 묶음 하나.
 *
 * @property id 묶음 구분값(coin_300 등). 분석 이벤트의 package_id다.
 * @property coins 묶음의 코인 수(화면 표시용. 실제로 받는 코인은 [CoinChargeEventVO.grantCoins])
 * @property price 원 단위 가격
 * @property imageId 묶음 그림 CDN 번호(`coins/{imageId}.png`, 1 동전 · 2 주머니 · 3 바구니)
 */
data class CoinChargePackageVO(
    val id: String = "",
    val coins: Int = 0,
    val price: Int = 0,
    val imageId: Long = 1,
)

/**
 * 지금 기기에서 본 코인 충전 이벤트.
 *
 * @property event 원격 구성 값
 * @property isClaimed 이 회차 코인을 이미 받았는지(기기 기록). 받았으면 공지를 띄우지 않고, 묶음을 눌러도 안내만 한다.
 */
data class CoinChargeOfferVO(
    val event: CoinChargeEventVO = CoinChargeEventVO(),
    val isClaimed: Boolean = false,
)

/**
 * 이벤트 코인을 받은 결과.
 *
 * @property grantedCoins 받은 코인 수
 * @property balance 받은 뒤 내 코인. 서버가 주지 않으면 null이고, 화면은 상단 바 잔액을 다시 읽어 보여 준다.
 */
data class CoinChargeGrantVO(
    val grantedCoins: Int = 0,
    val balance: Int? = null,
)
