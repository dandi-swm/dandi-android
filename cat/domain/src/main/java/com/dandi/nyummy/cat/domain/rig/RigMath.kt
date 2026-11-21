package com.dandi.nyummy.cat.domain.rig

import com.dandi.nyummy.cat.entity.rig.Interp
import com.dandi.nyummy.cat.entity.rig.RigKey
import com.dandi.nyummy.cat.entity.rig.RigTrack
import kotlin.math.cos
import kotlin.math.PI
import kotlin.math.pow

/** 리그 계산에 쓰는 작은 수학 함수들. 모두 실수 그대로 계산하고 반올림하지 않는다(서브픽셀 유지). */
internal object RigMath {
    fun clamp(x: Float, lo: Float, hi: Float): Float = if (x < lo) lo else if (x > hi) hi else x

    fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    /** a에서 b 사이를 0..1로 부드럽게(3차 smoothstep). */
    fun smooth(a: Float, b: Float, x: Float): Float {
        val t = clamp((x - a) / (b - a), 0f, 1f)
        return t * t * (3f - 2f * t)
    }

    /** 0..1 진행률에 보간 곡선을 건다. */
    fun ease(interp: Interp, t: Float): Float = when (interp) {
        Interp.LINEAR, Interp.HERMITE -> t
        Interp.EASE_IN -> t * t * t
        Interp.EASE_OUT -> 1f - (1f - t).pow(3)
        Interp.EASE_IN_OUT -> if (t < 0.5f) 4f * t * t * t else 1f - (-2f * t + 2f).pow(3) / 2f
        Interp.SINE_IN_OUT -> (-(cos(PI * t) - 1.0) / 2.0).toFloat()
        Interp.BACK -> {
            val c = 1.7f
            1f + (c + 1f) * (t - 1f).pow(3) + c * (t - 1f).pow(2)
        }
    }

    /**
     * 곡선의 [t]초 값. 첫 키 앞은 첫 키 값, 마지막 키 뒤는 마지막 키 값이다.
     * HERMITE 키는 앞뒤 이웃 키로 접선을 잡아(Catmull-Rom) 기울기까지 잇는다. 끝 키의 접선은 0이라 멈춤도 부드럽다.
     */
    fun evaluate(track: RigTrack, t: Float): Float {
        val keys = track.keys
        val first = keys[0]
        if (t <= first.time) return first.value
        val last = keys[keys.size - 1]
        if (t >= last.time) return last.value
        for (i in 1 until keys.size) {
            val b = keys[i]
            if (t < b.time) {
                val a = keys[i - 1]
                val span = b.time - a.time
                if (span <= 0f) return b.value
                val u = (t - a.time) / span
                return if (b.interp == Interp.HERMITE) hermite(keys, i, u, span) else lerp(a.value, b.value, ease(b.interp, u))
            }
        }
        return last.value
    }

    private fun hermite(keys: List<RigKey>, i: Int, u: Float, span: Float): Float {
        val a = keys[i - 1]
        val b = keys[i]
        // 끝 키에서는 기울기 0(멈춤), 가운데 키에서는 이웃 두 키를 잇는 기울기
        val m0 = if (i - 2 >= 0) (b.value - keys[i - 2].value) / (b.time - keys[i - 2].time) * span else 0f
        val m1 = if (i + 1 < keys.size) (keys[i + 1].value - a.value) / (keys[i + 1].time - a.time) * span else 0f
        val u2 = u * u
        val u3 = u2 * u
        return (2f * u3 - 3f * u2 + 1f) * a.value + (u3 - 2f * u2 + u) * m0 +
            (-2f * u3 + 3f * u2) * b.value + (u3 - u2) * m1
    }

    /** 각도 차이를 -PI..PI로 접는다(최단 호). */
    fun wrapAngle(d: Float): Float {
        var x = d
        val twoPi = (2.0 * PI).toFloat()
        while (x > PI) x -= twoPi
        while (x < -PI) x += twoPi
        return x
    }
}
