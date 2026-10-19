# 镜库 Jingku — Borrowable Design Language

> Design research synthesis, Oct 2026. 10 reference apps → 15 implementable rules for Material 3 / Android XML Views.

---

## Current UI Diagnosis

| Problem | Cause |
|---------|-------|
| Everything on one scrolling page | No screen separation; model prep, import, search, results, player are all stacked |
| Search field buried below the fold | User must scroll past model status + import to reach the core feature |
| Tiny thumbnails (96×72 dp) | Horizontal card layout undersells the visual nature of B-roll |
| Full-width "搜索" button below field | Form-like, not search-like; wastes vertical space |
| No empty state design | Just plain "尚未选择视频" text — no illustration, no guidance |
| Raw cosine scores shown to user | "余弦 0.920" is meaningless to non-technical users |
| "可能不准确" as italic colored text | Should be a Material 3 chip/badge — current feels like a warning label |
| Dark mode = inverted light mode | No surface ladder, no warmth, no personality |
| Generic purple accent (`#5B5BD6`) | No clear brand identity; feels like a default template |

---

## Reference Apps Studied

| App | Why relevant | Key takeaway |
|-----|-------------|--------------|
| **ChatGPT** | Search-like composer, streaming results | Radical subtraction; content-first neutrality |
| **Claude** | Warm dark mode, editorial craft | Cream/terracotta warmth; `#FAF9F5` light, `#141413` dark |
| **Perplexity** | Search-first AI with citations | Warm off-white `#fdfbfa`; inline source cards; follow-up bar |
| **Arc Search** | Bottom-anchored search, haptic choreography | Spring physics; progressive reveal; magazine-style results |
| **Google Photos** | M3 SearchBar, video thumbnails, filter chips | 56 dp pill bar; duration badge; shimmer skeleton; dynamic color |
| **Apple Photos** | Natural-language search, grid results | Pinch-to-zoom density; smart suggestions before typing |
| **Linear** | Dark-mode mastery, motion craft | Surface ladder; single-accent discipline; optimistic UI |
| **Grok** | Chat + search hybrid | Dark-first `#050505`; suggestion pills; strict 8 dp grid |
| **Raycast** | Command-palette search UX | Oversized search input; rich result rows; keyboard-first |
| **Notion** | Block-based AI output, mobile search | Full-screen search modal; breadcrumb context in results |

Sources: material.io, open-design.ai, mobbin.com, linear.app, arc.net, setproduct.com, fudge.design,
uxdesign.cc, designmd.co, saasui.design, byq.supply, raycast.com, spell.sh, nngroup.com

---

## 15 Concrete Rules for Jingku

### R1 · Search field is the hero (← Google Photos, Perplexity, Arc Search)

- Use M3 `SearchBar`: **56 dp height, fully rounded pill** (`ShapeDefaults.Full` = 28 dp radius)
- Place it **at the very top** of the home screen, 24 dp side margins
- Placeholder: `"搜一句台词…"` / `"Search a line of dialogue…"`
- Leading icon: search magnifier. Trailing: clear button (visible when text present)
- Remove the separate "搜索" button — submit on IME action `actionSearch`
- On focus: expand with M3 Expressive Fast Spatial spring (350 ms, overshoot)

```xml
<!-- Token reference -->
android:layout_height="56dp"
app:shapeAppearance="@style/ShapeAppearance.Material3.Corner.Full"
android:layout_marginHorizontal="24dp"
```

### R2 · Warm, not cold (← Claude, Perplexity)

Replace harsh whites/blacks with warm neutrals. The app handles video — a warm palette feels cinematic.

| Role | Light | Dark | M3 token |
|------|-------|------|----------|
| Canvas / background | `#FAF8F5` | `#141210` | `colorSurface` |
| Search bar fill | `#F0EDE8` | `#1E1C18` | `surfaceContainerHigh` |
| Card surface | `#FFFFFF` | `#252320` | `surfaceContainer` |
| Elevated card | — | `#302E2A` | `surfaceContainerHighest` |
| Primary text | `#1C1B18` | `#E8E2D9` | `onSurface` |
| Secondary text | `#6D6860` | `#A69E94` | `onSurfaceVariant` |

