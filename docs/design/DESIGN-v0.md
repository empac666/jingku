# 镜库 Jingku 1.0.3 — UI spec v0

Codex implements this in **one Activity, XML Views, Material Components 1.12+ (Material 3)**. No Compose, no Fragments, no Navigation graph, no shared-element transitions. Do not change `AppConfig.SEMANTIC_LOW_CONFIDENCE` (`0.7889297604560853`), `SEARCH_TOP_K` (3), seek math, SAF / gallery contracts, or the foreground index service. Every decision below is **proposed by Grok — pending agy/Codex**.

## 1. Principles

- The search field is the hero: 56dp pill, top of Home, IME submit. — Perplexity, Google Photos, Apple HIG
- The page is warm paper in light and warm black in dark. System bars use that surface. — Claude, Notion, Perplexity
- One filled action per screen, ink on paper. Amber is only the low-confidence chip. — ChatGPT, Claude, Linear
- A hit is a full-width 16:9 still with the spoken line under it. — Google Photos, Arc Search
- The time badge is the citation. Cosine, latency, and “Top-3” stay in logs. — Perplexity
- Depth is a 1dp hairline. Cards sit at elevation 0. — Linear, Raycast
- An empty screen is one sentence and one button. No illustration, no model names. — Notion, Grok
- Motion is 100–200ms, or a 1000ms linear shimmer. No bounce, no spring overshoot. — Material 3, Linear

## 2. Decisions

| Topic | Part A | Part B | Decision | Why |
|---|---|---|---|---|
| Primary | Indigo `#5250A2` / `#C5C0FF` on focus, buttons, match, chips | Ink `#1C1B19` / `#F3F0E8` for the one filled action | Ink primary. Drop `seed_primary` / `seed_secondary`. No dynamic color | Both reject `#5B5BD6`. Indigo is the same hue as today’s template. Chroma stays on the still |
| Match mark | Paint the accent on the words | Warm wash behind the words | Wash token. Literal-match rules unchanged (trim, ignore case, first hit, bold span) | A purple or yellow marker fights the frame. Dark span text uses `highlight_match_on` |
| Skeleton | 1200ms diagonal gradient, Compose fade | 1000ms alpha pulse on container blocks | 3 cards, `primary_container` shapes, alpha 0.45→1, 1000ms linear. No shimmer library | Same geometry, one animator, no new dependency |
| Navigation | Home, Results, Playback, Library, Settings plus shared elements | Search home; other jobs on other routes | One Activity. `ViewFlipper` body. Preparing and playback are overlays. Library is a `BottomSheetDialog`. About, crash, DB rebuild stay dialogs | Shared elements and fragments are the risky part. Separation still kills the one long form |
| Indexing | 56dp bar that opens a Library screen | Per-video rows inside the library surface | Bar on Home **and** rows in the sheet. The bar does not block search UI. Submit while `IndexTaskCallbacks.running` still toasts `index_busy` | The UI can be read during a job. `EngineHolder` is one mutex, so true concurrent search waits |
| Submit | IME only; 350ms overshoot on focus | 32dp send disc when the field is non-empty; no bounce | IME `actionSearch` **and** the disc. Focus is a 2dp ink stroke | Disc matches “one primary”. Overshoot needs a custom interpolator and reads as playful |
| Low confidence | Extra cuts at 0.85 and 0.70; fade the card under 0.70 | One chip; card stays opaque | One chip iff `score < SEMANTIC_LOW_CONFIDENCE`. Full opacity. No second string | The threshold is frozen. A 0.70 tier would change who is flagged |
| Empty / prep | 80dp illustration; ring plus bar; headline 28sp | No mascot; one 4dp bar; four type roles | 48dp `videocam`. One determinate bar and a percent. Empty title is Headline Small 24sp. No letter-spacing | One progress metaphor. 28sp plus a 56dp field crowds a phone. CJK must not be tracked or italic |

## 3. Tokens

`values/colors.xml` and `values-night/colors.xml`. `themes.xml` maps each name onto the Material 3 attr (`colorSurface` ← `@color/surface`, and so on). Also set `android:statusBarColor` and `android:navigationBarColor` to `@color/surface`, `windowLightStatusBar` / `windowLightNavigationBar` true only in light. Delete the purple seeds.

