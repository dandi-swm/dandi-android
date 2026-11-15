package com.dandi.nyummy.shop.entity

/**
 * 코인 충전 이벤트(가짜 결제 A/B 테스트) 한 회차. 원격 구성 `android_coin_charge_event` 값이고, Firebase A/B Testing 실험이
 * 그룹마다 다른 값을 준다. 실제 결제는 하지 않고, "결제하기"를 누르면 이번 한 번만 서버가 선물 코인을 넣어 준다(`POST /api/v1/coins/gifts`).
 *
 * @property eventId 회차 구분값. 분석 이벤트, "다시 보지 않기", 받음 기록이 이 값으로 회차를 나눈다.
 * @property group 이 기기가 받은 가격 그룹(A 또는 B). 선물 코인 받기 헤더, 분석 이벤트, 사용자 속성에 그대로 간다.
 * @property notice 홈에 띄울 공지. null이면 공지 없이 충전 화면만 있다(링크로만 들어옴).
 * @property packages 충전 화면에 보일 묶음. 그룹마다 가격만 다르고, 앱에 고정된 표에서 온다(서버와 같은 표).
 */
data class CoinChargeEventVO(
    val eventId: String = "",
    val group: String = "",
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
 * @property coinPackageId 화면의 묶음 번호(1~3). 선물 코인 받기 요청과 분석 이벤트의 package_id다.
 * @property coins 묶음의 코인 수(화면 표시용. 실제로 받는 코인은 서버가 정한다)
 * @property price 원 단위 기준 가격. 분석 이벤트에 그대로 남기고, 화면 가격은 이 값을 나라 통화로 바꿔 보인다([CoinChargePriceVO]).
 * @property imageId 묶음 그림 CDN 번호(`coins/{imageId}.png`, 1 동전 · 2 주머니 · 3 바구니)
 */
data class CoinChargePackageVO(
    val coinPackageId: Long = 0L,
    val coins: Int = 0,
    val price: Int = 0,
    val imageId: Long = 1,
)

/**
 * 충전 화면 가격의 통화. 실제 결제 전 가짜 결제 A/B 테스트라 원화 묶음 가격을 고정 환율로 바꾼 표만 둔다.
 *
 * @property code ISO 4217 통화 코드
 * @property fractionDigits 소수 자리 수. [CoinChargePriceVO.amount]가 이 자리만큼 작은 단위(센트)다.
 */
enum class CoinChargeCurrency(val code: String, val fractionDigits: Int) {
    KRW("KRW", 0),
    JPY("JPY", 0),
    USD("USD", 2),
}

/**
 * 화면에 보일 묶음 가격 하나.
 *
 * @property currency 통화
 * @property amount 통화의 가장 작은 단위 금액(원·엔은 그대로, 달러는 센트)
 */
data class CoinChargePriceVO(
    val currency: CoinChargeCurrency = CoinChargeCurrency.KRW,
    val amount: Long = 0L,
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
