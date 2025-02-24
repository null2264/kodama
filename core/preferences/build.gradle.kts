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
            implementation(libs.koin.core)
            implementation(libs.kermit)

            implementation(kotlinx.coroutines.core)

            implementation(libs.settings)
            implementation(libs.settings.coroutines)
            implementation(libs.settings.make.observable)
        }
        androidMain.dependencies {
            implementation(androidx.core)
            implementation(androidx.preference)
        }
        appleMain.dependencies {
        }
    }
}

android {
    namespace = "kodama.preferences"
}

tasks {
    withType<KotlinCompile> {
        compilerOptions.freeCompilerArgs.addAll(
            "-Xexpect-actual-classes",
            "-Xcontext-receivers",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.serialization.ExperimentalSerializationApi",
        )
    }
}