| colors.xml | Light | Dark | Role |
|---|---|---|---|
| `surface_dim` | `#E8E5DE` | `#0C0B09` | Behind system bars if a scrim is needed |
| `surface` | `#F6F5F1` | `#12110F` | Page, toolbar, sheets’ parent |
| `surface_container_low` | `#F3F1EB` | `#1A1917` | Index bar |
| `surface_container` | `#EDEAE3` | `#201F1C` | Skeleton blocks |
| `surface_container_high` | `#E8E4DA` | `#2A2824` | Pressed row, recent-chip fill |
| `surface_container_highest` | `#E2DED4` | `#35332E` | Dialog surface |
| `surface_container_lowest` | `#FFFFFF` | `#1C1B19` | Search pill, result card |
| `on_surface` | `#1C1B19` | `#F3F0E8` | Titles, dialogue |
| `on_surface_variant` | `#6B6862` | `#A8A396` | Filename, hints, icons |
| `outline` | `#7A776F` | `#948F84` | Focused search stroke |
| `outline_variant` | `#E4E1D8` | `#3A3832` | 1dp card / pill stroke |
| `primary` / `on_primary` | `#1C1B19` / `#F6F5F1` | `#F3F0E8` / `#12110F` | Filled button, send disc |
| `primary_container` / `on_primary_container` | `#E8E4DA` / `#1C1B19` | `#2A2824` / `#F3F0E8` | Chips, skeleton |
| `secondary` = `tertiary` | `#6B6862` | `#A8A396` | Neutral. `on_*` is `on_primary`. Containers copy `primary_container` |
| `error` / `on_error` | `#9C4221` / `#FFFFFF` | `#F2B8B5` / `#601410` | Real failures only |
| `error_container` / `on_error_container` | `#F8E4D8` / `#5C2412` | `#8C1D18` / `#F9DEDC` | Error dialogs |
| `highlight_match` | `#F3E6C4` | `#3E3420` | `BackgroundColorSpan` |
| `highlight_match_on` | `#1C1B19` | `#F0D7A2` | Span text. Light equals `on_surface` |
| `low_confidence` | `#8A5A12` | `#E6B15A` | Chip label, 12sp medium only |
| `low_confidence_container` | `#F4E8D4` | `#3A2C1A` | Chip fill |
| `scrim` / `badge_on` | `#99000000` / `#FFFFFF` | same | Time badge. Player well may be `#000000`; the page may not |

Type (Roboto / Noto Sans CJK, no italic, no `letterSpacing`):

| Use | Style |
|---|---|
| Toolbar 「镜库」 | `TextAppearance.Material3.TitleLarge` |
| Empty title | `TextAppearance.Material3.HeadlineSmall` |
| 「找到 N 个镜头」 | `TextAppearance.Material3.TitleMedium` |
| Dialogue, search input | `TextAppearance.Material3.BodyLarge` |
| Empty / prep body | `TextAppearance.Material3.BodyMedium` |
| Buttons, recent chips | `TextAppearance.Material3.LabelLarge` |
| Time, filename, stage, low-confidence | `TextAppearance.Material3.LabelMedium` + `fontFeatureSettings="tnum"` on times |

Dimens: `space_4` 4dp, `space_8` 8dp, `space_12` 12dp, `space_16` 16dp, `screen_pad` 20dp, `space_24` 24dp, `search_bar_height` 56dp, `send_size` 32dp, `touch_min` 48dp, `index_bar_height` 56dp, `progress_track` 4dp, `library_thumb_w` 112dp, `library_thumb_h` 63dp, `empty_icon` 48dp.

Shapes: `ShapeAppearance.Jingku.Full` parent `Corner.Full` (search, send, recent chips). `Jingku.Large` 16dp (result card, sheet). `Jingku.Small` 8dp (badge, low-confidence chip). `Jingku.None` 0dp. `cardElevation=0dp`, `cardUseCompatPadding=false`, stroke 1dp `outline_variant`.

Motion: press 100ms `PathInterpolator(0.2,0,0,1)` scale 0.96; fade 200ms `PathInterpolator(0,0,0,1)`; shimmer 1000ms linear; index-bar hold 2000ms then fade 200ms. Haptics, only: search submit `KEYBOARD_TAP`, open a shot `CONTEXT_CLICK`, model ready or index finished `CONFIRM`.

Icons: Material Symbols **Outlined** vectors, 24dp, `on_surface_variant`. `search`, `close`, `arrow_upward` (disc, `on_primary`), `videocam`, `video_library`, `info`, `arrow_back`. Language is the text 「EN」 or 「中」, not an icon.

