package com.dandi.nyummy.meal.presentation.component.ceremony

import androidx.compose.ui.geometry.Offset
import com.dandi.nyummy.common.presentation.cutscene.CutsceneBackdrop
import com.dandi.nyummy.common.presentation.cutscene.CutsceneBackdropChange
import com.dandi.nyummy.common.presentation.cutscene.CutsceneFinale
import com.dandi.nyummy.common.presentation.cutscene.CutsceneInk
import com.dandi.nyummy.common.presentation.cutscene.CutscenePage
import com.dandi.nyummy.common.presentation.cutscene.CutscenePage.Bottom
import com.dandi.nyummy.common.presentation.cutscene.CutscenePage.Seam
import com.dandi.nyummy.common.presentation.cutscene.CutscenePage.Split
import com.dandi.nyummy.common.presentation.cutscene.CutscenePage.Top
import com.dandi.nyummy.common.presentation.cutscene.CutscenePanel
import com.dandi.nyummy.common.presentation.cutscene.CutsceneSpec
import com.dandi.nyummy.common.presentation.cutscene.CutsceneWord
import com.dandi.nyummy.meal.presentation.MealCutscene
import com.dandi.nyummy.meal.presentation.R

/**
 * 식사 맛보기 컷신 25편의 정의. 360×780 캔버스를 [CutscenePage] 로 겹치지 않게 나눈다.
 *
 * 먹는 음식은 편마다 다르게 그린다. 칸 비율은 그림 비율에 가깝게 잡아 잘림을 줄인다. 3:2 그림은 한 줄 전체, 1:1 그림은 반 줄에 넣고,
 * 그래도 넘치는 쪽은 칸마다 정한 초점을 남기고 자른다. 그림은 밀도 폴더(mdpi~xxhdpi)에 WebP 로 두고 여러 편이 같이 쓴다.
 */
internal val MealCutscene.spec: CutsceneSpec
    get() = when (this) {
        MealCutscene.Tiyot -> Tiyot
        MealCutscene.HotFuuFuu -> HotFuuFuu
        MealCutscene.StrayParty -> StrayParty
        MealCutscene.FoodCritic -> FoodCritic
        MealCutscene.HamsterCheeks -> HamsterCheeks
        MealCutscene.SneakyBite -> SneakyBite
        MealCutscene.DivingCatch -> DivingCatch
        MealCutscene.FoodFighter -> FoodFighter
        MealCutscene.Foodstagram -> Foodstagram
        MealCutscene.ChopstickChallenge -> ChopstickChallenge
        MealCutscene.MorningWakeUp -> MorningWakeUp
        MealCutscene.CosmicTaste -> CosmicTaste
        MealCutscene.BlanketBurrito -> BlanketBurrito
        MealCutscene.LateToast -> LateToast
        MealCutscene.MilkMustache -> MilkMustache
        MealCutscene.CerealFlood -> CerealFlood
        MealCutscene.LoafMorning -> LoafMorning
        MealCutscene.MidnightAccomplice -> MidnightAccomplice
        MealCutscene.RamenCountdown -> RamenCountdown
        MealCutscene.DeliveryEars -> DeliveryEars
        MealCutscene.MidnightAsmr -> MidnightAsmr
        MealCutscene.ConvenienceStore -> ConvenienceStore
        MealCutscene.GiantNyummy -> GiantNyummy
        MealCutscene.LuckyNyummy -> LuckyNyummy
        MealCutscene.AllStars -> AllStars
    }

/** 띠욧 마지막 칸. 기본 띠욧과 앗뜨거가 같은 자리에 쓰고, 그림은 편마다 먹는 음식에 맞춘다. */
private fun tiyotFinale(image: Int, top: Seam, atMillis: Long, expandAtMillis: Long) = CutsceneFinale(
    image = image,
    shape = CutscenePage.row(top, Bottom),
    atMillis = atMillis,
    expandAtMillis = expandAtMillis,
    focus = Offset(0.5f, 0.55f),
    word = R.string.meal_ceremony_reaction_word,
    wordTop = 120f,
    wordFullScale = 1.6f,
)

// 네 줄 배치: 한 줄(3:2) / 반반(1:1 두 칸) / 한 줄(3:2) / 마지막 칸. 기본 띠욧, 길냥이, 평론가, 햄스터가 같이 쓴다.
private val FourRow1 = Seam(214f, 198f)
private val FourRow2 = Seam(388f, 402f)
private val FourRow2Split = Split(186f, 176f)
private val FourRow3 = Seam(590f, 572f)

/** 네 줄 배치의 칸 네 개. 위에서부터 한 줄, 왼쪽 반, 오른쪽 반, 한 줄 순서다. */
private val FourRowShapes = listOf(
    CutscenePage.row(Top, FourRow1),
    CutscenePage.left(FourRow1, FourRow2, FourRow2Split),
    CutscenePage.right(FourRow1, FourRow2, FourRow2Split),
    CutscenePage.row(FourRow2, FourRow3),
)

/** 네 줄 배치의 마지막 칸. */
private val FourRowFinale = CutscenePage.row(FourRow3, Bottom)

/** 네 줄 배치의 마지막 칸. 칸에 박혔다가 펼쳐지고, 글자는 위로 올라가며 커진다. */
private fun fourRowFinale(
    image: Int,
    word: Int,
    atMillis: Long,
    expandAtMillis: Long,
    wordScale: Float = 1.2f,
    wordFullScale: Float = 1.6f,
    wordInk: CutsceneInk = CutsceneInk.Brand,
) = CutsceneFinale(
    image = image,
    shape = FourRowFinale,
    atMillis = atMillis,
    expandAtMillis = expandAtMillis,
    focus = Offset(0.5f, 0.55f),
    word = word,
    wordTop = 120f,
    wordScale = wordScale,
    wordFullScale = wordFullScale,
    wordInk = wordInk,
)

// 기본 띠욧: 뜨기 / 꼴깍 | 반짝눈 / 앙! / 띠욧(마지막 칸)

private val Tiyot = CutsceneSpec(
    backdrop = CutsceneBackdrop.Paper,
    panels = listOf(
        CutscenePanel(R.drawable.meal_cutscene_scoop, FourRowShapes[0], atMillis = 300L, focus = Offset(0.6f, 0.4f)),
        CutscenePanel(R.drawable.meal_cutscene_lift, FourRowShapes[1], atMillis = 850L),
        CutscenePanel(R.drawable.meal_cutscene_eyes, FourRowShapes[2], atMillis = 1_350L),
        CutscenePanel(R.drawable.meal_cutscene_bite, FourRowShapes[3], atMillis = 1_900L, focus = Offset(0.55f, 0.45f)),
    ),
    words = listOf(
        CutsceneWord(R.string.meal_ceremony_bite_word, x = 310f, y = 428f, atMillis = 2_060L, tiltDegrees = 10f),
    ),
    finale = tiyotFinale(R.drawable.meal_cutscene_finale_star, FourRow3, atMillis = 2_700L, expandAtMillis = 3_600L),
)

