# Jingku 1.0.3 final local release / 本地最终版本

VersionName **1.0.3**, versionCode **4**. Bundled offline models and Material 3 UI are complete locally. No push, upload, store action or remote Git contact. Physical-device acceptance remains unverified.

## Final artifacts

| Artifact | Exact bytes | MiB | SHA-256 |
| --- | ---: | ---: | --- |
| [out/jingku-1.0.3-arm64-v8a.apk](out/jingku-1.0.3-arm64-v8a.apk) | 786,066,801 | 749.65 | `7a2db10294ed4ac22b3c6b690387b62d2529d2c9e661cb9f99a8c1683fcdf45a` |
| [out/jingku-1.0.3.aab](out/jingku-1.0.3.aab) | 580,466,169 | 553.58 | `c677642d64fd0fb0f51158de2ca775b9c7b4e7f2b0db49dc1b6886a90137708e` |

Both artifacts use certificate SHA-256 `74e523f6f442d7983a3bff59855ae7258a1535214a59167b02e5b5a0ae6a5d11`. APK signatures v2/v3 pass; AAB JAR signature and matching certificate pass. Bundletool 1.18.3 validates the AAB and generates the signed universal APK directly; no fallback. Non-debuggable, no testOnly, arm64-only, no INTERNET, four pinned model assets present and SHA-verified. All six native libraries are unchanged from 1.0.2, ZIP_STORED, 16KB ZIP aligned, and ELF LOAD-aligned at least 16KB. APK and signed AAB DEX match the final Gradle output. APK is below 2 GB.

Evidence: [artifact validation](out/final-artifact-validation.json), [source manifest](out/final-source-manifest.json), [screenshot hashes](out/final-screenshots.json), [checksums](out/SHA256SUMS), [build log](logs/build-sign-1.0.3-final.log), [test/lint archive](evidence/ui-1.0.3-round2/). Pre/rc1 APK/AAB copies are removed after validation; their hashes remain as comment history in SHA256SUMS. 1.0.2 APK is retained.

## Models and delivery

| Model or cache file | Bytes | Raw DEFLATE level 6 bytes | Bundled |
| --- | ---: | ---: | --- |
| `embeddinggemma-2-740m.litertlm` | 484,622,336 | 391,865,923 | yes |
| `model.int8.onnx` | 239,233,841 | 160,168,880 | yes |
| `tokens.txt` | 315,894 | 135,378 | yes |
| `silero_vad.onnx` | 643,854 | 508,088 | yes |
| `sensevoice.tar.bz2` | 1,047,870,769 | 1,047,959,461 | cache only |

Bundled sum: 724,815,925 bytes (691.24 MiB); raw DEFLATE sum 552,678,269 bytes, 23.75% saving. One install-time `:models` asset pack; weights are absent from base. Installed APK assets remain uncompressed for direct AssetManager access. The AAB transport uses DEFLATE. Cache and generated weights are excluded from Git; a Copy task checks fixed SHA values before packaging.

Bundletool estimated downloads, API35/arm64/xxhdpi/zh+en ([device spec](scripts/arm64-device.json)):

| Component | Min bytes | Max bytes |
| --- | ---: | ---: |
| Base | 27,213,705 | 27,213,705 |
| Model pack increment | 552,702,166 | 552,702,166 |
| Total | 579,915,871 | 579,915,871 |

`--modules=models` includes the base dependency, so pack-only download is total minus base. [CSV and installed-split size summary](out/final-download-sizes.json). These are bundletool estimates, not Play measurements; the base is well below either limit cited in the task, and the single 0.725 GB pack is below its task ceiling.

Sideload installed APK plus extracted embedding: **1,270,689,137 bytes (1.183 GiB)** before ART/DEX, SQLite, thumbnails and source videos. Embedding extraction adds 484,622,336 bytes; ASR and VAD use AssetManager directly. Allow embedding plus 64MiB reserve (526.17MiB, localized UI asks 530MiB). Install/download staging and old externally downloaded models can increase storage. Old models are not silently deleted.

