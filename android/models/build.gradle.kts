plugins { id("com.android.asset-pack") }

assetPack {
    packName.set("models")
    dynamicDelivery { deliveryType.set("install-time") }
}

// Weights remain in a cache outside the source tree; do not check generated assets in.
val copyModels by tasks.registering(Sync::class) {
    from(rootProject.layout.projectDirectory.dir("../models-cache")) {
        include("embeddinggemma-2-text-270m.litertlm", "model.int8.onnx", "tokens.txt", "silero_vad.onnx")
    }
    into(layout.projectDirectory.dir("src/main/assets/offline"))
    doFirst {
        val cache = rootProject.file("../models-cache")
        listOf("embeddinggemma-2-text-270m.litertlm", "model.int8.onnx", "tokens.txt", "silero_vad.onnx").forEach {
            check(cache.resolve(it).isFile) { "Missing pinned model: $it; run release/scripts/cache-models.py" }
        }
        val config = rootProject.file("app/src/main/java/com/broll/poc/AppConfig.kt").readText()
        mapOf("embeddinggemma-2-text-270m.litertlm" to "MODEL_SHA256", "model.int8.onnx" to "ASR_ONNX_SHA256",
            "tokens.txt" to "ASR_VOCAB_SHA256", "silero_vad.onnx" to "VAD_SHA256").forEach { (file, key) ->
            val expected = Regex("const val $key = \"([a-f0-9]+)\"").find(config)!!.groupValues[1]
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            cache.resolve(file).inputStream().use { input ->
                val buffer = ByteArray(1024 * 1024)
                while (true) { val n = input.read(buffer); if (n < 0) break; digest.update(buffer, 0, n) }
            }
            check(digest.digest().joinToString("") { "%02x".format(it) } == expected) { "Pinned SHA mismatch: $file" }
        }
    }
}
tasks.configureEach {
    if (name != "copyModels" && (name.contains("Asset") || name.startsWith("package") || name.startsWith("generate"))) {
        dependsOn(copyModels)
    }
}
