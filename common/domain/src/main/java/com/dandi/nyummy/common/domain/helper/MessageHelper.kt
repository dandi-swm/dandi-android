package com.dandi.nyummy.common.domain.helper

import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.common.domain.message.MessageEffect
import kotlinx.coroutines.flow.Flow

interface MessageHelper {
    val effect: Flow<MessageEffect>
    /** 누를 것 없는 짧은 안내. 화면 하단에 잠깐 떴다 사라진다. 버튼이 필요하면 [showSnackBar]를 쓴다. */
    fun showToast(toastMsg: String)
    fun showToast(messageRes: Int)
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

    /** 버튼 글자를 비워 두면(기본값) 기기 언어의 "확인"·"취소"를 쓴다. */
    fun showOneButtonDialog(
        titleText: String? = null,
        descText: String,
        cantIgnore: Boolean = false,
        buttonText: String = "",
        onClickButton: (() -> Unit)? = null,
    )

    /**
     * 사용자가 확인을 누를 때까지 닫을 수 없고, 다른 다이얼로그가 덮지 못하는 1버튼 다이얼로그(로그인 만료 안내).
     * 떠 있는 동안 오는 다른 다이얼로그는 버린다. 버튼을 누르는 것만이 이 안내를 끝낸다.
     * 기본 구현은 닫을 수 없는 [showOneButtonDialog]다(테스트 대역용). 앱 구현은 덮지 못하게 표시해 내보낸다.
     */
    fun showPinnedDialog(descText: String, buttonText: String, onClickButton: () -> Unit) {
        showOneButtonDialog(descText = descText, cantIgnore = true, buttonText = buttonText, onClickButton = onClickButton)
    }

    fun showTwoButtonDialog(
        titleText: String? = null,
        descText: String,
        cantIgnore: Boolean = false,
        leftButtonText: String = "",
        onClickLeftButton: (() -> Unit)? = null,
        rightButtonText: String = "",
        onClickRightButton: (() -> Unit)? = null,
    )
}
