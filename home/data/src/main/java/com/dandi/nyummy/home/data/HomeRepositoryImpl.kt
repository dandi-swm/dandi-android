package com.dandi.nyummy.home.data

import com.dandi.nyummy.common.data.preference.AppPreferenceProvider
import com.dandi.nyummy.common.domain.remoteconfig.RemoteConfigHelper
import com.dandi.nyummy.common.domain.remoteconfig.RemoteConfigKey
import com.dandi.nyummy.home.data.dto.HomeNoticeListDTO
import com.dandi.nyummy.home.domain.HomeRepository
import com.dandi.nyummy.home.entity.HomeNoticeVO
import com.dandi.nyummy.home.entity.HomeSummaryVO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * 미리 받은 요약은 조회 한 번만 넘겨준다. 그 뒤 조회는 매번 새로 받아, 식사를 기록하고 돌아오면 바뀐 숫자를 읽는다.
 * 미리 받기는 [ioScope]에서 돌려, 부른 화면(인트로, 방 준비)이 먼저 닫혀도 끊기지 않는다.
 *
 * 새 소식 공지는 원격 구성 문자열을 JSON으로 읽고, "이번 실행에서 다룸"은 메모리에만 둔다(앱을 다시 실행하면 다시 뜬다).
 */
class HomeRepositoryImpl(
    private val dataSource: HomeDataSource,
    private val ioScope: CoroutineScope,
    private val remoteConfigHelper: RemoteConfigHelper,
    private val appPreferenceProvider: AppPreferenceProvider,
    private val json: Json,
) : HomeRepository {

    /**
     * 미리 받는 중이거나 받아 둔 요약. 조회 한 번이 가져가면 비운다.
     * 냐미 미리 받기처럼 화면(ViewModel)이 메인 스레드에서 차례로 읽고 쓰고, 받는 코루틴은 이 값을 건드리지 않아 잠그지 않는다.
     */
    private var prefetched: Deferred<HomeSummaryVO>? = null

    override suspend fun getHomeSummary(): HomeSummaryVO {
        val pending = prefetched ?: return fetch()
        prefetched = null
        // 미리 받기가 실패했으면 다시 받지 않고 그 실패를 넘긴다. 홈은 다음에 화면이 보일 때 다시 받는다.
        return pending.await()
    }

    override fun prefetchHomeSummary() {
        if (prefetched?.isActive == true) return
        prefetched = ioScope.async { fetch() }
    }

    private suspend fun fetch(): HomeSummaryVO = dataSource.getHome().toVO()

    /** 이번 실행에서 새 소식을 이미 다뤘는지. 싱글턴 저장소에 두어 홈 화면이 다시 만들어져도 남는다. */
    private var noticeHandled = false

    override fun getNotices(): List<HomeNoticeVO> {
        val raw = remoteConfigHelper.get(RemoteConfigKey.HomeNotice).takeIf { it.isNotBlank() } ?: return emptyList()
        // 콘솔에서 JSON을 틀리게 넣어도 앱은 그대로 두고 공지만 띄우지 않는다.
        return runCatching { json.decodeFromString<HomeNoticeListDTO>(raw).toVO() }.getOrDefault(emptyList())
    }

    override suspend fun getNoticeHiddenDate(): LocalDate? =
        appPreferenceProvider.homeNoticeHiddenDate()?.let {
            try {
                LocalDate.parse(it)
            } catch (_: DateTimeParseException) {
                null
            }
        }

    override suspend fun setNoticeHiddenDate(date: LocalDate) {
        appPreferenceProvider.setHomeNoticeHiddenDate(date.toString())
    }

    override fun isNoticeHandled(): Boolean = noticeHandled

    override fun markNoticeHandled() {
        noticeHandled = true
    }
}
