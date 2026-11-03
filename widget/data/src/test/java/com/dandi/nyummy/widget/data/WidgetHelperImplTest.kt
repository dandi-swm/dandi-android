package com.dandi.nyummy.widget.data

import com.dandi.nyummy.common.entity.meal.Meal
import com.dandi.nyummy.common.entity.meal.MealTimeVO
import com.dandi.nyummy.common.entity.meal.MealTimesVO
import com.dandi.nyummy.widget.domain.WidgetRefresher
import com.dandi.nyummy.widget.domain.WidgetRepository
import com.dandi.nyummy.widget.entity.WidgetHomeHintVO
import com.dandi.nyummy.widget.entity.WidgetSnapshotVO
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlin.coroutines.cancellation.CancellationException

class WidgetHelperImplTest {

    private val refresher = RecordingRefresher()

    @Test
    fun `식사가 접수되면 그 순간을 마지막 기록으로 남기고 다시 그린다`() = runBlocking {
        val repository = FakeWidgetRepository(WidgetSnapshotVO(lastRecordedAtMillis = at(TODAY.minusDays(1), 19)))

        helper(repository, now = at(TODAY, 12, 30)).onMealRecorded()

        assertEquals(at(TODAY, 12, 30), repository.current.lastRecordedAtMillis)
        assertEquals(1, refresher.count)
    }

    @Test
    fun `기기 시계가 뒤로 가도 마지막 기록은 앞당기지 않는다`() = runBlocking {
        val repository = FakeWidgetRepository(WidgetSnapshotVO(lastRecordedAtMillis = at(TODAY, 13)))

        helper(repository, now = at(TODAY, 12)).onMealRecorded()

        assertEquals(at(TODAY, 13), repository.current.lastRecordedAtMillis)
    }

    @Test
    fun `연속이 끊긴 채 기록하면 재회로 남긴다`() = runBlocking {
        val repository = FakeWidgetRepository(WidgetSnapshotVO(lastRecordedAtMillis = at(TODAY.minusDays(3), 19)))

        helper(repository, now = at(TODAY, 12, 30)).onMealRecorded()

        assertEquals(at(TODAY, 12, 30), repository.current.reunionRecordedAtMillis)
    }

    @Test
    fun `며칠째인지 모르는 일 단위에서 기록해도 재회로 남긴다`() = runBlocking {
        val brokenHint = WidgetHomeHintVO(fetchedAtMillis = at(TODAY, 9), streakDays = 0, todayRecordedCount = 0)
        val repository = FakeWidgetRepository(WidgetSnapshotVO(homeHint = brokenHint))

        helper(repository, now = at(TODAY, 12, 30)).onMealRecorded()

        assertEquals(at(TODAY, 12, 30), repository.current.reunionRecordedAtMillis)
    }

    @Test
    fun `연속이 살아 있을 때 기록하면 재회로 남기지 않는다`() = runBlocking {
        val repository = FakeWidgetRepository(WidgetSnapshotVO(lastRecordedAtMillis = at(TODAY.minusDays(1), 19)))

        helper(repository, now = at(TODAY, 12, 30)).onMealRecorded()

        assertEquals(0L, repository.current.reunionRecordedAtMillis)
    }

    @Test
    fun `재회 뒤 같은 날 다시 기록해도 재회 시각은 그대로다`() = runBlocking {
        val reunionAt = at(TODAY, 12, 30)
        val repository = FakeWidgetRepository(
            WidgetSnapshotVO(lastRecordedAtMillis = reunionAt, reunionRecordedAtMillis = reunionAt),
        )

        helper(repository, now = at(TODAY, 18, 30)).onMealRecorded()

        assertEquals(at(TODAY, 18, 30), repository.current.lastRecordedAtMillis)
        assertEquals(reunionAt, repository.current.reunionRecordedAtMillis)
    }

    @Test
    fun `홈 요약을 받은 순간과 함께 연속일과 오늘 기록 수를 남긴다`() = runBlocking {
        val repository = FakeWidgetRepository(WidgetSnapshotVO.empty)

        helper(repository, now = at(TODAY, 9)).onHomeSummary(streakDays = 12, todayRecordedCount = -1)

        assertEquals(
            WidgetHomeHintVO(fetchedAtMillis = at(TODAY, 9), streakDays = 12, todayRecordedCount = 0),
            repository.current.homeHint,
        )
        assertEquals(1, refresher.count)
    }

    @Test
    fun `저장한 끼니 시각을 남긴다`() = runBlocking {
        val repository = FakeWidgetRepository(WidgetSnapshotVO.empty)
        val lunchAt13 = MealTimesVO.default.with(Meal.LUNCH, MealTimeVO(hour = 13))

        helper(repository, now = at(TODAY, 9)).onMealTimesSaved(lunchAt13)

        assertEquals(lunchAt13, repository.current.mealTimes)
        assertEquals(1, refresher.count)
    }

    @Test
    fun `비우면 남긴 값을 모두 지우고 다시 그린다`() = runBlocking {
        val repository = FakeWidgetRepository(
            WidgetSnapshotVO(lastRecordedAtMillis = at(TODAY, 8), mealTimes = MealTimesVO.default.with(Meal.LUNCH, MealTimeVO(hour = 13))),
        )

        helper(repository, now = at(TODAY, 9)).clear()

        assertEquals(WidgetSnapshotVO.empty, repository.current)
        assertEquals(1, refresher.count)
    }

    @Test
    fun `저장에 실패해도 예외를 넘기지 않고 다시 그리지도 않는다`() = runBlocking {
        val repository = FakeWidgetRepository(WidgetSnapshotVO.empty, failure = IOException("disk full"))
        val helper = helper(repository, now = at(TODAY, 9))

        helper.onMealRecorded()
        helper.onHomeSummary(streakDays = 1, todayRecordedCount = 1)
        helper.onMealTimesSaved(MealTimesVO.default)
        helper.clear()

        assertEquals(0, refresher.count)
    }

    @Test
    fun `취소는 삼키지 않는다`() {
        val repository = FakeWidgetRepository(WidgetSnapshotVO.empty, failure = CancellationException("cancelled"))

        val thrown = runCatching { runBlocking { helper(repository, now = at(TODAY, 9)).onMealRecorded() } }

        assertTrue(thrown.exceptionOrNull() is CancellationException)
    }

    private fun helper(repository: WidgetRepository, now: Long) =
        WidgetHelperImpl(repository, refresher, nowMillis = { now }, zone = { SEOUL })

    private fun at(date: LocalDate, hour: Int, minute: Int = 0): Long =
        LocalDateTime.of(date, LocalTime.of(hour, minute)).atZone(SEOUL).toInstant().toEpochMilli()

    private class FakeWidgetRepository(
        var current: WidgetSnapshotVO,
        private val failure: Exception? = null,
    ) : WidgetRepository {
        override suspend fun snapshot(): WidgetSnapshotVO = current

        override suspend fun update(transform: (WidgetSnapshotVO) -> WidgetSnapshotVO) {
            failure?.let { throw it }
            current = transform(current)
        }

        override suspend fun clear() {
            failure?.let { throw it }
            current = WidgetSnapshotVO.empty
        }

        override suspend fun isLoggedIn(): Boolean = true
    }

    private class RecordingRefresher : WidgetRefresher {
        var count = 0
        override fun requestRefresh() {
            count++
        }
    }

    private companion object {
        val SEOUL: ZoneId = ZoneId.of("Asia/Seoul")
        val TODAY: LocalDate = LocalDate.of(2026, 11, 4)
    }
}
