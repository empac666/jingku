# 镜库 Jingku 1.0.3 — UI spec v1.1

Agreed spec for **one Activity, XML Views, Material Components 1.12+ (Material 3)**. No Compose, fragments, Navigation graph, or shared-element transitions. Do not change `AppConfig.SEMANTIC_LOW_CONFIDENCE`, `SEARCH_TOP_K` (3), seek math, SAF / gallery contracts, or the foreground-service contract (`ACTION_INDEX` / `ACTION_STOP`, extras, `IndexResult` shapes). `EngineHolder`'s mutex covers `open` / `reset` only. One implementation pass.

## 1. Principles

- The search field is the hero: 56dp minimum pill, top of Home, IME submit. — Perplexity, Google Photos, Apple HIG
- The page is warm paper in light and warm black in dark. System bars are transparent over that surface. — Claude, Notion, Perplexity
- One dominant filled action per context. Amber is only the low-confidence label. — ChatGPT, Claude, Linear
- A hit is a full-width 16:9 still with the spoken line under it. — Google Photos, Arc Search
- The time badge is the citation. Cosine, latency, and “Top-3” stay in logs. — Perplexity
- Depth is a 1dp decorative hairline. Cards sit at elevation 0. — Linear, Raycast
- An empty screen is one sentence and one button. No illustration, no model names. — Notion, Grok
- Motion is 100–200ms, or a reversible 1000ms pulse. No bounce, no spring overshoot. — Material 3, Linear

## 2. Decisions

| Topic | Decision | Why | Status |
|---|---|---|---|
| Primary | Ink `#1C1B19` / `#F3F0E8` for the filled action. No dynamic color. Both day and night themes map every role below | Chroma stays on the still. Leftover Material defaults reintroduce the purple seeds | agreed (Grok+Codex+agy) |
| Match mark | Wash + bold + `ForegroundColorSpan(highlight_match_on)`. New spans on every bind. No span when the literal match misses | Dark body text on `#3E3420` is unspecified if only the background span is set | agreed with change |
| Skeleton | Three cards, alpha 0.45↔1, 1000ms reverse, `PathInterpolator(0.4,0,0.2,1)`, 150ms delay. Static if animator scale is 0. Stop when hidden or destroyed | A linear blink reads as a strobe. Fast searches must not flash | agreed with change |
| Navigation | One Activity. Root `FrameLayout`. One `render` on the main thread. No `ViewFlipper`. Precedence: startup failure, then preparing, then playback, then body | Overlays are simultaneous with the body. A flipper cannot express that | agreed with change |
| Indexing | Home bar plus a sheet titled **导入与索引**. Disc disabled while a batch runs. Rows are the retained queue only | There is no video table. Concurrent search would need engine work beyond this pass | agreed with change |
| Submit | IME `actionSearch` and a 32dp glyph inside its own 48dp target. Clear is a separate 48dp target. Hide the IME on accepted submit | 32dp is below the 48dp minimum. The two hit areas must not overlap | agreed with change |
| Low confidence | One non-focusable label iff `score < SEMANTIC_LOW_CONFIDENCE`. Card opacity 1 | The threshold is frozen. A faded card looks disabled | agreed (Grok+Codex+agy) |
| Empty / prep | 48dp `videocam`, 24sp title, one 4dp bar, percent once. At 100% copy: **正在验证模型…** / **Verifying model…**. Skip the overlay only when `ModelStore.locate()` is non-null and SenseVoice plus VAD are ready | File existence without the verified marker is not ready. Two percents repeat | agreed with change |

## 3. Tokens

`values/colors.xml` and `values-night/colors.xml`. `themes.xml` and `values-night/themes.xml` map `colorSurface`←`surface`, `colorOnSurface`←`on_surface`, `colorSurfaceVariant`←`surface_container_high`, `colorOnSurfaceVariant`←`on_surface_variant`, `colorOutline`←`outline`, `colorOutlineVariant`←`outline_variant`, and the same pattern for primary, secondary, tertiary, and error, including containers. `android:colorBackground`←`surface`. Light inverse surface/on-surface are `#12110F` / `#F3F0E8`; dark inverse is `#F6F5F1` / `#1C1B19`. `colorControlHighlight` is 12% `on_surface`. `elevationOverlayEnabled=false`. Progress indicator `primary`, track `surface_container_highest`. Delete `seed_primary` and `seed_secondary`.

