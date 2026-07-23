import com.android.build.api.dsl.ApplicationBaseFlavor
import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class AndroidAppPlugin : Plugin<Project> {
    override fun apply(project: Project) = with(project) {
        plugins {
            alias(androidx.plugins.application)
            alias(androidx.plugins.kodama.android.base)
        }
    }
}
