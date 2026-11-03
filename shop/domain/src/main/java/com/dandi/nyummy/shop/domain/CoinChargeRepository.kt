package com.dandi.nyummy.shop.domain

import com.dandi.nyummy.shop.entity.CoinChargeEventVO
import com.dandi.nyummy.shop.entity.CoinChargeGrantVO

/** 코인 충전 이벤트(가짜 결제 A/B 테스트) 저장소. 이벤트 값은 원격 구성에서, 받음 기록은 기기에서, 지급은 서버에서 온다. */
interface CoinChargeRepository {

    /** 지금 원격 구성에 있는 이벤트. 없거나 형식이 맞지 않으면 null이다. 받아 오지 않는다(앱 시작 때 받은 값을 읽는다). */
    fun getEvent(): CoinChargeEventVO?

    /** 이 기기에서 [eventId] 회차 코인을 이미 받았는지. */
    suspend fun isClaimed(eventId: String): Boolean

    /** [eventId] 회차 코인을 받은 것으로 기기에 남긴다. */
    suspend fun markClaimed(eventId: String)

    /**
     * 선물 코인을 받는다(`POST /api/v1/coins/gifts`, 가격 그룹은 헤더). [coinPackageId]는 고른 묶음 번호(1~3)이고,
     * 받는 코인 수는 서버가 정한다(묶음과 상관없이 같다). 이미 받은 사람이 다시 보내면 서버는 코인을 더 주지 않고 지금 잔액을 준다.
     */
    suspend fun claim(event: CoinChargeEventVO, coinPackageId: Long): CoinChargeGrantVO
}
