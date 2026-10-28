package com.dandi.nyummy.cat.domain

import com.dandi.nyummy.cat.entity.CatAnimationSetVO
import com.dandi.nyummy.cat.entity.CatLineVO
import com.dandi.nyummy.cat.entity.CatMotionVO
import com.dandi.nyummy.cat.entity.CatState
import com.dandi.nyummy.cat.entity.SpriteFrameVO
import java.time.Duration
import java.time.LocalDateTime
import javax.inject.Inject
import kotlin.random.Random

/**
 * 냐미의 분위기. 기록 여부와 상관없이 시각과 사용자가 건드린 지 얼마나 됐는지로만 정한다.
 *
 * @property states 동작을 고를 상황. 앞의 것이 비어 있으면 다음 것으로 내려간다.
 */
enum class CatMood(val states: List<CatState>) {
    /** 6시~23시. 일상 동작을 다채롭게 한다. */
    DAY(listOf(CatState.HOME_DAY)),

    /** 낮이라도 한동안 건드리지 않음. 엎드려 자거나 식빵을 굽는다. 누르면 다시 [DAY]다. */
    DROWSY(listOf(CatState.HOME_DROWSY, CatState.HOME_DAY)),

    /** 23시~6시. 졸린 동작을 하고 느긋하다. */
    NIGHT(listOf(CatState.HOME_NIGHT, CatState.HOME_DAY)),
}

/**
 * 고른 동작.
 *
 * @property state 동작을 고른 상황
 * @property frame 그 상황 시트의 프레임 규격
 * @property motion 재생할 동작
 */
data class CatPick(
    val state: CatState,
    val frame: SpriteFrameVO,
    val motion: CatMotionVO,
) {
    override fun toString(): String = "${state.name}.${motion.name}"
}

/**
 * 잠깐 끼어드는 반응. 앱이 확실히 아는 사건에만 한다.
 *
 * @property states 반응 동작을 고를 상황. 앞의 것이 비어 있으면 다음 것으로 내려가고, 다 비면 동작 없이 대사만 보인다.
 * @property lineTag 반응할 때 고를 대사 태그
 */
enum class CatReaction(val states: List<CatState>, val lineTag: String) {
    /** 오늘 처음 들어옴 */
    GREET(listOf(CatState.HOME_GREET), "greet"),

    /** 하루 넘어 들어옴 */
    WELCOME_BACK(listOf(CatState.HOME_WELCOME_BACK), "welcome_back"),

    /** 방금 기록하고 돌아옴 */
    EAT(listOf(CatState.HOME_AFTER_MEAL), "after_meal"),

    /** 냐미를 누름 */
    TAP(listOf(CatState.HOME_TAP, CatState.HOME_DAY), "tap"),

    /** 짧게 여러 번 누름. 귀찮아하거나 당황한다. */
    IRRITATED(listOf(CatState.HOME_TAP_BURST, CatState.HOME_TAP, CatState.HOME_DAY), "tap_burst"),
}

/**
 * 홈 냐미의 두뇌. 시각과 방치 시간으로 분위기를 정하고, 그 상황의 동작 목록에서 가중치로 동작을 고르고,
 * 반응 동작과 대사 은행의 대사를 고른다. 어떤 동작이 있고 얼마나 자주 나올지는 애니메이션([CatAnimationSetVO])이 정한다.
 * 고른 동작·대사 이력을 갖고 있어 같은 것이 연달아 나오지 않게 한다. 화면 하나에 하나씩 쓴다.
 */
class CatBrain(private val random: Random) {

    @Inject
    constructor() : this(Random.Default)

    /** 최근 분위기 동작 이름. 같은 동작이 여러 상황에 있어도 이름으로 알아본다. */
    private val recentMotions = ArrayDeque<String>()
    private val recentLines = ArrayDeque<String>()
    private var lastTap: String? = null

    /** 밤이면 [CatMood.NIGHT], 낮에 [idleMillis] 동안 건드리지 않았으면 [CatMood.DROWSY], 아니면 [CatMood.DAY]. */
    fun moodOf(now: LocalDateTime, idleMillis: Long = 0): CatMood = when {
        now.hour >= NIGHT_START_HOUR || now.hour < NIGHT_END_HOUR -> CatMood.NIGHT
        idleMillis >= DROWSY_AFTER_MILLIS -> CatMood.DROWSY
        else -> CatMood.DAY
    }

    /**
     * [mood] 상황에서 다음 동작을 가중치로 고른다. 직전 동작은 빼고, 최근에 본 동작은 덜 고른다.
     * 상황이 비어 있으면 다음 상황으로 내려가고, 다 비면 null이다.
     */
    fun nextMotion(mood: CatMood, set: CatAnimationSetVO): CatPick? {
        val (state, animation) = firstAvailable(mood.states, set) ?: return null
        val candidates = animation.motions.filter { it.name != recentMotions.lastOrNull() }.ifEmpty { animation.motions }
        val motion = weightedPick(candidates) { if (it.name in recentMotions) it.weight * RECENT_PENALTY else it.weight }
        recentMotions.addLast(motion.name)
        while (recentMotions.size > RECENT_MOTIONS) recentMotions.removeFirst()
        return CatPick(state, animation.frame, motion)
    }

