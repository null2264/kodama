plugins {
    alias(androidx.plugins.application)
    alias(kotlinx.plugins.compose)
    alias(kotlinx.plugins.compose.compiler)
}

android {
    namespace = "kodama.app"

    defaultConfig {
        applicationId = "io.github.null2264.kodama"
        versionCode = 1
        versionName = "0.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        jniLibs {
            keepDebugSymbols += "**/libandroidx.graphics.path.so"
        }
    }
}

dependencies {
    implementation(projects.core.main)
    implementation(projects.core.preference)
    implementation(projects.core.ui)

    implementation(compose.foundation)
    implementation(compose.material3)

    implementation(androidx.activity.compose)
    implementation(libs.koin.core)
    implementation(libs.koin.android)
}
