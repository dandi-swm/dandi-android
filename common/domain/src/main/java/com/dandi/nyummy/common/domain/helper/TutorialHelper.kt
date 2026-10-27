package com.dandi.nyummy.common.domain.helper

import com.dandi.nyummy.common.entity.tutorial.TutorialStep

/**
 * 첫 사용자 튜토리얼의 진행 상태. 서버에 저장하지 않고 이 기기에만 남긴다.
 *
 * 새로 가입해 온보딩을 마친 사용자만 대상이다([start]). 단계는 화면마다 따로 보여 주고 마치면 [complete]로 남긴다.
 * 건너뛰면([skipAll]) 남은 단계를 모두 끝내고, 로그아웃·탈퇴하면([clear]) 다른 계정이 이어 보지 않게 지운다.
 * 여러 기능(온보딩, 홈, 우편함, 마이룸, 인증, 설정)이 함께 쓰는 상태라 원격 설정처럼 공통 Helper로 둔다.
 */
interface TutorialHelper {
    /** [step]을 지금 보여 줘야 하는지. 대상이고, 건너뛰지 않았고, 그 단계를 아직 마치지 않았을 때 true. */
    suspend fun isPending(step: TutorialStep): Boolean

    /** [step]을 마쳤다고 남긴다. 다시 보여 주지 않는다. */
    suspend fun complete(step: TutorialStep)

    /** 튜토리얼 대상으로 표시한다. 온보딩을 마칠 때 부른다. 마친 단계 기록은 비운다. */
    suspend fun start()

    /** 건너뛰기. 남은 단계를 모두 끝낸다. */
    suspend fun skipAll()

    /** 대상 여부와 진행 기록을 모두 지운다. 로그아웃·탈퇴 때 부른다. */
    suspend fun clear()
}