// 길냥이 습격: 앙~ / 응? | 빤히… / 우르르! 같이 먹자… / 다같이 띠욧!!(마지막 칸)
private val StrayParty = CutsceneSpec(
    backdrop = CutsceneBackdrop.Paper,
    panels = listOf(
        CutscenePanel(R.drawable.meal_cutscene_stray_lift, FourRowShapes[0], atMillis = 300L),
        CutscenePanel(R.drawable.meal_cutscene_stray_sideeye, FourRowShapes[1], atMillis = 1_100L),
        CutscenePanel(R.drawable.meal_cutscene_stray_peek, FourRowShapes[2], atMillis = 1_700L),
        CutscenePanel(R.drawable.meal_cutscene_stray_crowd, FourRowShapes[3], atMillis = 2_500L),
    ),
    words = listOf(
        CutsceneWord(R.string.meal_cutscene_word_aang, x = 292f, y = 170f, atMillis = 460L, tiltDegrees = 8f),
        CutsceneWord(R.string.meal_cutscene_word_huh, x = 136f, y = 250f, atMillis = 1_250L, tiltDegrees = -6f, ink = CutsceneInk.Sleepy),
        CutsceneWord(R.string.meal_cutscene_word_stare, x = 272f, y = 366f, atMillis = 1_850L, tiltDegrees = 6f, scale = 0.85f, ink = CutsceneInk.Sleepy),
        CutsceneWord(R.string.meal_cutscene_word_swarm, x = 92f, y = 426f, atMillis = 2_580L, tiltDegrees = -8f, scale = 1.1f, ink = CutsceneInk.Hot),
        CutsceneWord(R.string.meal_cutscene_word_share, x = 268f, y = 506f, atMillis = 3_100L, tiltDegrees = 4f, scale = 0.8f),
    ),
    finale = fourRowFinale(
        R.drawable.meal_cutscene_finale_stray_party,
        R.string.meal_cutscene_word_tiyot_together,
        atMillis = 3_700L,
        expandAtMillis = 4_500L,
        wordScale = 1f,
        wordFullScale = 1.3f,
    ),
)

// 미식 평론가: 흠… / 킁킁 | 오물… / 이, 이 맛은…! / 띠욧!!(마지막 칸)
// 진지하게 굴다가 무너지는 한 박자를 위해, 놀라는 칸과 마지막 칸 사이를 조금 길게 둔다.
private val FoodCritic = CutsceneSpec(
    backdrop = CutsceneBackdrop.Paper,
    panels = listOf(
        CutscenePanel(R.drawable.meal_cutscene_critic_examine, FourRowShapes[0], atMillis = 300L),
        CutscenePanel(R.drawable.meal_cutscene_critic_sniff, FourRowShapes[1], atMillis = 1_000L),
        CutscenePanel(R.drawable.meal_cutscene_critic_taste, FourRowShapes[2], atMillis = 1_600L),
        CutscenePanel(R.drawable.meal_cutscene_critic_shock, FourRowShapes[3], atMillis = 2_300L),
    ),
    words = listOf(
        CutsceneWord(R.string.meal_cutscene_word_hmm, x = 292f, y = 170f, atMillis = 520L, tiltDegrees = 6f, scale = 0.85f, ink = CutsceneInk.Sleepy),
        CutsceneWord(R.string.meal_cutscene_word_sniff, x = 70f, y = 368f, atMillis = 1_200L, tiltDegrees = -6f, scale = 0.85f),
        CutsceneWord(R.string.meal_cutscene_word_chew_serious, x = 300f, y = 250f, atMillis = 1_800L, tiltDegrees = 6f, scale = 0.85f, ink = CutsceneInk.Sleepy),
        CutsceneWord(R.string.meal_cutscene_word_this_taste, x = 124f, y = 428f, atMillis = 2_420L, tiltDegrees = -4f, scale = 0.95f, ink = CutsceneInk.Hot),
    ),
    finale = fourRowFinale(
        R.drawable.meal_cutscene_finale_critic,
        R.string.meal_ceremony_reaction_word,
        atMillis = 3_200L,
        expandAtMillis = 4_000L,
    ),
)

// 볼빵빵 햄스터: 산더미… / 아~앙 | 빵빵! / 우물우물… / 꿀꺽… 띠욧!!(마지막 칸)
private val HamsterCheeks = CutsceneSpec(
    backdrop = CutsceneBackdrop.Paper,
    panels = listOf(
        CutscenePanel(R.drawable.meal_cutscene_hamster_spoon, FourRowShapes[0], atMillis = 300L),
        CutscenePanel(R.drawable.meal_cutscene_hamster_open, FourRowShapes[1], atMillis = 1_000L),
        CutscenePanel(R.drawable.meal_cutscene_hamster_cheeks, FourRowShapes[2], atMillis = 1_500L),
        CutscenePanel(R.drawable.meal_cutscene_hamster_chew, FourRowShapes[3], atMillis = 2_200L),
    ),
    words = listOf(
        CutsceneWord(R.string.meal_cutscene_word_mountain, x = 286f, y = 170f, atMillis = 520L, tiltDegrees = 6f, scale = 0.85f),
        CutsceneWord(R.string.meal_cutscene_word_open_wide, x = 96f, y = 370f, atMillis = 1_150L, tiltDegrees = -8f),
        CutsceneWord(R.string.meal_cutscene_word_puffed, x = 300f, y = 250f, atMillis = 1_600L, tiltDegrees = 8f, scale = 1.1f, ink = CutsceneInk.Hot),
        CutsceneWord(R.string.meal_cutscene_word_chomp, x = 112f, y = 426f, atMillis = 2_400L, tiltDegrees = -6f, scale = 0.85f),
    ),
    finale = fourRowFinale(
        R.drawable.meal_cutscene_finale_hamster,
        R.string.meal_cutscene_word_gulp_tiyot,
        atMillis = 3_100L,
        expandAtMillis = 3_900L,
        wordScale = 1f,
        wordFullScale = 1.3f,
    ),
)

// 앗뜨거: 모락모락 / 앗뜨!! / 후- 후- | 다시 앙! / 띠욧(마지막 칸)
private val HotRow1 = Seam(180f, 166f)
private val HotRow2 = Seam(392f, 374f)
private val HotRow3 = Seam(580f, 594f)
private val HotRow3Split = Split(176f, 184f)

