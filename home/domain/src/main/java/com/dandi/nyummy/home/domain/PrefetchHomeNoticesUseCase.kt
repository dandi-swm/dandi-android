package com.dandi.nyummy.home.domain

import com.dandi.nyummy.common.domain.helper.DeviceHelper
import com.dandi.nyummy.common.domain.helper.ImagePrefetchHelper
import com.dandi.nyummy.common.entity.time.DeviceTime
import javax.inject.Inject

/**
 * 홈에서 띄울 새 소식 공지의 사진을 미리 받아 둔다. 인트로가 원격 구성을 받은 뒤 홈으로 갈 회원일 때 부른다.
 * 홈과 같은 조건(이번 실행에 이미 다룸, 오늘 하루 숨김, 기간, 최소 versionCode)으로 고른 공지만 받고,
 * 공지를 보였다고 표시하지는 않는다. 그래서 홈이 열리자마자 사진이 디스크에서 바로 그려진다.
 * 받지 못한 사진은 알리지 않는다(홈이 그릴 때 다시 받는다).
 */
class PrefetchHomeNoticesUseCase @Inject constructor(
    private val repository: HomeRepository,
    private val deviceHelper: DeviceHelper,
    private val imagePrefetchHelper: ImagePrefetchHelper,
) {
    suspend operator fun invoke(nowMillis: Long = System.currentTimeMillis()) {
        if (repository.isNoticeHandled()) return
        if (repository.noticeHiddenDateOrNull() == DeviceTime.today(nowMillis)) return
        val versionCode = deviceHelper.appVersionCode
        val urls = repository.getNotices()
            .filter { it.isVisibleAt(nowMillis, versionCode) }
            .mapNotNull { it.imageUrl }
            .distinct()
        if (urls.isNotEmpty()) imagePrefetchHelper.prefetchImages(urls)
    }
}
