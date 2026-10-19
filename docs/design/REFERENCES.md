# 镜库 Jingku — 设计参考 / Design References

汇编：dispatcher，2026-10-19 23:45 (UTC+8)。两份独立调研原文合并，未改写。

- Part A — agy（Claude Opus 4.6 Thinking）：ChatGPT, Claude, Perplexity, Grok, Arc Search, Raycast, Google Photos, Apple Photos, Linear, Notion
- Part B — Grok（grok CLI）：ChatGPT, Claude, Perplexity, Grok, Arc Search, Raycast, Google Photos, Linear, Apple Photos/Spotlight, Notion

两份共识（dispatcher 摘要）：搜索框是主角（大圆角 pill，居中）；单一强调色+暖中性底色；结果用大 16:9 缩略图纵向卡片、时间码角标、台词高亮；隐藏余弦分数，只在低置信时显示 chip；空状态带一句引导+一个主操作；模型准备/索引用单条确定进度条、不阻塞；hairline 边框/色调层级代替阴影；动效 150–400ms 标准缓动；分屏而非一整页长表单。

---

# Part A — agy (Claude Opus 4.6)

## 镜库 Jingku — Borrowable Design Language

> Design research synthesis, Oct 2026. 10 reference apps → 15 implementable rules for Material 3 / Android XML Views.

---

### Current UI Diagnosis

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

### Reference Apps Studied

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

### 15 Concrete Rules for Jingku

#### R1 · Search field is the hero (← Google Photos, Perplexity, Arc Search)

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

#### R2 · Warm, not cold (← Claude, Perplexity)

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

#### R3 · Single accent discipline (← Linear)

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

#### R4 · Surface ladder, not flat inversion (← Linear, Google Photos)

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

#### R5 · Result cards: big thumbnail, vertical stack (← Perplexity, Google Photos)

Replace the current horizontal 96×72 dp thumbnail + text layout:

- **Card width:** full-width (match_parent minus 24 dp margins)
- **Thumbnail:** 16:9 aspect ratio, **full card width**, top of card. `ShapeAppearance.Material3.Corner.Medium` (12 dp top corners)
- **Content below thumbnail:** 16 dp padding. Video name (Title Small, 14 sp), dialogue text (Body Medium, 14 sp), timestamp pill
- **Duration badge** on thumbnail: bottom-right, white text on `#000000` @ 50% alpha, Label Small (11 sp), 4 dp corner radius
- **Card corner radius:** 16 dp (`ShapeAppearance.Material3.Corner.Large`)
- **Card spacing:** 12 dp vertical gap between cards
- **No elevation shadow** — use 1 dp hairline border `surfaceVariant` @ 12% alpha (← Linear)

#### R6 · Low-confidence as a proper chip (← Material 3 spec)

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

#### R7 · Hide cosine scores; show confidence tiers (← Perplexity, Arc Search)

Users don't understand "余弦 0.920." Replace with:

| Cosine range | Display | Visual |
|-------------|---------|--------|
| ≥ 0.85 | (nothing — high confidence is the default) | No chip |
| 0.70–0.84 | `可能不准确` chip | `colorErrorContainer` chip |
| < 0.70 | `仅供参考` chip | Muted `surfaceVariant` chip, lower card opacity (0.72) |

Timestamp format: `01:23` not `00:01:23 – 00:01:26`. Show range only on tap/expand.

#### R8 · Empty state with purpose (← Google Photos, ChatGPT, Linear)

When no videos are imported, show a centered empty state:

1. **Illustration:** A single-color outlined icon (film reel + magnifier), 80 dp, tinted `onSurfaceVariant`
2. **Headline:** `"导入视频，搜索台词"` — Headline Small (24 sp), centered
3. **Body:** `"从相册或文件导入视频，镜库会转写所有对白"` — Body Medium (14 sp), `onSurfaceVariant`
4. **CTA button:** `"选择视频"` — M3 FilledTonalButton, 40 dp height, full corner radius

No technical jargon. No model status. No "尚未选择视频" plain text.

#### R9 · Model preparation as onboarding overlay (← Arc Search, Notion)

First-launch model unpacking should NOT be an inline section. Use a **full-screen overlay / bottom sheet**:

