package com.dandi.nyummy.home.domain

import com.dandi.nyummy.common.domain.error.runSuspendCatching
import javax.inject.Inject

/**
 * 새 소식 공지를 닫는다. 이번 실행에서는 다시 띄우지 않고, [hideKeys]의 공지("다시 보지 않기"로 닫은 공지,
 * [com.dandi.nyummy.home.entity.HomeNoticeVO.hideKey])는 앞으로도 띄우지 않는다. 나중에 새로 올린 공지는 그대로 뜬다.
 * 튜토리얼이 떠 있어 이번 실행에 공지를 건너뛸 때는 [hideKeys] 없이 부른다.
 * 기기 저장소에 쓰지 못해도 이번 실행에서는 닫힌 채로 둔다.
 */
class CloseHomeNoticeUseCase @Inject constructor(
    private val repository: HomeRepository,
) {
    suspend operator fun invoke(hideKeys: Set<String> = emptySet()) {
        repository.markNoticeHandled()
        if (hideKeys.isEmpty()) return
        // 저장하지 못해도 다음 실행에 한 번 더 보일 뿐이라 알리지 않는다.
        runSuspendCatching { repository.hideNotices(hideKeys) }
    }
}
