package dev.ktxvulkan.graphics.vk.pipeline

import dev.ktxvulkan.graphics.utils.VertexFormat
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo

data class VertexInputState(val vertexFormat: VertexFormat) : PipelineState {
    override val dynamicStates = listOf<DynamicState>()

    context(MemoryStack)
    fun getVkPipelineVertexInputStateCreateInfo(): VkPipelineVertexInputStateCreateInfo {
        return VkPipelineVertexInputStateCreateInfo.calloc(this@MemoryStack)
            .sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO)
            .pVertexBindingDescriptions(vertexFormat.getBindingDescription())
            .pVertexAttributeDescriptions(vertexFormat.getAttributeDescriptions())
    }
}
