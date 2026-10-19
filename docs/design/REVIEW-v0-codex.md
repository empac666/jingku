# Jingku 1.0.3 — Codex review of v0
2026-10-20. Verdict: direction agreed; resolve P0 state/data contracts before implementation.
P0 = must change; P1 = should change; P2 = nice to have. Effort: S = hours; M = roughly 1 day; L = multiple days, including verification; estimates are provisional.
Read DESIGN.md, REFERENCES.md, and relevant production Activity, layouts, theme, strings, service/callbacks, database, model preparation and result-binding code. Source remained read-only; release/secrets was not read.
Viewed baseline PNGs: home-empty-zh-light, search-results-en-dark, indexing-progress-zh-light, crash-dialog-en-dark. They confirm the buried search, competing import buttons, technical copy, purple surfaces and permanent player well.

## §2 Decisions — every row

| Topic | Vote | Priority / effort | Alternative or implementation rationale |
|---|---|---|---|
| Primary | AGREE | P1 / S | Ink primary, no dynamic color; replace both day/night theme definitions and all inherited component roles, not only the two seeds. |
| Match mark | AGREE | P1 / S | Keep existing literal matching and bold span; add the dark foreground span explicitly. Reset spans on every bind; semantic hits need not contain the query literally. |
| Skeleton | CHANGE | P1 / S–M | Keep three placeholders and one 1000ms animator, but call it a pulse, reverse smoothly, and show a static version when animations are disabled. Delay appearance about 150ms to avoid flashing on fast searches; stop when hidden/destroyed. |
| Navigation | CHANGE | P0 / L | Keep one Activity, ViewFlipper and sheet; add one explicit UI-state renderer and overlay precedence: startup failure/dialogs > preparation > playback > body. Define Back/IME behavior for searching and no-results; hide search chrome and disable underlying input/accessibility during overlays. |
| Indexing | CHANGE | P0 / L | Keep the bar and active-row detail, retaining the busy guard. Show persistent “索引中，完成后可搜索 / Search available after indexing”; do not imply concurrent search. Define a batch snapshot and row-state ownership before promising a full library. EngineHolder's mutex guards open/reset, not the entire batch or embedText calls. |
| Submit | CHANGE | P0 / S–M | Keep IME + 32dp visual disc; reserve separate, non-overlapping 48dp targets for clear and submit. Use the specified ink focus stroke consistently (the token table currently assigns outline). Guard duplicate submissions; hide IME on accepted submit. |
| Low confidence | AGREE | P1 / S | Exactly score < 0.7889297604560853; no filtering, second tier or faded cards. Use a passive label styled as a chip rather than a focusable action. |
| Empty / prep | CHANGE | P1 / M | Agree 48dp icon, 24sp title and one bar. Show percent once; after copying reaches 100%, use “正在验证模型… / Verifying model…” until verification finishes. Skip only when verified model AND required speech assets are ready; file existence alone is insufficient. |

## Missing states and copy

