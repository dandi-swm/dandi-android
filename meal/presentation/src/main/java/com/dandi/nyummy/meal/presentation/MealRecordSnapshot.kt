package com.dandi.nyummy.meal.presentation

import androidx.lifecycle.SavedStateHandle
import com.dandi.nyummy.meal.entity.MealPhotoSource
import com.dandi.nyummy.meal.entity.RewardBoxGrade
import java.io.File

/**
 * 앱이 백그라운드에서 종료됐다가 돌아올 때 식사 기록 화면을 되살리는 데 필요한 사실만 모은 것입니다.
 * [SavedStateHandle] 에 두어 앱이 종료돼도 남습니다. 어디까지 갔는지가 타입이고, 그 단계에 있는 값만 가집니다.
 *
 * 화면 상태를 통째로 저장하지 않습니다. 촬영·사진 가져오기처럼 하던 중인 작업은 앱과 함께 끝났고, 카메라 권한은
 * 그사이 바뀌었을 수 있어(권한을 끄면 OS가 앱을 종료한다) 돌아온 뒤 다시 확인해야 하기 때문입니다.
 * OS는 앱이 백그라운드로 갈 때의 값을 저장하므로, 백그라운드로 간 뒤에 접수된 결과는 남지 않고 접수 전으로 되살아납니다.
 */
internal sealed interface MealRecordSnapshot {

    /** 아직 사진이 없다. */
    data object Preview : MealRecordSnapshot

    /** 사진을 찍었거나 가져와 확인하는 중이다. */
    data class Captured(val photoPath: String, val photoSource: MealPhotoSource) : MealRecordSnapshot

    /** 냐미에게 주기를 눌러 업로드를 시작했고, 서버가 받았는지는 아직 모른다. */
    data class Submitting(val photoPath: String, val photoSource: MealPhotoSource) : MealRecordSnapshot

    /** 서버가 기록을 받았지만 연출까지 가지 못했다. 찾은 음식을 고르던 중이거나 음식 아님 화면·인식 지연 안내를 보던 중이다. */
    data class Recorded(val photoPath: String, val photoSource: MealPhotoSource) : MealRecordSnapshot

    /** 접수되어 연출까지 이어졌다. [rewardBoxGrade]는 그때 받은 보상 상자 등급이다. */
    data class Accepted(
        val photoPath: String,
        val photoSource: MealPhotoSource,
        val rewardBoxGrade: RewardBoxGrade,
    ) : MealRecordSnapshot

    /**
     * 서버가 기록을 받았지만 연출까지 가지 못한 채 종료됐는지. 고르던 음식은 되살리지 않고 홈으로 보냅니다
     * (서버가 5분 뒤 찾은 음식 전부로 계산하고, 기록 탭에서 확인할 수 있습니다).
     */
    val isRecordedWithoutCeremony: Boolean
        get() = this is Recorded

    /**
     * 되살릴 시작 단계입니다.
     *
     * - 사진 확인 중이었으면 그 사진으로 확인 단계를 되살립니다. 캐시 정리로 사진이 지워졌으면 다시 찍게 합니다.
     * - 접수까지 됐으면 연출 끝 장면으로 갑니다. 상자는 이미 우편함에 있으니 상자 장면은 다시 보여 주지 않습니다.
     * - 업로드를 시작했지만 결과를 모르거나, 기록은 받았지만 연출까지 가지 못했으면 접수 전 연출 단계로 둡니다.
     *   ViewModel 이 이를 보고 안내 후 홈으로 보냅니다.
     */
    fun toPhase(): MealCameraPhase = when (this) {
        Preview -> MealCameraPhase.Preview
        is Captured -> {
            val exists = photoPath == TUTORIAL_PHOTO_PATH || File(photoPath).exists()
            if (exists) MealCameraPhase.Captured(photoPath, photoSource) else MealCameraPhase.Preview
        }
        is Submitting -> MealCameraPhase.Ceremony(photoPath, photoSource, stage = MealCeremonyStage.Waiting)
        is Recorded -> MealCameraPhase.Ceremony(photoPath, photoSource, stage = MealCeremonyStage.Waiting)
        is Accepted -> MealCameraPhase.Ceremony(
            photoPath,
            photoSource,
            MealSubmitStatus.Accepted(rewardBoxGrade),
            MealCeremonyStage.Finished,
        )
    }

