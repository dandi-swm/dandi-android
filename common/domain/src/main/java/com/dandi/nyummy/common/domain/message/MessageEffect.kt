package com.dandi.nyummy.common.domain.message

sealed interface MessageEffect {
    data class ShowToastMsg(val message: String) : MessageEffect
    data class ShowSnackBarError(val message: String) : MessageEffect
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
