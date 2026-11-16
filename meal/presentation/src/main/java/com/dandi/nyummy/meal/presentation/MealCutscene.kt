package com.dandi.nyummy.meal.presentation

import java.time.DayOfWeek
import kotlin.random.Random

/** 기록이 접수된 뒤 재생하는 맛보기 컷신. 무엇을 틀지는 [pickMealCutscene] 이 정한다. */
enum class MealCutscene {
    /** 뜨기 → 꼴깍 → 반짝눈 → 앙! → 띠욧!! */
    Tiyot,

    /** 모락모락 → 앗뜨!! → 후- 후- → 다시 앙! → 띠욧!! */
    HotFuuFuu,

    /** 앙~ → 응? → 빤히… → 우르르! 같이 먹자… → 다같이 띠욧!! */
    StrayParty,

    /** 흠… → 킁킁 → 오물… → 이, 이 맛은…! → 평론가 흉내를 집어던지고 띠욧!! */
    FoodCritic,

    /** 산더미… → 아~앙 → 빵빵! → 우물우물… → 꿀꺽… 띠욧!! */
    HamsterCheeks,

    /** 잘 먹겠습니다~ (집사 숟가락) → 슬금… → 어? → 시치미 뚝 → 헤헤… 띠욧!! */
    SneakyBite,

    /** 앙~ → 앗! → 안 돼애… → 슈퍼 세이브! → 띠욧!! */
    DivingCatch,

    /** 도전자 등장! → 냐미 VS 비둘기 → 와구와구! → K.O.! 띠욧!! */
    FoodFighter,

    /** 잠깐! → 각도가… → 찰칵찰칵 → 음, 좋아 → 띠욧!! */
    Foodstagram,

    /** 오늘은 젓가락으로… → 어라? → 휙! → 덥석! → 띠욧!! */
    ChopstickChallenge,

    /** 꾸벅… → 쏙 → 번쩍! → 개운해!! */
    MorningWakeUp,

    /** 앙! → 멈칫! → 둥실~ → 우주의 맛!! */
    CosmicTaste,

    /** 따르릉!! → 5분만… → 스윽 → 오물오물 → 이불 박차고 띠욧!! */
    BlanketBurrito,

    /** 헉!! → 앙! → 다다다다 → 쾅! → 토스트 문 비둘기와 두근!? */
    LateToast,

    /** 꿀꺽꿀꺽 → 푸하~ → 킥킥 → 오호…? → 에헴, 좋은 아침! */
    MilkMustache,

    /** 촤르르 → 콸콸콸 → 어? → 둥둥 → 숟가락 서핑 띠욧!! */
    CerealFlood,

    /** 따스~ → 꾹꾹 → 팅! → 갸웃? → 진짜 식빵 물고 띠욧!! */
    LoafMorning,

    /** 살금살금 → 끼익… → 헉! → 쉿! → 집사와 공범 띠욧!! */
    MidnightAccomplice,

    /** 보글보글 → 째깍… → 째깍째깍 → 덥석! → 냄비 뚜껑 후루룩 띠욧!! */
    RamenCountdown,

    /** 주문 완료! → 부릉? → 아니네… → 띵동!! → 상자 끌어안고 띠욧!! */
    DeliveryEars,

    /** 톡톡 → (소곤) 먹어볼게요… → 와사삭!!! → 딸깍, 딸깍 → (소곤) 띠욧… */
    MidnightAsmr,

    /** 딩동~ → 1, 2, 3! → 찌익… → 멍… → 그래도 띠욧!! */
    ConvenienceStore,

    /** 앙! → 어라? → 쿵! → 냐미다!! → 빌딩 사이에서 띠욧!!! */
    GiantNyummy,

    /** 앙! → 까딱? → 까딱까딱 → 짤랑짤랑 → 복 들어왔다냥!! */
    LuckyNyummy,

    /** 앙~ → 띵동!! → 나도 나도! → 하나, 둘… → 단체 사진 다같이 띠욧!! */
    AllStars,

    /** 뭐 먹지… → 돌려돌려 → 덜컹! → 또 이거?! → 그래도 띠욧!! */
    MenuRoulette,

    /** 줄이 길다… → 꼬르륵 → 흥! → 다음 손님~! → 입장 띠욧!! */
    LunchLine,

    /** 두근… → 딸깍 → 빼꼼 → 와아! → 띠욧!! */
    LunchboxReveal,

    /** 냠냠 → 배불러~ → 꾸벅… → 쿨… → 꿈에서도 띠욧… */
    FoodComa,

    /** 후루룩 → 톡! → 헉! → 슥슥슥 → 안심하고 띠욧!! */
    SoupSplash,

    /** 하나 → 또 하나 → 어? 꽁다리 → 이게 진짜지! → 꽁다리 띠욧!! */
    GimbapEnd,

    /** 풀… → 풀이네… → 어? 뭐가 있다 → 닭가슴살이다! → 맛있잖아 띠욧!! */
    SaladTwist,