private val HotFuuFuu = CutsceneSpec(
    backdrop = CutsceneBackdrop.Paper,
    panels = listOf(
        CutscenePanel(R.drawable.meal_cutscene_hot_scoop, CutscenePage.row(Top, HotRow1), atMillis = 300L, focus = Offset(0.6f, 0.4f)),
        CutscenePanel(
            R.drawable.meal_cutscene_hot,
            CutscenePage.row(HotRow1, HotRow2),
            atMillis = 850L,
            focus = Offset(0.55f, 0.45f),
        ),
        CutscenePanel(R.drawable.meal_cutscene_blow, CutscenePage.left(HotRow2, HotRow3, HotRow3Split), atMillis = 1_700L),
        CutscenePanel(
            R.drawable.meal_cutscene_hot_bite,
            CutscenePage.right(HotRow2, HotRow3, HotRow3Split),
            atMillis = 2_650L,
            focus = Offset(0.6f, 0.45f),
        ),
    ),
    words = listOf(
        CutsceneWord(R.string.meal_cutscene_word_steam, x = 286f, y = 146f, atMillis = 520L, tiltDegrees = 8f, scale = 0.65f),
        CutsceneWord(R.string.meal_cutscene_word_hot, x = 96f, y = 214f, atMillis = 930L, tiltDegrees = -10f, scale = 1.3f, ink = CutsceneInk.Hot),
        CutsceneWord(R.string.meal_cutscene_word_blow, x = 148f, y = 412f, atMillis = 1_950L, tiltDegrees = 6f, scale = 0.9f),
        CutsceneWord(R.string.meal_cutscene_word_blow, x = 120f, y = 548f, atMillis = 2_250L, tiltDegrees = -4f, scale = 0.75f),
        CutsceneWord(R.string.meal_ceremony_bite_word, x = 322f, y = 414f, atMillis = 2_800L, tiltDegrees = 10f),
    ),
    finale = tiyotFinale(R.drawable.meal_cutscene_finale_hot, HotRow3, atMillis = 3_300L, expandAtMillis = 4_100L),
)

// 시치미 뚝: 잘 먹겠습니다~(집사 숟가락) / 슬금… | 어? / 시치미 뚝 / 헤헤… 띠욧!!(마지막 칸)
private val SneakyBite = CutsceneSpec(
    backdrop = CutsceneBackdrop.Paper,
    panels = listOf(
        CutscenePanel(R.drawable.meal_cutscene_sneaky_hand, FourRowShapes[0], atMillis = 300L),
        CutscenePanel(R.drawable.meal_cutscene_sneaky_paw, FourRowShapes[1], atMillis = 1_000L),
        CutscenePanel(R.drawable.meal_cutscene_sneaky_empty, FourRowShapes[2], atMillis = 1_700L),
        CutscenePanel(R.drawable.meal_cutscene_sneaky_whistle, FourRowShapes[3], atMillis = 2_400L),
    ),
    words = listOf(
        CutsceneWord(R.string.meal_cutscene_word_lets_eat, x = 220f, y = 170f, atMillis = 450L, tiltDegrees = 4f, scale = 0.8f),
        CutsceneWord(R.string.meal_cutscene_word_sneak, x = 80f, y = 370f, atMillis = 1_150L, tiltDegrees = -6f, scale = 0.85f, ink = CutsceneInk.Sleepy),
        CutsceneWord(R.string.meal_cutscene_word_eh, x = 300f, y = 250f, atMillis = 1_850L, tiltDegrees = 8f),
        CutsceneWord(R.string.meal_cutscene_word_innocent, x = 110f, y = 426f, atMillis = 2_550L, tiltDegrees = -6f, scale = 0.9f),
    ),
    finale = fourRowFinale(
        R.drawable.meal_cutscene_finale_sneaky,
        R.string.meal_cutscene_word_hehe_tiyot,
        atMillis = 3_200L,
        expandAtMillis = 4_000L,
        wordScale = 1f,
        wordFullScale = 1.3f,
    ),
)

// 다이빙 캐치: 앙~ / 앗! | 안 돼애… / 슈퍼 세이브! / 띠욧!!(마지막 칸)
private val DivingCatch = CutsceneSpec(
    backdrop = CutsceneBackdrop.Paper,
    panels = listOf(
        CutscenePanel(R.drawable.meal_cutscene_dive_lift, FourRowShapes[0], atMillis = 300L),
        CutscenePanel(R.drawable.meal_cutscene_dive_slip, FourRowShapes[1], atMillis = 900L),
        CutscenePanel(R.drawable.meal_cutscene_dive_fall, FourRowShapes[2], atMillis = 1_400L),
        CutscenePanel(R.drawable.meal_cutscene_dive_save, FourRowShapes[3], atMillis = 2_200L),
    ),
    words = listOf(
        CutsceneWord(R.string.meal_cutscene_word_aang, x = 290f, y = 170f, atMillis = 450L, tiltDegrees = 8f),
        CutsceneWord(R.string.meal_cutscene_word_oops, x = 80f, y = 250f, atMillis = 1_000L, tiltDegrees = -8f, ink = CutsceneInk.Hot),
        CutsceneWord(R.string.meal_cutscene_word_noo, x = 270f, y = 370f, atMillis = 1_550L, tiltDegrees = 6f, scale = 0.85f, ink = CutsceneInk.Sleepy),
        CutsceneWord(R.string.meal_cutscene_word_super_save, x = 120f, y = 426f, atMillis = 2_280L, tiltDegrees = -6f, ink = CutsceneInk.Sunny),
    ),
    finale = fourRowFinale(
        R.drawable.meal_cutscene_finale_dive,
        R.string.meal_ceremony_reaction_word,
        atMillis = 3_000L,
        expandAtMillis = 3_800L,
    ),
)

// 푸드 파이터: 도전자 등장! / 냐미 | 비둘기 (사이에 VS) / 와구와구! / K.O.! 띠욧!!(마지막 칸)
// 대전 게임 선택 화면처럼 두 얼굴 칸이 잇달아 박히고, 그 경계에 VS 가 찍힌다.
private val FoodFighter = CutsceneSpec(
    backdrop = CutsceneBackdrop.Paper,
    panels = listOf(
        CutscenePanel(R.drawable.meal_cutscene_fighter_arrive, FourRowShapes[0], atMillis = 300L),
        CutscenePanel(R.drawable.meal_cutscene_fighter_nyummy, FourRowShapes[1], atMillis = 1_100L),
        CutscenePanel(R.drawable.meal_cutscene_fighter_pigeon, FourRowShapes[2], atMillis = 1_350L),
        CutscenePanel(R.drawable.meal_cutscene_fighter_gobble, FourRowShapes[3], atMillis = 2_100L),
    ),
    words = listOf(
        CutsceneWord(R.string.meal_cutscene_word_challenger, x = 120f, y = 170f, atMillis = 400L, tiltDegrees = -6f, scale = 0.9f, ink = CutsceneInk.Hot),
        CutsceneWord(R.string.meal_cutscene_word_vs, x = 181f, y = 300f, atMillis = 1_550L, tiltDegrees = 0f, scale = 1.5f, ink = CutsceneInk.Hot),
        CutsceneWord(R.string.meal_cutscene_word_gobble, x = 110f, y = 426f, atMillis = 2_200L, tiltDegrees = -6f),
    ),
    finale = fourRowFinale(
        R.drawable.meal_cutscene_finale_fighter,
        R.string.meal_cutscene_word_ko_tiyot,
        atMillis = 3_000L,
        expandAtMillis = 3_800L,
        wordScale = 1f,
        wordFullScale = 1.3f,
    ),
)

