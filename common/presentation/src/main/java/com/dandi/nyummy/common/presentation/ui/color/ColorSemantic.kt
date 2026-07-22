package com.dandi.nyummy.common.presentation.ui.color

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Public semantic color contract generated from the published Nyummy Figma variables.
 *
 * The constructor is role-grouped because Compose [Color] occupies two JVM parameter slots.
 * Keeping all 141 values in one constructor exceeds the JVM method-parameter limit used by
 * Android Studio Layoutlib. Public consumers still use the flat `designSystemColor.role` API.
 */
@Immutable
class DesignSystemSemanticColors internal constructor(
    private val background: DesignSystemBackgroundColors,
    private val content: DesignSystemContentColors,
    private val border: DesignSystemBorderColors,
    private val asset: DesignSystemAssetColors,
    private val data: DesignSystemDataColors,
) {
    val bgDefaultLevel0: Color get() = background.bgDefaultLevel0
    val bgCanvasAlt: Color get() = background.bgCanvasAlt
    val bgDefaultLevel1: Color get() = background.bgDefaultLevel1
    val bgSurfaceSubtle: Color get() = background.bgSurfaceSubtle
    val bgDefaultLevel2: Color get() = background.bgDefaultLevel2
    val bgSurfaceStrong: Color get() = background.bgSurfaceStrong
    val bgSurfaceInverse: Color get() = background.bgSurfaceInverse
    val bgBrandDefault: Color get() = background.bgBrandDefault
    val bgBrandSoft: Color get() = background.bgBrandSoft
    val bgSuccessSoft: Color get() = background.bgSuccessSoft
    val bgSuccessDefault: Color get() = background.bgSuccessDefault
    val bgInfoSoft: Color get() = background.bgInfoSoft
    val bgInfoDefault: Color get() = background.bgInfoDefault
    val bgWarningSoft: Color get() = background.bgWarningSoft
    val bgWarningDefault: Color get() = background.bgWarningDefault
    val bgDangerSoft: Color get() = background.bgDangerSoft
    val bgDangerDefault: Color get() = background.bgDangerDefault
    val bgProgressTrack: Color get() = background.bgProgressTrack
    val bgActionPrimaryDefault: Color get() = background.bgActionPrimaryDefault
    val bgActionPrimaryPressed: Color get() = background.bgActionPrimaryPressed
    val bgNavigationFloating: Color get() = background.bgNavigationFloating
    val bgNavigationFullWidth: Color get() = background.bgNavigationFullWidth
    val bgActionSecondaryDefault: Color get() = background.bgActionSecondaryDefault
    val bgSurfaceIvory: Color get() = background.bgSurfaceIvory
    val bgScrimModal: Color get() = background.bgScrimModal
    val bgScrimDefault: Color get() = background.bgScrimDefault
    val bgStatusPositive: Color get() = background.bgStatusPositive
    val bgSelectionPrimary: Color get() = background.bgSelectionPrimary
    val bgActionPrimaryDisabled: Color get() = background.bgActionPrimaryDisabled
    val bgActionSecondaryPressed: Color get() = background.bgActionSecondaryPressed
    val bgActionSecondaryDisabled: Color get() = background.bgActionSecondaryDisabled
    val bgActionDangerDefault: Color get() = background.bgActionDangerDefault
    val bgActionDangerPressed: Color get() = background.bgActionDangerPressed
    val bgActionDangerDisabled: Color get() = background.bgActionDangerDisabled
    val bgActionRewardDefault: Color get() = background.bgActionRewardDefault
    val bgActionRewardPressed: Color get() = background.bgActionRewardPressed
    val bgInputDefault: Color get() = background.bgInputDefault
    val bgInputDisabled: Color get() = background.bgInputDisabled
    val bgSurfaceControl: Color get() = background.bgSurfaceControl
    val bgSurfaceCardSubtle: Color get() = background.bgSurfaceCardSubtle
    val bgStatusProcessing: Color get() = background.bgStatusProcessing
    val bgStatusWarning: Color get() = background.bgStatusWarning
    val bgCalendarSelected: Color get() = background.bgCalendarSelected
    val bgMealPhoto: Color get() = background.bgMealPhoto
    val bgProgressNutritionTrack: Color get() = background.bgProgressNutritionTrack
    val bgCoachBubble: Color get() = background.bgCoachBubble
    val bgSheetHandle: Color get() = background.bgSheetHandle
    val contentDefaultLevel0: Color get() = content.contentDefaultLevel0
    val contentDefaultLevel1: Color get() = content.contentDefaultLevel1
    val contentDefaultLevel2: Color get() = content.contentDefaultLevel2
    val contentDefaultLevel3: Color get() = content.contentDefaultLevel3
    val contentInverseDefault: Color get() = content.contentInverseDefault
    val contentAccent: Color get() = content.contentAccent
    val contentSuccess: Color get() = content.contentSuccess
    val contentInfo: Color get() = content.contentInfo
    val contentWarning: Color get() = content.contentWarning
    val contentError: Color get() = content.contentError
    val contentIconLevel0: Color get() = content.contentIconLevel0
    val contentIconLevel1: Color get() = content.contentIconLevel1
    val contentIconSuccess: Color get() = content.contentIconSuccess
    val contentIconInfo: Color get() = content.contentIconInfo
    val contentIconWarning: Color get() = content.contentIconWarning
    val contentIconDanger: Color get() = content.contentIconDanger
    val contentActionPrimary: Color get() = content.contentActionPrimary
    val contentAccentSage: Color get() = content.contentAccentSage
    val contentAccentCoral: Color get() = content.contentAccentCoral
    val contentActionDisabled: Color get() = content.contentActionDisabled
    val contentActionSecondary: Color get() = content.contentActionSecondary
    val contentActionSecondaryDisabled: Color get() = content.contentActionSecondaryDisabled
    val contentActionDanger: Color get() = content.contentActionDanger
    val contentActionDangerDisabled: Color get() = content.contentActionDangerDisabled
    val contentActionReward: Color get() = content.contentActionReward
    val contentInputValue: Color get() = content.contentInputValue
    val contentInputPlaceholder: Color get() = content.contentInputPlaceholder
    val contentInputDisabled: Color get() = content.contentInputDisabled
    val contentCalendarAdjacentMonth: Color get() = content.contentCalendarAdjacentMonth
    val contentCalendarWeekday: Color get() = content.contentCalendarWeekday
    val contentCalendarAction: Color get() = content.contentCalendarAction
    val contentCalendarSaturday: Color get() = content.contentCalendarSaturday
    val contentCalendarSunday: Color get() = content.contentCalendarSunday
    val contentStatusProcessing: Color get() = content.contentStatusProcessing
    val contentStatusSupporting: Color get() = content.contentStatusSupporting
    val contentStatusWarning: Color get() = content.contentStatusWarning
    val contentNutritionLabel: Color get() = content.contentNutritionLabel
    val contentCalendarSelected: Color get() = content.contentCalendarSelected
    val contentCalendarDate: Color get() = content.contentCalendarDate
    val contentEconomyBalance: Color get() = content.contentEconomyBalance
    val contentSelectionPrimary: Color get() = content.contentSelectionPrimary
    val borderDefaultLevel1: Color get() = border.borderDefaultLevel1
    val borderDefaultLevel0: Color get() = border.borderDefaultLevel0
    val borderBrandDefault: Color get() = border.borderBrandDefault
    val borderSuccessDefault: Color get() = border.borderSuccessDefault
    val borderInfoDefault: Color get() = border.borderInfoDefault
    val borderWarningDefault: Color get() = border.borderWarningDefault
    val borderDangerDefault: Color get() = border.borderDangerDefault
    val borderDefaultLevel2: Color get() = border.borderDefaultLevel2
    val borderNavigationDefault: Color get() = border.borderNavigationDefault
    val borderNavigationFullWidthDivider: Color get() = border.borderNavigationFullWidthDivider
    val borderSageSoft: Color get() = border.borderSageSoft
    val borderInputFocused: Color get() = border.borderInputFocused
    val borderSelectionPrimary: Color get() = border.borderSelectionPrimary
    val borderActionSecondary: Color get() = border.borderActionSecondary
    val borderInputDefault: Color get() = border.borderInputDefault
    val borderInputError: Color get() = border.borderInputError
    val borderCalendarGrid: Color get() = border.borderCalendarGrid
    val borderCalendarOutline: Color get() = border.borderCalendarOutline
    val borderCardSubtle: Color get() = border.borderCardSubtle
    val borderMealRow: Color get() = border.borderMealRow
    val borderNutrition: Color get() = border.borderNutrition
    val borderStatusProcessing: Color get() = border.borderStatusProcessing
    val borderCalendarSelected: Color get() = border.borderCalendarSelected
    val borderMealPhoto: Color get() = border.borderMealPhoto
    val borderCoachBubble: Color get() = border.borderCoachBubble
    val assetCharacterBody: Color get() = asset.assetCharacterBody
    val assetCharacterHighlight: Color get() = asset.assetCharacterHighlight
    val assetCharacterAccent: Color get() = asset.assetCharacterAccent
    val assetCharacterInk: Color get() = asset.assetCharacterInk
    val assetCharacterShadow: Color get() = asset.assetCharacterShadow
    val assetSceneRoomWall: Color get() = asset.assetSceneRoomWall
    val assetSceneRoomFloor: Color get() = asset.assetSceneRoomFloor
    val assetSceneWindowSky: Color get() = asset.assetSceneWindowSky
    val assetScenePixelOutline: Color get() = asset.assetScenePixelOutline
    val assetScenePixelShadow: Color get() = asset.assetScenePixelShadow
    val dataNutrientCalorie: Color get() = data.dataNutrientCalorie
    val dataNutrientCarbohydrate: Color get() = data.dataNutrientCarbohydrate
    val dataNutrientProtein: Color get() = data.dataNutrientProtein
    val dataNutrientFat: Color get() = data.dataNutrientFat
    val dataEvaluationPositive: Color get() = data.dataEvaluationPositive
    val dataEvaluationNegative: Color get() = data.dataEvaluationNegative
    val dataEvaluationUnrecorded: Color get() = data.dataEvaluationUnrecorded
    val dataProgressDefault: Color get() = data.dataProgressDefault
    val dataTrendPrimary: Color get() = data.dataTrendPrimary
    val dataCalendarRecorded: Color get() = data.dataCalendarRecorded
    val dataCalendarToday: Color get() = data.dataCalendarToday
    val dataEconomyCoinBase: Color get() = data.dataEconomyCoinBase
    val dataEconomyCoinShadow: Color get() = data.dataEconomyCoinShadow
    val dataEconomyCoinHighlight: Color get() = data.dataEconomyCoinHighlight
    val dataCollectionFoodPack: Color get() = data.dataCollectionFoodPack
    val dataCollectionCatPack: Color get() = data.dataCollectionCatPack
    val dataProgressDailyTotal: Color get() = data.dataProgressDailyTotal
    val dataProgressMealContribution: Color get() = data.dataProgressMealContribution

    fun withStringKey(key: String): Color = when (key) {
        "bgDefaultLevel0" -> bgDefaultLevel0
        "bgCanvasAlt" -> bgCanvasAlt
        "bgDefaultLevel1" -> bgDefaultLevel1
        "bgSurfaceSubtle" -> bgSurfaceSubtle
        "bgDefaultLevel2" -> bgDefaultLevel2
        "bgSurfaceStrong" -> bgSurfaceStrong
        "bgSurfaceInverse" -> bgSurfaceInverse
        "bgBrandDefault" -> bgBrandDefault
        "bgBrandSoft" -> bgBrandSoft
        "bgSuccessSoft" -> bgSuccessSoft
        "bgSuccessDefault" -> bgSuccessDefault
        "bgInfoSoft" -> bgInfoSoft
        "bgInfoDefault" -> bgInfoDefault
        "bgWarningSoft" -> bgWarningSoft
        "bgWarningDefault" -> bgWarningDefault
        "bgDangerSoft" -> bgDangerSoft
        "bgDangerDefault" -> bgDangerDefault
        "contentDefaultLevel0" -> contentDefaultLevel0
        "contentDefaultLevel1" -> contentDefaultLevel1
        "contentDefaultLevel2" -> contentDefaultLevel2
        "contentDefaultLevel3" -> contentDefaultLevel3
        "contentInverseDefault" -> contentInverseDefault
        "contentAccent" -> contentAccent
        "contentSuccess" -> contentSuccess
        "contentInfo" -> contentInfo
        "contentWarning" -> contentWarning
        "contentError" -> contentError
        "contentIconLevel0" -> contentIconLevel0
        "contentIconLevel1" -> contentIconLevel1
        "contentIconSuccess" -> contentIconSuccess
        "contentIconInfo" -> contentIconInfo
        "contentIconWarning" -> contentIconWarning
        "contentIconDanger" -> contentIconDanger
        "borderDefaultLevel1" -> borderDefaultLevel1
        "borderDefaultLevel0" -> borderDefaultLevel0
        "borderBrandDefault" -> borderBrandDefault
        "borderSuccessDefault" -> borderSuccessDefault
        "borderInfoDefault" -> borderInfoDefault
        "borderWarningDefault" -> borderWarningDefault
        "borderDangerDefault" -> borderDangerDefault
        "bgProgressTrack" -> bgProgressTrack
        "assetCharacterBody" -> assetCharacterBody
        "assetCharacterHighlight" -> assetCharacterHighlight
        "assetCharacterAccent" -> assetCharacterAccent
        "assetCharacterInk" -> assetCharacterInk
        "assetCharacterShadow" -> assetCharacterShadow
        "assetSceneRoomWall" -> assetSceneRoomWall
        "assetSceneRoomFloor" -> assetSceneRoomFloor
        "assetSceneWindowSky" -> assetSceneWindowSky
        "assetScenePixelOutline" -> assetScenePixelOutline
        "assetScenePixelShadow" -> assetScenePixelShadow
        "bgActionPrimaryDefault" -> bgActionPrimaryDefault
        "bgActionPrimaryPressed" -> bgActionPrimaryPressed
        "contentActionPrimary" -> contentActionPrimary
        "dataNutrientCalorie" -> dataNutrientCalorie
        "dataNutrientCarbohydrate" -> dataNutrientCarbohydrate
        "dataNutrientProtein" -> dataNutrientProtein
        "dataNutrientFat" -> dataNutrientFat
        "dataEvaluationPositive" -> dataEvaluationPositive
        "dataEvaluationNegative" -> dataEvaluationNegative
        "dataEvaluationUnrecorded" -> dataEvaluationUnrecorded
        "borderDefaultLevel2" -> borderDefaultLevel2
        "bgNavigationFloating" -> bgNavigationFloating
        "bgNavigationFullWidth" -> bgNavigationFullWidth
        "borderNavigationDefault" -> borderNavigationDefault
        "borderNavigationFullWidthDivider" -> borderNavigationFullWidthDivider
        "bgActionSecondaryDefault" -> bgActionSecondaryDefault
        "contentAccentSage" -> contentAccentSage
        "dataProgressDefault" -> dataProgressDefault
        "contentAccentCoral" -> contentAccentCoral
        "bgSurfaceIvory" -> bgSurfaceIvory
        "borderSageSoft" -> borderSageSoft
        "bgScrimModal" -> bgScrimModal
        "bgScrimDefault" -> bgScrimDefault
        "bgStatusPositive" -> bgStatusPositive
        "bgSelectionPrimary" -> bgSelectionPrimary
        "dataTrendPrimary" -> dataTrendPrimary
        "borderInputFocused" -> borderInputFocused
        "dataCalendarRecorded" -> dataCalendarRecorded
        "borderSelectionPrimary" -> borderSelectionPrimary
        "bgActionPrimaryDisabled" -> bgActionPrimaryDisabled
        "contentActionDisabled" -> contentActionDisabled
        "bgActionSecondaryPressed" -> bgActionSecondaryPressed
        "bgActionSecondaryDisabled" -> bgActionSecondaryDisabled
        "contentActionSecondary" -> contentActionSecondary
        "contentActionSecondaryDisabled" -> contentActionSecondaryDisabled
        "borderActionSecondary" -> borderActionSecondary
        "bgActionDangerDefault" -> bgActionDangerDefault
        "bgActionDangerPressed" -> bgActionDangerPressed
        "bgActionDangerDisabled" -> bgActionDangerDisabled
        "contentActionDanger" -> contentActionDanger
        "contentActionDangerDisabled" -> contentActionDangerDisabled
        "bgActionRewardDefault" -> bgActionRewardDefault
        "bgActionRewardPressed" -> bgActionRewardPressed
        "contentActionReward" -> contentActionReward
        "bgInputDefault" -> bgInputDefault
        "bgInputDisabled" -> bgInputDisabled
        "borderInputDefault" -> borderInputDefault
        "borderInputError" -> borderInputError
        "contentInputValue" -> contentInputValue
        "contentInputPlaceholder" -> contentInputPlaceholder
        "contentInputDisabled" -> contentInputDisabled
        "contentCalendarAdjacentMonth" -> contentCalendarAdjacentMonth
        "contentCalendarWeekday" -> contentCalendarWeekday
        "contentCalendarAction" -> contentCalendarAction
        "contentCalendarSaturday" -> contentCalendarSaturday
        "contentCalendarSunday" -> contentCalendarSunday
        "contentStatusProcessing" -> contentStatusProcessing
        "contentStatusSupporting" -> contentStatusSupporting
        "contentStatusWarning" -> contentStatusWarning
        "contentNutritionLabel" -> contentNutritionLabel
        "bgSurfaceControl" -> bgSurfaceControl
        "bgSurfaceCardSubtle" -> bgSurfaceCardSubtle
        "bgStatusProcessing" -> bgStatusProcessing
        "bgStatusWarning" -> bgStatusWarning
        "borderCalendarGrid" -> borderCalendarGrid
        "borderCalendarOutline" -> borderCalendarOutline
        "borderCardSubtle" -> borderCardSubtle
        "borderMealRow" -> borderMealRow
        "borderNutrition" -> borderNutrition
        "borderStatusProcessing" -> borderStatusProcessing
        "bgCalendarSelected" -> bgCalendarSelected
        "borderCalendarSelected" -> borderCalendarSelected
        "contentCalendarSelected" -> contentCalendarSelected
        "dataCalendarToday" -> dataCalendarToday
        "contentCalendarDate" -> contentCalendarDate
        "contentEconomyBalance" -> contentEconomyBalance
        "dataEconomyCoinBase" -> dataEconomyCoinBase
        "dataEconomyCoinShadow" -> dataEconomyCoinShadow
        "dataEconomyCoinHighlight" -> dataEconomyCoinHighlight
        "dataCollectionFoodPack" -> dataCollectionFoodPack
        "dataCollectionCatPack" -> dataCollectionCatPack
        "bgMealPhoto" -> bgMealPhoto
        "borderMealPhoto" -> borderMealPhoto
        "bgProgressNutritionTrack" -> bgProgressNutritionTrack
        "dataProgressDailyTotal" -> dataProgressDailyTotal
        "dataProgressMealContribution" -> dataProgressMealContribution
        "bgCoachBubble" -> bgCoachBubble
        "borderCoachBubble" -> borderCoachBubble
        "bgSheetHandle" -> bgSheetHandle
        "contentSelectionPrimary" -> contentSelectionPrimary
        else -> contentDefaultLevel3
    }
}

