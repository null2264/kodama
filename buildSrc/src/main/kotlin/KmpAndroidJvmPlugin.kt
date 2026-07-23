import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KmpAndroidJvmPlugin : Plugin<Project> {
    override fun apply(project: Project) = with(project) {
        kotlin {
            android {
                compileSdk = AndroidConfig.compileSdk
                minSdk = AndroidConfig.minSdk
                enableCoreLibraryDesugaring = true
            }
        }

        dependencies {
            "coreLibraryDesugaring"(libs.desugar)
        }
    }
}

private fun Project.kotlin(block: KotlinMultiplatformExtension.() -> Unit) {
    extensions.configure(block)
}

private fun KotlinMultiplatformExtension.android(block: KotlinMultiplatformAndroidLibraryTarget.() -> Unit) {
    targets.withType<KotlinMultiplatformAndroidLibraryTarget>().configureEach(block)
}