// 먹스타그램: 잠깐! / 각도가… | 찰칵찰칵 / 음, 좋아 / 띠욧!!(마지막 칸)
private val Foodstagram = CutsceneSpec(
    backdrop = CutsceneBackdrop.Paper,
    panels = listOf(
        CutscenePanel(R.drawable.meal_cutscene_gram_wait, FourRowShapes[0], atMillis = 300L),
        CutscenePanel(R.drawable.meal_cutscene_gram_angle, FourRowShapes[1], atMillis = 1_000L),
        CutscenePanel(R.drawable.meal_cutscene_gram_lowshot, FourRowShapes[2], atMillis = 1_600L),
        CutscenePanel(R.drawable.meal_cutscene_gram_check, FourRowShapes[3], atMillis = 2_300L),
    ),
    words = listOf(
        CutsceneWord(R.string.meal_cutscene_word_wait, x = 290f, y = 170f, atMillis = 420L, tiltDegrees = 8f, ink = CutsceneInk.Hot),
        CutsceneWord(R.string.meal_cutscene_word_angle, x = 90f, y = 370f, atMillis = 1_150L, tiltDegrees = -6f, scale = 0.8f, ink = CutsceneInk.Sleepy),
        CutsceneWord(R.string.meal_cutscene_word_click, x = 270f, y = 250f, atMillis = 1_700L, tiltDegrees = 6f, scale = 0.85f),
        CutsceneWord(R.string.meal_cutscene_word_nice, x = 270f, y = 506f, atMillis = 2_500L, tiltDegrees = 4f, scale = 0.85f),
    ),
    finale = fourRowFinale(
        R.drawable.meal_cutscene_finale_gram,
        R.string.meal_ceremony_reaction_word,
        atMillis = 3_100L,
        expandAtMillis = 3_900L,
    ),
)

// 젓가락 대작전: 오늘은 젓가락으로… / 어라? | 휙! / 덥석! / 띠욧!!(마지막 칸)
private val ChopstickChallenge = CutsceneSpec(
    backdrop = CutsceneBackdrop.Paper,
    panels = listOf(
        CutscenePanel(R.drawable.meal_cutscene_chop_vow, FourRowShapes[0], atMillis = 300L),
        CutscenePanel(R.drawable.meal_cutscene_chop_cross, FourRowShapes[1], atMillis = 1_200L),
        CutscenePanel(R.drawable.meal_cutscene_chop_fling, FourRowShapes[2], atMillis = 1_800L),
        CutscenePanel(R.drawable.meal_cutscene_chop_facedive, FourRowShapes[3], atMillis = 2_500L),
    ),
    words = listOf(
        CutsceneWord(R.string.meal_cutscene_word_chopsticks, x = 190f, y = 170f, atMillis = 500L, tiltDegrees = -4f, scale = 0.7f),
        CutsceneWord(R.string.meal_cutscene_word_ehra, x = 90f, y = 250f, atMillis = 1_350L, tiltDegrees = -8f, ink = CutsceneInk.Sleepy),
        CutsceneWord(R.string.meal_cutscene_word_fwip, x = 280f, y = 260f, atMillis = 1_880L, tiltDegrees = 8f, ink = CutsceneInk.Hot),
        CutsceneWord(R.string.meal_cutscene_word_chomp_in, x = 110f, y = 426f, atMillis = 2_560L, tiltDegrees = -6f),
    ),
    finale = fourRowFinale(
        R.drawable.meal_cutscene_finale_chop,
        R.string.meal_ceremony_reaction_word,
        atMillis = 3_200L,
        expandAtMillis = 4_000L,
    ),
)

// 세 칸 + 큰 마지막 칸: 잠 깨는 맛과 우주의 맛이 같이 쓴다.
private val WideRow1 = Seam(236f, 220f)
private val WideRow2 = Seam(436f, 452f)
private val WideRow2Split = Split(184f, 174f)

// 잠 깨는 맛: 꾸벅… / 쏙 | 번쩍! / 개운해!!(마지막 칸)
private val MorningWakeUp = CutsceneSpec(
    backdrop = CutsceneBackdrop.Dawn,
    panels = listOf(
        CutscenePanel(
            R.drawable.meal_cutscene_sleepy,
            CutscenePage.row(Top, WideRow1),
            atMillis = 300L,
            focus = Offset(0.5f, 0.45f),
        ),
        CutscenePanel(
            R.drawable.meal_cutscene_nod,
            CutscenePage.left(WideRow1, WideRow2, WideRow2Split),
            atMillis = 1_350L,
        ),
        CutscenePanel(
            R.drawable.meal_cutscene_awake,
            CutscenePage.right(WideRow1, WideRow2, WideRow2Split),
            atMillis = 2_150L,
        ),
    ),
    words = listOf(
        CutsceneWord(R.string.meal_cutscene_word_nod, x = 84f, y = 206f, atMillis = 800L, scale = 0.75f, ink = CutsceneInk.Sleepy),
        CutsceneWord(R.string.meal_cutscene_word_pop, x = 152f, y = 404f, atMillis = 1_600L, tiltDegrees = 10f, ink = CutsceneInk.Sleepy),
        CutsceneWord(R.string.meal_cutscene_word_awake, x = 294f, y = 268f, atMillis = 2_230L, tiltDegrees = -8f, scale = 1.2f, ink = CutsceneInk.Sunny),
    ),
    finale = CutsceneFinale(
        image = R.drawable.meal_cutscene_finale_sunrise,
        shape = CutscenePage.row(WideRow2, Bottom),
        atMillis = 2_950L,
        expandAtMillis = 3_750L,
        focus = Offset(0.5f, 0.4f),
        word = R.string.meal_cutscene_word_refreshed,
        wordTop = 110f,
        wordTiltDegrees = -5f,
        wordScale = 1f,
        wordInk = CutsceneInk.Sunny,
    ),
    backdropChanges = listOf(CutsceneBackdropChange(atMillis = 2_150L, backdrop = CutsceneBackdrop.Paper)),
)

