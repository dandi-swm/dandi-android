package com.dandi.nyummy.cat.domain.rig

import com.dandi.nyummy.cat.domain.rig.RigMath.clamp
import com.dandi.nyummy.cat.domain.rig.RigMath.ease
import com.dandi.nyummy.cat.domain.rig.RigMath.lerp
import com.dandi.nyummy.cat.domain.rig.RigMath.wrapAngle
import com.dandi.nyummy.cat.entity.CatState
import com.dandi.nyummy.cat.entity.rig.BlendKind
import com.dandi.nyummy.cat.entity.rig.CatBone
import com.dandi.nyummy.cat.entity.rig.ChannelMode
import com.dandi.nyummy.cat.entity.rig.Interp
import com.dandi.nyummy.cat.entity.rig.RigChannel
import com.dandi.nyummy.cat.entity.rig.RigClip
import com.dandi.nyummy.cat.entity.rig.RigSegment
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * 리그 고양이의 상태 머신. 동작(클립)을 재생하고, 바뀔 때 포즈를 섞고, 기본 생기(숨, 깜빡임, 꼬리)와 2차 움직임(귀, 꼬리, 매듭 스프링)을 얹는다.
 *
 * 층 순서: 클립 층(블렌딩 포함) -> 기본 생기 층 -> 스프링. 결과는 [pose].
 *
 * 떨림 방지:
 * - 시간을 고정 하위 단계([STEP] = 1/240초)로만 진행한다. 화면 주사율이나 프레임이 끊겨도 같은 시각의 포즈는 같다.
 * - 스프링은 반암시적 오일러로 같은 하위 단계에서 풀어 결정적이고 안정하다(가장 빠른 귀도 약 2Hz).
 * - 상태 전환은 지금 그려진 클립 층을 스냅샷으로 잡고 새 클립과 섞는다(기준서 4-4). 블렌드 도중 또 바뀌면 섞인 값을 다시 잡는다.
 *
 * 재생 계약은 스프라이트 재생([com.dandi.nyummy.cat.entity.CatMotionVO])과 같다:
 * 들어가기 -> 되풀이 x loopTimes -> 돌아오기 -> 쉬기(마지막이 되풀이면 쉬는 동안 되풀이) -> [motionFinished].
 *
 * @param skeleton 머리 속도(2차 움직임)를 잴 스켈레톤
 * @param random 깜빡임, 귀 씰룩임 간격. 테스트는 씨앗을 고정한다.
 */
class CatRigAnimator(skeleton: CatSkeleton, private val random: Random = Random.Default) {

    private var solver = CatPoseSolver(skeleton)

    /** 지금 포즈(채널 값, [RigChannel.ordinal] 순서). [advance] 뒤에 읽는다. */
    val pose = FloatArray(RigChannel.count)

    /** 시작부터 흐른 시간(초). 하위 단계의 배수다. */
    var time: Double = 0.0
        private set

    /** 지금 재생 중인 클립 */
    var clip: RigClip = CatClipLibrary.clips.getValue(CatClipLibrary.IDLE)
        private set

    /** 지금 동작이 쉬는 시간까지 끝났는지. 끝나도 마지막이 되풀이면 계속 되풀이한다. */
    val motionFinished: Boolean get() = elapsed >= total

    /** 블렌드 진행률(0..1, 블렌드 중이 아니면 1). 블렌드 길이는 [blendDurationSeconds]. */
    val blendProgress: Float get() = if (blendDuration <= 0f) 1f else clamp(blendElapsed / blendDuration, 0f, 1f)

    /** 마지막 전환의 블렌드 길이(초) */
    val blendDurationSeconds: Float get() = blendDuration

    private val clipLayer = FloatArray(RigChannel.count) { RigChannel.entries[it].rest }
    private val previousLayer = FloatArray(RigChannel.count) { RigChannel.entries[it].rest }
    private val layerVelocity = FloatArray(RigChannel.count)
    private val target = FloatArray(RigChannel.count)
    private val projected = FloatArray(RigChannel.count)
    private var blendElapsed = 0f
    private var blendDuration = 0f

