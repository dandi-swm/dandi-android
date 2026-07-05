---
name: "compose-component"
description: "Create a new Jetpack Compose component (Composable function) following this project's conventions \u2014 module/package placement, Archi design tokens (no raw hex/sp), ArchiText, stateless/stateful split, @Preview. Trigger when the user asks to add a button, card, list item, dialog, or any new Composable to an existing screen/module."
---

# Compose component conventions

Multi-module project. Components consume the Archi design system ONLY through tokens. Related: `design-to-feature` builds a whole screen; this skill adds a single Composable.

## 1. Placement

| Component type | Location |
|---|---|
| Reusable across features (button, dialog, list cell...) | `common/presentation/src/main/java/com/jongchan/androidarchi/common/presentation/component/` |
| Feature-specific | `<feature>/presentation/src/main/java/com/jongchan/androidarchi/<feature>/presentation/` (subpackage allowed) |
| List/search shared pieces | `common/presentation/.../searchList/` |

Before writing a new one, check existing components: `ArchiText`, `BackArrowButton`, `FavoriteHeartButton`, `FavoriteHeartAnimationOverlay`, `ContentsList`, `MediaSearchBar`. (File names usually match the component, but not always: `ArchiText` lives in `component/Text.kt`, not `ArchiText.kt`.)

## 2. Token rules — HARD requirements

- Colors: `ArchiThemeImpl.archiColor.<slot>` ONLY. Never `Color(0xFF...)` / hex literals.
- Typography: `ArchiThemeImpl.typeScale.<slot>` via `ArchiText` ONLY. Never raw `fontSize = N.sp`. (Material `Text` 직접 사용 지양 — `ArchiText`가 기본 색/스타일을 토큰으로 강제한다)
- If the design needs a color/type that has no token slot → stop and run `design-token-sync` (don't invent values inline).
- Collections in parameters: `ImmutableList`/`ImmutableSet` (kotlinx.collections.immutable). New VO parameter types → register in `compose_stability.conf`.

## 3. File template

```kotlin
package com.jongchan.androidarchi.common.presentation.component  // match directory

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.jongchan.androidarchi.common.presentation.ui.theme.ArchiTheme
import com.jongchan.androidarchi.common.presentation.ui.theme.ArchiThemeImpl

@Composable
fun MyComponent(
    title: String,                      // 1) primary content param
    modifier: Modifier = Modifier,      // 2) modifier right after it, defaulted
    onClick: () -> Unit = {},           // 3) remaining data params + callbacks (on... prefix)
) {
    ArchiText(
        text = title,
        style = ArchiThemeImpl.typeScale.textStrongM,
        color = ArchiThemeImpl.archiColor.contentDefaultLevel1,
        modifier = modifier,
    )
}

@Preview
@Composable
private fun MyComponentPreview() {
    ArchiTheme {
        MyComponent(title = "Sample", onClick = {})
    }
}
```

## 4. Structure rules

- Every public Composable takes `modifier: Modifier = Modifier` and passes it to its outermost layout. Param order follows the project convention: primary content param first, then `modifier` (defaulted), then remaining data params + callbacks — e.g. `BackArrowButton(modifier, onClick)`, `FavoriteHeartButton(modifier, isFavorite, onToggle)`, `ArchiText(text, modifier, ...)`. (NOT modifier-last.)
- Stateful/stateless split: the ViewModel-aware wrapper collects `uiState` via `collectAsStateWithLifecycle()` and delegates to a stateless `...Content(state, onIntent)` — Preview attaches to the stateless one.
- Preview functions are `private` and wrapped in `ArchiTheme {}`.
- Strings via `stringResource` (add to `common/presentation/src/main/res/values/strings.xml` + `values-ko/`). Scrollable lists → add `JankScrollWatcher(listState)` (see docs/architecture/performance.md).
- Interactive elements that benchmarks/tests must find → `Modifier.testTag("...")` (don't remove existing tags).

## 5. Verify

1. `package` declaration matches the directory.
2. Compile the touched module: `./gradlew :common:presentation:compileDebugKotlin` (or the feature module).
3. New dependencies must come from `gradle/libs.versions.toml` — add to the catalog first if missing.
4. Run the `gradle-build-check` skill before reporting done.
