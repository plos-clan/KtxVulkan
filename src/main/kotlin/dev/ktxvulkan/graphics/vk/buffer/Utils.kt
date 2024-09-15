package dev.ktxvulkan.graphics.vk.buffer

import dev.ktxvulkan.graphics.vk.Device
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceMemoryProperties
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties

fun findMemoryType(device: Device, typeFilter: Int, properties: Int): Int {
    MemoryStack.stackPush().use { stack ->
        val memProperties = VkPhysicalDeviceMemoryProperties.calloc()
        vkGetPhysicalDeviceMemoryProperties(device.physicalDevice.vkPhysicalDevice, memProperties)

        for (i in 0..<memProperties.memoryTypeCount()) {
            if (memProperties.memoryTypes()[i].propertyFlags() and properties == properties
                && typeFilter and (1 shl i) != 0) return i
        }

        throw IllegalStateException("failed to find suitable memory type!");
    }
}