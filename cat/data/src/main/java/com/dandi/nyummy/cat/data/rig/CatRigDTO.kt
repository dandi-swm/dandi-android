package com.dandi.nyummy.cat.data.rig

import com.dandi.nyummy.cat.entity.rig.AttachmentArtVO
import com.dandi.nyummy.cat.entity.rig.AttachmentItemVO
import com.dandi.nyummy.cat.entity.rig.AttachmentSlot
import com.dandi.nyummy.cat.entity.rig.CatBodyRigVO
import com.dandi.nyummy.cat.entity.rig.CatBone
import com.dandi.nyummy.cat.entity.rig.CatCharacterSpec
import com.dandi.nyummy.cat.entity.rig.CatSkinId
import com.dandi.nyummy.cat.entity.rig.ColorTransformVO
import com.dandi.nyummy.cat.entity.rig.EarLayerMode
import com.dandi.nyummy.cat.entity.rig.PartMaterial
import com.dandi.nyummy.cat.entity.rig.RigBoxVO
import com.dandi.nyummy.cat.entity.rig.RigEarVO
import com.dandi.nyummy.cat.entity.rig.RigEyeVO
import com.dandi.nyummy.cat.entity.rig.RigLayerVO
import com.dandi.nyummy.cat.entity.rig.RigMouthVO
import com.dandi.nyummy.cat.entity.rig.RigPaletteVO
import com.dandi.nyummy.cat.entity.rig.RigPointVO
import kotlinx.serialization.Serializable

/**
 * 앱 에셋 `cats/rig/parts/<파츠 묶음>/<체형>/rig.json`. `tools/cat-rig/extract_parts.py`가 만든다.
 * 점은 [x, y], 사각형은 [왼쪽, 위, 오른쪽, 아래] 배열이고 단위는 리그 논리 좌표(u)다.
 */
@Serializable
data class CatBodyRigDTO(
    val version: Int? = null,
    val body: String? = null,
    val ground: Float? = null,
    val neck: List<Float>? = null,
    val headBox: List<Float>? = null,
    val bodyBox: List<Float>? = null,
    val bandanaBox: List<Float>? = null,
    val tailBase: List<Float>? = null,
    val tailTip: List<Float>? = null,
    val knot: List<Float>? = null,
    val earL: RigEarDTO? = null,
    val earR: RigEarDTO? = null,
    val eyeL: RigEyeDTO? = null,
    val eyeR: RigEyeDTO? = null,
    val mouth: RigMouthDTO? = null,
    val palette: Map<String, List<Int>>? = null,
    val layers: List<RigLayerDTO>? = null,
) {
    /** 파츠가 하나도 없으면 그릴 수 없으므로 null이다. [assetDir]는 파츠 그림 폴더(끝에 /). */
    fun toVO(assetDir: String): CatBodyRigVO? {
        val parts = layers.orEmpty().mapNotNull { it.toVO() }
        if (parts.isEmpty()) return null
        return CatBodyRigVO(
            body = body.orEmpty(),
            ground = ground ?: DEFAULT_GROUND,
            neck = neck.point(),
            headBox = headBox.box(),
            bodyBox = bodyBox.box(),
            bandanaBox = bandanaBox.box(),
            tailBase = tailBase.point(),
            tailTip = tailTip.point(),
            knot = knot.point(),
            earL = earL?.toVO() ?: RigEarVO(),
            earR = earR?.toVO() ?: RigEarVO(),
            eyeL = eyeL?.toVO() ?: RigEyeVO(),
            eyeR = eyeR?.toVO() ?: RigEyeVO(),
            mouth = mouth?.toVO() ?: RigMouthVO(),
            palette = palette.toPalette(),
            layers = parts,
            assetDir = assetDir,
        )
    }
}

/** rig.json에 바닥 y가 없을 때 쓰는 값(리그 논리 좌표의 바닥 앵커). */
private const val DEFAULT_GROUND = 118f

@Serializable
data class RigEarDTO(val base: List<Float>? = null, val top: List<Float>? = null, val tip: List<Float>? = null) {
    fun toVO() = RigEarVO(base = base.point(), top = top.point(), tip = tip.point())
}

@Serializable
data class RigEyeDTO(val center: List<Float>? = null, val radiusX: Float? = null, val radiusY: Float? = null) {
    fun toVO() = RigEyeVO(center = center.point(), radiusX = radiusX ?: 0f, radiusY = radiusY ?: 0f)
}

@Serializable
data class RigMouthDTO(val center: List<Float>? = null, val w: Float? = null, val h: Float? = null) {
    fun toVO() = RigMouthVO(center = center.point(), width = w ?: 0f, height = h ?: 0f)
}

@Serializable
data class RigLayerDTO(
    val id: String? = null,
    val file: String? = null,
    val x: Float? = null,
    val y: Float? = null,
    val w: Float? = null,
    val h: Float? = null,
) {
    fun toVO(): RigLayerVO? {
        val layerId = id?.takeIf { it.isNotBlank() } ?: return null
        val name = file?.takeIf { it.isNotBlank() } ?: return null
        return RigLayerVO(id = layerId, file = name, x = x ?: 0f, y = y ?: 0f, width = w ?: 0f, height = h ?: 0f)
    }
}

