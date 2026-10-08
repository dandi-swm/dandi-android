package com.dandi.nyummy.cat.domain

import javax.inject.Inject
import kotlin.random.Random

/**
 * 냐미가 다음에 할 동작과 대사를 고른다.
 *
 * 같은 동작이나 대사가 연달아 나오지 않게 하는 최소 규칙만 둔다.
 * 시간대나 최근에 본 동작까지 따지는 선택 규칙이 생기면 이 클래스를 바꾼다.
 */
class CatMotionPicker(private val random: Random) {

    @Inject
    constructor() : this(Random.Default)

    /**
     * 동작 묶음 [count]개 중 하나의 번호를 고른다. 묶음이 둘 이상이면 바로 전 묶음 [previous]는 고르지 않는다.
     * 묶음이 없으면 0을 돌려준다.
     */
    fun nextGroup(count: Int, previous: Int?): Int {
        if (count <= 1) return 0
        val candidates = (0 until count).filter { it != previous }
        return candidates[random.nextInt(candidates.size)]
    }

    /** 대사 하나를 고른다. 대사가 둘 이상이면 바로 전 대사 [previous]는 고르지 않는다. 대사가 없으면 null이다. */
    fun nextLine(lines: List<String>, previous: String?): String? {
        if (lines.isEmpty()) return null
        val candidates = lines.filter { it != previous }.ifEmpty { lines }
        return candidates[random.nextInt(candidates.size)]
    }

    /** 동작 하나가 끝난 뒤 다음 동작을 시작하기까지 쉬는 시간. 매번 조금씩 달라서 규칙적으로 움직이는 느낌을 줄인다. */
    fun restMillis(): Long = random.nextLong(MIN_REST_MILLIS, MAX_REST_MILLIS + 1)

    companion object {
        const val MIN_REST_MILLIS = 6_000L
        const val MAX_REST_MILLIS = 12_000L
    }
}
