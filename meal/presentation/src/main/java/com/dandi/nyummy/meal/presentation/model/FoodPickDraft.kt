package com.dandi.nyummy.meal.presentation.model

import com.dandi.nyummy.meal.entity.FoundFoodVO
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * 찾은 음식 고르기 화면의 한 줄.
 *
 * @property key 화면 안에서 줄을 가리키는 번호. 같은 이름의 음식이 둘이어도 구분된다.
 * @property mealFoodId 서버의 음식 줄 ID. 사용자가 추가한 줄은 [FoundFoodVO.NEW_ID]다.
 * @property name 음식 이름
 * @property isChecked 가져갈 음식인지 여부
 */
data class FoodPickLine(
    val key: Int,
    val mealFoodId: Long,
    val name: String,
    val isChecked: Boolean,
)

/**
 * 찾은 음식 고르기 화면에서 사용자가 고치는 내용. 기록 직후의 고르기와 기록 탭에서 들어온 고르기가 같이 쓴다.
 *
 * @property lines 음식 줄. 서버가 찾은 순서 뒤에 사용자가 추가한 줄이 붙는다.
 * @property nameInput 추가할 음식 이름 입력. 입력 줄이 닫혀 있으면 null이다.
 */
data class FoodPickDraft(
    val lines: ImmutableList<FoodPickLine> = persistentListOf(),
    val nameInput: String? = null,
) {
    /** 가져갈 음식 수. */
    val checkedCount: Int
        get() = lines.count { it.isChecked }

    /** 하나 이상 골라야 가져갈 수 있다. */
    val canSubmit: Boolean
        get() = checkedCount > 0

    /** 음식을 더 추가할 수 있는지 여부. 서버는 [MAX_LINES]줄까지 받는다. */
    val canAdd: Boolean
        get() = lines.size < MAX_LINES

    /** 입력한 이름으로 음식을 추가할 수 있는지 여부. */
    val canConfirmAdd: Boolean
        get() = canAdd && !nameInput.isNullOrBlank()

    /** 줄의 체크를 켜거나 끈다. */
    fun toggle(key: Int): FoodPickDraft =
        copy(lines = lines.map { if (it.key == key) it.copy(isChecked = !it.isChecked) else it }.toImmutableList())

    /** 이름 입력 줄을 연다. 더 추가할 수 없으면 그대로다. */
    fun openNameInput(): FoodPickDraft = if (canAdd && nameInput == null) copy(nameInput = "") else this

    /** 이름 입력을 바꾼다. [MAX_NAME_LENGTH]자를 넘는 입력은 받지 않는다. */
    fun changeNameInput(text: String): FoodPickDraft =
        if (nameInput == null || text.length > MAX_NAME_LENGTH) this else copy(nameInput = text)

    /** 입력한 이름을 체크된 줄로 목록 끝에 추가하고 입력 줄을 닫는다. 이름이 비었거나 줄이 가득 찼으면 그대로다. */
    fun confirmNameInput(): FoodPickDraft {
        val name = nameInput?.trim().orEmpty()
        if (!canAdd || name.isEmpty()) return this
        val line = FoodPickLine(
            key = (lines.maxOfOrNull { it.key } ?: 0) + 1,
            mealFoodId = FoundFoodVO.NEW_ID,
            name = name,
            isChecked = true,
        )
        return copy(lines = (lines + line).toImmutableList(), nameInput = null)
    }

    /** 서버에 보낼 목록. 체크한 줄만, 화면 순서대로다. */
    fun toFoods(): List<FoundFoodVO> =
        lines.filter { it.isChecked }.map { FoundFoodVO(mealFoodId = it.mealFoodId, name = it.name) }

    companion object {
        /** 식사 하나에 담을 수 있는 음식 줄 수. */
        const val MAX_LINES = 10

        /** 음식 이름 최대 길이. */
        const val MAX_NAME_LENGTH = 30

        /** 서버가 찾은 음식을 모두 체크한 상태로 시작한다. */
        fun of(foods: List<FoundFoodVO>): FoodPickDraft = FoodPickDraft(
            lines = foods.take(MAX_LINES).mapIndexed { index, food ->
                FoodPickLine(key = index + 1, mealFoodId = food.mealFoodId, name = food.name, isChecked = true)
            }.toImmutableList(),
        )
    }
}