@Immutable
internal class DesignSystemBackgroundColors internal constructor(
    val bgDefaultLevel0: Color,
    val bgCanvasAlt: Color,
    val bgDefaultLevel1: Color,
    val bgSurfaceSubtle: Color,
    val bgDefaultLevel2: Color,
    val bgSurfaceStrong: Color,
    val bgSurfaceInverse: Color,
    val bgBrandDefault: Color,
    val bgBrandSoft: Color,
    val bgSuccessSoft: Color,
    val bgSuccessDefault: Color,
    val bgInfoSoft: Color,
    val bgInfoDefault: Color,
    val bgWarningSoft: Color,
    val bgWarningDefault: Color,
    val bgDangerSoft: Color,
    val bgDangerDefault: Color,
    val bgProgressTrack: Color,
    val bgActionPrimaryDefault: Color,
    val bgActionPrimaryPressed: Color,
    val bgNavigationFloating: Color,
    val bgNavigationFullWidth: Color,
    val bgActionSecondaryDefault: Color,
    val bgSurfaceIvory: Color,
    val bgScrimModal: Color,
    val bgScrimDefault: Color,
    val bgStatusPositive: Color,
    val bgSelectionPrimary: Color,
    val bgActionPrimaryDisabled: Color,
    val bgActionSecondaryPressed: Color,
    val bgActionSecondaryDisabled: Color,
    val bgActionDangerDefault: Color,
    val bgActionDangerPressed: Color,
    val bgActionDangerDisabled: Color,
    val bgActionRewardDefault: Color,
    val bgActionRewardPressed: Color,
    val bgInputDefault: Color,
    val bgInputDisabled: Color,
    val bgSurfaceControl: Color,
    val bgSurfaceCardSubtle: Color,
    val bgStatusProcessing: Color,
    val bgStatusWarning: Color,
    val bgCalendarSelected: Color,
    val bgMealPhoto: Color,
    val bgProgressNutritionTrack: Color,
    val bgCoachBubble: Color,
    val bgSheetHandle: Color,
)

