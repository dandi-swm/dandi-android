package com.dandi.nyummy.meal.presentation.component.ceremony

import com.dandi.nyummy.common.presentation.cutscene.CutsceneBackdrop
import com.dandi.nyummy.common.presentation.cutscene.CutsceneInk
import com.dandi.nyummy.common.presentation.cutscene.CutscenePanel
import com.dandi.nyummy.common.presentation.cutscene.CutsceneSpec
import com.dandi.nyummy.common.presentation.cutscene.CutsceneWord
import com.dandi.nyummy.meal.presentation.R

/*
 * 네 줄 배치만 쓰는 컷신.
 * 칸 그림은 3:2 / 1:1 / 1:1 / 3:2, 마지막 칸은 2:3 이고 칸이 박히는 시각은 모두 같다. 편마다 다른 것은 그림, 글자, 글자 자리뿐이다.
 */

/** 네 줄 배치 컷신 한 편. [images] 는 칸 네 개 순서, [words] 는 칸마다 한 글자다. */
private fun fourRowCutscene(
    backdrop: CutsceneBackdrop,
    images: List<Int>,
    words: List<CutsceneWord>,
    finaleImage: Int,
    finaleWord: Int,
    longFinaleWord: Boolean,
) = CutsceneSpec(
    backdrop = backdrop,
    panels = images.mapIndexed { index, image -> CutscenePanel(image, FourRowShapes[index], atMillis = PanelMillis[index]) },
    words = words,
    finale = fourRowFinale(
        finaleImage,
        finaleWord,
        atMillis = 3_100L,
        expandAtMillis = 3_900L,
        wordScale = if (longFinaleWord) 1f else 1.2f,
        wordFullScale = if (longFinaleWord) 1.3f else 1.6f,
    ),
)

/** 칸 네 개가 박히는 시각. 글자는 칸보다 [WordDelayMillis] 늦게 찍힌다. */
private val PanelMillis = listOf(300L, 1_000L, 1_600L, 2_300L)
private const val WordDelayMillis = 160L

/** [panel] 번째(1~4) 칸에 찍는 글자. */
private fun word(panel: Int, text: Int, x: Float, y: Float, tilt: Float, scale: Float, ink: CutsceneInk = CutsceneInk.Brand) =
    CutsceneWord(text, x = x, y = y, atMillis = PanelMillis[panel - 1] + WordDelayMillis, tiltDegrees = tilt, scale = scale, ink = ink)

// 메뉴 룰렛: 뭐 먹지… / 돌려돌려 / 덜컹! / 또 이거?! / 그래도 띠욧!!(마지막 칸)
internal val MenuRoulette = fourRowCutscene(
    backdrop = CutsceneBackdrop.Paper,
    images = listOf(R.drawable.meal_cutscene_roulette_1, R.drawable.meal_cutscene_roulette_2, R.drawable.meal_cutscene_roulette_3, R.drawable.meal_cutscene_roulette_4),
    words = listOf(
        word(1, R.string.meal_cutscene_word_roulette_1, x = 268f, y = 172f, tilt = 6f, scale = 0.75f, ink = CutsceneInk.Sleepy),
        word(2, R.string.meal_cutscene_word_roulette_2, x = 94f, y = 368f, tilt = -6f, scale = 0.85f),
        word(3, R.string.meal_cutscene_word_roulette_3, x = 285f, y = 378f, tilt = 6f, scale = 0.85f),
        word(4, R.string.meal_cutscene_word_roulette_4, x = 114f, y = 428f, tilt = -6f, scale = 0.75f, ink = CutsceneInk.Hot),
    ),
    finaleImage = R.drawable.meal_cutscene_finale_roulette,
    finaleWord = R.string.meal_cutscene_word_still_tiyot,
    longFinaleWord = true,
)

// 맛집 웨이팅: 줄이 길다… / 꼬르륵 / 흥! / 다음 손님~! / 입장 띠욧!!(마지막 칸)
internal val LunchLine = fourRowCutscene(
    backdrop = CutsceneBackdrop.Paper,
    images = listOf(R.drawable.meal_cutscene_queue_1, R.drawable.meal_cutscene_queue_2, R.drawable.meal_cutscene_queue_3, R.drawable.meal_cutscene_queue_4),
    words = listOf(
        word(1, R.string.meal_cutscene_word_queue_1, x = 190f, y = 34f, tilt = 4f, scale = 0.75f, ink = CutsceneInk.Sleepy),
        word(2, R.string.meal_cutscene_word_queue_2, x = 94f, y = 368f, tilt = -6f, scale = 0.85f),
        word(3, R.string.meal_cutscene_word_queue_3, x = 240f, y = 374f, tilt = -6f, scale = 0.85f),
        word(4, R.string.meal_cutscene_word_queue_4, x = 114f, y = 428f, tilt = -6f, scale = 0.75f),
    ),
    finaleImage = R.drawable.meal_cutscene_finale_queue,
    finaleWord = R.string.meal_cutscene_word_queue_finale,
    longFinaleWord = true,
)

// 도시락 뚜껑: 두근… / 딸깍 / 빼꼼 / 와아! / 띠욧!!(마지막 칸)
internal val LunchboxReveal = fourRowCutscene(
    backdrop = CutsceneBackdrop.Paper,
    images = listOf(R.drawable.meal_cutscene_bento_1, R.drawable.meal_cutscene_bento_2, R.drawable.meal_cutscene_bento_3, R.drawable.meal_cutscene_bento_4),
    words = listOf(
        word(1, R.string.meal_cutscene_word_bento_1, x = 288f, y = 172f, tilt = 6f, scale = 0.85f, ink = CutsceneInk.Sleepy),
        word(2, R.string.meal_cutscene_word_bento_2, x = 94f, y = 368f, tilt = -6f, scale = 1.0f),
        word(3, R.string.meal_cutscene_word_bento_3, x = 285f, y = 380f, tilt = 6f, scale = 1.0f),
        word(4, R.string.meal_cutscene_word_bento_4, x = 114f, y = 428f, tilt = -6f, scale = 1.0f),
    ),
    finaleImage = R.drawable.meal_cutscene_finale_bento,
    finaleWord = R.string.meal_ceremony_reaction_word,
    longFinaleWord = false,
)

