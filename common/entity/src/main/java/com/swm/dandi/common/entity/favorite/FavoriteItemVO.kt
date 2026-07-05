package com.swm.dandi.common.entity.favorite

import com.swm.dandi.common.entity.media.MediaType

data class FavoriteItemVO(
    val type: MediaType,
    val title: String,
    val urlKey: String,
    val thumbnailUrl: String = "",
    val contentsImageUrl: String = "",
    val dateTime: String = "",
)