    /** 관성 블렌딩: 채널마다 남은 차이를 5차 다항식으로 줄인다(값, 속도, 가속도가 이어진다). */
    private val inertia = Array(RigChannel.count) { Inertia() }

    private var elapsed = 0f

    /** 새 클립을 늦게 시작하는 시간(초). 자세 차이가 클 때만 0보다 크다. */
    private var clipDelay = 0f
    private var loopCount = 0
    private var total = 0f
    private var accumulator = 0f

    private val earL = Spring(EAR_K, EAR_C)
    private val earR = Spring(EAR_K, EAR_C)
    private val tail = Spring(TAIL_K, TAIL_C)
    private val knot = Spring(KNOT_K, KNOT_C)
    private var springsReady = false
    private var prevHeadX = Float.NaN
    private var prevHeadY = Float.NaN
    private var headVx = 0f
    private var headUp = 0f

    private var nextBlink = 1.2
    private var blinkT = -1.0
    private var nextTwitch = 3.0
    private var sleepiness = 0f
    private var twitchT = -1.0
    private var twitchLeft = true
    private var twitchAmp = 0f
    private var breathAcc = 0.0
    private var lastBreathT = 0.0

    init {
        planTotal(0f)
        evaluateClip(clipLayer)
        compose(0f)
    }

    /** 체형이 바뀌면 머리 속도를 새 스켈레톤으로 잰다. 포즈와 스프링은 이어 간다. */
    fun setSkeleton(skeleton: CatSkeleton) {
        solver = CatPoseSolver(skeleton)
        prevHeadX = Float.NaN
    }

    /**
     * 동작 [motion](스프라이트 동작 이름)을 재생한다. 클립은 [CatClipLibrary.clipFor]로 찾는다(못 찾으면 상황 기본, 그것도 없으면 대기).
     *
     * @param loopTimes 되풀이 구간 횟수
     * @param restMillis 동작이 끝난 뒤 쉬는 시간
     * @param blend 섞는 방식. null이면 클립이 정한 방식
     * @return 재생하는 클립
     */
    fun play(
        motion: String,
        state: CatState? = null,
        loopTimes: Int = DEFAULT_LOOP_TIMES,
        restMillis: Long = 0,
        blend: BlendKind? = null,
        overrides: Map<String, String> = emptyMap(),
    ): RigClip = playClip(CatClipLibrary.clipFor(motion, state, overrides), loopTimes, restMillis, blend)

    fun playClip(next: RigClip, loopTimes: Int = DEFAULT_LOOP_TIMES, restMillis: Long = 0, blend: BlendKind? = null): RigClip {
        val kind = blend ?: next.blendIn
        clip = next
        elapsed = 0f
        loopCount = if (next.loop != null) loopTimes.coerceAtLeast(1) else 0
        // 지금 그려진 클립 층(블렌드 중이면 섞인 값)과 그 속도에서 새 클립으로 넘어간다(블렌드 도중 또 바뀌어도 같다).
        // 클립은 휴지 자세에서 시작하므로 시작 포즈와 속도는 휴지 값, 0이다.
        clipDelay = 0f
        evaluateClip(target)
        val distance = poseDistance(clipLayer, target)
        blendDuration = fitBlendToDistance(kind.millis / 1000f, distance)
        // 자세 차이가 크면 새 클립을 조금 늦게 시작해 먼저 자리를 잡고 움직인다(두 움직임이 겹쳐 잘게 떨리지 않게).
        // 가만히 있다가 바뀌면(차이 1u 이하) 지연은 0이다.
        // 움직이던 중이면(0.1초 뒤 갈 거리) 그만큼도 차이로 친다: 움직임을 관성으로 멈춘 뒤 새 동작을 시작한다
        for (i in projected.indices) projected[i] = clipLayer[i] + layerVelocity[i] * MOMENTUM_LOOKAHEAD
        val moving = poseDistance(clipLayer, projected)
        clipDelay = blendDuration * DELAY_SHARE * clamp((maxOf(distance, moving) - DELAY_FROM) / DELAY_RANGE, 0f, 1f)
        planTotal(restMillis / 1000f)
        blendElapsed = 0f
        for (i in clipLayer.indices) {
            val ch = RigChannel.entries[i]
            val offset = if (ch.angle) wrapAngle(clipLayer[i] - target[i]) else clipLayer[i] - target[i]
            inertia[i].start(offset, layerVelocity[i], blendDuration)
        }
        return next
    }

