package com.dandi.nyummy.home.domain

import com.dandi.nyummy.common.entity.time.DeviceTime
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * 새 소식 공지를 닫는다. 이번 실행에서는 다시 띄우지 않고, [hideToday]면 기기 날짜로 오늘 하루 더 띄우지 않는다.
 * 튜토리얼이 떠 있어 이번 실행에 공지를 건너뛸 때도 [hideToday] 없이 부른다.
 * 기기 저장소에 쓰지 못해도 이번 실행에서는 닫힌 채로 둔다.
 */
class CloseHomeNoticeUseCase @Inject constructor(
    private val repository: HomeRepository,
) {
    suspend operator fun invoke(hideToday: Boolean, nowMillis: Long = System.currentTimeMillis()) {
        repository.markNoticeHandled()
        if (!hideToday) return
        try {
            repository.setNoticeHiddenDate(DeviceTime.today(nowMillis))
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // 다음 실행에 한 번 더 보일 뿐이라 알리지 않는다.
        }
    }
}
