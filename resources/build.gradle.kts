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
            api(kotlinx.bundles.compose)
        }
        androidMain.dependencies {
        }
//        iosMain.dependencies {
//        }
    }

    android {
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
