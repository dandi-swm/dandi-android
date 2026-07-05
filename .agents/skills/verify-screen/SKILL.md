---
name: "verify-screen"
description: "Visually verify an implemented screen against its design spec \u2014 boot/use an emulator, deep-link into the screen, screenshot it, and compare with the Figma frame or design-spec image, reporting spacing/color/typography diffs. Trigger when the user asks to \"verify the screen\", \"compare with the design\", \"check it looks right\", or invokes the `verify-screen` skill, typically after design-to-feature."
---

# Screen visual verification (emulator vs design)

Semantic comparison by vision — not pixel-diff. Catches wrong tokens, missing sections, broken spacing.

## 0. Inputs

- **Screen**: feature name or route path (must exist in `AppRouteRegistry.kt`).
- **Design reference**: Figma frame (MCP `get_screenshot`) or design-spec image/PDF page. If none provided, ask.
- States to check (default: content state only; on request: loading/empty/error too).

## 1. Get the app running

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
ADB="$HOME/android/sdk/platform-tools/adb"
$ADB devices    # no device? boot AVD in background:
"$HOME/android/sdk/emulator/emulator" -avd <name> -no-snapshot-save -no-boot-anim -no-audio &
$ADB wait-for-device shell 'while [ "$(getprop sys.boot_completed)" != "1" ]; do sleep 2; done'
./gradlew :app:installDebug
```

## 2. Enter the screen directly (deep link)

```bash
M="com.jongchan.androidarchi/.main.presentation.MainActivity"
$ADB shell "am start -W -a android.intent.action.VIEW -d 'https://www.androidarchi.com/<path>' -n $M"
sleep 3   # let images/API settle
$ADB exec-out screencap -p > /tmp/screen_<feature>.png
```

> Always pass the explicit component (`-n`): emulators can't verify the App Links domain, so an implicit VIEW intent fails with "unable to resolve Intent" (verified). The deep-link parsing path is identical either way. Fire `am start` calls one at a time with sleeps — rapid-fire intents have crashed emulator system_server.

For non-default states: drive UI via `adb shell input` (tap/text/keyevent) or temporarily point the stub DataSource at the state.

## 3. Compare (Read both images)

Read the screenshot AND the design reference, then check in this order:

1. **Structure** — every design section present? order/alignment right? missing/extra elements?
2. **Tokens** — colors match the semantic slots? (a wrong-slot color usually means the screen bypassed `archiColor`) typography weights/sizes?
3. **Spacing** — paddings/gaps vs design values (Figma MCP values are authoritative; images are approximate).
4. **States** — loading/empty/error renders if requested.

## 4. Report & fix

Table per diff: `위치 | 디자인 | 구현 | 원인 추정 | 수정안`. Severity-ordered (구조 > 토큰 > 간격).

- Obvious token/spacing fixes: apply directly, rebuild+reinstall, re-screenshot, confirm — max 2 fix loops, then report what remains.
- Diffs caused by missing tokens → route to `design-token-sync` instead of inline hacks.
- Save before/after screenshots paths in the report.
