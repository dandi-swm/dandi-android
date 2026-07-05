package com.swm.dandi.common.domain.favorite

import com.swm.dandi.common.domain.base.BaseUseCase
import com.swm.dandi.common.domain.helper.MessageHelper
import com.swm.dandi.common.domain.helper.NavigationHelper
import com.swm.dandi.common.domain.helper.ResourceHelper
import com.swm.dandi.common.domain.helper.StringResource
import com.swm.dandi.common.domain.message.IconType
import com.swm.dandi.tti.TTIHelper
import javax.inject.Inject

class RemoveFavoriteItemUseCase @Inject constructor(
    private val favoriteRepository: FavoriteRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {
    suspend operator fun invoke(url: String): Result<Unit> =
        runCatching { favoriteRepository.deleteFavoriteItem(url) }
            .onFailure {
                messageHelper.showSnackBar(
                    iconType = IconType.ERROR,
                    messageText = resourceHelper.getString(StringResource.FAVORITE_REMOVE_FAILED),
                )
            }
}