// 우주의 맛: 앙! / 멈칫! | 둥실~ / 우주의 맛!!(마지막 칸)
private val CosmicTaste = CutsceneSpec(
    backdrop = CutsceneBackdrop.Paper,
    panels = listOf(
        CutscenePanel(R.drawable.meal_cutscene_cosmic_bite, CutscenePage.row(Top, WideRow1), atMillis = 300L, focus = Offset(0.55f, 0.45f)),
        CutscenePanel(
            R.drawable.meal_cutscene_gasp,
            CutscenePage.left(WideRow1, WideRow2, WideRow2Split),
            atMillis = 1_150L,
        ),
        CutscenePanel(
            R.drawable.meal_cutscene_float,
            CutscenePage.right(WideRow1, WideRow2, WideRow2Split),
            atMillis = 2_000L,
        ),
    ),
    words = listOf(
        CutsceneWord(R.string.meal_ceremony_bite_word, x = 312f, y = 190f, atMillis = 460L, tiltDegrees = 10f),
        CutsceneWord(R.string.meal_cutscene_word_freeze, x = 42f, y = 268f, atMillis = 1_350L, scale = 0.9f),
        CutsceneWord(R.string.meal_cutscene_word_float, x = 296f, y = 412f, atMillis = 2_200L, tiltDegrees = 6f, scale = 0.9f),
    ),
    finale = CutsceneFinale(
        image = R.drawable.meal_cutscene_finale_galaxy,
        shape = CutscenePage.row(WideRow2, Bottom),
        atMillis = 2_900L,
        expandAtMillis = 3_700L,
        focus = Offset(0.5f, 0.52f),
        word = R.string.meal_cutscene_word_cosmic,
        wordTop = 120f,
        wordScale = 1f,
        wordFullScale = 1.45f,
    ),
    backdropChanges = listOf(CutsceneBackdropChange(atMillis = 2_000L, backdrop = CutsceneBackdrop.Night)),
)

// ── 아침 끼니 ──

// 이불 밖은 위험해: 따르릉!! / 5분만… | 스윽 / 오물오물 / 이불 박차고 띠욧!!(마지막 칸)
// 이불 속에서 꼼지락대는 동안은 새벽빛 종이에 두고, 박차고 나올 때 낮 종이로 바꾼다.
private val BlanketBurrito = CutsceneSpec(
    backdrop = CutsceneBackdrop.Dawn,
    panels = listOf(
        CutscenePanel(R.drawable.meal_cutscene_blanket_alarm, FourRowShapes[0], atMillis = 300L),
        CutscenePanel(R.drawable.meal_cutscene_blanket_snooze, FourRowShapes[1], atMillis = 1_100L),
        CutscenePanel(R.drawable.meal_cutscene_blanket_drag, FourRowShapes[2], atMillis = 1_700L),
        CutscenePanel(R.drawable.meal_cutscene_blanket_chew, FourRowShapes[3], atMillis = 2_400L),
    ),
    words = listOf(
        CutsceneWord(R.string.meal_cutscene_word_ring, x = 276f, y = 160f, atMillis = 400L, tiltDegrees = 8f, ink = CutsceneInk.Hot),
        CutsceneWord(R.string.meal_cutscene_word_five_more, x = 96f, y = 370f, atMillis = 1_250L, scale = 0.85f, ink = CutsceneInk.Sleepy),
        CutsceneWord(R.string.meal_cutscene_word_slide, x = 290f, y = 250f, atMillis = 1_850L, tiltDegrees = 6f, scale = 0.85f, ink = CutsceneInk.Sleepy),
        CutsceneWord(R.string.meal_cutscene_word_munch, x = 124f, y = 428f, atMillis = 2_550L, scale = 0.85f),
    ),
    finale = fourRowFinale(
        R.drawable.meal_cutscene_finale_blanket,
        R.string.meal_ceremony_reaction_word,
        atMillis = 3_200L,
        expandAtMillis = 4_000L,
    ),
    backdropChanges = listOf(CutsceneBackdropChange(atMillis = 3_200L, backdrop = CutsceneBackdrop.Paper)),
)

// 지각 토스트: 헉!! / 앙! | 다다다다 / 쾅! / 두근!?(마지막 칸, 토스트 문 비둘기)
private val LateToast = CutsceneSpec(
    backdrop = CutsceneBackdrop.Paper,
    panels = listOf(
        CutscenePanel(R.drawable.meal_cutscene_late_clock, FourRowShapes[0], atMillis = 300L),
        CutscenePanel(R.drawable.meal_cutscene_late_toast, FourRowShapes[1], atMillis = 1_000L),
        CutscenePanel(R.drawable.meal_cutscene_late_run, FourRowShapes[2], atMillis = 1_500L),
        CutscenePanel(R.drawable.meal_cutscene_late_crash, FourRowShapes[3], atMillis = 2_200L),
    ),
    words = listOf(
        CutsceneWord(R.string.meal_cutscene_word_gasp, x = 176f, y = 170f, atMillis = 380L, tiltDegrees = -8f, scale = 1.2f, ink = CutsceneInk.Hot),
        CutsceneWord(R.string.meal_ceremony_bite_word, x = 80f, y = 250f, atMillis = 1_120L, tiltDegrees = 10f),
        CutsceneWord(R.string.meal_cutscene_word_dash, x = 270f, y = 366f, atMillis = 1_620L, tiltDegrees = 6f, scale = 0.8f, ink = CutsceneInk.Hot),
        CutsceneWord(R.string.meal_cutscene_word_bonk, x = 110f, y = 426f, atMillis = 2_260L, tiltDegrees = -10f, scale = 1.3f, ink = CutsceneInk.Hot),
    ),
    finale = fourRowFinale(
        R.drawable.meal_cutscene_finale_late,
        R.string.meal_cutscene_word_flutter,
        atMillis = 3_100L,
        expandAtMillis = 3_900L,
        wordInk = CutsceneInk.Hot,
    ),
)

// 우유 콧수염: 꿀꺽꿀꺽 / 푸하~ | 킥킥 / 오호…? / 에헴, 좋은 아침!(마지막 칸)
// 거울 보고 만족하는 한 박자를 둔다.
private val MilkMustache = CutsceneSpec(
    backdrop = CutsceneBackdrop.Paper,
    panels = listOf(
        CutscenePanel(R.drawable.meal_cutscene_milk_gulp, FourRowShapes[0], atMillis = 300L),
        CutscenePanel(R.drawable.meal_cutscene_milk_puha, FourRowShapes[1], atMillis = 1_000L),
        CutscenePanel(R.drawable.meal_cutscene_milk_mirror, FourRowShapes[2], atMillis = 1_600L),
        CutscenePanel(R.drawable.meal_cutscene_milk_admire, FourRowShapes[3], atMillis = 2_300L),
    ),
    words = listOf(
        CutsceneWord(R.string.meal_cutscene_word_gulp_gulp, x = 270f, y = 170f, atMillis = 450L, tiltDegrees = 6f, scale = 0.8f),
        CutsceneWord(R.string.meal_cutscene_word_ahh, x = 96f, y = 370f, atMillis = 1_150L, tiltDegrees = -6f),
        CutsceneWord(R.string.meal_cutscene_word_giggle, x = 290f, y = 250f, atMillis = 1_750L, tiltDegrees = 8f, scale = 0.85f, ink = CutsceneInk.Sleepy),
        CutsceneWord(R.string.meal_cutscene_word_oho, x = 124f, y = 428f, atMillis = 2_500L, scale = 0.9f),
    ),
    finale = fourRowFinale(
        R.drawable.meal_cutscene_finale_milk,
        R.string.meal_cutscene_word_gentleman,
        atMillis = 3_200L,
        expandAtMillis = 4_000L,
        wordScale = 0.85f,
        wordFullScale = 1.1f,
        wordInk = CutsceneInk.Sunny,
    ),
)