// 식곤증: 냠냠 / 배불러~ / 꾸벅… / 쿨… / 꿈에서도 띠욧…(마지막 칸)
internal val FoodComa = fourRowCutscene(
    backdrop = CutsceneBackdrop.Paper,
    images = listOf(R.drawable.meal_cutscene_coma_1, R.drawable.meal_cutscene_coma_2, R.drawable.meal_cutscene_coma_3, R.drawable.meal_cutscene_coma_4),
    words = listOf(
        word(1, R.string.meal_cutscene_word_coma_1, x = 288f, y = 172f, tilt = 6f, scale = 0.85f),
        word(2, R.string.meal_cutscene_word_coma_2, x = 94f, y = 368f, tilt = -6f, scale = 0.65f),
        word(3, R.string.meal_cutscene_word_nod, x = 258f, y = 380f, tilt = 6f, scale = 1.0f, ink = CutsceneInk.Sleepy),
        word(4, R.string.meal_cutscene_word_coma_4, x = 114f, y = 428f, tilt = -6f, scale = 1.0f, ink = CutsceneInk.Sleepy),
    ),
    finaleImage = R.drawable.meal_cutscene_finale_coma,
    finaleWord = R.string.meal_cutscene_word_coma_finale,
    longFinaleWord = true,
)

// 국물 한 방울: 후루룩 / 톡! / 헉! / 슥슥슥 / 안심하고 띠욧!!(마지막 칸)
internal val SoupSplash = fourRowCutscene(
    backdrop = CutsceneBackdrop.Paper,
    images = listOf(R.drawable.meal_cutscene_splash_1, R.drawable.meal_cutscene_splash_2, R.drawable.meal_cutscene_splash_3, R.drawable.meal_cutscene_splash_4),
    words = listOf(
        word(1, R.string.meal_cutscene_word_splash_1, x = 288f, y = 172f, tilt = 6f, scale = 0.85f),
        word(2, R.string.meal_cutscene_word_splash_2, x = 94f, y = 368f, tilt = -6f, scale = 0.85f),
        word(3, R.string.meal_cutscene_word_gasp_short, x = 300f, y = 380f, tilt = 6f, scale = 1.0f),
        word(4, R.string.meal_cutscene_word_splash_4, x = 114f, y = 428f, tilt = -6f, scale = 0.85f),
    ),
    finaleImage = R.drawable.meal_cutscene_finale_splash,
    finaleWord = R.string.meal_cutscene_word_splash_finale,
    longFinaleWord = true,
)

// 김밥 꽁다리: 하나 / 또 하나 / 어? 꽁다리 / 이게 진짜지! / 꽁다리 띠욧!!(마지막 칸)
internal val GimbapEnd = fourRowCutscene(
    backdrop = CutsceneBackdrop.Paper,
    images = listOf(R.drawable.meal_cutscene_gimbap_1, R.drawable.meal_cutscene_gimbap_2, R.drawable.meal_cutscene_gimbap_3, R.drawable.meal_cutscene_gimbap_4),
    words = listOf(
        word(1, R.string.meal_cutscene_word_gimbap_1, x = 288f, y = 172f, tilt = 6f, scale = 1.0f),
        word(2, R.string.meal_cutscene_word_gimbap_2, x = 94f, y = 368f, tilt = -6f, scale = 0.85f),
        word(3, R.string.meal_cutscene_word_gimbap_3, x = 272f, y = 248f, tilt = 6f, scale = 0.65f),
        word(4, R.string.meal_cutscene_word_gimbap_4, x = 268f, y = 545f, tilt = 4f, scale = 0.75f),
    ),
    finaleImage = R.drawable.meal_cutscene_finale_gimbap,
    finaleWord = R.string.meal_cutscene_word_gimbap_finale,
    longFinaleWord = true,
)

// 샐러드의 반전: 풀… / 풀이네… / 어? 뭐가 있다 / 닭가슴살이다! / 맛있잖아 띠욧!!(마지막 칸)
internal val SaladTwist = fourRowCutscene(
    backdrop = CutsceneBackdrop.Paper,
    images = listOf(R.drawable.meal_cutscene_salad_1, R.drawable.meal_cutscene_salad_2, R.drawable.meal_cutscene_salad_3, R.drawable.meal_cutscene_salad_4),
    words = listOf(
        word(1, R.string.meal_cutscene_word_salad_1, x = 288f, y = 172f, tilt = 6f, scale = 0.85f, ink = CutsceneInk.Sleepy),
        word(2, R.string.meal_cutscene_word_salad_2, x = 94f, y = 368f, tilt = -6f, scale = 0.75f, ink = CutsceneInk.Sleepy),
        word(3, R.string.meal_cutscene_word_salad_3, x = 272f, y = 248f, tilt = 6f, scale = 0.65f),
        word(4, R.string.meal_cutscene_word_salad_4, x = 114f, y = 428f, tilt = -6f, scale = 0.75f),
    ),
    finaleImage = R.drawable.meal_cutscene_finale_salad,
    finaleWord = R.string.meal_cutscene_word_salad_finale,
    longFinaleWord = true,
)

