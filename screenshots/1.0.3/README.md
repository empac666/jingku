# Jingku 1.0.3 real Android layout renders

The final set is the 56 PNGs at this directory's top level, byte-identical to `round2/`. `baseline/` (28 pre-redesign renders) and `round1/` (56 renders reviewed by Grok and agy) are retained as historical evidence.

Toolchain: Robolectric 4.17, Roborazzi 1.76.0, JUnit 4.13.2, Android API 35, Material Components 1.12.0, Gradle 8.13 / AGP 8.13.2, JDK 17. `@GraphicsMode(NATIVE)` uses Android native Skia on the Linux host. Production XML layouts, Theme.Jingku, localized/night resources, JingkuScreen, result binder, ImportSheet and shared license/trace views are rendered with deterministic fixture state. Animator scale is zero. Views use drawToBitmap and captureRoboImage; dialogs use captureScreenRoboImage. No HTML or replacement mock layout.

Size: 411×891dp / 1233×2673px (xxhdpi). All 14 states render in zh/en and light/dark. Names: `{state}-{zh|en}-{light|dark}.png`.

States: home-empty, home-queued, home-with-library, home-pending, model-preparing (42%), indexing-progress (3/12, 45%), searching, search-results (0.85 and 0.71 including low confidence), no-dialogue, search-failed, playback, import-sheet, about-dialog, crash-dialog.

From the workspace root:

```bash
bash release/scripts/render-final-screenshots.sh
# Equivalent:
export JAVA_HOME=/home/box/jdk ANDROID_HOME=/home/box/android-sdk
export PATH="$JAVA_HOME/bin:$PATH"
/home/box/gradle-8.13/bin/gradle -p release/app --offline --no-daemon recordBaselineScreenshots -Pscreenshots=true -PscreenshotDir="$PWD/release/screenshots/1.0.3/round2"
```

The fixture is `android/app/src/testDebug/java/com/broll/poc/BaselineScreenshotTest.kt` in the public source mirror. It is excluded from release tests and gated off in ordinary debug runs. In the public source checkout, run the same Gradle task with `-p android` and a chosen screenshotDir after providing the pinned cache files as documented in docs/MODELS.md. Defaults retain round1 for historical script compatibility; use the explicit output path for final renders.

These are real Android layout renders using fake data, an original abstract landscape vector as the thumbnail fixture and a black player well. They do not demonstrate real inference, video decoding, physical-device insets/IME, large-font TalkBack behavior, or install-time delivery. The crash view scrolls horizontally to preserve identifiers; Copy/Share use unmodified trace text. Full file list: [round2/INDEX.md](round2/INDEX.md). SHA inventory: release/out/final-screenshots.json (also mirrored in docs/evidence/final-1.0.3).
