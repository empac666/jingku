Reviewer: agy (gemini-3.8-flash-high stand-in; Opus 429)

## 1. Overall Score: 8.5/10 (vs Baseline 3.5/10)
A massive generational leap from 1.0.2's cluttered, engineer-centric form UI to a clean, cinematic AI search experience (ChatGPT/Claude/Perplexity vibe). Centered hero pill (56dp), warm paper palette (#F6F5F1/#12110F), full-bleed 16:9 cards, clean overlays, and zero technical clutter. Minor visual hierarchy and layout polish remain.

## 2. Spec Deviations
- `search-results`: Filename and time range split across card width (`city-walk.mp4 ·` on left, `00:32–00:36` on right); spec §5 requires contiguous meta `filename · range`.
- `indexing-progress`: Center body renders idle "已选 12 个视频" + disabled text "开始索引" while bottom bar is actively indexing 3/12; contradicts single dominant action and active state rules (§1, §5).
- `about-dialog`: Raw Markdown table syntax (`| --- |`) dumped unparsed into plain text dialog (§5 line 141); English locale displays untranslated Chinese legal audit body.
- Metadata typography: `textVideoName`, `textMeta`, `indexBusy`, `prepDetail`, `playNote` use `on_surface` (#1C1B19/#F3F0E8) instead of spec token `on_surface_variant` (#65625C/#A8A396).
- `playerWell`: Hardcoded to 220dp in XML instead of using dynamic 16:9 `AspectFrame`.

## 3. Per-Screenshot Findings & Concrete Fixes
- `search-results-*.png`:
  - Issue: Meta row split by `layout_weight="1"`; dangling dot `·` before void; color is `on_surface`.
  - Fix: `item_search_result.xml` set `textVideoName` `wrap_content`, `textColor="@color/on_surface_variant"`; bind single text `"$name · $range"` with middle ellipsis on name.
- `indexing-progress-*.png`:
  - Issue: Mid-screen displays ghost idle queue ("已选 12 个视频", "开始索引") during active background index.
  - Fix: `JingkuScreen.kt` guard `queued` body with `!s.running`; show indexing placeholder or hide action buttons while `s.running == true`.
- `import-sheet-*.png`:
  - Issue: Bottom actions (`导入视频`, `开始索引`) clustered on bottom-left leaving right half void; queue thumbnails have sharp 0dp corners.
  - Fix: `sheet_import.xml` button bar set `gravity="end"` or `space_between`; `item_queue.xml` wrap `queueThumb` in 8dp rounded card (`@style/Jingku.Small`).
- `about-dialog-*.png`:
  - Issue: Raw Markdown tables (`| --- |`) unreadable on mobile; English mode (`about-dialog-en-*.png`) has Chinese body text.
  - Fix: Strip table pipes into formatted key-value lines; provide localized `LICENSING-en.md` for `LocaleStore.isEn()`.
- `home-with-library-*.png` & `no-dialogue-*.png`:
  - Issue: Centered count headline ("可搜索 128 段台词") sits above left-aligned "最近搜过" chips, creating lopsided optical weight.
  - Fix: Center `recentTitle` and `recents` `ChipGroup` via `layout_gravity="center_horizontal"`, or left-align both with 20dp margin.
- `crash-dialog-*.png`:
  - Issue: In English (`crash-dialog-en-*.png`), "Dismiss and delete" crowds 3-button horizontal row without hierarchy.
  - Fix: Shorten EN label to "Dismiss" or "Delete report"; set Copy as filled/elevated primary.
- `home-empty-*.png` & `model-preparing-*.png`:
  - Issue: Secondary description text (`prepDetail`, `bodyDetail`) uses `on_surface`, flattening hierarchy against title.
  - Fix: Set `android:textColor="@color/on_surface_variant"` (#65625C / #A8A396) in `activity_main.xml`.

## 4. Clipping, Overlap, Contrast & i18n
- Contrast: Light theme `on_surface_variant` (#65625C) on `surface` (#F6F5F1) gives 5.57:1 (passes WCAG AA). Using `on_surface` on secondary text was 15.78:1 (too heavy, zero visual hierarchy).
- i18n / English expansion: "Dismiss and delete" is 18 chars, crowding the dialog row. Search hint "A line, or what it means." fits well in 56dp pill. "Search available after indexing" fits without truncation.
- i18n / Untranslated text: `about-dialog-en-*.png` displays 100% Chinese legal markdown in an English interface.
- Clipping / Overlap: Clear (48dp) and Search Send (48dp target, 32dp icon) are cleanly separated, no overlap detected. 16dp card corner radius clips cleanly.

## 5. Top 10 Prioritised Fixes (P0 / P1 / P2)
- **P0 (Critical Polish & State Glitches)**:
  1. [P0] `indexing-progress`: Hide idle "开始索引" and "已选 N 个视频" body prompt when `s.running == true` in `JingkuScreen.kt`.
  2. [P0] `search-results`: Fix meta row layout in `item_search_result.xml` — keep `city-walk.mp4 · 00:32–00:36` contiguous, don't strand trailing dot on left.
  3. [P0] `about-dialog`: Provide English `LICENSING-en.md` for EN locale and strip markdown table syntax (`| --- |`) into readable key-value bullets.
- **P1 (Visual Hierarchy & Material 3 Polish)**:
  4. [P1] `tokens / hierarchy`: Change secondary texts (`textVideoName`, `textMeta`, `indexBusy`, `prepDetail`, `playNote`, `queueStage`) to `@color/on_surface_variant`.
  5. [P1] `import-sheet`: Align bottom action buttons to `gravity="end"` (or space-between), avoiding empty void on bottom-right.
  6. [P1] `import-sheet`: Wrap `queueThumb` in 8dp rounded corner shape (`@style/Jingku.Small`) instead of sharp 0dp rectangle.
  7. [P1] `home-with-library`: Align `recentTitle` and `recents` ChipGroup symmetrically with centered headline.
- **P2 (Layout Refinement & UX Nuances)**:
  8. [P2] `crash-dialog`: Shorten EN "Dismiss and delete" to "Delete report" or stack dialog actions to prevent cramped button bar.
  9. [P2] `playback`: Replace fixed `220dp` with `AspectFrame` (16:9) in `activity_main.xml` for responsive video well sizing.
  10. [P2] `search-results`: Harmonize placeholder thumbnail background (#EDEAE3) with card fill (#FFFFFF) to prevent two-tone look in light mode.