// 회전초밥: 빙글빙글 / 저거다! / 앗, 놓쳤다 / 한 바퀴만 더… / 잡았다 띠욧!!(마지막 칸)
internal val SushiBelt = fourRowCutscene(
    backdrop = CutsceneBackdrop.Paper,
    images = listOf(R.drawable.meal_cutscene_sushi_1, R.drawable.meal_cutscene_sushi_2, R.drawable.meal_cutscene_sushi_3, R.drawable.meal_cutscene_sushi_4),
    words = listOf(
        word(1, R.string.meal_cutscene_word_sushi_1, x = 288f, y = 172f, tilt = 6f, scale = 0.75f),
        word(2, R.string.meal_cutscene_word_sushi_2, x = 94f, y = 368f, tilt = -6f, scale = 0.85f),
        word(3, R.string.meal_cutscene_word_sushi_3, x = 262f, y = 382f, tilt = 4f, scale = 0.75f),
        word(4, R.string.meal_cutscene_word_sushi_4, x = 114f, y = 428f, tilt = -6f, scale = 0.65f, ink = CutsceneInk.Sleepy),
    ),
    finaleImage = R.drawable.meal_cutscene_finale_sushi,
    finaleWord = R.string.meal_cutscene_word_sushi_finale,
    longFinaleWord = true,
)

// 퇴근 마중: 째깍… / 쫑긋! / 다다다다 / 왔다!! / 같이 띠욧!!(마지막 칸)
internal val WelcomeHome = fourRowCutscene(
    backdrop = CutsceneBackdrop.Paper,
    images = listOf(R.drawable.meal_cutscene_welcome_1, R.drawable.meal_cutscene_welcome_2, R.drawable.meal_cutscene_welcome_3, R.drawable.meal_cutscene_welcome_4),
    words = listOf(
        word(1, R.string.meal_cutscene_word_tick, x = 262f, y = 172f, tilt = 6f, scale = 0.85f, ink = CutsceneInk.Sleepy),
        word(2, R.string.meal_cutscene_word_welcome_2, x = 94f, y = 368f, tilt = -6f, scale = 0.85f),
        word(3, R.string.meal_cutscene_word_dash, x = 272f, y = 248f, tilt = 6f, scale = 0.85f),
        word(4, R.string.meal_cutscene_word_welcome_4, x = 114f, y = 428f, tilt = -6f, scale = 0.75f, ink = CutsceneInk.Hot),
    ),
    finaleImage = R.drawable.meal_cutscene_finale_welcome,
    finaleWord = R.string.meal_cutscene_word_welcome_finale,
    longFinaleWord = true,
)

// 고기 굽는 날: 치이익 / 아직… / 아직…! / 지금이야! / 쌈 띠욧!!(마지막 칸)
internal val GrillDay = fourRowCutscene(
    backdrop = CutsceneBackdrop.Paper,
    images = listOf(R.drawable.meal_cutscene_grill_1, R.drawable.meal_cutscene_grill_2, R.drawable.meal_cutscene_grill_3, R.drawable.meal_cutscene_grill_4),
    words = listOf(
        word(1, R.string.meal_cutscene_word_grill_1, x = 288f, y = 172f, tilt = 6f, scale = 1.0f),
        word(2, R.string.meal_cutscene_word_grill_2, x = 94f, y = 368f, tilt = -6f, scale = 0.85f, ink = CutsceneInk.Sleepy),
        word(3, R.string.meal_cutscene_word_grill_3, x = 272f, y = 382f, tilt = 6f, scale = 0.85f, ink = CutsceneInk.Sleepy),
        word(4, R.string.meal_cutscene_word_grill_4, x = 114f, y = 428f, tilt = -6f, scale = 0.85f),
    ),
    finaleImage = R.drawable.meal_cutscene_finale_grill,
    finaleWord = R.string.meal_cutscene_word_grill_finale,
    longFinaleWord = true,
)

// 치즈 쭈욱: 쭈욱 / 쭈우욱 / 쭈우우욱 / 어디까지…? / 띠욧!!(마지막 칸)
internal val CheesePull = fourRowCutscene(
    backdrop = CutsceneBackdrop.Paper,
    images = listOf(R.drawable.meal_cutscene_cheese_1, R.drawable.meal_cutscene_cheese_2, R.drawable.meal_cutscene_cheese_3, R.drawable.meal_cutscene_cheese_4),
    words = listOf(
        word(1, R.string.meal_cutscene_word_cheese_1, x = 288f, y = 172f, tilt = 6f, scale = 0.85f),
        word(2, R.string.meal_cutscene_word_cheese_2, x = 94f, y = 368f, tilt = -6f, scale = 0.85f),
        word(3, R.string.meal_cutscene_word_cheese_3, x = 272f, y = 248f, tilt = 6f, scale = 0.75f),
        word(4, R.string.meal_cutscene_word_cheese_4, x = 114f, y = 428f, tilt = -6f, scale = 0.75f, ink = CutsceneInk.Sleepy),
    ),
    finaleImage = R.drawable.meal_cutscene_finale_cheese,
    finaleWord = R.string.meal_ceremony_reaction_word,
    longFinaleWord = false,
)

