package com.dandi.nyummy.cat.entity.rig

/** 채널 값을 휴지 값과 합치는 방식. */
enum class ChannelMode { ADD, MULTIPLY }

/**
 * 클립이 움직이는 스켈레톤 공통 채널. 본 이름이 아니라 채널을 움직이므로 체형, 스킨이 달라도 같은 클립이 돈다.
 * 길이 단위는 리그 논리 좌표(u), 각도는 라디안(시계 방향 +).
 *
 * @property rest 휴지 값. 모든 클립은 이 값에서 시작해 이 값으로 끝난다.
 * @property mode 층(기본 생기, 클립)을 합치는 방식
 * @property angle 각도 채널이면 true. 블렌딩 때 최단 호로 섞는다.
 */
enum class RigChannel(val rest: Float, val mode: ChannelMode, val angle: Boolean = false) {
    /** 전체 좌우 이동(u) */
    ROOT_X(0f, ChannelMode.ADD),

    /** 점프 높이(u, 위가 +) */
    JUMP(0f, ChannelMode.ADD),

    /** 전체 회전(바닥 앵커 기준) */
    ROOT_ROT(0f, ChannelMode.ADD, angle = true),

    /** 전체 가로 배율(바닥 앵커 기준). 음수면 좌우가 뒤집힌다. */
    SQUASH_X(1f, ChannelMode.MULTIPLY),

    /** 전체 세로 배율(바닥 앵커 기준) */
    SQUASH_Y(1f, ChannelMode.MULTIPLY),

    /** 몸 기울기(발바닥 고정 기울임, 위로 갈수록 x가 밀리는 양) */
    LEAN(0f, ChannelMode.ADD),

    /** 숨쉬기(0..1) */
    BREATH(0f, ChannelMode.ADD),

    /** 몸통 세로 늘이기(발바닥 고정) */
    STRETCH(1f, ChannelMode.MULTIPLY),

    HEAD_X(0f, ChannelMode.ADD),
    HEAD_Y(0f, ChannelMode.ADD),
    HEAD_ROT(0f, ChannelMode.ADD, angle = true),

    /** 고개를 좌우로 돌린 정도(-1..1). 얼굴이 옆으로 밀리고 머리 가운데가 부푼다. */
    YAW(0f, ChannelMode.ADD),

    /** 고개를 위아래로 든 정도(-1..1, 숙임이 +) */
    PITCH(0f, ChannelMode.ADD),

    EAR_L(0f, ChannelMode.ADD, angle = true),
    EAR_R(0f, ChannelMode.ADD, angle = true),

    /** 다리 들어 올리기(0..1) */
    LEG_FL(0f, ChannelMode.ADD),
    LEG_FR(0f, ChannelMode.ADD),
    LEG_BL(0f, ChannelMode.ADD),
    LEG_BR(0f, ChannelMode.ADD),

    /** 꼬리 굽힘(라디안, 끝으로 갈수록 많이) */
    TAIL(0f, ChannelMode.ADD, angle = true),

    /** 꼬리 파동 세기(라디안) */
    TAIL_WAG(0f, ChannelMode.ADD),

    /** 반다나 매듭 흔들림 */
    KNOT(0f, ChannelMode.ADD, angle = true),

    /** 눈 뜬 정도(1 다 뜸, 0 감음) */
    EYE_OPEN(1f, ChannelMode.MULTIPLY),

    /** 눈 크기 */
    EYE_SCALE(1f, ChannelMode.MULTIPLY),

    /** 시선(u) */
    LOOK_X(0f, ChannelMode.ADD),
    LOOK_Y(0f, ChannelMode.ADD),

    /** 웃는 눈(0..1) */
    SMILE(0f, ChannelMode.ADD),

    /** 입 벌림(0..1.3) */
    MOUTH(0f, ChannelMode.ADD),

    /** 삐진 입(0..1) */
    POUT(0f, ChannelMode.ADD),

    /** 눈꺼풀이 위에서 덮는 양(0..1) */
    LID(0f, ChannelMode.ADD),

    /** 눈꺼풀 기울기(째려보기) */
    LID_SLANT(0f, ChannelMode.ADD),

    /** 볼 붉어짐(0..1) */
    BLUSH(0f, ChannelMode.ADD),

    /** 잠 효과(Zzz) 보이기(0..1) */
    ZZZ(0f, ChannelMode.ADD),
    ;

    companion object {
        val count: Int = entries.size
    }
}

/** 키 사이 보간. 계단형은 두지 않는다(뚝 끊김의 원인). */
enum class Interp {
    LINEAR,
    EASE_IN,
    EASE_OUT,
    EASE_IN_OUT,
    SINE_IN_OUT,

    /** 조금 넘쳤다 돌아오는 끝 */
    BACK,

    /** 이웃 키로 접선을 잡는 3차 Hermite(Catmull-Rom). 키가 여럿인 곡선을 기울기까지 이어 준다. */
    HERMITE,
}

/**
 * 키 하나.
 *
 * @property time 구간 안의 시각(초)
 * @property value 값
 * @property interp 앞 키에서 이 키로 들어오는 보간
 */
data class RigKey(val time: Float, val value: Float, val interp: Interp = Interp.EASE_IN_OUT)

/** 채널 하나의 곡선. 키는 시각 순서다. */
data class RigTrack(val keys: List<RigKey>) {
    init {
        require(keys.isNotEmpty()) { "키가 없는 곡선" }
    }
}

/**
 * 클립의 한 구간(들어가기, 되풀이, 돌아오기).
 *
 * @property duration 구간 길이(초)
 * @property tracks 채널별 곡선. 없는 채널은 휴지 값이다.
 */
data class RigSegment(val duration: Float, val tracks: Map<RigChannel, RigTrack>)

/** 상태가 바뀔 때 포즈를 섞는 방식(기준서 4-4). */
enum class BlendKind(val millis: Long) {
    /** 끼어드는 반응(터치) */
    INTERRUPT(150),

    /** 일반 감정, 행동 전환 */
    NORMAL(200),

    /** 앉기와 졸기, 자기처럼 자세가 바뀜 */
    POSTURE(300),
}

/**
 * 동작 하나의 클립. 스프라이트 동작의 시트 세 단(들어가기, 가운데 되풀이, 돌아오기)과 같은 모양이다.
 * 모든 클립은 휴지 자세에서 시작해 휴지 자세로 끝난다(되풀이 구간은 시작과 끝 값, 기울기가 같다).
 *
 * @property name 클립 이름
 * @property intro 들어가기
 * @property loop 가운데 되풀이(없으면 null)
 * @property outro 돌아오기(없으면 null)
 * @property blendIn 이 클립으로 바뀔 때 섞는 방식
 * @property sleeping 자는 동작. 깜빡임을 끈다.
 * @property approximate 원화 자세가 따로 필요해 앉은 리그로 근사한 동작(설계서 7-5)
 */
data class RigClip(
    val name: String,
    val intro: RigSegment,
    val loop: RigSegment? = null,
    val outro: RigSegment? = null,
    val blendIn: BlendKind = BlendKind.NORMAL,
    val sleeping: Boolean = false,
    val approximate: Boolean = false,
)
