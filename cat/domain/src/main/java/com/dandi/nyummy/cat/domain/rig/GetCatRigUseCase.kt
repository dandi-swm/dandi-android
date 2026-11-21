package com.dandi.nyummy.cat.domain.rig

import com.dandi.nyummy.cat.entity.rig.CatRigCharacterVO
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * 화면이 그릴 리그 고양이(스킨 + 지금 체형 + 끼운 아이템). 리그가 꺼져 있으면 null이라 화면은 스프라이트 냐미를 그린다.
 * 리그 데이터를 읽지 못해도 스프라이트로 되돌아가면 되므로 다이얼로그 없이 null을 돌려준다.
 */
class GetCatRigUseCase @Inject constructor(
    private val repository: CatRigRepository,
) {
    suspend operator fun invoke(): CatRigCharacterVO? = try {
        if (!repository.isEnabled()) {
            null
        } else {
            val character = repository.getCharacter(repository.selectedSkin())
            val ids = repository.equippedItemIds()
            val items = if (ids.isEmpty()) emptyList() else repository.getItems().filter { it.id in ids }
            character?.copy(equipped = items.associateBy { it.slot })
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }
}