/**
 * 앱 에셋 `cats/rig/skins/<스킨 id>.json`. 새 고양이는 이 파일(+ 원화가 오면 파츠 묶음)만 더한다.
 *
 * @property id 스킨 id
 * @property partSet 파츠 묶음 이름
 * @property materials 재질 이름(FUR, ACCESSORY, FACE) -> 색 변환
 * @property pivotOffsets 본 이름(CatBone) -> [x, y] 오프셋(u)
 * @property showDefaultBandana 기본 반다나를 그리는지
 * @property clipOverrides 동작 이름 -> 클립 이름
 */
@Serializable
data class CatSkinDTO(
    val id: String? = null,
    val partSet: String? = null,
    val materials: Map<String, ColorTransformDTO>? = null,
    val pivotOffsets: Map<String, List<Float>>? = null,
    val showDefaultBandana: Boolean? = null,
    val clipOverrides: Map<String, String>? = null,
) {
    fun toVO(): CatCharacterSpec? {
        val skin = id?.takeIf { it.isNotBlank() } ?: return null
        return CatCharacterSpec(
            id = CatSkinId(skin),
            partSet = partSet?.takeIf { it.isNotBlank() } ?: skin,
            materials = materials.orEmpty().mapNotNull { (k, v) ->
                PartMaterial.entries.firstOrNull { it.name == k }?.let { it to v.toVO() }
            }.toMap(),
            pivotOffsets = pivotOffsets.orEmpty().mapNotNull { (k, v) ->
                CatBone.entries.firstOrNull { it.name == k }?.let { it to v.point() }
            }.toMap(),
            showDefaultBandana = showDefaultBandana ?: true,
            clipOverrides = clipOverrides.orEmpty(),
        )
    }
}

/** 색 변환. multiply, add는 [r, g, b]. */
@Serializable
data class ColorTransformDTO(
    val saturation: Float? = null,
    val multiply: List<Float>? = null,
    val add: List<Float>? = null,
) {
    fun toVO() = ColorTransformVO(
        saturation = saturation ?: 1f,
        multiplyR = multiply?.getOrNull(0) ?: 1f,
        multiplyG = multiply?.getOrNull(1) ?: 1f,
        multiplyB = multiply?.getOrNull(2) ?: 1f,
        addR = add?.getOrNull(0) ?: 0f,
        addG = add?.getOrNull(1) ?: 0f,
        addB = add?.getOrNull(2) ?: 0f,
    )
}

/** 앱 에셋 `cats/rig/items.json`. 착용 아이템 목록. */
@Serializable
data class CatRigItemsDTO(val items: List<AttachmentItemDTO>? = null) {
    fun toVO(): List<AttachmentItemVO> = items.orEmpty().mapNotNull { it.toVO() }
}

@Serializable
data class AttachmentItemDTO(
    val id: String? = null,
    val slot: String? = null,
    val art: String? = null,
    val backArt: String? = null,
    val width: Float? = null,
    val height: Float? = null,
    val anchor: List<Float>? = null,
    val earLayerMode: String? = null,
    val brimY: Float? = null,
    val hidesDefaultBandana: Boolean? = null,
    val lineColor: List<Int>? = null,
) {
    /** id, 슬롯, 그림, 크기가 없으면 끼울 수 없으므로 null이다. */
    fun toVO(): AttachmentItemVO? {
        val itemId = id?.takeIf { it.isNotBlank() } ?: return null
        val itemSlot = AttachmentSlot.entries.firstOrNull { it.name == slot } ?: return null
        val key = art?.takeIf { it.isNotBlank() } ?: return null
        val w = width?.takeIf { it > 0f } ?: return null
        val h = height?.takeIf { it > 0f } ?: return null
        return AttachmentItemVO(
            id = itemId,
            slot = itemSlot,
            art = AttachmentArtVO(key),
            backArt = backArt?.takeIf { it.isNotBlank() }?.let { AttachmentArtVO(it) },
            width = w,
            height = h,
            anchorX = anchor?.getOrNull(0) ?: 0.5f,
            anchorY = anchor?.getOrNull(1) ?: 0.5f,
            earLayerMode = EarLayerMode.entries.firstOrNull { it.name == earLayerMode } ?: EarLayerMode.BETWEEN_EARS,
            brimY = brimY ?: 1f,
            hidesDefaultBandana = hidesDefaultBandana ?: false,
            lineColor = lineColor?.toArgb(),
        )
    }
}

/** [r, g, b] -> 불투명 ARGB. 세 값이 없으면 null이다. */
private fun List<Int>.toArgb(): Int? {
    if (size < 3) return null
    return (0xFF shl 24) or ((this[0] and 0xFF) shl 16) or ((this[1] and 0xFF) shl 8) or (this[2] and 0xFF)
}

/** "line", "pupil", "blush" -> [r, g, b]. 없는 색은 기본 냐미 색을 쓴다. */
private fun Map<String, List<Int>>?.toPalette(): RigPaletteVO {
    val base = RigPaletteVO()
    fun argb(key: String, fallback: Int): Int = this?.get(key)?.toArgb() ?: fallback
    return RigPaletteVO(line = argb("line", base.line), pupil = argb("pupil", base.pupil), blush = argb("blush", base.blush))
}

private fun List<Float>?.point(): RigPointVO = RigPointVO(this?.getOrNull(0) ?: 0f, this?.getOrNull(1) ?: 0f)

private fun List<Float>?.box(): RigBoxVO =
    RigBoxVO(this?.getOrNull(0) ?: 0f, this?.getOrNull(1) ?: 0f, this?.getOrNull(2) ?: 0f, this?.getOrNull(3) ?: 0f)
