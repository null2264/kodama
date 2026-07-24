import com.android.build.api.dsl.ManagedVirtualDevice
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(androidx.plugins.kodama.kmp.library)
    alias(kotlinx.plugins.compose)
    alias(kotlinx.plugins.compose.compiler)
    alias(kotlinx.plugins.serialization)
    alias(libs.plugins.sqldelight)
}

kotlin {
//    iosX64()
//    iosArm64()
//    iosSimulatorArm64()
    jvm()
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.preferences)
            implementation(projects.resources)

            implementation(androidx.compose.material3)
            implementation(kotlinx.bundles.compose)
            implementation(kotlinx.bundles.compose.ui.tooling)
            implementation(libs.bundles.voyager)
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
            implementation(kotlinx.compose.ui.test)
            implementation(kotlinx.coroutines.test)
        }

        androidMain.dependencies {
            implementation(androidx.activity.compose)
            implementation(libs.sqldelight.driver.android)
        }

        //iosMain.dependencies {
        //    implementation(libs.sqldelight.driver.native)
        //}
    }

    android {
        namespace = "kodama.ui"
    }
}

tasks {
    withType<KotlinCompile> {
        compilerOptions.freeCompilerArgs.addAll(
            "-Xexpect-actual-classes",
            "-Xcontext-receivers",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.serialization.ExperimentalSerializationApi",
        )
    }
}

//sqldelight {
//    databases {
//        create("Database") {
//            packageName.set("io.github.null2264.bonsai.db")
//        }
//    }
//}
