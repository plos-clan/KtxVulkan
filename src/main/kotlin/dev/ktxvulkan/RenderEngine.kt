package dev.ktxvulkan

import dev.ktxvulkan.graphics.Window
import dev.ktxvulkan.graphics.vk.Instance
import io.github.oshai.kotlinlogging.KLoggable
import org.lwjgl.glfw.GLFW.glfwPollEvents


object RenderEngine : KLoggable {
    override val logger = logger()

    lateinit var window: Window
    lateinit var instance: Instance

    fun initialize() {
        window = Window()
        instance = Instance(true)
    }

    fun run() {
        while (!window.shouldClose()) {
            glfwPollEvents()
            window.swapBuffer()
        }
    }

    fun cleanup() {
        instance.destroy()
        window.destroy()
    }
}