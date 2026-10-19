# Bundled offline models / 随应用离线模型

1.0.3 APK/AAB redistributes all four model files in the install-time `models` asset pack. No first-run network download, account or API token is needed; the merged manifest has no INTERNET permission. Weights remain excluded from source Git. 模型随应用安装，首次启动在后台准备，不需要联网。

| Model file | Bytes | Raw DEFLATE bytes | License |
| --- | ---: | ---: | --- |
| `embeddinggemma-2-740m.litertlm` | 484,622,336 | 391,865,923 | Apache-2.0 (EmbeddingGemma 2) |
| `model.int8.onnx` | 239,233,841 | 160,168,880 | FunASR model agreement v1.1 |
| `tokens.txt` | 315,894 | 135,378 | FunASR model agreement v1.1 |
| `silero_vad.onnx` | 643,854 | 508,088 | MIT (Silero) |

Total: 724,815,925 bytes (691.24 MiB). Only EmbeddingGemma is extracted into internal filesDir/models (484,622,336 bytes), with SHA verification, fsync/atomic rename and version+SHA marker. Later launches skip hashing when marker/size match. Speech models load directly through AssetManager. Allow at least 530 MiB free for extraction and reserve; APK+extracted embedding is about 1.18 GiB before app data/ART/videos and installation staging. Old downloaded models from upgrades may occupy additional storage.

For source builds, place the four exact approved files in `models-cache/` next to `android/` (ignored by Git). `:models:copyModels` checks every SHA before packaging. URLs/revision are pinned in AppConfig.kt. The mixed SenseVoice archive (1,047,870,769 bytes) is build cache only; extract just the int8 ONNX and tokens. Use `./gradlew testReleaseUnitTest lintRelease bundleRelease`; sign the AAB and use bundletool 1.18.3 universal mode for a single sideload APK. A regular base APK does not include the separate install-time pack.

| Artifact | Bytes | SHA-256 |
| --- | ---: | --- |
| embedding | 484622336 | `e7a8a2204b91e0f96e92960e84a09a89212e1633dcb7575a9bf3378b4df77f4c` |
| sensevoice_archive | 1047870769 | `f6b2a72ebcb1ac7a764d4cfccd886e6bcb2a95c4657c2199d0ba95ed4b9ea71a` |
| tokens.txt | 315894 | `f449eb28dc567533d7fa59be34e2abca8784f771850c78a47fb731a31429a1dc` |
| model.int8.onnx | 239233841 | `c71f0ce00bec95b07744e116345e33d8cbbe08cef896382cf907bf4b51a2cd51` |
| vad | 643854 | `9e2449e1087496d8d4caba907f23e0bd3f78d91fa552479bb9c23ac09cbb1fd6` |



Only transcript text is embedded. The 740M file includes unused multimodal capabilities. Thumbnails are display previews. Silero detects speech; SenseVoice transcribes. The historical warning threshold 0.7889297604560853 is unchanged; smaller models require a fresh quality/threshold evaluation.

Weights are REDISTRIBUTED in APK/AAB: retain FunASR v1.1 and attribution for SenseVoiceSmall; commercial-use interpretation remains unresolved. The exact pinned EmbeddingGemma **2** model declares Apache-2.0; older EmbeddingGemma 300M uses Gemma terms and is not substituted. Silero is MIT. See [full license audit](LICENSING.md), bundled full texts in `licenses/`, and [final report](REPORT-1.0.3.md) for smaller candidates and measured delivery sizes.
