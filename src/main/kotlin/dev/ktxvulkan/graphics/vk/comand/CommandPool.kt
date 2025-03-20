package dev.ktxvulkan.graphics.vk.comand

import dev.ktxvulkan.graphics.utils.vkCheckResult
import dev.ktxvulkan.graphics.vk.Device
import io.github.oshai.kotlinlogging.KLoggable
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import java.util.*

class CommandPool(val device: Device) : KLoggable {
    override val logger = logger()

    val commandPool: Long

    private val commandBuffers: MutableList<CommandBuffer> = ObjectArrayList()
    private val availableCmdBuffers: Queue<CommandBuffer> = ArrayDeque<CommandBuffer>()

    init {
        MemoryStack.stackPush().use { stack ->
            val queueFamilyIndices = device.physicalDevice.queueFamilyIndices

            val poolInfo = VkCommandPoolCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                .flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)
                .queueFamilyIndex(queueFamilyIndices.graphicsFamily)

            val commandPoolBuf = stack.callocLong(1)
            val vkCreateCommandPoolResult =
                vkCreateCommandPool(device.vkDevice, poolInfo, null, commandPoolBuf)
            vkCheckResult(vkCreateCommandPoolResult, "failed to create command pool")
            commandPool = commandPoolBuf[0]

            logger.info("successfully created command pool")
        }
    }

    fun getCommandBuffer(stack: MemoryStack): CommandBuffer {
        if (availableCmdBuffers.isEmpty()) {
            allocateCommandBuffers(stack)
        }

        val commandBuffer = availableCmdBuffers.poll()
        return commandBuffer
    }

    private fun allocateCommandBuffers(stack: MemoryStack) {
        val size = 10

        val allocInfo = VkCommandBufferAllocateInfo.calloc(stack)
        allocInfo.`sType$Default`()
        allocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
        allocInfo.commandPool(commandPool)
        allocInfo.commandBufferCount(size)

        val pCommandBuffer = stack.mallocPointer(size)
        vkAllocateCommandBuffers(device.vkDevice, allocInfo, pCommandBuffer)

        val fenceInfo = VkFenceCreateInfo.calloc(stack)
        fenceInfo.`sType$Default`()
        fenceInfo.flags(VK_FENCE_CREATE_SIGNALED_BIT)

        val semaphoreCreateInfo = VkSemaphoreCreateInfo.calloc(stack)
        semaphoreCreateInfo.`sType$Default`()

        for (i in 0..<size) {
            val pFence = stack.mallocLong(1)
            vkCreateFence(device.vkDevice, fenceInfo, null, pFence)

            val pSemaphore = stack.mallocLong(1)
            vkCreateSemaphore(device.vkDevice, semaphoreCreateInfo, null, pSemaphore)

            val vkCommandBuffer = VkCommandBuffer(pCommandBuffer.get(i), device.vkDevice)
            val commandBuffer = CommandBuffer(device, this, vkCommandBuffer, pFence.get(0), pSemaphore.get(0))
            commandBuffers.add(commandBuffer)
            availableCmdBuffers.add(commandBuffer)
        }
    }

    fun addToAvailable(commandBuffer: CommandBuffer?) {
        this.availableCmdBuffers.add(commandBuffer)
    }

    fun destroy() {
        for (commandBuffer in commandBuffers) {
            vkDestroyFence(device.vkDevice, commandBuffer.fence, null)
            vkDestroySemaphore(device.vkDevice, commandBuffer.semaphore, null)
        }
        vkResetCommandPool(device.vkDevice, commandPool, VK_COMMAND_POOL_RESET_RELEASE_RESOURCES_BIT)
        vkDestroyCommandPool(device.vkDevice, commandPool, null)
    }

    class CommandBuffer(
        val device: Device,
        val commandPool: CommandPool,
        val handle: VkCommandBuffer,
        val fence: Long,
        val semaphore: Long
    ) {
        var isSubmitted: Boolean = false
        var isRecording: Boolean = false

        fun begin(stack: MemoryStack) {
            val beginInfo = VkCommandBufferBeginInfo.calloc(stack)
            beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
            beginInfo.flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT)

            vkBeginCommandBuffer(this.handle, beginInfo)

            this.isRecording = true
        }

        fun submitCommands(stack: MemoryStack, queue: VkQueue, useSemaphore: Boolean, infoBuilder: VkSubmitInfo.() -> Unit = {}): Long {
            val fence = this.fence

            vkCheckResult(vkEndCommandBuffer(handle), "failed to record command buffer!")

            vkResetFences(device.vkDevice, this.fence)

            val submitInfo = VkSubmitInfo.calloc(stack)
            submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                .pCommandBuffers(stack.pointers(this.handle))

            if (useSemaphore) {
                submitInfo.pSignalSemaphores(stack.longs(this.semaphore))
            }

            submitInfo.infoBuilder()

//            vkQueueSubmit(queue, submitInfo, fence)
            vkCheckResult(vkQueueSubmit(queue, submitInfo, fence), "failed to submit queue")

            this.isRecording = false
            this.isSubmitted = true
            return fence
        }

        fun reset() {
            this.isSubmitted = false
            this.isRecording = false
            this.commandPool.addToAvailable(this)
        }
    }
}