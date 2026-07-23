import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(androidx.plugins.kodama.app)
    alias(kotlinx.plugins.compose)
    alias(kotlinx.plugins.compose.compiler)
}

android {
    namespace = "kodama.app"

    defaultConfig {
        applicationId = "io.github.null2264.kodama"
        versionCode = 1
        versionName = AppBuildConfig.VERSION

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

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        javaParameters.set(true)
    }
}

dependencies {
    implementation(projects.core.main)
    implementation(projects.core.preferences)
    implementation(projects.core.ui)

    implementation(compose.foundation)
    implementation(compose.material3)

    implementation(androidx.core)
    implementation(androidx.activity.compose)
    implementation(libs.koin.android)
    implementation(kotlinx.coroutines.android)
}
