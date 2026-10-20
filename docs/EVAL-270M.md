# EmbeddingGemma 2 text-270M evaluation

**Decision: ADOPT 270M; build 1.0.4 (versionCode 5), local only.** Hit@3 drop = 0.00 percentage points (≤3 pp: True); current Android-prefix LiteRT 740M point 75.81% lies within 270M Wilson 95% CI [63.85%, 84.75%]: True. Both user rules pass. These rules do not establish statistical equivalence or broad generalization.

AI-audited gold, not human listening ground truth. No gold/queries/transcripts/segments/splits were edited or filtered. All 31 low-confidence gold labels remain.

## Protocol and historical reproduction

Primary comparison: exact frozen iter5 full corpus (1161 SenseVoice windows from all 14 videos), 140 AI-audited queries, real-dev 40 / real-test 100; test positives 62, negatives 38. Same frozen `eval_real.evaluate`, video+window-midpoint within gold answer interval; cosine full 768-D, stable score sort, Top-10 retained, Top-3 no deduplication/no refusal. No translation or candidate-video restriction. Threshold only annotates low confidence; Top-3 remains visible. Query origin video defines clustering. No test-based parameter tuning.

Task requirements contain two different prefix protocols. Historical iter1/iter5: query `task: search result | query: ` and empty document prefix. Current Android AppConfig: query `task: search query | text: ` and document `task: search result | text: `. We ran both separately and use Android prefixes for the adoption/release threshold. A frozen desktop score must not be described as a device measurement.

The original historical 740M is HF BF16 text-tower inference (CPU2, batch8), not `.litertlm`. Replaying the archived rankings through the frozen evaluator exactly reproduces Hit@1=36/62 and Hit@3=47/62. Fresh HF queries against the archived HF corpus also reproduce those exact hit decisions. Fresh HF corpus + fresh queries yields Hit@1=35/62, Hit@3=47/62; the maximum vector difference and bridge evidence are in `eval-270m/historical-reproduction.json`. Our transformers is 5.20.2 vs archived lock 5.19.0; NumPy 2.5.4 vs 2.5.2; Torch 2.14.1+cpu and sentence-transformers 6.1.0 match. Fresh batch numeric differences can affect close ranks. No historical file or one-shot marker was touched.

LiteRT-LM Python 0.18.1, CPU4 single requests, normalizes output, explicit output_size=768, default special-token/overflow behavior. Android uses Kotlin 0.18.0. 740M loads its CPU vision/audio backends as in Android; 270M initializes only the text backend. No image/audio inference. Historical-prefix LiteRT 740M yields 36/62 Top-1 and 46/62 Top-3: net one fewer Top-3 than original HF (runtime/export numeric differences; two losses and one gain). Thus exact historical inference reproduction is **not** claimed; the scoring/data replay passes, and the controlled LiteRT pair is valid for the intended packaged model swap. Both LiteRT files run, so an HF-only model substitution was unnecessary.

Wilson intervals below use positive queries as trials (requested supplementary statistic). Queries within a video are correlated; the historical iter5 interval was video-cluster bootstrap, not Wilson. Both are retained in JSON; paired delta uses video-cluster bootstrap (2000 draws, seed 20261016), with exact McNemar diagnostic. Historical iter1 event-level Wilson is not interchangeable with these query-level intervals.

## Side-by-side real-test results

| Prefix protocol / model | Hit@1 | Wilson 95% | Hit@3 | Wilson 95% | Top-1 timestamp median / p90 (s) |
| --- | ---: | --- | ---: | --- | --- |
| historical / 740m | 36/62 (58.06%) | [45.67%, 69.52%] | 46/62 (74.19%) | [62.12%, 83.45%] | 3.030 / 58.684 (n=58) |
| historical / 270m | 36/62 (58.06%) | [45.67%, 69.52%] | 46/62 (74.19%) | [62.12%, 83.45%] | 3.030 / 58.684 (n=58) |
| android / 740m | 38/62 (61.29%) | [48.85%, 72.42%] | 47/62 (75.81%) | [63.85%, 84.75%] | 3.280 / 27.844 (n=59) |
| android / 270m | 39/62 (62.90%) | [50.46%, 73.84%] | 47/62 (75.81%) | [63.85%, 84.75%] | 2.780 / 25.786 (n=59) |

