package com.dandi.nyummy.widget.domain

import com.dandi.nyummy.common.entity.meal.Meal
import com.dandi.nyummy.common.entity.meal.MealTimesVO
import com.dandi.nyummy.widget.entity.WidgetPlanVO
import com.dandi.nyummy.widget.entity.WidgetSnapshotVO
import com.dandi.nyummy.widget.entity.WidgetStage
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/**
 * 남겨 둔 값과 지금 시각으로 위젯 상태를 계산한다. 네트워크도 Android도 쓰지 않는 순수 함수다.
 *
 * 규칙은 OpenSpec `add-home-widget` design.md D2~D5를 따른다.
 * - 마지막 기록 날짜는 이 기기 기록과 홈 요약 힌트 중 늦은 쪽이다.
 * - 하루 이상을 통째로 걸렀으면 일 단위([WidgetStage.AWAY]), 아니면 끼니 단위로 본다.
 * - 끼니 단위는 먹는 끼니마다 기다림 시작(끼니 시각 − 1시간)·끼니 시각·마감(끼니 시각 + 2시간)으로 가른다.
 */
object WidgetPlanner {

    private const val WAIT_BEFORE_MINUTES = 60L
    private const val DEADLINE_AFTER_MINUTES = 120L

    fun plan(
        snapshot: WidgetSnapshotVO,
        isLoggedIn: Boolean,
        nowMillis: Long,
        zone: ZoneId,
    ): WidgetPlanVO {
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        if (!isLoggedIn) {
            return WidgetPlanVO(
                stage = WidgetStage.LOGGED_OUT,
                episodeKey = "L:$today",
                nextChangeAtMillis = today.plusDays(1).startMillis(zone),
            )
        }
        val nextChange = nextChangeAt(snapshot.mealTimes, today, zone, nowMillis)
        val record = LastRecord.of(snapshot, zone)

        val lastDate = record.date
        if (lastDate == null) {
            // 이 기기 기록도, 기록을 알려 주는 홈 요약도 없다.
            if (record.brokenAsOf != null) {
                // 홈 요약으로 연속이 끊긴 것을 처음 본 날부터 센다. 며칠째인지는 최솟값만 안다.
                val days = ChronoUnit.DAYS.between(record.brokenAsOf, today).toInt() + 1
                return away(days.coerceAtLeast(1), isExact = false, today = today, nextChange = nextChange)
            }
            return intraday(snapshot, record, today, zone, nowMillis, nextChange)
        }

        val daysSince = ChronoUnit.DAYS.between(lastDate, today).toInt().coerceAtLeast(0)
        if (daysSince >= 2) {
            return away(daysSince - 1, isExact = true, today = today, nextChange = nextChange)
        }
        return intraday(snapshot, record, today, zone, nowMillis, nextChange)
    }

    private fun away(days: Int, isExact: Boolean, today: LocalDate, nextChange: Long) = WidgetPlanVO(
        stage = WidgetStage.AWAY,
        awayDays = days,
        isAwayDaysExact = isExact,
        streakDays = 0,
        isStreakLit = false,
        episodeKey = "A:$today",
        nextChangeAtMillis = nextChange,
    )

