package com.broll.poc

object AppConfig {
    // Downloaded and SHA-256 measured on the build box; see docs/MODELS.md.
    const val MODEL_SHA256 = "2d079ee2f6f066b1f368e8d7c819f55214eaef1d0513b312321901f30ab286fb"
    const val ASR_ARCHIVE_SHA256 = "f6b2a72ebcb1ac7a764d4cfccd886e6bcb2a95c4657c2199d0ba95ed4b9ea71a"
    const val ASR_ONNX_SHA256 = "c71f0ce00bec95b07744e116345e33d8cbbe08cef896382cf907bf4b51a2cd51"
    const val ASR_VOCAB_SHA256 = "f449eb28dc567533d7fa59be34e2abca8784f771850c78a47fb731a31429a1dc"
    const val VAD_SHA256 = "9e2449e1087496d8d4caba907f23e0bd3f78d91fa552479bb9c23ac09cbb1fd6"

    const val MODEL_FILE_NAME = "embeddinggemma-2-text-270m.litertlm"
    const val MODEL_URL =
        "https://huggingface.co/litert-community/embeddinggemma-2-text-270m-litert-lm/resolve/9be6e8b90982095dc05c2bd162e4b954ee4dbac7/embeddinggemma-2-text-270m.litertlm"

    /** Hugging Face sibling size for the CPU/GPU litertlm file, as of 2026-10-06. */
    const val MODEL_EXPECTED_BYTES = 164_626_432L

    /**
     * LiteRT-LM 0.18 embedding 文档里的搜索查询前缀。
     * 用户输入先 trim，再拼在前面。图像和音频不加这段前缀。
     * https://developers.google.com/edge/litert-lm/embedding_models
     */
    const val QUERY_PREFIX = "task: search query | text: "

    /** LiteRT-LM 文档 Document Indexing 前缀（与查询前缀不对称）。 */
    const val INDEX_PREFIX = "task: search result | text: "

    /** 索引库元数据：嵌入前缀 / schema 代次；变更后须清空重建。 */
    const val INDEX_META_GENERATION = 7
    const val INDEX_EMBED_PREFIX_ID = "litert-search-result-270m-v2"

    const val FULL_DIM = 768
    const val TRUNC_DIM = 256

    const val AUDIO_WINDOW_SEC = 30.0
    const val AUDIO_HOP_SEC = 15.0
    const val AUDIO_SAMPLE_RATE = 16_000

    /** SenseVoice small int8 (sherpa-onnx release asr-models). */
    const val ASR_MODEL_DIR = "sherpa-onnx-sense-voice-zh-en-ja-ko-yue-2024-07-17"
    const val ASR_MODEL_ONNX = "model.int8.onnx"
    const val ASR_TOKENS = "tokens.txt"
    const val ASR_ARCHIVE_URL =
        "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-sense-voice-zh-en-ja-ko-yue-2024-07-17.tar.bz2"
    const val VAD_MODEL_FILE = "silero_vad.onnx"
    const val VAD_MODEL_URL =
        "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/silero_vad.onnx"

    /** VAD 单段最长秒数；索引片段也按此上限切分。 */
    const val DIALOGUE_MAX_SEGMENT_SEC = 4.0

    const val SEARCH_TOP_K = 3

    /** 270M 按 iter1 S2 校准方法重新冻结：低于此余弦相似度显示「可能不准确」，仍保留 Top-3。 */
    const val SEMANTIC_LOW_CONFIDENCE = 0.8748952746391297


    /** 保留旧接口常量；270M 仅文本，不初始化图像/音频后端。 */
    const val VISION_TOKENS_PER_IMAGE = 140

    const val MAX_FRAME_EDGE = 768
}
