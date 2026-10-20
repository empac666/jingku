## Grok audit

Both must-fixes in [GROK-AUDIT.md](eval-270m/GROK-AUDIT.md) are accepted and corrected. **Decision remains ADOPT 270M.** The decision sentence's 75.81% baseline is the current Android-prefix LiteRT 740M re-run, not a claim of frozen per-query parity. [Resolution](eval-270m/AUDIT-RESOLUTION.md), [machine-readable replay evidence](eval-270m/audit-evidence.json), and `eval-270m/resolve_audit.py` record the checks. No gold, split, prefix, threshold or artifact changed during this audit.

| Comparison against frozen historical HF (47/62 Hit@3) | Lost Hit@3 query ids | Gained Hit@3 query ids | Net |
| --- | --- | --- | --- |
| Android-prefix LiteRT, each of 740M and 270M | `en03-q02`, `en03-q06`, `en04-q07` | `en02-q07`, `zh05-q05`, `zh05-q07` | 0/62 (0.00 pp) |
| Historical-prefix LiteRT, each of 740M and 270M | `en03-q06`, `en04-q07` | `en06-q03` | −1/62 (−1.61 pp) |
| Fresh historical HF | none | none | 0/62 (0.00 pp) |

Thus Android equals frozen HF only in aggregate Hit@3; “one fewer” historical LiteRT hit is a net change. Frozen `hit()` replay on all saved dev/test rankings has zero mismatches. Within each paired LiteRT protocol, 740M and 270M have zero discordant Hit@3 labels.

The deployment change is **drop unused encoders**: the [pinned model card](https://huggingface.co/litert-community/embeddinggemma-2-text-270m-litert-lm/blob/9be6e8b90982095dc05c2bd162e4b954ee4dbac7/README.md) lists the same 130M transformer plus 140M embeddings for both models; 740M additionally includes 170M vision and 300M audio encoders. This supports a shared text backbone, not proof of identical exported weights. Saved Android-prefix matrices are bit-identical for all 140/140 gold queries and 76/76 calibration queries; corpus rows match 1132/1161. The 29 different document rows have median row maximum absolute difference 0.00448318 and maximum 0.17572719 (`en01-w0001`). Calibration documents match 494/505. No positive query changes its Top-3 set; the one Hit@1 label change is `en04-q02`. The cause of the document differences has not been isolated; exact end-to-end vector identity is not claimed.

The shared Android threshold 0.8748952746391297 is `nextafter(0.8748952746391296, +∞)`, where that score belongs to calibration negative `neg03-zh`, for both exports. It is one operating point for the shared text tower with Android prefixes, not independent calibration evidence for two unrelated text models. Replaying the frozen boundary method reproduces it; calibration accepts `neg10-en` and `neg10-zh` (2/24, one paired fact). No recalibration change is needed.

Both user adoption clauses still pass: paired Android Hit@3 drop 0.00 pp ≤3 pp; current 740M point 75.81% is within candidate Wilson [63.85%, 84.75%]. The approximately 21 pp Wilson width limits precision. Video-cluster bootstrap [64.71%, 86.57%] also contains the baseline. The zero paired bootstrap interval and McNemar p=1 describe this sample's zero discordance; they do not establish population equivalence, and McNemar assumes independent queries.

Timing remains descriptive: a 740M Android corpus request for `zh01-w0050` stalled about 314 seconds during overlapping jobs, despite matching the 270M vector. Median/p90 conceal this tail; no controlled latency advantage is established. APK/AAB hashes, native 768-D output, generation 7, and 2024 ASR selection remain unchanged. Signed artifacts are reverified during audit; no rebuild or signing operation is needed for these reporting corrections. 1.0.3 remains preserved; physical-device acceptance is pending.
