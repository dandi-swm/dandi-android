package com.dandi.nyummy.cat.presentation

import androidx.compose.runtime.Immutable
import com.dandi.nyummy.cat.entity.rig.CatRigCharacterVO

/**
 * 화면 상태에 담는 리그 고양이. 값이 바뀌지 않는 VO를 감싸 Compose가 안정 타입으로 다루게 한다.
 * 화면 상태에 이것이 있으면 냐미를 리그로, 없으면 스프라이트로 그린다.
 */
@Immutable
data class CatRigUiModel(val character: CatRigCharacterVO)