- P0 / M: Specify home precedence when indexed dialogue and pending imports coexist. Queue currently survives success unchanged; define completed removal, failed/cancelled retention and whether new picks during a batch form a separate pending queue. Otherwise “Index” silently repeats completed work.
- P0 / L: “Library” has no current list API or video inventory table: DB stores segments; zero-dialogue and failed videos cannot be recovered from them. Lowest-effort 1.0.3 alternative: label the sheet “导入与索引 / Import and index” and scope rows to the retained queue; defer a persistent all-video library. A true library requires explicit data/API scope approval in the spec.
- P0 / M: Define queued/initializing/active/completed/failed/cancelled rows and “正在取消… / Cancelling…”. Callbacks retain only lastProgress/lastResult, not the full batch or per-row history; do not invent completed rows on reopen. Define Activity restoration, notification re-entry and process-death behavior without changing the service contract.
- P0 / S: Add search-failed + Retry, DB unavailable + recovery, and a search-cancel transition. Current search cancellation uses index_cancelled; do not tell users indexing was cancelled. Back must invalidate late search results as well as cancel work.
- P1 / S: “No matching line” is misleading: searchSemantic768 always returns the best available dialogue rows up to K, without a relevance cutoff. Use “还没有可搜索的台词 / No searchable dialogue yet” for an empty index; retain low-confidence results unchanged. The placeholder must describe dialogue meaning, not promise visual scene understanding.
- P1 / S–M: Add zero detected dialogue (“未识别到台词 / No dialogue detected”), unsupported/unreadable video, revoked access, partial batch failure, and persistent index-error Dismiss/Retry actions. Playback's “regrant access” needs a reachable Import action, not only an instruction.
- P1 / S: Name initializing-engine copy (index_engine_init still exposes internals), storage-error/retry presentation, and restoration after DB rebuild/Later. Specify notification denial continues indexing as today; picker cancellation returns to the prior state.
- P1 / S: Define clear during results/searching, new-query replacement, IME Back before route Back, no-results→home, and recent-query trimming/deduplication/max length. Add zh/en plurals for one video/shot/segment.
- P1 / S: About should contain clean attribution, removing its current “Long press the title…” instruction. Keep crash payload and deletion behavior; permit stacked dialog actions at large font sizes.

## XML Views feasibility and effort risks

