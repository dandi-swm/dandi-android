---
name: "design-to-feature"
description: "Build a complete feature (4-module set + Compose UI + route registration) from a design spec \u2014 a Figma frame link (via Figma MCP) OR a design-spec image/PDF. Orchestrates make-new-feature-module, api-dto-code-gen, compose-component and gradle-build-check end-to-end. Trigger when the user provides a Figma link / design image / design PDF and asks to \"implement this screen\", \"build this design\", \"make this into a feature\", or invokes the `design-to-feature` skill."
---

# Design → feature pipeline (Figma / image / PDF)

Turns one screen design into a working, building feature that follows this project's architecture. This skill ORCHESTRATES the existing skills — read and follow them at each step instead of improvising:

- `make-new-feature-module` — 4-module scaffolding rules
- `api-dto-code-gen` — data layer from a sample JSON
- `compose-component` — Composable conventions
- `design-token-sync` — token mapping rules
- `gradle-build-check` — build verification

## 0. Inputs — confirm before starting

### Required (ask once if missing)
- **Feature name** — lowerCamelCase, e.g. `productList`. Same validation/normalization as `make-new-feature-module` §0.
- **Design source** — one of:
  - Figma frame link + connected Figma MCP → `get_design_context` (layout/components/variables) + `get_screenshot` (visual reference) for the selected frame.
  - Design-spec image(s) (png/jpg) or PDF → Read them. For a PDF, ask which page(s) cover this screen if >3 pages.

### Optional (confirm once if unclear from the design)
- **API**: sample JSON response or endpoint spec? If none → build the data layer as a **stub** (see §4).
- **Bottom tab?** / **Navigation args?** — same semantics as `make-new-feature-module` §0.2.

## 1. Analyze the design — write the inventory first

Before any code, produce a short screen inventory in the conversation:

1. **Sections** — top-to-bottom UI blocks (app bar, search bar, list, empty state, ...).
2. **States** — loading / empty / error / content variants visible or implied by the design.
3. **Interactions** — taps, scrolls, pagination, navigation targets.
4. **Data** — what fields each section renders → draft of the VO.
5. **Tokens** — every color and text style used, mapped to `ArchiSemanticColors` / `ArchiTypeScale` slots per `design-token-sync` §1. **If any design value has no matching token, STOP and run `design-token-sync` first** (one batch, not per-value questions).

## 2. Pick the golden-example base

Match the inventory against the pattern catalog and say which one you picked:

| If the screen is... | Base feature to mirror |
|---|---|
| Simple remote fetch → render | `intro` |
| Searchable / paginated list, parallel API merge | `search` |
| Local-storage backed (favorites, settings), bottom tab, synthetic backstack | `favorite` |
| Full-screen media / Fragment-hosted / deep-link typed args | `fullScreenMedia` |

## 3. Scaffold

Follow `make-new-feature-module` exactly (conflict check → 4 modules → settings.gradle.kts → app deps → AppRouteRegistry entry). Do not deviate from its file list or naming table.

## 4. Implement

### UI (presentation)
- Colors: ONLY `ArchiThemeImpl.archiColor.*` — never raw `Color(0x...)` / hex.
- Typography: ONLY `ArchiThemeImpl.typeScale.*` via `ArchiText` — never raw `fontSize = N.sp`.
- Reuse common components before writing new ones: `ArchiText`, `BackArrowButton`, `FavoriteHeartButton`, `ContentsList`, `MediaSearchBar` ([common/presentation/component & searchList](common/presentation/src/main/java/com/jongchan/androidarchi/common/presentation/)).
- Spacing/sizes from the design context (Figma values take precedence over eyeballing the screenshot).
- MVI set per naming table: `{Feature}Page` / `{Feature}UIState` (ImmutableList/ImmutableSet only) / `{Feature}Intent` / `{Feature}ReducerEvent` / `{Feature}ViewModel`.
- Every state from §1.2 must render: content, loading, empty, error.

### Data
- Sample JSON provided → follow `api-dto-code-gen` (DTO all-nullable → VO defaulted → `toVO()` → ApiService/DataSource/RepositoryImpl/DataModule).
- No API spec → create the same structure but make the DataSource return hardcoded stub VOs (visually faithful to the design) and mark every stub with `// TODO-API-SPEC: replace with real endpoint`. The screen must still compile and render.

## 5. Verify — required, in this order

1. `gradle-build-check` skill — fix until green.
2. If domain logic was added (UseCase beyond pass-through), add the unit test per `make-new-feature-module` §test and run `run-android-tests`.
3. Ask the `architecture-guardian` subagent to review changed Kotlin/Gradle files. Fix HIGH/MEDIUM findings, then rerun `gradle-build-check`.
4. Optional visual check when an emulator is available: `./gradlew :app:installDebug`, then deep-link straight into the screen:
   `adb shell 'am start -W -a android.intent.action.VIEW -d "https://www.androidarchi.com/<featurePath>" com.jongchan.androidarchi'`
   Screenshot (`adb exec-out screencap -p > /tmp/<feature>.png`), Read it, and compare against the design source; list visual diffs (spacing/color/typography) and fix obvious ones.

## 6. Report format

- Created/modified file list (grouped by module).
- Token mapping table (design value → slot used) + unmapped values.
- Stubs left (`TODO-API-SPEC` locations).
- Base pattern used and any deviations from the golden example, with reasons.

## Hard rules

- Dependency direction: `presentation → domain → entity`, `data → domain → entity`. presentation ↔ data never touch.
- Never modify existing feature modules — only ADD (settings.gradle.kts / app deps / AppRouteRegistry).
- No raw hex colors, no raw sp sizes, no `List` in UIState (use kotlinx immutable).
- If the design contradicts an architecture rule, the rule wins — note the conflict in the report.