    /** 연속 기록이 살아 있는 날(또는 아무것도 모르는 날)의 끼니 단위 판정. */
    private fun intraday(
        snapshot: WidgetSnapshotVO,
        record: LastRecord,
        today: LocalDate,
        zone: ZoneId,
        nowMillis: Long,
        nextChange: Long,
    ): WidgetPlanVO {
        val slots = slotsOf(snapshot.mealTimes, today, zone)
        val lastDate = record.date
        val recordedToday = lastDate == today
        // 오늘 기록만 오늘 끼니를 덮는다. 기다림 시작이 덮는 시각보다 이르거나 같은 끼니는 먹은 것으로 본다.
        val coveredUntil = if (recordedToday) record.coveredUntilMillis else null
        fun Slot.isCovered() = coveredUntil != null && coveredUntil >= waitStart

        val streakDays = if (lastDate == null) 0 else record.streakFor(lastDate)
        val base = WidgetPlanVO(
            streakDays = streakDays,
            isStreakLit = recordedToday,
            nextChangeAtMillis = nextChange,
        )

        val waiting = slots.firstOrNull { !it.isCovered() && nowMillis >= it.waitStart && nowMillis < it.deadline }
        if (waiting != null) {
            return base.copy(
                stage = WidgetStage.WAITING,
                meal = waiting.meal,
                isMealTimeReached = nowMillis >= waiting.mealTime,
                episodeKey = "W:$today:${waiting.meal}",
            )
        }

        val missed = slots.filter { !it.isCovered() && nowMillis >= it.deadline }
        if (missed.isNotEmpty()) {
            return when {
                !recordedToday && missed.size == slots.size ->
                    base.copy(stage = WidgetStage.MISSED_ALL, episodeKey = "MA:$today")

                missed.size >= 2 ->
                    base.copy(stage = WidgetStage.MISSED_TWO, episodeKey = "M2:$today")

                else -> base.copy(
                    stage = WidgetStage.MISSED_ONE,
                    meal = missed.last().meal,
                    episodeKey = "M1:$today:${missed.last().meal}",
                )
            }
        }

        if (recordedToday) {
            val localAt = record.localAtMillis
            val isReunion = localAt != null && snapshot.reunionRecordedAtMillis == localAt
            return base.copy(
                stage = if (isReunion) WidgetStage.REUNION else WidgetStage.FULL,
                // 이 기기 접수 하나가 한 회차다. 다른 기기 기록(홈 요약)만 알면 그날을 한 회차로 본다.
                episodeKey = when {
                    isReunion -> "R:$localAt"
                    localAt != null -> "F:$localAt"
                    else -> "F:$today"
                },
            )
        }

        val nextMeal = slots.firstOrNull { !it.isCovered() } ?: slots.first()
        return base.copy(
            stage = WidgetStage.NIGHT,
            meal = nextMeal.meal,
            episodeKey = "N:$today",
        )
    }

    /** 다음에 다시 그릴 순간. 오늘·내일의 기다림 시작, 끼니 시각, 마감, 자정 가운데 지금 뒤로 가장 이른 때다. */
    private fun nextChangeAt(mealTimes: MealTimesVO, today: LocalDate, zone: ZoneId, nowMillis: Long): Long {
        val tomorrow = today.plusDays(1)
        val candidates = (slotsOf(mealTimes, today, zone) + slotsOf(mealTimes, tomorrow, zone))
            .flatMap { listOf(it.waitStart, it.mealTime, it.deadline) } +
            listOf(tomorrow.startMillis(zone), tomorrow.plusDays(1).startMillis(zone))
        return candidates.filter { it > nowMillis }.min()
    }

    /** 그 날의 먹는 끼니. 모두 건너뛰었으면 위젯만 기본 시각으로 판정한다(하루 종일 밤으로 남지 않게). */
    private fun slotsOf(mealTimes: MealTimesVO, date: LocalDate, zone: ZoneId): List<Slot> {
        val eating = Meal.entries.filterNot { mealTimes[it].isSkipped }
        val times = if (eating.isEmpty()) MealTimesVO.default else mealTimes
        val meals = eating.ifEmpty { Meal.entries }
        val dayStart = date.startMillis(zone)
        val dayEnd = date.plusDays(1).startMillis(zone)
        return meals.map { meal ->
            val mealAt: ZonedDateTime = date.atTime(LocalTime.of(times[meal].hour.coerceIn(0, 23), 0)).atZone(zone)
            Slot(
                meal = meal,
                waitStart = maxOf(mealAt.minusMinutes(WAIT_BEFORE_MINUTES).toMillis(), dayStart),
                mealTime = mealAt.toMillis(),
                deadline = minOf(mealAt.plusMinutes(DEADLINE_AFTER_MINUTES).toMillis(), dayEnd),
            )
        }.sortedBy { it.mealTime }
    }

