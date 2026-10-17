# 许可核查 / License audit — 2026-10-17

镜库原创应用代码采用 MIT；这不改变依赖与模型各自的许可。APK 不包含模型权重，公开源码不包含媒体或权重。应用直接从上游下载模型，保留模型名称和作者署名。本文记录核查结果，不作所有权重均属 MIT/Apache 的声明。

| Component | License / redistribution treatment | Primary source |
| --- | --- | --- |
| SenseVoice repository source | MIT; retain copyright/license | [LICENSE](https://github.com/FunAudioLLM/SenseVoice/blob/main/LICENSE) |
| Official SenseVoiceSmall weights and derivatives | Custom FunASR Model Open Source License Agreement v1.1; §2.2 requires source, author, model-name attribution; §4/5/6 conduct, termination, updates remain applicable. Not relicensed under MIT. | [Model card](https://huggingface.co/FunAudioLLM/SenseVoiceSmall), [MODEL_LICENSE](https://github.com/modelscope/FunASR/blob/main/MODEL_LICENSE) |
| sherpa-onnx v1.13.5 runtime | Apache-2.0; retain license and applicable notices | [Pinned LICENSE](https://github.com/k2-fsa/sherpa-onnx/blob/v1.13.5/LICENSE) |
| sherpa SenseVoice ONNX conversion | A converted model is separate from runtime code. Source: SenseVoiceSmall by FunASR/FunAudioLLM / Alibaba; conversion/distribution: k2-fsa. Do not assume runtime Apache license grants new rights in underlying weights. No redistribution in this repo/APK. | [Conversion/download docs](https://k2-fsa.github.io/sherpa/onnx/sense-voice/pretrained.html) |
| EmbeddingGemma 2 740M LiteRT file | Model card declares Apache-2.0, base google/embeddinggemma-2 also declares Apache-2.0. This is version 2, not the older Gemma-terms model. Retain license/notices if redistributing; no weights bundled here. | [Variant card](https://huggingface.co/litert-community/embeddinggemma-2-740m-litert-lm), [Base card](https://huggingface.co/google/embeddinggemma-2) |
| LiteRT-LM | Apache-2.0 | [LICENSE](https://github.com/google-ai-edge/LiteRT-LM/blob/main/LICENSE) |
| Silero VAD | MIT; downloaded separately | [LICENSE](https://github.com/snakers4/silero-vad/blob/master/LICENSE) |
| ONNX Runtime native dependency | MIT | [LICENSE](https://github.com/microsoft/onnxruntime/blob/main/LICENSE) |
| AndroidX / Material Components / Commons Compress / Kotlin coroutines | Apache-2.0; retain upstream notices; Gradle resolves binaries, not vendored source | [AndroidX](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/LICENSE.txt), [Material](https://github.com/material-components/material-components-android/blob/master/LICENSE), [Commons](https://github.com/apache/commons-compress/blob/master/LICENSE.txt), [coroutines](https://github.com/Kotlin/kotlinx.coroutines/blob/master/LICENSE.txt) |

## SenseVoice commercial-use interpretation remains unresolved

The repository README links a permissive interpretation of §3. However, [issue #334](https://github.com/QwenAudio/SenseVoice/issues/334#issuecomment-5083546605) currently labels the earlier answer as discussion history, says it is **not final license-owner confirmation**, and remains open pending core-maintainer/license-owner confirmation. Do not market this as unconditional commercial clearance, and do not treat the conversion artifact as automatically covered by a broader runtime license. The source repository can be shared under its own MIT terms with these separate model notices. Before commercial production or repackaging converted weights, resolve the upstream interpretation and exact artifact notices.

## Attribution

SenseVoiceSmall by FunASR / FunAudioLLM (Alibaba Group), converted for sherpa-onnx by k2-fsa. EmbeddingGemma 2 by Google DeepMind, LiteRT conversion by litert-community. sherpa-onnx by the sherpa development team. Silero VAD by Silero Team. LiteRT-LM by Google AI Edge. ONNX Runtime by Microsoft. AndroidX by The Android Open Source Project. Material Components by Google. Commons Compress by The Apache Software Foundation.

Text snapshots are in `licenses/`; primary links above are authoritative. APK ships license text assets. We have not modified vendored dependency source. Downloading from upstream does not waive the model terms.
