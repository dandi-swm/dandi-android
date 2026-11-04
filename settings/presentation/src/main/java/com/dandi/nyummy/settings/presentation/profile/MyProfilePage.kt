package com.dandi.nyummy.settings.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dandi.nyummy.auth.entity.Gender
import com.dandi.nyummy.auth.entity.UserProfileRange
import com.dandi.nyummy.common.entity.cat.CatNameValidator
import com.dandi.nyummy.common.entity.time.DeviceTime
import com.dandi.nyummy.common.presentation.cat.catName
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyBirthDatePicker
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyBottomSheet
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyListRow
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyListRowTrailing
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySectionCaption
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySegmentedControl
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySheetTitle
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextField
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTopBar
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyWheelPicker
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.settings.entity.MyProfileVO
import com.dandi.nyummy.settings.presentation.R
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import java.time.LocalDate

/**
 * 마이페이지. 고양이 이름과 집사 정보(이름, 성별, 생년월일, 키, 몸무게)를 줄로 보여 주고, 줄을 누르면 그 항목의 시트에서 고친다.
 * 성별은 회원가입과 같은 두 칸 선택, 생년월일·키·몸무게는 회원가입과 같은 휠 피커로 고른다.
 * 고양이 이름은 앱 루트가 기기에 기억한 이름을 그리므로, 바꾸면 이 화면과 다른 화면의 이름이 함께 바뀐다.
 */
@Composable
fun MyProfilePage(
    modifier: Modifier = Modifier,
    viewModel: MyProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.onIntent(MyProfileIntent.Enter)
    }

    MyProfileScreen(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

@Composable
internal fun MyProfileScreen(
    uiState: MyProfileUIState,
    onIntent: (MyProfileIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val catName = catName()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NyummyTheme.colors.bg.canvas),
    ) {
        NyummyTopBar(
            title = stringResource(R.string.my_profile_title),
            onBackClick = { onIntent(MyProfileIntent.ClickBack) },
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = ContentMaxWidth)
                .padding(horizontal = NyummyTheme.spacing.gutter)
                .padding(bottom = NyummyTheme.spacing.s24),
        ) {
            NyummySectionCaption(title = stringResource(R.string.my_profile_section_cat))
            NyummyListRow(
                title = stringResource(R.string.my_profile_cat_name),
                trailing = NyummyListRowTrailing.Value(catName),
                onClick = { onIntent(MyProfileIntent.ClickCatName(catName)) },
            )
            NyummySectionCaption(title = stringResource(R.string.my_profile_section_owner))
            OwnerRows(uiState = uiState, onIntent = onIntent)
            NyummyText(
                text = stringResource(R.string.my_profile_calorie_caption),
                style = NyummyTheme.typography.bodyS,
                color = NyummyTheme.colors.content.tertiary,
                modifier = Modifier.padding(top = NyummyTheme.spacing.s8),
            )
        }
    }

    uiState.editing?.let { field ->
        MyProfileSheet(field = field, uiState = uiState, onIntent = onIntent)
    }
}

/** 집사 정보 줄. 프로필을 읽기 전에는 값을 비우고 누를 수 없다. */
@Composable
private fun OwnerRows(uiState: MyProfileUIState, onIntent: (MyProfileIntent) -> Unit) {
    val profile = uiState.profile
    val unset = stringResource(R.string.my_profile_unset)
    fun shown(value: String?): String = when {
        !uiState.isLoaded -> ""
        value.isNullOrBlank() -> unset
        else -> value
    }
    val rows = listOf(
        MyProfileField.NICKNAME to (R.string.my_profile_nickname to shown(profile.nickname)),
        MyProfileField.GENDER to (R.string.my_profile_gender to shown(profile.gender?.let { genderText(it) })),
        MyProfileField.BIRTH to (R.string.my_profile_birth to shown(birthText(profile))),
        MyProfileField.HEIGHT to
            (R.string.my_profile_height to shown(profile.heightCm?.let { stringResource(R.string.my_profile_height_value, it) })),
        MyProfileField.WEIGHT to
            (R.string.my_profile_weight to shown(profile.weightKg?.let { stringResource(R.string.my_profile_weight_value, it) })),
    )
    rows.forEach { (field, row) ->
        val (title, value) = row
        NyummyListRow(
            title = stringResource(title),
            trailing = NyummyListRowTrailing.Value(value),
            onClick = { onIntent(MyProfileIntent.ClickField(field)) },
            enabled = uiState.isLoaded,
        )
    }
}

@Composable
private fun genderText(gender: Gender): String = stringResource(
    when (gender) {
        Gender.MALE -> R.string.my_profile_gender_male
        Gender.FEMALE -> R.string.my_profile_gender_female
        Gender.OTHER -> R.string.my_profile_gender_other
    },
)

/** "2001.03.14 (25세)". 나이는 기기 타임존의 오늘 기준 만 나이다. */
@Composable
private fun birthText(profile: MyProfileVO): String? {
    val birth = profile.birth ?: return null
    val age = profile.ageOn(DeviceTime.today()) ?: return null
    return stringResource(R.string.my_profile_birth_value, birth.year, birth.monthValue, birth.dayOfMonth, age)
}

