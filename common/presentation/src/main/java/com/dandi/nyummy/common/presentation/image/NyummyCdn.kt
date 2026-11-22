package com.dandi.nyummy.common.presentation.image

/**
 * 냐미 CDN 그림 주소. 서버는 그림 주소 대신 id를 주고, 앱이 종류별로 정해진 경로에 id를 붙여 주소를 만든다(개발, 운영 공통).
 * 음식 픽셀 아이콘은 앱에 넣어 두므로 여기 없다([NyummyFoodIcons]).
 *
 * 주소는 모두 `.webp`로 만든다. 서버가 아직 `.png`만 올려 둔 그림은 공용 이미지 로더가
 * 같은 경로의 `.png`로 다시 받는다([pngFallbackUrlOf], [CdnWebpFallback]). 그래서 화면은 확장자를 신경 쓰지 않는다.
 */
object NyummyCdn {

    private const val BaseUrl = "https://cdn.nyummy.co.kr"

    private const val WEBP = ".webp"
    private const val PNG = ".png"

    /** 아이템 그림(`items/{itemId}.webp`). 마이룸 배경과 인테리어, 아이템 우편이 쓴다. */
    fun itemUrl(itemId: Long): String = "$BaseUrl/items/$itemId$WEBP"

    /** 닫힌 보상 상자 그림(`reward-boxes/closed/{rewardBoxId}.webp`). 1 브론즈, 2 실버, 3 골드다. */
    fun rewardBoxClosedUrl(rewardBoxId: Long): String = "$BaseUrl/reward-boxes/closed/$rewardBoxId$WEBP"

    /** 열린 보상 상자 그림(`reward-boxes/opened/{rewardBoxId}.webp`). */
    fun rewardBoxOpenedUrl(rewardBoxId: Long): String = "$BaseUrl/reward-boxes/opened/$rewardBoxId$WEBP"

    // 열 수 있게 빛나는 상자 그림. 퀘스트를 마쳐 상자를 열 수 있을 때 쓸 경로라 미리 적어 둔다.
    // fun rewardBoxOpenableUrl(rewardBoxId: Long): String = "$BaseUrl/reward-boxes/openable/$rewardBoxId$WEBP"

    /** 코인 그림(`coins/{coinId}.webp`). */
    fun coinUrl(coinId: Long): String = "$BaseUrl/coins/$coinId$WEBP"

    /**
     * 앱이 만든 CDN `.webp` 주소면 같은 경로의 `.png` 주소를, 아니면 null을 준다.
     * 쿼리나 조각(`?`, `#`)이 붙은 주소, 다른 호스트의 주소(서버가 통째로 주는 배너, 공지 그림 등)는 건드리지 않는다.
     */
    fun pngFallbackUrlOf(url: String): String? {
        if (!url.startsWith("$BaseUrl/") || !url.endsWith(WEBP)) return null
        if (url.contains('?') || url.contains('#')) return null
        return url.removeSuffix(WEBP) + PNG
    }
}