@Immutable
internal class DesignSystemContentColors internal constructor(
    val contentDefaultLevel0: Color,
    val contentDefaultLevel1: Color,
    val contentDefaultLevel2: Color,
    val contentDefaultLevel3: Color,
    val contentInverseDefault: Color,
    val contentAccent: Color,
    val contentSuccess: Color,
    val contentInfo: Color,
    val contentWarning: Color,
    val contentError: Color,
    val contentIconLevel0: Color,
    val contentIconLevel1: Color,
    val contentIconSuccess: Color,
    val contentIconInfo: Color,
    val contentIconWarning: Color,
    val contentIconDanger: Color,
    val contentActionPrimary: Color,
    val contentAccentSage: Color,
    val contentAccentCoral: Color,
    val contentActionDisabled: Color,
    val contentActionSecondary: Color,
    val contentActionSecondaryDisabled: Color,
    val contentActionDanger: Color,
    val contentActionDangerDisabled: Color,
    val contentActionReward: Color,
    val contentInputValue: Color,
    val contentInputPlaceholder: Color,
    val contentInputDisabled: Color,
    val contentCalendarAdjacentMonth: Color,
    val contentCalendarWeekday: Color,
    val contentCalendarAction: Color,
    val contentCalendarSaturday: Color,
    val contentCalendarSunday: Color,
    val contentStatusProcessing: Color,
    val contentStatusSupporting: Color,
    val contentStatusWarning: Color,
    val contentNutritionLabel: Color,
    val contentCalendarSelected: Color,
    val contentCalendarDate: Color,
    val contentEconomyBalance: Color,
    val contentSelectionPrimary: Color,
)

