package com.dandi.nyummy.widget.domain

import com.dandi.nyummy.common.entity.meal.Meal
import com.dandi.nyummy.common.entity.meal.MealTimeVO
import com.dandi.nyummy.common.entity.meal.MealTimesVO
import com.dandi.nyummy.widget.entity.WidgetHomeHintVO
import com.dandi.nyummy.widget.entity.WidgetPlanVO
import com.dandi.nyummy.widget.entity.WidgetSnapshotVO
import com.dandi.nyummy.widget.entity.WidgetStage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

/** OpenSpec `add-home-widget` specs의 상태 판정 시나리오. 기본 끼니 시각은 8 / 12 / 18시다. */
class WidgetPlannerTest {

    // ── 로그아웃 ──────────────────────────────────────────────

    @Test
    fun `로그인하지 않았으면 로그아웃 카드이고 내일 자정에 다시 계산한다`() {
        val plan = plan(recordedYesterday(19), now = at(TODAY, 12), isLoggedIn = false)

        assertEquals(WidgetStage.LOGGED_OUT, plan.stage)
        assertEquals(at(TODAY.plusDays(1), 0), plan.nextChangeAtMillis)
    }

    // ── 밤 ────────────────────────────────────────────────────

    @Test
    fun `어제 기록하고 새벽이면 밤 카드이고 다음 끼니는 아침, 스트릭은 흐리다`() {
        val plan = plan(recordedYesterday(19), now = at(TODAY, 2))

        assertEquals(WidgetStage.NIGHT, plan.stage)
        assertEquals(Meal.BREAKFAST, plan.meal)
        assertFalse(plan.isStreakLit)
        assertEquals(at(TODAY, 7), plan.nextChangeAtMillis)
    }

    @Test
    fun `새벽에 기록하면 배빵빵이고 아침 기다림이 시작되면 기다림이 된다`() {
        val snapshot = recordedToday(1)

        val dawn = plan(snapshot, now = at(TODAY, 1, 30))
        assertEquals(WidgetStage.FULL, dawn.stage)
        assertTrue(dawn.isStreakLit)

        val morning = plan(snapshot, now = at(TODAY, 7))
        assertEquals(WidgetStage.WAITING, morning.stage)
        assertEquals(Meal.BREAKFAST, morning.meal)
    }

    // ── 기다림 ────────────────────────────────────────────────

    @Test
    fun `끼니 한 시간 전부터 마감 전까지 기다림이고 끼니 시각부터는 시각 도달로 표시한다`() {
        val snapshot = recordedToday(8, 10)

        val before = plan(snapshot, now = at(TODAY, 11))
        assertEquals(WidgetStage.WAITING, before.stage)
        assertEquals(Meal.LUNCH, before.meal)
        assertFalse(before.isMealTimeReached)
        assertEquals("W:$TODAY:LUNCH", before.episodeKey)

        val after = plan(snapshot, now = at(TODAY, 12, 30))
        assertEquals(WidgetStage.WAITING, after.stage)
        assertTrue(after.isMealTimeReached)
        assertEquals(before.episodeKey, after.episodeKey)

        assertEquals(WidgetStage.WAITING, plan(snapshot, now = at(TODAY, 13, 59)).stage)
    }

    @Test
    fun `앞 끼니를 놓쳤어도 다음 끼니 기다림이 먼저다`() {
        val snapshot = recordedYesterday(19)

        val missed = plan(snapshot, now = at(TODAY, 10, 30))
        assertEquals(WidgetStage.MISSED_ONE, missed.stage)
        assertEquals(Meal.BREAKFAST, missed.meal)

        val waiting = plan(snapshot, now = at(TODAY, 11))
        assertEquals(WidgetStage.WAITING, waiting.stage)
        assertEquals(Meal.LUNCH, waiting.meal)
    }

    @Test
    fun `끼니 시각 전에 미리 먹으면 그 끼니는 먹은 것이다`() {
        val snapshot = recordedToday(11, 20)

        assertEquals(WidgetStage.FULL, plan(snapshot, now = at(TODAY, 11, 30)).stage)
        assertEquals(WidgetStage.FULL, plan(snapshot, now = at(TODAY, 14, 30)).stage)
    }

    // ── 배빵빵 ────────────────────────────────────────────────

    @Test
    fun `아침을 기록하면 점심 기다림 전까지 배빵빵이고 스트릭은 흰색이다`() {
        val snapshot = recordedToday(8, 10)

        val full = plan(snapshot, now = at(TODAY, 10, 59))
        assertEquals(WidgetStage.FULL, full.stage)
        assertTrue(full.isStreakLit)
        assertEquals(WidgetStage.WAITING, plan(snapshot, now = at(TODAY, 11)).stage)
    }

