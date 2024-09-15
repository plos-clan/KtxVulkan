package dev.ktxvulkan.graphics.vk.buffer

import dev.ktxvulkan.RenderEngine.commandBuffer
import dev.ktxvulkan.graphics.utils.vkCheckResult
import dev.ktxvulkan.graphics.vk.pipeline.GraphicsPipeline
import dev.ktxvulkan.structs.Vertex
import dev.ktxvulkan.structs.sizeof
import dev.luna5ama.kmogus.Arr
import dev.luna5ama.kmogus.MutableArr
import dev.luna5ama.kmogus.asMutable
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkBufferCreateInfo
import org.lwjgl.vulkan.VkCommandBuffer
import org.lwjgl.vulkan.VkMemoryAllocateInfo
import org.lwjgl.vulkan.VkMemoryRequirements

class PMVertexBuffer(val pipeline: GraphicsPipeline, val bufferSizeMb: Int = 64) {
    private val bufferInfo: VkBufferCreateInfo
    lateinit var arr: MutableArr
    private val vertexBuffer: Long
    private val vertexBufferMemory: Long
    private var vertices = 0

    init {
        MemoryStack.stackPush().use { stack ->
            bufferInfo = VkBufferCreateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                .size(bufferSizeMb * 1024L * 1024L)
                .usage(VK_BUFFER_USAGE_VERTEX_BUFFER_BIT)
                .sharingMode(VK_SHARING_MODE_EXCLUSIVE)

            val longBuffer = stack.callocLong(1)

            val vkCreateBufferResult =
                vkCreateBuffer(pipeline.device.vkDevice, bufferInfo, null, longBuffer)
            vkCheckResult(vkCreateBufferResult, "failed to create vertex buffer")
            vertexBuffer = longBuffer[0]

            val memRequirements = VkMemoryRequirements.calloc(stack)
            vkGetBufferMemoryRequirements(pipeline.device.vkDevice, vertexBuffer, memRequirements)

            val allocInfo = VkMemoryAllocateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                .allocationSize(memRequirements.size())
                .memoryTypeIndex(findMemoryType(pipeline.device, memRequirements.memoryTypeBits(),
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK_MEMORY_PROPERTY_HOST_COHERENT_BIT))
            val vkAllocateMemoryResult =
                vkAllocateMemory(pipeline.device.vkDevice, allocInfo, null, longBuffer)
            vkCheckResult(vkAllocateMemoryResult, "failed to allocate device memory for vertex buffer")
            vertexBufferMemory = longBuffer[0]

            vkBindBufferMemory(pipeline.device.vkDevice, vertexBuffer, vertexBufferMemory, 0)

            map()
//            val pointerBuffer = stack.callocPointer(1)
//            vkMapMemory(pipeline.device.vkDevice, vertexBufferMemory, 0, bufferInfo.size(), 0, pointerBuffer)
//
//            arr = Arr.wrap(pointerBuffer.getByteBuffer(0, bufferInfo.size().toInt())).asMutable()
        }
    }

    fun vertex(x: Float, y: Float, z: Float, r: Float, g: Float, b: Float, a: Float) {
        val vertex = Vertex(arr)
        vertex.pos.apply {
            this.x = x
            this.y = y
            this.z = z
        }
        vertex.color.apply {
            this.x = r
            this.y = g
            this.z = b
            this.w = a
        }
        arr += sizeof(Vertex)
        vertices++
    }

    fun draw(cmd: VkCommandBuffer) {
        val vertexBuffers = longArrayOf(vertexBuffer)
        val offsets = longArrayOf(0)
        vkCmdBindVertexBuffers(cmd, 0, vertexBuffers, offsets)
        require(vertices % 3 == 0)
        vkCmdDraw(cmd, vertices, 1, 0, 0)
        vertices = 0
        arr.pos = 0
    }

    fun map() {
        MemoryStack.stackPush().use { stack ->
            val pointerBuffer = stack.callocPointer(1)
            vkMapMemory(pipeline.device.vkDevice, vertexBufferMemory, 0, bufferInfo.size(), 0, pointerBuffer)

            arr = Arr.wrap(pointerBuffer.getByteBuffer(0, bufferInfo.size().toInt())).asMutable()
        }
    }

    fun unmap() {
        vkUnmapMemory(pipeline.device.vkDevice, vertexBufferMemory)
    }

    fun destroy() {
        vkDestroyBuffer(pipeline.device.vkDevice, vertexBuffer, null)
        vkFreeMemory(pipeline.device.vkDevice, vertexBufferMemory, null)
    }
}