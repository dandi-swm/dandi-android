package com.dandi.nyummy.history.presentation.detail.v2

import com.dandi.nyummy.common.presentation.image.NyummyFoodIconCategory
import com.dandi.nyummy.common.presentation.mvi.UiState
import com.dandi.nyummy.history.entity.MealFoodChangeVO
import com.dandi.nyummy.history.entity.MealFoodNutritionVO
import com.dandi.nyummy.history.entity.MealFoodVO
import com.dandi.nyummy.history.entity.MealHistoryVO
import com.dandi.nyummy.history.presentation.detail.AnalysisFeedbackUiState
import com.dandi.nyummy.history.presentation.detail.MealDetailMode
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * 음식 목록 방식(V2) 식사 상세 화면 상태.
 *
 * @property meal 받은 식사. null이면 아직 받지 못했습니다([isLoading] 또는 [isLoadFailed]).
 * @property foods 식사에 든 음식 줄. 맨 위가 대표 음식입니다.
 * @property mode 식사 이름 수정·식사 삭제 다이얼로그 상태
 * @property nameDraft 식사 이름 수정 다이얼로그의 입력값
 * @property isActionInFlight 식사 이름 수정이나 식사 삭제를 서버에 보내는 중
 * @property feedback AI 분석 평가 카드. null이면 카드를 보여 주지 않습니다.
 * @property isCalculating 서버가 영양을 다시 구하는 중. 끝날 때까지 음식 목록을 고칠 수 없습니다.
 * @property isSavingFoods 음식 목록 변경(순서, 추가, 한 줄 수정·삭제)을 서버에 보내는 중
 * @property foodSheet 음식 고치기 시트. null이면 닫혀 있습니다.
 * @property addNameInput 추가할 음식 이름 입력. 입력 줄이 닫혀 있으면 null입니다.
 */
data class MealDetailV2UIState(
    val meal: MealHistoryVO? = null,
    val foods: ImmutableList<MealFoodVO> = persistentListOf(),
    val isLoading: Boolean = true,
    val isLoadFailed: Boolean = false,
    val mode: MealDetailMode = MealDetailMode.Viewing,
    val nameDraft: String = "",
    val isActionInFlight: Boolean = false,
    val feedback: AnalysisFeedbackUiState? = null,
    val isCalculating: Boolean = false,
    val isSavingFoods: Boolean = false,
    val foodSheet: MealFoodSheetUiState? = null,
    val addNameInput: String? = null,
) : UiState {

    /** 음식 목록을 고칠 수 있는지. 계산 중이거나 다른 변경을 보내는 중이면 고칠 수 없습니다. */
    val canEditFoods: Boolean
        get() = meal != null && !isCalculating && !isSavingFoods

    /** 음식을 더 추가할 수 있는지. */
    val canAddFood: Boolean
        get() = foods.size < MealFoodVO.MAX_FOODS

    /** 순서를 바꿀 음식이 둘 이상인지. */
    val canReorder: Boolean
        get() = foods.size > 1

    /** 입력한 이름으로 음식을 추가할 수 있는지. */
    val canConfirmAdd: Boolean
        get() = canEditFoods && canAddFood && !addNameInput.isNullOrBlank()

    fun withFeedback(transform: (AnalysisFeedbackUiState) -> AnalysisFeedbackUiState): MealDetailV2UIState =
        feedback?.let { copy(feedback = transform(it)) } ?: this

    fun withFoodSheet(transform: (MealFoodSheetUiState) -> MealFoodSheetUiState): MealDetailV2UIState =
        foodSheet?.let { copy(foodSheet = transform(it)) } ?: this
}

/** 음식 고치기 시트에서 직접 적는 영양 칸. */
enum class MealFoodNutrient { CALORIE, CARBOHYDRATE, PROTEIN, FAT }

/**
 * 음식 고치기 시트 상태. 저장하면 처음 값([original])과 달라진 항목만 서버에 보냅니다.
 *
 * @property original 시트를 열 때의 음식 줄
 * @property canDelete 이 음식을 지울 수 있는지. 식사에 음식이 하나만 남았으면 지울 수 없습니다.
 * @property iconCategory 아이콘 영역에서 고른 분류. null이면 "전체"입니다.
 * @property iconId 고른 아이콘 번호
 * @property name 이름 입력
 * @property portion 인분
 * @property isManualNutrition "직접 설정하기"를 켰는지. 열 때마다 꺼진 채로 시작하고, 켜야 영양 칸을 고칠 수 있습니다.
 * @property calorie 열량 입력(숫자 글자)
 * @property carbohydrate 탄수화물 입력
 * @property protein 단백질 입력
 * @property fat 지방 입력
 */
