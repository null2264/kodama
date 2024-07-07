import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(androidx.plugins.library)
    alias(kotlinx.plugins.multiplatform)
    alias(kotlinx.plugins.serialization)
}

kotlin {
    androidTarget()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    sourceSets {
        commonMain.dependencies {
        }
        androidMain.dependencies {
        }
        iosMain.dependencies {
        }
    }
}

android {
    namespace = "bonsai.i18n"
}

tasks {
    withType<KotlinCompile> {
        compilerOptions.freeCompilerArgs.addAll(
            "-Xcontext-receivers",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.serialization.ExperimentalSerializationApi",
        )
    }
}
