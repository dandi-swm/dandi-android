package com.swm.dandi.common.presentation.helper

import android.content.Context
import com.swm.dandi.common.domain.helper.ResourceHelper
import com.swm.dandi.common.domain.helper.StringResource
import com.swm.dandi.common.presentation.R

class ResourceHelperImpl(private val context: Context) : ResourceHelper {
    override fun getString(resource: StringResource): String =
        context.getString(resource.toResId())

    private fun StringResource.toResId(): Int = when (this) {
        StringResource.APP_NAME -> R.string.app_name
        StringResource.MEDIA_EMPTY -> R.string.media_empty
        StringResource.FAVORITE_EMPTY -> R.string.favorite_empty
        StringResource.MEDIA_DEFAULT_TITLE -> R.string.media_default_title
        StringResource.NAV_TAB_SEARCH -> R.string.nav_tab_search
        StringResource.NAV_TAB_FAVORITE -> R.string.nav_tab_favorite
        StringResource.NAV_BACK_BUTTON -> R.string.nav_back_button
        StringResource.FAVORITE_REGISTER_FAILED -> R.string.favorite_register_failed
        StringResource.FAVORITE_REMOVE_FAILED -> R.string.favorite_remove_failed
        StringResource.SEARCH_HINT -> R.string.search_hint
        StringResource.SEARCH_CLEAR -> R.string.search_clear
        StringResource.SEARCH_FAILED -> R.string.search_failed
        StringResource.SEARCH_PROMPT -> R.string.search_prompt
        StringResource.LIST_END_MARKER -> R.string.list_end_marker
    }
}
