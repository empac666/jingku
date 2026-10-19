# Jingku 1.0.3 UI — round 1

56 real Android native-Skia renders of production XML, Material 3 themes, and the shared `JingkuScreen` state renderer. Fake state data; checked-in launcher-vector thumbnails. The baseline directory is untouched.

| State | 中文 light | 中文 dark | English light | English dark |
|---|---|---|---|---|
| home-empty | [zh light](home-empty-zh-light.png) | [zh dark](home-empty-zh-dark.png) | [en light](home-empty-en-light.png) | [en dark](home-empty-en-dark.png) |
| home-queued | [zh light](home-queued-zh-light.png) | [zh dark](home-queued-zh-dark.png) | [en light](home-queued-en-light.png) | [en dark](home-queued-en-dark.png) |
| home-with-library | [zh light](home-with-library-zh-light.png) | [zh dark](home-with-library-zh-dark.png) | [en light](home-with-library-en-light.png) | [en dark](home-with-library-en-dark.png) |
| home-pending | [zh light](home-pending-zh-light.png) | [zh dark](home-pending-zh-dark.png) | [en light](home-pending-en-light.png) | [en dark](home-pending-en-dark.png) |
| model-preparing | [zh light](model-preparing-zh-light.png) | [zh dark](model-preparing-zh-dark.png) | [en light](model-preparing-en-light.png) | [en dark](model-preparing-en-dark.png) |
| indexing-progress | [zh light](indexing-progress-zh-light.png) | [zh dark](indexing-progress-zh-dark.png) | [en light](indexing-progress-en-light.png) | [en dark](indexing-progress-en-dark.png) |
| searching | [zh light](searching-zh-light.png) | [zh dark](searching-zh-dark.png) | [en light](searching-en-light.png) | [en dark](searching-en-dark.png) |
| search-results | [zh light](search-results-zh-light.png) | [zh dark](search-results-zh-dark.png) | [en light](search-results-en-light.png) | [en dark](search-results-en-dark.png) |
| no-dialogue | [zh light](no-dialogue-zh-light.png) | [zh dark](no-dialogue-zh-dark.png) | [en light](no-dialogue-en-light.png) | [en dark](no-dialogue-en-dark.png) |
| search-failed | [zh light](search-failed-zh-light.png) | [zh dark](search-failed-zh-dark.png) | [en light](search-failed-en-light.png) | [en dark](search-failed-en-dark.png) |
| playback | [zh light](playback-zh-light.png) | [zh dark](playback-zh-dark.png) | [en light](playback-en-light.png) | [en dark](playback-en-dark.png) |
| import-sheet | [zh light](import-sheet-zh-light.png) | [zh dark](import-sheet-zh-dark.png) | [en light](import-sheet-en-light.png) | [en dark](import-sheet-en-dark.png) |
| about-dialog | [zh light](about-dialog-zh-light.png) | [zh dark](about-dialog-zh-dark.png) | [en light](about-dialog-en-light.png) | [en dark](about-dialog-en-dark.png) |
| crash-dialog | [zh light](crash-dialog-zh-light.png) | [zh dark](crash-dialog-zh-dark.png) | [en light](crash-dialog-en-light.png) | [en dark](crash-dialog-en-dark.png) |

## Reproduce

```bash
release/scripts/render-ui-screenshots.sh
```

Robolectric 4.17, Roborazzi 1.76.0, API 35/JDK 17; `@GraphicsMode(NATIVE)`, `w411dp-h891dp-xxhdpi`, 1233×2673 PNGs. `Theme.Jingku` uses a `ContextThemeWrapper`; both the host and wrapper configuration select language/night mode so dialog windows resolve the same tokens. Animators are disabled (static skeletons). Dialog and sheet captures use `captureScreenRoboImage`; other states capture the root bitmap. Recording is gated by `jingku.screenshots` and `jingku.screenshotDir`; ordinary debug tests skip recording and release tests do not include this fixture.

All 56 states were recorded and inspected; affected states were refreshed after visual fixes. The command above records the complete final set, including the 16:9 playback geometry applied by MainActivity. Per-image SHA-256 inventory: [rc1-screenshots.json](../../../out/rc1-screenshots.json).

## Inspection and limits

All 56 images were inspected in four language/theme contact sheets, with full-size checks of home, results, preparation, import, and crash dialog. Fixed dialog night-resource selection, 16dp dialog corners/background, full-bleed thumbnail clipping, progress stop dots/gaps, repeated queue filenames, and filled pill-shaped recent chips. Highlight foreground/background and the low-confidence label are visible in both themes.

The playback well is intentionally black: no real media/native playback or model inference is simulated. Insets under gesture/three-button navigation, keyboard versus the sheet, 320dp/200% font behavior, TalkBack focus/one action per result, actual rotation during native inference, crash relaunch/share, and real import/index/seek still require device acceptance. These renders do not establish those device checks.