/** 항목 하나를 고치는 시트. 제목, 고르는 칸, 안내, 저장 버튼 순서다. */
@Composable
private fun MyProfileSheet(
    field: MyProfileField,
    uiState: MyProfileUIState,
    onIntent: (MyProfileIntent) -> Unit,
) {
    val draft = uiState.draft
    NyummyBottomSheet(onDismissRequest = { onIntent(MyProfileIntent.DismissSheet) }) {
        NyummySheetTitle(text = stringResource(field.sheetTitle))
        when (field) {
            MyProfileField.CAT_NAME, MyProfileField.NICKNAME -> NameField(field = field, uiState = uiState, onIntent = onIntent)
            MyProfileField.GENDER -> {
                NyummySegmentedControl(
                    options = persistentListOf(
                        stringResource(R.string.my_profile_gender_male),
                        stringResource(R.string.my_profile_gender_female),
                    ),
                    selectedIndex = when (draft.gender) {
                        Gender.MALE -> 0
                        Gender.FEMALE -> 1
                        else -> NO_SELECTION
                    },
                    onSelect = { index -> onIntent(MyProfileIntent.SelectGender(if (index == 0) Gender.MALE else Gender.FEMALE)) },
                    enabled = !uiState.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )
                SheetHelper(stringResource(R.string.my_profile_sheet_gender_note))
            }
            MyProfileField.BIRTH -> {
                // 시트를 열 때 기기 타임존 기준 올해까지로 정한다.
                val years = remember { UserProfileRange.BIRTH_YEAR_FIRST..DeviceTime.today().year }
                NyummyBirthDatePicker(
                    year = draft.birthYear,
                    month = draft.birthMonth,
                    day = draft.birthDay,
                    years = years,
                    onYearChange = { onIntent(MyProfileIntent.SelectBirthYear(it)) },
                    onMonthChange = { onIntent(MyProfileIntent.SelectBirthMonth(it)) },
                    onDayChange = { onIntent(MyProfileIntent.SelectBirthDay(it)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                SheetHelper(stringResource(R.string.my_profile_sheet_birth_helper))
            }
            MyProfileField.HEIGHT -> {
                RangeWheel(
                    range = UserProfileRange.HEIGHT_CM,
                    value = draft.heightCm,
                    unit = stringResource(R.string.my_profile_unit_height),
                    description = stringResource(R.string.my_profile_height),
                    onChange = { onIntent(MyProfileIntent.SelectHeight(it)) },
                )
                SheetHelper(stringResource(R.string.my_profile_sheet_recalculate))
            }
            MyProfileField.WEIGHT -> {
                RangeWheel(
                    range = UserProfileRange.WEIGHT_KG,
                    value = draft.weightKg,
                    unit = stringResource(R.string.my_profile_unit_weight),
                    description = stringResource(R.string.my_profile_weight),
                    onChange = { onIntent(MyProfileIntent.SelectWeight(it)) },
                )
                SheetHelper(stringResource(R.string.my_profile_sheet_recalculate))
            }
        }
        NyummyButton(
            text = stringResource(R.string.my_profile_sheet_save),
            onClick = { onIntent(MyProfileIntent.ClickSave) },
            enabled = uiState.isSaveEnabled,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** 고양이 이름·집사 이름 입력. 고양이 이름만 글자 수 안내를 둔다. */
@Composable
private fun NameField(field: MyProfileField, uiState: MyProfileUIState, onIntent: (MyProfileIntent) -> Unit) {
    val isCatName = field == MyProfileField.CAT_NAME
    NyummyTextField(
        value = uiState.draft.text,
        onValueChange = { onIntent(MyProfileIntent.InputName(it)) },
        label = stringResource(R.string.my_profile_sheet_name_label),
        helperText = if (isCatName) stringResource(R.string.my_profile_sheet_cat_name_helper, CatNameValidator.MAX_LENGTH) else null,
        errorMessage = uiState.nameError?.let { error ->
            when (error) {
                NameInputError.EMPTY -> stringResource(R.string.my_profile_error_name_empty)
                NameInputError.CAT_NAME_TOO_LONG ->
                    stringResource(R.string.my_profile_error_cat_name_too_long, CatNameValidator.MAX_LENGTH)
            }
        },
        enabled = !uiState.isSaving,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** 키·몸무게처럼 범위 안의 정수 하나를 고르는 휠. */
@Composable
private fun RangeWheel(range: IntRange, value: Int, unit: String, description: String, onChange: (Int) -> Unit) {
    val items = remember(range) { range.map(Int::toString).toImmutableList() }
    NyummyWheelPicker(
        items = items,
        selectedIndex = value.coerceIn(range) - range.first,
        onSelectedIndexChange = { onChange(range.first + it) },
        unit = unit,
        contentDescription = description,
        compact = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SheetHelper(text: String) {
    NyummyText(text = text, style = NyummyTheme.typography.bodyS, color = NyummyTheme.colors.content.tertiary)
}

private val MyProfileField.sheetTitle: Int
    get() = when (this) {
        MyProfileField.CAT_NAME -> R.string.my_profile_sheet_cat_name
        MyProfileField.NICKNAME -> R.string.my_profile_nickname
        MyProfileField.GENDER -> R.string.my_profile_gender
        MyProfileField.BIRTH -> R.string.my_profile_birth
        MyProfileField.HEIGHT -> R.string.my_profile_height
        MyProfileField.WEIGHT -> R.string.my_profile_weight
    }

private const val NO_SELECTION = -1
private val ContentMaxWidth = 480.dp

private val previewProfile = MyProfileVO(
    nickname = "진우",
    gender = Gender.FEMALE,
    birth = LocalDate.of(2001, 3, 14),
    heightCm = 163,
    weightKg = 52,
)

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MyProfileScreenPreview() {
    NyummyTheme {
        MyProfileScreen(uiState = MyProfileUIState(isLoaded = true, profile = previewProfile), onIntent = {})
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MyProfileScreenUnsetPreview() {
    NyummyTheme {
        MyProfileScreen(uiState = MyProfileUIState(isLoaded = true, profile = MyProfileVO(nickname = "진우")), onIntent = {})
    }
}