Timestamp error is absolute Top-1 midpoint distance to nearest gold answer center, **conditional on correct video**, over positive queries. Wrong-video predictions have undefined time distance and are excluded; n is explicit. Hit metrics still count those as misses. This is a new descriptive extension, not a replacement for frozen interval-hit scoring.

| Paired Top-3 comparison | Gains / losses | Δ270M−740M | Paired video bootstrap 95% | McNemar exact p |
| --- | ---: | ---: | --- | ---: |
| historical | 0 / 0 | 0.00 pp | [0.00%, 0.00%] | 1.0000 |
| android | 0 / 0 | 0.00 pp | [0.00%, 0.00%] | 1.0000 |

## Android-prefix language breakdown (real-test positives)

| Group | 740M Hit@1 / Hit@3 | 270M Hit@1 / Hit@3 | 270M Hit@3 Wilson 95% |
| --- | --- | --- | --- |
| zh, n=32 | 56.25% / 68.75% | 56.25% / 68.75% | [51.43%, 82.05%] |
| en, n=30 | 66.67% / 83.33% | 70.00% / 83.33% | [66.44%, 92.66%] |
| cross_language, n=17 | 52.94% / 64.71% | 52.94% / 64.71% | [41.30%, 82.69%] |

Cross-language overlaps zh/en. No positive-query category strata exist in this protocol; negative types affect no-answer diagnostics rather than positive Hit@3. Both dev/test full per-query rankings and all original group metrics/bootstrap intervals are retained.

## Confidence calibration

Reused exactly the iter1 S2 method and its 76-query calibration split (52 positive / 24 negative), frozen calibration windows/targets: choose the lowest score boundary with negative accepted fraction ≤0.10, accepting score≥threshold; include float64 `nextafter(+∞)` boundaries. This is not F1 optimization for wrong-Top-1 detection. No real-test score entered threshold selection.

New Android-prefix 270M threshold: **0.8748952746391297**. Both paired models select the same value. Historical-prefix 270M threshold: **0.8035151958465577**. Do not reuse the old HF historical-prefix 0.7889297604560853 after a runtime/prefix change. All candidate boundaries/FPR counts are saved in `*-threshold-curve.json`; chosen boundary and raw rankings were saved before test ranking.

| Split (270M Android prefixes) | Wrong Top-1 | Flagged | Correctly flagged | Precision | Recall |
| --- | ---: | ---: | ---: | ---: | ---: |
| iter1 calibration (n=76) | 41 | 53 | 39 | 73.58% | 95.12% |
| dev (n=40) | 28 | 35 | 27 | 77.14% | 96.43% |
| test (n=100) | 61 | 80 | 55 | 68.75% | 90.16% |

Calibration negatives accepted: 2/24 = 8.33%. Wrong Top-1 means no target (including all negative queries) or Top-1 outside the gold localization rule. Iter1 uses same-video midpoint error ≤2s; iter5 uses its gold tolerance interval. Flag means score<threshold. The empirical false-accept cap is calibration-only, not a correctness probability or guaranteed test risk.

## Box embedding cost

| Android prefixes | Load (s) | Corpus request median / p90 (ms) | Query request median / p90 (ms) | Peak process RSS (MiB) |
| --- | ---: | --- | --- | ---: |
| 740m | 0.332 | 36.77 / 45.26 | 25.64 / 27.83 | 447.76 |
| 270m | 0.366 | 26.72 / 40.45 | 25.35 / 27.90 | 406.83 |

Shared x86_64 CPU box, four runtime threads; warm filesystem/cache, no affinity or idle guarantee, one run per model/protocol, other setup/HF/ASR jobs may overlap. ru_maxrss is whole-process high-water RSS, including imports/backends. These are descriptive box costs, not randomized benchmarks, device latency, or device RAM. Individual request timings are preserved in JSON.

## Official smaller SenseVoice packages

| Official package | Archive bytes | ONNX bytes | ONNX SHA-256 |
| --- | ---: | ---: | --- |
| int8 2024-07-17 | 163,002,883 | 239,233,841 | `c71f0ce00bec95b07744e116345e33d8cbbe08cef896382cf907bf4b51a2cd51` |
| int8 2025-09-09 | 165,783,878 | 237,115,547 | `12ca1a2ae7ecf3e0019ef2822307ee0b5cadc9196569e379b4c4026f8205276d` |

