# FX-7000G

A faithful recreation of the 1985 CASIO fx-7000G scientific calculator for Android,
built with Kotlin + Jetpack Compose.

## Highlights
- **Real dot-matrix LCD** — rendered on an actual 96×64 pixel grid with a hand-built
  5×7 font and the classic green tint (see `ui/DotFont.kt`, `ui/LcdDisplay.kt`).
- **Authentic keypad** — SHIFT-modified keys, DEG/RAD/GRA mode cycling, EXE, AC, DEL,
  Ans and independent memory (M+ / M recall).
- **Scientific engine** — recursive-descent evaluator with:
  - trig `sin/cos/tan` + inverse (`sin⁻¹` …), honouring the current angle mode
  - `log`, `ln`, `10ˣ`, `eˣ`, `x²`, `xʸ`, `√`, `!`, `π`, `e`, `EXP`
  - implicit multiplication, operator precedence, `Ans` continuation
  - 10-significant-digit formatting with exponential fallback and `Ma ERROR` handling

## Build & run
Requires JDK 17 and the Android SDK.

```sh
./gradlew :app:assembleDebug        # build the debug APK
./gradlew installDebug              # install on a connected device/emulator
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.
Or just open the folder in Android Studio and press Run.

## Not included
Graphing and program storage/execution (the "G" features) were intentionally left out
to keep this a focused scientific calculator.