// 밥친구 고르기: 뭐 보지? / 딸깍 딸깍 / 흐음… / 앗, 식는다! / 그냥 띠욧!!(마지막 칸)
internal val MealShow = fourRowCutscene(
    backdrop = CutsceneBackdrop.Paper,
    images = listOf(R.drawable.meal_cutscene_show_1, R.drawable.meal_cutscene_show_2, R.drawable.meal_cutscene_show_3, R.drawable.meal_cutscene_show_4),
    words = listOf(
        word(1, R.string.meal_cutscene_word_show_1, x = 288f, y = 172f, tilt = 6f, scale = 0.75f),
        word(2, R.string.meal_cutscene_word_show_2, x = 94f, y = 368f, tilt = -6f, scale = 0.75f),
        word(3, R.string.meal_cutscene_word_show_3, x = 272f, y = 248f, tilt = 6f, scale = 0.85f, ink = CutsceneInk.Sleepy),
        word(4, R.string.meal_cutscene_word_show_4, x = 114f, y = 428f, tilt = -6f, scale = 0.65f),
    ),
    finaleImage = R.drawable.meal_cutscene_finale_show,
    finaleWord = R.string.meal_cutscene_word_show_finale,
    longFinaleWord = true,
)

// 마지막 한 조각: 하나 남았다… / 눈치… / 동시에! / 반띵…? / 사이좋게 띠욧!!(마지막 칸)
internal val LastPiece = fourRowCutscene(
    backdrop = CutsceneBackdrop.Paper,
    images = listOf(R.drawable.meal_cutscene_lastone_1, R.drawable.meal_cutscene_lastone_2, R.drawable.meal_cutscene_lastone_3, R.drawable.meal_cutscene_lastone_4),
    words = listOf(
        word(1, R.string.meal_cutscene_word_lastone_1, x = 196f, y = 32f, tilt = 3f, scale = 0.75f, ink = CutsceneInk.Sleepy),
        word(2, R.string.meal_cutscene_word_lastone_2, x = 94f, y = 368f, tilt = -6f, scale = 0.85f, ink = CutsceneInk.Sleepy),
        word(3, R.string.meal_cutscene_word_lastone_3, x = 272f, y = 248f, tilt = 6f, scale = 0.85f),
        word(4, R.string.meal_cutscene_word_lastone_4, x = 86f, y = 545f, tilt = -4f, scale = 0.75f, ink = CutsceneInk.Sleepy),
    ),
    finaleImage = R.drawable.meal_cutscene_finale_lastone,
    finaleWord = R.string.meal_cutscene_word_lastone_finale,
    longFinaleWord = true,
)

// 간 보기: 보글보글 / 간만 볼게 / 한 입만 더 / …어라? / 그래도 띠욧!!(마지막 칸)
internal val TasteTest = fourRowCutscene(
    backdrop = CutsceneBackdrop.Paper,
    images = listOf(R.drawable.meal_cutscene_taste_1, R.drawable.meal_cutscene_taste_2, R.drawable.meal_cutscene_taste_3, R.drawable.meal_cutscene_taste_4),
    words = listOf(
        word(1, R.string.meal_cutscene_word_bubble, x = 288f, y = 172f, tilt = 6f, scale = 0.75f),
        word(2, R.string.meal_cutscene_word_taste_2, x = 94f, y = 368f, tilt = -6f, scale = 0.75f),
        word(3, R.string.meal_cutscene_word_taste_3, x = 272f, y = 248f, tilt = 6f, scale = 0.75f),
        word(4, R.string.meal_cutscene_word_taste_4, x = 92f, y = 552f, tilt = -4f, scale = 0.85f, ink = CutsceneInk.Sleepy),
    ),
    finaleImage = R.drawable.meal_cutscene_finale_taste,
    finaleWord = R.string.meal_cutscene_word_still_tiyot,
    longFinaleWord = true,
)

// 포장 봉지: 이 냄새는… / 킁킁킁 / 쏙! / 안 보여~ / 봉지 쓰고 띠욧!!(마지막 칸)
internal val TakeoutBag = fourRowCutscene(
    backdrop = CutsceneBackdrop.Paper,
    images = listOf(R.drawable.meal_cutscene_bag_1, R.drawable.meal_cutscene_bag_2, R.drawable.meal_cutscene_bag_3, R.drawable.meal_cutscene_bag_4),
    words = listOf(
        word(1, R.string.meal_cutscene_word_bag_1, x = 250f, y = 172f, tilt = 6f, scale = 0.75f, ink = CutsceneInk.Sleepy),
        word(2, R.string.meal_cutscene_word_bag_2, x = 94f, y = 368f, tilt = -6f, scale = 0.65f),
        word(3, R.string.meal_cutscene_word_bag_3, x = 272f, y = 248f, tilt = 6f, scale = 0.85f),
        word(4, R.string.meal_cutscene_word_bag_4, x = 114f, y = 428f, tilt = -6f, scale = 0.85f),
    ),
    finaleImage = R.drawable.meal_cutscene_finale_bag,
    finaleWord = R.string.meal_cutscene_word_bag_finale,
    longFinaleWord = true,
)

// 비 오는 날 파전: 주룩주룩 / 지글지글? / 휙! / 착! / 바삭 띠욧!!(마지막 칸)
internal val RainyPajeon = fourRowCutscene(
    backdrop = CutsceneBackdrop.Paper,
    images = listOf(R.drawable.meal_cutscene_jeon_1, R.drawable.meal_cutscene_jeon_2, R.drawable.meal_cutscene_jeon_3, R.drawable.meal_cutscene_jeon_4),
    words = listOf(
        word(1, R.string.meal_cutscene_word_jeon_1, x = 288f, y = 172f, tilt = 6f, scale = 0.75f),
        word(2, R.string.meal_cutscene_word_jeon_2, x = 94f, y = 368f, tilt = -6f, scale = 0.75f),
        word(3, R.string.meal_cutscene_word_fwip, x = 300f, y = 382f, tilt = 6f, scale = 0.85f),
        word(4, R.string.meal_cutscene_word_jeon_4, x = 114f, y = 428f, tilt = -6f, scale = 0.85f),
    ),
    finaleImage = R.drawable.meal_cutscene_finale_jeon,
    finaleWord = R.string.meal_cutscene_word_jeon_finale,
    longFinaleWord = true,
)

