package com.swm.dandi.common.domain.favorite

import com.swm.dandi.common.domain.base.BaseUseCase
import com.swm.dandi.common.domain.helper.MessageHelper
import com.swm.dandi.common.domain.helper.NavigationHelper
import com.swm.dandi.common.domain.helper.ResourceHelper
import com.swm.dandi.common.entity.favorite.FavoriteItemVO
import com.swm.dandi.tti.TTIHelper
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetFavoriteItemsUseCase @Inject constructor(
    private val favoriteRepository: FavoriteRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {
    operator fun invoke(): Flow<List<FavoriteItemVO>> =
        favoriteRepository.getFavoriteItemsFlow()
}
