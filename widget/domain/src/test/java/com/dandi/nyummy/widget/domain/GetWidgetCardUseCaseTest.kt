package com.dandi.nyummy.widget.domain

import com.dandi.nyummy.widget.entity.WidgetPlanVO
import com.dandi.nyummy.widget.entity.WidgetSnapshotVO
import com.dandi.nyummy.widget.entity.WidgetStage
import com.dandi.nyummy.widget.entity.WidgetVariantPickVO
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlin.random.Random

class GetWidgetCardUseCaseTest {

    @Test
    fun `같은 회차에 여러 번 그려도 같은 안이다`() = runBlocking {
        val repository = FakeWidgetRepository(nightSnapshot())
        val useCase = GetWidgetCardUseCase(repository)

        val first = useCase(threeVariants, nowMillis = at(TODAY, 2), zone = SEOUL, random = Random(1))
        val again = (2..20).map { seed ->
            useCase(threeVariants, nowMillis = at(TODAY, 3), zone = SEOUL, random = Random(seed)).variant
        }

        assertEquals(WidgetStage.NIGHT, first.plan.stage)
        assertTrue(again.all { it == first.variant })
    }

    @Test
    fun `다음 회차에는 직전에 보인 안을 빼고 고른다`() = runBlocking {
        (0..30).forEach { seed ->
            val yesterdayPick = WidgetVariantPickVO(episodeKey = "N:${TODAY.minusDays(1)}", chosen = 1)
            val repository = FakeWidgetRepository(
                nightSnapshot().copy(variantPicks = mapOf(WidgetStage.NIGHT to yesterdayPick)),
            )

            val card = GetWidgetCardUseCase(repository)(threeVariants, at(TODAY, 2), SEOUL, Random(seed))

            assertNotEquals(1, card.variant)
            val saved = repository.current.variantPicks.getValue(WidgetStage.NIGHT)
            assertEquals("N:$TODAY", saved.episodeKey)
            assertEquals(card.variant, saved.chosen)
            assertEquals(1, saved.previous)
        }
    }

    @Test
    fun `안이 한 장뿐인 상태는 늘 그 안이다`() = runBlocking {
        val previousPick = WidgetVariantPickVO(episodeKey = "N:${TODAY.minusDays(1)}", chosen = 0)
        val repository = FakeWidgetRepository(
            nightSnapshot().copy(variantPicks = mapOf(WidgetStage.NIGHT to previousPick)),
        )

        val card = GetWidgetCardUseCase(repository)({ 1 }, at(TODAY, 2), SEOUL, Random(7))

        assertEquals(0, card.variant)
    }

    @Test
    fun `시안 수가 줄어 고른 안이 없어졌으면 있는 안 가운데 다시 고른다`() = runBlocking {
        val stalePick = WidgetVariantPickVO(episodeKey = "N:$TODAY", chosen = 5)
        val repository = FakeWidgetRepository(
            nightSnapshot().copy(variantPicks = mapOf(WidgetStage.NIGHT to stalePick)),
        )

        val card = GetWidgetCardUseCase(repository)(threeVariants, at(TODAY, 2), SEOUL, Random(3))

        assertTrue(card.variant in 0..2)
    }

    @Test
    fun `상태마다 고른 안을 따로 기억한다`() = runBlocking {
        val waitingPick = WidgetVariantPickVO(episodeKey = "W:${TODAY.minusDays(1)}:LUNCH", chosen = 2)
        val repository = FakeWidgetRepository(
            nightSnapshot().copy(variantPicks = mapOf(WidgetStage.WAITING to waitingPick)),
        )

        GetWidgetCardUseCase(repository)(threeVariants, at(TODAY, 2), SEOUL, Random(3))

        assertEquals(waitingPick, repository.current.variantPicks[WidgetStage.WAITING])
        assertTrue(WidgetStage.NIGHT in repository.current.variantPicks)
    }

    @Test
    fun `로그인이 없으면 로그아웃 카드다`() = runBlocking {
        val repository = FakeWidgetRepository(nightSnapshot(), isLoggedIn = false)

        val card = GetWidgetCardUseCase(repository)({ 2 }, at(TODAY, 12), SEOUL, Random(0))

        assertEquals(WidgetStage.LOGGED_OUT, card.plan.stage)
        assertTrue(card.variant in 0..1)
    }

    @Test
    fun `시안 수는 계산한 상태로 묻는다`() = runBlocking {
        val asked = mutableListOf<WidgetPlanVO>()
        val repository = FakeWidgetRepository(nightSnapshot())

        GetWidgetCardUseCase(repository)({ plan -> asked += plan; 3 }, at(TODAY, 2), SEOUL, Random(0))

        assertEquals(WidgetStage.NIGHT, asked.single().stage)
    }

    private val threeVariants: (WidgetPlanVO) -> Int = { 3 }

    /** 어제 저녁에 기록해 오늘 새벽에는 밤 카드가 보이는 스냅샷. */
    private fun nightSnapshot() = WidgetSnapshotVO(lastRecordedAtMillis = at(TODAY.minusDays(1), 19))

    private fun at(date: LocalDate, hour: Int): Long =
        LocalDateTime.of(date, LocalTime.of(hour, 0)).atZone(SEOUL).toInstant().toEpochMilli()

    private class FakeWidgetRepository(
        var current: WidgetSnapshotVO,
        private val isLoggedIn: Boolean = true,
    ) : WidgetRepository {
        override suspend fun snapshot(): WidgetSnapshotVO = current

        override suspend fun update(transform: (WidgetSnapshotVO) -> WidgetSnapshotVO) {
            current = transform(current)
        }

        override suspend fun clear() {
            current = WidgetSnapshotVO.empty
        }

        override suspend fun isLoggedIn(): Boolean = isLoggedIn
    }

    private companion object {
        val SEOUL: ZoneId = ZoneId.of("Asia/Seoul")
        val TODAY: LocalDate = LocalDate.of(2026, 11, 4)
    }
}
