package dev.ktxvulkan.graphics

import dev.ktxvulkan.graphics.utils.vkCheckResult
import dev.ktxvulkan.graphics.vk.Instance
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWVulkan.glfwCreateWindowSurface
import org.lwjgl.vulkan.KHRSurface.vkDestroySurfaceKHR

class Window(val width: Int = 1600, val height: Int = 900) {
    val handle: Long
    var surface: Long = 0; private set

    init {
        glfwInit()
        // No OpenGL stuffs
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API)
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE)
        handle = glfwCreateWindow(width, height, "KtxVulkan", 0, 0)
        glfwSetFramebufferSizeCallback(handle, ::onFramebufferResize)
    }

    private fun onFramebufferResize(window: Long, width: Int, height: Int) {

    }

    fun shouldClose() = glfwWindowShouldClose(handle)

    fun swapBuffer() {
        glfwSwapInterval(0)
    }

    fun createSurface(instance: Instance) {
        surface = LongArray(1).let {
            val glfwCreateWindowSurfaceResult = glfwCreateWindowSurface(instance.vkInstance, handle, null, it)
            vkCheckResult(glfwCreateWindowSurfaceResult, "failed to create window surface")
            it[0]
        }
    }

    fun destroy() {
        glfwDestroyWindow(handle)
        glfwTerminate()
    }
}