package com.dandi.nyummy.common.presentation.cat

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.res.stringResource
import com.dandi.nyummy.common.entity.cat.CatName
import com.dandi.nyummy.common.presentation.R

/** 사용자가 온보딩에서 지어 준 고양이 이름. 앱 루트가 기기에 기억해 둔 이름을 공급한다. 모르면 null이다. */
val LocalCatName = compositionLocalOf<String?> { null }

/** 지금 고양이 이름. 아직 모르면 기본 이름(냐미)이다. */
@Composable
@ReadOnlyComposable
fun catName(): String =
    LocalCatName.current?.takeIf { it.isNotBlank() } ?: stringResource(R.string.nyummy_cat_default_name)

/**
 * 고양이 이름이 든 문자열 리소스를 읽는다. 리소스의 `%1$s`가 이름 자리이고 [formatArgs]는 `%2$`부터 들어간다.
 * 이름 뒤 조사를 `%1$s(이)가`처럼 써 두면 이름 받침에 맞춰 고른다. 이름 자리가 없는 리소스는 그대로 읽는다.
 */
@Composable
@ReadOnlyComposable
fun catNameStringResource(@StringRes id: Int, vararg formatArgs: Any): String =
    CatName.resolveParticles(stringResource(id, catName(), *formatArgs))

/** 대사 은행처럼 리소스가 아닌 문장의 [CatName.NAME_TOKEN] 자리에 고양이 이름을 넣는다. */
@Composable
@ReadOnlyComposable
fun withCatName(text: String): String = CatName.fill(text, catName())
