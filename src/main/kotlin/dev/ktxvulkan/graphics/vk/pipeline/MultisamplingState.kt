package dev.ktxvulkan.graphics.vk.pipeline

import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO
import org.lwjgl.vulkan.VkPipelineMultisampleStateCreateInfo

data class MultisamplingState(
    val rasterizationSamples: Int,
    val sampleShadingEnable: Boolean = true,
    val minSampleShading: Float = 0.2f,
    val sampleMasks: List<Int>? = null,
    val alphaToCoverageEnable: Boolean = false,
    val alphaToOneEnable: Boolean = false
) : PipelineState {
    override val dynamicStates = listOf<DynamicState>()

    context(MemoryStack)
    fun getVkPipelineMultisampleStateCreateInfo(): VkPipelineMultisampleStateCreateInfo {
        val info = VkPipelineMultisampleStateCreateInfo.calloc(this@MemoryStack)
        info.sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO)
            .rasterizationSamples(rasterizationSamples)
            .sampleShadingEnable(sampleShadingEnable)
            .minSampleShading(minSampleShading)
            .alphaToCoverageEnable(alphaToCoverageEnable)
            .alphaToOneEnable(alphaToOneEnable)
        if (!sampleMasks.isNullOrEmpty()) {
            val pSampleMask = callocInt(sampleMasks.size)
            sampleMasks.forEachIndexed { index, mask -> pSampleMask.put(index, mask) }
            info.pSampleMask(pSampleMask)
        }
        return info
    }
}