The 2024 int8-only ONNX **and tokens are byte-identical** to current packaged files, so there is no installed-footprint saving; only archive packaging shrinks versus the mixed 1,047,870,769-byte archive. The 2025 file saves 2,118,294 bytes (~0.89%). Both official download URLs and archive/file hashes are retained; release URLs are tag-addressed rather than immutable, so SHA-256 identifies the actual bytes.

| Paired ONNX pipeline | Windows | Test Hit@1 | Test Hit@3 | Whisper disagreement proxy CER |
| --- | ---: | ---: | ---: | ---: |
| 2024-07-17 | 1610 | 54.84% | 77.42% | 41.26% |
| 2025-09-09 | 1504 | 50.00% | 74.19% | 44.30% |

2025−2024 downstream Hit@3 change: -3.23 pp. Keep the existing 2024 ONNX in 1.0.4: the small byte saving does not justify a pipeline/model change with any regression or mixed metric evidence. The int8-only 2024 download is interchangeable but does not change installed bytes.

ASR comparison: all original 14 raw 16k mono WAVs; identical Silero `faster_whisper.vad` max6s/min_silence300ms spans shared between variants; sherpa-onnx 1.13.8 CPU2, greedy, language auto, ITN on. Same 740M LiteRT Android-prefix embedder for both, same frozen gold/splits/midpoint hit evaluator. Tokens/timestamps are sherpa BPE tokens; token end is next token timestamp (final token capped at +60ms and VAD end), then unchanged iter1 `transcript_windows` grouping. The historical corpus used FunASR FP32 word timestamps: its 1161 windows cannot remain fixed when measuring a different ASR. These paired ONNX results are a separate ASR diagnostic, not an embedding A/B replacement for the frozen corpus. Android VAD itself uses max4s/min_silence250ms, unlike this real-test pipeline; device regression remains untested.

No human reference transcript exists. “CER” here is only normalized full-video ASR disagreement against the same frozen independent Whisper transcript (NFKC/lowercase/alphanumeric chars); it is **not** true human CER or proof of recognition accuracy. Keep that limitation with the number.

## Provenance, reproducibility and scope

270M repo: [pinned model card](https://huggingface.co/litert-community/embeddinggemma-2-text-270m-litert-lm/tree/9be6e8b90982095dc05c2bd162e4b954ee4dbac7), file `embeddinggemma-2-text-270m.litertlm`, 164,626,432 bytes, SHA-256 `2d079ee2f6f066b1f368e8d7c819f55214eaef1d0513b312321901f30ab286fb`. 740M SHA-256 `e7a8a2204b91e0f96e92960e84a09a89212e1633dcb7575a9bf3378b4df77f4c` (484,622,336 bytes), pinned revision `24d962e906c7d332c6428e71c9676855024569e2`. 270M native output verified at 768 dimensions; FULL_DIM/DB vector shape stays 768, but generation must change to prevent mixed-model indices. Text-only packaged model removes 319,995,904 bytes.

Runtime reference: [LiteRT-LM embedding API](https://ai.google.dev/edge/litert-lm/embedding_models); [official SenseVoice packages](https://k2-fsa.github.io/sherpa/onnx/sense-voice/pretrained.html). Inference is entirely local. HTTP downloads only fetched public runtime/model inputs; no remote Git contact, push, media upload or store action. Secrets were neither read nor printed.

Evidence and scripts: `eval-270m/input-manifest.json`, `historical-reproduction.json`, `{740m,270m}-{historical,android}.json`, `*-threshold.json`, `paired-comparison.json`, `decision.json`, `asr-packages.json`, `asr-results.json`, per-video ASR JSON and VAD spans; logs `logs/eval-270m-*.log`. Scripts write only evaluation outputs; they import the frozen evaluator rather than its one-shot CLI. Archived frozen markers are never reset. This authorized new-model comparison is a new retrospective experiment, not the original one-shot test or unseen heldout validation.

Reproduce with the recorded `requirements.txt`, cached pinned model files, then `python evaluate_models.py --model {740m,270m} --protocol {historical,android}` (one combination per invocation), `python reproduce_historical.py`, `python evaluate_asr.py`, `python report_evaluation.py`; retain completed outputs in a separate copied output directory for a new run. Delete only generated runtime/build intermediates. Original frozen files remain read-only; input hashes are rechecked at completion.

Release artifacts, checks, storage estimates, screenshots, source mirror and local commit: [REPORT-1.0.4.md](REPORT-1.0.4.md). Physical-device acceptance remains pending.

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
