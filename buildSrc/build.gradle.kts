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
