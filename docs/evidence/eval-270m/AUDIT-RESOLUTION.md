# Grok audit resolution — 270M / 1.0.4

**ADOPT 270M remains the decision. Both must-fixes are accepted and fixed in the reporting.** 1.0.4 remains the selected local release; 1.0.3 artifacts are preserved. No inference inputs, threshold, app source or signed artifact needed changing. No push, upload, remote Git contact or secret access occurred.

## Must-fix 1 — aggregate equality is not frozen per-query parity

Accepted. Independently replayed frozen `hit()` on all saved dev/test rankings for both LiteRT exports/protocols and fresh HF: zero saved-label mismatches. Against frozen HF, each Android export loses `en03-q02`, `en03-q06`, `en04-q07` and gains `en02-q07`, `zh05-q05`, `zh05-q07`. Each historical LiteRT export loses `en03-q06`, `en04-q07` and gains `en06-q03`: net −1/62 (−1.61 pp). Fresh HF has no Hit@3 discordance. These counts independently reproduce the audit.

Fixed [EVAL-270M.md](../EVAL-270M.md) to identify the decision's 75.81% as the current Android-prefix LiteRT 740M baseline, qualify “one fewer” as net, and list every frozen-discordant id in its Grok audit section. The report generator preserves the corrections on regeneration.

## Must-fix 2 — shared text backbone / shared operating point

Accepted. Exact float32 matrix comparison confirms 140/140 gold queries and 76/76 calibration queries match, as do 1132/1161 corpus rows. All 29 changed corpus ids and per-row differences are in [audit-evidence.json](audit-evidence.json). Median row maximum absolute difference is 0.00448318; maximum 0.17572719 is `en01-w0001`. Calibration document matches are 494/505. No positive-query Top-3 set changes; only `en04-q02` changes its Hit@1 label. The document-difference cause remains unproven.

The [pinned model card](270m-pinned-model-card.md) specifies the common 130M transformer / 140M embedding components and the additional 740M vision/audio components. Report now describes the change as **drop unused encoders**, without claiming all vectors or exported weights are identical.

Replayed calibration-only boundary selection from the saved raw rankings: both choose 0.8748952746391297, nextafter of `neg03-zh` score 0.8748952746391296. Both accept `neg10-en` and `neg10-zh` (2/24). Report identifies this as one operating point for the shared tower under Android prefixes. Test scores were not used for selection. Threshold/app changes and a rebuild would have no evidentiary basis.

## Decision, validation and artifacts

Paired Android Hit@3 remains 47/62 vs 47/62; drop 0.00 pp passes ≤3 pp. The current 740M point 75.81% lies in 270M Wilson [63.85%, 84.75%], so the second user clause also passes. No population equivalence is claimed. The Grok audit section also records cluster uncertainty, query dependence and the 314-second timing outlier.

Reproducible audit: `python3 release/eval-270m/resolve_audit.py` writes machine-readable evidence with input hashes; it neither regenerates embeddings nor overwrites frozen inputs. Frozen manifest hashes and original 1.0.3 artifact hashes pass. `python3 release/scripts/verify-1.0.4.py` rechecks signatures, unchanged certificate, packaged assets, DEX/source consistency, native alignment, archived 38 release tests and lint evidence; see `release/logs/eval-270m-audit-artifacts.log`. Prior device-acceptance limitations remain.

| Unchanged signed artifact | SHA-256 |
| --- | --- |
| 1.0.4 APK | `9db753ac9a3f2c504ff8290033fb88b8cb545c49f58c28f5b827a260259de3de` |
| 1.0.4 AAB | `753d74c9731766e91b256ccb91daa903185f2848f012fd47ac4225a83fd2f1c0` |

No new APK/AAB build or signing was performed; the original artifacts retain certificate `74e523f6f442d7983a3bff59855ae7258a1535214a59167b02e5b5a0ae6a5d11`. Reporting clarifications leave all release metrics, byte sizes and hashes unchanged. The release report receives an audit cross-reference. Docs and reproducible audit evidence are synchronized to the local mirror; local scan/commit outcome is recorded in STATE.md.