    /** 단계 이름과 그 단계의 값을 저장한다. 그 단계에 없는 값은 비워 예전 단계의 값이 남지 않게 한다. */
    fun writeTo(handle: SavedStateHandle) = when (this) {
        Preview -> write(handle, STEP_PREVIEW)
        is Captured -> write(handle, STEP_CAPTURED, photoPath, photoSource)
        is Submitting -> write(handle, STEP_SUBMITTING, photoPath, photoSource)
        is Recorded -> write(handle, STEP_RECORDED, photoPath, photoSource)
        is Accepted -> write(handle, STEP_ACCEPTED, photoPath, photoSource, rewardBoxGrade)
    }

    companion object {

        fun of(phase: MealCameraPhase): MealRecordSnapshot = when (phase) {
            MealCameraPhase.Preview -> Preview
            is MealCameraPhase.Captured -> Captured(phase.photoPath, phase.source)
            is MealCameraPhase.Ceremony -> when (val submit = phase.submit) {
                is MealSubmitStatus.Accepted -> Accepted(phase.photoPath, phase.source, submit.rewardBoxGrade)
                else -> Submitting(phase.photoPath, phase.source)
            }
            is MealCameraPhase.FoodPick -> Recorded(phase.photoPath, phase.source)
            is MealCameraPhase.NotFood -> Recorded(phase.photoPath, phase.source)
            is MealCameraPhase.SlowNotice -> Recorded(phase.photoPath, phase.source)
        }

        /**
         * 저장해 둔 사실을 읽는다. 화면에 처음 들어왔으면 저장한 적이 없어 null 이다.
         *
         * 저장된 값이 그 단계에 모자라면 읽을 수 있는 데까지만 믿는다. 사진을 읽지 못하면 다시 찍게 하고,
         * 접수됐다는데 등급을 읽지 못하면 결과를 모르는 것([Submitting])으로 본다.
         */
        fun readFrom(handle: SavedStateHandle): MealRecordSnapshot? {
            val step = handle.get<String>(KEY_STEP) ?: return null
            val photoPath = handle.get<String>(KEY_PHOTO_PATH) ?: return Preview
            val photoSource = handle.get<String>(KEY_PHOTO_SOURCE)
                ?.let { name -> MealPhotoSource.entries.firstOrNull { it.name == name } }
                ?: return Preview
            return when (step) {
                STEP_CAPTURED -> Captured(photoPath, photoSource)
                STEP_SUBMITTING -> Submitting(photoPath, photoSource)
                STEP_RECORDED -> Recorded(photoPath, photoSource)
                STEP_ACCEPTED ->
                    handle.get<String>(KEY_REWARD_BOX_GRADE)
                        ?.let { name -> RewardBoxGrade.entries.firstOrNull { it.name == name } }
                        ?.let { grade -> Accepted(photoPath, photoSource, grade) }
                        ?: Submitting(photoPath, photoSource)
                else -> Preview
            }
        }

        private fun write(
            handle: SavedStateHandle,
            step: String,
            photoPath: String? = null,
            photoSource: MealPhotoSource? = null,
            rewardBoxGrade: RewardBoxGrade? = null,
        ) {
            handle[KEY_STEP] = step
            handle[KEY_PHOTO_PATH] = photoPath
            handle[KEY_PHOTO_SOURCE] = photoSource?.name
            handle[KEY_REWARD_BOX_GRADE] = rewardBoxGrade?.name
        }

        private const val KEY_STEP = "meal_record_step"
        private const val KEY_PHOTO_PATH = "meal_record_photo_path"
        private const val KEY_PHOTO_SOURCE = "meal_record_photo_source"
        private const val KEY_REWARD_BOX_GRADE = "meal_record_reward_box_grade"

        private const val STEP_PREVIEW = "PREVIEW"
        private const val STEP_CAPTURED = "CAPTURED"
        private const val STEP_SUBMITTING = "SUBMITTING"
        private const val STEP_RECORDED = "RECORDED"
        private const val STEP_ACCEPTED = "ACCEPTED"
    }
}
