---
name: "design-token-sync"
description: "Sync design tokens (colors / typography / dimensions) from a Figma file (via Figma MCP) or a design-spec image/PDF into this project's Nyummy design system (common/presentation designsystem/token and designsystem/theme) and its contract tests. Trigger when the user asks to \"sync tokens\", \"apply the Figma variables\", \"update the theme from the design\", provides a Figma link or design-spec image and asks to update colors/typography, or invokes the `design-token-sync` skill."
---

# Design token sync (Figma → NyummyTheme)

Token values live in `common/presentation/src/main/java/com/dandi/nyummy/common/presentation/designsystem/`:

| File | What it defines | Figma source |
|---|---|---|
| `token/NyummyPalette.kt` | Raw palette (`internal object NyummyPalette`) | Primitive (palette) variables |
| `theme/NyummyColors.kt` | Role colors `DefaultNyummyColors` (`bg`, `content`, `border`, `data`, `external`) | `2 Semantic` variables |
| `theme/NyummyTypography.kt` | Text roles `DefaultNyummyTypography`, font families `NyummyFontFamily` | Text styles |
| `theme/NyummyDimensions.kt` | Spacing, radius, border width, sizes | `3 Dimension` variables |
| `theme/NyummyElevation.kt` | Shadows `soft`, `float` | Effect styles |

Contract tests hold the expected Figma values and MUST change in the same edit:

- `common/presentation/src/test/.../designsystem/theme/NyummyTokenContractTest.kt` (colors, dimensions, effects)
- `common/presentation/src/test/.../designsystem/theme/NyummyTypographyContractTest.kt` (text styles)

Architecture and rules: [docs/architecture/design-system.md](../../../docs/architecture/design-system.md).

## 0. Inputs — confirm before starting

- **Design source** (required, one of):
  - Figma file/frame link + connected Figma MCP server → use MCP tools (`get_variable_defs` for variables, `get_design_context` / `get_screenshot` for context; discover exact tool names via the session's MCP tool list).
  - Design-spec image(s) or PDF → Read the file(s) and extract colors (hex) and text styles (weight/size/line-height) visually.
- **Scope** (optional): colors / typography / dimensions. Default: all.

If the Figma MCP is not connected and no image/PDF is given, stop and ask for one of the two.

## 1. Naming convention (Figma ↔ code)

| Figma | Code |
|---|---|
| `palette/{family}/{step}` (e.g. `palette/evergreen/600`) | `NyummyPalette.{Family}{Step}` (e.g. `Evergreen600`) |
| `{group}/{role}` with kebab case (e.g. `bg/surface-sunken`, `content/on-action`) | `NyummyColors.{group}.{roleCamel}` (e.g. `bg.surfaceSunken`, `content.onAction`) |
| Nested role (e.g. `bg/action/primary-pressed`, `bg/brand-subtle`) | Joined camel case (e.g. `bg.actionPrimaryPressed`, `bg.brandSubtle`) |
| Text style `{group}/{size}` (e.g. `title/m`, `label/m-strong`) | `NyummyTypography.{group}{Size}` (e.g. `titleM`, `labelMStrong`) |
| `3 Dimension` spacing, radius, size | `NyummyTheme.spacing.s{n}`, `NyummyTheme.radius.{xs,s,m,l,full}`, `NyummyTheme.size.*` |

The contract tests list every Figma color and text style name next to its code slot — use them as the mapping table.

When extracting from an image/PDF (no variable names available), map by ROLE: backgrounds → `bg`, text → `content`, dividers/strokes → `border`, charts and progress → `data`. State your role assignments in the report.

## 2. Procedure

1. Extract all color values, text styles, and dimensions from the design source.
2. Diff against the current token files and contract test expectations.
3. Apply changes:
   - New palette colors → add to `NyummyPalette` (keep naming convention).
   - Changed role values → repoint the role to the right palette entry. Role colors MUST reference `NyummyPalette` — never a raw `Color(0x...)` literal.
   - Text styles → update size / weight / line height / letter spacing. New font files in `res/font/` need user confirmation.
   - Update the contract test expectations with the same Figma values.
4. If the design needs a role that doesn't exist, report it and ask before adding it. If approved, update the data class, the default value, the contract test, and consumers together.
5. Do not delete palette entries or roles that the design no longer lists without asking — screens may still use them.

## 3. Verification — required

- `./gradlew :common:presentation:testDebugUnitTest` (contract tests)
- Run the `gradle-build-check` skill (at minimum `:app:assembleDebug`).

## 4. Report format

End with a table:

| Design value | Mapped to | Status |
|---|---|---|
| `bg/surface-sunken` #F9FBF9 | `bg.surfaceSunken` ← `Gray50` | updated |
| `content/warning-strong` #584600 | — | **unmapped: no role** (needs user decision) |

Also list code roles that the design did NOT cover.
