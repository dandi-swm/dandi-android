package com.dandi.nyummy.cat.domain.rig

import com.dandi.nyummy.cat.entity.CatState
import com.dandi.nyummy.cat.entity.rig.BlendKind
import com.dandi.nyummy.cat.entity.rig.RigChannel.BLUSH
import com.dandi.nyummy.cat.entity.rig.RigChannel.EAR_L
import com.dandi.nyummy.cat.entity.rig.RigChannel.EAR_R
import com.dandi.nyummy.cat.entity.rig.RigChannel.EYE_OPEN
import com.dandi.nyummy.cat.entity.rig.RigChannel.EYE_SCALE
import com.dandi.nyummy.cat.entity.rig.RigChannel.HEAD_ROT
import com.dandi.nyummy.cat.entity.rig.RigChannel.HEAD_X
import com.dandi.nyummy.cat.entity.rig.RigChannel.HEAD_Y
import com.dandi.nyummy.cat.entity.rig.RigChannel.JUMP
import com.dandi.nyummy.cat.entity.rig.RigChannel.LEAN
import com.dandi.nyummy.cat.entity.rig.RigChannel.LEG_BR
import com.dandi.nyummy.cat.entity.rig.RigChannel.LEG_FL
import com.dandi.nyummy.cat.entity.rig.RigChannel.LEG_FR
import com.dandi.nyummy.cat.entity.rig.RigChannel.LID
import com.dandi.nyummy.cat.entity.rig.RigChannel.LID_SLANT
import com.dandi.nyummy.cat.entity.rig.RigChannel.LOOK_X
import com.dandi.nyummy.cat.entity.rig.RigChannel.LOOK_Y
import com.dandi.nyummy.cat.entity.rig.RigChannel.MOUTH
import com.dandi.nyummy.cat.entity.rig.RigChannel.PITCH
import com.dandi.nyummy.cat.entity.rig.RigChannel.POUT
import com.dandi.nyummy.cat.entity.rig.RigChannel.ROOT_ROT
import com.dandi.nyummy.cat.entity.rig.RigChannel.ROOT_X
import com.dandi.nyummy.cat.entity.rig.RigChannel.SMILE
import com.dandi.nyummy.cat.entity.rig.RigChannel.SQUASH_X
import com.dandi.nyummy.cat.entity.rig.RigChannel.SQUASH_Y
import com.dandi.nyummy.cat.entity.rig.RigChannel.STRETCH
import com.dandi.nyummy.cat.entity.rig.RigChannel.TAIL
import com.dandi.nyummy.cat.entity.rig.RigChannel.TAIL_WAG
import com.dandi.nyummy.cat.entity.rig.RigChannel.YAW
import com.dandi.nyummy.cat.entity.rig.RigChannel.ZZZ
import com.dandi.nyummy.cat.entity.rig.RigClip

/**
 * 리그 클립 모음. 동작 이름(앱 에셋 `animations.json`의 동작 이름)으로 클립을 찾는다.
 *
 * - 기준서 4-1의 핵심 클립 8종(IDLE_BREATH, BLINK는 [CatRigAnimator]의 기본 생기 층)과,
 *   스프라이트 동작 48종 전부의 이름별 클립이 있다.
 * - 모든 클립은 휴지 자세에서 시작해 휴지 자세로 끝난다. 되풀이 구간은 시작과 끝 값, 기울기가 같다.
 * - [RigClip.approximate]인 11종은 원화 자세(눕기, 구르기 등)가 따로 필요해 앉은 리그로 근사한 것이다.
 *
 * 값의 단위: 길이 u(136 x 136 논리 좌표), 각도 라디안(시계 방향 +).
 */
object CatClipLibrary {

    const val IDLE = "IDLE_BREATH"
    const val TAP_REACT = "TAP_REACT"
    const val TAP_IRRITATED = "TAP_IRRITATED"
    const val EAT_HAPPY = "EAT_HAPPY"
    const val GREET_WAVE = "GREET_WAVE"
    const val DROWSY_DOZE = "DROWSY_DOZE"
    const val SLEEP_LOOP = "SLEEP_LOOP"

    private val IR = BlendKind.INTERRUPT
    private val PO = BlendKind.POSTURE

