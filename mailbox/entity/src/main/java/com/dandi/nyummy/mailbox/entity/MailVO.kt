package com.dandi.nyummy.mailbox.entity

/** 우편 종류. 보상 상자는 열어서 받고, 코인과 아이템은 바로 받는다. */
enum class MailCategory {
    REWARD_BOX,
    COIN,
    ITEM,
}

/**
 * 우편함 아이템 탭의 우편 하나.
 *
 * @property title 우편 제목(예 "코인 100개", "퀘스트 상자").
 * @property content 보낸 이유나 받은 내용(예 "첫 기록 보상", "상점에서 샀어요").
 * @property imageUrl 우편 그림 URL.
 * @property isClaimed 이미 받았다.
 * @property expiresAtMillis 받지 않으면 사라지는 시각(epoch millis). null 이면 사라지지 않는다.
 * @property createdAtMillis 도착 시각(epoch millis). 화면은 기기 타임존으로 바꿔 보여 준다.
 */
data class MailVO(
    val mailId: Long = 0L,
    val category: MailCategory = MailCategory.ITEM,
    val title: String = "",
    val content: String = "",
    val imageUrl: String = "",
    val isClaimed: Boolean = false,
    val expiresAtMillis: Long? = null,
    val createdAtMillis: Long = 0L,
)

/**
 * 우편 목록 한 묶음(최신순). 만료된 우편은 서버가 빼고 준다.
 *
 * @property nextCursor 다음 묶음을 읽을 때 넘길 값. 마지막 묶음이면 null.
 */
data class MailPageVO(
    val mails: List<MailVO> = emptyList(),
    val nextCursor: Long? = null,
) {
    val hasNext: Boolean
        get() = nextCursor != null

    companion object {
        val empty = MailPageVO()
    }
}

/**
 * 우편을 받고 지급된 보상. 모두 받기는 받은 우편의 합계이고 [rewardBox]가 없다.
 *
 * @property coin 받은 코인. 코인이 없으면 null.
 * @property items 받은 아이템. 없으면 빈 목록.
 * @property rewardBox 보상 상자 우편이면 상자 그림과 열기 연출. 코인, 아이템 우편이면 null.
 */
data class RewardVO(
    val coin: RewardCoinVO? = null,
    val items: List<RewardItemVO> = emptyList(),
    val rewardBox: RewardBoxVO? = null,
) {
    companion object {
        val empty = RewardVO()
    }
}

/** 받은 코인. */
data class RewardCoinVO(
    val quantity: Int = 0,
    val imageUrl: String = "",
)

/** 받은 아이템 하나. */
data class RewardItemVO(
    val itemId: Long = 0L,
    val name: String = "",
    val imageUrl: String = "",
    val quantity: Int = 0,
)

/**
 * 보상 상자 그림과 열기 연출.
 *
 * @property animationUrl 열기 연출 파일 URL. 서버가 아직 주지 않으면 null.
 */
data class RewardBoxVO(
    val imageUrl: String = "",
    val animationUrl: String? = null,
)
