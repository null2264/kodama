import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(androidx.plugins.kodama.kmp.library)
    alias(kotlinx.plugins.compose)
    alias(kotlinx.plugins.compose.compiler)
    alias(kotlinx.plugins.serialization)
}

kotlin {
//    iosX64()
//    iosArm64()
//    iosSimulatorArm64()
    jvm()
    sourceSets {
        commonMain.dependencies {
            implementation(libs.koin.core)
            implementation(libs.kermit)

            implementation(kotlinx.compose.foundation)

            implementation(kotlinx.coroutines.core)

            implementation(libs.settings)
            implementation(libs.settings.coroutines)
            implementation(libs.settings.make.observable)
        }
        androidMain.dependencies {
            implementation(androidx.core)
            implementation(androidx.preference)
        }
//        appleMain.dependencies {
//        }
    }

    android {
        namespace = "kodama.preferences"
    }
}

tasks {
    withType<KotlinCompile> {
        compilerOptions.freeCompilerArgs.addAll(
            "-Xexpect-actual-classes",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.serialization.ExperimentalSerializationApi",
        )
    }
}
