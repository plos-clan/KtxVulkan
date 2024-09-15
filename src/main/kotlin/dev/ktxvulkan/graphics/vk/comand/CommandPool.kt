package dev.ktxvulkan.graphics.vk.comand

import dev.ktxvulkan.graphics.utils.vkCheckResult
import dev.ktxvulkan.graphics.vk.Device
import io.github.oshai.kotlinlogging.KLoggable
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkCommandBuffer
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo
import org.lwjgl.vulkan.VkCommandPoolCreateInfo
import org.lwjgl.vulkan.VkQueue

class CommandPool(val device: Device) : KLoggable {
    override val logger = logger()

    val commandPoolQueue: VkQueue
    val commandPool: Long
    val primaryBuffer: VkCommandBuffer

    init {
        MemoryStack.stackPush().use { stack ->
            val queueFamilyIndices = device.physicalDevice.queueFamilyIndices

            val poolInfo = VkCommandPoolCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                .flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)
                .queueFamilyIndex(queueFamilyIndices.graphicsFamily)

            val commandPoolQueueBuf = stack.callocPointer(1)
            vkGetDeviceQueue(device.vkDevice, poolInfo.queueFamilyIndex(), 0, commandPoolQueueBuf)
            commandPoolQueue = VkQueue(commandPoolQueueBuf[0], device.vkDevice)

            val commandPoolBuf = stack.callocLong(1)
            val vkCreateCommandPoolResult =
                vkCreateCommandPool(device.vkDevice, poolInfo, null, commandPoolBuf)
            vkCheckResult(vkCreateCommandPoolResult, "failed to create command pool")
            commandPool = commandPoolBuf[0]

            logger.info("successfully created command pool")

            primaryBuffer = allocateCommandBuffer(true)
        }
    }

    fun allocateCommandBuffer(primary: Boolean = false): VkCommandBuffer {
        MemoryStack.stackPush().use { stack ->
            val allocInfo = VkCommandBufferAllocateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                .commandPool(commandPool)
                .level(if (primary) VK_COMMAND_BUFFER_LEVEL_PRIMARY else VK_COMMAND_BUFFER_LEVEL_SECONDARY)
                .commandBufferCount(1)

            val p = stack.callocPointer(1).also {
                val result = vkAllocateCommandBuffers(device.vkDevice, allocInfo, it)
                vkCheckResult(result, "failed to allocate command buffer")
            }

            return VkCommandBuffer(p[0], device.vkDevice)
        }
    }

    fun destroy() {
        vkDestroyCommandPool(device.vkDevice, commandPool, null)
    }
}