- Center: subtle animated progress ring (indeterminate circular, 48 dp)
- Headline: `"正在准备离线引擎…"` (Headline Small, 24 sp)
- Body: `"首次启动需解压模型，此后完全离线"` (Body Medium)
- Determinate progress bar below (M3 LinearProgressIndicator, 4 dp track height)
- **No model names** (EmbeddingGemma, SenseVoice) — those are developer details

Once ready, overlay dismisses → home screen with search bar.

#### R10 · Shimmer skeleton for search results (← Google Photos, Linear)

While searching (typically < 500 ms on-device, but can be 1-2 s):

- Show 3 placeholder cards matching R5 layout geometry
- Shimmer: diagonal gradient sweep, 1200 ms cycle, `LinearEasing`
- Light: base `#E8E4DE`, highlight `#F5F2ED`
- Dark: base `#252320`, highlight `#353230`
- Transition to real content: `AnimatedVisibility` fade (200 ms)
- Match card corner radii (16 dp) in skeleton shapes

#### R11 · Separate screens, not one scroll (← all reference apps)

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

#### R12 · Motion tokens (← Linear, M3 Expressive, Arc Search)

| Purpose | Duration | Easing | Example |
|---------|----------|--------|---------|
| Micro (toggle, press feedback) | 100–150 ms | `FastOutSlowIn` | Chip select, icon state |
| Component (card reveal, sheet) | 200–300 ms | Spring: stiffness 400, damping 30 | Result card entrance |
| Hero (search expand, screen) | 350 ms | `cubic-bezier(0.42, 1.67, 0.21, 0.90)` | Search bar expand |
| Layout (screen transition) | 400–500 ms | `cubic-bezier(0.38, 1.21, 0.22, 1.00)` | Results → Playback |

Values > 1.0 in y-axis create the signature M3 Expressive overshoot/bounce.
**Never** exceed 500 ms for any UI transition. Only animate `translationY`, `alpha`, `scaleX/Y`.

#### R13 · Typography tightening (← Linear, Claude)

Use the M3 type scale but with these overrides:

- **Search input:** Body Large (16 sp, Regular 400)
- **Result card title:** Title Small (14 sp, Medium 500). Negative tracking: `letterSpacing="-0.01em"`
- **Matched dialogue:** Body Medium (14 sp, Regular 400)
- **Timestamp:** Label Small (11 sp, Medium 500), `onSurfaceVariant`
- **Section headers:** Title Medium (16 sp, Medium 500), negative tracking `-0.02em`
- **Empty state headline:** Headline Small (24 sp, Regular 400)

Font: system default (Roboto). Do NOT use a custom typeface — this is a tool, not a brand showcase.

#### R14 · Iconography (← Linear, Google Photos)

- **Grid:** 24 dp with 2 dp padding
- **Stroke:** 1.5 dp uniform weight, **square/butt caps** (not rounded — gives precision feel)
- **Style:** Outlined by default, filled for active/selected state
- **Color:** `onSurfaceVariant` default. `colorPrimary` for active/selected only
- Use Material Symbols Outlined — consistent with M3, no custom icon set needed
- Key icons: `search`, `videocam`, `folder_open`, `play_circle`, `info`, `settings`

#### R15 · Indexing progress: non-blocking, ambient (← Linear, Notion)

Indexing should NOT freeze the search UI. Show progress as:

- **Persistent bottom bar** (56 dp) when indexing is active: filename + determinate progress + "N/M videos"
- Tapping bar expands to full Library screen with per-video detail
- User can still search already-indexed content while indexing continues
- Completion: bar briefly shows `"✓ 3 个视频已索引"` then auto-dismisses (2 s, fade out)

---

### What to AVOID

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

### Implementation Priority

1. **R1 + R11**: Search-first home screen + screen separation (biggest UX impact)
2. **R2 + R4**: Warm color palette + surface ladder (biggest aesthetic impact)
3. **R5 + R6 + R7**: Redesigned result cards (core feature polish)
4. **R9 + R8**: Model onboarding overlay + empty state (first impression)
5. **R10 + R12**: Shimmer + motion (perceived quality)
6. **R3 + R13 + R14**: Accent, type, icons (refinement)
7. **R15**: Non-blocking indexing (advanced UX)

---

# Part B — Grok

## Jingku visual references

Research for an offline Android (Material 3, XML Views) B-roll finder: import your own videos, transcribe on device, search a line, jump to the shot. Light and dark, zh/en. Read against `app/app/src/main/res` and `screenshots/` (HTML renders, not device captures).

