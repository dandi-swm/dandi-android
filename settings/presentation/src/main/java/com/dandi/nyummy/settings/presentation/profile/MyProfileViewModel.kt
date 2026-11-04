package com.dandi.nyummy.settings.presentation.profile

import androidx.lifecycle.viewModelScope
import com.dandi.nyummy.auth.domain.LogoutUseCase
import com.dandi.nyummy.auth.domain.SignUpFieldError
import com.dandi.nyummy.auth.domain.SignUpValidator
import com.dandi.nyummy.auth.entity.Gender
import com.dandi.nyummy.auth.entity.UserProfileRange
import com.dandi.nyummy.cat.domain.RenameCatUseCase
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.entity.cat.CatNameError
import com.dandi.nyummy.common.entity.cat.CatNameValidator
import com.dandi.nyummy.common.entity.time.DeviceTime
import com.dandi.nyummy.common.presentation.designsystem.component.isBirthAfter
import com.dandi.nyummy.common.presentation.mvi.MviViewModel
import com.dandi.nyummy.settings.domain.GetMyProfileUseCase
import com.dandi.nyummy.settings.domain.UpdateMyProfileUseCase
import com.dandi.nyummy.settings.entity.MyProfileChange
import com.dandi.nyummy.settings.entity.MyProfileVO
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyProfileViewModel @Inject constructor(
    private val getMyProfile: GetMyProfileUseCase,
    private val updateMyProfile: UpdateMyProfileUseCase,
    private val renameCat: RenameCatUseCase,
    private val logout: LogoutUseCase,
    private val navigationHelper: NavigationHelper,
) : MviViewModel<MyProfileIntent, MyProfileUIState, MyProfileReducerEvent>(MyProfileUIState.empty) {

    override fun onIntent(intent: MyProfileIntent) {
        when (intent) {
            MyProfileIntent.Enter -> load()
            MyProfileIntent.ClickBack -> navigationHelper.navigateToBack()
            is MyProfileIntent.ClickCatName ->
                open(MyProfileField.CAT_NAME, MyProfileDraft(text = intent.currentName, initialText = intent.currentName))
            is MyProfileIntent.ClickField -> openProfileField(intent.field)
            is MyProfileIntent.InputName -> editDraft { it.copy(text = intent.value) }
            is MyProfileIntent.SelectGender -> editDraft { it.copy(gender = intent.gender) }
            is MyProfileIntent.SelectBirthYear -> editDraft { it.copy(birthYear = intent.year).notAfterToday() }
            is MyProfileIntent.SelectBirthMonth -> editDraft { it.copy(birthMonth = intent.month).notAfterToday() }
            is MyProfileIntent.SelectBirthDay -> editDraft { it.copy(birthDay = intent.day).notAfterToday() }
            is MyProfileIntent.SelectHeight -> editDraft { it.copy(heightCm = intent.cm) }
            is MyProfileIntent.SelectWeight -> editDraft { it.copy(weightKg = intent.kg) }
            MyProfileIntent.ClickSave -> save()
            MyProfileIntent.DismissSheet -> if (!currentState.isSaving) dispatch(MyProfileReducerEvent.SheetClosed)
        }
    }

    override fun reduce(state: MyProfileUIState, event: MyProfileReducerEvent): MyProfileUIState = when (event) {
        is MyProfileReducerEvent.Loaded -> state.copy(isLoaded = true, profile = event.profile)
        is MyProfileReducerEvent.SheetOpened -> state.copy(editing = event.field, draft = event.draft, nameError = null)
        MyProfileReducerEvent.SheetClosed -> state.copy(editing = null, nameError = null)
        is MyProfileReducerEvent.DraftChanged -> state.copy(
            draft = event.draft,
            nameError = if (event.draft.text != state.draft.text) null else state.nameError,
        )
        is MyProfileReducerEvent.NameInvalid -> state.copy(nameError = event.error)
        is MyProfileReducerEvent.SavingChanged -> state.copy(isSaving = event.saving)
        is MyProfileReducerEvent.ProfileChanged -> state.copy(profile = event.change.applyTo(state.profile))
    }

    private fun load() {
        // 실패 안내와 "다시 시도"는 UseCase가 띄운다. 다시 시도하면 읽기부터 다시 한다.
        // 회원이 없으면(404) UseCase가 다시 로그인하라고 알리고, 확인을 누르면 로그아웃해 로그인 화면으로 간다.
        viewModelScope.launch {
            getMyProfile(onRetry = ::load, onSignOut = ::signOut).onSuccess { dispatch(MyProfileReducerEvent.Loaded(it)) }
        }
    }

    private fun signOut() {
        viewModelScope.launch { logout() }
    }

    /** 집사 정보는 읽은 뒤에만 고친다. 읽기 전 값(빈 값)으로 저장하지 않게 하려는 것이다. */
    private fun openProfileField(field: MyProfileField) {
        val state = currentState
        if (!state.isLoaded || field == MyProfileField.CAT_NAME) return
        val draft = draftOf(state.profile)
        open(field, if (field == MyProfileField.NICKNAME) draft.copy(text = state.profile.nickname, initialText = state.profile.nickname) else draft)
    }

    private fun open(field: MyProfileField, draft: MyProfileDraft) {
        if (currentState.editing != null || currentState.isSaving) return
        dispatch(MyProfileReducerEvent.SheetOpened(field, draft))
    }

    private inline fun editDraft(edit: (MyProfileDraft) -> MyProfileDraft) {
        if (currentState.editing == null || currentState.isSaving) return
        dispatch(MyProfileReducerEvent.DraftChanged(edit(currentState.draft)))
    }

    /**
     * 시트의 값을 저장한다. 이름은 시트 안에서 먼저 검사해 오류를 보여 준다.
     * 바뀐 값이 없으면 서버에 보내지 않고 시트만 닫는다.
     */
    private fun save() {
        val state = currentState
        val field = state.editing ?: return
        if (!state.isSaveEnabled) return
        val draft = state.draft
        when (field) {
            MyProfileField.CAT_NAME -> {
                CatNameValidator.validate(draft.text)?.let { error ->
                    dispatch(MyProfileReducerEvent.NameInvalid(error.toInputError()))
                    return
                }
                val name = draft.text.trim()
                if (name == draft.initialText.trim()) dispatch(MyProfileReducerEvent.SheetClosed) else saveCatName(name)
            }
            MyProfileField.NICKNAME -> {
                SignUpValidator.validateNickname(draft.text)?.let { error ->
                    val inputError = if (error == SignUpFieldError.NICKNAME_TOO_LONG) NameInputError.NICKNAME_TOO_LONG else NameInputError.EMPTY
                    dispatch(MyProfileReducerEvent.NameInvalid(inputError))
                    return
                }
                saveProfile(MyProfileChange(nickname = draft.text.trim()))
            }
            MyProfileField.GENDER -> saveProfile(MyProfileChange(gender = draft.gender))
            MyProfileField.BIRTH -> saveProfile(MyProfileChange(birth = draft.birth))
            MyProfileField.HEIGHT -> saveProfile(MyProfileChange(heightCm = draft.heightCm))
            MyProfileField.WEIGHT -> saveProfile(MyProfileChange(weightKg = draft.weightKg))
        }
    }

    /**
     * [change]를 서버에 저장한다. 실패 안내 스낵바가 시트 창에 가려지지 않게 성공·실패 모두 시트를 닫는다.
     * "다시 시도"는 시트 없이 같은 값을 다시 보낸다.
     */
    private fun saveProfile(change: MyProfileChange) {
        val state = currentState
        if (state.isSaving) return
        if (change.applyTo(state.profile) == state.profile) {
            dispatch(MyProfileReducerEvent.SheetClosed)
            return
        }
        dispatch(MyProfileReducerEvent.SavingChanged(saving = true))
        viewModelScope.launch {
            val result = updateMyProfile(change, onRetry = { saveProfile(change) })
            dispatch(MyProfileReducerEvent.SavingChanged(saving = false))
            dispatch(MyProfileReducerEvent.SheetClosed)
            result.onSuccess { dispatch(MyProfileReducerEvent.ProfileChanged(it)) }
        }
    }

    /** 고양이 이름을 바꾼다. 저장되면 UseCase가 기기에 기억한 이름도 바꿔 화면 곳곳의 이름이 함께 바뀐다. */
    private fun saveCatName(name: String) {
        if (currentState.isSaving) return
        dispatch(MyProfileReducerEvent.SavingChanged(saving = true))
        viewModelScope.launch {
            renameCat(name, onRetry = { saveCatName(name) })
            dispatch(MyProfileReducerEvent.SavingChanged(saving = false))
            dispatch(MyProfileReducerEvent.SheetClosed)
        }
    }

    /** 지금 프로필로 시트 값을 채운다. 서버가 비워 둔 값은 회원가입 첫 값을, 고를 수 없는 범위 밖 값은 가장 가까운 값을 가리킨다. */
    private fun draftOf(profile: MyProfileVO): MyProfileDraft {
        val birth = profile.birth ?: MyProfileDraft.DEFAULT_BIRTH
        return MyProfileDraft(
            gender = profile.gender?.takeIf { it == Gender.MALE || it == Gender.FEMALE },
            birthYear = birth.year.coerceIn(UserProfileRange.BIRTH_YEAR_FIRST, DeviceTime.today().year),
            birthMonth = birth.monthValue,
            birthDay = birth.dayOfMonth,
            heightCm = (profile.heightCm ?: MyProfileDraft.DEFAULT_HEIGHT_CM).coerceIn(UserProfileRange.HEIGHT_CM),
            weightKg = (profile.weightKg ?: MyProfileDraft.DEFAULT_WEIGHT_KG).coerceIn(UserProfileRange.WEIGHT_KG),
        ).notAfterToday()
    }

    /** 미래 생년월일은 고르지 못한다. 고른 날짜가 오늘(기기 타임존)보다 뒤가 되면 오늘로 맞춘다. */
    private fun MyProfileDraft.notAfterToday(): MyProfileDraft {
        val today = DeviceTime.today()
        if (!isBirthAfter(birthYear, birthMonth, birthDay, latest = today)) return this
        return copy(birthYear = today.year, birthMonth = today.monthValue, birthDay = today.dayOfMonth)
    }

    private fun CatNameError.toInputError(): NameInputError = when (this) {
        CatNameError.EMPTY -> NameInputError.EMPTY
        CatNameError.TOO_LONG -> NameInputError.CAT_NAME_TOO_LONG
    }
}