// 쌍란: 조심조심 / 톡! / 어? / 두 개!! / 운수 좋은 띠욧!!(마지막 칸)
internal val DoubleYolk = fourRowCutscene(
    backdrop = CutsceneBackdrop.Dawn,
    images = listOf(R.drawable.meal_cutscene_twin_1, R.drawable.meal_cutscene_twin_2, R.drawable.meal_cutscene_twin_3, R.drawable.meal_cutscene_twin_4),
    words = listOf(
        word(1, R.string.meal_cutscene_word_twin_1, x = 288f, y = 172f, tilt = 6f, scale = 0.85f),
        word(2, R.string.meal_cutscene_word_splash_2, x = 94f, y = 368f, tilt = -6f, scale = 1.0f),
        word(3, R.string.meal_cutscene_word_eh, x = 272f, y = 248f, tilt = 6f, scale = 1.0f),
        word(4, R.string.meal_cutscene_word_twin_4, x = 114f, y = 428f, tilt = -6f, scale = 0.85f, ink = CutsceneInk.Hot),
    ),
    finaleImage = R.drawable.meal_cutscene_finale_twin,
    finaleWord = R.string.meal_cutscene_word_twin_finale,
    longFinaleWord = true,
)

// 기지개: 쭈우욱~ / 쭈우우욱~ / 미끄덩! / 퐁당 / 헤헤 띠욧!!(마지막 칸)
internal val MorningStretch = fourRowCutscene(
    backdrop = CutsceneBackdrop.Dawn,
    images = listOf(R.drawable.meal_cutscene_stretch_1, R.drawable.meal_cutscene_stretch_2, R.drawable.meal_cutscene_stretch_3, R.drawable.meal_cutscene_stretch_4),
    words = listOf(
        word(1, R.string.meal_cutscene_word_stretch_1, x = 288f, y = 172f, tilt = 6f, scale = 0.85f),
        word(2, R.string.meal_cutscene_word_stretch_2, x = 94f, y = 368f, tilt = -6f, scale = 0.75f),
        word(3, R.string.meal_cutscene_word_stretch_3, x = 272f, y = 248f, tilt = 6f, scale = 0.85f),
        word(4, R.string.meal_cutscene_word_stretch_4, x = 114f, y = 428f, tilt = -6f, scale = 0.85f),
    ),
    finaleImage = R.drawable.meal_cutscene_finale_stretch,
    finaleWord = R.string.meal_cutscene_word_stretch_finale,
    longFinaleWord = true,
)

// 집사 깨우기: 꾹꾹 / 톡톡 / 냐아아!! / 쓰담쓰담 / 아침 띠욧!!(마지막 칸)
internal val WakeOwner = fourRowCutscene(
    backdrop = CutsceneBackdrop.Dawn,
    images = listOf(R.drawable.meal_cutscene_rouse_1, R.drawable.meal_cutscene_rouse_2, R.drawable.meal_cutscene_rouse_3, R.drawable.meal_cutscene_rouse_4),
    words = listOf(
        word(1, R.string.meal_cutscene_word_knead, x = 288f, y = 172f, tilt = 6f, scale = 0.75f),
        word(2, R.string.meal_cutscene_word_tap, x = 48f, y = 240f, tilt = -6f, scale = 0.75f),
        word(3, R.string.meal_cutscene_word_rouse_3, x = 272f, y = 248f, tilt = 6f, scale = 0.75f, ink = CutsceneInk.Hot),
        word(4, R.string.meal_cutscene_word_rouse_4, x = 114f, y = 428f, tilt = -6f, scale = 0.85f),
    ),
    finaleImage = R.drawable.meal_cutscene_finale_rouse,
    finaleWord = R.string.meal_cutscene_word_rouse_finale,
    longFinaleWord = true,
)

// 참새 손님: 짹? / 빤히… / 으음… / 자, 한 입 / 나란히 띠욧!!(마지막 칸)
internal val SparrowGuest = fourRowCutscene(
    backdrop = CutsceneBackdrop.Dawn,
    images = listOf(R.drawable.meal_cutscene_sparrow_1, R.drawable.meal_cutscene_sparrow_2, R.drawable.meal_cutscene_sparrow_3, R.drawable.meal_cutscene_sparrow_4),
    words = listOf(
        word(1, R.string.meal_cutscene_word_sparrow_1, x = 288f, y = 172f, tilt = 6f, scale = 0.85f),
        word(2, R.string.meal_cutscene_word_stare, x = 94f, y = 368f, tilt = -6f, scale = 1.0f, ink = CutsceneInk.Sleepy),
        word(3, R.string.meal_cutscene_word_sparrow_3, x = 272f, y = 248f, tilt = 6f, scale = 0.85f, ink = CutsceneInk.Sleepy),
        word(4, R.string.meal_cutscene_word_sparrow_4, x = 114f, y = 428f, tilt = -6f, scale = 0.75f),
    ),
    finaleImage = R.drawable.meal_cutscene_finale_sparrow,
    finaleWord = R.string.meal_cutscene_word_sparrow_finale,
    longFinaleWord = true,
)

