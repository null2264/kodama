import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(androidx.plugins.kmp.library)
    alias(kotlinx.plugins.compose)
    alias(kotlinx.plugins.compose.compiler)
    alias(kotlinx.plugins.multiplatform)
    alias(kotlinx.plugins.serialization)
}

kotlin {
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    jvm("desktop")
    sourceSets {
        commonMain.dependencies {
            api(kotlinx.compose.runtime)
            api(kotlinx.compose.components)
        }
        androidMain.dependencies {
        }
        iosMain.dependencies {
        }
    }

    androidLibrary {
        namespace = "kodama.resources"
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "kodama.resources"
}

tasks {
    val localesConfigTask = getLocalesConfigTask()

    withType<KotlinCompile> {
        dependsOn(localesConfigTask)

        compilerOptions.freeCompilerArgs.addAll(
            "-Xexpect-actual-classes",
        )
    }
}
