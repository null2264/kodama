import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(androidx.plugins.kodama.kmp.library)
    alias(kotlinx.plugins.serialization)
    alias(libs.plugins.buildconfig)
}

kotlin {
//    iosX64()
//    iosArm64()
//    iosSimulatorArm64()
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

        val nonJsMain = create("nonJsMain") {
            dependencies {
                api(libs.ktor.cio)
            }

            dependsOn(commonMain.get())
        }

        androidMain {
            dependsOn(nonJsMain)
        }

//        appleMain {
//            dependsOn(nonJsMain)
//        }

        jvmMain {
            dependsOn(nonJsMain)
        }
    }

    android {
        namespace = "kodama.core"
    }
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

buildConfig {
    buildConfigField("String", "SUPABASE_URL", env.fetch("SUPABASE_URL"))
    buildConfigField("String", "SUPABASE_KEY", env.fetch("SUPABASE_KEY"))
}