// 시리얼 대홍수: 촤르르 / 콸콸콸 | 어? / 둥둥 / 숟가락 서핑 띠욧!!(마지막 칸)
private val CerealFlood = CutsceneSpec(
    backdrop = CutsceneBackdrop.Paper,
    panels = listOf(
        CutscenePanel(R.drawable.meal_cutscene_cereal_pour, FourRowShapes[0], atMillis = 300L),
        CutscenePanel(R.drawable.meal_cutscene_cereal_milk, FourRowShapes[1], atMillis = 1_000L),
        CutscenePanel(R.drawable.meal_cutscene_cereal_overflow, FourRowShapes[2], atMillis = 1_700L),
        CutscenePanel(R.drawable.meal_cutscene_cereal_sea, FourRowShapes[3], atMillis = 2_400L),
    ),
    words = listOf(
        CutsceneWord(R.string.meal_cutscene_word_rattle, x = 290f, y = 170f, atMillis = 450L, tiltDegrees = 6f, scale = 0.85f, ink = CutsceneInk.Sleepy),
        CutsceneWord(R.string.meal_cutscene_word_glug, x = 96f, y = 370f, atMillis = 1_150L, scale = 0.85f),
        CutsceneWord(R.string.meal_cutscene_word_eh, x = 290f, y = 250f, atMillis = 1_780L, tiltDegrees = 8f, scale = 1.1f, ink = CutsceneInk.Hot),
        CutsceneWord(R.string.meal_cutscene_word_bob, x = 124f, y = 428f, atMillis = 2_600L, scale = 0.9f, ink = CutsceneInk.Sleepy),
    ),
    finale = fourRowFinale(
        R.drawable.meal_cutscene_finale_cereal,
        R.string.meal_ceremony_reaction_word,
        atMillis = 3_200L,
        expandAtMillis = 4_000L,
    ),
)

// 식빵 굽는 아침: 따스~ / 꾹꾹 | 팅! / 갸웃? / 진짜 식빵 물고 띠욧!!(마지막 칸)
// 햇살 아래 나른한 두 칸은 새벽빛 종이에 두고, 토스터가 튀어 오를 때 낮 종이로 바꾼다.
private val LoafMorning = CutsceneSpec(
    backdrop = CutsceneBackdrop.Dawn,
    panels = listOf(
        CutscenePanel(R.drawable.meal_cutscene_loaf_sun, FourRowShapes[0], atMillis = 300L),
        CutscenePanel(R.drawable.meal_cutscene_loaf_knead, FourRowShapes[1], atMillis = 1_100L),
        CutscenePanel(R.drawable.meal_cutscene_loaf_ting, FourRowShapes[2], atMillis = 1_800L),
        CutscenePanel(R.drawable.meal_cutscene_loaf_twins, FourRowShapes[3], atMillis = 2_500L),
    ),
    words = listOf(
        CutsceneWord(R.string.meal_cutscene_word_warm, x = 290f, y = 170f, atMillis = 500L, tiltDegrees = 6f, scale = 0.85f, ink = CutsceneInk.Sunny),
        CutsceneWord(R.string.meal_cutscene_word_knead, x = 96f, y = 370f, atMillis = 1_300L, scale = 0.85f),
        CutsceneWord(R.string.meal_cutscene_word_ting, x = 290f, y = 250f, atMillis = 1_860L, tiltDegrees = 10f, scale = 1.1f, ink = CutsceneInk.Hot),
        CutsceneWord(R.string.meal_cutscene_word_tilt, x = 124f, y = 428f, atMillis = 2_700L, tiltDegrees = -8f, scale = 0.9f, ink = CutsceneInk.Sleepy),
    ),
    finale = fourRowFinale(
        R.drawable.meal_cutscene_finale_loaf,
        R.string.meal_ceremony_reaction_word,
        atMillis = 3_300L,
        expandAtMillis = 4_100L,
    ),
    backdropChanges = listOf(CutsceneBackdropChange(atMillis = 1_800L, backdrop = CutsceneBackdrop.Paper)),
)

// ── 야식 ── 페이지는 밤 종이에 깐다.

// 한밤의 공범: 살금살금 / 끼익… | 헉! / 쉿! / 집사와 공범 띠욧!!(마지막 칸)
private val MidnightAccomplice = CutsceneSpec(
    backdrop = CutsceneBackdrop.Night,
    panels = listOf(
        CutscenePanel(R.drawable.meal_cutscene_night_tiptoe, FourRowShapes[0], atMillis = 300L),
        CutscenePanel(R.drawable.meal_cutscene_night_fridge, FourRowShapes[1], atMillis = 1_100L),
        CutscenePanel(R.drawable.meal_cutscene_night_owner, FourRowShapes[2], atMillis = 1_800L),
        CutscenePanel(R.drawable.meal_cutscene_night_shush, FourRowShapes[3], atMillis = 2_500L),
    ),
    words = listOf(
        CutsceneWord(R.string.meal_cutscene_word_tiptoe, x = 124f, y = 170f, atMillis = 500L, scale = 0.8f, ink = CutsceneInk.Sleepy),
        CutsceneWord(R.string.meal_cutscene_word_creak, x = 96f, y = 370f, atMillis = 1_300L, tiltDegrees = -4f, scale = 0.85f, ink = CutsceneInk.Sleepy),
        CutsceneWord(R.string.meal_cutscene_word_gasp_short, x = 290f, y = 250f, atMillis = 1_860L, tiltDegrees = 8f, scale = 1.1f, ink = CutsceneInk.Hot),
        CutsceneWord(R.string.meal_cutscene_word_shush, x = 124f, y = 428f, atMillis = 2_700L, tiltDegrees = -4f),
    ),
    finale = fourRowFinale(
        R.drawable.meal_cutscene_finale_night,
        R.string.meal_cutscene_word_accomplice_tiyot,
        atMillis = 3_300L,
        expandAtMillis = 4_100L,
        wordScale = 1f,
        wordFullScale = 1.3f,
    ),
)

