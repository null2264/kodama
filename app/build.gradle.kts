import org.jetbrains.compose.ExperimentalComposeLibrary
import com.android.build.api.dsl.ManagedVirtualDevice
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree

plugins {
    alias(androidx.plugins.application)
    alias(kotlinx.plugins.compose)
    alias(kotlinx.plugins.compose.compiler)
    alias(kotlinx.plugins.multiplatform)
    alias(kotlinx.plugins.serialization)
    alias(libs.plugins.buildconfig)
    alias(libs.plugins.sqldelight)
}

kotlin {
    androidTarget {
        //https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-test.html
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        instrumentedTestVariant {
            sourceSetTree.set(KotlinSourceSetTree.test)
            dependencies {
                debugImplementation(androidx.compose.test.manifest)
                implementation(androidx.compose.test.junit4)
            }
        }
    }

    // listOf(
    //     iosX64(),
    //     iosArm64(),
    //     iosSimulatorArm64()
    // ).forEach {
    //     it.binaries.framework {
    //         baseName = "BonsaiKt"
    //         isStatic = true
    //     }
    // }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core)
            implementation(projects.resources)

            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.voyager.navigator)
            implementation(libs.coil)
            implementation(libs.coil.network.ktor)
            implementation(kotlinx.coroutines.core)
            implementation(libs.icons.feather)
            implementation(kotlinx.serialization.json)

            implementation(libs.koin.compose)

            implementation(project.dependencies.platform(libs.supabase.bom))
            implementation(libs.bundles.supabase)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            @OptIn(ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
            implementation(kotlinx.coroutines.test)
        }

        androidMain.dependencies {
            implementation(compose.uiTooling)
            implementation(androidx.activity.compose)
            implementation(kotlinx.coroutines.android)
            implementation(libs.sqldelight.driver.android)
        }

        // iosMain.dependencies {
        //     implementation(libs.sqldelight.driver.native)
        // }
    }
}

android {
    namespace = "io.github.null2264.bonsai"

    defaultConfig {
        applicationId = "io.github.null2264.bonsai.androidApp"
        versionCode = 1
        versionName = "0.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    sourceSets["main"].apply {
        manifest.srcFile("src/androidMain/AndroidManifest.xml")
        res.srcDirs("src/androidMain/res")
    }
    buildFeatures {
        compose = true
    }
}

buildConfig {
    // BuildConfig configuration here.
    // https://github.com/gmazzo/gradle-buildconfig-plugin#usage-in-kts
}

sqldelight {
    databases {
        create("Database") {
            packageName.set("io.github.null2264.bonsai.db")
        }
    }
}
