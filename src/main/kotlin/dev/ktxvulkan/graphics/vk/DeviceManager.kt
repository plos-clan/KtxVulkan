package dev.ktxvulkan.graphics.vk

import dev.ktxvulkan.graphics.Window
import io.github.oshai.kotlinlogging.KLoggable
import org.lwjgl.vulkan.EXTDescriptorIndexing.VK_EXT_DESCRIPTOR_INDEXING_EXTENSION_NAME
import org.lwjgl.vulkan.KHRBufferDeviceAddress.VK_KHR_BUFFER_DEVICE_ADDRESS_EXTENSION_NAME
import org.lwjgl.vulkan.KHRDeferredHostOperations.VK_KHR_DEFERRED_HOST_OPERATIONS_EXTENSION_NAME
import org.lwjgl.vulkan.KHRPipelineLibrary.VK_KHR_PIPELINE_LIBRARY_EXTENSION_NAME
import org.lwjgl.vulkan.KHRShaderFloatControls.VK_KHR_SHADER_FLOAT_CONTROLS_EXTENSION_NAME
import org.lwjgl.vulkan.KHRSpirv14.VK_KHR_SPIRV_1_4_EXTENSION_NAME
import org.lwjgl.vulkan.KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME

class DeviceManager(instance: Instance, window: Window) : KLoggable {
    override val logger = logger()
    val physicalDevice = PhysicalDevice(instance, window)
    val device = Device(instance, physicalDevice)

    val deviceProperties = physicalDevice.vkPhysicalDeviceProperties
    val memoryProperties = physicalDevice.vkPhysicalDeviceMemoryProperties

    val surfaceProperties = physicalDevice.surfaceProperties

    fun destroy() {
        device.destroy()
        physicalDevice.destroy()
    }

    companion object {
        val deviceExtensions = listOf(
            VK_KHR_SWAPCHAIN_EXTENSION_NAME,
            VK_EXT_DESCRIPTOR_INDEXING_EXTENSION_NAME,
            VK_KHR_BUFFER_DEVICE_ADDRESS_EXTENSION_NAME,
            "VK_KHR_synchronization2",
//            VK_KHR_ACCELERATION_STRUCTURE_EXTENSION_NAME,
//            VK_KHR_RAY_TRACING_PIPELINE_EXTENSION_NAME,
//            VK_KHR_RAY_QUERY_EXTENSION_NAME,
            VK_KHR_PIPELINE_LIBRARY_EXTENSION_NAME,
            VK_KHR_DEFERRED_HOST_OPERATIONS_EXTENSION_NAME,
            VK_KHR_SPIRV_1_4_EXTENSION_NAME,
            VK_KHR_SHADER_FLOAT_CONTROLS_EXTENSION_NAME
        )
    }
}