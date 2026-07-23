import org.gradle.api.Plugin
import org.gradle.api.Project

class KmpJvmPlugin : Plugin<Project> {
    override fun apply(project: Project) = with(project) {
        plugins {
            alias(kotlinx.plugins.multiplatform)
        }
    }
}