// 믹서기 깜짝: 꾹 / 위이잉!! / … / 킁킁 / 띠욧!!(마지막 칸)
internal val BlenderScare = fourRowCutscene(
    backdrop = CutsceneBackdrop.Dawn,
    images = listOf(R.drawable.meal_cutscene_blender_1, R.drawable.meal_cutscene_blender_2, R.drawable.meal_cutscene_blender_3, R.drawable.meal_cutscene_blender_4),
    words = listOf(
        word(1, R.string.meal_cutscene_word_blender_1, x = 288f, y = 172f, tilt = 6f, scale = 1.0f),
        word(2, R.string.meal_cutscene_word_blender_2, x = 94f, y = 368f, tilt = -6f, scale = 0.75f, ink = CutsceneInk.Hot),
        word(3, R.string.meal_cutscene_word_blender_3, x = 272f, y = 248f, tilt = 6f, scale = 1.0f, ink = CutsceneInk.Sleepy),
        word(4, R.string.meal_cutscene_word_sniff, x = 114f, y = 428f, tilt = -6f, scale = 1.0f),
    ),
    finaleImage = R.drawable.meal_cutscene_finale_blender,
    finaleWord = R.string.meal_ceremony_reaction_word,
    longFinaleWord = false,
)

// 밥 먹고 가!: 어디 가?! / 꼬옥 / 밥 먹고 가! / 헤헤 / 든든하게 띠욧!!(마지막 칸)
internal val EatBeforeYouGo = fourRowCutscene(
    backdrop = CutsceneBackdrop.Dawn,
    images = listOf(R.drawable.meal_cutscene_stay_1, R.drawable.meal_cutscene_stay_2, R.drawable.meal_cutscene_stay_3, R.drawable.meal_cutscene_stay_4),
    words = listOf(
        word(1, R.string.meal_cutscene_word_stay_1, x = 288f, y = 172f, tilt = 6f, scale = 0.75f, ink = CutsceneInk.Hot),
        word(2, R.string.meal_cutscene_word_stay_2, x = 94f, y = 368f, tilt = -6f, scale = 1.0f),
        word(3, R.string.meal_cutscene_word_stay_3, x = 272f, y = 248f, tilt = 6f, scale = 0.75f),
        word(4, R.string.meal_cutscene_word_stay_4, x = 114f, y = 428f, tilt = -6f, scale = 1.0f),
    ),
    finaleImage = R.drawable.meal_cutscene_finale_stay,
    finaleWord = R.string.meal_cutscene_word_stay_finale,
    longFinaleWord = true,
)

// 천사와 악마: 꼬르륵… / 먹자 먹자 / 안 돼 안 돼 / …먹자! / 다같이 띠욧!!(마지막 칸)
internal val AngelDevil = fourRowCutscene(
    backdrop = CutsceneBackdrop.Night,
    images = listOf(R.drawable.meal_cutscene_angel_1, R.drawable.meal_cutscene_angel_2, R.drawable.meal_cutscene_angel_3, R.drawable.meal_cutscene_angel_4),
    words = listOf(
        word(1, R.string.meal_cutscene_word_angel_1, x = 288f, y = 172f, tilt = 6f, scale = 0.85f, ink = CutsceneInk.Sleepy),
        word(2, R.string.meal_cutscene_word_angel_2, x = 78f, y = 374f, tilt = -6f, scale = 0.75f),
        word(3, R.string.meal_cutscene_word_angel_3, x = 272f, y = 248f, tilt = 6f, scale = 0.75f),
        word(4, R.string.meal_cutscene_word_angel_4, x = 114f, y = 428f, tilt = -6f, scale = 0.75f, ink = CutsceneInk.Sleepy),
    ),
    finaleImage = R.drawable.meal_cutscene_finale_angel,
    finaleWord = R.string.meal_cutscene_word_tiyot_together,
    longFinaleWord = true,
)

// 치킨 다리: 다리는 두 개 / 하나는 집사 / 하나는 내 거 / 날개는…? / 띠욧!!(마지막 칸)
internal val DrumstickShare = fourRowCutscene(
    backdrop = CutsceneBackdrop.Night,
    images = listOf(R.drawable.meal_cutscene_drum_1, R.drawable.meal_cutscene_drum_2, R.drawable.meal_cutscene_drum_3, R.drawable.meal_cutscene_drum_4),
    words = listOf(
        word(1, R.string.meal_cutscene_word_drum_1, x = 255f, y = 172f, tilt = 6f, scale = 0.75f),
        word(2, R.string.meal_cutscene_word_drum_2, x = 94f, y = 368f, tilt = -6f, scale = 0.75f),
        word(3, R.string.meal_cutscene_word_drum_3, x = 268f, y = 374f, tilt = 4f, scale = 0.75f),
        word(4, R.string.meal_cutscene_word_drum_4, x = 114f, y = 428f, tilt = -6f, scale = 0.65f, ink = CutsceneInk.Sleepy),
    ),
    finaleImage = R.drawable.meal_cutscene_finale_drum,
    finaleWord = R.string.meal_ceremony_reaction_word,
    longFinaleWord = false,
)

// 포장마차 어묵: 모락모락 / 호호 / 후루룩 / 하아~ / 뜨끈하게 띠욧!!(마지막 칸)
internal val FishCakeCart = fourRowCutscene(
    backdrop = CutsceneBackdrop.Night,
    images = listOf(R.drawable.meal_cutscene_odeng_1, R.drawable.meal_cutscene_odeng_2, R.drawable.meal_cutscene_odeng_3, R.drawable.meal_cutscene_odeng_4),
    words = listOf(
        word(1, R.string.meal_cutscene_word_steam, x = 288f, y = 172f, tilt = 6f, scale = 0.85f),
        word(2, R.string.meal_cutscene_word_odeng_2, x = 94f, y = 368f, tilt = -6f, scale = 0.85f),
        word(3, R.string.meal_cutscene_word_splash_1, x = 275f, y = 385f, tilt = 6f, scale = 1.0f),
        word(4, R.string.meal_cutscene_word_odeng_4, x = 114f, y = 428f, tilt = -6f, scale = 1.0f),
    ),
    finaleImage = R.drawable.meal_cutscene_finale_odeng,
    finaleWord = R.string.meal_cutscene_word_odeng_finale,
    longFinaleWord = true,
)

