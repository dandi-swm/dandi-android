package com.dandi.nyummy.common.domain.message

sealed interface MessageEffect {
    data class ShowToastMsg(val message: String) : MessageEffect
    /**
     * 스낵바. [actionLabel]이 있으면 오른쪽에 글자 버튼(예: 되돌리기)을 두고, 누르면 [onAction]을 부른다.
     */
    data class ShowSnackBarError(
        val message: String,
        val actionLabel: String? = null,
        val onAction: (() -> Unit)? = null,
    ) : MessageEffect
    data class ShowOneButtonDialog(
        val titleText: String?,
        val descText: String,
        val cantIgnore: Boolean,
        val buttonText: String,
        val onClickButton: (() -> Unit)?,
    ) : MessageEffect

    data class ShowTwoButtonDialog(
        val titleText: String?,
        val descText: String,
        val cantIgnore: Boolean,
        val leftButtonText: String,
        val onClickLeftButton: (() -> Unit)?,
        val rightButtonText: String,
        val onClickRightButton: (() -> Unit)?,
    ) : MessageEffect
}