- P0 / M: ViewFlipper alone does not model simultaneous preparation, DB loading, indexing and search. Replace scattered setStatus calls with explicit rendering; marshal callbacks to main, detach on destroy, replay retained state once, and prevent old callbacks/2s timers from overwriting a new job or errors.
- P0 / M: Use a root FrameLayout for actual overlays; constrain toolbar/search/body/bar with explicit layout behaviors or constraints. The structure sketch omits body offsets and overlay bounds. Reserve bar space so it cannot cover results; handle IME, cutout and navigation insets in the sheet too.
- P0 / M: Theme colors alone cannot guarantee system-bar appearance under target-35 edge-to-edge. Draw surface behind transparent bars, apply insets and set icon appearance; verify gestures and three-button navigation. [Android Views guidance](https://developer.android.com/develop/ui/views/layout/edge-to-edge).
- P1 / M: VideoView uses SurfaceView: do not make a perfect video alpha fade an acceptance requirement. Fade the surrounding overlay or a still cover; stop/hide the surface and reset playingUri/preparedPlayer on close so reopening the same URI prepares correctly. Verify background/foreground, controller anchoring and Back on a device. [SurfaceView reference](https://developer.android.com/reference/android/view/SurfaceView).
- P1 / M: RecyclerView is feasible but new adapter/recycling work versus today's at-most-three LinearLayout cards; it is not a scalability blocker with frozen K=3. If retained, cancel thumbnail jobs on recycle and check row identity before applying posted bitmaps; current binder can post stale images.
- P1 / M: Current VideoFrameThumb defaults to 320px max edge and decodes before downscaling; full-width images can look soft and library batches can spike memory. Specify bounded off-main loading, visible-row loading, a neutral fallback, crop policy and cache budget; preserve seek math.
- P1 / M: Synchronous bundled extraction can outlive Activity coroutine cancellation; define one preparation owner or safely reattach to the existing operation after recreate. Ignore old progress callbacks; do not show a 0→100 flash when verified assets are already ready.
- P1 / S–M: Implement 16:9 via constrained image bounds and a black player container that preserves video aspect ratio. Portrait video must letterbox, not stretch; filenames need ellipsis while time ranges stay visible.
- P1 / M: The 48 screenshot fixtures verify geometry/theme, not MediaController, permissions, restoration or actual playback. Add focused device checks for those paths plus 320dp/landscape, 200% font, TalkBack and keyboard navigation; freeze pulse/progress for deterministic captures.

## Contrast and accessibility — calculated from the supplied sRGB hex values

Ratios below are light / dark. Small text needs 4.5:1; essential component boundaries/state cues need 3:1 where required. Decorative hairlines need not meet that boundary rule. [Text criterion](https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html), [non-text criterion](https://www.w3.org/WAI/WCAG22/Understanding/non-text-contrast).

| Foreground / background | Ratio L / D | Assessment |
|---|---|---|
| on_surface / surface; inverse primary button pair | 15.78 / 16.57 | Pass. |
| on_surface_variant / surface | 5.09 / 7.50 | Pass for hints, filenames and labels at full opacity. |
| on_surface_variant / surface_container_highest | 4.13 / 5.01 | P0 / S: light dialog small muted text fails; use on_surface or darker muted text there. |
| low_confidence / low_confidence_container | 4.88 / 6.95 | Pass at 12sp; being a label does not grant a lower contrast threshold. |
| highlight_match_on / highlight_match | 13.87 / 8.71 | Pass; warm wash plus bold is sufficient without colored body text. |
| on_secondary / secondary (also tertiary) | 5.09 / 7.50 | Pass; map on-container roles explicitly as well. |
| error / surface; on_error / error; on_error_container / error_container | 5.99 / 11.05; 6.53 / 7.66; 9.95 / 7.17 | All pass. |
| outline_variant / surface; search fill / surface | 1.20 / 1.61; 1.09 / 1.10 | P1 / S: retain subtle card decoration, but give the search control an identifiable boundary; do not rely on these pairs alone. |
| outline / search fill | 4.47 / 5.34 | Pass for control boundary; ink focus also passes. Resolve which token is authoritative. |
| badge_on / scrim over white worst-case still | 5.74 / 5.74 | Pass: #99000000 composites to #666666 over white; no additional badge opacity reduction. |

- P0 / S–M: Fixed 56dp must be a minimum at large font; toolbar actions, clear/send and chips need 48dp touch targets without overlapping TouchDelegates. Provide search/library/info/action labels, explicit focus indicators and readable disabled states.
- P1 / S: Group each result into one actionable accessibility node including dialogue, filename, time and low confidence; a filename-only thumbnail description otherwise repeats metadata. Hide skeleton/overlay-obscured nodes; move and restore focus when opening/closing sheets and playback.
- P1 / S: Announce search completion/errors and stage changes politely; expose progress semantics without announcing every percentage tick. Respect disabled animations and haptic settings; guard version-specific CONFIRM haptics with a fallback.

## Dark mode and the elegance goal

- P1 / S–M: Define colorBackground/windowBackground, surfaceVariant compatibility, inverse roles, ripple/disabled states, progress track/indicator and dialog/sheet theme overlays; otherwise inherited Material defaults can reintroduce lavender. Override both existing themes, not only values/colors.xml.
- P1 / S: Dark surface_container_lowest (#1C1B19) is brighter than low (#1A1917), contrary to the role names. Use explicit surface roles consistently; disable unintended elevation tint so dialogs, sheets and cards match the approved palette.
- P1 / S: The bar and sheet can remain available during indexing, but an apparently usable search that repeatedly toasts “busy” feels broken. Persistent explanatory copy preserves trust without changing engine concurrency.
- P1 / S: Avoid technical “dialogue segments indexed” as the home hero; “可搜索 128 段台词 / 128 searchable lines” is calmer. Keep exact counts and technical diagnostics secondary.
- P1 / S: Large stills support the goal only when they contain recognizable footage. Launcher-logo fixtures cannot approve image balance or dark scrim behavior; add a redistributable real-frame fixture alongside deterministic placeholder tests.
- P2 / S: Do not enforce “one filled action” mechanically: a nonempty search disc plus Import/Retry can legitimately coexist. Define one dominant action per context, use ripple for cards, and reserve scale animation for compact controls.
- P2 / S: Keep the quiet canvas, warm ink, dialogue-first hierarchy and no vendor names/scores. Duplicate percentages, perpetual skeleton pulses, multiple stage counts and three toolbar utilities compete with that restraint; remove redundant copy before adding motion.

Implementation recommendation: ship theme + search shell/cards first, then the explicit state/overlay model and scoped import sheet, then motion and fixture expansion. Overall M–L work across several days; full persistent library and concurrent search are separate functional work, not XML polish.
