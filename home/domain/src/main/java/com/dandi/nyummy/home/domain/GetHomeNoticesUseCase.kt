package com.dandi.nyummy.home.domain

import com.dandi.nyummy.common.domain.helper.DeviceHelper
import com.dandi.nyummy.common.entity.time.DeviceTime
import com.dandi.nyummy.home.entity.HomeNoticeVO
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * 홈 첫 진입 때 띄울 새 소식 공지를 고른다. 홈이 띄우기로 한 때에만 부른다.
 *
 * 한 실행에 한 번만 고른다. 부르는 순간 이번 실행은 다룬 것으로 표시해, 홈에 다시 들어오거나 홈 화면이 새로 만들어져도
 * 다시 띄우지 않는다. "오늘 하루 보지 않기"로 닫은 날(기기 날짜)이 오늘이면 아무것도 고르지 않는다.
 * 남은 공지는 JSON 순서를 지키며, 지금이 기간 안이고 앱 versionCode가 최소 버전 이상인 것만 고른다(적지 않은 조건은 보지 않는다).
 */
class GetHomeNoticesUseCase @Inject constructor(
    private val repository: HomeRepository,
    private val deviceHelper: DeviceHelper,
) {
    suspend operator fun invoke(nowMillis: Long = System.currentTimeMillis()): List<HomeNoticeVO> {
        if (repository.isNoticeHandled()) return emptyList()
        repository.markNoticeHandled()
        if (hiddenDate() == DeviceTime.today(nowMillis)) return emptyList()
        val versionCode = deviceHelper.appVersionCode
        return repository.getNotices().filter { it.isVisible(nowMillis, versionCode) }
    }

    /** 기기 저장소를 읽지 못하면 숨기지 않은 것으로 본다. */
    private suspend fun hiddenDate() = try {
        repository.getNoticeHiddenDate()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

    private fun HomeNoticeVO.isVisible(nowMillis: Long, versionCode: Long): Boolean =
        startAtMillis.let { it == null || it <= nowMillis } &&
            endAtMillis.let { it == null || nowMillis <= it } &&
            minVersionCode.let { it == null || it <= versionCode }
}