    @Test
    fun `저녁을 기록하면 자정까지 배빵빵이다`() {
        val plan = plan(recordedToday(19), now = at(TODAY, 23, 59))

        assertEquals(WidgetStage.FULL, plan.stage)
        assertEquals(at(TODAY.plusDays(1), 0), plan.nextChangeAtMillis)
    }

    // ── 놓침 ──────────────────────────────────────────────────

    @Test
    fun `아침만 먹고 점심 마감이 지나면 한 끼 놓침이고 스트릭은 흰색이다`() {
        val plan = plan(recordedToday(8, 10), now = at(TODAY, 14))

        assertEquals(WidgetStage.MISSED_ONE, plan.stage)
        assertEquals(Meal.LUNCH, plan.meal)
        assertTrue(plan.isStreakLit)
    }

    @Test
    fun `아침만 먹고 저녁 마감이 지나면 자정까지 두 끼 놓침이다`() {
        val snapshot = recordedToday(8, 10)

        assertEquals(WidgetStage.MISSED_TWO, plan(snapshot, now = at(TODAY, 20)).stage)
        assertEquals(WidgetStage.MISSED_TWO, plan(snapshot, now = at(TODAY, 23, 59)).stage)
    }

    @Test
    fun `오늘 기록 없이 저녁 마감이 지나면 집사를 찾습니다이고 스트릭은 흐리다`() {
        val plan = plan(recordedYesterday(19), now = at(TODAY, 20))

        assertEquals(WidgetStage.MISSED_ALL, plan.stage)
        assertFalse(plan.isStreakLit)
    }

    @Test
    fun `아침을 놓치고 점심을 먹으면 아침 놓침을 다시 세지 않는다`() {
        val snapshot = recordedToday(12, 30)

        assertEquals(WidgetStage.FULL, plan(snapshot, now = at(TODAY, 13)).stage)
        assertEquals(WidgetStage.FULL, plan(snapshot, now = at(TODAY, 16, 59)).stage)
    }

    // ── 끼니 시각 ─────────────────────────────────────────────

    @Test
    fun `아침을 건너뛰면 아침 기다림이 없다`() {
        val plan = plan(recordedYesterday(19, mealTimes = SKIP_BREAKFAST), now = at(TODAY, 7, 30))

        assertEquals(WidgetStage.NIGHT, plan.stage)
        assertEquals(Meal.LUNCH, plan.meal)
    }

    @Test
    fun `아침을 건너뛰는 사용자가 점심과 저녁을 놓치면 집사를 찾습니다다`() {
        val plan = plan(recordedYesterday(19, mealTimes = SKIP_BREAKFAST), now = at(TODAY, 20))

        assertEquals(WidgetStage.MISSED_ALL, plan.stage)
    }

    @Test
    fun `세 끼를 모두 건너뛰면 위젯만 기본 시각으로 판정한다`() {
        val allSkipped = MealTimesVO(
            breakfast = MealTimeVO(hour = 9, isSkipped = true),
            lunch = MealTimeVO(hour = 13, isSkipped = true),
            dinner = MealTimeVO(hour = 19, isSkipped = true),
        )
        val plan = plan(recordedYesterday(19, mealTimes = allSkipped), now = at(TODAY, 11))

        assertEquals(WidgetStage.WAITING, plan.stage)
        assertEquals(Meal.LUNCH, plan.meal)
    }

    @Test
    fun `끼니 시각을 저장한 적 없으면 12시 점심을 기준으로 11시에 기다림이다`() {
        val plan = plan(WidgetSnapshotVO.empty, now = at(TODAY, 11))

        assertEquals(WidgetStage.WAITING, plan.stage)
        assertEquals(Meal.LUNCH, plan.meal)
    }

    @Test
    fun `점심을 13시로 저장하면 12시에 점심 기다림이 시작된다`() {
        val lunchAt13 = MealTimesVO.default.with(Meal.LUNCH, MealTimeVO(hour = 13))
        val snapshot = recordedToday(8, 10, mealTimes = lunchAt13)

        assertEquals(WidgetStage.FULL, plan(snapshot, now = at(TODAY, 11, 30)).stage)
        val waiting = plan(snapshot, now = at(TODAY, 12))
        assertEquals(WidgetStage.WAITING, waiting.stage)
        assertEquals(Meal.LUNCH, waiting.meal)
    }

    // ── 일 단위 ───────────────────────────────────────────────

    @Test
    fun `하루를 통째로 거르면 하루 동안 1일 일 단위이고 스트릭은 0일이다`() {
        val snapshot = recorded(TODAY.minusDays(2), 19)

        val morning = plan(snapshot, now = at(TODAY, 8))
        assertEquals(WidgetStage.AWAY, morning.stage)
        assertEquals(1, morning.awayDays)
        assertTrue(morning.isAwayDaysExact)
        assertEquals(0, morning.streakDays)
        assertFalse(morning.isStreakLit)

        val night = plan(snapshot, now = at(TODAY, 23))
        assertEquals(WidgetStage.AWAY, night.stage)
        assertEquals(morning.episodeKey, night.episodeKey)
    }

