package dev.ktxvulkan

import dev.ktxvulkan.graphics.Window
import dev.ktxvulkan.graphics.vk.Instance
import dev.ktxvulkan.graphics.vk.PhysicalDevice
import io.github.oshai.kotlinlogging.KLoggable
import org.lwjgl.glfw.GLFW.glfwPollEvents


object RenderEngine : KLoggable {
    override val logger = logger()

    lateinit var window: Window
    lateinit var instance: Instance
    lateinit var physicalDevice: PhysicalDevice

    fun initialize() {
        window = Window()
        instance = Instance(true)
        window.createSurface(instance)
        physicalDevice = PhysicalDevice(instance, window)
    }

    fun run() {
        while (!window.shouldClose()) {
            glfwPollEvents()
            window.swapBuffer()
        }
    }

    fun cleanup() {
        window.destroy(instance)
        physicalDevice.destroy()
        instance.destroy()
    }
}