@Immutable
internal class DesignSystemBorderColors internal constructor(
    val borderDefaultLevel1: Color,
    val borderDefaultLevel0: Color,
    val borderBrandDefault: Color,
    val borderSuccessDefault: Color,
    val borderInfoDefault: Color,
    val borderWarningDefault: Color,
    val borderDangerDefault: Color,
    val borderDefaultLevel2: Color,
    val borderNavigationDefault: Color,
    val borderNavigationFullWidthDivider: Color,
    val borderSageSoft: Color,
    val borderInputFocused: Color,
    val borderSelectionPrimary: Color,
    val borderActionSecondary: Color,
    val borderInputDefault: Color,
    val borderInputError: Color,
    val borderCalendarGrid: Color,
    val borderCalendarOutline: Color,
    val borderCardSubtle: Color,
    val borderMealRow: Color,
    val borderNutrition: Color,
    val borderStatusProcessing: Color,
    val borderCalendarSelected: Color,
    val borderMealPhoto: Color,
    val borderCoachBubble: Color,
)

@Immutable
internal class DesignSystemAssetColors internal constructor(
    val assetCharacterBody: Color,
    val assetCharacterHighlight: Color,
    val assetCharacterAccent: Color,
    val assetCharacterInk: Color,
    val assetCharacterShadow: Color,
    val assetSceneRoomWall: Color,
    val assetSceneRoomFloor: Color,
    val assetSceneWindowSky: Color,
    val assetScenePixelOutline: Color,
    val assetScenePixelShadow: Color,
)

@Immutable
internal class DesignSystemDataColors internal constructor(
    val dataNutrientCalorie: Color,
    val dataNutrientCarbohydrate: Color,
    val dataNutrientProtein: Color,
    val dataNutrientFat: Color,
    val dataEvaluationPositive: Color,
    val dataEvaluationNegative: Color,
    val dataEvaluationUnrecorded: Color,
    val dataProgressDefault: Color,
    val dataTrendPrimary: Color,
    val dataCalendarRecorded: Color,
    val dataCalendarToday: Color,
    val dataEconomyCoinBase: Color,
    val dataEconomyCoinShadow: Color,
    val dataEconomyCoinHighlight: Color,
    val dataCollectionFoodPack: Color,
    val dataCollectionCatPack: Color,
    val dataProgressDailyTotal: Color,
    val dataProgressMealContribution: Color,
)
