package com.dandi.nyummy.common.domain.base

import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.navigation.NavRoute
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.tti.TTIHelper

open class BaseUseCase(
    protected open val resourceHelper: ResourceHelper,
    protected open val messageHelper: MessageHelper,
    protected open val navigationHelper: NavigationHelper,
    protected open val ttiHelper: TTIHelper,
) {

    fun executeCommonErrorHanding(e: HttpResponseException) {
        when (e.rawCode) {
            401 -> {
                messageHelper.showOneButtonDialog(
                    cantIgnore = true,
                    descText = "Session expired. Please login again.",
                    buttonText = "Move to login",
                    // common 은 feature(auth)를 참조할 수 없어 LoginPage.PATH("/login") 리터럴을 쓴다.
                    onClickButton = { navigationHelper.navigateByRoute(NavRoute("/login")) },
                )
            }

            404 -> {
                messageHelper.showOneButtonDialog(
                    cantIgnore = true,
                    descText = "This feature is not currently available in the app version.",
                    buttonText = "Move to back",
                    onClickButton = {
                        navigationHelper.navigateToBack()
                    }
                )
            }

            else -> {
                messageHelper.showOneButtonDialog(
                    titleText = "A temporary error occurred.",
                    descText = "error status : ${e.print()}",
                )
            }
        }
    }
}
