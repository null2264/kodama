import com.android.build.api.dsl.ApplicationBaseFlavor
import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class AndroidPlugin : Plugin<Project> {
    override fun apply(project: Project) = with(project) {
        android {
            compileSdk {
                version = release(AndroidConfig.compileSdk)
            }
            ndkVersion = AndroidConfig.ndk

            defaultConfig.apply {
                minSdk = AndroidConfig.minSdk
                if (this is ApplicationBaseFlavor) {
                    targetSdk = AndroidConfig.targetSdk
                }
            }

            compileOptions.apply {
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

private fun Project.android(block: CommonExtension.() -> Unit) {
    extensions.configure(block)
}
