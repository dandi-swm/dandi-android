package com.dandi.nyummy.settings.domain

import com.dandi.nyummy.auth.entity.Gender
import com.dandi.nyummy.settings.entity.MyProfileChange
import com.dandi.nyummy.settings.entity.MyProfileVO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MyProfileVOTest {

    private val profile = MyProfileVO(
        nickname = "진우",
        gender = Gender.FEMALE,
        birth = LocalDate.of(2001, 3, 14),
        heightCm = 163,
        weightKg = 52,
    )

    @Test
    fun `바꾸는 항목만 적용하고 나머지는 그대로 둔다`() {
        val changed = MyProfileChange(weightKg = 50).applyTo(profile)

        assertEquals(profile.copy(weightKg = 50), changed)
    }

    @Test
    fun `바꾸는 항목이 없으면 비어 있다`() {
        assertTrue(MyProfileChange().isEmpty)
        assertFalse(MyProfileChange(nickname = "").isEmpty)
    }

    @Test
    fun `만 나이는 생일이 지나야 한 살 많아진다`() {
        assertEquals(24, profile.ageOn(LocalDate.of(2026, 3, 13)))
        assertEquals(25, profile.ageOn(LocalDate.of(2026, 3, 14)))
    }

    @Test
    fun `생년월일을 모르면 나이도 모른다`() {
        assertNull(profile.copy(birth = null).ageOn(LocalDate.of(2026, 11, 3)))
    }

    @Test
    fun `생년월일이 오늘보다 뒤면 0살로 본다`() {
        assertEquals(0, profile.copy(birth = LocalDate.of(2027, 1, 1)).ageOn(LocalDate.of(2026, 11, 3)))
    }
}