| Token | Light | Dark | Role |
|---|---|---|---|
| `surface_dim` | `#E8E5DE` | `#0C0B09` | Scrim behind bars if needed |
| `surface` | `#F6F5F1` | `#12110F` | Page, window background |
| `surface_container_lowest` | `#FFFFFF` | `#0E0D0B` | Light search and cards. Dark: darkest well, not the card |
| `surface_container_low` | `#F3F1EB` | `#1A1917` | Index bar |
| `surface_container` | `#EDEAE3` | `#242220` | Light skeleton. Dark result card |
| `surface_container_high` | `#E8E4DA` | `#2C2A26` | Pressed row, chips, dark skeleton, dark search pill |
| `surface_container_highest` | `#E2DED4` | `#3A3834` | Dialogs, sheets |
| `on_surface` | `#1C1B19` | `#F3F0E8` | Titles, dialogue, dialog body |
| `on_surface_variant` | `#65625C` | `#A8A396` | Filename, hints, icons |
| `outline` | `#7A776F` | `#948F84` | Search stroke at rest |
| `outline_variant` | `#A39D90` | `#6A645A` | 1dp card hairline |
| `primary` / `on_primary` | `#1C1B19` / `#F6F5F1` | `#F3F0E8` / `#12110F` | Filled button, send glyph |
| `primary_container` / `on_primary_container` | `#E8E4DA` / `#1C1B19` | `#2C2A26` / `#F3F0E8` | Chips |
| `secondary` = `tertiary` | `#65625C` | `#A8A396` | `on_*` copies `on_primary`. Containers copy `primary_container` |
| `error` / `on_error` | `#9C4221` / `#FFFFFF` | `#F2B8B5` / `#601410` | Failure text |
| `error_container` / `on_error_container` | `#F8E4D8` / `#5C2412` | `#8C1D18` / `#F9DEDC` | Error dialogs |
| `highlight_match` / `highlight_match_on` | `#F3E6C4` / `#1C1B19` | `#3E3420` / `#F0D7A2` | Both spans |
| `low_confidence` / `low_confidence_container` | `#8A5A12` / `#F4E8D4` | `#E6B15A` / `#3A2C1A` | 12sp label |
| `scrim` / `badge_on` | `#99000000` / `#FFFFFF` | same | Time badge. Player well `#000000` |

Ratios are WCAG 2.2 relative luminance, light / dark. Small text needs 4.5:1. A control boundary needs 3:1. A decorative hairline does not.

| Pair | L / D | Result |
|---|---|---|
| `on_surface` / `surface`; filled button pair | 15.78 / 16.57 | Pass |
| `on_surface_variant` / `surface` | 5.57 / 7.50 | Pass. Was 5.09 / 7.50 |
| `on_surface_variant` / `surface_container_highest` | 4.52 / 4.65 | Pass. Light was 4.13 (`#6B6862` on `#E2DED4`) |
| `outline` / search fill (`#FFFFFF` / `#2C2A26`) | 4.47 / 4.45 | Pass. This is the search boundary |
| `outline_variant` / card (`#FFFFFF` / `#242220`) | 2.70 / 2.71 | Decorative. Old hairline was 1.31 / 1.47 |
| `highlight_match_on` / `highlight_match` | 13.87 / 8.71 | Pass |
| `low_confidence` / `low_confidence_container` | 4.88 / 6.95 | Pass at 12sp |
| `error` / `surface`; `on_error` / `error`; `on_error_container` / `error_container` | 5.99 / 11.05; 6.53 / 7.66; 9.95 / 7.17 | Pass |
| `badge_on` / `#99000000` over white | 5.74 | Pass. No extra badge alpha |
| `#D5D1C7` / `#F6F5F1`; `#D5D1C7` / `#FFFFFF` | 1.40 ; 1.52 | Not used |