Use a **warm brown-tinted neutral** palette (like Claude's charcoal-brown `#141413`),
NOT cold blue-grays (current M3 default) or pure `#000000`.

### R3 · Single accent discipline (← Linear)

Pick **one** brand accent. Current `#5B5BD6` is okay but clashes with secondary `#7C4DFF`.
Drop the secondary. Use the accent **only** for:

- Search bar focus ring / active state
- Primary action buttons
- Matched-text highlight in results
- Active tab / selected chip

Suggested accent: keep `#5E5AD6` (indigo-violet) — close to Linear's `#5E6AD2`.
Generate full M3 tonal palette from this single seed via Material Theme Builder.

| Role | Light | Dark |
|------|-------|------|
| `colorPrimary` | `#5250A2` | `#C5C0FF` |
| `colorPrimaryContainer` | `#E3DFFF` | `#3A3790` |
| `colorTertiaryContainer` | — | — (unused; avoid) |

### R4 · Surface ladder, not flat inversion (← Linear, Google Photos)

Dark mode uses 5-step surface elevation:

```
surfaceDim         #0E0D0A   ← status bar, system bars
surface            #141210   ← main canvas
surfaceContainerLow    #1A1816   ← recessed areas
surfaceContainer       #1E1C18   ← search bar background
surfaceContainerHigh   #252320   ← result cards
surfaceContainerHighest #302E2A  ← dialogs, tooltips
```

Each step is ~6-8 L* apart in LCH. NEVER use pure `#000000`.

### R5 · Result cards: big thumbnail, vertical stack (← Perplexity, Google Photos)

Replace the current horizontal 96×72 dp thumbnail + text layout:

- **Card width:** full-width (match_parent minus 24 dp margins)
- **Thumbnail:** 16:9 aspect ratio, **full card width**, top of card. `ShapeAppearance.Material3.Corner.Medium` (12 dp top corners)
- **Content below thumbnail:** 16 dp padding. Video name (Title Small, 14 sp), dialogue text (Body Medium, 14 sp), timestamp pill
- **Duration badge** on thumbnail: bottom-right, white text on `#000000` @ 50% alpha, Label Small (11 sp), 4 dp corner radius
- **Card corner radius:** 16 dp (`ShapeAppearance.Material3.Corner.Large`)
- **Card spacing:** 12 dp vertical gap between cards
- **No elevation shadow** — use 1 dp hairline border `surfaceVariant` @ 12% alpha (← Linear)

### R6 · Low-confidence as a proper chip (← Material 3 spec)

Replace italic colored text with M3 `Chip`:

```xml
<com.google.android.material.chip.Chip
    style="@style/Widget.Material3.Chip.Assist"
    app:chipIcon="@drawable/ic_info_outline_18"
    app:chipBackgroundColor="?attr/colorErrorContainer"
    android:textColor="?attr/colorOnErrorContainer"
    android:text="可能不准确" />
```

Height: 32 dp. Corner radius: 8 dp. Icon: 18 dp. Typography: Label Large (14 sp).
Use `colorErrorContainer` / `colorOnErrorContainer` — amber/warm-orange tones, NOT red.

### R7 · Hide cosine scores; show confidence tiers (← Perplexity, Arc Search)

Users don't understand "余弦 0.920." Replace with:

| Cosine range | Display | Visual |
|-------------|---------|--------|
| ≥ 0.85 | (nothing — high confidence is the default) | No chip |
| 0.70–0.84 | `可能不准确` chip | `colorErrorContainer` chip |
| < 0.70 | `仅供参考` chip | Muted `surfaceVariant` chip, lower card opacity (0.72) |

Timestamp format: `01:23` not `00:01:23 – 00:01:26`. Show range only on tap/expand.

### R8 · Empty state with purpose (← Google Photos, ChatGPT, Linear)

When no videos are imported, show a centered empty state:

1. **Illustration:** A single-color outlined icon (film reel + magnifier), 80 dp, tinted `onSurfaceVariant`
2. **Headline:** `"导入视频，搜索台词"` — Headline Small (24 sp), centered
3. **Body:** `"从相册或文件导入视频，镜库会转写所有对白"` — Body Medium (14 sp), `onSurfaceVariant`
4. **CTA button:** `"选择视频"` — M3 FilledTonalButton, 40 dp height, full corner radius

No technical jargon. No model status. No "尚未选择视频" plain text.

### R9 · Model preparation as onboarding overlay (← Arc Search, Notion)

First-launch model unpacking should NOT be an inline section. Use a **full-screen overlay / bottom sheet**:

- Center: subtle animated progress ring (indeterminate circular, 48 dp)
- Headline: `"正在准备离线引擎…"` (Headline Small, 24 sp)
- Body: `"首次启动需解压模型，此后完全离线"` (Body Medium)
- Determinate progress bar below (M3 LinearProgressIndicator, 4 dp track height)
- **No model names** (EmbeddingGemma, SenseVoice) — those are developer details

Once ready, overlay dismisses → home screen with search bar.

### R10 · Shimmer skeleton for search results (← Google Photos, Linear)

While searching (typically < 500 ms on-device, but can be 1-2 s):

- Show 3 placeholder cards matching R5 layout geometry
- Shimmer: diagonal gradient sweep, 1200 ms cycle, `LinearEasing`
- Light: base `#E8E4DE`, highlight `#F5F2ED`
- Dark: base `#252320`, highlight `#353230`
- Transition to real content: `AnimatedVisibility` fade (200 ms)
- Match card corner radii (16 dp) in skeleton shapes

### R11 · Separate screens, not one scroll (← all reference apps)

Split into distinct screens/fragments:

| Screen | Content |
|--------|---------|
| **Home** | Search bar (hero) + suggestion chips + recent searches |
| **Results** | Result cards (R5) — transition from home via shared-element search bar |
| **Playback** | Full-width 16:9 video + overlaid timestamp + matched dialogue below |
| **Library** | Imported videos list + "Add" FAB + indexing progress |
| **Settings** | Model status, language toggle, about/licenses |

Home → Results: search bar stays in place, results cards animate up from below (M3 Expressive, 350 ms).
Result tap → Playback: shared-element thumbnail expansion (500 ms, Expressive Default Spatial).

### R12 · Motion tokens (← Linear, M3 Expressive, Arc Search)

| Purpose | Duration | Easing | Example |
|---------|----------|--------|---------|
| Micro (toggle, press feedback) | 100–150 ms | `FastOutSlowIn` | Chip select, icon state |
| Component (card reveal, sheet) | 200–300 ms | Spring: stiffness 400, damping 30 | Result card entrance |
| Hero (search expand, screen) | 350 ms | `cubic-bezier(0.42, 1.67, 0.21, 0.90)` | Search bar expand |
| Layout (screen transition) | 400–500 ms | `cubic-bezier(0.38, 1.21, 0.22, 1.00)` | Results → Playback |

Values > 1.0 in y-axis create the signature M3 Expressive overshoot/bounce.
**Never** exceed 500 ms for any UI transition. Only animate `translationY`, `alpha`, `scaleX/Y`.

### R13 · Typography tightening (← Linear, Claude)

Use the M3 type scale but with these overrides:

- **Search input:** Body Large (16 sp, Regular 400)
- **Result card title:** Title Small (14 sp, Medium 500). Negative tracking: `letterSpacing="-0.01em"`
- **Matched dialogue:** Body Medium (14 sp, Regular 400)
- **Timestamp:** Label Small (11 sp, Medium 500), `onSurfaceVariant`
- **Section headers:** Title Medium (16 sp, Medium 500), negative tracking `-0.02em`
- **Empty state headline:** Headline Small (24 sp, Regular 400)

Font: system default (Roboto). Do NOT use a custom typeface — this is a tool, not a brand showcase.

### R14 · Iconography (← Linear, Google Photos)

- **Grid:** 24 dp with 2 dp padding
- **Stroke:** 1.5 dp uniform weight, **square/butt caps** (not rounded — gives precision feel)
- **Style:** Outlined by default, filled for active/selected state
- **Color:** `onSurfaceVariant` default. `colorPrimary` for active/selected only
- Use Material Symbols Outlined — consistent with M3, no custom icon set needed
- Key icons: `search`, `videocam`, `folder_open`, `play_circle`, `info`, `settings`

### R15 · Indexing progress: non-blocking, ambient (← Linear, Notion)

Indexing should NOT freeze the search UI. Show progress as:

- **Persistent bottom bar** (56 dp) when indexing is active: filename + determinate progress + "N/M videos"
- Tapping bar expands to full Library screen with per-video detail
- User can still search already-indexed content while indexing continues
- Completion: bar briefly shows `"✓ 3 个视频已索引"` then auto-dismisses (2 s, fade out)

---

## What to AVOID

| Anti-pattern | Why | Seen in current UI |
|-------------|-----|-------------------|
| **Showing raw model names** (EmbeddingGemma, SenseVoice) | Users don't care about model internals | ✅ Prominent |
| **Cosine similarity scores** | Meaningless to non-technical users | ✅ "余弦 0.920" |
| **Full-width action buttons** below form fields | Feels like a web form, not an app | ✅ "搜索" button |
| **Pure `#000000` dark backgrounds** | Harsh, creates contrast fatigue | ✅ Night mode |
| **Everything on one screen** | Cognitive overload, buries the primary action | ✅ Single scroll |
| **Download/retry buttons for models** | Implementation detail; handle automatically | ✅ "下载嵌入模型" |
| **Italic text for warnings** | Not a recognized UI pattern; use chips/badges | ✅ "可能不准确" |
| **Hardcoded color values** (not M3 tokens) | Breaks dark/light theming | ✅ `@color/low_confidence` |
| **`cardElevation="2dp"` drop shadows** | Dated; modern apps use hairline borders or tonal fill | ✅ Result cards |
| **LinearLayout for result lists** | No recycling; crashes with many results. Use RecyclerView | ✅ `resultContainer` |

---

## Implementation Priority

1. **R1 + R11**: Search-first home screen + screen separation (biggest UX impact)
2. **R2 + R4**: Warm color palette + surface ladder (biggest aesthetic impact)
3. **R5 + R6 + R7**: Redesigned result cards (core feature polish)
4. **R9 + R8**: Model onboarding overlay + empty state (first impression)
5. **R10 + R12**: Shimmer + motion (perceived quality)
6. **R3 + R13 + R14**: Accent, type, icons (refinement)
7. **R15**: Non-blocking indexing (advanced UX)