    /** 클립 이름 -> 클립 */
    val clips: Map<String, RigClip> = listOf(
        // ---- 핵심 클립 ----
        clip(IDLE, intro = segment(1.0) {}),
        clip(
            TAP_REACT, blend = IR,
            intro = segment(0.68) {
                SQUASH_Y(k(0, 1), k(0.11, 0.9, OUT), k(0.29, 1.07, BACK), k(0.4, 0.95, IN), k(0.55, 1.02), k(0.68, 1))
                SQUASH_X(k(0, 1), k(0.11, 1.08, OUT), k(0.29, 0.95, BACK), k(0.4, 1.04, IN), k(0.55, 0.99), k(0.68, 1))
                JUMP(k(0.11, 0), k(0.29, 7.5, OUT), k(0.4, 0))
                HEAD_ROT(k(0, 0), k(0.11, -0.07, OUT), k(0.29, 0.03), k(0.48, -0.02), k(0.68, 0))
                SMILE(k(0.1, 0), k(0.2, 1, OUT), k(0.5, 1), k(0.66, 0))
                EAR_L(k(0.1, 0), k(0.29, 0.21, BACK), k(0.5, 0.1), k(0.68, 0))
                EAR_R(k(0.1, 0), k(0.29, -0.21, BACK), k(0.5, -0.1), k(0.68, 0))
                TAIL(k(0.1, 0), k(0.29, 0.42, OUT), k(0.5, 0.2), k(0.68, 0))
            },
        ),
        clip(
            TAP_IRRITATED, blend = IR,
            intro = segment(0.85) {
                EAR_L(k(0, 0), k(0.14, -0.38, OUT), k(0.56, -0.38), k(0.85, 0))
                EAR_R(k(0, 0), k(0.14, 0.38, OUT), k(0.56, 0.38), k(0.85, 0))
                LID(k(0, 0), k(0.14, 0.42, OUT), k(0.56, 0.42), k(0.85, 0))
                LID_SLANT(k(0, 0), k(0.14, 0.3, OUT), k(0.56, 0.3), k(0.85, 0))
                POUT(k(0.02, 0), k(0.14, 1, LIN), k(0.6, 1), k(0.8, 0, LIN))
                STRETCH(k(0, 1), k(0.14, 0.94, OUT), k(0.56, 0.94), k(0.85, 1))
                // 도리도리: 기준서 140ms 주기를 6Hz 아래(170ms)로 늦춰 떨림 기준과 맞춘다
                HEAD_ROT.oscillate(0.14f, 0.82f, 0f, 0.1f, 4)
                TAIL(k(0.14, 0), k(0.24, 0.5, OUT), k(0.34, 0.14, IN), k(0.44, 0.5, OUT), k(0.56, 0.14, IN), k(0.85, 0))
            },
        ),
        clip(
            EAT_HAPPY,
            intro = segment(0.3) {
                SMILE(k(0, 0), k(0.3, 1))
                BLUSH(k(0, 0), k(0.3, 0.65))
                STRETCH(k(0, 1), k(0.3, 1.02))
            },
            loop = segment(1.2) {
                SMILE(k(0, 1), k(1.2, 1))
                BLUSH(k(0, 0.65), k(0.6, 0.95, SINE), k(1.2, 0.65, SINE))
                MOUTH(k(0, 0), k(0.15, 0.6, SINE), k(0.3, 0, SINE), k(0.45, 0.6, SINE), k(0.6, 0, SINE), k(0.75, 0.6, SINE), k(0.9, 0, SINE), k(1.05, 0.6, SINE), k(1.2, 0, SINE))
                HEAD_ROT.oscillate(0f, 1.2f, 0f, 0.087f, 2)
                STRETCH(k(0, 1.02), k(0.3, 1.04, SINE), k(0.6, 1.02, SINE), k(0.9, 1.04, SINE), k(1.2, 1.02, SINE))
                HEAD_Y(k(0, 0), k(0.3, -1, SINE), k(0.6, 0, SINE), k(0.9, -1, SINE), k(1.2, 0, SINE))
            },
            outro = segment(0.4) {
                SMILE(k(0, 1), k(0.4, 0))
                BLUSH(k(0, 0.65), k(0.4, 0))
                STRETCH(k(0, 1.02), k(0.4, 1))
            },
        ),
        clip(
            GREET_WAVE,
            intro = segment(1.4) {
                // 앞다리 분리 원화 전까지는 작은 앞발 들기(설계서 12절 C1)
                LEG_FR(k(0, 0), k(0.24, 1, BACK), k(0.4, 0.75), k(0.56, 1), k(0.72, 0.75), k(0.88, 1), k(1.04, 0.75), k(1.12, 1), k(1.4, 0))
                HEAD_ROT(k(0, 0), k(0.3, -0.17), k(1.12, -0.17), k(1.4, 0))
                SMILE(k(0, 0), k(0.2, 1), k(1.15, 1), k(1.38, 0))
                MOUTH(k(0, 0), k(0.2, 0.6, OUT), k(1.1, 0.6), k(1.35, 0))
                EAR_L(k(0, 0), k(0.2, 0.14, OUT), k(1.1, 0.07), k(1.4, 0))
                EAR_R(k(0, 0), k(0.2, -0.14, OUT), k(1.1, -0.07), k(1.4, 0))
                TAIL(k(0, 0), k(0.3, 0.4), k(1.1, 0.4), k(1.4, 0))
            },
        ),
        clip(
            DROWSY_DOZE, blend = PO,
            intro = segment(0.8) {
                EYE_OPEN(k(0, 1), k(0.8, 0.45))
                EAR_L(k(0, 0), k(0.8, -0.08))
                EAR_R(k(0, 0), k(0.8, 0.08))
            },
            loop = segment(3.6) {
                EYE_OPEN(k(0, 0.45), k(2.4, 0.15, SINE), k(2.75, 0.85, OUT), k(3.6, 0.45, SINE))
                HEAD_ROT(k(0, 0), k(2.4, 0.24, SINE), k(2.75, -0.035, OUT), k(3.6, 0, SINE))
                HEAD_Y(k(0, 0), k(2.4, 2.6, SINE), k(2.75, -0.4, OUT), k(3.6, 0, SINE))
                PITCH(k(0, 0), k(2.4, 0.5, SINE), k(2.75, -0.1, OUT), k(3.6, 0, SINE))
                EAR_L(k(0, -0.08), k(2.4, -0.16), k(2.75, 0.06, OUT), k(3.6, -0.08))
                EAR_R(k(0, 0.08), k(2.4, 0.16), k(2.75, -0.06, OUT), k(3.6, 0.08))
            },
            outro = segment(0.6) {
                EYE_OPEN(k(0, 0.45), k(0.6, 1))
                EAR_L(k(0, -0.08), k(0.6, 0))
                EAR_R(k(0, 0.08), k(0.6, 0))
            },
        ),
        clip(
            SLEEP_LOOP, blend = PO, sleeping = true, approximate = true,
            intro = segment(1.0) {
                STRETCH(k(0, 1), k(1.0, 0.88))
                HEAD_Y(k(0, 0), k(1.0, 2.7))
                HEAD_ROT(k(0, 0), k(1.0, 0.14))
                PITCH(k(0, 0), k(1.0, 0.3))
                EYE_OPEN(k(0, 1), k(0.6, 0.3), k(1.0, 0))
                EAR_L(k(0, 0), k(1.0, -0.12))
                EAR_R(k(0, 0), k(1.0, 0.12))
                TAIL(k(0, 0), k(1.0, -0.3))
                ZZZ(k(0.6, 0), k(1.0, 1))
            },
            loop = segment(3.4) {
                STRETCH(k(0, 0.88), k(1.7, 0.925, SINE), k(3.4, 0.88, SINE))
                HEAD_Y(k(0, 2.7), k(1.88, 1.9, SINE), k(3.4, 2.7, SINE))
                HEAD_ROT(k(0, 0.14), k(3.4, 0.14))
                PITCH(k(0, 0.3), k(3.4, 0.3))
                EYE_OPEN(k(0, 0), k(3.4, 0))
                EAR_L(k(0, -0.12), k(3.4, -0.12))
                EAR_R(k(0, 0.12), k(3.4, 0.12))
                TAIL(k(0, -0.3), k(1.7, -0.25, SINE), k(3.4, -0.3, SINE))
                ZZZ(k(0, 1), k(3.4, 1))
            },
            outro = segment(1.1) {
                STRETCH(k(0, 0.88), k(0.8, 1))
                HEAD_Y(k(0, 2.7), k(0.8, 0))
                HEAD_ROT(k(0, 0.14), k(0.8, 0))
                PITCH(k(0, 0.3), k(0.8, 0))
                EYE_OPEN(k(0, 0), k(0.5, 0), k(0.9, 1, OUT))
                EAR_L(k(0, -0.12), k(0.8, 0))
                EAR_R(k(0, 0.12), k(0.8, 0))
                TAIL(k(0, -0.3), k(0.9, 0))
                ZZZ(k(0, 1), k(0.3, 0))
            },
        ),

        // ---- 동작 48종: 리그로 충실 ----
        clip(
            "blink",
            intro = segment(0.9) {
                EYE_OPEN(k(0, 1), k(0.06, 0, IN), k(0.08, 0), k(0.15, 1, OUT))
            },
        ),
        clip(
            "slow_blink",
            intro = segment(1.8) {
                EYE_OPEN(k(0, 1), k(0.5, 0.05), k(1.0, 0.05), k(1.5, 1))
                SMILE(k(0, 0), k(0.5, 0.3), k(1.0, 0.3), k(1.5, 0))
                HEAD_ROT(k(0, 0), k(0.6, 0.05), k(1.4, 0))
            },
        ),
        clip(
            "head_tilt", blend = IR,
            intro = segment(1.9) {
                HEAD_ROT(k(0, 0), k(0.12, 0.04), k(0.55, -0.25, SINE), k(1.25, -0.25), k(1.7, 0))
                HEAD_X(k(0.12, 0), k(0.55, -1.0, SINE), k(1.25, -1.0), k(1.7, 0))
                EAR_L(k(0.2, 0), k(0.45, 0.2, OUT), k(1.3, 0.12), k(1.7, 0))
                EAR_R(k(0.2, 0), k(0.45, -0.14, OUT), k(1.3, -0.08), k(1.7, 0))
                EYE_SCALE(k(0.2, 1), k(0.45, 1.07, OUT), k(1.3, 1.07), k(1.7, 1))
                TAIL(k(0.2, 0), k(0.6, 0.3), k(1.3, 0.3), k(1.8, 0))
            },
        ),
        clip(
            "nod",
            intro = segment(1.5) {
                PITCH(k(0, 0), k(0.14, -0.3), k(0.36, 0.95), k(0.58, 0.05), k(0.8, 0.85), k(1.15, 0))
                HEAD_Y(k(0, 0), k(0.14, -0.3), k(0.36, 1.3), k(0.58, 0.16), k(0.8, 1.1), k(1.15, 0))
                EYE_OPEN(k(0.2, 1), k(0.36, 0.7), k(0.58, 1), k(0.8, 0.7), k(1.05, 1))
            },
        ),
        clip(
            "look_around",
            intro = segment(3.0) {
                YAW(k(0, 0), k(0.5, -1), k(1.15, -1), k(1.75, 1), k(2.3, 1), k(2.8, 0))
                HEAD_ROT(k(0, 0), k(0.5, -0.05), k(1.15, -0.05), k(1.75, 0.05), k(2.3, 0.05), k(2.8, 0))
                LOOK_X(k(0, 0), k(0.4, -0.72, OUT), k(1.15, -0.72), k(1.6, 0.72), k(2.3, 0.72), k(2.75, 0))
                EAR_L(k(0.3, 0), k(0.55, 0.16, OUT), k(1.15, 0.1), k(1.5, 0))
                EAR_R(k(1.5, 0), k(1.8, -0.16, OUT), k(2.3, -0.1), k(2.7, 0))
                EYE_OPEN(k(1.2, 1), k(1.28, 0.08, IN), k(1.4, 1, OUT))
                LEAN(k(0, 0), k(0.5, -0.012), k(1.15, -0.012), k(1.75, 0.012), k(2.3, 0.012), k(2.8, 0))
            },
        ),
        clip(
            "listen",
            intro = segment(2.4) {
                EAR_L(k(0, 0), k(0.25, 0.24, OUT), k(0.9, 0.24), k(1.1, -0.05), k(1.6, -0.05), k(2.1, 0))
                EAR_R(k(0, 0), k(0.6, 0), k(0.85, -0.24, OUT), k(1.5, -0.24), k(2.1, 0))
                HEAD_ROT(k(0, 0), k(0.4, 0.08), k(1.0, 0.08), k(1.5, -0.06), k(2.2, 0))
                LOOK_Y(k(0, 0), k(0.4, -0.5), k(1.8, -0.5), k(2.2, 0))
                EYE_SCALE(k(0, 1), k(0.3, 1.06), k(1.8, 1.06), k(2.2, 1))
            },
        ),
        clip(
            "perk", blend = IR,
            intro = segment(1.7) {
                SQUASH_Y(k(0, 1), k(0.1, 0.965, OUT), k(0.3, 1.03, BACK), k(1.15, 1.02), k(1.5, 1))
                SQUASH_X(k(0, 1), k(0.1, 1.03, OUT), k(0.3, 0.985, BACK), k(1.15, 0.99), k(1.5, 1))
                HEAD_Y(k(0, 0), k(0.1, 0.32), k(0.3, -1.3, BACK), k(1.15, -1.0), k(1.5, 0))
                PITCH(k(0.1, 0), k(0.3, -0.35, OUT), k(1.15, -0.3), k(1.5, 0))
                EAR_L(k(0, 0), k(0.1, -0.1), k(0.28, 0.26, BACK), k(1.15, 0.2), k(1.5, 0))
                EAR_R(k(0, 0), k(0.1, 0.1), k(0.28, -0.26, BACK), k(1.15, -0.2), k(1.5, 0))
                EYE_SCALE(k(0.1, 1), k(0.3, 1.13, OUT), k(1.15, 1.1), k(1.5, 1))
                TAIL(k(0.1, 0), k(0.35, 0.55, BACK), k(1.15, 0.5), k(1.6, 0))
            },
        ),
        clip(
            "purr", blend = IR,
            intro = segment(3.6) {
                SMILE(k(0.15, 0), k(0.4, 1), k(3.0, 1), k(3.3, 0))
                LEAN(k(0.2, 0), k(0.8, -0.03), k(1.5, 0.03), k(2.2, -0.03), k(2.9, 0.02), k(3.4, 0))
                HEAD_ROT(k(0.2, 0), k(0.8, 0.07), k(1.5, -0.07), k(2.2, 0.07), k(2.9, -0.05), k(3.4, 0))
                EAR_L(k(0.2, 0), k(0.6, -0.1), k(3.0, -0.1), k(3.4, 0))
                EAR_R(k(0.2, 0), k(0.6, 0.1), k(3.0, 0.1), k(3.4, 0))
                TAIL_WAG(k(0.2, 0), k(0.7, 0.22), k(3.0, 0.22), k(3.5, 0))
                HEAD_Y(k(0.2, 0), k(0.6, 0.4), k(3.0, 0.4), k(3.4, 0))
                BLUSH(k(0.2, 0), k(0.6, 0.6), k(3.0, 0.6), k(3.4, 0))
            },
        ),
        clip(
            "purr_sway",
            intro = segment(2.6) {
                LEAN(k(0, 0), k(0.6, 0.025, SINE), k(1.3, -0.025, SINE), k(2.0, 0.02, SINE), k(2.5, 0, SINE))
                SMILE(k(0, 0), k(0.3, 1), k(2.2, 1), k(2.5, 0))
                HEAD_ROT(k(0, 0), k(0.7, -0.05, SINE), k(1.4, 0.05, SINE), k(2.1, -0.03, SINE), k(2.5, 0, SINE))
                TAIL_WAG(k(0, 0), k(0.5, 0.2), k(2.0, 0.2), k(2.5, 0))
            },
        ),
        clip(
            "call",
            intro = segment(1.6) {
                PITCH(k(0, 0), k(0.12, 0.2), k(0.36, -0.55, OUT), k(1.0, -0.5), k(1.35, 0))
                HEAD_Y(k(0, 0), k(0.12, 0.24), k(0.36, -1.0, OUT), k(1.0, -0.8), k(1.35, 0))
                MOUTH(k(0.22, 0), k(0.38, 1, OUT), k(0.95, 1), k(1.15, 0))
                EYE_OPEN(k(0.24, 1), k(0.36, 0.09), k(0.98, 0.09), k(1.12, 1, OUT))
                EAR_L(k(0.2, 0), k(0.38, -0.14, OUT), k(1.0, -0.14), k(1.35, 0))
                EAR_R(k(0.2, 0), k(0.38, 0.14, OUT), k(1.0, 0.14), k(1.35, 0))
                SQUASH_Y(k(0.2, 1), k(0.38, 1.02, OUT), k(1.0, 1.015), k(1.35, 1))
                HEAD_ROT(k(0.4, 0), k(0.55, 0.03), k(0.7, -0.03), k(0.85, 0.02), k(1.0, 0))
            },
        ),
        clip(
            "yawn",
            intro = segment(2.7) {
                PITCH(k(0, 0), k(0.25, 0.25), k(0.95, -0.75), k(1.75, -0.7), k(2.3, 0))
                HEAD_Y(k(0, 0), k(0.25, 0.32), k(0.95, -1.3), k(1.75, -1.1), k(2.3, 0))
                HEAD_ROT(k(0.25, 0), k(0.95, -0.07), k(1.75, -0.05), k(2.3, 0))
                MOUTH(k(0.5, 0), k(0.98, 1.3), k(1.7, 1.3), k(2.1, 0))
                EYE_OPEN(k(0.4, 1), k(0.8, 0.08), k(1.9, 0.08), k(2.2, 1), k(2.32, 0.1, IN), k(2.44, 1, OUT))
                EAR_L(k(0.5, 0), k(0.98, -0.3), k(1.75, -0.3), k(2.3, 0))
                EAR_R(k(0.5, 0), k(0.98, 0.3), k(1.75, 0.3), k(2.3, 0))
                STRETCH(k(0.5, 1), k(0.98, 1.03), k(1.75, 1.03), k(2.3, 1))
                TAIL(k(0.5, 0), k(1.0, -0.25), k(1.75, -0.25), k(2.4, 0))
            },
        ),
        clip(
            "doze_startle", blend = PO,
            intro = segment(3.7) {
                EYE_OPEN(k(0, 1), k(0.6, 0.5), k(1.3, 0.16), k(1.9, 0.08), k(2.28, 0.08), k(2.55, 1, OUT))
                EYE_SCALE(k(2.28, 1), k(2.55, 1.16, OUT), k(3.0, 1.1), k(3.3, 1))
                PITCH(k(0, 0), k(0.7, 0.3), k(1.4, 0.6), k(2.1, 1.05, IN), k(2.28, 1.1), k(2.6, -0.45, OUT), k(3.1, -0.2), k(3.5, 0))
                HEAD_Y(k(0, 0), k(1.4, 1.0), k(2.1, 2.4, IN), k(2.28, 2.55), k(2.6, -1.4, OUT), k(3.1, -0.5), k(3.5, 0))
                HEAD_ROT(k(0, 0), k(1.4, 0.05), k(2.1, 0.1, IN), k(2.28, 0.1), k(2.6, -0.03, OUT), k(2.9, 0.03), k(3.2, 0))
                EAR_L(k(0, 0), k(2.1, -0.16), k(2.28, -0.16), k(2.55, 0.28, OUT), k(3.1, 0.1), k(3.5, 0))
                EAR_R(k(0, 0), k(2.1, 0.16), k(2.28, 0.16), k(2.55, -0.28, OUT), k(3.1, -0.1), k(3.5, 0))
                SQUASH_Y(k(2.28, 1), k(2.55, 1.04, OUT), k(2.85, 0.99), k(3.15, 1))
                TAIL(k(2.28, 0), k(2.55, 0.6, OUT), k(3.1, 0.2), k(3.6, 0))
            },
        ),
        clip(
            "glare", blend = IR,
            intro = segment(2.7) {
                LID(k(0, 0), k(0.16, 0.5, OUT), k(2.1, 0.5), k(2.45, 0))
                LID_SLANT(k(0, 0), k(0.16, 0.34, OUT), k(2.1, 0.34), k(2.45, 0))
                YAW(k(0, 0), k(0.3, -0.55), k(2.0, -0.55), k(2.45, 0))
                HEAD_ROT(k(0, 0), k(0.3, 0.05), k(2.0, 0.05), k(2.45, 0))
                LOOK_X(k(0.1, 0), k(0.35, 0.72, OUT), k(2.0, 0.72), k(2.4, 0))
                EAR_L(k(0, 0), k(0.18, -0.42, OUT), k(2.1, -0.42), k(2.5, 0))
                EAR_R(k(0, 0), k(0.18, 0.42, OUT), k(2.1, 0.42), k(2.5, 0))
                TAIL(k(0.3, 0), k(0.42, 0.6, OUT), k(0.5, 0.14, IN), k(0.64, 0), k(0.9, 0), k(1.02, 0.6, OUT), k(1.1, 0.14, IN), k(1.24, 0), k(1.5, 0), k(1.62, 0.6, OUT), k(1.7, 0.14, IN), k(1.84, 0))
                LEAN(k(0, 0), k(0.3, 0.015), k(2.0, 0.015), k(2.45, 0))
            },
        ),
        clip(
            "ears_back", blend = IR,
            intro = segment(3.5) {
                LID(k(0, 0), k(0.14, 0.42, OUT), k(0.75, 0.42), k(0.95, 0))
                LID_SLANT(k(0, 0), k(0.14, 0.32, OUT), k(0.75, 0.32), k(0.95, 0))
                POUT(k(0.04, 0), k(0.16, 1, LIN), k(3.0, 1), k(3.14, 0, LIN))
                EYE_OPEN(k(0.72, 1), k(0.94, 0), k(2.72, 0), k(2.98, 1, OUT))
                YAW(k(0.62, 0), k(1.02, -1.15, BACK), k(2.62, -1.15), k(3.05, 0))
                PITCH(k(0.62, 0), k(1.02, -0.75, OUT), k(2.62, -0.75), k(3.05, 0))
                HEAD_ROT(k(0.62, 0), k(1.02, 0.11, BACK), k(2.62, 0.11), k(3.05, 0))
                HEAD_X(k(0.62, 0), k(1.02, -1.3, OUT), k(2.62, -1.3), k(3.05, 0))
                HEAD_Y(k(0.62, 0), k(1.02, -0.8, OUT), k(2.62, -0.8), k(3.05, 0))
                EAR_L(k(0, 0), k(0.16, -0.36, OUT), k(2.7, -0.36), k(3.2, 0))
                EAR_R(k(0, 0), k(0.16, 0.36, OUT), k(2.7, 0.36), k(3.2, 0))
                LEAN(k(0.62, 0), k(1.02, 0.014), k(2.62, 0.014), k(3.05, 0))
            },
        ),
        clip(
            "recoil", blend = IR,
            intro = segment(1.4) {
                LEAN(k(0, 0), k(0.1, 0.05, OUT), k(0.55, 0.035), k(1.0, 0))
                HEAD_X(k(0, 0), k(0.1, 1.3, OUT), k(0.55, 0.8), k(1.0, 0))
                SQUASH_Y(k(0, 1), k(0.1, 1.06, OUT), k(0.28, 0.97), k(0.52, 1))
                SQUASH_X(k(0, 1), k(0.1, 0.95, OUT), k(0.28, 1.02), k(0.52, 1))
                JUMP(k(0, 0), k(0.12, 2.4, OUT), k(0.26, 0))
                EYE_SCALE(k(0.02, 1), k(0.1, 1.24, OUT), k(0.7, 1.16), k(1.05, 1))
                EAR_L(k(0, 0), k(0.1, -0.5, OUT), k(0.6, -0.3), k(1.1, 0))
                EAR_R(k(0, 0), k(0.1, 0.5, OUT), k(0.6, 0.3), k(1.1, 0))
                TAIL(k(0, 0), k(0.1, 0.8, OUT), k(0.7, 0.5), k(1.2, 0))
                HEAD_ROT(k(0, 0), k(0.1, -0.06, OUT), k(0.7, -0.03), k(1.1, 0))
            },
        ),
        clip(
            "tail_thump", blend = IR,
            intro = segment(1.5) {
                TAIL(k(0, 0), k(0.15, 0.5, OUT), k(0.3, 0.1, IN), k(0.5, 0.5, OUT), k(0.65, 0.1, IN), k(0.85, 0.5, OUT), k(1.0, 0.1, IN), k(1.4, 0))
                LID(k(0, 0), k(0.2, 0.25), k(1.1, 0.25), k(1.4, 0))
                EAR_L(k(0, 0), k(0.2, -0.15), k(1.1, -0.15), k(1.4, 0))
                EAR_R(k(0, 0), k(0.2, 0.15), k(1.1, 0.15), k(1.4, 0))
            },
        ),
        clip(
            "pop", blend = IR,
            intro = segment(1.03) {
                SQUASH_Y(k(0.08, 1), k(0.18, 0.88, OUT), k(0.38, 1.06), k(0.58, 0.95), k(0.74, 1.02), k(0.9, 1))
                SQUASH_X(k(0.08, 1), k(0.18, 1.08, OUT), k(0.38, 0.96), k(0.58, 1.03), k(0.74, 0.99), k(0.9, 1))
                JUMP(k(0.18, 0), k(0.38, 6.0, OUT), k(0.58, 0))
                SMILE(k(0.16, 0), k(0.24, 1, OUT), k(0.7, 1), k(0.88, 0))
                MOUTH(k(0.18, 0), k(0.28, 0.85, OUT), k(0.63, 0.85), k(0.8, 0))
                TAIL(k(0.17, 0), k(0.32, 0.5, OUT), k(0.68, 0.2), k(0.98, 0))
                HEAD_ROT(k(0.17, 0), k(0.32, -0.05, OUT), k(0.58, 0.03), k(0.88, 0))
            },
        ),
        clip(
            "bounce", blend = IR,
            intro = segment(1.38) {
                JUMP(k(0.08, 0), k(0.26, 4.5, OUT), k(0.44, 0), k(0.64, 3.5, OUT), k(0.82, 0))
                SQUASH_Y(k(0.08, 1), k(0.17, 0.93, OUT), k(0.28, 1.05), k(0.44, 0.94, IN), k(0.64, 1.04), k(0.82, 0.96, IN), k(1.03, 1))
                SQUASH_X(k(0.08, 1), k(0.17, 1.05, OUT), k(0.28, 0.97), k(0.44, 1.04, IN), k(0.64, 0.98), k(0.82, 1.03, IN), k(1.03, 1))
                SMILE(k(0.08, 0), k(0.2, 1), k(1.03, 1), k(1.28, 0))
                EAR_L(k(0.08, 0), k(0.26, 0.15), k(0.98, 0.1), k(1.28, 0))
                EAR_R(k(0.08, 0), k(0.26, -0.15), k(0.98, -0.1), k(1.28, 0))
                TAIL(k(0.08, 0), k(0.28, 0.5), k(0.98, 0.4), k(1.38, 0))
            },
        ),
        clip(
            "wiggle", blend = IR,
            intro = segment(1.9) {
                // 귀를 세운 뒤(0.2초) 2Hz로 흔든다. 귀 세우기와 겹치면 귀 끝이 잘게 떨린다.
                ROOT_X.oscillate(0.2f, 1.7f, 0f, 1.0f, 3)
                LEAN.oscillate(0.2f, 1.7f, 0f, 0.02f, 3)
                EYE_SCALE(k(0, 1), k(0.15, 1.1), k(1.6, 1.1), k(1.85, 1))
                EAR_L(k(0, 0), k(0.15, 0.18), k(1.6, 0.18), k(1.85, 0))
                EAR_R(k(0, 0), k(0.15, -0.18), k(1.6, -0.18), k(1.85, 0))
                TAIL_WAG(k(0, 0), k(0.2, 0.3), k(1.6, 0.3), k(1.85, 0))
            },
        ),
        clip(
            "nuzzle", blend = IR,
            intro = segment(2.2) {
                HEAD_ROT.oscillate(0.1f, 1.9f, 0f, 0.12f, 2)
                HEAD_X.oscillate(0.1f, 1.9f, 0f, 1.5f, 2)
                EYE_OPEN(k(0, 1), k(0.3, 0.1), k(1.8, 0.1), k(2.1, 1))
                SMILE(k(0, 0), k(0.3, 0.6), k(1.8, 0.6), k(2.1, 0))
                BLUSH(k(0, 0), k(0.3, 0.5), k(1.8, 0.5), k(2.1, 0))
            },
        ),
        clip(
            "cheek_rub",
            intro = segment(2.4) {
                HEAD_ROT(k(0, 0), k(0.45, -0.2, SINE), k(1.9, -0.2), k(2.3, 0))
                HEAD_X(k(0, 0), k(0.35, -2.0), k(1.9, -2.0), k(2.3, 0))
                HEAD_Y.oscillate(0.4f, 1.85f, 0f, 0.8f, 3)
                EYE_OPEN(k(0, 1), k(0.35, 0.3), k(1.9, 0.3), k(2.3, 1))
                SMILE(k(0, 0), k(0.35, 0.5), k(1.9, 0.5), k(2.3, 0))
            },
        ),
        clip(
            "head_bump",
            intro = segment(1.7) {
                PITCH(k(0, 0), k(0.25, 0.6, OUT), k(0.45, 0.1), k(0.7, 0.6, OUT), k(0.9, 0.1), k(1.4, 0))
                HEAD_Y(k(0, 0), k(0.25, 2.0, OUT), k(0.45, 0.4), k(0.7, 2.0, OUT), k(0.9, 0.4), k(1.4, 0))
                SQUASH_Y(k(0, 1), k(0.25, 1.02), k(0.9, 1.02), k(1.4, 1))
                EYE_OPEN(k(0, 1), k(0.2, 0.3), k(1.0, 0.3), k(1.4, 1))
                SMILE(k(0, 0), k(0.2, 0.5), k(1.0, 0.5), k(1.4, 0))
            },
        ),
        clip(
            "lean_in",
            intro = segment(2.2) {
                SQUASH_X(k(0, 1), k(0.5, 1.04), k(1.6, 1.04), k(2.1, 1))
                SQUASH_Y(k(0, 1), k(0.5, 1.04), k(1.6, 1.04), k(2.1, 1))
                HEAD_Y(k(0, 0), k(0.5, 1.0), k(1.6, 1.0), k(2.1, 0))
                EYE_SCALE(k(0, 1), k(0.5, 1.08), k(1.6, 1.08), k(2.1, 1))
                SMILE(k(0, 0), k(0.5, 0.4), k(1.6, 0.4), k(2.1, 0))
            },
        ),
        clip(
            "sniff",
            intro = segment(2.2) {
                PITCH(k(0, 0), k(0.3, 0.3), k(1.8, 0.3), k(2.1, 0))
                HEAD_Y(k(0, 0), k(0.3, 1.6), k(0.55, 1.2), k(0.8, 1.6), k(1.05, 1.2), k(1.3, 1.6), k(1.55, 1.2), k(1.8, 1.6), k(2.1, 0))
                EYE_OPEN(k(0, 1), k(0.3, 0.8), k(1.8, 0.8), k(2.1, 1))
                EAR_L(k(0, 0), k(0.3, 0.12), k(1.8, 0.12), k(2.1, 0))
                EAR_R(k(0, 0), k(0.3, -0.12), k(1.8, -0.12), k(2.1, 0))
            },
        ),
        clip(
            "sniff_floor",
            intro = segment(2.4) {
                PITCH(k(0, 0), k(0.4, 0.9), k(1.9, 0.9), k(2.3, 0))
                HEAD_Y(k(0, 0), k(0.4, 4.6), k(0.65, 4.2), k(0.9, 4.6), k(1.15, 4.2), k(1.4, 4.6), k(1.65, 4.2), k(1.9, 4.6), k(2.3, 0))
                STRETCH(k(0, 1), k(0.4, 0.97), k(1.9, 0.97), k(2.3, 1))
                EYE_OPEN(k(0, 1), k(0.4, 0.6), k(1.9, 0.6), k(2.3, 1))
            },
        ),
        clip(
            "peek",
            intro = segment(2.4) {
                LEAN(k(0, 0), k(0.4, -0.05), k(1.8, -0.05), k(2.3, 0))
                HEAD_X(k(0, 0), k(0.4, -3.0), k(1.8, -3.0), k(2.3, 0))
                YAW(k(0, 0), k(0.45, -0.8), k(1.8, -0.8), k(2.3, 0))
                EYE_SCALE(k(0, 1), k(0.45, 1.1), k(1.8, 1.1), k(2.3, 1))
                EAR_L(k(0, 0), k(0.4, 0.18), k(1.8, 0.18), k(2.3, 0))
                EAR_R(k(0, 0), k(0.4, -0.18), k(1.8, -0.18), k(2.3, 0))
            },
        ),
        clip(
            "tail_up",
            intro = segment(2.2) {
                TAIL(k(0, 0), k(0.4, 0.8, OUT), k(1.8, 0.8), k(2.2, 0))
                TAIL_WAG(k(0, 0), k(0.4, 0.15), k(1.8, 0.15), k(2.2, 0))
                SMILE(k(0, 0), k(0.3, 0.4), k(1.8, 0.4), k(2.1, 0))
                EAR_L(k(0, 0), k(0.3, 0.14), k(1.8, 0.14), k(2.1, 0))
                EAR_R(k(0, 0), k(0.3, -0.14), k(1.8, -0.14), k(2.1, 0))
                SQUASH_Y(k(0, 1), k(0.3, 1.02), k(1.8, 1.02), k(2.1, 1))
            },
        ),
        clip(
            "groom",
            intro = segment(2.6) {
                PITCH(k(0, 0), k(0.4, 0.8), k(2.1, 0.8), k(2.5, 0))
                HEAD_Y(k(0, 0), k(0.4, 3.2), k(2.1, 3.2), k(2.5, 0))
                HEAD_ROT(k(0, 0), k(0.4, 0.1), k(2.1, 0.1), k(2.5, 0))
                EYE_OPEN(k(0, 1), k(0.4, 0.2), k(2.1, 0.2), k(2.5, 1))
                MOUTH(k(0.45, 0), k(0.65, 0.4, SINE), k(0.85, 0.05, SINE), k(1.05, 0.4, SINE), k(1.25, 0.05, SINE), k(1.45, 0.4, SINE), k(1.65, 0.05, SINE), k(1.85, 0.4, SINE), k(2.05, 0, SINE))
                LEG_FL(k(0, 0), k(0.4, 0.3), k(2.1, 0.3), k(2.5, 0))
            },
        ),
        clip(
            "knead",
            intro = segment(2.6) {
                LEG_FL(k(0, 0), k(0.3, 0.7), k(0.6, 0), k(0.9, 0.7), k(1.2, 0), k(1.5, 0.7), k(1.8, 0), k(2.3, 0))
                LEG_FR(k(0, 0), k(0.3, 0), k(0.6, 0.7), k(0.9, 0), k(1.2, 0.7), k(1.5, 0), k(1.8, 0.7), k(2.1, 0))
                EYE_OPEN(k(0, 1), k(0.3, 0.4), k(2.0, 0.4), k(2.4, 1))
                SMILE(k(0, 0), k(0.3, 0.5), k(2.0, 0.5), k(2.4, 0))
            },
        ),
        clip(
            "bat_air",
            intro = segment(1.8) {
                LEG_FR(k(0, 0), k(0.2, 1, OUT), k(0.4, 0.2), k(0.6, 1, OUT), k(0.8, 0.2), k(1.0, 1, OUT), k(1.3, 0))
                PITCH(k(0, 0), k(0.2, -0.3), k(1.2, -0.3), k(1.6, 0))
                LOOK_Y(k(0, 0), k(0.2, -0.6), k(1.2, -0.6), k(1.6, 0))
                EYE_SCALE(k(0, 1), k(0.2, 1.1), k(1.2, 1.1), k(1.6, 1))
                EAR_L(k(0, 0), k(0.2, 0.15), k(1.2, 0.15), k(1.6, 0))
                EAR_R(k(0, 0), k(0.2, -0.15), k(1.2, -0.15), k(1.6, 0))
            },
        ),
        clip(
            "paw_swat",
            intro = segment(1.4) {
                LEG_FL(k(0, 0), k(0.15, 1, OUT), k(0.35, 0.1, IN), k(0.5, 0.9, OUT), k(0.7, 0, IN))
                LEAN(k(0, 0), k(0.15, -0.03), k(0.7, -0.02), k(1.1, 0))
                EYE_SCALE(k(0, 1), k(0.15, 1.1), k(0.8, 1.1), k(1.2, 1))
                EAR_L(k(0, 0), k(0.15, 0.15), k(0.8, 0.1), k(1.2, 0))
                EAR_R(k(0, 0), k(0.15, -0.15), k(0.8, -0.1), k(1.2, 0))
            },
        ),
        clip(
            "shake",
            intro = segment(1.5) {
                HEAD_ROT(k(0, 0), k(0.2, 0.1, SINE), k(0.4, -0.1, SINE), k(0.6, 0.075, SINE), k(0.8, -0.05, SINE), k(1.0, 0.025, SINE), k(1.2, 0, SINE))
                EYE_OPEN(k(0, 1), k(0.1, 0.2), k(1.1, 0.2), k(1.35, 1))
                SQUASH_Y(k(0, 1), k(0.1, 0.98), k(1.1, 0.98), k(1.4, 1))
            },
        ),
        clip(
            "sit_proud",
            intro = segment(2.4) {
                SQUASH_Y(k(0, 1), k(0.4, 1.03), k(1.9, 1.03), k(2.3, 1))
                PITCH(k(0, 0), k(0.4, -0.3), k(1.9, -0.3), k(2.3, 0))
                EYE_OPEN(k(0, 1), k(0.4, 0.75), k(1.9, 0.75), k(2.3, 1))
                SMILE(k(0, 0), k(0.4, 0.3), k(1.9, 0.3), k(2.3, 0))
                TAIL(k(0, 0), k(0.4, 0.4), k(1.9, 0.4), k(2.3, 0))
            },
        ),
        clip(
            "daydream",
            intro = segment(3.0) {
                LOOK_Y(k(0, 0), k(0.6, -0.6), k(2.4, -0.6), k(2.9, 0))
                LOOK_X(k(0, 0), k(0.6, 0.4), k(2.4, 0.4), k(2.9, 0))
                EYE_OPEN(k(0, 1), k(0.6, 0.8), k(2.4, 0.8), k(2.9, 1))
                HEAD_ROT(k(0, 0), k(0.8, 0.05), k(2.4, 0.05), k(2.9, 0))
                TAIL_WAG(k(0, 0), k(0.6, 0.12), k(2.4, 0.12), k(2.9, 0))
            },
        ),

        // ---- 동작 48종: 근사(원화 자세 필요, 설계서 13절 A4) ----
        clip(
            "belly", blend = PO, approximate = true,
            intro = segment(0.6) {
                ROOT_ROT(k(0, 0), k(0.6, -0.18))
                LEG_FL(k(0, 0), k(0.6, 0.6))
                LEG_FR(k(0, 0), k(0.6, 0.6))
                SMILE(k(0, 0), k(0.6, 1))
                STRETCH(k(0, 1), k(0.6, 0.95))
            },
            loop = segment(1.2) {
                ROOT_ROT(k(0, -0.18), k(0.6, -0.12, SINE), k(1.2, -0.18, SINE))
                LEG_FL(k(0, 0.6), k(0.3, 0.9, SINE), k(0.6, 0.6, SINE), k(1.2, 0.6))
                LEG_FR(k(0, 0.6), k(0.6, 0.6), k(0.9, 0.9, SINE), k(1.2, 0.6, SINE))
                SMILE(k(0, 1), k(1.2, 1))
                STRETCH(k(0, 0.95), k(1.2, 0.95))
            },
            outro = segment(0.6) {
                ROOT_ROT(k(0, -0.18), k(0.6, 0))
                LEG_FL(k(0, 0.6), k(0.6, 0))
                LEG_FR(k(0, 0.6), k(0.6, 0))
                SMILE(k(0, 1), k(0.6, 0))
                STRETCH(k(0, 0.95), k(0.6, 1))
            },
        ),
        clip(
            "pounce", blend = PO, approximate = true,
            intro = segment(0.5) {
                STRETCH(k(0, 1), k(0.5, 0.9))
                PITCH(k(0, 0), k(0.5, 0.2))
                EYE_SCALE(k(0, 1), k(0.5, 1.15))
                EAR_L(k(0, 0), k(0.5, 0.2))
                EAR_R(k(0, 0), k(0.5, -0.2))
                LEAN(k(0, 0), k(0.5, 0.025))
            },
            loop = segment(0.8) {
                STRETCH(k(0, 0.9), k(0.8, 0.9))
                PITCH(k(0, 0.2), k(0.8, 0.2))
                EYE_SCALE(k(0, 1.15), k(0.8, 1.15))
                EAR_L(k(0, 0.2), k(0.8, 0.2))
                EAR_R(k(0, -0.2), k(0.8, -0.2))
                // 꼭짓점에서 시작하고 끝나 되풀이 이음매에서 멈추지 않는다
                LEAN(k(0, 0.025), k(0.2, -0.025, SINE), k(0.4, 0.025, SINE), k(0.6, -0.025, SINE), k(0.8, 0.025, SINE))
                TAIL_WAG(k(0, 0), k(0.4, 0.35, SINE), k(0.8, 0, SINE))
            },
            outro = segment(1.0) {
                STRETCH(k(0, 0.9), k(0.25, 1.04, SINE), k(0.5, 0.96, SINE), k(0.8, 1))
                LEAN(k(0, 0.025), k(0.25, 0))
                JUMP(k(0, 0), k(0.28, 5.0, SINE), k(0.52, 0))
                SQUASH_X(k(0, 1), k(0.28, 1.03), k(0.52, 1.05, IN), k(0.8, 1))
                SQUASH_Y(k(0, 1), k(0.28, 1.02), k(0.52, 0.95, IN), k(0.8, 1))
                PITCH(k(0, 0.2), k(0.3, -0.2), k(0.8, 0))
                EYE_SCALE(k(0, 1.15), k(0.8, 1))
                EAR_L(k(0, 0.2), k(0.8, 0))
                EAR_R(k(0, -0.2), k(0.8, 0))
            },
        ),
        clip(
            "roll", blend = PO, approximate = true,
            intro = segment(2.2) {
                ROOT_ROT(k(0, 0), k(0.45, 0.15, SINE), k(1.05, -0.15, SINE), k(1.6, 0.1, SINE), k(2.1, 0, SINE))
                LEG_FL(k(0, 0), k(0.4, 0.6), k(1.6, 0.6), k(2.0, 0))
                LEG_FR(k(0, 0), k(0.4, 0.6), k(1.6, 0.6), k(2.0, 0))
                SMILE(k(0, 0), k(0.3, 1), k(1.7, 1), k(2.1, 0))
                EAR_L(k(0, 0), k(0.4, -0.15), k(1.6, -0.15), k(2.0, 0))
                EAR_R(k(0, 0), k(0.4, 0.15), k(1.6, 0.15), k(2.0, 0))
                TAIL(k(0, 0), k(0.4, -0.3), k(0.9, 0.3), k(1.4, -0.2), k(1.9, 0))
            },
        ),
        clip(
            "stretch", blend = PO, approximate = true,
            intro = segment(2.6) {
                STRETCH(k(0, 1), k(0.8, 1.08), k(1.8, 1.08), k(2.4, 1))
                SQUASH_X(k(0, 1), k(0.8, 0.96), k(1.8, 0.96), k(2.4, 1))
                PITCH(k(0, 0), k(0.8, -0.4), k(1.8, -0.4), k(2.4, 0))
                EYE_OPEN(k(0, 1), k(0.6, 0.05), k(1.9, 0.05), k(2.3, 1))
                MOUTH(k(0.3, 0), k(0.9, 0.6), k(1.7, 0.6), k(2.1, 0))
                EAR_L(k(0, 0), k(0.8, -0.25), k(1.8, -0.25), k(2.4, 0))
                EAR_R(k(0, 0), k(0.8, 0.25), k(1.8, 0.25), k(2.4, 0))
                LEG_FL(k(0, 0), k(0.8, 0.4), k(1.8, 0.4), k(2.4, 0))
                LEG_FR(k(0, 0), k(0.8, 0.4), k(1.8, 0.4), k(2.4, 0))
            },
        ),
        clip(
            "scratch", approximate = true,
            intro = segment(2.2) {
                STRETCH(k(0, 1), k(0.3, 1.05), k(1.8, 1.05), k(2.1, 1))
                LEG_FL(k(0, 0), k(0.3, 0.8), k(0.6, 0.2), k(0.9, 0.8), k(1.2, 0.2), k(1.5, 0.8), k(1.8, 0))
                LEG_FR(k(0, 0), k(0.45, 0.8), k(0.75, 0.2), k(1.05, 0.8), k(1.35, 0.2), k(1.65, 0.8), k(1.95, 0))
                LID(k(0, 0), k(0.3, 0.2), k(1.8, 0.2), k(2.1, 0))
                TAIL(k(0, 0), k(0.3, 0.3), k(1.8, 0.3), k(2.1, 0))
            },
        ),
        clip(
            "spin", approximate = true,
            intro = segment(2.05) {
                SQUASH_X(k(0.15, 1), k(0.75, -1), k(1.35, 1))
                JUMP(k(0.15, 0), k(0.45, 3, OUT), k(0.75, 0), k(1.05, 3, OUT), k(1.35, 0))
                SMILE(k(0.15, 0), k(0.35, 1), k(1.55, 1), k(1.85, 0))
                MOUTH(k(0.15, 0), k(0.35, 0.6), k(1.45, 0.6), k(1.75, 0))
                TAIL_WAG(k(0.15, 0), k(0.45, 0.3), k(1.45, 0.3), k(1.85, 0))
            },
        ),
        clip(
            "tail_chase", approximate = true,
            intro = segment(2.6) {
                YAW(k(0, 0), k(0.4, 0.6), k(2.0, 0.6), k(2.4, 0))
                LOOK_X(k(0, 0), k(0.4, 0.8), k(2.0, 0.8), k(2.4, 0))
                ROOT_X(k(0, 0), k(0.4, 1.5), k(0.8, -0.5), k(1.2, 1.5), k(1.6, -0.5), k(2.0, 1.0), k(2.4, 0))
                TAIL_WAG(k(0, 0), k(0.3, 0.5), k(2.0, 0.5), k(2.4, 0))
                TAIL(k(0, 0), k(0.4, -0.4), k(2.0, -0.4), k(2.4, 0))
                EAR_L(k(0, 0), k(0.3, 0.2), k(2.0, 0.2), k(2.4, 0))
                EAR_R(k(0, 0), k(0.3, -0.2), k(2.0, -0.2), k(2.4, 0))
                EYE_SCALE(k(0, 1), k(0.3, 1.12), k(2.0, 1.12), k(2.4, 1))
            },
        ),
        clip(
            "paws_up", approximate = true,
            intro = segment(1.8) {
                LEG_FL(k(0, 0), k(0.25, 1, BACK), k(1.3, 1), k(1.7, 0))
                LEG_FR(k(0, 0), k(0.25, 1, BACK), k(1.3, 1), k(1.7, 0))
                JUMP(k(0, 0), k(0.25, 3, OUT), k(0.45, 0), k(0.7, 2, OUT), k(0.9, 0))
                SMILE(k(0, 0), k(0.2, 1), k(1.4, 1), k(1.7, 0))
                MOUTH(k(0, 0), k(0.2, 0.7), k(1.3, 0.7), k(1.6, 0))
                TAIL(k(0, 0), k(0.3, 0.5), k(1.3, 0.5), k(1.7, 0))
            },
        ),
        clip(
            "face_wash", approximate = true,
            intro = segment(2.6) {
                LEG_FR(k(0, 0), k(0.3, 1), k(2.1, 1), k(2.5, 0))
                HEAD_ROT.oscillate(0.35f, 2.05f, 0f, 0.12f, 3)
                PITCH(k(0, 0), k(0.3, 0.25), k(2.1, 0.25), k(2.5, 0))
                EYE_OPEN(k(0, 1), k(0.3, 0.15), k(2.1, 0.15), k(2.5, 1))
            },
        ),
        clip(
            "ear_scratch", approximate = true,
            intro = segment(2.2) {
                LEG_BR(k(0, 0), k(0.4, 1, SINE), k(0.65, 0.7, SINE), k(0.9, 1, SINE), k(1.15, 0.7, SINE), k(1.4, 1, SINE), k(1.85, 0, SINE))
                HEAD_ROT(k(0, 0), k(0.45, 0.25, SINE), k(1.6, 0.25), k(2.0, 0))
                EYE_OPEN(k(0, 1), k(0.3, 0.2), k(1.6, 0.2), k(2.0, 1))
                EAR_R(k(0, 0), k(0.3, 0.2), k(1.6, 0.2), k(2.0, 0))
            },
        ),
    ).associateBy { it.name }

