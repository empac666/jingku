# 1.0.4 (versionCode 5) — local release

中文：换用经同协议评估的 EmbeddingGemma 2 270M 纯文本模型，Top-3 与 740M 相同（47/62）；APK 减少约 320 MB，仍完整离线。低置信提示阈值重新校准；升级后提示清空旧索引并重新索引，原视频保留。新模型校验成功后清理旧提取文件。保留 1.0.3 界面、崩溃防护与 2024 SenseVoice；2025 ASR 有回退，未采用。未真机验证；仅本地构建，无发布。

English: Adopt evaluated EmbeddingGemma 2 text-270M: identical Top-3 hit decisions (47/62), about 320 MB smaller APK, fully offline. Recalibrated warning threshold and index-generation migration require re-indexing; original videos remain. Verified replacement cleans the obsolete extracted model. Keeps 1.0.3 UI/crash safeguards and 2024 SenseVoice; the 2025 ASR regressed and was rejected. Physical-device acceptance pending; local only.

APK: `out/jingku-1.0.4-arm64-v8a.apk`, 466,070,902 bytes, SHA-256 `9db753ac9a3f2c504ff8290033fb88b8cb545c49f58c28f5b827a260259de3de`.

AAB: `out/jingku-1.0.4.aab`, 317,681,969 bytes, SHA-256 `753d74c9731766e91b256ccb91daa903185f2848f012fd47ac4225a83fd2f1c0`.

38 release / 43 debug tests pass; lint 0 errors / 74 warnings; same signing certificate, v2/v3, 16KB native alignment, 60 native layout renders. [Evaluation](docs/EVAL-270M.md) · [release report](docs/REPORT-1.0.4.md). 1.0.3 artifacts retained.

# 1.0.3 (versionCode 4) — final local release

中文：离线模型随 APK/AAB 安装，无需首次联网下载。新增暖色 Material 3 深浅主题、搜索与台词卡片、导入队列与索引进度；修复活动索引时的空闲操作、最近搜索触控尺寸、结果元信息及许可文案。保留 1.0.2 启动防护与崩溃报告。权重再分发适用独立许可，SenseVoice 商用解释仍待确认。尚未真机验证；无上传或发布。

English: Models are bundled in the install-time asset pack; first launch prepares them offline. Warm Material 3 light/dark UI adds dialogue result cards, import queue and indexing progress. Round2 fixes indexing prompts, Recent touch size, metadata and localized license readability. 1.0.2 startup/crash safeguards remain. Separate model redistribution terms apply; SenseVoice commercial interpretation remains unresolved. Physical-device acceptance is pending; this is local only.

APK: `out/jingku-1.0.3-arm64-v8a.apk` (786,066,801 bytes), SHA-256 `7a2db10294ed4ac22b3c6b690387b62d2529d2c9e661cb9f99a8c1683fcdf45a`.

AAB: `out/jingku-1.0.3.aab` (580,466,169 bytes), SHA-256 `c677642d64fd0fb0f51158de2ca775b9c7b4e7f2b0db49dc1b6886a90137708e`.

Checks: 37 release / 42 debug tests; lint 0 errors / 74 warnings; same certificate, APK v2/v3, no INTERNET/debug, 16KB native alignment. 56 real Android layout renders. [Full report](docs/REPORT-1.0.3.md).

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
