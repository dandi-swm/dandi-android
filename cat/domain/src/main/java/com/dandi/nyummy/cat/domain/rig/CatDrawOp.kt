package com.dandi.nyummy.cat.domain.rig

import com.dandi.nyummy.cat.entity.rig.PartMaterial

/**
 * 한 프레임에 그릴 것 하나. 그리는 쪽(Android Canvas)은 이 값만 보고 그리고, 엔진은 그림 내용을 모른다.
 * 프레임마다 새로 만들지 않고 값만 바꿔 재사용한다(프레임 루프 할당 0).
 *
 * @property z 그리는 순서([com.dandi.nyummy.cat.entity.rig.CatZOrder])
 * @property order 같은 z 안의 순서
 * @property texture 그림 키. 파츠는 `part:<파츠 id>`, 아이템은 아이템 그림 키
 * @property material 스킨 색 변환을 고를 재질. 아이템은 null(변환 없음)
 */
sealed class CatDrawOp(val z: Int, val order: Int, val texture: String, val material: PartMaterial?) {
    /** 불투명도(0이면 그리지 않는다) */
    var alpha: Float = 1f

    /**
     * 잘라 낼 다각형(x, y 쌍). 이 안만 그린다. null이면 자르지 않는다.
     * [CatMeshOp]는 화면(리그) 좌표, [CatRigidOp]와 [CatShapeOp]는 변환 전 휴지 좌표다.
     */
    var clip: FloatArray? = null
}

/**
 * 격자 메시로 변형해 그리는 그림. [vertices]는 (cols+1) x (rows+1) 꼭짓점의 화면 좌표(x, y)이고,
 * 그림 전체가 격자에 고르게 펼쳐진다(drawBitmapMesh 규약).
 */
class CatMeshOp(
    z: Int,
    order: Int,
    texture: String,
    material: PartMaterial?,
    val cols: Int,
    val rows: Int,
) : CatDrawOp(z, order, texture, material) {
    val vertices = FloatArray((cols + 1) * (rows + 1) * 2)
}

/**
 * 휴지 좌표의 사각형([left], [top], [width], [height])에 그림을 깔고 [matrix]로 옮겨 그린다(강체 파츠, 강체 아이템).
 * [matrix]는 아핀 변환 6개 값(a, b, c, d, tx, ty).
 */
class CatRigidOp(
    z: Int,
    order: Int,
    texture: String,
    material: PartMaterial?,
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
) : CatDrawOp(z, order, texture, material) {
    val matrix = FloatArray(6)
}

/** 코드로 그리는 모양. 그림 에셋이 아니라 엔진이 정한 매개변수로 그린다. */
enum class CatShape {
    /** 감은 눈 선(아래로 휜 ‿). params: 가운데 x, y, 반폭, 휨 깊이, 선 굵기 */
    CLOSED_EYE,

    /** 웃는 눈(위로 휜 ∩). params: 가운데 x, y, 반폭, 휨 높이, 선 굵기 */
    SMILE_EYE,

    /** 벌린 입. params: 가운데 x, 위 y, 폭, 높이 */
    OPEN_MOUTH,

    /** 삐진 입(∩). params: 가운데 x, y, 반폭, 높이, 선 굵기 */
    POUT_MOUTH,

    /** 볼 붉어짐. params: 왼쪽 볼 x, y, 오른쪽 볼 x, y, 반지름 */
    BLUSH,

    /** 바닥 그림자 타원. params: 가운데 x, y, 반폭, 반높이 */
    SHADOW,

    /** 잠 Zzz. params: 시작 x, y, 크기, 위상(0..1) */
    ZZZ,
}

/** 코드로 그리는 모양 하나. [matrix]로 휴지 좌표를 옮겨 그린다. */
class CatShapeOp(z: Int, order: Int, val shape: CatShape) : CatDrawOp(z, order, "shape:${shape.name}", null) {
    val matrix = FloatArray(6)
    val params = FloatArray(6)
}

/**
 * 한 프레임에 그릴 목록. [ops]는 그리는 순서(뒤 -> 앞)로 정렬돼 있다.
 */
class CatRigFrame(val ops: List<CatDrawOp>)
