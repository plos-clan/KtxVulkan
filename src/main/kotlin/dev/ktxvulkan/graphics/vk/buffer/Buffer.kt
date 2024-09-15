package dev.ktxvulkan.graphics.vk.buffer

import dev.ktxvulkan.graphics.utils.vkCheckResult
import dev.ktxvulkan.graphics.vk.Device
import dev.luna5ama.kmogus.Arr
import dev.luna5ama.kmogus.MutableArr
import dev.luna5ama.kmogus.asMutable
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkBufferCreateInfo
import org.lwjgl.vulkan.VkMemoryAllocateInfo
import org.lwjgl.vulkan.VkMemoryRequirements

open class Buffer(val device: Device, val usage: Int, val sharingMode: Int, val bufferSize: Long, val properties: Int) {
    protected val arr: MutableArr
    protected val bufferHandle: Long
    protected val bufferMemoryHandle: Long

    init {
        MemoryStack.stackPush().use { stack ->
            val bufferInfo = VkBufferCreateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                .size(bufferSize)
                .usage(usage)
                .sharingMode(sharingMode)

            val longBuffer = stack.callocLong(1)

            val vkCreateBufferResult =
                vkCreateBuffer(device.vkDevice, bufferInfo, null, longBuffer)
            vkCheckResult(vkCreateBufferResult, "failed to create vertex buffer")
            bufferHandle = longBuffer[0]

            val memRequirements = VkMemoryRequirements.calloc(stack)
            vkGetBufferMemoryRequirements(device.vkDevice, bufferHandle, memRequirements)

            val allocInfo = VkMemoryAllocateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                .allocationSize(memRequirements.size())
                .memoryTypeIndex(findMemoryType(device, memRequirements.memoryTypeBits(), properties))
            val vkAllocateMemoryResult =
                vkAllocateMemory(device.vkDevice, allocInfo, null, longBuffer)
            vkCheckResult(vkAllocateMemoryResult, "failed to allocate device memory for vertex buffer")
            bufferMemoryHandle = longBuffer[0]

            vkBindBufferMemory(device.vkDevice, bufferHandle, bufferMemoryHandle, 0)

            val pointerBuffer = stack.callocPointer(1)
            vkMapMemory(device.vkDevice, bufferMemoryHandle, 0, bufferInfo.size(), 0, pointerBuffer)

            arr = Arr.wrap(pointerBuffer.getByteBuffer(0, bufferInfo.size().toInt())).asMutable()
        }
    }

    fun destroy() {
        vkDestroyBuffer(device.vkDevice, bufferHandle, null)
        vkFreeMemory(device.vkDevice, bufferMemoryHandle, null)
    }
}