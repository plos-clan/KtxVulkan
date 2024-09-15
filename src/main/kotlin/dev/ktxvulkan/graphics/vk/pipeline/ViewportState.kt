package dev.ktxvulkan.graphics.vk.pipeline

import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO
import org.lwjgl.vulkan.VkPipelineViewportStateCreateInfo
import org.lwjgl.vulkan.VkRect2D
import org.lwjgl.vulkan.VkViewport

data class ViewportState(
    val viewportCount: Int,
    val scissorCount: Int,
    val viewports: List<VkViewport>?,
    val scissors: List<VkRect2D>?
) : PipelineState {
    override val dynamicStates = listOf(DynamicState.DYNAMIC_STATE_VIEWPORT)

    context(MemoryStack)
    fun getVkPipelineViewportStateCreateInfo(): VkPipelineViewportStateCreateInfo {
        val info = VkPipelineViewportStateCreateInfo.calloc(this@MemoryStack)
            .sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO)
            .viewportCount(viewportCount)
            .scissorCount(scissorCount)

        if (viewports != null) {
            val viewportsBuf = VkViewport.calloc(viewports.size, this@MemoryStack)
            viewports.forEachIndexed { index, vkViewport ->
                viewportsBuf.put(index, vkViewport)
            }
            info.pViewports(viewportsBuf)
        }

        if (scissors != null) {
            val scissorsBuf = VkRect2D.calloc(scissors.size, this@MemoryStack)
            scissors.forEachIndexed { index, vkRect2D ->
                scissorsBuf.put(index, vkRect2D)
            }
            info.pScissors(scissorsBuf)
        }

        return info
    }
}
