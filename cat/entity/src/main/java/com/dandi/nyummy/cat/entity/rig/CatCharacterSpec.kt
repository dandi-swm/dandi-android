package com.dandi.nyummy.cat.entity.rig

/** 고양이 스킨 id. 기본은 냐미([NYAMI])이고, 상점 고양이가 늘면 데이터(스킨 JSON)만 더한다. */
@JvmInline
value class CatSkinId(val value: String) {
    companion object {
        val NYAMI = CatSkinId("nyami")
    }
}

/** 파츠 재질. 스킨의 색 변환이 재질 단위로 걸린다. */
enum class PartMaterial {
    /** 털(머리, 귀, 몸, 꼬리). 윤곽선도 같이 바뀐다. */
    FUR,

    /** 기본 반다나 */
    ACCESSORY,

    /** 눈, 코, 입 */
    FACE,
}

/**
 * 색 변환. 결과 = 채도 조절(원래 색, [saturation]) x 곱 + 더하기. 값은 0..1 색 기준이다.
 * 변환 없음은 [IDENTITY]다.
 */
data class ColorTransformVO(
    val saturation: Float = 1f,
    val multiplyR: Float = 1f,
    val multiplyG: Float = 1f,
    val multiplyB: Float = 1f,
    val addR: Float = 0f,
    val addG: Float = 0f,
    val addB: Float = 0f,
) {
    val isIdentity: Boolean get() = this == IDENTITY

    companion object {
        val IDENTITY = ColorTransformVO()
    }
}

/**
 * 고양이 한 종(스킨)의 정의(`cats/rig/skins/<스킨 id>.json`).
 * 엔진은 스킨을 이 데이터로만 안다. 새 고양이는 이 JSON(+ 원화가 오면 파츠 묶음 하나)만 더하면
 * 같은 스켈레톤 채널을 쓰는 클립 전부와 착용 슬롯 전부가 그대로 맞는다.
 *
 * @property id 스킨 id
 * @property partSet 파츠 묶음 이름(`cats/rig/parts/<partSet>/<체형>/`)
 * @property materials 재질별 색 변환. 없는 재질은 변환하지 않는다.
 * @property pivotOffsets 본별 미세 오프셋(u). 본과 그 본에 붙은 파츠가 함께 옮겨진다.
 * @property showDefaultBandana 기본 반다나를 그리는지(목 장식을 끼우면 이 값과 상관없이 숨긴다)
 * @property clipOverrides 이 고양이만 다른 클립을 쓰는 동작(동작 이름 -> 클립 이름)
 */
data class CatCharacterSpec(
    val id: CatSkinId = CatSkinId.NYAMI,
    val partSet: String = "nyami",
    val materials: Map<PartMaterial, ColorTransformVO> = emptyMap(),
    val pivotOffsets: Map<CatBone, RigPointVO> = emptyMap(),
    val showDefaultBandana: Boolean = true,
    val clipOverrides: Map<String, String> = emptyMap(),
) {
    fun colorOf(material: PartMaterial): ColorTransformVO = materials[material] ?: ColorTransformVO.IDENTITY

    fun pivotOffset(bone: CatBone): RigPointVO = pivotOffsets[bone] ?: ZERO

    private companion object {
        val ZERO = RigPointVO()
    }
}

/**
 * 화면이 그릴 리그 고양이 하나: 스킨 정의 + 지금 체형의 파츠 묶음 + 끼운 아이템.
 *
 * @property spec 스킨 정의
 * @property bodyRig 지금 체형의 파츠 묶음
 * @property equipped 슬롯별로 끼운 아이템
 */
data class CatRigCharacterVO(
    val spec: CatCharacterSpec = CatCharacterSpec(),
    val bodyRig: CatBodyRigVO = CatBodyRigVO(),
    val equipped: Map<AttachmentSlot, AttachmentItemVO> = emptyMap(),
)
