import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(kotlinx.plugins.compose) apply false
    alias(kotlinx.plugins.compose.compiler) apply false
    alias(kotlinx.plugins.serialization) apply false
    alias(libs.plugins.buildconfig) apply false
    alias(libs.plugins.sqldelight) apply false
    alias(libs.plugins.dotenv)
}

subprojects {
    tasks.withType<KotlinCompile> {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
            freeCompilerArgs.addAll(
                "-Xjdk-release=${JavaVersion.VERSION_17}",
                "-Xexpect-actual-classes",
                "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
                "-opt-in=kotlinx.serialization.ExperimentalSerializationApi",
                "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
                "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
                "-opt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
            )
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
}