Type (Roboto / Noto Sans CJK, no italic, no `letterSpacing`): toolbar `TitleLarge`; empty title `HeadlineSmall`; result header `TitleMedium`; dialogue and search input `BodyLarge`; empty / prep body `BodyMedium`; buttons and chips `LabelLarge`; time, filename, stage, low-confidence `LabelMedium` with `tnum` on times.

Dimens: `space_4` 4, `space_8` 8, `space_12` 12, `space_16` 16, `screen_pad` 20, `space_24` 24, `search_bar_height` 56 **minimum**, `send_size` 32, `touch_min` 48, `index_bar_height` 56 minimum, `progress_track` 4, `library_thumb_w` 112, `library_thumb_h` 63, `empty_icon` 48, `chip_max_w` 180. All dp.

Shapes: `Jingku.Full` parent `Corner.Full` (search, send, chips). `Jingku.Large` 16dp (card, sheet). `Jingku.Small` 8dp (badge, low-confidence). `Jingku.None` 0. `cardElevation=0`, `cardUseCompatPadding=false`, stroke 1dp `outline_variant`.

Motion: compact controls scale 0.96 in 100ms `PathInterpolator(0.2,0,0,1)`. Cards use a ripple, not a scale. Fade 200ms `PathInterpolator(0,0,0,1)` on overlay chrome only. Pulse as in §2. Index-bar hold 2000ms then fade 200ms, tagged with the job generation. Haptics only if the system haptic setting is on: submit `KEYBOARD_TAP`, open a shot `CONTEXT_CLICK`, model ready or index finished `CONFIRM` on API 30+ else `CONTEXT_CLICK`.

Icons: Material Symbols Outlined, 24dp, `on_surface_variant`, inside a 48dp target. `search`, `close`, `arrow_upward` (glyph `on_primary`), `videocam`, `video_library`, `info`, `arrow_back`. Language is the text **EN** or **中**.

## 4. Structure

```
FrameLayout  background surface, draws behind transparent bars
  LinearLayout  chrome column
    Toolbar
    search row     min 56dp, gone under startup / preparing / playback
    body FrameLayout   one visible child
    index bar      in the column, gone when idle or when the IME is visible
  overlayPreparing   match parent, gone unless preparing
  overlayPlayback    match parent, gone unless playing
```

`WindowCompat.setDecorFitsSystemWindows(window, false)`. Status and navigation colors transparent. Light status and navigation icons in the light theme only. Pad the toolbar with status-bar and cutout insets. Pad the index bar with navigation-bar insets. Pad the sheet with navigation-bar and IME insets. Gesture and three-button navigation both keep the last control clear of the system bar.

`render` is the only writer of visibility, copy, and enabled state. Callbacks post to main. `onDestroy` detaches `IndexTaskCallbacks`, cancels jobs, and clears timers. A generation integer drops stale search results and stale 2s holds.

Body, first match: startup failure replaces the tree; else DB not yet opened → **正在打开索引库…** / **Opening the index…**; else DB unavailable; else searching; else search failed; else results; else queued if the queue is non-empty and dialogue count is 0; else ready if count > 0; else no-dialogue if the last search finished with zero hits; else empty. A pending queue does not replace a ready home. Clear, or a change in dialogue count, leaves no-dialogue.

Overlay precedence on top of that body: preparing, else playback, else none. Under preparing or playback, the search row is gone and the body is `IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS`. Focus moves to the overlay’s first control and returns to the opener on close.

Back: if the IME is visible, do not consume it. Else close playback, else dismiss the sheet, else cancel a search in flight, else from results / search-failed / no-dialogue return home (keep the query, clear the list), else finish.

Target 35 edge-to-edge is an acceptance check on a gesture device and a three-button device. `VideoView` stays a `SurfaceView`: do not animate its alpha. On close, `stopPlayback`, set `GONE`, and null `playingUri` and `preparedPlayer` before the chrome fades.