    /** 빙글빙글 → 저거다! → 앗, 놓쳤다 → 한 바퀴만 더… → 잡았다 띠욧!! */
    SushiBelt,

    /** 째깍… → 쫑긋! → 다다다다 → 왔다!! → 같이 띠욧!! */
    WelcomeHome,

    /** 치이익 → 아직… → 아직…! → 지금이야! → 쌈 띠욧!! */
    GrillDay,

    /** 쭈욱 → 쭈우욱 → 쭈우우욱 → 어디까지…? → 띠욧!! */
    CheesePull,

    /** 뭐 보지? → 딸깍 딸깍 → 흐음… → 앗, 식는다! → 그냥 띠욧!! */
    MealShow,

    /** 하나 남았다… → 눈치… → 동시에! → 반띵…? → 사이좋게 띠욧!! */
    LastPiece,

    /** 보글보글 → 간만 볼게 → 한 입만 더 → …어라? → 그래도 띠욧!! */
    TasteTest,

    /** 이 냄새는… → 킁킁킁 → 쏙! → 안 보여~ → 봉지 쓰고 띠욧!! */
    TakeoutBag,

    /** 주룩주룩 → 지글지글? → 휙! → 착! → 바삭 띠욧!! */
    RainyPajeon,

    /** 조심조심 → 톡! → 어? → 두 개!! → 운수 좋은 띠욧!! */
    DoubleYolk,

    /** 쭈우욱~ → 쭈우우욱~ → 미끄덩! → 퐁당 → 헤헤 띠욧!! */
    MorningStretch,

    /** 꾹꾹 → 톡톡 → 냐아아!! → 쓰담쓰담 → 아침 띠욧!! */
    WakeOwner,

    /** 짹? → 빤히… → 으음… → 자, 한 입 → 나란히 띠욧!! */
    SparrowGuest,

    /** 꾹 → 위이잉!! → … → 킁킁 → 띠욧!! */
    BlenderScare,

    /** 어디 가?! → 꼬옥 → 밥 먹고 가! → 헤헤 → 든든하게 띠욧!! */
    EatBeforeYouGo,

    /** 꼬르륵… → 먹자 먹자 → 안 돼 안 돼 → …먹자! → 다같이 띠욧!! */
    AngelDevil,

    /** 다리는 두 개 → 하나는 집사 → 하나는 내 거 → 날개는…? → 띠욧!! */
    DrumstickShare,

    /** 모락모락 → 호호 → 후루룩 → 하아~ → 뜨끈하게 띠욧!! */
    FishCakeCart,

    /** 달이 밝다… → 꼬르륵 → 어? 호떡…? → 잡았다! → 달콤하게 띠욧!! */
    HotteokMoon,

    /** 타닥타닥 → 꾸벅… → 톡 → 토끼다! → 같이 띠욧!! */
    OvertimeMate,

    /** 앙! → …? → 스읍— → 화르르!! → 그래도 띠욧!! */
    SpicyFire,

    /** 후루룩 → 후루루룩 → 아직도? → 너였어?! → 팽팽하게 띠욧!! */
    EndlessNoodle,

    /** 월요일… → 축… → 오물 → 번쩍! → 월요일도 띠욧!! */
    MondayBlues,

    /** 금요일이다! → 얼쑤! → 짠! → 우와아 → 불금 띠욧!! */
    FridayParty,

    /** 하암~ → 아침? 점심? → 몰라~ → 둘 다! → 느긋하게 띠욧!! */
    WeekendBrunch,

    /** 전하~ → 12첩! → 기미 중… → 통과! → 어명이오 띠욧!! */
    RoyalFeast,

    /** 사라졌다! → 단서 발견 → 추리 중… → 범인은… 나?! → 자수하고 띠욧!! */
    PuddingDetective,
}

/**
 * 이번 기록에 틀 컷신을 고릅니다. 시각과 상관없이 가끔 [RareCutscenes] 중 하나를 내고, 오늘 요일에 맞는 컷신([dayCutscene])이 있으면 가끔 그 편을 냅니다.
 * 그 밖에는 아침이면 [MorningCutscenes], 야식 시간이면 [LateNightCutscenes] 중 하나를 고르게 내고,
 * 점심이면 [LunchCutscenes], 저녁이면 [DinnerCutscenes] 를 [EverydayCutscenes] 와 섞어 고르게 냅니다. 나머지 시간에는 [EverydayCutscenes] 만 냅니다.
 *
 * 상자 등급과는 묶지 않습니다. 컷신 세기가 등급을 따라가면 음식 평가처럼 읽히기 때문입니다.
 *
 * @param hour 기기 타임존 기준 지금 시각(0~23).
 * @param dayOfWeek 기기 타임존 기준 오늘 요일.
 */
