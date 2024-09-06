package dev.ktxvulkan.graphics.utils

enum class OSType {
    WINDOWS, MACOS, LINUX, OTHER
}

fun getOS(): OSType {
    val os = System.getProperty("os.name", "generic").lowercase()
    val result = when {
        os.indexOf("mac") >= 0 || os.indexOf("darwin") >= 0 -> {
            OSType.MACOS
        }
        os.indexOf("win") >= 0 -> {
            OSType.WINDOWS
        }
        os.indexOf("nux") >= 0 -> {
            OSType.LINUX
        }
        else -> {
            OSType.OTHER
        }
    }

    return result
}