Keep: `PickMultipleVisualMedia(20)` video-only; `OpenMultipleDocuments("video/*")` plus persistable read; `POST_NOTIFICATIONS` then the service, and a denial still calls `startIndexInternal`; `SEEK_CLOSEST`; `seekSecForQuery` / `segment_start_unaligned` when the note is non-blank; `LocaleStore` + `recreate()`; `LICENSING.md`; CrashGuard, and delete the crash file **only** on 关闭并删除.

`recreate()` restores the queue and query, reattaches callbacks, and replays `lastProgress` once if `running`. It may dismiss playback and the sheet. It does not stop the service. Process death: the service is `START_NOT_STICKY`. Restore the queue only. Do not invent completed rows.

Notification content intent only brings this activity forward. An empty picker result leaves the current body.

## 5. Screens

Chrome except startup, preparing, and playback: toolbar, then the pill. Pill fill is `surface_container_lowest` in light and `surface_container_high` in dark. Hint `hint_search`: **一句台词，或它的意思** / **A line, or what it means.** Clear (**清除** / **Clear**, 48dp) and the send target (**搜索** / **Search**, 48dp, glyph 32dp) show only when the field is non-empty. They do not overlap. Focused pill: 2dp `primary`. Resting pill: 1dp `outline`. The disc is the filled control while it is visible, and a body primary on that screen becomes a text button. Disabled disc alpha 0.38 (disabled text is exempt from 4.5:1); the status line carries the reason.

**Home empty.** 48dp `videocam`. **还没有片子** / **No videos yet**. **导入一段视频，就能按台词找到镜头** / **Import a video to find a shot by its line.** Filled **导入视频** / **Import** opens 相册|文件. Search may run and hit no-dialogue.

**Home queued** (queue non-empty, 0 dialogue). **已选 1 个视频** / **1 video selected**; **已选 %1$d 个视频** / **%1$d videos selected**. **索引之后就能按台词搜索** / **Index them to search by line.** Filled **开始索引** / **Index**. Text **导入视频** / **Import**.

**Home ready** (dialogue count > 0). **可搜索 1 段台词** / **1 searchable line**; **可搜索 %1$d 段台词** / **%1$d searchable lines**. **最近搜过** / **Recent**, up to 8 chips, hidden when empty. If the queue is also non-empty: **还有 1 个视频待索引** / **1 video still to index**; **还有 %1$d 个视频待索引** / **%1$d videos still to index**, and text **查看** / **View** opens the sheet. Toolbar `video_library` contentDescription **导入与索引** / **Import and index**. Info **关于** / **About**.

**Model preparing.** Not cancelable. Title `models_preparing`. Body `models_preparing_detail`. One label: **正在解压模型 %1$d%%** / **Unpacking model %1$d%%**, then **正在验证模型…** / **Verifying model…**. If `ready()` is already true, do not show the overlay and do not zero the bar. If a process-wide prepare is already running, show the verify line indeterminate instead of restarting at 0. Ignore progress after detach. Failure keeps the overlay with `models_storage_error` or `models_prepare_error`, filled **重试** / **Retry**, text **稍后** / **Not now** (hides the overlay; search and index then toast `index_need_embed`), and the existing report dialog.

**Indexing bar.** Column child, `surface_container_low`, text `on_surface`. **正在索引 %1$d/%2$d · %3$s** / **Indexing %1$d/%2$d · %3$s**. Second line, the whole time the job runs: **索引中，完成后可搜索** / **Search available after indexing**. Determinate bar only while embedding; otherwise indeterminate. The field stays editable. The disc is disabled, contentDescription **索引完成前无法搜索** / **Search is unavailable until indexing finishes**. A submit that still arrives toasts `index_busy` once. Tap the bar to open the sheet. User-visible stage strings, including the notification text: **正在准备索引…** / **Preparing to index…** (`index_engine_init`); **正在读取音轨…** / **Reading audio…**; **正在转写…** / **Transcribing…**; **正在整理 %1$s / %2$s** / **Organising %1$s / %2$s**. Channel name and **正在建索引…** / **Indexing…** stay. Success hold: **索引完成：1 段台词** / **Done: 1 dialogue segment**, or **索引完成：%1$d 段台词** / **Done: %1$d dialogue segments**. If that total is 0: **索引完成，未识别到台词** / **Done. No dialogue detected**. Haptic, 2s, fade, refresh the count. Cancel immediately shows **正在取消…** / **Cancelling…** (indeterminate) until the result, then **索引已取消** / **Indexing cancelled** for 2s. Error: `error` text on the same bar until **关闭** / **Dismiss**; **重试** / **Retry** if the remaining queue is non-empty (filled only while the disc is hidden). Announce a stage change, not each percent tick. Do not print `IndexResult.Error.message`.

