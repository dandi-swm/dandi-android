package com.dandi.nyummy.common.presentation.ui.typo

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.dandi.nyummy.common.presentation.R

@OptIn(ExperimentalTextApi::class)
internal val notoSansKrTextFont = FontFamily(
    Font(
        R.font.noto_sans_kr_wght,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400)),
    ),
    Font(
        R.font.noto_sans_kr_wght,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500)),
    ),
    Font(
        R.font.noto_sans_kr_wght,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700)),
    ),
)

internal val juaDisplayFont = FontFamily(
    Font(R.font.jua_regular, weight = FontWeight.Normal),
)

internal val gowunDodumVoiceFont = FontFamily(
    Font(R.font.gowun_dodum_regular, weight = FontWeight.Normal),
)

@OptIn(ExperimentalTextApi::class)
internal val fredokaNumberFont = FontFamily(
    Font(
        R.font.fredoka_wdth_wght,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700)),
    ),
)
