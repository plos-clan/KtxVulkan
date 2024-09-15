package dev.ktxvulkan.graphics.utils

import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkVertexInputAttributeDescription
import org.lwjgl.vulkan.VkVertexInputBindingDescription

class VertexFormat(val attributes: List<VertexAttribute>) {
    val size: Int
    val elements: List<VertexElement>

    init {
        var currentOffset = 0
        elements = buildList {
            attributes.forEachIndexed { index, vertexAttribute ->
                add(VertexElement(index, vertexAttribute, currentOffset))
                currentOffset += vertexAttribute.size
            }
        }
        size = currentOffset
    }

    context(MemoryStack)
    fun getBindingDescription(
        binding: Int = 0,
        inputRate: Int = VK_VERTEX_INPUT_RATE_VERTEX
    ): VkVertexInputBindingDescription {
        return VkVertexInputBindingDescription.calloc(this@MemoryStack)
            .stride(size)
            .inputRate(inputRate)
            .binding(binding)
    }

    context(MemoryStack)
    fun getAttributeDescriptions(binding: Int): VkVertexInputAttributeDescription.Buffer {
        return VkVertexInputAttributeDescription.calloc(elements.size, this@MemoryStack)
            .also {
                elements.forEachIndexed { index, vertexElement ->
                    it[index].binding(binding)
                        .location(vertexElement.location)
                        .format(vertexElement.attribute.format)
                        .offset(vertexElement.offset)
                }
            }
    }

    data class VertexElement(val location: Int, val attribute: VertexAttribute, val offset: Int)
}

enum class VertexAttribute(val format: Int, val size: Int, val dataType: DataType, val capacity: Int) {
    R32_SFLOAT(VK_FORMAT_R32_SFLOAT, 4, DataType.SFLOAT, 1),
    R32G32_SFLOAT(VK_FORMAT_R32G32_SFLOAT, 8, DataType.SFLOAT, 2),
    R32G32B32_SFLOAT(VK_FORMAT_R32G32B32_SFLOAT, 12, DataType.SFLOAT, 3),
    R32_SINT(VK_FORMAT_R32_SINT, 4, DataType.SINT, 1),
    R32G32_SINT(VK_FORMAT_R32G32_SINT, 8, DataType.SINT, 2),
    R32G32B32_SINT(VK_FORMAT_R32G32B32_SINT, 12, DataType.SINT, 3),
    R32_UINT(VK_FORMAT_R32_UINT, 4, DataType.UINT, 1),
    R32G32_UINT(VK_FORMAT_R32G32_UINT, 8, DataType.UINT, 2),
    R32G32B32_UINT(VK_FORMAT_R32G32B32_UINT, 12, DataType.UINT, 3)
}

enum class DataType {
    SFLOAT, SINT, UINT
}