**Why the current UI reads as ugly.** `activity_main.xml` is one scroll of admin: model names, three download buttons, two equal import buttons, index logs, an outlined field plus a full-width 搜索, then a permanent `VideoView`. `themes.xml` paints `statusBarColor` with `colorPrimary` (`#5B5BD6` light, `#C3BFFF` night). `item_search_result.xml` is a 96×72 side thumb, `cardElevation` 2dp, filename in bold, dialogue secondary, meta `余弦 0.920`, and “可能不准确” as italic `#B45309`. The shot is the smallest thing on the card. Elegant search apps do the opposite: the canvas is quiet, the query is the screen, the hit is a picture plus one line.

Hex values below mix **official specs** and **third-party reconstructions** of shipping UI (several catalogs disagree by a few points). Patterns are the part to copy. Do not ship another product’s brand color.

### What each product actually does

#### ChatGPT (iOS / Android)

The UI gets out of the way so text is the product. Canvas is white `#FFFFFF` or charcoal `#212121`, ink `#0D0D0D` / `#ECECEC`. After 2024 the old green `#10A37F` is mostly gone; links are a quiet blue `#2A7FFF`. User text sits in a soft bubble `#F7F7F8` / `#2F2F2F`; the assistant is plain prose, no card. Composer is a large rounded well (about 26px radius, 16px type) docked to the bottom; empty, it shows attach / voice; typed, those collapse into a 32px black circle with a white up-arrow (inverse in dark). Press scales to about 0.94 with a medium haptic. New chat is almost empty: model chip, no illustration. Starters are removable chips whose placeholder matches the mode. OpenAI’s own plugin rules: system font (SF Pro / Android sans), body and body-small only, no custom fonts, no gradients, brand color on a badge not on the page, at most two actions, imagery at a fixed ratio. Streaming is a three-dot pulse or a shimmer in the composer, not a skeleton of the whole screen. Weights stay 400 / 500 / 600.

#### Claude

Warm paper, not tech white. Page `#F5F4ED`–`#FAF9F5`, ink `#141413`, every gray yellow-brown (`#5E5D59`, `#6C6A64`). Terracotta (“clay”) `#C96442`–`#D97757` is the logomark and rare accent; **buttons are black pills**, not clay. Display is a light serif (Anthropic Serif / Copernicus, weight ~330–400, tight tracking); UI is a humanist sans (Styrene / Anthropic Sans). Depth is a 1px ring, not a drop shadow. Dark is olive-black `#141413` / `#181715`, not cool slate. Empty prompts are short literary lines. Density is magazine: ~32px card padding, long gaps between sections. Motion is calm; nothing bounces.

#### Perplexity

Search, not a persona. Home is a centered “Ask anything…” on paper `#FBFAF4` / `#FCFCF9`, ink `#091717`, one teal (`#20808D`, a 2026 CSS extract also shows `#016A71`; on dark the teal lifts to `#34B4C4` over `#0D1117`). Type is a single grotesque (pplxSans / FK Grotesk), 16px answer, ~1.6 leading. Composer radius ~16px, 1px warm border `#E4E4DC`, focus ring `3px rgba(32,128,141,0.12)`. Suggestion pills are 34px, full radius, active state a teal tint `#E5F2F2` not a filled brand button. **Sources are the trust UI**: horizontal source cards on mobile (favicon, domain, snippet; radius ~10px, hairline), inline citation chips 11px / 600, related questions as stacked chips under the answer. No relevance score. Flat by default; shadow only when something floats. Empty library is one muted sentence (`#5C6A6A`), no mascot. Follow-up composer sticks to the bottom with safe-area inset.

#### Grok

OLED black `#000000`, text `#E7E9EA`, secondary `#71767B`, surfaces `#16181C` / `#1E2126`, hairline `#2F3336`. White is the accent: send is a 32px white circle, black arrow, press scale 0.92 plus a soft haptic. The only chroma is link blue `#1D9BF0`, and only on citations. Inter 16/400 for the answer, 13px for source meta, 12px disclaimer (“can make mistakes”). Empty state is a wordmark plus 3–4 chips (`#16181C`, 14px radius, 14pt) — no illustration. Citation cards are 16px radius, 1px border, no shadow. Mode switch is a 200ms spring. A 2026 composer teardown: first screen has no tool vocabulary; you can just type; a filled arrow appears only when there is something to send. Reconstructions call the app dark-only; later builds added an appearance setting. The elegant part is the void and the white send, not a theme picker. There is no token stream in Jingku, so do not copy the blinking cursor.

