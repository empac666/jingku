# Jingku / 镜库 1.0.0 — local release candidate

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
