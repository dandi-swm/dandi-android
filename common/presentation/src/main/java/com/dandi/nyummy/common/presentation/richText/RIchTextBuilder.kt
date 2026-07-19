package com.dandi.nyummy.common.presentation.richText

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import com.dandi.nyummy.common.entity.richText.RichTextVO
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemThemeImpl

object RichTextBuilder {
    @Composable
    fun build(textVOs: List<RichTextVO>): AnnotatedString = buildAnnotatedString {
        textVOs.forEach { textVO ->
            val textStyle = DesignSystemThemeImpl.typeScale.withStringKey(textVO.typeScale)
                .copy(DesignSystemThemeImpl.designSystemColor.withStringKey(textVO.color))

            if (textVO.link.isEmpty()) {
                withStyle(textStyle.toSpanStyle()) {
                    append(textVO.text)
                }
            } else {
                withLink(
                    LinkAnnotation.Clickable(
                        tag = textVO.text,
                        styles = TextLinkStyles(style = textStyle.toSpanStyle()),
                        linkInteractionListener = { /* handle textVO.link */ },
                    ),
                ) {
                    append(textVO.text)
                }
            }
        }
    }
}
