package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.dandi.nyummy.common.presentation.R
import com.dandi.nyummy.common.presentation.designsystem.foundation.NyummyComponentDimens
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme

/** 탄수화물, 단백질, 지방. 각자 음식 일러스트 아이콘을 가진다(색 점 범례를 쓰지 않는다). */
enum class NyummyNutrient(@StringRes internal val label: Int, @DrawableRes internal val icon: Int) {
    Carb(R.string.nyummy_nutrient_carb, R.drawable.nyummy_nutrient_carb),
    Protein(R.string.nyummy_nutrient_protein, R.drawable.nyummy_nutrient_protein),
    Fat(R.string.nyummy_nutrient_fat, R.drawable.nyummy_nutrient_fat),
}

/**
 * 영양소 한 칸. 일러스트 아이콘 32 + 이름(body/s, tertiary) + 그램(number/s).
 * 상자 없이 세 칸을 균등하게 나란히 두는 용도라 너비는 호출하는 쪽이 정한다.
 */
@Composable
fun NyummyNutrientStat(
    nutrient: NyummyNutrient,
    grams: Int,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(nutrient.label)
    val value = stringResource(R.string.nyummy_nutrient_grams, grams)
    Row(
        modifier = modifier.semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(nutrient.icon),
            contentDescription = null,
            modifier = Modifier.size(NyummyComponentDimens.NutrientIconSize),
        )
        Column {
            NyummyText(
                text = label,
                style = NyummyTheme.typography.bodyS,
                color = NyummyTheme.colors.content.tertiary,
                maxLines = 1,
            )
            NyummyText(text = value, style = NyummyTheme.typography.numberS, maxLines = 1)
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun NyummyNutrientStatPreview() {
    NyummyTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8)) {
            NyummyNutrientStat(NyummyNutrient.Carb, grams = 185, modifier = Modifier.weight(1f))
            NyummyNutrientStat(NyummyNutrient.Protein, grams = 42, modifier = Modifier.weight(1f))
            NyummyNutrientStat(NyummyNutrient.Fat, grams = 31, modifier = Modifier.weight(1f))
        }
    }
}
