# Jingku 1.0.4 local release

VersionName **1.0.4**, versionCode **5**. Adopted pinned EmbeddingGemma 2 text-270M after the controlled evaluation; existing 2024 SenseVoice int8 is retained because the smaller 2025 file regressed. Full evaluation: [EVAL-270M.md](EVAL-270M.md).

## Signed artifacts

| Artifact | Bytes | SHA-256 |
| --- | ---: | --- |
| [out/jingku-1.0.4-arm64-v8a.apk](out/jingku-1.0.4-arm64-v8a.apk) | 466,070,902 | `9db753ac9a3f2c504ff8290033fb88b8cb545c49f58c28f5b827a260259de3de` |
| [out/jingku-1.0.4.aab](out/jingku-1.0.4.aab) | 317,681,969 | `753d74c9731766e91b256ccb91daa903185f2848f012fd47ac4225a83fd2f1c0` |

Certificate SHA-256 `74e523f6f442d7983a3bff59855ae7258a1535214a59167b02e5b5a0ae6a5d11` matches all prior releases. APK v2/v3 signatures and AAB JAR signature/certificate pass. Bundletool 1.18.3 validates the AAB and builds the signed universal APK directly. Version/code, arm64-only, non-debuggable/non-testOnly, no INTERNET verified. All six native libraries are byte-identical to 1.0.2, uncompressed, 16KB ZIP and ELF LOAD aligned. APK/AAB/final Gradle DEX match. Four packaged assets have the pinned hashes; the 740M asset is absent, and all packaged notices/license assets match current source.

1.0.3 APK/AAB are preserved byte-for-byte, including their original evidence. Compared with 1.0.3, APK saves 319,995,899 bytes and AAB saves 262,784,200 bytes. [Checksums](out/SHA256SUMS).

## Models, delivery and storage

| Packaged file | Bytes | Raw DEFLATE level6 bytes |
| --- | ---: | ---: |
| `embeddinggemma-2-text-270m.litertlm` | 164,626,432 | 129,081,018 |
| `model.int8.onnx` | 239,233,841 | 160,168,880 |
| `tokens.txt` | 315,894 | 135,378 |
| `silero_vad.onnx` | 643,854 | 508,088 |

One install-time `models` asset pack; total raw model bytes 404,820,021. Exact Gradle allowlist excludes all cached alternatives and `Sync` removes stale generated assets. Source Git excludes all model weights. The universal APK keeps model assets uncompressed; AAB transport compresses them. SenseVoice/tokens/VAD load directly through AssetManager. Only embedding is extracted (164,626,432 bytes), verified by unchanged atomic/fsync/marker logic.

| Arm64/API35/xxhdpi/zh+en bundletool download estimate | Bytes |
| --- | ---: |
| Base | 27,213,897 |
| Install-time models increment | 289,912,600 |
| Total | 317,126,497 |

`--modules=models` includes the base dependency; pack increment is total−base. Installed split APK sizes are retained in `out/eval270-download-sizes.json`.

Universal installed footprint estimate: **630,697,334 bytes (601.48 MiB)** = APK 466,070,902 + extracted embedding 164,626,432 + separately extracted ASR **0**. Split-install estimate: **638,722,212 bytes (609.13 MiB)** = sum of device-specific split APKs + extracted embedding. These exclude ART/app data, thumbnails, indexes, videos and package installation staging. Extraction needs 164,626,432+67,108,864 reserve = 231,735,296 free bytes. During upgrade, old 484,622,336-byte extracted embedding temporarily remains until replacement verifies, then its known filename/marker/temp files are removed. Cleanup failure is nonfatal and can leave extra old storage; user media is never touched.

## Behavior, migration and checks

- Same 1.0.3 UI, CrashGuard, extraction integrity and video import/index/playback logic. Native text-only engine avoids configuring absent vision/audio backends.
- `FULL_DIM=768`; query/document prefixes unchanged from Android 1.0.3. Threshold changes to **0.8748952746391297**, calibrated with the exact frozen iter1 S2 method and Android prefixes. Top-3 never refused.
- Index generation **7** / prefix metadata `litert-search-result-270m-v2`: old databases are blocked by the existing guarded open path; localized dialog explains clearing/re-indexing and preserving original videos. The user can choose Later; old vectors are never mixed with the new model.
- **38 release unit tests / 43 debug tests**, zero failures/errors/skips. Added replacement-marker/retired-file cleanup regression; previous extraction/SQLite/semantic768/queue/crash/audio tests retained. **lint 0 errors / 74 warnings**, no new suppression or baseline.
- **60 real Android native-Skia screenshots** (15 states × zh/en × light/dark), including the migration dialog; attribution and confidence fixtures updated. [Render provenance/list](screenshots/1.0.4/README.md), [hashes](out/eval270-screenshots.json). Inspected migration, attribution and high/low-confidence results. 1.0.3 screenshot sets preserved.

Build: `bash release/scripts/build-1.0.4.sh`; screenshots: `bash release/scripts/render-1.0.4-screenshots.sh`; validation: `python3 release/scripts/verify-1.0.4.py`. Build uses offline cached Gradle dependencies and the same keystore through password-file arguments only. Evidence: [artifact/source validation](out/eval270-artifact-validation.json), [source manifest](out/eval270-source-manifest.json), [model sizes](out/model-sizes-1.0.4.json), `evidence/eval-270m/`, `logs/eval-270m-*.log`.

## Local delivery and limitations

Mirror `repo/android` was verified byte-identical (108 files) to source, excluding weights/build/cache; current model docs, bilingual notes, license assets, evaluation/report/evidence and 1.0.4 screenshots are synchronized. Gitleaks directory and committed history, plus existing scanner, run locally with zero findings; local commit and completion evidence recorded in STATE.md. Generated build/pack/APK-set/runtime/venv intermediates removed after archival. Pinned model cache, final evaluation evidence, prior 1.0.3 artifacts and new final APK/AAB remain. Disk guard requires ≥8 GiB free and was never crossed.

No physical-device inference/import/playback or real Play delivery acceptance performed. Frozen gold is AI-audited, not human; sample is small and within-video correlated. Exact historical HF inference parity is not claimed for LiteRT; the report quantifies the discrepancy and uses matched runtime/prefix pairs. Current APK contains Apache-2.0 EmbeddingGemma 2 text-270M, FunASR-v1.1 SenseVoice (commercial interpretation unresolved), MIT Silero with all terms/attribution preserved. No remote Git contact, push, upload or store action; no secret contents printed.

## Grok audit

Both must-fixes were accepted and corrected in the evaluation's [Grok audit section](EVAL-270M.md#grok-audit) and [resolution](eval-270m/AUDIT-RESOLUTION.md). **ADOPT remains unchanged**: this deployment drops unused encoders from the shared text backbone. Equal aggregate Android Hit@3 does not mean parity with frozen HF query labels. Replayed calibration retains the shared operating point; no app/threshold/artifact changes or rebuild. Signed artifacts were reverified; 1.0.3 stays preserved. See `logs/eval-270m-audit-artifacts.log` and `eval-270m/audit-evidence.json`.