    /** [dt]초 진행한다. 0.05초보다 긴 간격은 잘라 낸다(멈췄다 돌아와도 튀지 않게). */
    fun advance(dt: Float) {
        accumulator += clamp(dt, 0f, MAX_DT)
        // 1/60초 같은 간격이 실수 오차로 하위 단계 하나를 놓치지 않게 아주 작은 여유를 둔다
        while (accumulator >= STEP * STEP_TOLERANCE) {
            step(STEP)
            accumulator -= STEP
        }
    }

    /**
     * 자세 차이가 크면 기준서 길이(150~300ms)로는 첫 프레임들에 한꺼번에 움직인다.
     * 차이를 프레임당 [BLEND_SPEED_LIMIT] 안에서 줄일 수 있게 길이를 늘린다. 기준서 범위를 넘는 것은
     * 잠든 자세에서 깨어나는 것처럼 차이가 아주 큰 경우뿐이고 그때도 [MAX_BLEND](450ms)까지다(설계서 7-4).
     */
    private fun fitBlendToDistance(base: Float, distance: Float): Float {
        val needed = INERTIA_MAX_SLOPE * distance / (BLEND_SPEED_LIMIT * FRAMES_PER_SECOND)
        return clamp(needed, base, MAX_BLEND)
    }

    /** 두 포즈의 본 위치 차이 중 가장 큰 값(u). */
    private fun poseDistance(a: FloatArray, b: FloatArray): Float {
        val ax = FloatArray(CatBone.count)
        val ay = FloatArray(CatBone.count)
        solver.solve(a, time)
        for (bone in CatBone.entries) {
            ax[bone.ordinal] = solver.worldX(bone)
            ay[bone.ordinal] = solver.worldY(bone)
        }
        solver.solve(b, time)
        var d = 0f
        for (bone in CatBone.entries) {
            d = maxOf(d, kotlin.math.hypot(solver.worldX(bone) - ax[bone.ordinal], solver.worldY(bone) - ay[bone.ordinal]))
        }
        // 본 피벗보다 멀리 있는 귀 끝, 머리 윗부분은 회전으로 더 움직이므로 여유를 둔다
        return d * POSE_REACH
    }

    private fun planTotal(rest: Float) {
        val c = clip
        val loop = c.loop
        val play = clipDelay + c.intro.duration + (loop?.duration ?: 0f) * loopCount + (c.outro?.duration ?: 0f)
        total = if (loop != null && c.outro == null) {
            // 마지막이 되풀이면 쉬는 시간을 되풀이 한 바퀴 단위로 채운다(구간을 중간에 끊지 않는다)
            play + ceil(rest / loop.duration) * loop.duration
        } else {
            play + rest
        }
    }

    private fun step(h: Float) {
        time += h
        elapsed += h
        evaluateClip(target)
        System.arraycopy(clipLayer, 0, previousLayer, 0, clipLayer.size)
        if (blendElapsed < blendDuration) {
            blendElapsed += h
            for (i in clipLayer.indices) clipLayer[i] = target[i] + inertia[i].at(blendElapsed)
        } else {
            System.arraycopy(target, 0, clipLayer, 0, clipLayer.size)
        }
        for (i in clipLayer.indices) {
            val d = clipLayer[i] - previousLayer[i]
            layerVelocity[i] = (if (RigChannel.entries[i].angle) wrapAngle(d) else d) / h
        }
        compose(h)
    }

