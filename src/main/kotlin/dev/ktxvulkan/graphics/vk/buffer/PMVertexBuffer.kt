package dev.ktxvulkan.graphics.vk.buffer

import dev.ktxvulkan.graphics.vk.Device
import dev.ktxvulkan.structs.Vertex
import dev.ktxvulkan.structs.sizeof
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VK13.vkCmdSetPrimitiveTopology
import org.lwjgl.vulkan.VkCommandBuffer

class PMVertexBuffer(
    device: Device, bufferSizeMb: Int = 64
) : Buffer(
    device,
    VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
    VK_SHARING_MODE_EXCLUSIVE,
    bufferSizeMb * 1024L * 1024L,
    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK_MEMORY_PROPERTY_HOST_COHERENT_BIT
) {
    private var vertices = 0


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

    fun draw(cmd: VkCommandBuffer, topology: Int) {
        val vertexBuffers = longArrayOf(bufferHandle)
        val offsets = longArrayOf(0)
        vkCmdBindVertexBuffers(cmd, 0, vertexBuffers, offsets)
        vkCmdSetPrimitiveTopology(cmd, topology)
        vkCmdDraw(cmd, vertices, 1, 0, 0)
        vertices = 0
        arr.pos = 0
    }
}