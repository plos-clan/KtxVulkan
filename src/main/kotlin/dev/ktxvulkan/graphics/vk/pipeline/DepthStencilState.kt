package dev.ktxvulkan.graphics.vk.pipeline

import dev.ktxvulkan.graphics.vk.CompareOp
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO
import org.lwjgl.vulkan.VkPipelineDepthStencilStateCreateInfo
import org.lwjgl.vulkan.VkStencilOpState

enum class StencilOp(val vkStencilOp: Int) {
    STENCIL_OP_KEEP(0),
    STENCIL_OP_ZERO(1),
    STENCIL_OP_REPLACE(2),
    STENCIL_OP_INCREMENT_AND_CLAMP(3),
    STENCIL_OP_DECREMENT_AND_CLAMP(4),
    STENCIL_OP_INVERT(5),
    STENCIL_OP_INCREMENT_AND_WRAP(6),
    STENCIL_OP_DECREMENT_AND_WRAP(7)
}

data class StencilOpState(
    val failOp: StencilOp = StencilOp.STENCIL_OP_KEEP,
    val passOp: StencilOp = StencilOp.STENCIL_OP_KEEP,
    val depthFailOp: StencilOp = StencilOp.STENCIL_OP_KEEP,
    val compareOp: StencilOp = StencilOp.STENCIL_OP_KEEP,
    val compareMask: Int = 0x7ffffff,
    val writeMask: Int = 0x7ffffff,
    val reference: Int = 0
) : PipelineState {
    override val dynamicStates = listOf<DynamicState>()

    context(MemoryStack)
    fun getVkStencilOpState(): VkStencilOpState {
        return VkStencilOpState.calloc(this@MemoryStack)
            .failOp(failOp.vkStencilOp)
            .passOp(passOp.vkStencilOp)
            .depthFailOp(depthFailOp.vkStencilOp)
            .compareOp(compareOp.vkStencilOp)
            .compareMask(compareMask)
            .writeMask(writeMask)
            .reference(reference)
    }
}

data class DepthStencilState(
    val flags: VkPipelineDepthStencilStateCreateFlagBits = VkPipelineDepthStencilStateCreateFlagBits.NONE,
    val depthTestEnable: Boolean = true,
    val depthWriteEnable: Boolean = true,
    val depthCompareOp: CompareOp = CompareOp.COMPARE_OP_LESS,
    val depthBoundsTestEnable: Boolean = false,
    val stencilTestEnable: Boolean = false,
    val front: StencilOpState? = null,
    val back: StencilOpState? = null,
    val minDepthBounds: Float = 0f,
    val maxDepthBounds: Float = 0f
) : PipelineState {
    override val dynamicStates = listOf<DynamicState>()

    context(MemoryStack)
    fun getVkPipelineDepthStencilStateCreateInfo(): VkPipelineDepthStencilStateCreateInfo {
        val info = VkPipelineDepthStencilStateCreateInfo.calloc(this@MemoryStack)
        info.sType(VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO)
            .flags(flags.bits)
            .depthTestEnable(depthTestEnable)
            .depthWriteEnable(depthWriteEnable)
            .depthCompareOp(depthCompareOp.vkCompareOp)
            .depthBoundsTestEnable(depthBoundsTestEnable)
            .stencilTestEnable(stencilTestEnable)
            .minDepthBounds(minDepthBounds)
            .maxDepthBounds(maxDepthBounds)
        if (front != null) info.front(front.getVkStencilOpState())
        if (back != null) info.back(back.getVkStencilOpState())
        return info
    }
}

@JvmInline
value class VkPipelineDepthStencilStateCreateFlagBits private constructor(val bits: Int) {
    infix fun or(other: VkPipelineDepthStencilStateCreateFlagBits) =
        VkPipelineDepthStencilStateCreateFlagBits(this.bits or other.bits)

    infix fun and(other: VkPipelineDepthStencilStateCreateFlagBits) =
        VkPipelineDepthStencilStateCreateFlagBits(this.bits and other.bits)

    operator fun contains(other: VkPipelineDepthStencilStateCreateFlagBits) =
        (other and this).bits != 0

    companion object {
        val NONE = VkPipelineDepthStencilStateCreateFlagBits(0)
        val VK_PIPELINE_DEPTH_STENCIL_STATE_CREATE_RASTERIZATION_ORDER_ATTACHMENT_DEPTH_ACCESS_BIT_EXT =
            VkPipelineDepthStencilStateCreateFlagBits(0x00000001)
        val VK_PIPELINE_DEPTH_STENCIL_STATE_CREATE_RASTERIZATION_ORDER_ATTACHMENT_STENCIL_ACCESS_BIT_EXT =
            VkPipelineDepthStencilStateCreateFlagBits(0x00000002)
        val VK_PIPELINE_DEPTH_STENCIL_STATE_CREATE_RASTERIZATION_ORDER_ATTACHMENT_DEPTH_ACCESS_BIT_ARM =
            VK_PIPELINE_DEPTH_STENCIL_STATE_CREATE_RASTERIZATION_ORDER_ATTACHMENT_DEPTH_ACCESS_BIT_EXT
        val VK_PIPELINE_DEPTH_STENCIL_STATE_CREATE_RASTERIZATION_ORDER_ATTACHMENT_STENCIL_ACCESS_BIT_ARM =
            VK_PIPELINE_DEPTH_STENCIL_STATE_CREATE_RASTERIZATION_ORDER_ATTACHMENT_STENCIL_ACCESS_BIT_EXT
    }
}