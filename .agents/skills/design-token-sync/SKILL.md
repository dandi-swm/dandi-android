---
name: "design-token-sync"
description: "Sync design tokens (colors / typography) from a Figma file (via Figma MCP) or a design-spec image/PDF into this project's single token file (common/presentation ui/token/DesignTokens.kt). Trigger when the user asks to \"sync tokens\", \"apply the Figma variables\", \"update the theme from the design\", provides a Figma link or design-spec image and asks to update colors/typography, or invokes the `design-token-sync` skill."
---

# Design token sync (Figma → DesignTokens.kt)

All default token values live in ONE file:

- [common/presentation/.../ui/token/DesignTokens.kt](common/presentation/src/main/java/com/jongchan/androidarchi/common/presentation/ui/token/DesignTokens.kt)

It has three `FIGMA-TOKEN-INJECTION-POINT` markers — these are the ONLY edit targets of this skill:

| Marker | What it defines | Kotlin symbol |
|---|---|---|
| `palette` | Raw color palette | `ArchiPaletteColors` enum |
| `semantic-colors` | Semantic slot → palette mapping | `DefaultArchiColor` |
| `type-scale` | Text styles | `DefaultArchiStaticTypeScale` |

The slot *structure* (which slots exist) lives elsewhere and is NOT changed without explicit user confirmation:
- Color slots: `ArchiSemanticColors` in [ui/color/ColorSemantic.kt](common/presentation/src/main/java/com/jongchan/androidarchi/common/presentation/ui/color/ColorSemantic.kt) (also update its `withStringKey`)
- Type slots: `ArchiTypeScale` in [ui/typo/ArchiTypeScale.kt](common/presentation/src/main/java/com/jongchan/androidarchi/common/presentation/ui/typo/ArchiTypeScale.kt) (also update its `withStringKey`)

## 0. Inputs — confirm before starting

- **Design source** (required, one of):
  - Figma file/frame link + connected Figma MCP server → use MCP tools (`get_variable_defs` for variables, `get_design_context` / `get_screenshot` for context; discover exact tool names via the session's MCP tool list).
  - Design-spec image(s) or PDF → Read the file(s) and extract colors (hex) and text styles (weight/size/line-height) visually.
- **Scope** (optional): colors only / typography only / both. Default: both.

If the Figma MCP is not connected and no image/PDF is given, stop and ask for one of the two.

## 1. Naming convention (Figma ↔ code)

| Figma | Code |
|---|---|
| Variable `palette/{name}/{step}` (e.g. `palette/gray/900`) | `ArchiPaletteColors.{Name}{Step}` (e.g. `Gray900`) |
| Variable `{role}/{variant}/{level}` (e.g. `bg/default/level0`, `content/accent`) | `DefaultArchiColor.{role}{Variant}{Level}` (e.g. `bgDefaultLevel0`, `contentAccent`) |
| Text Style `{group}/{weight}/{size}` (e.g. `title/strong/L`) | `DefaultArchiStaticTypeScale` slot `_{group}{Weight}{Size}` (e.g. `_titleStrongL`) |

When extracting from an image/PDF (no variable names available), map by ROLE: background colors → `bg*`, text colors → `content*`, dividers/strokes → `border*`, CTA/highlight → `contentAccent`, like/favorite → `contentFavorite`. State your role assignments in the report.

## 2. Procedure

1. Extract all color values and text styles from the design source.
2. Diff against current `DesignTokens.kt` values.
3. Apply changes **only inside the three marker sections**:
   - New palette colors → add enum entries (keep naming convention).
   - Changed slot values → repoint the slot to the right palette entry. Semantic slots MUST reference `ArchiPaletteColors` — never a raw `Color(0x...)` literal.
   - Type scale → update fontSize / fontWeight / lineHeight / letterSpacing per slot. Font family changes (new font files in `res/font/`) need user confirmation.
4. If the design needs a slot that doesn't exist (e.g. a third accent color), report it and ask before touching `ArchiSemanticColors` / `ArchiTypeScale`. If approved, update the data class, `withStringKey`, and the token file together.
5. Remove nothing: unused palette entries stay (they're `@Suppress("Unused")`).

## 3. Verification — required

Run the `gradle-build-check` skill (at minimum `:common:presentation:compileDebugKotlin` then `:app:assembleDebug`).

## 4. Report format

End with a table:

| Design value | Mapped to | Status |
|---|---|---|
| `bg/default/level0` #F7F7F7 | `bgDefaultLevel0` ← `Gray900` | updated |
| `content/warning` #FFAA00 | — | **unmapped: no slot** (needs user decision) |

Also list code slots that the design did NOT cover (kept at template defaults).
