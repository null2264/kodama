plugins {
    `kotlin-dsl`
}

gradlePlugin {
    plugins {
        register("kmp.android.jvm") {
            id = "kmp.android.jvm"
            implementationClass = "KmpAndroidJvmPlugin"
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