## 4. Structure

```
CoordinatorLayout
  AppBarLayout elev 0, color surface
    Toolbar  镜库 | [EN|中]  video_library  info     long-press title = About
  homeFlipper                         search pill pinned above the flipper
    0 empty  1 queued  2 library  3 searching  4 results  5 no-results
  overlayPreparing   gone unless first extract or a failed extract
  overlayPlayback    gone until a card tap; back closes this first
  index bar          bottom, gone when idle
```

Dialogs: `BottomSheetDialog` library, `MaterialAlertDialogBuilder` about / crash / DB rebuild, `PopupMenu` 相册|文件. Back: playback, then sheet, then results→home (query kept, list cleared), then finish. `recreate()` from the language toggle keeps the import queue and query; it may dismiss playback and the sheet. The index service keeps running.

Keep: `PickMultipleVisualMedia(20)` video-only; `OpenMultipleDocuments("video/*")` plus persistable read permission; `POST_NOTIFICATIONS` then `IndexForegroundService`; `VideoView` + `MediaController` + `SEEK_CLOSEST`; `seekSecForQuery` / `segment_start_unaligned` when the note is non-blank; `LocaleStore` + `recreate()`; `assets/licenses/LICENSING.md`; CrashGuard startup path and previous-crash file delete **only** on 关闭并删除.

## 5. Screens

Chrome under every non-dialog screen: toolbar, then the pill. Placeholder `hint_search`: **一句台词，或一件事** / **A line, or a moment**. Clear (`清除` / `Clear`) and the 32dp disc (`搜索` / `Search`, a11y) show only when text is non-empty. No full-width 搜索 button.

**Home empty** (0 dialogues, empty queue). 48dp `videocam`. Headline **还没有片子** / **No videos yet**. Body **导入一段视频，就能按台词找到镜头** / **Import a video to find a shot by its line.** Filled **导入视频** / **Import** opens the menu. Search stays usable and can return no-results.

```
[ 🔍  一句台词，或一件事          ]
        (videocam)
       还没有片子
 导入一段视频，就能按台词找到镜头
        [ 导入视频 ]
```

**Home queued** (picked, 0 dialogues). Headline **已选 %1$d 个视频** / **%1$d videos selected**. Body **索引之后就能按台词搜索** / **Index them to search by line.** Filled **开始索引** / **Index**. Text button **导入视频** / **Import**.

```
已选 3 个视频
索引之后就能按台词搜索
导入视频          [ 开始索引 ]
```

**Home with library** (`status_dialogue_count`, e.g. 已索引台词 128 条 / 128 dialogue segments indexed). Label **最近搜过** / **Recent**, then a horizontal row of up to 8 Assist chips (`primary_container`, full radius). Chips are successful queries that returned ≥1 hit, newest first, `SharedPreferences`. Hide the row when empty. Tap = fill + search. Toolbar `video_library` opens the sheet. No second filled button.

```
已索引台词 128 条
最近搜过
( 日落 ) ( 海边 ) ( 开场 )
```

**Model preparing.** Full-screen overlay, not cancelable. Title `models_preparing` **正在准备离线模型…** / **Preparing offline models…**. Body `models_preparing_detail` **模型已随应用提供，首次准备完成后即可离线使用** / **Models are bundled. After first preparation, everything works offline.** 4dp determinate bar, label **正在解压模型 %1$d%%** / **Unpacking model %1$d%%**. Skip the overlay if the model is already on disk. Failure keeps the overlay: `models_storage_error` or `models_prepare_error`, filled **重试** / **Retry**, and the existing startup report dialog. No EmbeddingGemma / SenseVoice / Silero here.

```
正在准备离线模型…
模型已随应用提供，首次准备完成后即可离线使用
[=========>          ] 42%
正在解压模型 42%
```

**Indexing.** Home stays on its flipper child. Bottom bar, 56dp, `surface_container_low`: **正在索引 %1$d/%2$d · %3$s** / **Indexing %1$d/%2$d · %3$s** plus a 4dp bar (indeterminate on decode/transcribe, determinate on embed percent). Tap opens the sheet. Success: bar shows `index_done` **索引完成：%1$d 段台词** / **Done: %1$d dialogue segments**, haptic CONFIRM, 2s, fade, refresh the count. Cancel: `index_cancelled` **索引已取消** / **Indexing cancelled** for 2s. Error: `colorError` until dismissed. Rewrite user-visible stage strings (they also feed the notification): **正在读取音轨…** / **Reading audio…**; **正在转写…** / **Transcribing…**; **正在整理 %1$s / %2$s** / **Organising %1$s / %2$s**. Channel name and “正在建索引…” / “Indexing…” stay.