// 달이 호떡으로 보여: 달이 밝다… / 꼬르륵 / 어? 호떡…? / 잡았다! / 달콤하게 띠욧!!(마지막 칸)
internal val HotteokMoon = fourRowCutscene(
    backdrop = CutsceneBackdrop.Night,
    images = listOf(R.drawable.meal_cutscene_moon_1, R.drawable.meal_cutscene_moon_2, R.drawable.meal_cutscene_moon_3, R.drawable.meal_cutscene_moon_4),
    words = listOf(
        word(1, R.string.meal_cutscene_word_moon_1, x = 250f, y = 172f, tilt = 6f, scale = 0.75f, ink = CutsceneInk.Sleepy),
        word(2, R.string.meal_cutscene_word_queue_2, x = 94f, y = 368f, tilt = -6f, scale = 1.0f),
        word(3, R.string.meal_cutscene_word_moon_3, x = 262f, y = 300f, tilt = -4f, scale = 0.65f, ink = CutsceneInk.Sleepy),
        word(4, R.string.meal_cutscene_word_moon_4, x = 114f, y = 428f, tilt = -6f, scale = 0.75f),
    ),
    finaleImage = R.drawable.meal_cutscene_finale_moon,
    finaleWord = R.string.meal_cutscene_word_moon_finale,
    longFinaleWord = true,
)

// 야근 메이트: 타닥타닥 / 꾸벅… / 톡 / 토끼다! / 같이 띠욧!!(마지막 칸)
internal val OvertimeMate = fourRowCutscene(
    backdrop = CutsceneBackdrop.Night,
    images = listOf(R.drawable.meal_cutscene_overtime_1, R.drawable.meal_cutscene_overtime_2, R.drawable.meal_cutscene_overtime_3, R.drawable.meal_cutscene_overtime_4),
    words = listOf(
        word(1, R.string.meal_cutscene_word_overtime_1, x = 288f, y = 172f, tilt = 6f, scale = 0.75f),
        word(2, R.string.meal_cutscene_word_nod, x = 94f, y = 368f, tilt = -6f, scale = 1.0f, ink = CutsceneInk.Sleepy),
        word(3, R.string.meal_cutscene_word_overtime_3, x = 272f, y = 248f, tilt = 6f, scale = 1.0f),
        word(4, R.string.meal_cutscene_word_overtime_4, x = 114f, y = 428f, tilt = -6f, scale = 0.85f),
    ),
    finaleImage = R.drawable.meal_cutscene_finale_overtime,
    finaleWord = R.string.meal_cutscene_word_welcome_finale,
    longFinaleWord = true,
)

// 매운맛: 앙! / …? / 스읍— / 화르르!! / 그래도 띠욧!!(마지막 칸)
internal val SpicyFire = fourRowCutscene(
    backdrop = CutsceneBackdrop.Paper,
    images = listOf(R.drawable.meal_cutscene_spicy_1, R.drawable.meal_cutscene_spicy_2, R.drawable.meal_cutscene_spicy_3, R.drawable.meal_cutscene_spicy_4),
    words = listOf(
        word(1, R.string.meal_ceremony_bite_word, x = 288f, y = 172f, tilt = 6f, scale = 1.0f),
        word(2, R.string.meal_cutscene_word_spicy_2, x = 94f, y = 368f, tilt = -6f, scale = 1.0f, ink = CutsceneInk.Sleepy),
        word(3, R.string.meal_cutscene_word_spicy_3, x = 272f, y = 248f, tilt = 6f, scale = 0.85f),
        word(4, R.string.meal_cutscene_word_spicy_4, x = 114f, y = 428f, tilt = -6f, scale = 0.85f, ink = CutsceneInk.Hot),
    ),
    finaleImage = R.drawable.meal_cutscene_finale_spicy,
    finaleWord = R.string.meal_cutscene_word_still_tiyot,
    longFinaleWord = true,
)

// 끝없는 면: 후루룩 / 후루루룩 / 아직도? / 너였어?! / 팽팽하게 띠욧!!(마지막 칸)
internal val EndlessNoodle = fourRowCutscene(
    backdrop = CutsceneBackdrop.Paper,
    images = listOf(R.drawable.meal_cutscene_noodle_1, R.drawable.meal_cutscene_noodle_2, R.drawable.meal_cutscene_noodle_3, R.drawable.meal_cutscene_noodle_4),
    words = listOf(
        word(1, R.string.meal_cutscene_word_splash_1, x = 288f, y = 172f, tilt = 6f, scale = 1.0f),
        word(2, R.string.meal_cutscene_word_noodle_2, x = 94f, y = 368f, tilt = -6f, scale = 0.75f),
        word(3, R.string.meal_cutscene_word_noodle_3, x = 272f, y = 248f, tilt = 6f, scale = 0.75f),
        word(4, R.string.meal_cutscene_word_noodle_4, x = 190f, y = 425f, tilt = -4f, scale = 0.75f, ink = CutsceneInk.Hot),
    ),
    finaleImage = R.drawable.meal_cutscene_finale_noodle,
    finaleWord = R.string.meal_cutscene_word_noodle_finale,
    longFinaleWord = true,
)

