package com.dandi.nyummy.settings.presentation.profile

import com.dandi.nyummy.auth.entity.Gender
import com.dandi.nyummy.common.presentation.designsystem.component.birthDayOfMonth
import com.dandi.nyummy.common.presentation.mvi.UiState
import com.dandi.nyummy.settings.entity.MyProfileVO
import java.time.LocalDate

/** 마이페이지에서 시트로 고치는 항목. */
enum class MyProfileField { CAT_NAME, NICKNAME, GENDER, BIRTH, HEIGHT, WEIGHT }

/** 이름 시트(고양이 이름, 집사 이름)의 입력 오류. */
enum class NameInputError { EMPTY, CAT_NAME_TOO_LONG }

/**
 * 시트에서 고치는 중인 값. 시트를 열 때 지금 값으로 채우고, 저장을 눌러야 서버에 보낸다.
 *
 * @property text 이름 시트의 입력.
 * @property initialText 시트를 열 때의 이름. 그대로 저장하면 서버에 보내지 않는다.
 * @property gender 성별 시트에서 고른 값. 서버가 비워 뒀거나 앱에서 고를 수 없는 값(OTHER)이면 null이고 저장을 막는다.
 */
data class MyProfileDraft(
    val text: String = "",
    val initialText: String = "",
    val gender: Gender? = null,
    val birthYear: Int = DEFAULT_BIRTH.year,
    val birthMonth: Int = DEFAULT_BIRTH.monthValue,
    val birthDay: Int = DEFAULT_BIRTH.dayOfMonth,
    val heightCm: Int = DEFAULT_HEIGHT_CM,
    val weightKg: Int = DEFAULT_WEIGHT_KG,
) {
    /** 고른 생년월일. 고른 일이 그달에 없으면(2월 30일 등) 그달 마지막 날이다. */
    val birth: LocalDate
        get() = LocalDate.of(birthYear, birthMonth, birthDayOfMonth(birthYear, birthMonth, birthDay))

    companion object {
        /** 서버가 비워 둔 값을 고칠 때 피커가 처음 가리키는 값. 회원가입 첫 값과 같다. */
        val DEFAULT_BIRTH: LocalDate = LocalDate.of(2000, 3, 30)
        const val DEFAULT_HEIGHT_CM = 172
        const val DEFAULT_WEIGHT_KG = 70
    }
}

/**
 * 마이페이지 UI 상태. 고양이 이름은 앱 루트가 기기에 기억한 이름(`LocalCatName`)을 화면이 바로 그린다.
 *
 * @property isLoaded 내 프로필을 읽었다. 읽기 전에는 집사 정보 줄을 눌러도 시트를 열지 않는다.
 * @property editing 열려 있는 시트. 닫혀 있으면 null.
 * @property nameError 이름 시트의 입력 오류. 입력을 고치면 지운다.
 * @property isSaving 저장 중. 저장 버튼과 시트 닫기를 막는다.
 */
data class MyProfileUIState(
    val isLoaded: Boolean = false,
    val profile: MyProfileVO = MyProfileVO.empty,
    val editing: MyProfileField? = null,
    val draft: MyProfileDraft = MyProfileDraft(),
    val nameError: NameInputError? = null,
    val isSaving: Boolean = false,
) : UiState {

    /** 시트의 저장 버튼을 누를 수 있다. 성별은 남성·여성 중 하나를 골라야 한다. */
    val isSaveEnabled: Boolean
        get() = !isSaving && when (editing) {
            null -> false
            MyProfileField.GENDER -> draft.gender == Gender.MALE || draft.gender == Gender.FEMALE
            else -> true
        }

    companion object {
        val empty = MyProfileUIState()
    }
}
