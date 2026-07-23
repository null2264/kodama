import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.android.build.api.dsl.release
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KmpAndroidJvmPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.configure<KotlinMultiplatformExtension> {
            targets.withType<KotlinMultiplatformAndroidLibraryTarget>().configureEach {
                compileSdk {
                    version = release(AndroidConfig.compileSdk)
                }
                minSdk {
                    version = release(AndroidConfig.minSdk)
                }
                enableCoreLibraryDesugaring = true
            }
        }

        project.dependencies {
            add("coreLibraryDesugaring", project.libs.findLibrary("desugar").get())
        }
    }
}
