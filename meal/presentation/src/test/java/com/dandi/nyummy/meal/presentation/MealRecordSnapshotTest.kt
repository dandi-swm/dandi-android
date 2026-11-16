package com.dandi.nyummy.meal.presentation

import androidx.lifecycle.SavedStateHandle
import com.dandi.nyummy.meal.entity.MealPhotoSource
import com.dandi.nyummy.meal.entity.RewardBoxGrade
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 식사 기록 스냅샷이 단계별 값만 저장하고 그대로 되살아나는지 확인합니다. */
class MealRecordSnapshotTest {

    private val captured = MealRecordSnapshot.Captured(PHOTO, MealPhotoSource.CAMERA)
    private val submitting = MealRecordSnapshot.Submitting(PHOTO, MealPhotoSource.CAMERA)
    private val recorded = MealRecordSnapshot.Recorded(PHOTO, MealPhotoSource.GALLERY)
    private val accepted = MealRecordSnapshot.Accepted(PHOTO, MealPhotoSource.CAMERA, RewardBoxGrade.GOLD)

    @Test
    fun `저장한 적이 없으면 읽을 것이 없다`() {
        assertNull(MealRecordSnapshot.readFrom(SavedStateHandle()))
    }

    @Test
    fun `어느 단계든 저장한 그대로 읽는다`() {
        listOf(MealRecordSnapshot.Preview, captured, submitting, recorded, accepted).forEach { snapshot ->
            assertEquals(snapshot, MealRecordSnapshot.readFrom(savedOf(snapshot)))
        }
    }

    @Test
    fun `단계가 바뀌면 앞 단계의 값은 남지 않는다`() {
        val handle = savedOf(accepted)

        MealRecordSnapshot.Preview.writeTo(handle)

        assertEquals(MealRecordSnapshot.Preview, MealRecordSnapshot.readFrom(handle))
        assertNull(handle.get<String>(KEY_PHOTO_PATH))
        assertNull(handle.get<String>(KEY_REWARD_BOX_GRADE))
    }

    @Test
    fun `기록만 받은 단계만 연출 없이 기록된 것으로 본다`() {
        assertEquals(
            listOf(false, false, false, true, false),
            listOf(MealRecordSnapshot.Preview, captured, submitting, recorded, accepted)
                .map { it.isRecordedWithoutCeremony },
        )
    }

    @Test
    fun `업로드 중이었거나 기록만 받았으면 접수 전 연출 단계로 되살린다`() {
        listOf(submitting, recorded).forEach { snapshot ->
            val phase = snapshot.toPhase() as MealCameraPhase.Ceremony

            assertEquals(MealSubmitStatus.Uploading, phase.submit)
            assertEquals(MealCeremonyStage.Waiting, phase.stage)
        }
    }

    @Test
    fun `접수됐으면 그 등급으로 연출 끝 장면을 되살린다`() {
        val phase = accepted.toPhase() as MealCameraPhase.Ceremony

        assertEquals(MealSubmitStatus.Accepted(RewardBoxGrade.GOLD), phase.submit)
        assertEquals(MealCeremonyStage.Finished, phase.stage)
    }

    @Test
    fun `접수됐다는데 등급을 읽지 못하면 결과를 모르는 것으로 본다`() {
        val handle = savedOf(accepted)
        handle[KEY_REWARD_BOX_GRADE] = "UNKNOWN"

        assertEquals(submitting, MealRecordSnapshot.readFrom(handle))
    }

    @Test
    fun `사진을 읽지 못하면 다시 찍게 한다`() {
        val handle = savedOf(recorded)
        handle[KEY_PHOTO_PATH] = null

        assertEquals(MealRecordSnapshot.Preview, MealRecordSnapshot.readFrom(handle))
    }

    @Test
    fun `모르는 단계 이름이 저장돼 있으면 다시 찍게 한다`() {
        val handle = savedOf(captured)
        handle[KEY_STEP] = "UNKNOWN"

        assertEquals(MealRecordSnapshot.Preview, MealRecordSnapshot.readFrom(handle))
    }

    private fun savedOf(snapshot: MealRecordSnapshot) = SavedStateHandle().also(snapshot::writeTo)

    private companion object {
        const val PHOTO = TUTORIAL_PHOTO_PATH
        const val KEY_STEP = "meal_record_step"
        const val KEY_PHOTO_PATH = "meal_record_photo_path"
        const val KEY_REWARD_BOX_GRADE = "meal_record_reward_box_grade"
    }
}
