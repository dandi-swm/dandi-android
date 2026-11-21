package com.dandi.nyummy.cat.entity.rig

/**
 * 그리는 순서(뒤 0 -> 앞 23). 단디-디자이너 검수 기준서 1-5절 번호를 따르되,
 * 원화 분해 구조 때문에 귀(12)와 목 장식(9)은 머리(13) 아래에 둔다(설계서 12절 C2, C3).
 * 결정이 바뀌면 이 표의 값만 고친다.
 */
object CatZOrder {
    const val SHADOW = 0
    const val BACK_ITEM = 1
    const val TAIL = 2
    const val TAIL_ITEM = 3
    const val BODY = 4
    const val OUTFIT_BACK = 5
    const val OUTFIT = 6
    const val FRONT_LEGS = 7
    const val NECK_BACK = 8
    const val NECK = 9
    const val HAT_BACK = 11
    const val EARS = 12
    const val HEAD = 13
    const val EYES = 15
    const val MOUTH = 16
    const val FACE_ITEM = 18
    const val HAT = 19
    const val FRONT_LEGS_FOREGROUND = 20
    const val EFFECT = 22
    const val DEBUG = 23
}

/**
 * 착용 슬롯. 슬롯마다 붙는 본, 그리는 순서, 체형에 따라 늘어나는 방식이 정해져 있다.
 *
 * @property bone 아이템이 따라가는 본
 * @property z 아이템 본체의 그리는 순서
 * @property backZ 뒤판(모자 뒤 테두리, 목걸이 뒤끈 등)의 그리는 순서. 뒤판이 없는 슬롯은 null
 * @property follow 본을 따라가는 방식
 */
enum class AttachmentSlot(
    val bone: CatBone,
    val z: Int,
    val backZ: Int?,
    val follow: AttachmentFollow,
) {
    /** 모자, 머리 장식. 머리와 한 몸처럼 움직이고 체형과 상관없이 크기가 같다. */
    HEAD_HAT(CatBone.HEAD, CatZOrder.HAT, CatZOrder.HAT_BACK, AttachmentFollow.RIGID),

    /** 안경, 볼 장식. 얼굴(고개 돌림 시차 포함)을 따라간다. 크기는 체형과 상관없이 같다. */
    FACE_ITEM(CatBone.FACE, CatZOrder.FACE_ITEM, null, AttachmentFollow.RIGID),

    /** 목걸이, 스카프. 끼우면 기본 반다나를 숨긴다. 가로만 체형 목 폭을 따라 늘어난다. */
    NECK_ACCESSORY(CatBone.NECK, CatZOrder.NECK, CatZOrder.NECK_BACK, AttachmentFollow.NECK_WIDTH),

    /** 옷. 몸통과 같은 가중치로 스키닝해 숨쉬기와 기울기를 그대로 따라가고, 가로는 체형 몸 폭을 따른다. */
    TORSO_OUTFIT(CatBone.TORSO, CatZOrder.OUTFIT, CatZOrder.OUTFIT_BACK, AttachmentFollow.BODY_SKIN),

    /** 등 뒤 장식(날개, 망토 뒤판). */
    BACK_ITEM(CatBone.TORSO, CatZOrder.BACK_ITEM, null, AttachmentFollow.RIGID),

    /** 꼬리 리본. 꼬리 가운데 마디를 따라간다. */
    TAIL_ACCESSORY(CatBone.TAIL_2, CatZOrder.TAIL_ITEM, null, AttachmentFollow.RIGID),
}

/** 아이템이 본을 따라가는 방식. */
enum class AttachmentFollow {
    /** 본의 위치와 회전만 따라간다. 크기는 고정. */
    RIGID,

    /** 본을 따라가되 가로만 체형 목 폭 비율로 늘어난다(매듭, 방울은 장식 서브레이어라 늘지 않음). */
    NECK_WIDTH,

    /** 몸통 메시와 같은 가중치로 꼭짓점마다 스키닝한다. 가로는 체형 몸 폭 비율로 늘어난다. */
    BODY_SKIN,
}

/** 모자가 귀를 다루는 방식(기준서 3-2-1). */
enum class EarLayerMode {
    /** 비니처럼 귀를 덮는다. 귀는 모자 챙 선 위쪽이 잘려 모자를 뚫고 나오지 않는다. */
    OVER_EARS,

    /** 왕관처럼 귀 사이에 놓인다. 뒤판은 귀 뒤, 앞판은 귀 앞에 그린다. */
    BETWEEN_EARS,
}

/**
 * 착용 아이템의 그림. 엔진은 그림 내용을 모르고, 그리는 쪽이 [key]로 찾는다.
 *
 * @property key 에셋 경로(`cats/rig/items/...webp`) 또는 코드로 그린 임시 그림 이름(`sample:...`)
 */
data class AttachmentArtVO(val key: String = "")

/**
 * 착용 아이템 하나. 크기와 자리는 리그 논리 좌표(u)로 정한다.
 *
 * @property id 아이템 id
 * @property slot 끼우는 슬롯
 * @property art 본체 그림
 * @property backArt 뒤판 그림(BETWEEN_EARS 모자의 뒤 테두리, 목걸이 뒤끈). 없으면 null
 * @property width 그림 폭(u). NORMAL 체형 기준이다.
 * @property height 그림 높이(u)
 * @property anchorX 그림 안에서 슬롯 소켓에 맞출 점(0..1, 왼쪽에서)
 * @property anchorY 그림 안에서 슬롯 소켓에 맞출 점(0..1, 위에서)
 * @property earLayerMode 모자일 때 귀 처리. 모자가 아니면 무시한다.
 * @property brimY OVER_EARS 모자의 챙 선(0..1, 그림 위에서). 귀는 이 선 위쪽이 잘린다.
 * @property hidesDefaultBandana 이 아이템을 끼우면 기본 반다나를 숨긴다. 목 장식은 항상 숨긴다.
 */
data class AttachmentItemVO(
    val id: String = "",
    val slot: AttachmentSlot = AttachmentSlot.HEAD_HAT,
    val art: AttachmentArtVO = AttachmentArtVO(),
    val backArt: AttachmentArtVO? = null,
    val width: Float = 0f,
    val height: Float = 0f,
    val anchorX: Float = 0.5f,
    val anchorY: Float = 0.5f,
    val earLayerMode: EarLayerMode = EarLayerMode.BETWEEN_EARS,
    val brimY: Float = 1f,
    val hidesDefaultBandana: Boolean = false,
) {
    /** 기본 반다나를 숨기는지. 목 장식은 늘 대신 들어가므로 숨긴다. */
    val replacesBandana: Boolean get() = slot == AttachmentSlot.NECK_ACCESSORY || hidesDefaultBandana
}
