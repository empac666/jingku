# 镜库 Jingku

用一句话，找回本地视频里说过的那一刻。

[English](README.md) · [许可核查](docs/LICENSING.md) · [发布说明](RELEASE_NOTES.md)

镜库是一款 Android 视频台词语义检索原型：选择本地视频，SenseVoice 转写，EmbeddingGemma 2 建立向量索引，输入中文或英文描述，查看余弦 Top-3 并点按跳转播放。视频不上传。模型首次下载需要联网，准备完成后索引和搜索在设备本地运行。

## 功能

- 中文优先，可切换英文；Material 3，随系统深浅色。
- 多选导入视频、串行索引、逐片和总进度、可取消。
- Top-3 卡片：真实视频帧缩略图、台词字面高亮、时间戳、点按播放。
- 每个结果的余弦低于 iter1 冻结 S2 阈值 `0.7889297604560853` 时显示“可能不准确”。分数不是正确概率。语义命中没有相同词时不伪造高亮。
- 保留候选供用户判断；不运行判断模型，不做硬拒答。

## 使用与构建

Android 8.0+（API 26），**仅 arm64-v8a**。推荐先在空闲、充电状态准备模型和索引；空间与内存需求取决于视频和模型。未在真机完成验证。

1. 安装签名 release APK（由项目发布者另行提供；源码仓库不存 APK）。
2. 准备 SenseVoiceSmall、Silero VAD、EmbeddingGemma 2 模型；遵守各自条款，详见 [模型说明](docs/MODELS.md)。
3. 导入你有权使用的视频并开始索引；搜索描述，点按候选定位播放。取消会移除当前未完成视频的部分索引，已经完成的视频保留。

构建需 JDK 17+、Android SDK 35、Build Tools 35.0.0。构建会从 Google Maven/Maven Central/JitPack 获取依赖。

```sh
cd android
export ANDROID_HOME=/path/to/android-sdk
./gradlew testReleaseUnitTest lintRelease assembleRelease
```

默认生成未签名 release APK。私钥应在源码树外保管；用 Android Build Tools 的 `zipalign`、`apksigner sign` 和 `apksigner verify` 本地签名验证。请勿把签名文件或密码提交到 Git。

## 真实评估，及其边界

历史 iter5 real-test：10 个视频、100 查询，其中 62 条有答案、38 条无答案；总素材库 14 个视频约 70 分钟。**Top-3 时间命中 75.81%（约 76%），Top-1 58.06%（约 58%）**，分母为有答案的 62 条；命中要求视频正确且窗口中点落在金标容忍时间内。视频聚类 bootstrap 95% 区间分别为 [65.57%, 86.44%] / [45.00%, 71.88%]。

历史阈值拒答策略（iter5 独立 dev 阈值 `0.743815303`）：开拒答后拒答率 **58.00%**（全部 100 查询），误接受率 **28.95%**（38 条无答案查询），接受且时间命中 33.87%（62 条有答案查询）。这些数字属于历史离线评估；当前应用始终显示 Top-3，只给低置信提示。

**标签由 AI 审计产生，非人工听辨金标。** Whisper 可能漏词或幻觉；审查者看过听者，投票不完全独立；31/140 条低置信仍保留，8 条曾因 ASR 证据不足由 yes 改成 no，ASR 漏词并不证明原音频无答案。素材来源集中于 VOA/ICANN；样本少、跨来源泛化有限。

**本 release APK 未在真机测试，也没有复跑整套端上准确率。** Android 沿用 iter2 LiteRT EmbeddingGemma 2 文件、前缀、≤4 秒台词窗口和 token 对齐/段起点跳转；历史离线评估的模型表示、查询前缀和窗口中点判定不同。因此 **76% 不是本 APK 的实测准确率**，iter1 阈值在此仅为历史参考提示，未重新校准。不保证语义相近即事实正确，不保证跨语言或嘈杂语音的效果。

判断模型路线在 iter6 未达到预注册成功标准，已经终止。发布链仅保留 SenseVoice + EmbeddingGemma 余弦 Top-3；没有 LLM 复核、翻译或云检索。

## 许可与隐私

原创应用源码 [MIT](LICENSE)。依赖/模型分别适用自己的许可；特别是 SenseVoiceSmall 权重采用自定义 FunASR 协议，不是 MIT，商用解释仍有上游待确认事项。详见 [LICENSING.md](docs/LICENSING.md)。权重、密钥、keystore、第三方视频均不在仓库。

模型从 GitHub/Hugging Face 上游下载；服务器会看到模型下载网络请求。选中的视频、转写和向量留在应用目录，卸载会删除应用数据；视频原件仍由原提供者保管。应用已禁用 Android 自动备份；仍请按数据敏感程度管理设备与文件共享。只导入有权处理的内容。

## 界面示意

以下为标注演示数据的 HTML 高保真渲染，不是 Android／真机截图，也不是实际推理结果。

[导入与索引 / Import](screenshots/01-import-index.png) · [Top-3](screenshots/02-search-results.png) · [播放 / Playback](screenshots/03-playback-seek.png) · [低置信 / Low confidence](screenshots/04-low-confidence.png)
