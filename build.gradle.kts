import com.android.build.gradle.BasePlugin
import com.android.build.api.dsl.CommonExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(androidx.plugins.application) apply false
    alias(androidx.plugins.library) apply false
    alias(androidx.plugins.kmp.library) apply false
    alias(kotlinx.plugins.android) apply false
    alias(kotlinx.plugins.compose) apply false
    alias(kotlinx.plugins.compose.compiler) apply false
    alias(kotlinx.plugins.multiplatform) apply false
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

    fun Project.configureAndroidJvm() {
        configure<CommonExtension> {
            compileSdkVersion(AndroidConfig.compileSdk)
            ndkVersion = AndroidConfig.ndk

            defaultConfig.apply {
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

    plugins.withId(androidx.plugins.application.get().pluginId) {
        configureAndroidJvm()
    }
    plugins.withId(androidx.plugins.kmp.library.get().pluginId) {
        configureAndroidJvm()
    }
    plugins.withId(androidx.plugins.library.get().pluginId) {
        configureAndroidJvm()
    }
}
