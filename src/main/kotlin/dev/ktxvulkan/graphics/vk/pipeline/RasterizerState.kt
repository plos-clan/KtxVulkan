package dev.ktxvulkan.graphics.vk.pipeline

import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkPipelineRasterizationStateCreateInfo

data class RasterizerState(
    val depthClampEnable: Boolean = false,
    val rasterizerDiscardEnable: Boolean = false,
    val polygonMode: Int = VK_POLYGON_MODE_FILL,
    val lineWidth: Float = 1.0f,
    val cullMode: CullMode = CullMode.None,
    val depthBiasEnable: Boolean = false
) : PipelineState {
    override val dynamicStates = listOf<DynamicState>()

    context(MemoryStack)
    fun getVkPipelineRasterizationStateCreateInfo(): VkPipelineRasterizationStateCreateInfo {
        return VkPipelineRasterizationStateCreateInfo.calloc(this@MemoryStack)
            .sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO)
            .depthClampEnable(depthClampEnable)
            .polygonMode(polygonMode)
            .lineWidth(lineWidth)
            .depthBiasEnable(depthBiasEnable)
            .apply {
                if (cullMode == CullMode.None) cullMode(CullMode.None.cullMode)
                else cullMode(cullMode.cullMode).frontFace(cullMode.frontFace)
            }
    }
}

sealed class CullMode(val cullMode: Int, val frontFace: Int) {
    data object None : CullMode(VK_CULL_MODE_NONE, -1)

    sealed class Front(frontFace: Int) : CullMode(VK_CULL_MODE_FRONT_BIT, frontFace) {
        data object ClockWise : Front(VK_FRONT_FACE_CLOCKWISE)
        data object CounterClockWise : Front(VK_FRONT_FACE_COUNTER_CLOCKWISE)
    }

    sealed class Back(frontFace: Int) : CullMode(VK_CULL_MODE_BACK_BIT, frontFace) {
        data object ClockWise : Front(VK_FRONT_FACE_CLOCKWISE)
        data object CounterClockWise : Front(VK_FRONT_FACE_COUNTER_CLOCKWISE)
    }

    sealed class FrontBack(frontFace: Int) : CullMode(VK_CULL_MODE_FRONT_AND_BACK, frontFace) {
        data object ClockWise : Front(VK_FRONT_FACE_CLOCKWISE)
        data object CounterClockWise : Front(VK_FRONT_FACE_COUNTER_CLOCKWISE)
    }

    companion object {
        operator fun invoke(cullMode: Int, frontFace: Int): CullMode = when (cullMode) {
            VK_CULL_MODE_FRONT_BIT -> when (frontFace) {
                VK_FRONT_FACE_CLOCKWISE -> Front.ClockWise
                VK_FRONT_FACE_COUNTER_CLOCKWISE -> Front.CounterClockWise
                else -> throw IllegalArgumentException("Illegal frontFace: $frontFace")
            }
            VK_CULL_MODE_BACK_BIT -> when (frontFace) {
                VK_FRONT_FACE_CLOCKWISE -> Back.ClockWise
                VK_FRONT_FACE_COUNTER_CLOCKWISE -> Back.CounterClockWise
                else -> throw IllegalArgumentException("Illegal frontFace: $frontFace")
            }
            VK_CULL_MODE_FRONT_AND_BACK -> when (frontFace) {
                VK_FRONT_FACE_CLOCKWISE -> FrontBack.ClockWise
                VK_FRONT_FACE_COUNTER_CLOCKWISE -> FrontBack.CounterClockWise
                else -> throw IllegalArgumentException("Illegal frontFace: $frontFace")
            }
            else -> throw IllegalArgumentException("Illegal cullMode: $cullMode")
        }
    }
}