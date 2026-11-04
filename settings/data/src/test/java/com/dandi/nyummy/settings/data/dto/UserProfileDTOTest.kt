package com.dandi.nyummy.settings.data.dto

import com.dandi.nyummy.auth.entity.Gender
import com.dandi.nyummy.common.entity.meal.MealTimeVO
import com.dandi.nyummy.settings.entity.MyProfileVO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class UserProfileDTOTest {

    @Test
    fun `집사 정보와 식사 시각을 그대로 옮긴다`() {
        val profile = UserProfileDTO(
            nickname = " 진우 ",
            gender = "FEMALE",
            birth = "2001-03-14",
            height = 163,
            weight = 52,
            lunchHour = 13,
        ).toMyProfileVO()

        assertEquals("진우", profile.nickname)
        assertEquals(Gender.FEMALE, profile.gender)
        assertEquals(LocalDate.of(2001, 3, 14), profile.birth)
        assertEquals(163, profile.heightCm)
        assertEquals(52, profile.weightKg)
        assertEquals(MealTimeVO(hour = 13), profile.mealTimes.lunch)
    }

    @Test
    fun `서버가 비워 둔 값은 비어 있는 것으로 둔다`() {
        val profile = UserProfileDTO().toMyProfileVO()

        assertEquals(MyProfileVO(mealTimes = profile.mealTimes), profile)
    }

    @Test
    fun `모르는 성별과 날짜 형식이 아닌 생년월일은 비어 있는 것으로 본다`() {
        val profile = UserProfileDTO(gender = "UNKNOWN", birth = "2001/03/14").toMyProfileVO()

        assertNull(profile.gender)
        assertNull(profile.birth)
    }
}