    /** 지금 시각의 클립 값을 [out]에 쓴다. */
    private fun evaluateClip(out: FloatArray) {
        val c = clip
        var e = (elapsed - clipDelay).coerceAtLeast(0f)
        val segment: RigSegment
        val local: Float
        val loop = c.loop
        val outro = c.outro
        if (e < c.intro.duration) {
            segment = c.intro
            local = e
        } else {
            e -= c.intro.duration
            if (loop != null && e < loop.duration * loopCount) {
                segment = loop
                local = e % loop.duration
            } else {
                if (loop != null) e -= loop.duration * loopCount
                when {
                    outro != null -> {
                        segment = outro
                        local = e.coerceAtMost(outro.duration)
                    }
                    loop != null -> {
                        segment = loop
                        local = e % loop.duration
                    }
                    else -> {
                        segment = c.intro
                        local = c.intro.duration
                    }
                }
            }
        }
        for (i in out.indices) out[i] = RigChannel.entries[i].rest
        for ((ch, track) in segment.tracks) out[ch.ordinal] = RigMath.evaluate(track, local)
    }

    /** 클립 층 + 기본 생기 + 스프링을 합쳐 [pose]를 만든다. */
    private fun compose(h: Float) {
        System.arraycopy(clipLayer, 0, pose, 0, pose.size)
        val t = time
        // 잠자는 정도는 0.5초에 걸쳐 바뀐다(숨 빠르기가 튀지 않게)
        val sleepTarget = if (clip.sleeping && !(clip.outro != null && inOutro())) 1f else 0f
        sleepiness += (sleepTarget - sleepiness) * clamp(h / SLEEP_EASE, 0f, 1f)
        val breathPeriod = lerp(BREATH_PERIOD, SLEEP_BREATH_PERIOD, sleepiness)
        val breath = (0.5 - 0.5 * cos(2.0 * PI * breathPhase(t, breathPeriod))).toFloat()
        add(RigChannel.BREATH, breath)
        add(RigChannel.HEAD_ROT, IDLE_HEAD_ROT * (1f - 0.5f * sleepiness) * sin(t * 1.18).toFloat())
        add(RigChannel.TAIL, ((0.1 * sin(t * 2.4) + 0.05 * sin(t * 5.3 + 1.0)) * (1f - 0.7f * sleepiness)).toFloat())
        add(RigChannel.TAIL_WAG, IDLE_TAIL_WAG * (1f - sleepiness))
        add(RigChannel.KNOT, (0.03 * sin(t * 2.1 + 0.6)).toFloat())
        pose[RigChannel.EYE_OPEN.ordinal] *= blink(h, awake = sleepiness < 0.5f)
        secondary(h)
    }

    /** 숨 위상. 주기가 바뀌어도 위상이 이어지게 적분한다. */
    private fun breathPhase(t: Double, period: Float): Double {
        breathAcc += (t - lastBreathT) / period
        lastBreathT = t
        return breathAcc
    }

    private fun inOutro(): Boolean {
        val c = clip
        val start = clipDelay + c.intro.duration + (c.loop?.duration ?: 0f) * loopCount
        return elapsed >= start
    }

    private fun add(ch: RigChannel, v: Float) {
        if (ch.mode == ChannelMode.ADD) pose[ch.ordinal] += v else pose[ch.ordinal] *= v
    }

    /** 깜빡임(기준서 BLINK: 60ms에 감고 20ms 머물고 70ms에 뜸, 3.2~5.8초마다, 15%는 두 번). */
    private fun blink(h: Float, awake: Boolean): Float {
        if (blinkT < 0) {
            if (time >= nextBlink && awake) blinkT = 0.0 else return 1f
        }
        blinkT += h
        val b = blinkT.toFloat()
        val v = when {
            b < 0.06f -> lerp(1f, 0f, ease(Interp.EASE_IN, b / 0.06f))
            b < 0.08f -> 0f
            else -> lerp(0f, 1f, ease(Interp.EASE_OUT, clamp((b - 0.08f) / 0.07f, 0f, 1f)))
        }
        if (b >= 0.15f) {
            blinkT = -1.0
            nextBlink = time + if (random.nextFloat() < DOUBLE_BLINK) 0.12 else 3.2 + random.nextDouble() * 2.6
        }
        return v
    }

