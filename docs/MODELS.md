# 模型准备 / Model setup

Weights are deliberately excluded from Git and APK. Use the app's explicit setup/download action. No account, API token or cloud inference is required. A network connection to GitHub and Hugging Face is needed for these downloads; no media is sent to either service.

| Model | Exact artifact / origin | Terms |
| --- | --- | --- |
| EmbeddingGemma 2 740M | [`embeddinggemma-2-740m.litertlm`](https://huggingface.co/litert-community/embeddinggemma-2-740m-litert-lm/resolve/main/embeddinggemma-2-740m.litertlm), expected 484,622,336 bytes | Apache-2.0 per variant/base model cards |
| SenseVoiceSmall int8 ONNX | [sherpa release archive](https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-sense-voice-zh-en-ja-ko-yue-2024-07-17.tar.bz2); extracts `model.int8.onnx` and `tokens.txt` | Underlying weights: FunASR custom model agreement; preserve SenseVoiceSmall / FunASR / FunAudioLLM / Alibaba attribution, plus k2-fsa conversion attribution |
| Silero VAD | [`silero_vad.onnx`](https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/silero_vad.onnx) | MIT; preserve Silero Team notice |

模型下载由用户主动触发。Embedding 文件约 485 MB（十进制）；SenseVoice 模型还需数百 MB，归档解压期间需要额外空间。下载失败可重试，勿在应用运行中手动替换模型。模型文件位于应用专用 `models/` 目录；卸载应用会移除这些模型，源视频不受影响。

The inherited downloader validates embedding size and basic ASR file sizes, not cryptographic model authenticity. HTTPS is used, but upstream `main`/release URLs are not content-pinned. No claim of hash-verified model supply-chain integrity is made. Before production distribution, pin upstream revision and digests, validate converted artifact notices and complete physical-device tests.

Only transcript text is embedded. The 740M file includes multimodal capabilities, but Jingku does not use image/audio embeddings or index visual scenes. Video thumbnails are display previews, not search inputs. Silero only detects speech segments; SenseVoice performs transcription.

See [license audit](LICENSING.md), especially the unresolved upstream commercial-use interpretation. Do not assume source-code MIT licensing covers downloaded model weights.

The database stores local transcripts and embeddings. Schema generation 6 asks users of earlier experimental builds to explicitly clear/rebuild their old index; there is no silent schema migration. Search scans stored dialogue vectors in memory and returns three windows without deduplication. Large libraries and physical-device memory/thermal behavior are not characterized in this candidate.
