package com.dandi.nyummy.common.domain.message

sealed interface MessageEffect {
    /** 앱 안 토스트. 누를 것 없는 짧은 안내를 화면 하단에 잠깐 띄운다. */
    data class ShowToastMsg(val message: String) : MessageEffect
    /**
     * 스낵바. [actionLabel]이 있으면 오른쪽에 글자 버튼(예: 되돌리기)을 두고, 누르면 [onAction]을 부른다.
     */
    data class ShowSnackBarError(
        val message: String,
        val actionLabel: String? = null,
        val onAction: (() -> Unit)? = null,
    ) : MessageEffect
    /**
     * 1버튼 다이얼로그. [pinned]면 떠 있는 동안 다른 다이얼로그가 덮지 못한다([canShowDialogOver]).
     */
    data class ShowOneButtonDialog(
        val titleText: String?,
        val descText: String,
        val cantIgnore: Boolean,
        val buttonText: String,
        val onClickButton: (() -> Unit)?,
        val pinned: Boolean = false,
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

/**
 * 지금 떠 있는 1버튼 다이얼로그 [shown] 위에 [incoming] 다이얼로그를 띄울지.
 *
 * 고정 다이얼로그(로그인 만료)가 떠 있으면 다른 다이얼로그는 버린다. 덮으면 그 확인 버튼의 동작(정리 뒤 로그인 화면으로 가기)이
 * 사라져, 토큰이 지워진 화면에 갇힌다. 다이얼로그를 그리는 곳(RootComposable)이 다이얼로그를 받을 때마다 묻는다.
 */
fun canShowDialogOver(shown: MessageEffect.ShowOneButtonDialog?, incoming: MessageEffect): Boolean =
    shown?.pinned != true || (incoming is MessageEffect.ShowOneButtonDialog && incoming.pinned)
