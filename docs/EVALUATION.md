# Historical evaluation / 历史评估

Source records: iter1 freeze, iter5 `REPORT-eval.md`, iter6 `REPORT.md` from the local development archive, frozen before this release UI work. No new accuracy evaluation was run for this APK.

| Metric, iter5 real-test | Estimate | Denominator | 95% video-cluster bootstrap CI |
| --- | ---: | --- | --- |
| Top-1 timestamp hit | 58.06% | 62 answerable queries | 45.00–71.88% |
| Top-3 timestamp hit | 75.81% | 62 answerable queries | 65.57–86.44% |
| Refusal with optional threshold | 58.00% | 100 queries | 48.00–68.00% |
| False acceptance | 28.95% | 38 unanswerable queries | 12.12–42.87% |
| Accepted and timestamp hit | 33.87% | 62 answerable queries | 25.00–42.42% |

Test = 10 videos/100 queries; dev = 4 videos/40 queries. Total corpus 14 videos/~70 minutes; 1161 SenseVoice windows. Correct video and window midpoint within the adjudicated timestamp tolerance are required for a hit. Bootstrap: 2,000 replicates, seed 20261016. Corpus windows include both dev/test videos; query splits are held out.

Labels: `gold_type=ai_audited`, **not human listening ground truth**. 90 yes/50 no across the full 140 queries. 31 low-confidence labels retained. Reviewer saw listener output; original prelabels shared older ASR/query design; the three votes are not independent ground truth. Independent Whisper may omit or invent words. Eight original yes labels were changed to no for insufficient Whisper evidence, which is not proof the audio contains no answer. No low-confidence labels were excluded based on test performance. Sources are concentrated in VOA/ICANN, so institution/topic diversity is limited.

## Thresholds and protocol differences

- iter1 SenseVoice S2 frozen threshold: **0.7889297604560853**. The app uses this only as a historical warning reference. All Top-3 candidates remain visible.
- iter5 separately dev-selected threshold: **0.743815303**. This generated the historical refusal/false-acceptance figures. It was not substituted into the app to make those figures appear to be app measurements.
- Android preserves iter2 `task: search query | text: ` / `task: search result | text: ` prefixes and the LiteRT EmbeddingGemma 2 740M artifact. The historical offline query prefix was `task: search result | query: `, with a different local model representation. No claim of identical encodings or parity is made.
- Android uses ≤4s dialogue segments and token-aligned or segment-start playback; offline timestamp scoring used window midpoints. Threshold transfer is not calibrated on this APK.

## Judge route ended

iter4 adapters were not verified Android judge inference artifacts. iter5 main report therefore evaluated retrieval + threshold only. iter6 preregistered success required the main B variant's probability-based joint result to beat both cosine and overlap LR with paired 95% interval lower bounds above zero on both dev and test. It failed.

B test probability joint 44%; cosine difference -4 percentage points, interval [-13,+5]; overlap LR difference +3 points, interval [-9,+17]. B dev probability joint 40%. More conservative generated-clip rejection numbers use a different output rule and cannot establish the failed success criterion. No judge weights, adapters, prompts or judge runtime ship in this release.
