plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.broll.poc"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.broll.poc"
        minSdk = 26
        targetSdk = 35
        versionCode = 5
        versionName = "1.0.4"
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    assetPacks += listOf(":models")
    androidResources { noCompress += listOf("onnx", "litertlm", "txt") }

    buildFeatures { buildConfig = true }
    lint { checkReleaseBuilds = true }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    // Official LiteRT-LM Android artifact (Google Maven, verified 0.18.0).
    // API: EmbeddingEngine / EmbeddingEngineConfig / InputData / EmbeddingOptions.
    implementation("com.google.ai.edge.litertlm:litertlm-android:0.18.0")
    // Official sherpa-onnx Android artifact via JitPack (see k2-fsa.github.io/sherpa/onnx/java-api/anroid-java.html)
    implementation("com.github.k2-fsa.sherpa-onnx:sherpa-onnx:v1.13.5")
    implementation("androidx.sqlite:sqlite-android:2.7.1")
    implementation("androidx.sqlite:sqlite-bundled-android:2.7.1")
    testImplementation("androidx.sqlite:sqlite-bundled-jvm:2.7.1")
    testImplementation("junit:junit:4.13.2")
}

// Screenshot fixtures live only in testDebug; release unit tests never execute them.
dependencies {
    testImplementation("org.robolectric:robolectric:4.17")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.76.0")
}
tasks.withType<Test>().configureEach {
    systemProperty("roborazzi.test.record", providers.gradleProperty("screenshots").getOrElse("false"))
    systemProperty("jingku.screenshots", providers.gradleProperty("screenshots").getOrElse("false"))
    systemProperty("jingku.screenshotStates", providers.gradleProperty("screenshotStates").getOrElse(""))
    systemProperty("jingku.screenshotDir", providers.gradleProperty("screenshotDir").getOrElse(rootProject.file("../screenshots/1.0.3/round1").absolutePath))
    systemProperty("robolectric.dependency.repo.url", "https://repo.maven.apache.org/maven2")
    maxHeapSize = "1536m"
    if (name == "testDebugUnitTest" && providers.gradleProperty("screenshots").getOrElse("false") == "true") {
        outputs.upToDateWhen { false }
    }
}
tasks.register("recordBaselineScreenshots") {
    group = "verification"
    description = "Render agreed UI states to round1 (-Pscreenshots=true); historical baseline is preserved"
    dependsOn("testDebugUnitTest")
}