#### Arc Search

A browser that feels as fast as a gesture. Chrome collapses to three thumb targets (search, page actions, tabs). Search opens over the keyboard; favorites sit **above the keyboard**, then swap for suggestions as you type. “Browse for Me” does not return a chat log: it builds a page of short nuggets, each with a source you can open. Tabs are large cards, like the iOS app switcher, and transitions are deliberately snappier than Safari. The Browser Company aimed at “lightness”: translucency, no heavy texture. Product chrome is sans (Inter); marketing uses a serif. Shapes are squircles, about 12–16px on cards and full pills for tags. Color is allowed to come from the **content** (the page tint), not from a fixed brand fill. Lesson for Jingku: the answer is a composed frame, motion is short, and the tool chrome disappears once you are in the shot.

#### Raycast (Mac launcher, iOS 2025)

Desktop is a command palette: canvas near `#07080A`–`#111113`, text `#F2F2F2`, placeholder at 40% white, selection and the loader are the same red `#FF6363` — punctuation, not a theme. Hairline borders, **no drop shadow**. Inter everywhere (stylistic set ss03); mono only for shortcuts and micro-labels. List rows, not cards. iOS could not be a hotkey launcher, so they inverted it: a Home of favorites, **pull down to search**, hold to talk, Action-button entry. Search is a gesture. The red never fills a screen.

#### Google Photos (search + Memories)

Search is a primary tab, not a field buried under settings. The query box accepts sentences (“kayaking on a lake…”), not only keywords. Before you type, a row of **face and place chips** is the suggestion UI. Results are the pictures: large thumbs, little gap, video duration on the thumb, sort by relevance or date. Ask Photos can answer in words, but a toggle returns you to the classic grid — the photo stays the proof. Memories are full-bleed cards whose surface color is sampled from the still (Material 3 Expressive), with a 16:9-class image and a short title. Loading uses a tiny blurred placeholder, then the real frame. Albums picked up a floating toolbar instead of a stack of labeled buttons.

#### Linear

Dark-native `#08090A`, panels barely lifted (`#0F1011`, `#1C1C1F`), hairline `rgba(255,255,255,0.05–0.08)`. Accent indigo `#5E6AD2` light / `#7170FF` dark is selection and focus only; it brightens in dark so it still reads. Status is a **dot or a small label**, never a tinted card. Inter Variable, weight ~510 for UI, 13px for meta, negative tracking on Latin display only. Rows are dense and full-width. Primary marketing actions are near-white pills, not indigo fills. Mobile inherits the same idea: a library is a list; color means state.

#### Apple Photos and Spotlight

HIG: if search matters, put it in a primary slot (Photos uses a Search tab; Notes puts the field in the bottom toolbar, and it **rises over the keyboard**). Field anatomy is fixed: leading magnifier, placeholder that states scope, clear button, cancel when focused. Search-as-you-type. Suggestions and tokens narrow scope in place (person + place). Photos results are a grid; natural-language queries still return pictures, with extra refinement chips after the first hit. Spotlight is a translucent sheet of grouped rows (icon, title, one metadata line). HIG haptics: use system patterns for their meaning, keep them short, fire them on discrete events only. Large titles are the system display face; body stays the text face. Do not invent a second font.

#### Notion (short)

Document, not dashboard. Canvas `#F7F6F3`, warm ink `#37352F`, hairline `#E9E9E7`, placeholder `#9B9A97`. Page title ~32px; body 16/1.5; blocks have **no cards**. Empty is the sentence “Type / for commands”. Color is a pastel tag, not a purple AI panel. AI sits in the flow of the page.

### Borrowable design language for Jingku

System font only: Roboto / Noto Sans CJK (Android). No italic on Chinese. No negative tracking on CJK. Timestamps: `fontFeatureSettings="tnum"`. One accent family. Status bar and navigation bar use `colorSurface`, with `WindowInsetsController` light/dark icons — never `colorPrimary`.

#### Colour roles

