import com.android.build.gradle.BaseExtension
import com.android.build.gradle.BasePlugin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.buildconfig) apply false
    alias(libs.plugins.kotlinx.serialization) apply false
    alias(libs.plugins.sqldelight) apply false
}

subprojects {
    tasks.withType<KotlinCompile> {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
            freeCompilerArgs.add("-Xjdk-release=${JavaVersion.VERSION_17}")
        }
    }

    /*
    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            events(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
        }
    }
     */

    plugins.withType<BasePlugin> {
        configure<BaseExtension> {
            compileSdkVersion(35)
            ndkVersion = "23.1.7779620"

            defaultConfig {
                minSdk = 23
                targetSdk = 35
            }

            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
                isCoreLibraryDesugaringEnabled = true
            }

            dependencies {
                add("coreLibraryDesugaring", libs.desugar)
            }
        }
    }
}