**Searching.** After 150ms, three static-layout skeleton cards (16:9, two text bars, radius 16 / 8) then the pulse. `importantForAccessibility=no`. Back cancels and announces **已取消搜索** / **Search cancelled**. Late results with an old generation are dropped. Duplicate submit is ignored. Hide the IME on accept. If dialogue count is 0, do not embed; go straight to no-dialogue.

**Results.** **找到 1 个镜头** / **1 shot**; **找到 %1$d 个镜头** / **%1$d shots**. No millisecond string (`search_done` may stay in logs). `RecyclerView`, gap 12dp, inset 20dp. Card fill is `surface_container_lowest` in light and `surface_container` in dark, with the hairline. 16:9 thumb, full bleed, 16dp clip; badge bottom-start **mm:ss**; 12dp text pad; dialogue max 3 lines with both spans; low-confidence label only under the threshold, not focusable, text `low_confidence_hint`; meta **city-walk.mp4 · 00:32–00:36** (en dash). Filename ellipsizes; the time range does not. The card is one accessibility action: dialogue, filename, range, and the low-confidence phrase when shown. Thumb is not its own node. Announce the header politely when the list is bound. Fixture: query 日落 / sunset; line 一起去看日落吧。 / Let us watch the sunset together.; card 0 `city-walk.mp4` high; card 1 `weekend.mp4` score 0.71 with the label; thumb `ic_launcher_foreground`.

**No dialogue.** **还没有可搜索的台词** / **No searchable dialogue yet**. **导入一段视频后再搜** / **Import a video, then search.** Recent chips under that, if any.

**Search failed.** **搜索失败** / **Search failed**. **再试一次** / **Try again.** Filled **重试** / **Retry** resubmits the same query. Disc hidden. Announce the failure. This is not `index_cancelled`.

**DB unavailable.** **索引库未打开** / **Index database is not open**. Text **处理索引库** / **Fix the index** reopens the rebuild dialog. Later leaves this body and the dialog message in `error`.

**Playback.** Top-start `arrow_back` (**返回** / **Back**). 16:9 well `#000000`. On prepared, size the `VideoView` fit-center from the video aspect so portrait letterboxes and does not stretch. `MediaController` anchors to the well. Under it: highlighted line; **已跳到 %1$s** / **Jumped to %1$s**; `segment_start_unaligned` in `on_surface_variant` when the note is non-blank. `play_error` replaces that caption in `error`, with text **导入视频** / **Import**. Close stops and hides the surface, then fades the chrome 200ms.

**Import and index.** `BottomSheetDialog`, radius 16dp, surface `surface_container_highest`. Title **导入与索引** / **Import and index**, or **索引 %1$d / %2$d** / **Indexing %1$d / %2$d** while running. Empty: **还没有待索引的视频** / **No videos waiting**. Rows are the in-memory queue plus the current batch snapshot. Never rebuild rows from `segments`. 112×63 center-crop thumb, filename ellipsized, one stage line, 4dp progress on the active row. Queued rows have a 48dp **移除** / **Remove** (`close`). The active row does not. Text **导入视频** opens **相册** / **Gallery** and **文件** / **Files**. Filled **开始索引** / **Index** when some queued row is idle. While running, that button is gone and **取消** / **Cancel** is text (`stopIndex`). Toasts unchanged: `index_need_video`, `index_need_embed`, `index_busy`, `models_preparing`.

