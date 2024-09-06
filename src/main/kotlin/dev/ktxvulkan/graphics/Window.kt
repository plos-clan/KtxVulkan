package dev.ktxvulkan.graphics

import org.lwjgl.glfw.GLFW.*

class Window(val width: Int = 1600, val height: Int = 900) {
    private val handle: Long

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

    fun destroy() {
        glfwDestroyWindow(handle)
    }
}