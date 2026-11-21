package com.dandi.nyummy.cat.entity.rig

/**
 * 고양이 스켈레톤의 본. 모든 고양이(스킨)와 체형이 같은 계층을 쓴다.
 * 순서는 부모가 자식보다 앞이다(정방향 운동학을 이 순서대로 한 번에 계산한다).
 *
 * 자식은 부모의 위치와 회전만 물려받고 배율(숨쉬기, 스쿼시)은 물려받지 않는다.
 * 그래서 몸이 부풀거나 눌려도 머리, 눈, 모자는 찌그러지지 않는다.
 *
 * @property parent 부모 본. 맨 위 [ROOT]만 null이다.
 */
enum class CatBone(val parent: CatBone?) {
    /** 바닥 앵커(목 x, 발바닥 y). 이동, 점프, 전체 스쿼시의 기준 */
    ROOT(null),

    /** 골반. 몸 기울기(발바닥 고정 기울임)의 축 */
    HIP(ROOT),

    /** 몸통. 숨쉬기와 늘이기(발바닥 고정 배율) */
    TORSO(HIP),

    /** 목. 머리 회전 일부를 미리 받아 목 장식이 턱과 가슴 사이에 머문다 */
    NECK(TORSO),

    /** 머리. 피벗은 턱과 목의 경계다 */
    HEAD(NECK),
    EAR_L(HEAD),
    EAR_R(HEAD),

    /** 얼굴. 고개를 돌리는 효과(yaw, pitch 시차)를 받는다 */
    FACE(HEAD),
    EYE_L(FACE),
    EYE_R(FACE),
    MOUTH(FACE),

    /** 앞다리. 피벗은 앞발(바닥)이라 몸통이 숨쉬거나 웅크려도 발은 바닥에 붙어 있다. 들면 발이 올라간다. */
    LEG_FL(HIP),
    LEG_FR(HIP),

    /** 뒷다리. 피벗은 뒷발(바닥) */
    LEG_BL(HIP),
    LEG_BR(HIP),

    /** 꼬리 뿌리에서 끝으로 세 마디 */
    TAIL_1(HIP),
    TAIL_2(TAIL_1),
    TAIL_3(TAIL_2),

    /** 기본 반다나 매듭 끝. 목을 따라가다 2차 움직임으로 흔들린다 */
    KNOT(NECK),
    ;

    companion object {
        /** 본 개수. 변환 배열 크기에 쓴다. */
        val count: Int = entries.size
    }
}