// 3분이 제일 길다: 보글보글 / 째깍… | 째깍째깍 / 덥석! / 냄비 뚜껑 후루룩 띠욧!!(마지막 칸)
// 기다리는 두 칸은 길게 둔다.
private val RamenCountdown = CutsceneSpec(
    backdrop = CutsceneBackdrop.Night,
    panels = listOf(
        CutscenePanel(R.drawable.meal_cutscene_ramen_boil, FourRowShapes[0], atMillis = 300L),
        CutscenePanel(R.drawable.meal_cutscene_ramen_wait, FourRowShapes[1], atMillis = 1_000L),
        CutscenePanel(R.drawable.meal_cutscene_ramen_dizzy, FourRowShapes[2], atMillis = 1_800L),
        CutscenePanel(R.drawable.meal_cutscene_ramen_lid, FourRowShapes[3], atMillis = 2_500L),
    ),
    words = listOf(
        CutsceneWord(R.string.meal_cutscene_word_bubble, x = 280f, y = 170f, atMillis = 450L, tiltDegrees = 6f, scale = 0.8f),
        CutsceneWord(R.string.meal_cutscene_word_tick, x = 96f, y = 370f, atMillis = 1_200L, scale = 0.85f, ink = CutsceneInk.Sleepy),
        CutsceneWord(R.string.meal_cutscene_word_tick_tock, x = 272f, y = 250f, atMillis = 2_000L, tiltDegrees = 6f, scale = 0.8f, ink = CutsceneInk.Sleepy),
        CutsceneWord(R.string.meal_cutscene_word_chomp_in, x = 124f, y = 428f, atMillis = 2_560L, tiltDegrees = -8f, ink = CutsceneInk.Hot),
    ),
    finale = fourRowFinale(
        R.drawable.meal_cutscene_finale_ramen,
        R.string.meal_cutscene_word_slurp_tiyot,
        atMillis = 3_200L,
        expandAtMillis = 4_000L,
        wordScale = 1f,
        wordFullScale = 1.3f,
    ),
)

// 배달 기다리는 귀: 주문 완료! / 부릉? | 아니네… / 띵동!! / 상자 끌어안고 띠욧!!(마지막 칸)
private val DeliveryEars = CutsceneSpec(
    backdrop = CutsceneBackdrop.Night,
    panels = listOf(
        CutscenePanel(R.drawable.meal_cutscene_delivery_order, FourRowShapes[0], atMillis = 300L),
        CutscenePanel(R.drawable.meal_cutscene_delivery_vroom, FourRowShapes[1], atMillis = 1_000L),
        CutscenePanel(R.drawable.meal_cutscene_delivery_nope, FourRowShapes[2], atMillis = 1_700L),
        CutscenePanel(R.drawable.meal_cutscene_delivery_ding, FourRowShapes[3], atMillis = 2_500L),
    ),
    words = listOf(
        CutsceneWord(R.string.meal_cutscene_word_ordered, x = 262f, y = 170f, atMillis = 450L, tiltDegrees = 4f, scale = 0.8f),
        CutsceneWord(R.string.meal_cutscene_word_vroom, x = 96f, y = 370f, atMillis = 1_150L, tiltDegrees = -8f, scale = 0.9f, ink = CutsceneInk.Hot),
        CutsceneWord(R.string.meal_cutscene_word_not_it, x = 272f, y = 366f, atMillis = 1_900L, tiltDegrees = 4f, scale = 0.85f, ink = CutsceneInk.Sleepy),
        CutsceneWord(R.string.meal_cutscene_word_ding_dong, x = 124f, y = 428f, atMillis = 2_560L, tiltDegrees = -8f, scale = 1.1f, ink = CutsceneInk.Hot),
    ),
    finale = fourRowFinale(
        R.drawable.meal_cutscene_finale_delivery,
        R.string.meal_ceremony_reaction_word,
        atMillis = 3_200L,
        expandAtMillis = 4_000L,
    ),
)

// 한밤의 ASMR: 톡톡 / (소곤) 먹어볼게요… | 와사삭!!! / 딸깍, 딸깍 / (소곤) 띠욧…(마지막 칸)
// 속삭이는 글자는 일부러 작게, 와사삭만 크게 찍어 크기 차이로 웃긴다. 마지막 띠욧도 작게 둔다.
private val MidnightAsmr = CutsceneSpec(
    backdrop = CutsceneBackdrop.Night,
    panels = listOf(
        CutscenePanel(R.drawable.meal_cutscene_asmr_mic, FourRowShapes[0], atMillis = 300L),
        CutscenePanel(R.drawable.meal_cutscene_asmr_whisper, FourRowShapes[1], atMillis = 1_000L),
        CutscenePanel(R.drawable.meal_cutscene_asmr_crunch, FourRowShapes[2], atMillis = 1_900L),
        CutscenePanel(R.drawable.meal_cutscene_asmr_neighbors, FourRowShapes[3], atMillis = 2_600L),
    ),
    words = listOf(
        CutsceneWord(R.string.meal_cutscene_word_tap, x = 290f, y = 170f, atMillis = 450L, tiltDegrees = 4f, scale = 0.75f),
        CutsceneWord(R.string.meal_cutscene_word_whisper, x = 96f, y = 370f, atMillis = 1_200L, tiltDegrees = -2f, scale = 0.48f, ink = CutsceneInk.Sleepy),
        CutsceneWord(R.string.meal_cutscene_word_crunch, x = 264f, y = 260f, atMillis = 1_950L, tiltDegrees = 8f, scale = 1.1f, ink = CutsceneInk.Hot),
        CutsceneWord(R.string.meal_cutscene_word_click_click, x = 124f, y = 428f, atMillis = 2_800L, scale = 0.8f, ink = CutsceneInk.Sunny),
    ),
    finale = fourRowFinale(
        R.drawable.meal_cutscene_finale_asmr,
        R.string.meal_cutscene_word_whisper_tiyot,
        atMillis = 3_400L,
        expandAtMillis = 4_200L,
        wordScale = 0.55f,
        wordFullScale = 0.7f,
        wordInk = CutsceneInk.Sleepy,
    ),
)

// 편의점 삼각김밥: 딩동~ / 1, 2, 3! | 찌익… / 멍… / 그래도 띠욧!!(마지막 칸)
private val ConvenienceStore = CutsceneSpec(
    backdrop = CutsceneBackdrop.Night,
    panels = listOf(
        CutscenePanel(R.drawable.meal_cutscene_store_door, FourRowShapes[0], atMillis = 300L),
        CutscenePanel(R.drawable.meal_cutscene_store_steps, FourRowShapes[1], atMillis = 1_000L),
        CutscenePanel(R.drawable.meal_cutscene_store_rip, FourRowShapes[2], atMillis = 1_700L),
        CutscenePanel(R.drawable.meal_cutscene_store_blank, FourRowShapes[3], atMillis = 2_400L),
    ),
    words = listOf(
        CutsceneWord(R.string.meal_cutscene_word_store_chime, x = 290f, y = 170f, atMillis = 450L, tiltDegrees = 6f, scale = 0.85f),
        CutsceneWord(R.string.meal_cutscene_word_one_two_three, x = 96f, y = 370f, atMillis = 1_150L, tiltDegrees = -4f, scale = 0.85f),
        CutsceneWord(R.string.meal_cutscene_word_rip, x = 280f, y = 250f, atMillis = 1_800L, tiltDegrees = 8f, scale = 0.9f, ink = CutsceneInk.Hot),
        CutsceneWord(R.string.meal_cutscene_word_blank, x = 124f, y = 428f, atMillis = 2_650L, ink = CutsceneInk.Sleepy),
    ),
    finale = fourRowFinale(
        R.drawable.meal_cutscene_finale_store,
        R.string.meal_cutscene_word_still_tiyot,
        atMillis = 3_300L,
        expandAtMillis = 4_100L,
        wordScale = 1f,
        wordFullScale = 1.3f,
    ),
)