## What changed

First launch prepares the bundled embedding on IO with free-space checks, tmp write, fsync, pinned SHA, atomic publication and version+SHA marker. Matching marker/size skips later copying/hashing; interruption, stale tmp and verification failure recover safely. No model download buttons, debug tmp imports or INTERNET permission. CrashGuard-first startup, previous-crash report, safe LocaleStore fallback, IO DB initialization and nonfatal metrics remain.

Warm ink/paper light/dark Material 3 themes; centered empty states; prominent search pill; 16:9 result cards, timestamps, literal two-color highlights and passive low-confidence labels. Queue sheet, live progress, preparation, retries, search cancellation/generation and fit-center playback are retained. Round2 fixes true 48dp recents, active indexing idle prompts, contiguous muted metadata, rounded queue thumbs, action alignment, full-bleed abstract screenshot fixture, localized readable licenses and horizontally scrollable trace. All P0/P1 review findings accepted and resolved; [decision record](design/XREVIEW-r1-resolution.md), [DESIGN v1.1](design/DESIGN.md). Frozen warning threshold and Top-3 semantics unchanged.

## Tests and screenshots

`testReleaseUnitTest lintRelease bundleRelease` succeeded on final sources: **37 release tests, zero failures/errors/skips; lint 0 errors / 74 warnings** (no new suppression/baseline). Native screenshot recording passed **42 debug tests**. Added regression verifies no idle indexing action while running, no-dialogue action visibility and true chip drawable height; prior extraction, crash, audio, seek, SQLite, queue and retrieval regressions remain passing.

**56 real Android native-Skia PNGs**, 14 states × zh/en × light/dark, 1233×2673. Final set: [screenshots/1.0.3/](screenshots/1.0.3/), byte-identical [round2/](screenshots/1.0.3/round2/). Historical baseline/round1 retained. [Full list, tool versions and command](screenshots/1.0.3/README.md). Production XML/theme/shared renderer with fake state and original abstract landscape placeholder, no HTML. Visually inspected light/en and dark/zh contact sheets plus key full-resolution states. These do not claim device inference/playback validation.

## Contributors and references

- agy: research Part A, v0 review (Gemini stand-in due to Opus 429), r1 review (Gemini stand-in).
- Grok: research Part B, DESIGN v0/v1, r1 review.
- Codex: packaging, implementation, v0 feasibility review, fixes, final verification/docs/local mirror.

Reference products: ChatGPT, Claude, Perplexity, Google Photos, Apple HIG, Notion, Linear, Raycast, Arc Search and Grok; [research links/rationale](design/REFERENCES.md).

## Licensing and risks

Weights are **REDISTRIBUTED in the APK/AAB**, excluded from source Git. SenseVoiceSmall retains the FunASR Model Open Source License Agreement v1.1 plus FunASR/FunAudioLLM/Alibaba and k2-fsa conversion attribution. Its commercial-use interpretation remains unresolved; no unconditional clearance is claimed. Silero retains MIT. The exact pinned EmbeddingGemma **2** artifact declares Apache-2.0, unlike the older Gemma-terms family; that evidence and the full Apache license are bundled. Older alternatives require Gemma terms review. Public NOTICE, model docs, audit and complete license assets are synchronized. [Recorded upstream evidence and terms](repo/docs/LICENSING.md).

No physical-device or emulator inference, full import/index/seek lifecycle, RAM/thermal, large-font/TalkBack, IME/insets/rotation or real Play asset delivery acceptance. Rendering validates layouts; JVM tests validate isolated logic. API compatibility compiles but native loading, device storage and split delivery still need device acceptance. No smaller model substituted or threshold recalibrated. No store approval claimed.

## Smaller candidates researched, not adopted

No model was changed and no threshold was recalibrated. Metadata evidence (upstream API file sizes/revisions): [model-alternatives.json](out/model-alternatives.json).