| Role | Light | Dark | Use |
|---|---|---|---|
| `colorSurface` | `#F6F5F1` | `#12110F` | Page. Warm paper / warm black (Claude, Notion, Perplexity paper). |
| `surfaceContainerLowest` | `#FFFFFF` | `#1C1B19` | Search well, result card, sheets. |
| `colorOnSurface` | `#1C1B19` | `#F3F0E8` | Titles and dialogue. |
| `colorOnSurfaceVariant` | `#6B6862` | `#A8A396` | Filename, hints, stage caption. |
| `colorOutlineVariant` | `#E4E1D8` | `#3A3832` | 1dp stroke. Replaces elevation. |
| `colorPrimary` | `#1C1B19` | `#F3F0E8` | The one filled action and the send disc (ChatGPT / Claude black-or-white). |
| `colorOnPrimary` | `#F6F5F1` | `#12110F` | Label on that fill. |
| `colorPrimaryContainer` | `#E8E4DA` | `#2A2824` | Tonal chip, pressed row. |
| Match | bg `#F3E6C4` | bg `#3E3420` | Dialogue span only. Text stays `colorOnSurface`; dark matched words `#F0D7A2`. |
| Low confidence | bg `#F4E8D4` / fg `#8A5A12` | bg `#3A2C1A` / fg `#E6B15A` | Chip only. Amber is status (Linear), not decoration. |
| Scrim | `#99000000` | same | Timestamp badge on the still. |

WCAG AA: `#8A5A12` on `#F4E8D4` and `#E6B15A` on `#3A2C1A` are for 12sp medium labels; do not set them as body text. Do not also introduce teal or purple.

#### Type (four sizes)

| Style | Spec | Jingku |
|---|---|---|
| `textAppearanceHeadlineMedium` | 28sp / 36sp, w500 | Screen title「镜库」, empty-state title. |
| `textAppearanceTitleLarge` | 22sp / 28sp, w500 | 「找到 3 个镜头」. |
| `textAppearanceBodyLarge` | 16sp / 24sp, w400 | The matched line. This is the hero. |
| `textAppearanceLabelMedium` | 12sp / 16sp, w500 | Timestamp, filename, chip, stage caption. |

Buttons and suggestion chips use `labelLarge` 14sp / 20sp, w500. Section headers, if any remain, are `titleMedium` 16sp w500 — and there should be at most one per screen.

#### Space, shape, motion

Base unit 4dp. Screen horizontal padding **20dp**. Related items 8dp. Gap before a new group **24dp**. Card-to-card **12dp**. Inside a result, 12dp under the image, 16dp text inset. Minimum hit target 48dp.

| Shape | Radius | Where |
|---|---|---|
| Full | 28dp | M3 search bar is 56dp tall; corner = height/2. Suggestion chips also full (32dp tall). |
| Large | 16dp | Result card, player if inset, sheets. |
| Small | 8dp | Timestamp badge, low-confidence chip. |
| None | 0 | Full-bleed player and thumbnail top edge (clip with the card). |

Stroke 1dp `colorOutlineVariant`. `cardElevation="0dp"`. `app:cardUseCompatPadding="false"`.

| Token | Duration | Easing | Jingku |
|---|---|---|---|
| short2 | 100ms | standard `cubic-bezier(0.2, 0, 0, 1)` | Chip / send press, scale 0.96. |
| short4 | 200ms | emphasized accelerate `cubic-bezier(0.3, 0, 0.8, 0.15)` | Player and sheet exit. |
| medium1 | 250ms | standard decelerate `cubic-bezier(0, 0, 0, 1)` | Result list fade + 8dp rise. Stagger 50ms (short1). |
| medium4 | 400ms | emphasized decelerate `cubic-bezier(0.05, 0.7, 0.1, 1)` | Thumbnail expands into the player. |
| long4 | 1000ms | linear | Skeleton shimmer, opacity 0.45→1, one cycle feel, no rainbow. |

No bounce. No status-bar color animation. Progress track is the M3 4dp determinate bar; indeterminate only while the byte count is unknown.

Haptics, three events, `View.performHapticFeedback`: search submit `KEYBOARD_TAP`; open a shot `CONTEXT_CLICK`; model ready or index finished `CONFIRM`. Nothing on scroll or on each progress tick.

Icons: Material Symbols **outlined**, 24dp, `colorOnSurfaceVariant`, one style. Language is a toolbar label 「中」「EN」, not a second button.

