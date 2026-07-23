plugins {
    `java-gradle-plugin`
}

gradlePlugin {
    plugins {
        register("kodama.plugins.kmp.library") {
            id = androidx.plugins.kodama.kmp.library.get().pluginId
            implementationClass = "KmpLibraryPlugin"
        }
        register("kodama.plugins.library") {
            id = androidx.plugins.kodama.library.get().pluginId
            implementationClass = "AndroidLibPlugin"
        }
        register("kodama.plugins.app") {
            id = androidx.plugins.kodama.app.get().pluginId
            implementationClass = "AndroidAppPlugin"
        }
        register("kodama.plugins.android.base") {
            id = androidx.plugins.kodama.android.base.get().pluginId
            implementationClass = "AndroidPlugin"
        }
    }
}

repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
}

dependencies {
    compileOnly(gradleKotlinDsl())
    compileOnly(androidx.gradle)
    compileOnly(kotlinx.gradle)

    compileOnly(files(androidx::class.java.superclass.protectionDomain.codeSource.location))
    compileOnly(files(kotlinx::class.java.superclass.protectionDomain.codeSource.location))
    compileOnly(files(libs::class.java.superclass.protectionDomain.codeSource.location))
}
