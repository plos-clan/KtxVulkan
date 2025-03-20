package dev.ktxvulkan.graphics.utils

import io.github.oshai.kotlinlogging.KLoggable
import org.lwjgl.glfw.GLFW
import java.util.*

enum class OSType {
    WINDOWS, MACOS, LINUX, OTHER
}

private fun getOS(): OSType {
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

object Platform : KLoggable {
    override val logger = logger()
    val activePlat: Int = supportedPlat
    private val activeDE = determineDE()

    fun init() {
        GLFW.glfwInitHint(GLFW.GLFW_PLATFORM, activePlat)
        logger.info("Selecting Platform: {}", getStringFromPlat(activePlat))
        logger.info("GLFW: {}", GLFW.glfwGetVersionString())
        GLFW.glfwInit()
    }

    // Actually detect the currently active Display Server (if both Wayland and X11 are present on the system and/or GLFW is compiled to support both)
    private fun determineDisplayServer(): Int {
        // Return Null platform if not on Linux (i.e. no X11 or Wayland)

        val xdgSessionType = System.getenv("XDG_SESSION_TYPE")
        if (xdgSessionType == null) return GLFW.GLFW_ANY_PLATFORM // Likely Android

        return when (xdgSessionType) {
            "wayland" -> GLFW.GLFW_PLATFORM_WAYLAND
            "x11" -> GLFW.GLFW_PLATFORM_X11
            else -> GLFW.GLFW_ANY_PLATFORM
        }
    }

    private val supportedPlat: Int
        get() {
            // Switch statement would be ideal, but couldn't find a good way of implementing it, so fell back to basic if statements/branches
            if (getOS() == OSType.WINDOWS) return GLFW.GLFW_PLATFORM_WIN32
            if (getOS() == OSType.MACOS) return GLFW.GLFW_PLATFORM_COCOA
            if (getOS() == OSType.LINUX) return determineDisplayServer() // Linux Or Android

            return GLFW.GLFW_ANY_PLATFORM // Unknown platform
        }

    private fun getStringFromPlat(plat: Int): String {
        return when (plat) {
            GLFW.GLFW_PLATFORM_WIN32 -> "WIN32"
            GLFW.GLFW_PLATFORM_WAYLAND -> "WAYLAND"
            GLFW.GLFW_PLATFORM_X11 -> "X11"
            GLFW.GLFW_PLATFORM_COCOA -> "MACOS"
            GLFW.GLFW_ANY_PLATFORM -> "ANDROID"
            else -> throw IllegalStateException("Unexpected value: $plat")
        }
    }

    private fun determineDE(): String {
        val xdgSessionDesktop = System.getenv("XDG_SESSION_DESKTOP")
        val xdgCurrentDesktop = System.getenv("XDG_CURRENT_DESKTOP")
        if (xdgSessionDesktop != null) return xdgSessionDesktop.lowercase(Locale.getDefault())
        if (xdgCurrentDesktop != null) return xdgCurrentDesktop.lowercase(Locale.getDefault())
        return "N/A"
    }

    val isWayLand: Boolean
        //Allows platform specific checks to be handled
        get() = activePlat == GLFW.GLFW_PLATFORM_WAYLAND

    val isX11: Boolean
        get() = activePlat == GLFW.GLFW_PLATFORM_X11

    val isWindows: Boolean
        get() = activePlat == GLFW.GLFW_PLATFORM_WIN32

    val isMacOS: Boolean
        get() = activePlat == GLFW.GLFW_PLATFORM_COCOA

    val isAndroid: Boolean
        get() = activePlat == GLFW.GLFW_ANY_PLATFORM

    val isGnome: Boolean
        //Desktop Environment Names: https://wiki.archlinux.org/title/Xdg-utils#Usage
        get() = activeDE.contains("gnome")

    val isWeston: Boolean
        get() = activeDE.contains("weston")

    val isGeneric: Boolean
        get() = activeDE.contains("generic")
}
