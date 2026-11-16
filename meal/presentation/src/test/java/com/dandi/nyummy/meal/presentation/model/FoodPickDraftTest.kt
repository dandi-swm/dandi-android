package com.dandi.nyummy.meal.presentation.model

import com.dandi.nyummy.meal.entity.FoundFoodVO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodPickDraftTest {

    private val foods = listOf("밥", "된장찌개", "불고기", "시금치나물", "배추김치").mapIndexed { index, name ->
        FoundFoodVO(mealFoodId = index + 11L, name = name)
    }

    @Test
    fun `처음에는 찾은 음식이 모두 체크돼 있다`() {
        val draft = FoodPickDraft.of(foods)

        assertEquals(5, draft.lines.size)
        assertTrue(draft.lines.all { it.isChecked })
        assertEquals(5, draft.checkedCount)
        assertEquals(foods, draft.toFoods())
    }

    @Test
    fun `체크를 끄면 가져갈 개수가 줄고 보낼 목록에서 빠진다`() {
        val draft = FoodPickDraft.of(foods).toggle(5)

        assertEquals(4, draft.checkedCount)
        assertEquals(foods.take(4), draft.toFoods())
        // 다시 누르면 돌아온다.
        assertEquals(5, draft.toggle(5).checkedCount)
    }

    @Test
    fun `모두 끄면 가져갈 수 없다`() {
        val draft = (1..5).fold(FoodPickDraft.of(foods)) { acc, key -> acc.toggle(key) }

        assertFalse(draft.canSubmit)
        assertTrue(draft.toFoods().isEmpty())
    }

    @Test
    fun `이름을 적어 추가한 음식은 체크된 새 줄로 목록 끝에 들어간다`() {
        val draft = FoodPickDraft.of(foods).openNameInput().changeNameInput("  김밥 ").confirmNameInput()

        val added = draft.lines.last()
        assertEquals("김밥", added.name)
        assertTrue(added.isChecked)
        assertEquals(FoundFoodVO.NEW_ID, added.mealFoodId)
        assertNull(draft.nameInput)
        assertEquals(6, draft.checkedCount)
        assertTrue(draft.toFoods().last().isNew)
        // 줄 번호가 겹치지 않아 새 줄만 따로 끌 수 있다.
        assertEquals(5, draft.toggle(added.key).checkedCount)
    }

    @Test
    fun `이름이 비었거나 공백뿐이면 추가하지 않는다`() {
        val opened = FoodPickDraft.of(foods).openNameInput()

        assertFalse(opened.canConfirmAdd)
        assertFalse(opened.changeNameInput("   ").canConfirmAdd)
        assertSame(opened, opened.confirmNameInput())
    }

    @Test
    fun `이름은 서른 자까지만 받는다`() {
        val opened = FoodPickDraft.of(foods).openNameInput()
        val thirty = "가".repeat(30)

        assertEquals(thirty, opened.changeNameInput(thirty).nameInput)
        assertEquals("", opened.changeNameInput(thirty + "나").nameInput)
    }

    @Test
    fun `열 줄이면 더 추가할 수 없다`() {
        val full = FoodPickDraft.of(foods + foods.map { it.copy(mealFoodId = it.mealFoodId + 100) })

        assertEquals(10, full.lines.size)
        assertFalse(full.canAdd)
        assertSame(full, full.openNameInput())
    }

    @Test
    fun `아홉 줄에서 하나를 추가하면 가득 찬다`() {
        val nine = FoodPickDraft.of(foods + foods.take(4).map { it.copy(mealFoodId = it.mealFoodId + 100) })

        val full = nine.openNameInput().changeNameInput("김밥").confirmNameInput()

        assertEquals(10, full.lines.size)
        assertFalse(full.canAdd)
    }

    @Test
    fun `서버가 열 줄 넘게 줘도 열 줄만 쓴다`() {
        val many = (1..12).map { FoundFoodVO(mealFoodId = it.toLong(), name = "음식$it") }

        assertEquals(10, FoodPickDraft.of(many).lines.size)
    }

    @Test
    fun `입력 줄이 닫혀 있으면 입력을 받지 않는다`() {
        val draft = FoodPickDraft.of(foods)

        assertSame(draft, draft.changeNameInput("김밥"))
    }
}
