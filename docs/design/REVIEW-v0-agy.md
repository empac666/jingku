Reviewer: agy (gemini-3.8-flash-high; Claude Opus quota-blocked)

# Review: Jingku 1.0.3 UI Spec (v0)

## 1. §2 Decisions Table Evaluation
1. **Primary**: **AGREE**. Ink primary (`#1C1B19` / `#F3F0E8`) strips the generic purple template feel and lets thumbnail frames own the chroma, matching Claude/ChatGPT neutrality.
2. **Match mark**: **CHANGE**. Alternative: Keep warm wash behind words, but mandate adding `ForegroundColorSpan(highlight_match_on)` (`#F0D7A2`) in `DialogueHighlighter`. Reason: Current code only applies `BackgroundColorSpan`; in dark mode, text inherits `on_surface` or default span styling, failing intended highlight contrast.
3. **Skeleton**: **CHANGE**. Alternative: Alpha pulse using M3 standard decelerate/sine easing (`PathInterpolator(0.4, 0, 0.2, 1)`) instead of linear. Reason: A 1000ms linear alpha pulse reads as a flashing/strobing glitch rather than a calm, breathing placeholder (Linear/Claude).
4. **Navigation**: **CHANGE**. Alternative: Drop child 2 (`library`) from `homeFlipper` (keep Library strictly as `BottomSheetDialog`); replace `ViewFlipper` with explicit `FrameLayout` view-swapping managed by a simple `ViewState` enum. Reason: `ViewFlipper` child 2 contradicts §4/§5, loses `RecyclerView` scroll/focus state, and complicates back-stack handling.
5. **Indexing**: **AGREE**. Rejecting concurrent native search is honest and protects the engine mutex. Alternative refinement: Visibly disable/dim the send disc during active indexing so the user understands search is paused before typing and hitting an unexpected `index_busy` toast.
6. **Submit**: **AGREE**. Dual IME `actionSearch` + 32dp send disc handles diverse Android OEM keyboards while maintaining "one primary". Dropping bounce overshoot keeps it calm and editorial.
7. **Low confidence**: **AGREE**. Stick strictly to frozen `SEMANTIC_LOW_CONFIDENCE` (0.789) and single `Assist` chip. Card opacity must stay 1.0 (dropping opacity makes hits look disabled/unclickable).
8. **Empty / prep**: **AGREE**. 24sp headline avoids small-screen crowding. Progress bar plus percent is clean and eliminates developer jargon from onboarding.

## 2. Prioritised Critiques & Gaps

### P0 (Must Change — Blocks A11y, Visual Contrast, or Runtime Stability)
- **A11y Touch Target Violation**: Send disc is specified as 32dp (`send_size 32dp`), directly violating WCAG 2.5.5 and M3 minimum touch target (48dp). Must wrap in a 48dp target container or set `TouchDelegate`.
- **Card Boundary & Contrast Collapse**: In light mode, `#FFFFFF` card on `#F6F5F1` surface is 1.09:1 contrast; 1dp `#E4E1D8` outline is only 1.20:1 (fails WCAG 1.4.11 3:1 non-text boundary requirement). Cards dissolve into the canvas on bright mobile screens. In dark mode, `#1C1B19` card on `#12110F` is 1.07:1. Fix: Darken light outline to `#D5D1C7` (2.0:1+) or use subtle surface tinting (`#FAF8F5`).
- **VideoView / SurfaceView Punch-Through**: `VideoView` wraps `SurfaceView`. In XML-Views single Activity, animating `overlayPlayback` with alpha (spec line 73: 200ms fade) causes black punch-through artifacts and clips over `BottomSheetDialog`. Fix: Use `TextureView` or toggle player visibility without alpha fade.
- **Match Highlight Span Bug**: Spec defines `highlight_match_on` (`#1C1B19` light, `#F0D7A2` dark), but existing `DialogueHighlighter` only sets `BackgroundColorSpan`. Dark mode highlight text will not render correctly without `ForegroundColorSpan`.

### P1 (Should Change — Architectural Integrity, Dark Mode & Missing UX)
- **Dark Mode Surface Hierarchy Inversion**: Token table sets `surface_container_lowest` to `#1C1B19`, which is *lighter* than `surface` (`#12110F`). In M3, cards should use `surface_container` (`#201F1C`) or `high` (`#2A2824`) to establish authentic elevation depth rather than a murky void.
- **Single-Activity Risks**:
  - `ViewFlipper` does not manage Android back-stack or retain `RecyclerView` scroll/focus across child transitions.
  - Soft keyboard (IME) open/close state collides with pinned pill and bottom sheet if `adjustResize` is untamed.
  - Activity `recreate()` on language switch can leak dialog windows or desync with `IndexForegroundService`.
- **Missing States & Copy**:
  - *Android 13+ Notification Permission Denied*: `POST_NOTIFICATIONS` is required for foreground indexing; missing rationale copy/fallback if rejected.
  - *Queue Item Removal*: No way to remove an accidental video from `importQueue` before starting index.
  - *Playback Error Recovery*: `play_error` leaves red text on caption without a CTA to re-grant SAF access or choose another file.
  - *Permanent Storage Error*: If device has <530MB, clicking "重试" loops infinitely; needs a clear exit or link to storage settings.
  - *Search DB Failure State*: If SQLite read fails during search, spec has no error flipper child (only no-results).
- **Viewport Sandwiching**: Simultaneous 56dp pinned top search and 56dp bottom index bar severely compress the usable viewport on 16:9/20:9 screens. Bottom bar must consume `WindowInsetsCompat` navigation insets.

### P2 (Nice to Have — Refinement Toward AI Elegance)
- **Result Card Density**: Full-width 16:9 thumbnail makes each card ~330dp tall, showing only ~1.5 cards per screen. Tighten vertical padding to 12dp so at least 2 full hits fit above the fold (Perplexity style).
- **Recent Query Chip Overflow**: Long dialogue queries (15+ CJK chars) create excessively wide chips. Set `maxWidth=180dp` with ellipsis to avoid breaking horizontal chip flow.
- **Pure Black Video Well Clashing**: `#000000` player well against warm `#12110F` dark canvas creates a harsh cutoff; softening or blending borders elevates cinematic feel.
