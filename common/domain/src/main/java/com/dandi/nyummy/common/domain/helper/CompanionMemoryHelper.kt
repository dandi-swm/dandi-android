package com.dandi.nyummy.common.domain.helper

import kotlinx.coroutines.flow.Flow

/**
 * 냐미가 이 기기에서 사용자를 기억하는 값. 서버에 저장하지 않고 이 기기에만 남긴다.
 * 마지막으로 홈을 본 순간을 남겨 들어올 때 인사(오늘 처음, 오랜만에, 방금)를 고르고,
 * 마지막으로 알던 냐미 체형을 남겨 네트워크를 기다리지 않고 바로 그린다.
 * 사용자가 지어 준 고양이 이름도 남겨, 화면 문구의 "냐미" 자리에 그 이름을 넣는다.
 *
 * 여러 기능(홈, 인증, 설정)이 함께 쓰는 상태라 공통 Helper로 둔다. 로그아웃·탈퇴하면 [clear]로 지운다.
 */
interface CompanionMemoryHelper {
    /** 마지막으로 홈을 본 순간(epoch millis). 없으면 null이다. */
    suspend fun lastSeenAtMillis(): Long?

    suspend fun setLastSeenAtMillis(millis: Long)

    /** 마지막으로 알던 냐미 체형(LEAN, SLIM, NORMAL, CHUBBY, PLUMP). 없으면 null이다. */
    suspend fun lastCatWeight(): String?

    suspend fun setLastCatWeight(weight: String)

    /** 사용자가 지어 준 고양이 이름. 저장한 값이 바뀔 때마다 다시 내보낸다. 모르면 null이다. */
    fun catName(): Flow<String?>

    suspend fun setCatName(name: String)

    suspend fun clear()
}
