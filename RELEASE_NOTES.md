# Jingku / 镜库 1.0.2 — local release candidate

中文 pitch：用一句话，找回本地视频里说过的那一刻。

English pitch: Find the moment someone said it—in your local videos, with a sentence.

- 中文优先，可切英文，Material 3 深浅色和矢量 adaptive icon。
- 导入视频、可取消索引与逐片/总进度，余弦 Top-3 卡片、台词高亮、缩略图、时间跳转播放。
- 低于 iter1 冻结 S2 参考阈值时显示“可能不准确”；不做硬拒答。
- 判断模型路线终止；仅 SenseVoice + EmbeddingGemma 文本检索。仅 arm64-v8a，Android API 26+。
- 历史 real-test Top-3 时间命中 75.81%（约 76%）、Top-1 58.06%；历史阈值策略拒答 58%、误接受 28.95%。AI 审计标签，非人工金标；不是本 APK 实测准确率。未在真机测试。
- 模型权重、视频、密钥和 keystore 均不入库。模型使用自己的许可，SenseVoice 权重的商用解释仍待上游最终确认。

This is a locally built release candidate, not a published store release. See README for metric denominators, offline/Android protocol differences and license limitations. UI preview screenshots, when provided, use explicitly marked demo data and are not inference evidence. Real physical-device indexing/search/playback, thermal/memory checks and model-distribution review remain release gates. No remote repository has been created or pushed.

Build note: release shrinking/obfuscation is disabled. The inherited Kotlin/R8 combination emitted metadata warnings and an actual Metaspace OOM; this candidate uses D8 with JNI/runtime names preserved. `testReleaseUnitTest` includes the full-768 retrieval regression in addition to the inherited FIR/resampling, seek, FTS-schema and SQLite transaction tests. Artifact validation is recorded in `docs/BUILD_VALIDATION.json`.

1.0.1 (versionCode 2): fixed EmbeddingGemma to HF commit 24d962e906c7d332c6428e71c9676855024569e2, retained the explicit GitHub asr-models release tag and pinned SHA-256 for the ASR archive, extracted ONNX/tokens and VAD. Existing files and downloads are verified; corrupt files are deleted and a corrupt download is retried once. Local tmp import/fallback is debug-only. Release lint is enabled. Fixed per-video dialogue SQL counting with regression coverage, localized the seek fallback note, and stopped computing/storing unused 256-D vectors (legacy column remains empty). Corrected labelled HTML renders with attribution and optional VAD.

1.0.2 (versionCode 3): fixes an early-startup null application-context access in locale setup. Installs a crash guard before locale initialization; Java/Kotlin uncaught exceptions save device/version details and stack traces locally, then chain to Android’s handler. On next launch, a Chinese/English dialog offers Copy, Share and Dismiss/delete. Initialization errors use an in-app fallback screen. Initial SQLite/native loading and model verification run on IO; metrics file/probe failures no longer prevent startup. Added crash-report formatting, UTF-8 write/overwrite and handler-chain/disk-failure unit coverage. Signed with the same release certificate as 1.0.1. Static triage and build checks passed; the reported phone failure has not been reproduced on a device. Native signals/ANRs and installation failures do not produce Java crash reports. See docs/CRASHFIX-1.0.2.md for evidence and device follow-up.
