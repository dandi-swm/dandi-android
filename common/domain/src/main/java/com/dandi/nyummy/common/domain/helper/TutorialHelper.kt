package com.dandi.nyummy.common.domain.helper

import com.dandi.nyummy.common.entity.tutorial.TutorialStep

/**
 * 첫 사용자 튜토리얼의 진행 상태. 이 기기에 남긴다.
 *
 * - 첫 식사 튜토리얼(홈 소개, 식사 기록 튜토리얼)을 할지는 서버가 정한다. 홈이 받은 값을 [setMealTutorialRequired]로
 *   옮겨 두면, 홈을 모르는 식사 기록 화면이 [isMealTutorialRequired]로 읽는다.
 * - 뒤 안내(우편함, 마이룸)는 튜토리얼 식사가 접수되면 시작한다([start]). 단계는 화면마다 따로 보여 주고 마치면
 *   [complete]로 남긴다. 건너뛰면([skipAll]) 남은 단계를 모두 끝낸다.
 * - 로그아웃·탈퇴하면([clear]) 다른 계정이 이어 보지 않게 모두 지운다.
 *
 * 여러 기능(홈, 식사 기록, 우편함, 마이룸, 인증, 설정)이 함께 쓰는 상태라 원격 설정처럼 공통 Helper로 둔다.
 */
interface TutorialHelper {
    /** [step]을 지금 보여 줘야 하는지. 대상이고, 건너뛰지 않았고, 그 단계를 아직 마치지 않았을 때 true. */
    suspend fun isPending(step: TutorialStep): Boolean

    /** [step]을 마쳤다고 남긴다. 다시 보여 주지 않는다. */
    suspend fun complete(step: TutorialStep)

    /** 뒤 안내 대상으로 표시한다. 튜토리얼 식사가 접수되면 부른다. 마친 단계 기록은 비운다. */
    suspend fun start()

    /** 건너뛰기. 남은 단계를 모두 끝낸다. */
    suspend fun skipAll()

    /** 대상 여부와 진행 기록을 모두 지운다. 로그아웃·탈퇴 때 부른다. */
    suspend fun clear()

    /** 식사 기록을 튜토리얼(예시 사진으로 기록)로 열어야 하는지. 홈이 서버 값을 옮겨 두기 전이면 false. */
    suspend fun isMealTutorialRequired(): Boolean

    /** 서버가 알려 준 "튜토리얼 식사를 아직 하지 않았다"를 옮겨 둔다. 홈이 응답을 받을 때마다 부른다. */
    suspend fun setMealTutorialRequired(required: Boolean)
}