Row copy, only from callbacks observed this session: queued (filename); preparing **正在准备索引…**; active (stage); completed **已完成 · 1 段台词** / **Done · 1 line**, or **已完成 · %1$d 段台词** / **Done · %1$d lines**, or **未识别到台词** / **No dialogue detected** when that summary’s count is 0; failed **未能索引** / **Could not index**; cancelled **已取消** / **Cancelled**. On `Success`, drop every snapshot URI from the queue after the 2s hold, including zero-dialogue files. On `Cancelled` or `Error`, drop a URI only if a **later** video’s progress was observed. Do not mark the in-flight row complete. Picks during a batch append to the pending queue and are not sent to the running service. Reopen while `running`: one active row from `lastProgress`, no synthetic completed prefix. Thumbs: off the main thread, max edge 720px, center-crop, fallback `surface_container`, cancel on recycle, set the bitmap only if the row’s URI tag still matches. Do not change frame-time selection.

**About.** Info and title long-press. Title `about_licenses`. Scroll: `model_attribution` **without** the “长按标题…” / “Long press the title…” sentence, then `LICENSING.md`. Button OK.

**DB rebuild.** Keep `db_dialog_title`, opener message, **清空并重建** / **Wipe and rebuild**, **稍后** / **Later**, non-cancelable. Wipe then shows `db_rebuild_hint` and count 0.

**Crash dialog.** Unchanged 1.0.2 behavior and payload. `MaterialAlertDialogBuilder` so actions may stack at large type. Startup title `startup_error_title`, body `startup_failed` plus `CrashGuard.describe`, activity content a padded scroll of that text. Previous crash: `crash_report_title`, body = the crash file. **复制** / **Copy**, **分享** / **Share**, **关闭并删除** / **Dismiss and delete** (deletes only for the previous-crash dialog). Share failure toast `crash_share_unavailable`.

**Language.** Toolbar end, before the library icon. Zh shows **EN**; en shows **中**. `contentDescription` **切换语言** / **Change language**. Same `LocaleStore` toggle and `recreate()`.

Recents: `SharedPreferences`, queries that returned ≥1 hit, trimmed, exact dedupe, newest first, cap 8, store at most 80 characters. Chip `minHeight` 48dp, `maxWidth` 180dp, ellipsis. Tap fills and searches. Clear empties the field, cancels an in-flight search, and shows home. Plurals: `one` and `other` in en; zh uses `other`.

## 6. Screenshots and acceptance

Extend `BaselineScreenshotTest`: sdk 35, `w411dp-h891dp-xxhdpi`, bitmap 1233×2673, `Theme.Jingku` via `ContextThemeWrapper`, gates `jingku.screenshots` and `jingku.screenshotDir`. File `{state}-{zh|en}-{light|dark}.png`. Production layouts. Placeholder thumb. Dialogs use `captureScreenRoboImage`. Animator scale 0 so the pulse is a still frame.

States × light/dark × zh/en = 56: `home-empty`, `home-queued`, `home-with-library`, `home-pending`, `model-preparing` (42%), `indexing-progress` (weekend.mp4, 3/12, 45%, busy line visible), `searching`, `search-results`, `no-dialogue`, `search-failed`, `playback` (jumped to 00:32, unaligned gone), `import-sheet`, `about-dialog`, `crash-dialog` (current demo trace). Language is the toolbar label, not its own state.

- [ ] Hex table, transparent bars over `surface`, no `#5B5BD6`, `#7C4DFF`, `#C3BFFF`, `#33FFC107`, no elevation tint
- [ ] Task screens show no model names, cosine, milliseconds, or “Top-3”
- [ ] Low-confidence label uses the frozen threshold only; the card is opaque and one action
- [ ] Gallery and Files import; denial of notifications still indexes; cancel calls `stopIndex` and says **正在取消…** then **索引已取消**
- [ ] Search during an index does not open a second embedder; the disc is disabled; search cancel says **已取消搜索**
- [ ] Search failure has Retry; empty dialogue uses the no-dialogue copy; DB Later can reopen Fix the index
- [ ] Success removes finished URIs; cancel/error removes only URIs before the last observed index; process death invents nothing
- [ ] Playback seeks with `SEEK_CLOSEST`, letterboxes, hides the surface on close, and offers Import on `play_error`
- [ ] Language recreates zh↔en; About has attribution plus `LICENSING.md`; title long-press still opens it
- [ ] Crash dialog keeps three roles, non-cancelable, and deletes the file only on 关闭并删除
- [ ] 56 screenshots; wash on 日落 / sunset; low-confidence label only on the low card
- [ ] Clear, send, toolbar, chips, and remove are 48dp and do not overlap. Search outline meets 3:1. Dialog muted text meets 4.5:1
- [ ] Device pass, not Roborazzi: insets for gesture and three-button, IME versus the sheet, 320dp width, 200% font, TalkBack one node per card, rotation replays one progress

