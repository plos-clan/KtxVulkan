package dev.ktxvulkan.graphics.vk

import dev.ktxvulkan.graphics.utils.vkCheckResult
import dev.ktxvulkan.graphics.vk.DeviceManager.Companion.deviceExtensions
import io.github.oshai.kotlinlogging.KLoggable
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO
import org.lwjgl.vulkan.VK10.vkCreateDevice
import org.lwjgl.vulkan.VK10.vkDestroyDevice
import org.lwjgl.vulkan.VK10.vkDeviceWaitIdle
import org.lwjgl.vulkan.VK10.vkGetDeviceQueue
import org.lwjgl.vulkan.VK11.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FEATURES_2
import org.lwjgl.vulkan.VK11.vkGetPhysicalDeviceFeatures2
import org.lwjgl.vulkan.VK12.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_2_FEATURES
import org.lwjgl.vulkan.VK13.VK_API_VERSION_1_3
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkDeviceCreateInfo
import org.lwjgl.vulkan.VkDeviceQueueCreateInfo
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures2
import org.lwjgl.vulkan.VkPhysicalDeviceVulkan12Features
import org.lwjgl.vulkan.VkQueue

class Device(val instance: Instance, val physicalDevice: PhysicalDevice) : KLoggable {
        override val logger = logger()
        val vkDevice: VkDevice
        val graphicsQueue: VkQueue
        val presentQueue: VkQueue

        val drawIndirectSupported: Boolean

        init {
            MemoryStack.stackPush().use { stack ->
                val uniqueQueueFamilies = listOf(
                    physicalDevice.queueFamilyIndices.graphicsFamily,
                    physicalDevice.queueFamilyIndices.presentFamily
                )
                logger.info("queue families: {}", uniqueQueueFamilies)
                val queueCreateInfos = VkDeviceQueueCreateInfo.calloc(uniqueQueueFamilies.size, stack)
                val queuePriority = stack.callocFloat(1)
                queuePriority.put(0, 1.0f)
                uniqueQueueFamilies.forEachIndexed { index, queueFamily ->
                    queueCreateInfos[index]
                        .sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                        .queueFamilyIndex(queueFamily)
                        .pQueuePriorities(queuePriority)
                }

                val vkPhysicalDeviceVulkan12Features = VkPhysicalDeviceVulkan12Features.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_2_FEATURES)
                val vkPhysicalDeviceFeatures2 = VkPhysicalDeviceFeatures2.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FEATURES_2)
                    .pNext(vkPhysicalDeviceVulkan12Features)
                vkGetPhysicalDeviceFeatures2(physicalDevice.vkPhysicalDevice, vkPhysicalDeviceFeatures2)

                val ppValidationLayers = stack.callocPointer(instance.validationLayers.size)
                instance.validationLayers.forEach { s ->
                    ppValidationLayers.put(stack.ASCII(s))
                }
                ppValidationLayers.flip()

                val ppExtensionNames = stack.callocPointer(deviceExtensions.size)
                deviceExtensions.forEach { s ->
                    ppExtensionNames.put(stack.ASCII(s))
                }
                ppExtensionNames.flip()

                val vkDeviceCreateInfo = VkDeviceCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
                    .pQueueCreateInfos(queueCreateInfos)
                    .pEnabledFeatures(vkPhysicalDeviceFeatures2.features())
                    .ppEnabledExtensionNames(ppExtensionNames)
                    .ppEnabledLayerNames(ppValidationLayers)
                    .pNext(vkPhysicalDeviceVulkan12Features)

                val pDevice = stack.callocPointer(1)
                val vkCreateDeviceResult =
                    vkCreateDevice(physicalDevice.vkPhysicalDevice, vkDeviceCreateInfo, null, pDevice)
                vkCheckResult(vkCreateDeviceResult, "failed to create result")
                logger.info("created logical device")
                vkDevice = VkDevice(pDevice[0], physicalDevice.vkPhysicalDevice, vkDeviceCreateInfo, VK_API_VERSION_1_3)

                val handleBuffer = stack.callocPointer(1)
                vkGetDeviceQueue(vkDevice, physicalDevice.queueFamilyIndices.graphicsFamily, 0, handleBuffer)
                graphicsQueue = VkQueue(handleBuffer[0], vkDevice)
                vkGetDeviceQueue(vkDevice, physicalDevice.queueFamilyIndices.presentFamily, 0, handleBuffer)
                presentQueue = VkQueue(handleBuffer[0], vkDevice)
                logger.info("successfully initialized queues")

                drawIndirectSupported = physicalDevice.vkPhysicalDeviceFeatures2.features().multiDrawIndirect()
                        && physicalDevice.vkPhysicalDeviceVulkan11Features.shaderDrawParameters()
            }
        }

        fun waitIdle() {
            vkDeviceWaitIdle(vkDevice)
        }

        fun destroy() {
            vkDestroyDevice(vkDevice, null)
        }
    }