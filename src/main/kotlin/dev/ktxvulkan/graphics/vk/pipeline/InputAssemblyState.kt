package dev.ktxvulkan.graphics.vk.pipeline

import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO
import org.lwjgl.vulkan.VkPipelineInputAssemblyStateCreateInfo

data class InputAssemblyState(
    val vertexTopology: Int,
    val primitiveRestart: Boolean = false
) : PipelineState {
    override val dynamicStates = listOf<DynamicState>()
    context(MemoryStack)
    fun getVkPipelineInputAssemblyStateCreateInfo(): VkPipelineInputAssemblyStateCreateInfo {
        return VkPipelineInputAssemblyStateCreateInfo.calloc(this@MemoryStack)
            .sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO)
            .topology(vertexTopology)
            .primitiveRestartEnable(primitiveRestart)
    }
}