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
    jvm()

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain {
            dependencies {
                api(libs.kermit)
                api(libs.koin.core)

                implementation(projects.core.preferences)

                api(project.dependencies.platform(libs.supabase.bom))
                api(libs.bundles.supabase)
            }
        }

        val nonJsMain by creating {
            dependencies {
                api(libs.ktor.cio)
            }
        }

        androidMain {
            dependsOn(nonJsMain)
        }

        appleMain {
            dependsOn(nonJsMain)
        }

        jvmMain {
            dependsOn(nonJsMain)
        }
    }
}

android {
    namespace = "kodama.core"
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