## 7. Later

Persistent video inventory (a new table) so zero-dialogue and failed files survive process death. `IndexResult` carrying partial summaries. Concurrent search while a batch embeds. TextureView, Compose, shared elements. A redistributable real-frame fixture. A relevance cutoff (would change who appears). An OEM storage-settings link.

## 8. Review resolution

| Item | Raised by | Decision | Reason |
|---|---|---|---|
| Ink primary; map both themes | both | Accept | Decision stands. Every Material role is set so seeds cannot leak |
| `ForegroundColorSpan` on the wash | both | Accept | Dark wash otherwise keeps unstyled body text. Rebind builds new spans |
| Pulse, delay, reduced motion | both | Accept | Linear alpha strobes. 150ms delay skips fast searches. Scale 0 is a still |
| State renderer and overlay precedence | Codex | Accept | Startup, then preparing, then playback, then body, one main-thread `render` |
| Drop the library flipper child | agy | Accept | The sheet is the only queue surface. A flipper also drops list state |
| Root `FrameLayout` overlays | Codex | Accept | Preparing and playback are bounded siblings. The bar is in the column |
| Sheet is the retained queue | Codex | Accept | Segments cannot rebuild a library. A video table is Later |
| Busy line and dimmed disc | both | Accept | The field stays editable. Submit waits, with the reason on screen |
| Mutex is open/reset only | Codex | Accept | Said explicitly. This pass still refuses a second embed |
| 48dp clear and send | both | Accept | 32dp is the glyph. Separate 48dp targets, no shared `TouchDelegate` |
| Rest stroke versus focus stroke | Codex | Accept | Rest `outline` is 4.47 / 4.45. Focus is 2dp `primary` |
| One search generation; hide IME | Codex | Accept | Duplicate submit is ignored. Accepted submit dismisses the keyboard |
| Passive low-confidence label | both | Accept | Frozen threshold, opacity 1, not a focusable chip |
| Verify line and real skip rule | Codex | Accept | One percent. Skip only for a verified embed plus speech assets |
| Ready home beats a pending queue | Codex | Accept | Pending copy and View sit on the ready home. Queued is only at count 0 |
| Queue removal rules | Codex | Accept | Success drops the snapshot. Cancel/error drops only URIs before the last observed one |
| No invented history on reopen | Codex | Accept | Replay `lastProgress` once. Process death restores the queue only |
| Search failed + Retry | Codex | Accept | A thrown search is its own body, with the query kept |
| Search-cancel copy | Codex | Accept | **已取消搜索**, and late results die with the generation |
| **正在取消…** | Codex | Accept | Index cancel copy until `onFinished`, then **索引已取消** |
| DB unavailable + Later | Codex | Accept | The rebuild dialog stays. Later leaves Fix the index |
| Edge-to-edge insets | Codex | Accept | Transparent bars, surface behind, insets on toolbar, bar, and sheet |
| Dialog muted text 4.13:1 | Codex | Accept | `#65625C` on `#E2DED4` is 4.52:1. Dark `#A8A396` on `#3A3834` is 4.65:1 |
| Card outline to `#D5D1C7` for 3:1 | agy | Reject | That pair is 1.40:1 on the page and 1.52:1 on white. Hairline `#A39D90` / `#6A645A` is 2.70 / 2.71 and stays decorative |
| `TextureView` instead of a fade | agy | Reject | No alpha on the `SurfaceView`. Hide it on close. A player rewrite is Later |
| Dark `lowest` lighter than `surface` | both | Accept | `lowest` is `#0E0D0B`. Cards use `#242220`. Elevation tint is off |
| Bar plus pill sandwich | agy | Accept | The IME hides the bar. The sheet pads for IME and navigation bars |
| Notification rationale screen | agy | Reject | Denial still starts the service, as today. The bar is the progress UI |
| Remove a queued video | agy | Accept | 48dp remove on non-active rows. No new table |
| Import action on `play_error` | Codex | Accept | The existing sentence stays and gains **导入视频** |
| Storage-settings link | agy | Reject | **稍后** closes the overlay. The 530 MiB sentence stays. OEM settings are Later |
| “No matching line” | Codex | Accept | With dialogue present, top-K returns hits. Zero dialogue has its own copy |
| Hint must not promise a scene | Codex | Accept | **一句台词，或它的意思** / **A line, or what it means.** |
| Zero dialogue and failed-video copy | Codex | Accept | Fixed strings from observed summaries. Raw `error.message` is logged, not shown |
| `index_engine_init` wording | Codex | Accept | Same resource, now **正在准备索引…** / **Preparing to index…** |
| Picker cancel; notification re-entry | Codex | Accept | Empty picker keeps the body. The intent only foregrounds the activity |
| Clear, IME back, recents, plurals | Codex | Accept | Specified in §5. Recents stay in `SharedPreferences` |
| About “long press” sentence | Codex | Accept | Removed from attribution. The gesture stays |
| Crash actions at large type | Codex | Accept | Material dialog may stack. Payload and delete rules stay |
| Callback lifetime | Codex | Accept | Main thread, detach on destroy, generation on the 2s hold |
| Stale result bitmaps | Codex | Accept | `RecyclerView` is fine at K=3. Match the URI tag before `setImage` |
| Thumb memory | Codex | Accept | Max edge 720, center-crop, off main. Seek math unchanged |
| Letterbox and ellipsis | Codex | Accept | Fit-center in a `#000000` well. The time range does not ellipsize |
| Fixtures versus device checks | Codex | Accept | 56 images cover theme and copy. The checklist covers insets, TalkBack, type, playback |
| Real-frame screenshot fixture | Codex | Reject | No redistributable frame is in the repo. Placeholder thumbs stay |
| One node per card; hide skeletons | Codex | Accept | Filename-only thumb descriptions would repeat the row |
| Reduced motion and haptic fallback | Codex | Accept | Static pulse. Haptics follow the system switch |
| Full theme, no lavender | Codex | Accept | Both themes, inverse surfaces, ripple, progress, overlay tint off |
| Calmer count line | Codex | Accept | **可搜索 N 段台词** / **N searchable lines** |
| Do not force a single filled control | Codex | Accept | One dominant control. The disc wins while the field has text |
| Two full cards above the fold | agy | Reject | Full-width 16:9 cannot stack twice on a phone. Text pad is 12dp |
| Chip `maxWidth` 180dp | agy | Accept | Long lines ellipsize inside a 48dp-tall chip |
| Blend the player well into the page | agy | Reject | Letterbox stays `#000000` so portrait aspect stays honest |
| Drop duplicate percent and vendor names | Codex | Accept | One percent. Model names stay in About |
| Multi-phase delivery | Codex | Reject | One XML pass. Inside it: theme, renderer, sheet, then motion |

## 9. Round 1 refinement (v1.1)

Recent chips draw at 48dp with LabelLarge and 16dp horizontal padding. Center the home cluster and recents in the available body. During indexing with no searchable lines show “正在索引视频 / Indexing videos” and the busy explanation; hide idle queue actions. No-dialogue has no Import button below the search disc. Metadata is contiguous filename plus a fixed middot/time range, both muted; only the filename may shrink. Secondary preparation, progress, queue and playback text uses on_surface_variant. Index title truncates to one line.

The screenshot-only placeholder is a full-bleed abstract 16:9 landscape vector. Real thumbnails are unchanged. Queue thumbs clip at 8dp and sheet actions align to the end. About uses localized readable summaries, with complete original legal texts still bundled. Crash trace uses selectable 12sp monospace in horizontal and vertical scrolling; Copy and Share retain the original trace and all three actions retain their roles.
