package com.dandi.nyummy.settings.presentation.profile

import com.dandi.nyummy.common.presentation.mvi.ReducerEvent
import com.dandi.nyummy.settings.entity.MyProfileChange
import com.dandi.nyummy.settings.entity.MyProfileVO

sealed interface MyProfileReducerEvent : ReducerEvent {
    data class Loaded(val profile: MyProfileVO) : MyProfileReducerEvent

    /** [field] 시트를 [draft]로 채워 연다. */
    data class SheetOpened(val field: MyProfileField, val draft: MyProfileDraft) : MyProfileReducerEvent

    data object SheetClosed : MyProfileReducerEvent

    /** 시트에서 값을 고쳤다. 이름 입력이 바뀌면 이름 오류를 지운다. */
    data class DraftChanged(val draft: MyProfileDraft) : MyProfileReducerEvent

    data class NameInvalid(val error: NameInputError) : MyProfileReducerEvent

    data class SavingChanged(val saving: Boolean) : MyProfileReducerEvent

    /** 서버에 저장한 [change]를 화면 값에 반영한다. */
    data class ProfileChanged(val change: MyProfileChange) : MyProfileReducerEvent
}