fun pickMealCutscene(hour: Int, dayOfWeek: DayOfWeek, random: Random = Random.Default): MealCutscene {
    if (random.nextInt(RareOdds) == 0) return RareCutscenes.random(random)
    val today = dayCutscene(hour, dayOfWeek)
    if (today != null && random.nextInt(DayOdds) == 0) return today
    return when (hour) {
        in MorningHours -> MorningCutscenes
        in LateNightHours -> LateNightCutscenes
        in LunchHours -> LunchCutscenes + EverydayCutscenes
        in DinnerHours -> DinnerCutscenes + EverydayCutscenes
        else -> EverydayCutscenes
    }.random(random)
}

/** 오늘 요일에만 나오는 컷신. 월요일과 금요일은 하루 종일, 주말 브런치는 주말 늦은 아침부터 점심까지다. */
private fun dayCutscene(hour: Int, dayOfWeek: DayOfWeek): MealCutscene? = when (dayOfWeek) {
    DayOfWeek.MONDAY -> MealCutscene.MondayBlues
    DayOfWeek.FRIDAY -> MealCutscene.FridayParty
    DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> MealCutscene.WeekendBrunch.takeIf { hour in BrunchHours }
    else -> null
}

/** 아침 끼니에 나오는 컷신. */
private val MorningCutscenes = listOf(
    MealCutscene.MorningWakeUp,
    MealCutscene.BlanketBurrito,
    MealCutscene.LateToast,
    MealCutscene.MilkMustache,
    MealCutscene.CerealFlood,
    MealCutscene.LoafMorning,
    MealCutscene.DoubleYolk,
    MealCutscene.MorningStretch,
    MealCutscene.WakeOwner,
    MealCutscene.SparrowGuest,
    MealCutscene.BlenderScare,
    MealCutscene.EatBeforeYouGo,
)

/** 야식 시간에 나오는 컷신. */
private val LateNightCutscenes = listOf(
    MealCutscene.MidnightAccomplice,
    MealCutscene.RamenCountdown,
    MealCutscene.DeliveryEars,
    MealCutscene.MidnightAsmr,
    MealCutscene.ConvenienceStore,
    MealCutscene.AngelDevil,
    MealCutscene.DrumstickShare,
    MealCutscene.FishCakeCart,
    MealCutscene.HotteokMoon,
    MealCutscene.OvertimeMate,
)

/** 시각과 상관없이 가끔 나오는 컷신. */
private val RareCutscenes = listOf(
    MealCutscene.CosmicTaste,
    MealCutscene.GiantNyummy,
    MealCutscene.LuckyNyummy,
    MealCutscene.AllStars,
    MealCutscene.RoyalFeast,
    MealCutscene.PuddingDetective,
)

/** 아침도 야식 시간도 아니면 나오는 컷신. 점심·저녁에는 그 끼니 컷신과 섞인다. */
private val EverydayCutscenes = listOf(
    MealCutscene.Tiyot,
    MealCutscene.HotFuuFuu,
    MealCutscene.StrayParty,
    MealCutscene.FoodCritic,
    MealCutscene.HamsterCheeks,
    MealCutscene.SneakyBite,
    MealCutscene.DivingCatch,
    MealCutscene.FoodFighter,
    MealCutscene.Foodstagram,
    MealCutscene.ChopstickChallenge,
    MealCutscene.SpicyFire,
    MealCutscene.EndlessNoodle,
)

/** 점심 끼니에 평소 컷신과 섞여 나오는 컷신. */
private val LunchCutscenes = listOf(
    MealCutscene.MenuRoulette,
    MealCutscene.LunchLine,
    MealCutscene.LunchboxReveal,
    MealCutscene.FoodComa,
    MealCutscene.SoupSplash,
    MealCutscene.GimbapEnd,
    MealCutscene.SaladTwist,
    MealCutscene.SushiBelt,
)

/** 저녁 끼니에 평소 컷신과 섞여 나오는 컷신. */
private val DinnerCutscenes = listOf(
    MealCutscene.WelcomeHome,
    MealCutscene.GrillDay,
    MealCutscene.CheesePull,
    MealCutscene.MealShow,
    MealCutscene.LastPiece,
    MealCutscene.TasteTest,
    MealCutscene.TakeoutBag,
    MealCutscene.RainyPajeon,
)

/** 아침 끼니로 보는 시각. 5시부터 10시 59분까지. */
private val MorningHours = 5..10

/** 점심 끼니로 보는 시각. 11시부터 14시 59분까지. */
private val LunchHours = 11..14

/** 저녁 끼니로 보는 시각. 17시부터 20시 59분까지. */
private val DinnerHours = 17..20

/** 주말 브런치 컷신이 나올 수 있는 시각. 9시부터 13시 59분까지. */
private val BrunchHours = 9..13

/** 야식으로 보는 시각. 21시부터 새벽 4시 59분까지. */
private val LateNightHours = (21..23) + (0..4)

/** 희귀 컷신이 나올 확률은 1/[RareOdds]. 희귀 컷신 한 편은 1/([RareOdds] × 희귀 편 수)로 나온다. */
private const val RareOdds = 10

/** 요일 컷신이 있는 날, 희귀 컷신이 아닐 때 그 편이 나올 확률은 1/[DayOdds]. */
private const val DayOdds = 5
