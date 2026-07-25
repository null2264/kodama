package kodama.core.util

enum class OperatingSystem {
    ANDROID,
    IOS,
    JVM,
}

expect fun getCurrentOS(): OperatingSystem
