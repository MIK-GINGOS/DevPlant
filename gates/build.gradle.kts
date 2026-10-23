// gates: the tools. Database, leaf photos, AI model. Runs fully offline, imports nothing of ours.
plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.mixesgates.devplant.gates"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 29
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    androidResources {
        // The .tflite file must stay uncompressed so it can be memory-mapped.
        noCompress += "tflite"
    }
}

dependencies {
    api(libs.room.runtime)
    annotationProcessor(libs.room.compiler)
    implementation(libs.litert)
}
