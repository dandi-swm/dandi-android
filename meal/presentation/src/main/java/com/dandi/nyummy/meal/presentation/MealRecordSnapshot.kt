package com.dandi.nyummy.meal.presentation

import androidx.lifecycle.SavedStateHandle
import com.dandi.nyummy.meal.entity.MealPhotoSource
import com.dandi.nyummy.meal.entity.RewardBoxGrade
import java.io.File

/**
 * 앱이 백그라운드에서 종료됐다가 돌아올 때 식사 기록 화면을 되살리는 데 필요한 사실만 모은 것입니다.
 * [SavedStateHandle] 에 두어 앱이 종료돼도 남습니다.
 *
 * 화면 상태를 통째로 저장하지 않습니다. 촬영·사진 가져오기처럼 하던 중인 작업은 앱과 함께 끝났고, 카메라 권한은
 * 그사이 바뀌었을 수 있어(권한을 끄면 OS가 앱을 종료한다) 돌아온 뒤 다시 확인해야 하기 때문입니다.
 * OS는 앱이 백그라운드로 갈 때의 값을 저장하므로, 백그라운드로 간 뒤에 접수된 결과는 남지 않고 접수 전으로 되살아납니다.
 *
 * @property photoPath 찍었거나 가져온 사진의 캐시 파일 경로
 * @property photoSource 사진을 어디서 가져왔는지
 * @property isSubmitStarted 냐미에게 주기를 눌러 업로드를 시작했는지
 * @property acceptedRewardBoxGrade 연출까지 이어졌다면 보상 상자 등급, 그 전이면 null
 * @property recordedMealId 서버가 기록을 받았다면 그 식사 ID, 응답을 받기 전이면 null. 찾은 음식을 고르던 중이거나
 * 음식이 아니라는 화면·인식이 늦다는 안내를 보던 중이었으면 값이 있습니다.
 */
internal data class MealRecordSnapshot(
    val photoPath: String? = null,
    val photoSource: MealPhotoSource? = null,
    val isSubmitStarted: Boolean = false,
    val acceptedRewardBoxGrade: RewardBoxGrade? = null,
    val recordedMealId: Long? = null,
) {

    /**
     * 서버가 기록을 받았지만 연출까지 가지 못한 채 종료됐는지. 고르던 음식은 되살리지 않고 홈으로 보냅니다
     * (서버가 5분 뒤 찾은 음식 전부로 계산하고, 기록 탭에서 확인할 수 있습니다).
     */
    val isRecordedWithoutCeremony: Boolean
        get() = isSubmitStarted && acceptedRewardBoxGrade == null && recordedMealId != null


    /**
     * 되살릴 시작 단계입니다.
     *
     * - 사진 확인 중이었으면 그 사진으로 확인 단계를 되살립니다. 캐시 정리로 사진이 지워졌으면 다시 찍게 합니다.
     * - 접수까지 됐으면 연출 끝 장면으로 갑니다. 상자는 이미 우편함에 있으니 상자 장면은 다시 보여 주지 않습니다.
     * - 업로드를 시작했지만 결과를 모르거나, 기록은 받았지만 연출까지 가지 못했으면 접수 전 연출 단계로 둡니다.
     *   ViewModel 이 이를 보고 안내 후 홈으로 보냅니다.
     */
    fun toPhase(): MealCameraPhase {
        val path = photoPath ?: return MealCameraPhase.Preview
        val source = photoSource ?: return MealCameraPhase.Preview
        if (!isSubmitStarted) {
            val exists = path == TUTORIAL_PHOTO_PATH || File(path).exists()
            return if (exists) MealCameraPhase.Captured(path, source) else MealCameraPhase.Preview
        }
        val grade = acceptedRewardBoxGrade
            ?: return MealCameraPhase.Ceremony(path, source, stage = MealCeremonyStage.Waiting)
        return MealCameraPhase.Ceremony(path, source, MealSubmitStatus.Accepted(grade), MealCeremonyStage.Finished)
    }

    fun writeTo(handle: SavedStateHandle) {
        handle[KEY_PHOTO_PATH] = photoPath
        handle[KEY_PHOTO_SOURCE] = photoSource?.name
        handle[KEY_SUBMIT_STARTED] = isSubmitStarted
        handle[KEY_REWARD_BOX_GRADE] = acceptedRewardBoxGrade?.name
        handle[KEY_RECORDED_MEAL_ID] = recordedMealId
    }

    companion object {

        fun of(phase: MealCameraPhase): MealRecordSnapshot = when (phase) {
            MealCameraPhase.Preview -> MealRecordSnapshot()
            is MealCameraPhase.Captured -> MealRecordSnapshot(phase.photoPath, phase.source)
            is MealCameraPhase.Ceremony -> MealRecordSnapshot(
                photoPath = phase.photoPath,
                photoSource = phase.source,
                isSubmitStarted = true,
                acceptedRewardBoxGrade = (phase.submit as? MealSubmitStatus.Accepted)?.rewardBoxGrade,
            )
            is MealCameraPhase.FoodPick -> recorded(phase.photoPath, phase.source, phase.mealId)
            // 식사 ID는 되살리는 데 쓰지 않는다. 기록을 받았다는 사실만 남긴다.
            is MealCameraPhase.NotFood -> recorded(phase.photoPath, phase.source, RECORDED_WITHOUT_ID)
            is MealCameraPhase.SlowNotice -> recorded(phase.photoPath, phase.source, RECORDED_WITHOUT_ID)
        }

        private fun recorded(photoPath: String, source: MealPhotoSource, mealId: Long) = MealRecordSnapshot(
            photoPath = photoPath,
            photoSource = source,
            isSubmitStarted = true,
            recordedMealId = mealId,
        )

        /** 저장해 둔 사실을 읽는다. 화면에 처음 들어왔으면 저장한 적이 없어 null 이다. */
        fun readFrom(handle: SavedStateHandle): MealRecordSnapshot? {
            if (!handle.contains(KEY_PHOTO_PATH)) return null
            return MealRecordSnapshot(
                photoPath = handle[KEY_PHOTO_PATH],
                photoSource = handle.get<String>(KEY_PHOTO_SOURCE)
                    ?.let { name -> MealPhotoSource.entries.firstOrNull { it.name == name } },
                isSubmitStarted = handle[KEY_SUBMIT_STARTED] ?: false,
                acceptedRewardBoxGrade = handle.get<String>(KEY_REWARD_BOX_GRADE)
                    ?.let { name -> RewardBoxGrade.entries.firstOrNull { it.name == name } },
                recordedMealId = handle[KEY_RECORDED_MEAL_ID],
            )
        }

        private const val KEY_PHOTO_PATH = "meal_record_photo_path"
        private const val KEY_PHOTO_SOURCE = "meal_record_photo_source"
        private const val KEY_SUBMIT_STARTED = "meal_record_submit_started"
        private const val KEY_REWARD_BOX_GRADE = "meal_record_reward_box_grade"
        private const val KEY_RECORDED_MEAL_ID = "meal_record_recorded_meal_id"

        /** 식사 ID를 화면이 들고 있지 않은 단계에서 "기록은 받았다"만 남길 때 쓰는 값. */
        private const val RECORDED_WITHOUT_ID = 0L
    }
}
