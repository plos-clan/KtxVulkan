package dev.ktxvulkan.graphics.vk.pipeline

import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO
import org.lwjgl.vulkan.VkPipelineDynamicStateCreateInfo

interface PipelineState {
    val dynamic: Boolean get() = false
    val dynamicStates: List<DynamicState>
}

data class PipelineStates(
    val vertexInputState: VertexInputState,
    val inputAssemblyState: InputAssemblyState,
    val viewPortState: ViewportState,
    val rasterizerState: RasterizerState,
    val multisamplingState: MultisamplingState,
    val depthStencilState: DepthStencilState,
    val colorBlendState: ColorBlendState,
    val additionalDynamicStates: Set<DynamicState>
) {
    private val values: List<PipelineState> get() = listOf(
        vertexInputState,
        inputAssemblyState,
        viewPortState,
        rasterizerState,
        multisamplingState,
        depthStencilState,
        colorBlendState
    )

    fun getDynamicStates(): Set<DynamicState> {
        return buildSet {
            values.forEach { pipelineState ->
                if (pipelineState.dynamic) addAll(pipelineState.dynamicStates)
            }
            addAll(additionalDynamicStates)
        }
    }

    context(MemoryStack)
    fun getVkPipelineDynamicStateCreateInfo(): VkPipelineDynamicStateCreateInfo {
        val dynamicStates = getDynamicStates().map { it.vkDynamicState }
        val dynamicStatesBuf = callocInt(dynamicStates.size)
        dynamicStates.forEachIndexed { index, i -> dynamicStatesBuf.put(index, i) }

        return VkPipelineDynamicStateCreateInfo.calloc()
            .sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO)
            .pDynamicStates(dynamicStatesBuf)
    }
}