    @Test
    fun `열흘째 기록하지 않으면 거른 날 수가 7일을 넘는다`() {
        val plan = plan(recorded(TODAY.minusDays(10), 19), now = at(TODAY, 9))

        assertEquals(WidgetStage.AWAY, plan.stage)
        assertEquals(9, plan.awayDays)
        assertTrue(plan.awayDays >= 7)
    }

    @Test
    fun `며칠째인지 모르면 최솟값으로 보고 날수를 모른다고 표시하며 하루마다 하나씩 올린다`() {
        val snapshot = WidgetSnapshotVO(homeHint = hint(at(TODAY, 10), streakDays = 0, todayRecordedCount = 0))

        val today = plan(snapshot, now = at(TODAY, 12))
        assertEquals(WidgetStage.AWAY, today.stage)
        assertEquals(1, today.awayDays)
        assertFalse(today.isAwayDaysExact)

        val tomorrow = plan(snapshot, now = at(TODAY.plusDays(1), 8))
        assertEquals(2, tomorrow.awayDays)
        assertFalse(tomorrow.isAwayDaysExact)
    }

    // ── 재회 ──────────────────────────────────────────────────

    @Test
    fun `연속이 끊긴 뒤 기록하면 다음 기다림까지 재회이고 그 뒤는 기다림이다`() {
        val recordedAt = at(TODAY, 12, 30)
        val snapshot = WidgetSnapshotVO(lastRecordedAtMillis = recordedAt, reunionRecordedAtMillis = recordedAt)

        val reunion = plan(snapshot, now = at(TODAY, 12, 40))
        assertEquals(WidgetStage.REUNION, reunion.stage)
        assertTrue(reunion.isStreakLit)
        assertEquals(1, reunion.streakDays)
        assertEquals(WidgetStage.REUNION, plan(snapshot, now = at(TODAY, 16, 59)).stage)

        val dinner = plan(snapshot, now = at(TODAY, 17))
        assertEquals(WidgetStage.WAITING, dinner.stage)
        assertEquals(Meal.DINNER, dinner.meal)
    }

    @Test
    fun `재회 뒤 다음 기록부터는 배빵빵이다`() {
        val snapshot = WidgetSnapshotVO(
            lastRecordedAtMillis = at(TODAY, 18, 30),
            reunionRecordedAtMillis = at(TODAY, 12, 30),
        )

        assertEquals(WidgetStage.FULL, plan(snapshot, now = at(TODAY, 19)).stage)
    }

    @Test
    fun `연속이 살아 있을 때 기록하면 재회가 아니라 배빵빵이다`() {
        val plan = plan(recordedToday(12, 30), now = at(TODAY, 13))

        assertEquals(WidgetStage.FULL, plan.stage)
    }

    // ── 홈 요약 힌트 ──────────────────────────────────────────

    @Test
    fun `홈 요약에 연속만 있으면 어제 기록한 것으로 보고 오늘 끼니 단계와 흐린 연속일을 보인다`() {
        val snapshot = WidgetSnapshotVO(homeHint = hint(at(TODAY, 10, 30), streakDays = 12, todayRecordedCount = 0))

        val plan = plan(snapshot, now = at(TODAY, 11))
        assertEquals(WidgetStage.WAITING, plan.stage)
        assertEquals(Meal.LUNCH, plan.meal)
        assertEquals(12, plan.streakDays)
        assertFalse(plan.isStreakLit)
    }

    @Test
    fun `홈 요약에 오늘 기록이 있으면 다른 기기에서 오늘 기록한 것으로 보고 스트릭을 흰색으로 보인다`() {
        val snapshot = WidgetSnapshotVO(homeHint = hint(at(TODAY, 9), streakDays = 5, todayRecordedCount = 1))

        val plan = plan(snapshot, now = at(TODAY, 9, 30))
        assertEquals(WidgetStage.FULL, plan.stage)
        assertTrue(plan.isStreakLit)
        assertEquals(5, plan.streakDays)
    }

    @Test
    fun `어제까지 연속이면 오늘 이 기기에서 기록한 날을 하루 더 센다`() {
        val snapshot = WidgetSnapshotVO(
            lastRecordedAtMillis = at(TODAY, 8, 10),
            homeHint = hint(at(TODAY.minusDays(1), 20), streakDays = 12, todayRecordedCount = 2),
        )

        assertEquals(13, plan(snapshot, now = at(TODAY, 9)).streakDays)
    }

