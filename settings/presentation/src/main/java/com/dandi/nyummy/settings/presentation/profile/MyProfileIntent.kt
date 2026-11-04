package com.dandi.nyummy.settings.presentation.profile

import com.dandi.nyummy.auth.entity.Gender
import com.dandi.nyummy.common.presentation.mvi.MviIntent

/** 마이페이지에서 발생하는 사용자 의도. */
sealed interface MyProfileIntent : MviIntent {

    /** 화면에 들어왔다. 내 프로필을 읽는다. */
    data object Enter : MyProfileIntent

    data object ClickBack : MyProfileIntent

    /**
     * 고양이 이름 줄을 눌렀다. 이름 시트를 연다.
     *
     * @param currentName 지금 보이는 고양이 이름. 화면이 기기에 기억한 이름을 넘긴다.
     */
    data class ClickCatName(val currentName: String) : MyProfileIntent

    /** 집사 정보 줄([MyProfileField.CAT_NAME] 외)을 눌렀다. 그 항목의 시트를 연다. */
    data class ClickField(val field: MyProfileField) : MyProfileIntent

    /** 이름 시트에 입력했다. */
    data class InputName(val value: String) : MyProfileIntent

    data class SelectGender(val gender: Gender) : MyProfileIntent

    data class SelectBirthYear(val year: Int) : MyProfileIntent

    data class SelectBirthMonth(val month: Int) : MyProfileIntent

    data class SelectBirthDay(val day: Int) : MyProfileIntent

    data class SelectHeight(val cm: Int) : MyProfileIntent

    data class SelectWeight(val kg: Int) : MyProfileIntent

    /** 시트의 저장을 눌렀다. 바뀐 값이 있을 때만 서버에 보낸다. */
    data object ClickSave : MyProfileIntent

    /** 시트를 닫았다(바깥을 누르거나 아래로 내림). 저장 중에는 닫지 않는다. */
    data object DismissSheet : MyProfileIntent
}