    /** 스프라이트 동작 이름 -> 클립 이름(이름이 같으면 적지 않는다). 핵심 클립을 그대로 쓰는 동작. */
    private val aliases = mapOf(
        "greet" to GREET_WAVE,
        "eat" to EAT_HAPPY,
        "doze" to DROWSY_DOZE,
        "lie_down" to SLEEP_LOOP,
    )

    /** 근사 클립 이름(원화 자세 필요). */
    val approximateMotions: Set<String> = setOf(
        "belly", "pounce", "roll", "lie_down", "stretch", "scratch", "spin", "tail_chase", "paws_up", "face_wash", "ear_scratch",
    )

    /** 상황별 기본 클립. 동작 이름으로 못 찾으면 이것을 쓴다. */
    private val stateFallback = mapOf(
        CatState.HOME_TAP to TAP_REACT,
        CatState.HOME_TAP_BURST to TAP_IRRITATED,
        CatState.HOME_GREET to GREET_WAVE,
        CatState.HOME_WELCOME_BACK to GREET_WAVE,
        CatState.HOME_AFTER_MEAL to EAT_HAPPY,
        CatState.HOME_DROWSY to DROWSY_DOZE,
        CatState.HOME_NIGHT to SLEEP_LOOP,
    )

    /** 동작 이름으로 클립을 찾는다. 이름으로 없으면 상황 기본 클립, 그것도 없으면 [IDLE]이다(미재생 0). */
    fun clipFor(motion: String, state: CatState? = null, overrides: Map<String, String> = emptyMap()): RigClip {
        val name = overrides[motion] ?: aliases[motion] ?: motion
        return clips[name]?.takeIf { it.intro.duration > MIN_DURATION }
            ?: state?.let { stateFallback[it] }?.let { clips[it] }
            ?: clips.getValue(IDLE)
    }

    /** 동작 이름으로 클립이 따로 있는지(별칭 포함). */
    fun hasClip(motion: String): Boolean {
        val name = aliases[motion] ?: motion
        return clips[name]?.let { it.intro.duration > MIN_DURATION } == true
    }

    private const val MIN_DURATION = 0.05f
}
