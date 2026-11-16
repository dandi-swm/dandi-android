package com.dandi.nyummy.meal.presentation

import androidx.lifecycle.SavedStateHandle
import com.dandi.nyummy.meal.entity.MealPhotoSource
import com.dandi.nyummy.meal.entity.RewardBoxGrade
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 식사 기록 스냅샷이 단계 하나로 저장되고 되살아나는지 확인합니다. */
class MealRecordSnapshotTest {

    @Test
    fun `저장한 적이 없으면 읽을 것이 없다`() {
        assertNull(MealRecordSnapshot.readFrom(SavedStateHandle()))
    }

    @Test
    fun `저장한 단계와 사진과 등급을 그대로 읽는다`() {
        val snapshot = MealRecordSnapshot(
            step = MealRecordStep.ACCEPTED,
            photoPath = TUTORIAL_PHOTO_PATH,
            photoSource = MealPhotoSource.CAMERA,
            acceptedRewardBoxGrade = RewardBoxGrade.NONE,
        )
        val handle = SavedStateHandle().also(snapshot::writeTo)

        assertEquals(snapshot, MealRecordSnapshot.readFrom(handle))
    }

    @Test
    fun `기록만 받은 단계만 연출 없이 기록된 것으로 본다`() {
        MealRecordStep.entries.forEach { step ->
            assertEquals(step == MealRecordStep.RECORDED, MealRecordSnapshot(step).isRecordedWithoutCeremony)
        }
    }

    @Test
    fun `업로드 중이었거나 기록만 받았으면 접수 전 연출 단계로 되살린다`() {
        listOf(MealRecordStep.SUBMITTING, MealRecordStep.RECORDED).forEach { step ->
            val phase = snapshotOf(step).toPhase() as MealCameraPhase.Ceremony

            assertEquals(MealSubmitStatus.Uploading, phase.submit)
            assertEquals(MealCeremonyStage.Waiting, phase.stage)
        }
    }

    @Test
    fun `접수됐으면 그 등급으로 연출 끝 장면을 되살린다`() {
        val phase = snapshotOf(MealRecordStep.ACCEPTED, RewardBoxGrade.NONE).toPhase() as MealCameraPhase.Ceremony

        assertEquals(MealSubmitStatus.Accepted(RewardBoxGrade.NONE), phase.submit)
        assertEquals(MealCeremonyStage.Finished, phase.stage)
    }

    @Test
    fun `접수됐다는데 등급을 읽지 못하면 결과를 모르는 것으로 본다`() {
        val phase = snapshotOf(MealRecordStep.ACCEPTED, grade = null).toPhase() as MealCameraPhase.Ceremony

        assertEquals(MealSubmitStatus.Uploading, phase.submit)
    }

    @Test
    fun `모르는 단계 이름이 저장돼 있으면 처음부터 다시 찍게 한다`() {
        val handle = SavedStateHandle().also(snapshotOf(MealRecordStep.CAPTURED)::writeTo)
        handle["meal_record_step"] = "UNKNOWN"

        val snapshot = MealRecordSnapshot.readFrom(handle)

        assertEquals(MealRecordStep.PREVIEW, snapshot?.step)
        assertEquals(MealCameraPhase.Preview, snapshot?.toPhase())
        assertFalse(snapshot?.isRecordedWithoutCeremony ?: true)
        assertTrue(snapshot != null)
    }

    private fun snapshotOf(step: MealRecordStep, grade: RewardBoxGrade? = null) = MealRecordSnapshot(
        step = step,
        photoPath = TUTORIAL_PHOTO_PATH,
        photoSource = MealPhotoSource.CAMERA,
        acceptedRewardBoxGrade = grade,
    )
}