    /** 머리 속도에 끌려 늦게 따라오는 귀, 꼬리, 매듭. 5~9초마다 한쪽 귀가 씰룩인다(기준서 IDLE_BREATH). */
    private fun secondary(h: Float) {
        solver.solve(pose, time)
        val hx = solver.worldX(CatBone.HEAD)
        val hy = solver.worldY(CatBone.HEAD)
        if (!prevHeadX.isNaN() && h > 0f) {
            // 머리 속도는 60ms 저역 통과로 다듬어 쓴다(곡선 꺾임마다 귀가 채찍처럼 튀지 않게)
            val k = clamp(h / HEAD_SPEED_SMOOTHING, 0f, 1f)
            headVx += (clamp((hx - prevHeadX) / h, -MAX_HEAD_SPEED, MAX_HEAD_SPEED) - headVx) * k
            headUp += (clamp(-(hy - prevHeadY) / h, -MAX_HEAD_SPEED, MAX_HEAD_SPEED) - headUp) * k
        }
        val vx = headVx
        val up = headUp
        prevHeadX = hx
        prevHeadY = hy
        val iL = RigChannel.EAR_L.ordinal
        val iR = RigChannel.EAR_R.ordinal
        val iT = RigChannel.TAIL.ordinal
        val iK = RigChannel.KNOT.ordinal
        if (!springsReady) {
            earL.reset(pose[iL]); earR.reset(pose[iR]); tail.reset(pose[iT]); knot.reset(pose[iK])
            springsReady = true
        }
        // 귀 씰룩임: 속도 충격 대신 0.25초 사인 모양으로 목표를 잠깐 옮긴다(반대로 움직이던 귀가 한 프레임에 꺾이지 않게)
        if (twitchT < 0 && time >= nextTwitch && sleepiness < 0.5f) {
            twitchLeft = random.nextBoolean()
            twitchAmp = (TWITCH_MIN + random.nextFloat() * TWITCH_RANGE) * if (twitchLeft) 1f else -1f
            twitchT = 0.0
            nextTwitch = time + 5.0 + random.nextDouble() * 4.0
        }
        var twitch = 0f
        if (twitchT >= 0) {
            twitchT += h
            val u = (twitchT / TWITCH_DURATION).toFloat()
            if (u >= 1f) twitchT = -1.0 else twitch = twitchAmp * sin(PI * u).toFloat()
        }
        if (twitchLeft) pose[iL] += twitch else pose[iR] += twitch
        if (h <= 0f) return
        pose[iL] = earL.step(pose[iL] + (-up * EAR_UP - vx * EAR_SIDE), h)
        pose[iR] = earR.step(pose[iR] + (up * EAR_UP - vx * EAR_SIDE), h)
        pose[iT] = tail.step(pose[iT] + (up * TAIL_UP - vx * TAIL_SIDE), h)
        pose[iK] = knot.step(pose[iK] + (vx * KNOT_SIDE - up * KNOT_SIDE), h)
    }

    /**
     * 관성 블렌딩 한 채널(Bollo, "Inertialization", GDC 2016). 남은 차이 x0와 그 속도 v0에서 시작해
     * [duration] 뒤 값, 속도, 가속도가 모두 0이 되는 5차 다항식. 전환 순간 값과 속도가 이어져 끊김과 되튐이 없다.
     */
    private class Inertia {
        private var a = 0f
        private var b = 0f
        private var c = 0f
        private var acc = 0f
        private var v0 = 0f
        private var x0 = 0f
        private var t1 = 0f