| Candidate | Bytes | Expected tradeoff / compatibility |
| --- | ---: | --- |
| [EmbeddingGemma 2 text 270M .litertlm](https://huggingface.co/litert-community/embeddinggemma-2-text-270m-litert-lm/resolve/9be6e8b90982095dc05c2bd162e4b954ee4dbac7/embeddinggemma-2-text-270m.litertlm) | 164,626,432 | Most promising for this text-only pipeline: drops vision/audio encoders. Expected to retain core text capability, but embeddings/quantization and retrieval quality need measurement; do not reuse the current threshold without evaluation. Apache-2.0. |
| [EmbeddingGemma 2 text-vision 440M .litertlm](https://huggingface.co/litert-community/embeddinggemma-2-text-vision-440m-litert-lm/resolve/e301f74d5551b0c2641bd5cb4652a76239d5c5f8/embeddinggemma-2-text-vision-440m.litertlm) | 387,710,976 | Removes audio; retains vision this app does not use. Smaller saving; text retrieval still needs validation. Apache-2.0. |
| [Older EmbeddingGemma 300M seq256 mixed-precision .tflite](https://huggingface.co/litert-community/embeddinggemma-300m/resolve/459f1d37fec9635eb1730ebdbc219bfc3226c8e7/embeddinggemma-300M_seq256_mixed-precision.tflite) | 179,131,736 | Different model family/version, 256-token context; truncation risk for longer inputs. .tflite needs a different runtime integration from current LiteRT-LM .litertlm. Gemma terms rather than current v2 Apache terms; re-evaluate quality and licensing. |
| [Older EmbeddingGemma 300M seq2048 mixed-precision .tflite](https://huggingface.co/litert-community/embeddinggemma-300m/resolve/459f1d37fec9635eb1730ebdbc219bfc3226c8e7/embeddinggemma-300M_seq2048_mixed-precision.tflite) | 195,912,440 | Larger context than seq256 but more memory/compute; same migration/quality/terms caveats. |
| [Official sherpa SenseVoice int8-only 2024 archive](https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-sense-voice-zh-en-ja-ko-yue-int8-2024-07-17.tar.bz2) | 163,002,883 (archive) | Much smaller build-time download than the pinned mixed archive. Official docs still list a ~228 MiB int8 model, so this does **not** imply a smaller installed ASR model; exact hash identity was not independently downloaded/reverified. |
| [Official sherpa SenseVoice int8 2025 Cantonese archive](https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-sense-voice-zh-en-ja-ko-yue-int8-2025-09-09.tar.bz2) | 165,783,878 (archive) | Cantonese fine-tune; official docs say no punctuation. Not a demonstrated smaller int8 deployment; transcript/seek/retrieval behavior would change. |

The current pinned 740M general CPU/GPU file is already smaller than its hardware-specific siblings; no smaller same-740M general .litertlm sequence/quant variant was listed in the inspected repository. The text-only candidate is the meaningful alternative, not a silent packaging swap. Sources: [pinned variant listing](https://huggingface.co/litert-community/embeddinggemma-2-740m-litert-lm/tree/24d962e906c7d332c6428e71c9676855024569e2), [official SenseVoice model docs](https://k2-fsa.github.io/sherpa/onnx/sense-voice/pretrained.html), [GitHub release metadata](https://api.github.com/repos/k2-fsa/sherpa-onnx/releases/tags/asr-models). Quality tradeoffs above are expectations/inferences, not measured results in this app.


## Reproduction and local mirror

`bash release/scripts/render-final-screenshots.sh`, `bash release/scripts/build-final-1.0.3.sh`, `python3 release/scripts/verify-final-1.0.3.py`. Use the ignored pinned model cache. Scripts use password-file arguments without printing secrets. Validation supports archived evidence after removal of generated intermediates.

Public `release/repo/android` is checked byte-identical against source with generated weights/build/cache directories excluded. Final docs/evidence/screenshots are mirrored. Gitleaks 8.30.1 directory/history and the existing credential/artifact scanner all returned zero findings; scan logs and local commit are recorded in STATE.md. No remote operation is performed. Final generated pack copies, build directories and APK sets are removed; model cache and final artifacts remain.
