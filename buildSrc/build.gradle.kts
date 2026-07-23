plugins {
    alias(kotlinx.plugins.jvm)
    alias(kotlinx.plugins.sam.with.receiver)
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
        register("kodama.plugins.jvm") {
            id = androidx.plugins.kodama.jvm.get().pluginId
            implementationClass = "KmpJvmPlugin"
        }
    }
}


samWithReceiver {
    annotation("org.gradle.api.HasImplicitReceiver")
}

repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
}

dependencies {
    compileOnly(gradleKotlinDsl())
    implementation(androidx.gradle)
    implementation(kotlinx.gradle)

    compileOnly(files(androidx::class.java.superclass.protectionDomain.codeSource.location))
    compileOnly(files(kotlinx::class.java.superclass.protectionDomain.codeSource.location))
    compileOnly(files(libs::class.java.superclass.protectionDomain.codeSource.location))
}