// 월요일: 월요일… / 축… / 오물 / 번쩍! / 월요일도 띠욧!!(마지막 칸)
internal val MondayBlues = fourRowCutscene(
    backdrop = CutsceneBackdrop.Paper,
    images = listOf(R.drawable.meal_cutscene_monday_1, R.drawable.meal_cutscene_monday_2, R.drawable.meal_cutscene_monday_3, R.drawable.meal_cutscene_monday_4),
    words = listOf(
        word(1, R.string.meal_cutscene_word_monday_1, x = 262f, y = 172f, tilt = 6f, scale = 0.85f, ink = CutsceneInk.Sleepy),
        word(2, R.string.meal_cutscene_word_monday_2, x = 94f, y = 368f, tilt = -6f, scale = 0.85f, ink = CutsceneInk.Sleepy),
        word(3, R.string.meal_cutscene_word_monday_3, x = 272f, y = 248f, tilt = 6f, scale = 1.0f),
        word(4, R.string.meal_cutscene_word_awake, x = 114f, y = 428f, tilt = -6f, scale = 0.75f),
    ),
    finaleImage = R.drawable.meal_cutscene_finale_monday,
    finaleWord = R.string.meal_cutscene_word_monday_finale,
    longFinaleWord = true,
)

// 금요일: 금요일이다! / 얼쑤! / 짠! / 우와아 / 불금 띠욧!!(마지막 칸)
internal val FridayParty = fourRowCutscene(
    backdrop = CutsceneBackdrop.Paper,
    images = listOf(R.drawable.meal_cutscene_friday_1, R.drawable.meal_cutscene_friday_2, R.drawable.meal_cutscene_friday_3, R.drawable.meal_cutscene_friday_4),
    words = listOf(
        word(1, R.string.meal_cutscene_word_friday_1, x = 262f, y = 176f, tilt = 6f, scale = 0.75f),
        word(2, R.string.meal_cutscene_word_friday_2, x = 94f, y = 368f, tilt = -6f, scale = 0.75f),
        word(3, R.string.meal_cutscene_word_friday_3, x = 272f, y = 248f, tilt = 6f, scale = 0.85f),
        word(4, R.string.meal_cutscene_word_friday_4, x = 114f, y = 428f, tilt = -6f, scale = 1.0f),
    ),
    finaleImage = R.drawable.meal_cutscene_finale_friday,
    finaleWord = R.string.meal_cutscene_word_friday_finale,
    longFinaleWord = true,
)

// 주말 브런치: 하암~ / 아침? 점심? / 몰라~ / 둘 다! / 느긋하게 띠욧!!(마지막 칸)
internal val WeekendBrunch = fourRowCutscene(
    backdrop = CutsceneBackdrop.Paper,
    images = listOf(R.drawable.meal_cutscene_brunch_1, R.drawable.meal_cutscene_brunch_2, R.drawable.meal_cutscene_brunch_3, R.drawable.meal_cutscene_brunch_4),
    words = listOf(
        word(1, R.string.meal_cutscene_word_brunch_1, x = 288f, y = 172f, tilt = 6f, scale = 0.85f),
        word(2, R.string.meal_cutscene_word_brunch_2, x = 94f, y = 368f, tilt = -6f, scale = 0.65f),
        word(3, R.string.meal_cutscene_word_brunch_3, x = 272f, y = 248f, tilt = 6f, scale = 0.75f),
        word(4, R.string.meal_cutscene_word_brunch_4, x = 114f, y = 428f, tilt = -6f, scale = 0.85f),
    ),
    finaleImage = R.drawable.meal_cutscene_finale_brunch,
    finaleWord = R.string.meal_cutscene_word_brunch_finale,
    longFinaleWord = true,
)

// 수라상 냐미: 전하~ / 12첩! / 기미 중… / 통과! / 어명이오 띠욧!!(마지막 칸)
internal val RoyalFeast = fourRowCutscene(
    backdrop = CutsceneBackdrop.Paper,
    images = listOf(R.drawable.meal_cutscene_royal_1, R.drawable.meal_cutscene_royal_2, R.drawable.meal_cutscene_royal_3, R.drawable.meal_cutscene_royal_4),
    words = listOf(
        word(1, R.string.meal_cutscene_word_royal_1, x = 288f, y = 172f, tilt = 6f, scale = 0.75f),
        word(2, R.string.meal_cutscene_word_royal_2, x = 94f, y = 368f, tilt = -6f, scale = 0.75f),
        word(3, R.string.meal_cutscene_word_royal_3, x = 258f, y = 382f, tilt = 6f, scale = 0.85f, ink = CutsceneInk.Sleepy),
        word(4, R.string.meal_cutscene_word_royal_4, x = 190f, y = 424f, tilt = -4f, scale = 0.85f),
    ),
    finaleImage = R.drawable.meal_cutscene_finale_royal,
    finaleWord = R.string.meal_cutscene_word_royal_finale,
    longFinaleWord = true,
)

// 명탐정 냐미: 사라졌다! / 단서 발견 / 추리 중… / 범인은… 나?! / 자수하고 띠욧!!(마지막 칸)
internal val PuddingDetective = fourRowCutscene(
    backdrop = CutsceneBackdrop.Paper,
    images = listOf(R.drawable.meal_cutscene_detective_1, R.drawable.meal_cutscene_detective_2, R.drawable.meal_cutscene_detective_3, R.drawable.meal_cutscene_detective_4),
    words = listOf(
        word(1, R.string.meal_cutscene_word_detective_1, x = 258f, y = 172f, tilt = 6f, scale = 0.85f),
        word(2, R.string.meal_cutscene_word_detective_2, x = 94f, y = 368f, tilt = -6f, scale = 0.75f),
        word(3, R.string.meal_cutscene_word_detective_3, x = 272f, y = 248f, tilt = 6f, scale = 0.85f, ink = CutsceneInk.Sleepy),
        word(4, R.string.meal_cutscene_word_detective_4, x = 114f, y = 428f, tilt = -6f, scale = 0.65f, ink = CutsceneInk.Sleepy),
    ),
    finaleImage = R.drawable.meal_cutscene_finale_detective,
    finaleWord = R.string.meal_cutscene_word_detective_finale,
    longFinaleWord = true,
)
