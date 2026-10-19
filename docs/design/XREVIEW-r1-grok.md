# Jingku 1.0.3 — round 1 cross-review (Grok)

**Score: 7.5 / 10** vs baseline. Baseline is one purple form: search below the fold, cosine on the card, tiny thumbs, model names on Home. Round 1 is a product surface: page `#F6F5F1` / `#12110F`, ~56dp pill, ink primary, empty copy, 16:9 wells, both highlight spans, no cosine on screen. Light and dark match. The still, the 32dp chips, the result meta, and the indexing empty state keep it under 9.

Sampled and holding: rest stroke `#7A776F`, hairline `#A39D90`, wash `#F3E6C4` / foreground `#F0D7A2`, chip fill `#E8E4DA`, progress track `#E2DED4` at 45% and ~4dp, disabled disc α 0.38 (rgb 168 on white). Clear and send are separate 48dp targets. Disc face is 32dp. En dash in the range. Low-confidence only on card 1. No `#5B5BD6`.

## Per screenshot

Same issue across zh/en and light/dark unless named.

- `home-empty-*` `home-queued-*` `home-with-library-*` `home-pending-*` `no-dialogue-*` `search-failed-*` — cluster starts after `paddingTop` 40dp; the lower half is empty. Fix: empty/queued/failed inner column `match_parent` + `gravity=center`. Recent stays under the cluster. English titles fit, including “No searchable dialogue yet”.
- `home-with-library-*` `home-pending-*` `no-dialogue-*` — Recent pills draw **32dp** (`y` 726–821). `minHeight` is 48; `chipMinHeight` is still the M3 32dp default. Fix: `chipMinHeight=48dp`, horizontal padding 16dp, LabelLarge, `maxWidth` 180dp kept.
- `no-dialogue-*` — text **导入视频 / Import** sits under the filled disc. That paragraph is title + sentence + chips. Fix: hide `btnPrimary` when `noDialogue`.
- `model-preparing-*` — pass. One 4dp bar, one percent, centered. English body wraps to two lines. No model names.
- `indexing-progress-*` — bar copy, 45% ink, busy line, and dimmed disc are right. The body still says “索引之后就能按台词搜索” and an enabled text **开始索引 / Index**. Fix: while `running`, `btnPrimary.isEnabled=false` (α 0.38) or hide it. `indexTitle` needs `maxLines=1` and `ellipsize=end` so a long name does not grow the bar.
- `searching-*` — three 16:9 skeletons; the third is cut by the screen (accepted). Light fill `#EDEAE3` on `#F6F5F1` is faint. Fix: skeleton `surface_container_high` `#E8E4DA` / dark `#3A3834`.
- `search-results-*` — well is 16:9 (~369×207dp, 16dp top clip, 12dp text pad). `ic_launcher_foreground` paints a ~206×193dp white plate and **~82dp `#EDEAE3` gutters**; `00:32` sits in the gutter. Fix: scale the glyph box (24,28)–(84,84) to the view, or use a full-bleed 16:9 plate. Meta reads `city-walk.mp4 ·` then a wide gap then `00:32–00:36`, both `#1C1B19`. Fix: name only; `textMeta` = `· 00:32–00:36`; both `textColor=@color/on_surface_variant`. “May be inaccurate” fits its own row.
- `playback-*` — pass. Well `#000000`, back, highlighted line, “已跳到 / Jumped to 00:32”. Empty space under the caption matches the spec.
- `import-sheet-*` — 112×63 slot, same unfilled glyph (white plate ~61×41dp). Idle rows correctly show filename + remove. Fix: the same full-bleed plate. Scrim and 16dp corners are fine.
- `about-dialog-*` — attribution is clean. `LICENSING.md` is raw; tables and URLs wrap mid-token. The OK rule does not cut glyphs. Fix: 12sp monospace, `breakStrategy=simple`, horizontal scroll.
- `crash-dialog-*` — **Copy / 复制** is fully on screen. Order Share · Dismiss and delete · Copy matches 1.0.2. The trace splits the identifier: `prepare(M` / `odelStore.kt:15)` on all four. Baseline broke at `prepare(`. Fix: ZWSP after `.` and `(`, or a horizontal-scroll monospace message.

## Spec deviations

- Drawn chip height is 32dp. §5 and the acceptance line say 48dp.
- Filename is `on_surface`. §3 assigns filename to `on_surface_variant`.
- Indexing Home still offers **开始索引** while the bar is the live status.
- No-dialogue shows Import, which that paragraph does not list.
- `result_meta_format` still contains “余弦 / cosine”. The binder does not use it. Delete it.
- The 16:9 well matches the spec. The agreed placeholder does not fill it, so every result and sheet still reads as a sticker.

## Clipping / i18n

No CJK overflow. English titles, the hint, “Dismiss and delete”, and “May be inaccurate” fit at 411dp. The crash identifier split is the hard wrap. About URLs wrap; OK does not cover them.

## Top 10

1. **P0** `chipMinHeight` 48dp on Recent chips.
2. **P1** Full-bleed plate in the 16:9 well and the 112×63 row (`search-results-*`, `import-sheet-*`).
3. **P1** Middot moves onto the time view; filename and range use `on_surface_variant`.
4. **P1** While indexing, disable or hide Home **Index** (`indexing-progress-*`).
5. **P1** Crash message must not break `ModelStore` (`crash-dialog-*`).
6. **P2** Vertically center empty, queued, and failed clusters.
7. **P2** Light skeleton fill `#E8E4DA`.
8. **P2** About: monospace + horizontal scroll for `LICENSING.md`.
9. **P2** `indexTitle` `maxLines=1` `ellipsize=end`.
10. **P2** Hide no-dialogue Import; delete unused `result_meta_format`.