    private data class Slot(
        val meal: Meal,
        val waitStart: Long,
        val mealTime: Long,
        val deadline: Long,
    )

    /**
     * 마지막 기록(design.md D2). 날짜는 이 기기 기록과 홈 요약 힌트 중 늦은 쪽이다.
     *
     * 그날 끼니를 덮는 시각은 그 날짜에 이 기기 접수가 있으면 그 접수 시각이다. 홈 요약을 받은 시각은 식사 시각이 아니라서
     * (홈은 볼 때마다 요약을 다시 받는다), 이 기기 기록 없이 다른 기기에서 기록한 날에만 "받기 전까지는 먹었다"로 쓴다.
     *
     * @property date 마지막 기록 날짜. 힌트가 "전날 기록"만 알려 주면 전날이다.
     * @property coveredUntilMillis 그날 끼니를 덮는 시각. 기다림 시작이 이 시각보다 이르거나 같은 끼니는 먹은 것으로 본다.
     * @property localAtMillis 그날의 이 기기 접수 시각. 재회와 배빵빵 회차를 가른다. 다른 기기 기록만 알면 null이다.
     * @property brokenAsOf 홈 요약으로 연속 끊김을 처음 확인한 날짜
     * @property anchor 서버 연속 기록일이 가리키는 날짜와 일수. 마지막 기록 날짜와 견줘 보일 연속일을 정한다.
     */
    private class LastRecord(
        val date: LocalDate?,
        val coveredUntilMillis: Long?,
        val localAtMillis: Long?,
        val brokenAsOf: LocalDate?,
        private val anchor: Pair<LocalDate, Int>?,
    ) {
        fun streakFor(lastDate: LocalDate): Int {
            val (anchorDate, anchorStreak) = anchor ?: return 1
            return when {
                lastDate == anchorDate -> anchorStreak
                lastDate == anchorDate.plusDays(1) -> anchorStreak + 1
                lastDate.isAfter(anchorDate) -> 1
                else -> anchorStreak
            }
        }

        companion object {
            fun of(snapshot: WidgetSnapshotVO, zone: ZoneId): LastRecord {
                val local = snapshot.lastRecordedAtMillis.takeIf { it > 0L }
                val localDate = local?.toDate(zone)
                val hint = snapshot.homeHint
                if (hint.isEmpty) {
                    return LastRecord(localDate, local, local, brokenAsOf = null, anchor = null)
                }

                val fetchedDate = hint.fetchedAtMillis.toDate(zone)
                var hintDate: LocalDate? = null
                var hintCoveredUntil: Long? = null
                var anchor: Pair<LocalDate, Int>? = null
                when {
                    hint.todayRecordedCount > 0 -> {
                        hintDate = fetchedDate
                        hintCoveredUntil = hint.fetchedAtMillis
                        anchor = fetchedDate to hint.streakDays.coerceAtLeast(1)
                    }

                    hint.streakDays > 0 -> {
                        hintDate = fetchedDate.minusDays(1)
                        anchor = hintDate to hint.streakDays
                    }
                }
                val brokenAsOf = if (hint.isBroken) {
                    (hint.brokenSinceMillis.takeIf { it > 0L } ?: hint.fetchedAtMillis).toDate(zone)
                } else {
                    null
                }
                val date = when {
                    localDate == null -> hintDate
                    hintDate == null || !hintDate.isAfter(localDate) -> localDate
                    else -> hintDate
                }
                val fromLocal = localDate != null && localDate == date
                return LastRecord(
                    date = date,
                    coveredUntilMillis = if (fromLocal) local else hintCoveredUntil,
                    localAtMillis = if (fromLocal) local else null,
                    brokenAsOf = brokenAsOf,
                    anchor = anchor,
                )
            }
        }
    }

    private fun Long.toDate(zone: ZoneId): LocalDate = Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

    private fun LocalDate.startMillis(zone: ZoneId): Long = atStartOfDay(zone).toInstant().toEpochMilli()

    private fun ZonedDateTime.toMillis(): Long = toInstant().toEpochMilli()
}
