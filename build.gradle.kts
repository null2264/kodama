import com.android.build.gradle.BaseExtension
import com.android.build.gradle.BasePlugin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(androidx.plugins.application) apply false
    alias(androidx.plugins.library) apply false
    alias(kotlinx.plugins.multiplatform) apply false
    alias(kotlinx.plugins.compose) apply false
    alias(kotlinx.plugins.compose.compiler) apply false
    alias(kotlinx.plugins.serialization) apply false
    alias(libs.plugins.buildconfig) apply false
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
            compileSdkVersion(AndroidConfig.compileSdk)
            ndkVersion = AndroidConfig.ndk

            defaultConfig {
                minSdk = AndroidConfig.minSdk
                targetSdk = AndroidConfig.targetSdk
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
