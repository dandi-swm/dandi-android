package com.dandi.nyummy.history.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.common.domain.session.SessionExpiryHandler
import com.dandi.nyummy.history.entity.MealHistoryVO
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject

/** 식사 단건 상세 조회 (사진 URL 포함). */
class GetMealDetailUseCase @Inject constructor(
    private val repository: HistoryRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
    sessionExpiryHandler: SessionExpiryHandler? = null,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper, sessionExpiryHandler) {

    // 네트워크 끊김 등 응답 전 실패는 따로 안내하지 않는다. 상세는 목록 데이터로 이미 열려 있다.
    suspend operator fun invoke(mealId: Long): Result<MealHistoryVO> = request(
        HistoryErrorType.entries,
        onBusinessError = { type, cause ->
            messageHelper.showSnackBar(iconType = IconType.ERROR, messageText = resourceHelper.getString(type.errorMsg))
            Result.failure(cause)
        },
    ) {
        repository.getMeal(mealId)
    }
}
