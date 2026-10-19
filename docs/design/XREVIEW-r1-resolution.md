# Round 1 resolution — 1.0.3 final

Both cross-reviews were read in full. All P0/P1 requests accepted, including the overlapping requests below. DESIGN.md is v1.1.

| Review request | Resolution |
| --- | --- |
| Grok P0: 48dp Recent pills | chipMinHeight=48dp, LabelLarge, 16dp padding; centered group |
| agy P0 / Grok P1: active indexing idle prompt | Explicit running title/busy explanation, hide idle Index and import actions |
| agy P0 / Grok P1: contiguous metadata | Wrap-content row; shrinking filename and fixed middot/range; both muted |
| agy P0: About English and raw Markdown | Localized readable summaries used in both runtime and screenshot harness; complete audit and license files retained |
| Grok P1: full-bleed fixture | Original abstract landscape vector fills 16:9 wells; real video loader unchanged |
| Grok P1: crash identifier wrapping | Shared selectable monospace horizontal-scroll trace; original copy/share payload unchanged |
| agy P1: secondary hierarchy | Explicit muted colors for filename, range, busy, preparation, playback note, queue stage |
| agy P1: sheet buttons/thumb corners | End alignment and 8dp rounded queue thumbs |
| agy P1: Recent alignment | Centered title and wrap-content group |
| Grok P2: empty centering, skeleton, index title, no-dialogue, unused cosine | Accepted: centered body; higher skeleton surface; single-line title; hidden Import; obsolete cosine resource deleted |
| Grok P2: monospace horizontal legal Markdown | Replaced by readable localized summaries; full legal originals remain assets. Prose is easier to read than horizontal Markdown tables |
| agy P2: shorten crash delete action / filled Copy | Rejected: preserve explicit destructive meaning and existing 1.0.2 roles. Material dialog supports stacking; no clipping demonstrated at fixture size |
| agy P2: AspectFrame player | No change needed: MainActivity already sizes well from measured width to 16:9 and fit-centers video; harness applies the identical size. XML 220dp is an initial fallback |

Validation: native render set and UI regression tests recorded with final release evidence. Physical-device large-font, TalkBack, IME/insets and real native inference remain unverified.