```
…home body stays visible…
┌────────────────────────────┐
│ 正在索引 3/12 · weekend.mp4│
│ [=========>          ] 45% │
└────────────────────────────┘
```

**Searching.** Flipper child replaces the body with 3 skeleton cards (16:9 block, two text bars, radius 16 / 8). Pill keeps the query. Back cancels `work`.

```
[ 🔍  日落                    ✕ ↑ ]
┌────────────────────────────┐
│ ░░░░░ 16:9 ░░░░░░░░░░░░░░░ │
│ ░░░░░░░░░░░░    ░░░░░░     │
└────────────────────────────┘
  (two more of the same)
```

**Results.** Header **找到 %1$d 个镜头** / **%1$d shots**. No millisecond string on screen (`search_done` may remain for logs). `RecyclerView`, gap 12dp, inset 20dp. Card: 16:9 thumb, full bleed, clipped by 16dp; badge bottom-start, 8dp radius, scrim, **00:32** via the existing minute:second formatter; 16dp text pad; dialogue `BodyLarge` max 3 lines with the wash span; low-confidence chip only when under the threshold, not clickable, 8dp radius, no icon, text `low_confidence_hint` **可能不准确** / **May be inaccurate**; meta `LabelMedium` **city-walk.mp4 · 00:32–00:36** (en dash, no cosine). Tap = playback. Thumb `contentDescription` is the file name. Screenshot fixture: query 日落 / sunset; line 一起去看日落吧。 / Let us watch the sunset together.; card 0 `city-walk.mp4` high; card 1 `weekend.mp4` score 0.71 chip on; thumb `ic_launcher_foreground`.

```
找到 2 个镜头
┌────────────────────────────┐
│ 16:9 still          [00:32]│
│ 一起去看[日落]吧。          │
│ city-walk.mp4 · 00:32–00:36│
└────────────────────────────┘
┌────────────────────────────┐
│ 16:9 still          [01:05]│
│ …line…                     │
│ [可能不准确]               │
│ weekend.mp4 · 01:05–01:09  │
└────────────────────────────┘
```

**No results.** **没有对上的台词** / **No matching line**. **换一句试试** / **Try another line.** Recent chips stay under that copy.

```
没有对上的台词
换一句试试
( 日落 ) ( 海边 )
```

**Playback.** Overlay. Top-start `arrow_back` (**返回** / **Back**). 16:9 `VideoView`, well `#000000`, `MediaController`. Under it: the same highlighted line; **已跳到 %1$s** / **Jumped to %1$s** with the seek timestamp; `segment_start_unaligned` (**段起点（未对齐）** / **Segment start (unaligned)**) in `on_surface_variant` when the note is non-blank. `play_error` (**无法播放此视频，请重新授权文件或尝试其他格式** / **Cannot play this video. Regrant access or try another format.**) in `colorError` on that caption. Close fades 200ms and stops playback.

```
←
┌────────────────────────────┐
│         16:9 video         │
└────────────────────────────┘
一起去看[日落]吧。
已跳到 00:32
```

**Library / import.** Sheet radius 16dp. Title **片库** / **Library**, or **索引 %1$d / %2$d** / **Indexing %1$d / %2$d** while running. Rows: 112×63 thumb, filename, one stage line, 4dp progress on the active row. Empty sheet: **还没有导入** / **Nothing imported**. Text **导入视频** opens 相册 / Gallery and 文件 / Files (`btn_pick_gallery` shortened to **相册** / **Gallery**, `btn_pick_saf` to **文件** / **Files**; multi-select unchanged). One filled **开始索引** / **Index** when the queue is non-empty and idle. While running, that button is gone and **取消** / **Cancel** is a text button (`stopIndex`). Toasts unchanged: `index_need_video` **请先选择视频** / **Select videos first**, `index_need_embed` **请等待离线模型准备完成** / **Wait for offline model preparation**, `index_busy` **有任务进行中** / **Task in progress**, `models_preparing`.

