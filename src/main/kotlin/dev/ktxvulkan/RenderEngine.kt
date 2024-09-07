package dev.ktxvulkan

import dev.ktxvulkan.graphics.Window
import dev.ktxvulkan.graphics.vk.Device
import dev.ktxvulkan.graphics.vk.Instance
import dev.ktxvulkan.graphics.vk.PhysicalDevice
import dev.ktxvulkan.graphics.vk.Swapchain
import io.github.oshai.kotlinlogging.KLoggable
import org.lwjgl.glfw.GLFW.glfwPollEvents


object RenderEngine : KLoggable {
    override val logger = logger()

    lateinit var window: Window
    lateinit var instance: Instance
    lateinit var physicalDevice: PhysicalDevice
    lateinit var device: Device
    lateinit var swapchain: Swapchain

    fun initialize() {
        window = Window()
        instance = Instance(true)
        window.createSurface(instance)
        physicalDevice = PhysicalDevice(instance, window)
        device = Device(physicalDevice)
        swapchain = Swapchain(device, window)
    }

    fun run() {
        while (!window.shouldClose()) {
            glfwPollEvents()
            window.swapBuffer()
        }
    }

    fun cleanup() {
        swapchain.destroy()
        window.destroy()
        device.destroy()
        physicalDevice.destroy()
        instance.destroy()
    }
}