package com.dandi.nyummy.common.presentation.image

/**
 * 냐미 CDN 그림 주소. 서버는 그림 주소 대신 id를 주고, 앱이 종류별로 정해진 경로에 id를 붙여 주소를 만든다(개발, 운영 공통).
 */
object NyummyCdn {

    private const val BaseUrl = "https://cdn.nyummy.co.kr"

    /** 음식 픽셀 아이콘(`icons/{iconId}.png`). */
    fun foodIconUrl(iconId: String): String = "$BaseUrl/icons/$iconId.png"

    /** 아이템 그림(`items/{itemId}.png`). 마이룸 배경과 인테리어, 아이템 우편이 쓴다. */
    fun itemUrl(itemId: Long): String = "$BaseUrl/items/$itemId.png"

    /** 닫힌 보상 상자 그림(`reward-boxes/closed/{rewardBoxId}.png`). 1 브론즈, 2 실버, 3 골드다. */
    fun rewardBoxClosedUrl(rewardBoxId: Long): String = "$BaseUrl/reward-boxes/closed/$rewardBoxId.png"

    /** 열린 보상 상자 그림(`reward-boxes/opened/{rewardBoxId}.png`). */
    fun rewardBoxOpenedUrl(rewardBoxId: Long): String = "$BaseUrl/reward-boxes/opened/$rewardBoxId.png"

    // 열 수 있게 빛나는 상자 그림. 퀘스트를 마쳐 상자를 열 수 있을 때 쓸 경로라 미리 적어 둔다.
    // fun rewardBoxOpenableUrl(rewardBoxId: Long): String = "$BaseUrl/reward-boxes/openable/$rewardBoxId.png"

    /** 코인 그림(`coins/{coinId}.png`). */
    fun coinUrl(coinId: Long): String = "$BaseUrl/coins/$coinId.png"
}
