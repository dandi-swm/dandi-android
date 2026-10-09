package com.dandi.nyummy.common.domain.helper

import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.common.domain.message.MessageEffect
import kotlinx.coroutines.flow.Flow

interface MessageHelper {
    val effect: Flow<MessageEffect>
    fun showToast(toastMsg: String)
    fun showSnackBar(
        iconType: IconType = IconType.SUCCESS,
        messageText: String,
        callToActionText: String? = null,
        onClickCTA: (() -> Unit)? = null,
    )
    fun showSnackBar(
        iconType: IconType = IconType.SUCCESS,
        messageRes: Int,
        callToActionText: String? = null,
        onClickCTA: (() -> Unit)? = null,
    )

    fun showOneButtonDialog(
        titleText: String? = null,
        descText: String,
        cantIgnore: Boolean = false,
        buttonText: String = "확인",
        onClickButton: (() -> Unit)? = null,
    )

    fun showTwoButtonDialog(
        titleText: String? = null,
        descText: String,
        cantIgnore: Boolean = false,
        leftButtonText: String = "취소",
        onClickLeftButton: (() -> Unit)? = null,
        rightButtonText: String = "확인",
        onClickRightButton: (() -> Unit)? = null,
    )
}