    @Test
    fun `오늘 기록 뒤 홈 요약을 다시 받아도 연속일이 같다`() {
        val snapshot = WidgetSnapshotVO(
            lastRecordedAtMillis = at(TODAY, 8, 10),
            homeHint = hint(at(TODAY, 8, 20), streakDays = 13, todayRecordedCount = 1),
        )

        assertEquals(13, plan(snapshot, now = at(TODAY, 9)).streakDays)
    }

    @Test
    fun `이 기기 기록이 홈 요약보다 늦으면 이 기기 기록을 쓴다`() {
        val snapshot = WidgetSnapshotVO(
            lastRecordedAtMillis = at(TODAY, 8, 10),
            homeHint = hint(at(TODAY, 7), streakDays = 12, todayRecordedCount = 0),
        )

        val plan = plan(snapshot, now = at(TODAY, 9))
        assertEquals(WidgetStage.FULL, plan.stage)
        assertTrue(plan.isStreakLit)
        assertEquals(13, plan.streakDays)
    }

    // ── 타임존·자정·다시 그릴 시각 ─────────────────────────────

    @Test
    fun `같은 순간이라도 기기 타임존의 시각으로 판정한다`() {
        val snapshot = recordedYesterday(19)
        val now = at(TODAY, 12) // 한국 12시 = UTC 3시

        assertEquals(WidgetStage.WAITING, WidgetPlanner.plan(snapshot, true, now, SEOUL).stage)
        assertEquals(WidgetStage.NIGHT, WidgetPlanner.plan(snapshot, true, now, ZoneOffset.UTC).stage)
    }

    @Test
    fun `자정이 지나면 전날 배빵빵이 새 날의 밤으로 바뀐다`() {
        val snapshot = recordedToday(19)

        val before = plan(snapshot, now = at(TODAY, 23, 59))
        val after = plan(snapshot, now = at(TODAY.plusDays(1), 0))

        assertEquals(WidgetStage.FULL, before.stage)
        assertEquals(WidgetStage.NIGHT, after.stage)
        assertNotEquals(before.episodeKey, after.episodeKey)
        assertEquals("N:${TODAY.plusDays(1)}", after.episodeKey)
    }

    @Test
    fun `다음에 다시 그릴 시각은 지금 뒤의 가장 이른 경계다`() {
        val snapshot = recordedToday(8, 10)

        assertEquals(at(TODAY, 10), plan(snapshot, now = at(TODAY, 8, 10)).nextChangeAtMillis)
        assertEquals(at(TODAY, 12), plan(snapshot, now = at(TODAY, 11)).nextChangeAtMillis)
        assertEquals(at(TODAY, 14), plan(snapshot, now = at(TODAY, 12)).nextChangeAtMillis)
        assertEquals(at(TODAY.plusDays(1), 0), plan(snapshot, now = at(TODAY, 20, 30)).nextChangeAtMillis)
    }

    @Test
    fun `일 단위에서도 자정에 다시 그려 날수를 올린다`() {
        val plan = plan(recorded(TODAY.minusDays(2), 19), now = at(TODAY, 20, 30))

        assertEquals(at(TODAY.plusDays(1), 0), plan.nextChangeAtMillis)
    }

    // ── 도우미 ────────────────────────────────────────────────

    private fun plan(snapshot: WidgetSnapshotVO, now: Long, isLoggedIn: Boolean = true): WidgetPlanVO =
        WidgetPlanner.plan(snapshot, isLoggedIn = isLoggedIn, nowMillis = now, zone = SEOUL)

    private fun recorded(date: LocalDate, hour: Int, minute: Int = 0, mealTimes: MealTimesVO = MealTimesVO.default) =
        WidgetSnapshotVO(lastRecordedAtMillis = at(date, hour, minute), mealTimes = mealTimes)

    private fun recordedToday(hour: Int, minute: Int = 0, mealTimes: MealTimesVO = MealTimesVO.default) =
        recorded(TODAY, hour, minute, mealTimes)

    private fun recordedYesterday(hour: Int, mealTimes: MealTimesVO = MealTimesVO.default) =
        recorded(TODAY.minusDays(1), hour, mealTimes = mealTimes)

    private fun hint(fetchedAt: Long, streakDays: Int, todayRecordedCount: Int) =
        WidgetHomeHintVO(fetchedAtMillis = fetchedAt, streakDays = streakDays, todayRecordedCount = todayRecordedCount)

    private fun at(date: LocalDate, hour: Int, minute: Int = 0): Long =
        LocalDateTime.of(date, java.time.LocalTime.of(hour, minute)).atZone(SEOUL).toInstant().toEpochMilli()

    private companion object {
        val SEOUL: ZoneId = ZoneId.of("Asia/Seoul")
        val TODAY: LocalDate = LocalDate.of(2026, 11, 4)
        val SKIP_BREAKFAST: MealTimesVO = MealTimesVO.default.with(Meal.BREAKFAST, MealTimeVO(hour = 8, isSkipped = true))
    }
}
