package com.dandi.nyummy.common.presentation.helper

import android.content.Context
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.helper.StringResource
import com.dandi.nyummy.common.presentation.R

class ResourceHelperImpl(private val context: Context) : ResourceHelper {
    override fun getString(resource: StringResource): String =
        context.getString(resource.toResId())

    private fun StringResource.toResId(): Int = when (this) {
        StringResource.APP_NAME -> R.string.app_name
        StringResource.NAV_BACK_BUTTON -> R.string.nav_back_button
    }
}
