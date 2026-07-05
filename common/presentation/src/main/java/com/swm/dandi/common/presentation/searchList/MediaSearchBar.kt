package com.swm.dandi.common.presentation.searchList

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.swm.dandi.common.presentation.R
import com.swm.dandi.common.presentation.component.ArchiText
import com.swm.dandi.common.presentation.ui.theme.DesignSystemThemeImpl

/**
 * Baseline Profile generator 등 UiAutomator 가 `By.res(packageName, "search_text_field")`
 * 으로 입력창을 찾아 검색어를 넣을 수 있도록 노출시키는 testTag.
 */
const val SEARCH_TEXT_FIELD_TEST_TAG = "search_text_field"

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MediaSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    // 입력 텍스트도 placeholder(hint) 와 동일한 디자인 토큰(textRegularL = 16sp/Normal)을 사용한다.
    // raw sp 금지 규칙(CLAUDE.md #4)에 따라 TextStyle 을 직접 만들지 않고 typeScale 토큰을 복제한다.
    val textStyle = DesignSystemThemeImpl.typeScale.textRegularL.copy(
        color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel1,
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(DesignSystemThemeImpl.designSystemColor.bgDefaultLevel0)
            .padding(horizontal = 15.dp)
            // testTag 가 UiAutomator By.res() 로 보이도록 SemanticsTree 단위로 opt-in.
            .semantics { testTagsAsResourceId = true },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_search_24),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(18.dp),
        )
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .testTag(SEARCH_TEXT_FIELD_TEST_TAG),
            singleLine = true,
            textStyle = LocalTextStyle.current.merge(textStyle),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(DesignSystemThemeImpl.designSystemColor.contentDefaultLevel1),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    keyboardController?.hide()
                    onSearch(query)
                },
            ),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        ArchiText(
                            text = stringResource(R.string.search_hint),
                            style = DesignSystemThemeImpl.typeScale.textRegularL,
                            color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel3,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    innerTextField()
                }
            },
        )
        if (query.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(DesignSystemThemeImpl.designSystemColor.bgDefaultLevel2)
                    .clickable(onClick = onClear),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.close_small_24),
                    contentDescription = stringResource(R.string.search_clear),
                    tint = DesignSystemThemeImpl.designSystemColor.bgDefaultLevel1,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
