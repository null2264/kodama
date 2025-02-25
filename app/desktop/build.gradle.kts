import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(kotlinx.plugins.multiplatform)
    alias(kotlinx.plugins.compose)
    alias(kotlinx.plugins.compose.compiler)
    alias(kotlinx.plugins.serialization)
}

kotlin {
    jvm()
    sourceSets {
        jvmMain.dependencies {
            implementation(projects.core.main)
            implementation(projects.core.preferences)
            implementation(projects.core.ui)

            implementation(compose.desktop.currentOs)
        }
    }
}

compose.desktop {
    application {
        mainClass = "kodama.app.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Kodama"
            windows {
                msiPackageVersion = AppBuildConfig.MSI_VERSION
            }
            packageVersion = AppBuildConfig.VERSION
        }
    }
}
