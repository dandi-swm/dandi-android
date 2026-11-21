package com.dandi.nyummy.cat.domain.rig

import com.dandi.nyummy.cat.entity.rig.BlendKind
import com.dandi.nyummy.cat.entity.rig.Interp
import com.dandi.nyummy.cat.entity.rig.RigChannel
import com.dandi.nyummy.cat.entity.rig.RigClip
import com.dandi.nyummy.cat.entity.rig.RigKey
import com.dandi.nyummy.cat.entity.rig.RigSegment
import com.dandi.nyummy.cat.entity.rig.RigTrack

/** 클립을 짧게 적는 도우미. 시각은 초, 길이는 u, 각도는 라디안. */
internal class SegmentBuilder(private val duration: Float) {
    private val tracks = LinkedHashMap<RigChannel, RigTrack>()

    operator fun RigChannel.invoke(vararg keys: RigKey) {
        tracks[this] = RigTrack(keys.toList())
    }

    /**
     * [from]초부터 [to]초까지 [center]를 가운데로 [amp]만큼 [cycles]번 오가는 사인 모양 곡선. 앞뒤는 [center]에서 멈춘다.
     * 키는 꼭짓점(+amp, -amp)에만 두어 가운데를 지날 때 멈추지 않는다(멈췄다 가면 끊겨 보인다).
     */
    fun RigChannel.oscillate(from: Float, to: Float, center: Float, amp: Float, cycles: Int) {
        val keys = ArrayList<RigKey>()
        keys += RigKey(from, center)
        val quarter = (to - from) / (cycles * 4)
        var sign = 1f
        var t = from + quarter
        while (t < to - quarter * 0.5f) {
            keys += RigKey(t, center + amp * sign, Interp.SINE_IN_OUT)
            sign = -sign
            t += quarter * 2
        }
        keys += RigKey(to, center, Interp.SINE_IN_OUT)
        tracks[this] = RigTrack(keys)
    }

    fun build() = RigSegment(duration, tracks)
}

internal fun k(time: Number, value: Number, interp: Interp = Interp.EASE_IN_OUT) =
    RigKey(time.toFloat(), value.toFloat(), interp)

internal fun segment(duration: Number, block: SegmentBuilder.() -> Unit): RigSegment =
    SegmentBuilder(duration.toFloat()).apply(block).build()

internal fun clip(
    name: String,
    blend: BlendKind = BlendKind.NORMAL,
    sleeping: Boolean = false,
    approximate: Boolean = false,
    intro: RigSegment,
    loop: RigSegment? = null,
    outro: RigSegment? = null,
) = RigClip(name, intro, loop, outro, blend, sleeping, approximate)

internal val OUT = Interp.EASE_OUT
internal val IN = Interp.EASE_IN
internal val IO = Interp.EASE_IN_OUT
internal val SINE = Interp.SINE_IN_OUT
internal val BACK = Interp.BACK
internal val LIN = Interp.LINEAR
internal val HERM = Interp.HERMITE
