// mixes: the screens. Draws the interface and nothing else. Knows contracts, never gates.
plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.mixesgates.devplant.mixes"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }
    resourcePrefix = "mm_"

    defaultConfig {
        minSdk = 29
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    api(project(":contracts"))
    implementation(libs.appcompat)
    implementation(libs.activity.ktx)
    implementation(libs.fragment)
    implementation(libs.material)
    implementation(libs.viewpager2)
    implementation(libs.recyclerview)
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
}
