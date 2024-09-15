package dev.ktxvulkan.graphics.utils

import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo
import org.lwjgl.vulkan.VkVertexInputAttributeDescription
import org.lwjgl.vulkan.VkVertexInputBindingDescription

class VertexFormat(val attributes: List<VertexAttribute>) {
    val elements: List<VertexElement>
    val size: Int

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
//        bindings: Int = 1,
        inputRate: Int = VK_VERTEX_INPUT_RATE_VERTEX
    ): VkVertexInputBindingDescription.Buffer {
        return VkVertexInputBindingDescription.calloc(1, this@MemoryStack).also {
            repeat(1) { binding ->
                it[binding].stride(size).inputRate(inputRate).binding(binding)
            }
        }

    }

    context(MemoryStack)
    fun getAttributeDescriptions(binding: Int = 0): VkVertexInputAttributeDescription.Buffer {
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

    override fun toString(): String {
        return "VertexFormat(attributes=$attributes, elements=$elements, size=$size)"
    }

    data class VertexElement(val location: Int, val attribute: VertexAttribute, val offset: Int)
}

enum class VertexAttribute(val format: Int, val size: Int, val dataType: DataType, val capacity: Int) {
    R32_SFLOAT(VK_FORMAT_R32_SFLOAT, 4, DataType.SFLOAT, 1),
    R32G32_SFLOAT(VK_FORMAT_R32G32_SFLOAT, 8, DataType.SFLOAT, 2),
    R32G32B32_SFLOAT(VK_FORMAT_R32G32B32_SFLOAT, 12, DataType.SFLOAT, 3),
    R32G32B32A32_SFLOAT(VK_FORMAT_R32G32B32A32_SFLOAT, 16, DataType.SFLOAT, 4),
    R32_SINT(VK_FORMAT_R32_SINT, 4, DataType.SINT, 1),
    R32G32_SINT(VK_FORMAT_R32G32_SINT, 8, DataType.SINT, 2),
    R32G32B32_SINT(VK_FORMAT_R32G32B32_SINT, 12, DataType.SINT, 3),
    R32G32B32A32_SINT(VK_FORMAT_R32G32B32A32_SINT, 16, DataType.SFLOAT, 4),
    R32_UINT(VK_FORMAT_R32_UINT, 4, DataType.UINT, 1),
    R32G32_UINT(VK_FORMAT_R32G32_UINT, 8, DataType.UINT, 2),
    R32G32B32_UINT(VK_FORMAT_R32G32B32_UINT, 12, DataType.UINT, 3),
    R32G32B32A32_UINT(VK_FORMAT_R32G32B32A32_UINT, 16, DataType.SFLOAT, 4)
}

enum class DataType {
    SFLOAT, SINT, UINT
}
