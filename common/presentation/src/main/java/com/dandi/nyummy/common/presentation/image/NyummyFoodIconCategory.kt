package com.dandi.nyummy.common.presentation.image

/**
 * 음식 아이콘의 분류. 음식 아이콘을 고르는 화면이 분류별로 걸러 보여 줄 때 쓴다.
 *
 * 분류는 서버가 주지 않고 앱이 아이콘을 보고 정해 둔 것이다. 서버가 아이콘을 추가하면 [NyummyFoodIcons]에 리소스를
 * 넣으면서 여기에도 분류를 넣는다. 넣지 않은 번호는 [SIDE]로 본다.
 */
enum class NyummyFoodIconCategory {
    /** 주식: 밥·면·빵·떡처럼 탄수화물이 중심이거나 한 그릇·한 판으로 끼니가 되는 것 */
    MAIN,

    /** 반찬: 고기·생선·계란·채소 요리와 국·찌개·탕 */
    SIDE,

    /** 과일 */
    FRUIT,

    /** 간식, 디저트: 과자·빵과자·떡·아이스크림·견과류·길거리 간식 */
    SNACK,

    /** 음료 */
    DRINK,
    ;

    companion object {
        /** 아이콘 번호의 분류. 숫자가 아니거나 분류를 정해 두지 않은 번호는 [SIDE]다. */
        fun of(iconId: String): NyummyFoodIconCategory = iconId.toIntOrNull()?.let(categoryById::get) ?: SIDE

        /** 앱에 든 모든 아이콘 번호. 번호순이다. */
        val allIconIds: List<String> = (1..NyummyFoodIcons.MaxIconId).map(Int::toString)

        /** 그 분류의 아이콘 번호. 번호순이다. */
        fun iconIdsOf(category: NyummyFoodIconCategory): List<String> = iconIdsByCategory.getValue(category)

        private val main = intArrayOf(
        2, 3, 4, 5, 7, 8, 11, 12, 13, 17, 19, 21, 22, 23, 27, 30,
        31, 36, 40, 52, 54, 55, 56, 58, 59, 62, 63, 65, 71, 73, 78, 80,
        81, 82, 89, 96, 101, 108, 109, 114, 115, 116, 120, 122, 132, 134, 142, 148,
        155, 166, 168, 176, 179, 195, 198, 199, 201, 203, 206, 208, 210,
        )

        private val fruit = intArrayOf(
        25, 38, 43, 75, 111, 117, 121, 131, 137, 143, 180, 184,
        )

        private val snack = intArrayOf(
        9, 32, 33, 34, 42, 44, 49, 60, 79, 83, 84, 87, 88, 94, 95, 99,
        110, 126, 127, 128, 139, 149, 150, 151, 153, 154, 160, 169, 174, 182, 194, 196,
        204, 205, 207,
        )

        private val drink = intArrayOf(
        6, 26, 35, 46, 69, 85, 91, 92, 93, 105, 112, 113, 123, 124, 138, 140,
        145, 167, 170, 171, 183, 185, 191, 197, 200,
        )

        private val categoryById: Map<Int, NyummyFoodIconCategory> = buildMap {
            main.forEach { put(it, MAIN) }
            fruit.forEach { put(it, FRUIT) }
            snack.forEach { put(it, SNACK) }
            drink.forEach { put(it, DRINK) }
        }

        private val iconIdsByCategory: Map<NyummyFoodIconCategory, List<String>> =
            allIconIds.groupBy { of(it) }.let { grouped -> entries.associateWith { grouped[it].orEmpty() } }
    }
}