data class MealFoodSheetUiState(
    val original: MealFoodVO,
    val canDelete: Boolean,
    val iconCategory: NyummyFoodIconCategory? = null,
    val iconId: String = original.foodIconId,
    val name: String = original.name,
    val portion: Double = original.portion,
    val isManualNutrition: Boolean = false,
    val calorie: String = original.calorieKcal.toString(),
    val carbohydrate: String = original.carbohydrateGram.toString(),
    val protein: String = original.proteinGram.toString(),
    val fat: String = original.fatGram.toString(),
) {
    val canDecreasePortion: Boolean
        get() = portion > MealFoodVO.MIN_PORTION

    val canIncreasePortion: Boolean
        get() = portion < MealFoodVO.MAX_PORTION

    /** 직접 적은 영양 값. 비었거나 숫자가 아닌 칸이 있으면 null입니다. */
    private val manualNutrition: MealFoodNutritionVO?
        get() {
            val kcal = calorie.toIntOrNull() ?: return null
            val carbs = carbohydrate.toIntOrNull() ?: return null
            val proteinGram = protein.toIntOrNull() ?: return null
            val fatGram = fat.toIntOrNull() ?: return null
            return MealFoodNutritionVO(kcal, carbs, proteinGram, fatGram)
        }

    /** 저장할 수 있는 입력인지. 이름이 있어야 하고, 영양을 직접 적는 중이면 네 칸이 모두 숫자여야 합니다. */
    val canSave: Boolean
        get() = name.isNotBlank() && (!isManualNutrition || manualNutrition != null)

    /**
     * 처음 값과 달라진 항목만 담은 변경. 영양은 "직접 설정하기"를 켜고 값을 바꿨을 때만 담습니다
     * (담아 보내면 서버가 그 줄의 영양을 고정하므로, 켜지 않았으면 예전 값도 보내지 않습니다).
     */
    fun toChange(): MealFoodChangeVO {
        val newName = name.trim()
        val nutrition = manualNutrition?.takeIf { isManualNutrition && it != original.toNutrition() }
        return MealFoodChangeVO(
            name = newName.takeIf { it.isNotEmpty() && it != original.name },
            portion = portion.takeIf { it != original.portion },
            nutrition = nutrition,
            iconId = iconId.takeIf { it != original.foodIconId }?.toLongOrNull(),
        )
    }

    /** "직접 설정하기"를 켜거나 끈다. 끄면 영양 칸을 시트를 열 때의 값으로 되돌린다. */
    fun withManualNutrition(enabled: Boolean): MealFoodSheetUiState =
        if (enabled) {
            copy(isManualNutrition = true)
        } else {
            copy(
                isManualNutrition = false,
                calorie = original.calorieKcal.toString(),
                carbohydrate = original.carbohydrateGram.toString(),
                protein = original.proteinGram.toString(),
                fat = original.fatGram.toString(),
            )
        }

    /** 영양 칸 하나를 고친다. "직접 설정하기"가 꺼져 있거나, 숫자가 아니거나, 범위를 넘으면 받지 않는다. */
    fun withNutrient(nutrient: MealFoodNutrient, text: String): MealFoodSheetUiState {
        if (!isManualNutrition) return this
        val max = if (nutrient == MealFoodNutrient.CALORIE) MealFoodVO.MAX_CALORIE else MealFoodVO.MAX_NUTRIENT
        if (text.isNotEmpty() && (text.any { !it.isDigit() } || (text.toIntOrNull() ?: Int.MAX_VALUE) > max)) return this
        // "007"처럼 앞에 0이 붙지 않게 한다.
        val cleaned = text.toIntOrNull()?.toString() ?: ""
        return when (nutrient) {
            MealFoodNutrient.CALORIE -> copy(calorie = cleaned)
            MealFoodNutrient.CARBOHYDRATE -> copy(carbohydrate = cleaned)
            MealFoodNutrient.PROTEIN -> copy(protein = cleaned)
            MealFoodNutrient.FAT -> copy(fat = cleaned)
        }
    }

    /** 인분을 한 칸(0.5) 올리거나 내린다. 0.5~5.0을 벗어나지 않는다. */
    fun withPortionStep(increase: Boolean): MealFoodSheetUiState {
        val next = portion + if (increase) MealFoodVO.PORTION_STEP else -MealFoodVO.PORTION_STEP
        return copy(portion = next.coerceIn(MealFoodVO.MIN_PORTION, MealFoodVO.MAX_PORTION))
    }

    /** 이름을 고친다. 30자를 넘는 입력은 받지 않는다. */
    fun withName(text: String): MealFoodSheetUiState =
        if (text.length > MealFoodVO.MAX_NAME_LENGTH) this else copy(name = text)

    companion object {
        /** 시트는 "전체" 분류, "직접 설정하기" 꺼짐으로 연다. */
        fun of(food: MealFoodVO, canDelete: Boolean) = MealFoodSheetUiState(original = food, canDelete = canDelete)
    }
}

private fun MealFoodVO.toNutrition() = MealFoodNutritionVO(calorieKcal, carbohydrateGram, proteinGram, fatGram)