#### Fifteen rules

1. **Home is search.** (Perplexity, Photos, HIG, Raycast pull.) Title, one sentence, 56dp search bar, a horizontal chip row. Import, models, and the player are other routes. `SearchBar` (`com.google.android.material.search`) or a 56dp container, leading icon, `imeOptions="actionSearch"`. Placeholder: 「一句台词，或一件事」 / “A line, or a moment”. No separate 搜索 button. Submit on IME search; a 32dp `colorPrimary` disc appears only when the field is non-empty (Grok, ChatGPT).
2. **Chips are past lines, not features.** (Photos faces, Perplexity pills, Grok starters.) Recent successful queries, horizontally scrolling because Chinese labels are wide. Assist chip, full radius, `colorPrimaryContainer` at rest. Tapping one runs the search.
3. **A hit is a 16:9 still with the line under it.** (Photos grid, Arc nuggets, ChatGPT carousel “always an image”.) Full-bleed thumb, then dialogue `bodyLarge` (max 3 lines), then filename `labelMedium`. The line is the title; the filename is meta. Drop the 96×72 side layout.
4. **The timestamp is the citation.** (Perplexity source chip, Grok citation meta.) Badge on the still, bottom-start, 8dp radius, scrim, `tnum`, 「00:12」. End time can sit in the caption. Never print cosine, latency, or “语义 Top-3”.
5. **Low confidence is a small chip.** (Linear status dot, Grok’s rare amber disclaimer.) `labelMedium`, not italic, under the line: 「可能不准确」 / “Low confidence”. The card stays tappable. No orange left border, no score.
6. **Match highlight is a warm wash behind the words.** (Perplexity citation tint, not a marker pen.) `BackgroundColorSpan` with the match token. Do not paint the card.
7. **Empty library is one sentence and one button.** (Perplexity, Grok, Notion.) 「还没有片子」 plus 「导入一段视频，就能按台词找到镜头」 plus a single filled 「导入视频」. No-results: 「没有对上的台词」 and the recent-query chips. No mascot.
8. **First launch is one task.** (ChatGPT names the outcome, not the model.) Full screen 「正在准备离线模型」, one determinate 4dp bar, percent, one human line 「正在解压语音模型」. Vendor strings move to 关于. Do not list EmbeddingGemma / SenseVoice / Silero on this screen.
9. **Indexing stays out of the way.** (Arc keeps the tool usable; Linear uses rows.) A library list: 112×63 thumb (16:9), name, one stage line, 4dp progress on that row. Overall 「2 / 3」 lives in the sheet title. Search still works for lines already indexed. Cancel is a text button.
10. **Search wait is three skeleton cards.** (Photos low-res tile, not a chat spinner.) 16:9 block + two text bars in `colorPrimaryContainer`, shimmer 1000ms. Replace, do not stack under a frozen form.
11. **Playback is the still, enlarged.** (Photos Memories, ChatGPT fullscreen, Arc “the page is the answer”.) 400ms expand from the card. Controls fade. Under the frame: the same highlighted line and 「已跳到 00:12」. Unaligned start is a 12sp caption 「起点未对齐」, not a banner. Back closes in 200ms. No `VideoView` on the home scroll.
12. **One filled action per screen.** (ChatGPT “two actions max, one primary”; Claude black pill.) Import on empty home. While indexing, the primary becomes 取消 only if you must; otherwise cancel is text. Gallery vs files is a menu on the import button, not two peer pills.
13. **Hairlines, not shadows.** (Linear, Raycast, Perplexity, Claude’s ring.) Elevation 0. Pressed state is `colorPrimaryContainer`, plus ripple.
14. **Dark is the same layout with warm black.** (ChatGPT `#212121`, Claude olive black, Perplexity lifts the accent.) Do not tint surfaces lavender. Do not go pure `#000000` (Grok’s void fights a photo grid and a light theme).
15. **Bilingual copy is short on purpose.** (OpenAI: body and body-small.) Buttons ≤ 6 Chinese characters or one English word: 导入, 取消, 搜索 is gone. The English placeholder must not be a specification.

#### Avoid

