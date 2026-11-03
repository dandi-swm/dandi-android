package com.dandi.nyummy.widget.domain

import com.dandi.nyummy.common.entity.time.DeviceTime
import com.dandi.nyummy.widget.entity.WidgetCardVO
import com.dandi.nyummy.widget.entity.WidgetPlanVO
import com.dandi.nyummy.widget.entity.WidgetVariantPickVO
import java.time.ZoneId
import javax.inject.Inject
import kotlin.random.Random

/**
 * 지금 위젯에 그릴 카드를 정한다.
 *
 * 남겨 둔 값으로 상태를 계산하고([WidgetPlanner]), 그 상태의 시안 가운데 한 장을 고른다.
 * 같은 회차([WidgetPlanVO.episodeKey]) 동안은 같은 시안을 유지하고, 회차가 바뀌면 직전 시안을 빼고 무작위로 고른다.
 * 위젯 receiver가 부르며, 실패해도 화면 알림이나 이동을 하지 않는다.
 */
class GetWidgetCardUseCase @Inject constructor(
    private val repository: WidgetRepository,
) {
    /**
     * @param variantCount 상태마다 시안이 몇 장인지. 시안 목록은 presentation이 안다.
     */
    suspend operator fun invoke(
        variantCount: (WidgetPlanVO) -> Int,
        nowMillis: Long = System.currentTimeMillis(),
        zone: ZoneId = DeviceTime.zone(),
        random: Random = Random.Default,
    ): WidgetCardVO {
        val snapshot = repository.snapshot()
        val plan = WidgetPlanner.plan(snapshot, repository.isLoggedIn(), nowMillis, zone)
        val count = variantCount(plan).coerceAtLeast(1)
        val pick = snapshot.variantPicks[plan.stage]
        if (pick != null && pick.episodeKey == plan.episodeKey && pick.chosen in 0 until count) {
            return WidgetCardVO(plan = plan, variant = pick.chosen)
        }
        val previous = pick?.chosen ?: -1
        val candidates = (0 until count).filter { it != previous }.ifEmpty { listOf(0) }
        val chosen = candidates[random.nextInt(candidates.size)]
        repository.update { current ->
            current.copy(
                variantPicks = current.variantPicks +
                    (plan.stage to WidgetVariantPickVO(episodeKey = plan.episodeKey, chosen = chosen, previous = previous)),
            )
        }
        return WidgetCardVO(plan = plan, variant = chosen)
    }
}