// ── 희귀 ── 평범한 한 입으로 시작했다가 뒤집힌다.

// 거대 냐미 출현: 앙! / 어라? | 쿵! / 냐미다!! / 빌딩 사이에서 띠욧!!!(마지막 칸)
private val GiantNyummy = CutsceneSpec(
    backdrop = CutsceneBackdrop.Paper,
    panels = listOf(
        CutscenePanel(R.drawable.meal_cutscene_giant_bite, FourRowShapes[0], atMillis = 300L, focus = Offset(0.55f, 0.45f)),
        CutscenePanel(R.drawable.meal_cutscene_giant_grow, FourRowShapes[1], atMillis = 1_100L),
        CutscenePanel(R.drawable.meal_cutscene_giant_ceiling, FourRowShapes[2], atMillis = 1_800L),
        CutscenePanel(R.drawable.meal_cutscene_giant_street, FourRowShapes[3], atMillis = 2_500L),
    ),
    words = listOf(
        CutsceneWord(R.string.meal_ceremony_bite_word, x = 300f, y = 170f, atMillis = 460L, tiltDegrees = 10f),
        CutsceneWord(R.string.meal_cutscene_word_ehra, x = 96f, y = 370f, atMillis = 1_300L, tiltDegrees = -8f, ink = CutsceneInk.Sleepy),
        CutsceneWord(R.string.meal_cutscene_word_thud, x = 290f, y = 250f, atMillis = 1_860L, tiltDegrees = 8f, scale = 1.2f, ink = CutsceneInk.Hot),
        CutsceneWord(R.string.meal_cutscene_word_its_nyummy, x = 124f, y = 428f, atMillis = 2_580L, tiltDegrees = -6f, ink = CutsceneInk.Hot),
    ),
    finale = fourRowFinale(
        R.drawable.meal_cutscene_finale_giant,
        R.string.meal_cutscene_word_giant_tiyot,
        atMillis = 3_300L,
        expandAtMillis = 4_100L,
        wordScale = 1.4f,
        wordFullScale = 2f,
    ),
)

// 복냐미: 앙! / 까딱? | 까딱까딱 / 짤랑짤랑 / 복 들어왔다냥!!(마지막 칸)
private val LuckyNyummy = CutsceneSpec(
    backdrop = CutsceneBackdrop.Paper,
    panels = listOf(
        CutscenePanel(R.drawable.meal_cutscene_lucky_bite, FourRowShapes[0], atMillis = 300L, focus = Offset(0.55f, 0.45f)),
        CutscenePanel(R.drawable.meal_cutscene_lucky_paw, FourRowShapes[1], atMillis = 1_100L),
        CutscenePanel(R.drawable.meal_cutscene_lucky_bell, FourRowShapes[2], atMillis = 1_700L),
        CutscenePanel(R.drawable.meal_cutscene_lucky_rain, FourRowShapes[3], atMillis = 2_400L),
    ),
    words = listOf(
        CutsceneWord(R.string.meal_ceremony_bite_word, x = 300f, y = 170f, atMillis = 460L, tiltDegrees = 10f),
        CutsceneWord(R.string.meal_cutscene_word_flick, x = 96f, y = 370f, atMillis = 1_300L, tiltDegrees = -6f, ink = CutsceneInk.Sleepy),
        CutsceneWord(R.string.meal_cutscene_word_beckon, x = 272f, y = 250f, atMillis = 1_850L, tiltDegrees = 6f, scale = 0.8f),
        CutsceneWord(R.string.meal_cutscene_word_jingle, x = 124f, y = 428f, atMillis = 2_500L, tiltDegrees = -6f, scale = 0.9f, ink = CutsceneInk.Sunny),
    ),
    finale = fourRowFinale(
        R.drawable.meal_cutscene_finale_lucky,
        R.string.meal_cutscene_word_lucky,
        atMillis = 3_200L,
        expandAtMillis = 4_000L,
        wordScale = 0.9f,
        wordFullScale = 1.15f,
        wordInk = CutsceneInk.Sunny,
    ),
)

// 냐미 올스타: 앙~ / 띵동!! | 나도 나도! / 하나, 둘… / 단체 사진 다같이 띠욧!!(마지막 칸)
// 길냥이 습격과 같은 첫 칸으로 시작해, 아는 사람은 한 번 더 속는다.
private val AllStars = CutsceneSpec(
    backdrop = CutsceneBackdrop.Paper,
    panels = listOf(
        CutscenePanel(R.drawable.meal_cutscene_stray_lift, FourRowShapes[0], atMillis = 300L),
        CutscenePanel(R.drawable.meal_cutscene_allstar_door, FourRowShapes[1], atMillis = 1_100L),
        CutscenePanel(R.drawable.meal_cutscene_allstar_selfie, FourRowShapes[2], atMillis = 1_800L),
        CutscenePanel(R.drawable.meal_cutscene_allstar_count, FourRowShapes[3], atMillis = 2_500L),
    ),
    words = listOf(
        CutsceneWord(R.string.meal_cutscene_word_aang, x = 292f, y = 170f, atMillis = 460L, tiltDegrees = 8f),
        CutsceneWord(R.string.meal_cutscene_word_ding_dong, x = 96f, y = 370f, atMillis = 1_200L, tiltDegrees = -8f, ink = CutsceneInk.Hot),
        CutsceneWord(R.string.meal_cutscene_word_me_too, x = 272f, y = 366f, atMillis = 1_900L, tiltDegrees = 6f, scale = 0.75f, ink = CutsceneInk.Hot),
        CutsceneWord(R.string.meal_cutscene_word_one_two, x = 124f, y = 428f, atMillis = 2_700L, scale = 0.85f, ink = CutsceneInk.Sleepy),
    ),
    finale = fourRowFinale(
        R.drawable.meal_cutscene_finale_allstar,
        R.string.meal_cutscene_word_tiyot_together,
        atMillis = 3_400L,
        expandAtMillis = 4_200L,
        wordScale = 1f,
        wordFullScale = 1.3f,
    ),
)