- Purple seed `#5B5BD6` / `#7C4DFF` / night `#C3BFFF` on the status bar, buttons, and progress. Two purples is a template, not an accent.
- One scroll that stacks 模型准备, 导入, 搜索, and 播放器.
- Outlined `TextInputLayout` plus a full-width 搜索.
- Cosine, milliseconds, “Top-3”, and model attribution on the home path.
- Italic Chinese, yellow `#33FFC107` spans, orange body text for uncertainty.
- `cardElevation` 2dp and `cardUseCompatPadding` (double inset, gray halo).
- Twin primary buttons (相册 | 文件, 下载 ASR | 下载 VAD).
- Indeterminate bars with three technical lines as the main progress UI.
- A blinking cursor, gradient orb, glassmorphism, or neon. Arc’s tint belongs to a browser; Jingku’s color belongs to the still.
- A serif UI font. Claude’s serif is display Latin; a CJK serif on Android looks like a document, and XML Views should stay on the system sans.
- Dark-only. Ship both themes; do not copy Grok’s missing light mode.
- Haptics on progress ticks, and any custom vibration pattern.
- Showing internal rank to build trust. Perplexity shows a source; a cosine is not a source. The still and the line are the source.

### Sources

Official or first-party:

- OpenAI plugin UI guidelines (system font, no gradients, accent on badges, carousel needs an image): https://developers.openai.com/apps-sdk/concepts/design-guidelines
- ChatGPT composer teardown (one + menu, chips show scope, outcome labels): https://aiuxplayground.com/teardowns/chatgpt/composer
- Material 3 motion tokens (durations, emphasized decelerate / accelerate): https://m3.material.io/styles/motion/easing-and-duration/tokens-specs
- Material 3 search bar (56dp, 16dp padding): https://m3.material.io/components/search/specs
- Material 3 shape scale (4 / 8 / 12 / 16 / 28 / full): https://m3.material.io/styles/shape/overview
- Material 3 progress (4dp track, determinate): https://m3.material.io/components/progress-indicators/specs
- Apple HIG search fields and searching: https://developer.apple.com/design/human-interface-guidelines/search-fields and https://developer.apple.com/design/human-interface-guidelines/searching
- Apple HIG haptics: https://developer.apple.com/design/human-interface-guidelines/playing-haptics
- Android `HapticFeedbackConstants`: https://developer.android.com/develop/ui/views/haptics/haptics-apis
- Google Photos descriptive search and Ask Photos: https://blog.google/products-and-platforms/products/photos/google-ask-photos-early-access/
- Photos search vs Ask toggle: https://9to5google.com/2026/07/20/google-photos-classic-search-toggle/
- Face chips returning to search: https://9to5google.com/2025/12/23/google-photos-brings-person-and-pet-face-shortcuts-back-to-search/
- Memories / larger thumbs: https://blog.google/products-and-platforms/products/photos/redesigned-google-photos/
- Raycast for iOS (home, pull to search, hold for voice): https://www.raycast.com/blog/raycast-for-ios
- Arc Search review (thumb search, fast tabs, Browse for Me nuggets): https://www.macstories.net/reviews/arc-search-for-iphone/
- Arc design intent (lightness, cut clutter): https://www.inverse.com/tech/arc-search-ios-app-interview-lead-designer-engineer
- Grok composer teardown (empty canvas, send only when there is content): https://aiuxplayground.com/teardowns/grok/composer

Reconstructed token catalogs (not vendor specs; hex can drift):

- ChatGPT iOS notes: https://raw.githubusercontent.com/Meliwat/awesome-ios-design-md/main/design-md/misc/chatgpt/DESIGN.md
- Grok iOS notes: https://raw.githubusercontent.com/Meliwat/awesome-ios-design-md/main/design-md/misc/grok/DESIGN.md
- Claude tokens: https://opendesigner.io/design-systems/claude and https://www.shadcn.io/design/claude
- Perplexity tokens: https://unpkg.com/oh-my-design-cli@1.8.2/web/references/perplexity/DESIGN.md and https://www.saasui.design/application/perplexity-ai
- Raycast tokens: https://raw.githubusercontent.com/dhananjay6561/design-md-hub/main/brands/raycast/DESIGN.md
- Linear tokens: https://raw.githubusercontent.com/dhananjay6561/design-md-hub/main/brands/linear/DESIGN.md
- Notion notes: https://raw.githubusercontent.com/Meliwat/awesome-ios-design-md/main/design-md/productivity/notion/DESIGN.md
