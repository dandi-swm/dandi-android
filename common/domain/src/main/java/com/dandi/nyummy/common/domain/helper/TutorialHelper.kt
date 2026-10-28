package com.dandi.nyummy.common.domain.helper

import com.dandi.nyummy.common.entity.tutorial.TutorialStep

/**
 * 첫 사용자 튜토리얼의 진행 상태. 이 기기에 남긴다.
 *
 * - 튜토리얼 식사 앞 단계(홈 소개, 카메라, 식사 기록 튜토리얼)는 서버 값으로 정한다. 홈이 받은 값을
 *   [setMealTutorialRequired]로 옮겨 두면, 홈을 모르는 식사 기록 화면이 [isMealTutorialRequired]로 읽는다.
 * - 튜토리얼 식사가 접수되면 [startAfterMeal]로 뒤 단계를 모두 남기고, 화면마다 마치면 [complete]로 지운다.
 * - 건너뛰면 [skipToHealthStory]로 냐미 건강 이야기만 남기고, 마침에서 [finish]로 모두 끝낸다.
 * - 로그아웃·탈퇴하면 [clear]로 다른 계정이 이어 보지 않게 모두 지운다.
 *
 * 여러 기능(홈, 식사 기록, 우편함, 마이룸, 인증, 설정)이 함께 쓰는 상태라 [CompanionMemoryHelper]처럼 공통 Helper로 둔다.
 */
interface TutorialHelper {
    /** 아직 마치지 않은 뒤 단계. 튜토리얼 식사 전이거나 모두 마쳤으면 비어 있다. */
    suspend fun pendingSteps(): Set<TutorialStep>

    /** [step]을 아직 마치지 않았는지. */
    suspend fun isPending(step: TutorialStep): Boolean = step in pendingSteps()

    /** 튜토리얼 식사가 접수되면 부른다. 뒤 단계를 모두 남긴다. */
    suspend fun startAfterMeal()

    /** [step]을 마쳤다고 남긴다. 다시 보여 주지 않는다. */
    suspend fun complete(step: TutorialStep)

    /** 건너뛰기를 확정하면 부른다. 냐미 건강 이야기만 남긴다. */
    suspend fun skipToHealthStory()

    /** 마침에서 부른다. 남은 단계를 모두 지운다. */
    suspend fun finish()

    /** 진행 상태와 식사 튜토리얼 캐시를 모두 지운다. 로그아웃·탈퇴 때 부른다. */
    suspend fun clear()

    /** 식사 기록을 튜토리얼(예시 사진으로 기록)로 열어야 하는지. 홈이 서버 값을 옮겨 두기 전이면 false. */
    suspend fun isMealTutorialRequired(): Boolean

    /** 서버가 알려 준 "튜토리얼 식사를 아직 하지 않았다"를 옮겨 둔다. 홈이 응답을 받을 때마다 부른다. */
    suspend fun setMealTutorialRequired(required: Boolean)
}