```
片库
[thumb] city-walk.mp4
[thumb] weekend.mp4
        正在转写…
        [=====>    ]
        导入视频          [ 开始索引 ]
```

**About / licenses.** Info action and title long-press. Title `about_licenses` **关于与许可证** / **About and licenses**. ScrollView: `model_attribution` (model names live only here), then `licenses/LICENSING.md` unchanged. Button OK.

```
┌ 关于与许可证 ──────────────┐
│ SenseVoiceSmall · …        │
│ EmbeddingGemma 2 · …       │
│ (LICENSING.md, scroll)     │
│                       [OK] │
└────────────────────────────┘
```

**DB rebuild** (not a new visual system). Keep `db_dialog_title`, message from the opener, **清空并重建** / **Wipe and rebuild**, **稍后** / **Later**, non-cancelable. Later sets the stats line to the message in `colorError`.

**Crash-report dialog** (1.0.2 behavior). Non-cancelable. Startup title `startup_error_title` **启动错误** / **Startup error**, body `startup_failed` (**启动失败，请复制或分享以下信息以帮助排查。** / **Startup failed. Copy or share the details below to help diagnose the problem.**) plus `CrashGuard.describe`, and the activity content becomes a padded scroll of that same text. Previous crash: title `crash_report_title` **上次崩溃报告** / **Previous crash report**, body = `filesDir/crash/last_crash.txt`. Buttons, same roles: positive **复制** / **Copy** (clipboard, label = title), neutral **分享** / **Share** (`ACTION_SEND` text/plain; failure toast `crash_share_unavailable` **没有可用的分享应用** / **No app available to share this report**), negative **关闭并删除** / **Dismiss and delete** (deletes the file **only** for the previous-crash dialog). Use `MaterialAlertDialogBuilder` so the theme’s ink applies. Do not change the report text.

```
┌ 上次崩溃报告 ───────────────┐
│ Jingku 1.0.3 …              │
│ Demo exception: …           │
│ [复制]  [分享]  [关闭并删除] │
└─────────────────────────────┘
```

**Language.** On the toolbar’s end, before the library icon. Zh UI shows **EN**; en UI shows **中**. `contentDescription` **切换语言** / **Change language**. Same `LocaleStore` toggle and `recreate()`.

```
镜库                      EN  [library] ⓘ
Jingku                     中  [library] ⓘ
```

## 6. Screenshots

Extend `BaselineScreenshotTest`: sdk 35, `w411dp-h891dp-xxhdpi`, bitmap 1233×2673, `Theme.Jingku` via `ContextThemeWrapper`, gates `jingku.screenshots` and `jingku.screenshotDir`. File `{state}-{zh|en}-{light|dark}.png`. Inflate production layouts. Placeholder thumb only. Dialogs use `captureScreenRoboImage`.

States (× light/dark × zh/en = 48): `home-empty`, `home-queued`, `home-with-library`, `model-preparing` (42%), `indexing-progress` (bar, weekend.mp4, 3/12, 45%), `searching`, `search-results` (high card + low card), `no-results`, `playback` (jumped to 00:32, unaligned caption gone), `library-sheet`, `about-dialog`, `crash-dialog` (keep the current demo trace). Language is the toolbar label on the home shots, not a 13th state.

## 7. Acceptance

- [ ] Light and dark use the hex table; status bar is `surface`; no `#5B5BD6`, `#7C4DFF`, `#C3BFFF`, `#33FFC107`
- [ ] Home, preparing, results, and the index bar show no model names, cosine, milliseconds, or “Top-3”
- [ ] Low-confidence chip uses the frozen threshold only; the card is fully opaque and tappable
- [ ] Gallery and Files both import; index runs in the foreground service; cancel calls `stopIndex`
- [ ] Search during an index toasts `index_busy` and does not open a second embedder
- [ ] Playback seeks with `SEEK_CLOSEST`; unaligned uses the existing string; play errors use `play_error`
- [ ] Language toggle recreates zh↔en; About shows attribution plus `LICENSING.md`; title long-press still opens it
- [ ] Crash dialog keeps the three buttons, non-cancelable, and deletes the crash file only on 关闭并删除
- [ ] DB-rebuild dialog still offers wipe vs later
- [ ] 48 screenshots exist; result shots show a wash on 日落 / sunset and a chip only on the low card
- [ ] No Compose, no fragment transactions, elevation 0 on cards, one filled action on each screen
