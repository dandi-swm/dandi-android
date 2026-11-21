package com.dandi.nyummy.cat.entity.rig

/**
 * 리그 논리 좌표의 점. 136 x 136 단위(u), 왼쪽 위가 원점이고 y는 아래로 커진다.
 */
data class RigPointVO(val x: Float = 0f, val y: Float = 0f)

/** 리그 논리 좌표의 사각형(왼쪽, 위, 오른쪽, 아래). */
data class RigBoxVO(val left: Float = 0f, val top: Float = 0f, val right: Float = 0f, val bottom: Float = 0f) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
}

/**
 * 귀 하나의 기준점.
 *
 * @property base 귀 절단선의 바깥쪽 끝(실루엣 쪽)
 * @property top 귀 절단선의 안쪽 끝(정수리 쪽)
 * @property tip 귀 끝
 */
data class RigEarVO(
    val base: RigPointVO = RigPointVO(),
    val top: RigPointVO = RigPointVO(),
    val tip: RigPointVO = RigPointVO(),
) {
    /** 귀 뿌리(절단선 가운데). 귀 본의 피벗이다. */
    val root: RigPointVO get() = RigPointVO((base.x + top.x) / 2f, (base.y + top.y) / 2f)
}

/** 눈 하나. 가운데와 반지름. */
data class RigEyeVO(
    val center: RigPointVO = RigPointVO(),
    val radiusX: Float = 0f,
    val radiusY: Float = 0f,
)

/** 코와 입 그림의 가운데와 크기. */
data class RigMouthVO(
    val center: RigPointVO = RigPointVO(),
    val width: Float = 0f,
    val height: Float = 0f,
)

/**
 * 원화에서 뽑은 색(0xAARRGGBB). 코드로 그리는 얼굴 모양(감은 눈 선, 입, 그림자)이 원화와 같은 색을 쓰게 한다.
 * 고양이(파츠 묶음)마다 달라 데이터로 둔다.
 *
 * @property line 윤곽선
 * @property pupil 눈동자
 * @property blush 볼터치
 */
data class RigPaletteVO(
    val line: Int = 0xFF764829.toInt(),
    val pupil: Int = 0xFF2B1B14.toInt(),
    val blush: Int = 0xFFF4A9A0.toInt(),
)

/**
 * 파츠 그림 한 장의 자리.
 *
 * @property id 파츠 이름(tail, body, bandana, earL, earR, head, eyeL, eyeR, mouth)
 * @property file 파츠 폴더 안의 그림 파일 이름
 * @property x 그림 왼쪽 위의 리그 좌표
 * @property y 그림 왼쪽 위의 리그 좌표
 * @property width 그림이 차지하는 리그 폭
 * @property height 그림이 차지하는 리그 높이
 */
data class RigLayerVO(
    val id: String = "",
    val file: String = "",
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Float = 0f,
    val height: Float = 0f,
)

/**
 * 한 체형의 파츠 묶음(`cats/rig/parts/<파츠 묶음>/<체형>/rig.json`).
 * 공식 정면 일러스트를 `tools/cat-rig/extract_parts.py`가 분해해 만든다. 좌표는 모두 리그 논리 좌표(u)다.
 *
 * @property body 체형 이름(LEAN, SLIM, NORMAL, CHUBBY, PLUMP)
 * @property ground 발바닥 y. 모든 체형이 118이다.
 * @property neck 목 기준점(반다나 윗단 가운데). x가 얼굴 대칭축이다.
 * @property headBox 귀를 뺀 머리
 * @property bodyBox 몸통(앞발, 뒷발 포함)
 * @property bandanaBox 기본 반다나
 * @property tailBase 꼬리 뿌리
 * @property tailTip 꼬리 끝
 * @property knot 반다나 매듭
 * @property palette 원화에서 뽑은 색
 * @property layers 그리는 순서(뒤에서 앞)의 파츠 그림
 * @property assetDir 파츠 그림이 든 앱 에셋 폴더(끝에 / 포함)
 */
data class CatBodyRigVO(
    val body: String = "",
    val ground: Float = 0f,
    val neck: RigPointVO = RigPointVO(),
    val headBox: RigBoxVO = RigBoxVO(),
    val bodyBox: RigBoxVO = RigBoxVO(),
    val bandanaBox: RigBoxVO = RigBoxVO(),
    val tailBase: RigPointVO = RigPointVO(),
    val tailTip: RigPointVO = RigPointVO(),
    val knot: RigPointVO = RigPointVO(),
    val earL: RigEarVO = RigEarVO(),
    val earR: RigEarVO = RigEarVO(),
    val eyeL: RigEyeVO = RigEyeVO(),
    val eyeR: RigEyeVO = RigEyeVO(),
    val mouth: RigMouthVO = RigMouthVO(),
    val palette: RigPaletteVO = RigPaletteVO(),
    val layers: List<RigLayerVO> = emptyList(),
    val assetDir: String = "",
) {
    fun layer(id: String): RigLayerVO? = layers.firstOrNull { it.id == id }
}