    /** 반응 동작. 터치 반응은 바로 전 터치 반응을 빼고 고른다. 반응 상황이 다 비어 있으면 null이다. */
    fun reaction(reaction: CatReaction, set: CatAnimationSetVO): CatPick? {
        val (state, animation) = firstAvailable(reaction.states, set) ?: return null
        val isTap = reaction == CatReaction.TAP || reaction == CatReaction.IRRITATED
        val candidates = animation.motions.filter { !isTap || it.name != lastTap }.ifEmpty { animation.motions }
        val motion = weightedPick(candidates) { it.weight }
        if (isTap) lastTap = motion.name
        return CatPick(state, animation.frame, motion)
    }

    /** 동작이 끝난 뒤 다음 동작까지 쉬는 시간. 졸리거나 밤이면 길다. */
    fun restMillis(mood: CatMood): Long {
        val base = random.nextLong(MIN_REST_MILLIS, MAX_REST_MILLIS + 1)
        return if (mood == CatMood.DAY) base else (base * SLEEPY_REST_SCALE).toLong()
    }

    /** 일상 대사. 아침(6~11시)이면 아침 대사와 낮 대사를 섞는다. 최근 대사는 피한다. */
    fun moodLine(now: LocalDateTime, lines: List<CatLineVO>): String? {
        val tags = when {
            moodOf(now) == CatMood.NIGHT -> setOf(TAG_NIGHT)
            now.hour < MORNING_END_HOUR -> setOf(TAG_MORNING, TAG_DAY)
            else -> setOf(TAG_DAY)
        }
        return pickLine(lines, tags)
    }

    /**
     * 반응 대사. 고른 동작([motion])에 맞춘 대사(`<반응 태그>:<동작 이름>`, 예 `tap_burst:recoil`)가 있으면 그것을,
     * 없으면 반응 태그 대사를, 그것도 없으면 일상 대사를 고른다.
     */
    fun reactionLine(reaction: CatReaction, now: LocalDateTime, lines: List<CatLineVO>, motion: CatMotionVO? = null): String? =
        motion?.let { pickLine(lines, setOf("${reaction.lineTag}:${it.name}")) }
            ?: pickLine(lines, setOf(reaction.lineTag))
            ?: moodLine(now, lines)

    /** 들어올 때 보일 인사. 10분 안에 다시 왔으면 null, 하루 넘어 왔으면 반가움, 날짜가 바뀌었으면 인사. */
    fun greetingOf(lastSeen: LocalDateTime?, now: LocalDateTime): CatReaction? = when {
        lastSeen == null -> CatReaction.GREET
        Duration.between(lastSeen, now) < GREETING_COOLDOWN -> null
        Duration.between(lastSeen, now) >= Duration.ofDays(1) -> CatReaction.WELCOME_BACK
        lastSeen.toLocalDate() != now.toLocalDate() -> CatReaction.GREET
        else -> null
    }

    private fun firstAvailable(states: List<CatState>, set: CatAnimationSetVO) =
        states.firstNotNullOfOrNull { state -> set.animations[state]?.takeIf { it.motions.isNotEmpty() }?.let { state to it } }

    private fun weightedPick(motions: List<CatMotionVO>, weightOf: (CatMotionVO) -> Double): CatMotionVO {
        var roll = random.nextDouble() * motions.sumOf(weightOf)
        return motions.firstOrNull { roll -= weightOf(it); roll < 0 } ?: motions.last()
    }

    private fun pickLine(lines: List<CatLineVO>, tags: Set<String>): String? {
        val matching = lines.filter { line -> line.tags.any { it in tags } }.map { it.text }
        if (matching.isEmpty()) return null
        val candidates = matching.filter { it !in recentLines }.ifEmpty { matching }
        val line = candidates[random.nextInt(candidates.size)]
        recentLines.addLast(line)
        while (recentLines.size > RECENT_LINES) recentLines.removeFirst()
        return line
    }

    private companion object {
        const val NIGHT_START_HOUR = 23
        const val NIGHT_END_HOUR = 6
        const val MORNING_END_HOUR = 11
        const val RECENT_MOTIONS = 5
        const val RECENT_LINES = 10
        const val RECENT_PENALTY = 0.3
        const val MIN_REST_MILLIS = 1_000L
        const val MAX_REST_MILLIS = 3_000L
        const val SLEEPY_REST_SCALE = 1.5
        const val DROWSY_AFTER_MILLIS = 20_000L
        const val TAG_MORNING = "morning"
        const val TAG_DAY = "day"
        const val TAG_NIGHT = "night"
        val GREETING_COOLDOWN: Duration = Duration.ofMinutes(10)
    }
}