        fun start(offset: Float, velocity: Float, duration: Float) {
            x0 = offset
            // 차이에서 멀어지는 속도는 버린다(넘쳐 지나가지 않게)
            v0 = if (offset * velocity > 0f) 0f else velocity
            t1 = duration
            if (v0 != 0f) t1 = minOf(t1, -5f * x0 / v0)
            if (t1 < MIN_INERTIA || x0 == 0f && v0 == 0f) {
                t1 = 0f
                return
            }
            val t = t1
            // 처음 가속도: 차이 방향으로 넘치지 않는 쪽만 쓴다(x0 > 0이면 0 이상, x0 < 0이면 0 이하)
            val raw = (-8f * v0 * t - 20f * x0) / (t * t)
            acc = if (x0 >= 0f) maxOf(raw, 0f) else minOf(raw, 0f)
            a = -(acc * t * t + 6f * v0 * t + 12f * x0) / (2f * t * t * t * t * t)
            b = (3f * acc * t * t + 16f * v0 * t + 30f * x0) / (2f * t * t * t * t)
            c = -(3f * acc * t * t + 12f * v0 * t + 20f * x0) / (2f * t * t * t)
        }

        /** 시작 뒤 [t]초의 남은 차이 */
        fun at(t: Float): Float {
            if (t >= t1) return 0f
            return (((a * t + b) * t + c) * t + acc / 2f) * t * t + v0 * t + x0
        }

        private companion object {
            const val MIN_INERTIA = 1f / 240f
        }
    }

    /** 감쇠 스프링(반암시적 오일러). 감쇠비는 0.6~0.65로, 흔들린 뒤 한두 번 안에 가라앉아 머리의 빠른 움직임에 공진하지 않는다. */
    private class Spring(val k: Float, val c: Float) {
        var x = 0f
        var v = 0f
        fun reset(at: Float) {
            x = at
            v = 0f
        }

        fun step(target: Float, h: Float): Float {
            v += (-k * (x - target) - c * v) * h
            x += v * h
            return x
        }
    }

    companion object {
        /** 고정 하위 단계(초) */
        const val STEP = 1f / 240f
        const val MAX_DT = 0.05f
        private const val STEP_TOLERANCE = 0.999f

        /** 블렌드 중 프레임당 이동 상한(u, 60fps 기준)과 블렌드 최대 길이(기준서 300ms) */
        private const val BLEND_SPEED_LIMIT = 1.5f
        private const val FRAMES_PER_SECOND = 60f
        private const val MAX_BLEND = 0.45f
        private const val POSE_REACH = 1.3f

        /** 5차 관성 다항식의 최대 기울기(속도 0에서 시작할 때 15/8) */
        private const val INERTIA_MAX_SLOPE = 1.875f

        /** 새 클립 시작 지연: 자세 차이 1u부터 7u까지 0에서 블렌드 길이의 60%로 늘린다 */
        private const val DELAY_SHARE = 0.6f
        private const val DELAY_FROM = 1f
        private const val DELAY_RANGE = 6f
        private const val MOMENTUM_LOOKAHEAD = 0.1f
        const val DEFAULT_LOOP_TIMES = 3

        const val BREATH_PERIOD = 2.8f
        const val SLEEP_BREATH_PERIOD = 3.4f
        private const val SLEEP_EASE = 0.5f
        private const val IDLE_HEAD_ROT = 0.018f
        private const val IDLE_TAIL_WAG = 0.05f
        private const val DOUBLE_BLINK = 0.15f

        private const val TWITCH_DURATION = 0.25
        private const val TWITCH_MIN = 0.16f
        private const val TWITCH_RANGE = 0.1f

        private const val EAR_K = 170f
        private const val EAR_C = 17f
        private const val TAIL_K = 70f
        private const val TAIL_C = 10f
        private const val KNOT_K = 90f
        private const val KNOT_C = 11f

        /** 머리 속도(u/초) -> 귀, 꼬리, 매듭 기울기. 프로토타입 값(원화 px/초 기준)을 u로 환산한 값이다. */
        private const val EAR_UP = 0.0035f
        private const val EAR_SIDE = 0.003f
        private const val TAIL_UP = 0.0063f
        private const val TAIL_SIDE = 0.005f
        private const val KNOT_SIDE = 0.0075f
        private const val MAX_HEAD_SPEED = 72f
        private const val HEAD_SPEED_SMOOTHING = 0.06f
    }
}
