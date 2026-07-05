---
name: "new-project-from-archi"
description: "Clone the AndroidArchi template into a brand-new Android project \u2014 copy the tree, rename package/applicationId/app name/deep-link host, optionally remove reference features, then verify with a full build and a leftover-string sweep. Optionally takes a design spec (Figma link via Figma MCP, design-spec image(s), or PDF) and continues into design-token-sync + design-to-feature so the new project's theme and screens are generated straight from the design. Trigger when the user asks to \"start a new project from this template\", \"make project X from AndroidArchi\", provides a design spec (Figma/image/PDF) and asks to \"create a new project from this design\", or invokes the `new-project-from-archi` skill."
---

# New project from AndroidArchi template

This procedure was validated when AndroidArchi itself was extracted from its parent project. Follow it exactly — the traps (zsh word-splitting, BSD sed) are real.

## 0. Inputs — confirm before starting

- **Project name** (PascalCase, e.g. `MyShop`) → rootProject.name, app_name, `{Name}Application` class.
- **Package / applicationId** (e.g. `com.example.myshop`) → also derives the package directory path.
- **Destination directory** (default: sibling of the template).
- **Reference features to KEEP** (default: remove all of intro/search/favorite/fullScreenMedia — but `intro` must be kept or replaced by another start destination since the start stack and AppRouteRegistry reference it. If user removes all, keep `intro` and say why).
- **Deep-link host** (default: `www.<projectname-lowercase>.com`).
- **Design spec** (optional) — one of: Figma file/frame link (requires connected Figma MCP), design-spec image(s) (png/jpg), or PDF. When given, run §7 after §6 passes so the project comes out with its real theme and screens, not just the template shell.

Conflict check: destination must not exist. Never overwrite.

## 1. Copy

```bash
rsync -a \
  --exclude 'build/' --exclude '.gradle/' --exclude '.git/' --exclude '.DS_Store' \
  --exclude '.kotlin/' --exclude 'local.properties' --exclude '.idea/' --exclude '*.iml' \
  --exclude '.claude/' --exclude '.codex/worktrees/' \
  AndroidArchi/ <Dest>/
```

Keep Codex project artifacts (`AGENTS.md`, `.agents/`, `.codex/`) and the `gradle/` wrapper so Codex skills/config travel with the project. Do not require legacy source-agent directories for a Codex-only project.

## 2. Rename package directories

```bash
for d in $(find . -type d -path "*com/jongchan/androidarchi" -not -path "*/build/*"); do ...; done
```

Rename `com/jongchan/androidarchi` dirs to the new package path (create intermediate dirs when the new package has different depth). ⚠ zsh does NOT word-split unquoted `$VAR` — iterate with `$(find ...)` command substitution or `find -print0 | xargs -0`, never `for f in $FILES`.

## 3. Content replacements

Apply to all text files (`-name "*.kt" -o -name "*.kts" -o -name "*.xml" -o -name "*.toml" -o -name "*.md" -o -name "*.conf" -o -name "*.properties" -o -name "*.pro" -o -name "*.json" -o -name "*.txt"`, excluding `*/build/*`), via `find ... -print0 | xargs -0 -n 20 sed -i ''`:

| Old | New |
|---|---|
| `com.jongchan.androidarchi` | new package |
| `com/jongchan/androidarchi` | new package path (docs/skills/baseline profiles) |
| `www.androidarchi.com` | new deep-link host |
| `AndroidArchiApplication` | `{Name}Application` (+ rename the file) |
| `AndroidArchi` | `{Name}` (rootProject.name, app_name, Theme.AndroidArchi) |
| `androidarchi_prefs` | `{name}_prefs` |

⚠ BSD sed: no `\b` word boundaries; use plain unique substrings. ⚠ `.txt` must be included — generated baseline profiles (`app/src/release/generated/baselineProfiles/*.txt`) contain class descriptors.

Decide with the user whether to keep the `Archi` design-system prefix (ArchiTheme/ArchiText/archiColor — recommended: keep; it's the design system's name, not branding).

## 4. Remove unwanted reference features (per feature)

1. Delete `<feature>/` directory.
2. Remove its 4 `include(...)` lines from `settings.gradle.kts`.
3. Remove its 4 `implementation(project(...))` lines from `app/build.gradle.kts`.
4. Remove its 2 `implementation(project(...))` lines (presentation/domain) from `main/presentation/build.gradle.kts`.
5. Remove its `AppRoute` entry from `AppRouteRegistry.kt` (+ imports, + tab entry in `RootComposable.kt` if `isBottomTab`).
6. Chase cross-feature references (compile will surface them): e.g. search→fullScreenMedia Args, intro→SearchPage navigation, favorite UseCases in `common/domain`, `MediaSearch*` in `common/data`, baselineprofile scenario, and any feature-specific FQN lines in the single root `compose_stability.conf` (one shared file referenced by `app` + every `:*:presentation`; remove only lines naming the deleted feature — leave the file itself in place).
7. After removal, run `./gradlew :app:clean` once before rebuilding — stale Hilt aggregation artifacts in `app/build` keep referencing deleted classes (`Could not find class file for ...`) and fail `hiltJavaCompileDebug` (verified by smoke test).
8. If removing `search`: replace `GetIntroUseCase`'s `navigateTo(SearchPage)` with the new start destination, and rewrite/disable the baselineprofile scenario.

## 5. Config

- `local.properties`: `sdk.dir=...` (+ `API_KEY` / `API_BASE_URL` for the real backend; defaults point at the demo Kakao API).
- Review `NetworkModule` auth-header format at the `API-CONFIG-INJECTION-POINT` markers.

## 6. Verify — all required

```bash
grep -ri "androidarchi\|jongchan" . --exclude-dir=build   # branding leftovers = 0 (design-system Archi prefix exempt if kept)
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"  # if no system JDK
./gradlew :app:assembleDebug && ./gradlew test
```

## 7. Design spec → theme + screens (only when a design spec was provided)

Work inside the NEW project (its project agent artifacts were copied in §1, so the same skills/config apply there — either continue in this session using the new project's absolute paths, or tell the user to start a session at the new root). Follow docs/DESIGN_TO_CODE_GUIDE.md:

1. **Tokens first** — use the `design-token-sync` skill with the design source (Figma link / image / PDF). Review the mapping report with the user before building screens.
2. **Enumerate screens** — Figma: one frame per screen; PDF: ask which pages map to which screens; images: group them per screen. Confirm the screen list + feature names (lowerCamelCase) with the user in one batch.
3. **Per screen** — use the `design-to-feature` skill with `<featureName> <frame link | image | PDF pages>`. It picks the golden-example base, scaffolds 4 modules, implements token-only Compose UI, registers the route, and build-checks.
4. **Start destination** — if `intro` was kept only as a placeholder start destination (§0), repoint the start stack/AppRouteRegistry to the first real screen and remove `intro` per §4.
5. **Verify** — use `run-android-tests`; if an emulator is available, use `verify-screen` per screen against the design source.

## 8. Report

Report: what was renamed, features removed, leftover-sweep result, build/test status, and — if a design spec was given — the token mapping report, screens generated, and remaining stubs (`TODO-API-SPEC`). Otherwise point at next steps (`design-token-sync` → `design-to-feature` per docs/DESIGN_TO_CODE_GUIDE.md).
