# 模型准备 / Model setup

Weights are deliberately excluded from Git and APK. Use the app's explicit setup/download action. No account, API token or cloud inference is required. A network connection to GitHub and Hugging Face is needed for these downloads; no media is sent to either service.

| Model | Exact artifact / origin | Terms |
| --- | --- | --- |
| EmbeddingGemma 2 740M | [`embeddinggemma-2-740m.litertlm`](https://huggingface.co/litert-community/embeddinggemma-2-740m-litert-lm/resolve/24d962e906c7d332c6428e71c9676855024569e2/embeddinggemma-2-740m.litertlm), expected 484,622,336 bytes | Apache-2.0 per variant/base model cards |
| SenseVoiceSmall int8 ONNX | [sherpa release archive](https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-sense-voice-zh-en-ja-ko-yue-2024-07-17.tar.bz2); extracts `model.int8.onnx` and `tokens.txt` | Underlying weights: FunASR custom model agreement; preserve SenseVoiceSmall / FunASR / FunAudioLLM / Alibaba attribution, plus k2-fsa conversion attribution |
| Silero VAD | [`silero_vad.onnx`](https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/silero_vad.onnx) | MIT; preserve Silero Team notice |

模型下载由用户主动触发。Embedding 文件约 485 MB（十进制）；SenseVoice 下载归档约 1.05 GB，仅保留 239,233,841 字节 ONNX 与 315,894 字节 tokens；归档解压期间需要额外空间。下载失败可重试，勿在应用运行中手动替换模型。模型文件位于应用专用 `models/` 目录；卸载应用会移除这些模型，源视频不受影响。

Downloads use a fixed HF commit and explicit GitHub `asr-models` release-tag assets, with SHA-256 constants measured from complete downloads on the build box. The release tag is not a content-addressed identifier: the hashes below enforce the exact approved bytes even if an upstream release asset is replaced. Existing installed weights are checked as well. Corrupt files are deleted; a corrupt download is retried once, then fails without accepting partial bytes. The ASR archive is checked before extraction, and ONNX/tokens are checked independently afterwards. Release builds cannot import or fall back to `/data/local/tmp`; debug imports still require these digests. Temporary build-box weights were deleted after verification.

| Artifact | Bytes | SHA-256 |
| --- | ---: | --- |
| embedding | 484622336 | `e7a8a2204b91e0f96e92960e84a09a89212e1633dcb7575a9bf3378b4df77f4c` |
| sensevoice_archive | 1047870769 | `f6b2a72ebcb1ac7a764d4cfccd886e6bcb2a95c4657c2199d0ba95ed4b9ea71a` |
| tokens.txt | 315894 | `f449eb28dc567533d7fa59be34e2abca8784f771850c78a47fb731a31429a1dc` |
| model.int8.onnx | 239233841 | `c71f0ce00bec95b07744e116345e33d8cbbe08cef896382cf907bf4b51a2cd51` |
| vad | 643854 | `9e2449e1087496d8d4caba907f23e0bd3f78d91fa552479bb9c23ac09cbb1fd6` |


Only transcript text is embedded. The 740M file includes multimodal capabilities, but Jingku does not use image/audio embeddings or index visual scenes. Video thumbnails are display previews, not search inputs. Silero only detects speech segments; SenseVoice performs transcription.

See [license audit](LICENSING.md), especially the unresolved upstream commercial-use interpretation. Do not assume source-code MIT licensing covers downloaded model weights.

The database stores local transcripts and embeddings. Schema generation 6 asks users of earlier experimental builds to explicitly clear/rebuild their old index; there is no silent schema migration. Search scans stored dialogue vectors in memory and returns three windows without deduplication. Large libraries and physical-device memory/thermal behavior are not characterized in this candidate.
