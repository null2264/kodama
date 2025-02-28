import java.io.File
import org.gradle.api.Project

fun Project.getLocalProperty(
    key: String,
    file: String = "local.properties",
    defaultFile: String = "local.defaults.properties",
): String {
    val localProperties = java.util.Properties()
    val localPropertiesFile = File(rootDir, file)
    if (localPropertiesFile.isFile) {
        java.io.InputStreamReader(java.io.FileInputStream(localPropertiesFile), Charsets.UTF_8).use { reader ->
            localProperties.load(reader)
        }
    }

    val localDefaultProperties = java.util.Properties()
    val localDefaultPropertiesFile = File(rootDir, defaultFile)
    if (localDefaultPropertiesFile.isFile) {
        java.io.InputStreamReader(java.io.FileInputStream(localDefaultPropertiesFile), Charsets.UTF_8).use { reader ->
            localDefaultProperties.load(reader)
        }
    } else error("Unable to load default local properties")

    return localProperties.getProperty(key, localDefaultProperties.getProperty(key))
}
