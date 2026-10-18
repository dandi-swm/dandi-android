---
name: "compose-component"
description: "Create a new Jetpack Compose component (Composable function) following this project's conventions — module/package placement, NyummyTheme design tokens (no raw hex/sp), NyummyText, stateless/stateful split, @Preview. Trigger when the user asks to add a button, card, list item, dialog, or any new Composable to an existing screen/module."
---

# Compose component conventions

Multi-module project. Components consume the Nyummy design system ONLY through `NyummyTheme` tokens. Related: `design-to-feature` builds a whole screen; this skill adds a single Composable. Architecture and token rules: [docs/architecture/design-system.md](../../../docs/architecture/design-system.md).

## 1. Placement

| Component type | Location |
|---|---|
| Reusable across features (button, dialog, list row...) | `common/presentation/src/main/java/com/dandi/nyummy/common/presentation/designsystem/component/` |
| Feature-specific | `<feature>/presentation/src/main/java/com/dandi/nyummy/<feature>/presentation/` (subpackage `component/` allowed) |

Before writing a new one, check the existing `designsystem/component/` files: `NyummyText`, `NyummyButton`, `NyummyIconButton`, `NyummyTextButton`, `NyummyCard`, `NyummyListRow`, `NyummyBadge`, `NyummyChip`, `NyummyBottomSheet`, `NyummyDialog`, `NyummySnackbar`, `NyummyTextField`, `NyummyWheelPicker`, `NyummyPoseImage`, `NyummySpriteView` and others. Extend a shared component rather than re-implementing it in a feature module.

## 2. Token rules — HARD requirements

- Colors: `NyummyTheme.colors.<group>.<role>` ONLY (`bg`, `content`, `border`, `data`, `external`). Never `Color(0xFF...)` / hex literals. `NyummyPalette` is internal — use role colors.
- Typography: `NyummyTheme.typography.<role>` via `NyummyText` ONLY. Never raw `fontSize = N.sp`.
- Spacing / radius / sizes: `NyummyTheme.spacing.sN`, `NyummyTheme.radius.{xs,s,m,l,full}`, `NyummyTheme.size.*`, `NyummyTheme.borderWidth.*`. Fixed dimensions that only a shared component needs go in `designsystem/foundation/NyummyComponentDimens` (internal), not in screens.
- Shadows: `NyummyTheme.elevation.{soft,float}`. Buttons and cards have no shadow.
- Icons: Lucide vectors `R.drawable.nyummy_ic_*` from `common:presentation`. No emoji, no hand-drawn shapes.
- `NyummyTheme` getters are `@Composable` — read them outside `Canvas`/`drawBehind` blocks and capture the value.
- If the design needs a color/type that has no token → stop and run `design-token-sync` (don't invent values inline).
- Collections in parameters: `ImmutableList`/`ImmutableSet` (kotlinx.collections.immutable). New VO parameter types → register in `compose_stability.conf`.

## 3. File template

```kotlin
package com.dandi.nyummy.common.presentation.designsystem.component  // match directory

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme

/** 한 줄 요약. 언제 쓰는지와 디자인상 제약을 적는다. */
@Composable
fun NyummyMyComponent(
    title: String,                      // 1) required params
    onClick: () -> Unit,
    modifier: Modifier = Modifier,      // 2) modifier, defaulted
    enabled: Boolean = true,            // 3) optional params
) {
    NyummyText(
        text = title,
        style = NyummyTheme.typography.labelM,
        color = NyummyTheme.colors.content.secondary,
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun NyummyMyComponentPreview() {
    NyummyTheme {
        NyummyMyComponent(title = "예시", onClick = {})
    }
}
```

## 4. Structure rules

- Every public Composable takes `modifier: Modifier = Modifier` and passes it to its outermost layout. Order: required params, then `modifier`, then optional params (same as `NyummyButton(text, onClick, modifier, style, ...)`).
- Stateful/stateless split: the ViewModel-aware wrapper collects `uiState` via `collectAsStateWithLifecycle()` and delegates to a stateless `...Screen(uiState, onIntent)` — Preview attaches to the stateless one.
- Preview functions are `private` and wrapped in `NyummyTheme {}`.
- Strings via `stringResource` (`values/strings.xml` + `values-ko/`). UI copy follows the project copy rules (no interpunct "·", no needless explanatory text).
- Scrollable lists → add `JankScrollWatcher(listState)` (see docs/architecture/performance.md).
- Interactive elements that benchmarks/tests must find → `Modifier.testTag("...")` (don't remove existing tags).
- New shared components: add a section to the debug catalog `NyummyDsCatalogActivity` and a Compose UI test under `common/presentation/src/androidTest/.../designsystem/component/`.

## 5. Verify

1. `package` declaration matches the directory.
2. Compile the touched module: `./gradlew :common:presentation:compileDebugKotlin` (or the feature module).
3. New dependencies must come from `gradle/libs.versions.toml` — add to the catalog first if missing.
4. Run the `gradle-build-check` skill before reporting done.
