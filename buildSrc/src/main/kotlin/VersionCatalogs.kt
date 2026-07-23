import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.accessors.dm.LibrariesForKotlinx
import org.gradle.accessors.dm.LibrariesForAndroidx
import org.gradle.api.Project
import org.gradle.api.plugins.PluginManager
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType

internal val Project.libs get() = the<LibrariesForLibs>()
internal val Project.androidx get() = the<LibrariesForAndroidx>()
internal val Project.kotlinx get() = the<LibrariesForKotlinx>()
