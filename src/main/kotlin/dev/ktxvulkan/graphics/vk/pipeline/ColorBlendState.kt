package dev.ktxvulkan.graphics.vk.pipeline

import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkPipelineColorBlendAttachmentState
import org.lwjgl.vulkan.VkPipelineColorBlendStateCreateInfo


enum class BlendFactor(val vkBlendFactor: Int) {
    BLEND_FACTOR_ZERO(0),
    BLEND_FACTOR_ONE(1),
    BLEND_FACTOR_SRC_COLOR(2),
    BLEND_FACTOR_ONE_MINUS_SRC_COLOR(3),
    BLEND_FACTOR_DST_COLOR(4),
    BLEND_FACTOR_ONE_MINUS_DST_COLOR(5),
    BLEND_FACTOR_SRC_ALPHA(6),
    BLEND_FACTOR_ONE_MINUS_SRC_ALPHA(7),
    BLEND_FACTOR_DST_ALPHA(8),
    BLEND_FACTOR_ONE_MINUS_DST_ALPHA(9),
    BLEND_FACTOR_CONSTANT_COLOR(10),
    BLEND_FACTOR_ONE_MINUS_CONSTANT_COLOR(11),
    BLEND_FACTOR_CONSTANT_ALPHA(12),
    BLEND_FACTOR_ONE_MINUS_CONSTANT_ALPHA(13),
    BLEND_FACTOR_SRC_ALPHA_SATURATE(14),
    BLEND_FACTOR_SRC1_COLOR(15),
    BLEND_FACTOR_ONE_MINUS_SRC1_COLOR(16),
    BLEND_FACTOR_SRC1_ALPHA(17),
    BLEND_FACTOR_ONE_MINUS_SRC1_ALPHA(18),
}

enum class BlendOp(val vkBlendOp: Int) {
    BLEND_OP_ADD(0),
    BLEND_OP_SUBTRACT(1),
    BLEND_OP_REVERSE_SUBTRACT(2),
    BLEND_OP_MIN(3),
    BLEND_OP_MAX(4),
    // region Provided by VK_EXT_blend_operation_advanced
    BLEND_OP_ZERO_EXT(1000148000),
    BLEND_OP_SRC_EXT(1000148001),
    BLEND_OP_DST_EXT(1000148002),
    BLEND_OP_SRC_OVER_EXT(1000148003),
    BLEND_OP_DST_OVER_EXT(1000148004),
    BLEND_OP_SRC_IN_EXT(1000148005),
    BLEND_OP_DST_IN_EXT(1000148006),
    BLEND_OP_SRC_OUT_EXT(1000148007),
    BLEND_OP_DST_OUT_EXT(1000148008),
    BLEND_OP_SRC_ATOP_EXT(1000148009),
    BLEND_OP_DST_ATOP_EXT(1000148010),
    BLEND_OP_XOR_EXT(1000148011),
    BLEND_OP_MULTIPLY_EXT(1000148012),
    BLEND_OP_SCREEN_EXT(1000148013),
    BLEND_OP_OVERLAY_EXT(1000148014),
    BLEND_OP_DARKEN_EXT(1000148015),
    BLEND_OP_LIGHTEN_EXT(1000148016),
    BLEND_OP_COLORDODGE_EXT(1000148017),
    BLEND_OP_COLORBURN_EXT(1000148018),
    BLEND_OP_HARDLIGHT_EXT(1000148019),
    BLEND_OP_SOFTLIGHT_EXT(1000148020),
    BLEND_OP_DIFFERENCE_EXT(1000148021),
    BLEND_OP_EXCLUSION_EXT(1000148022),
    BLEND_OP_INVERT_EXT(1000148023),
    BLEND_OP_INVERT_RGB_EXT(1000148024),
    BLEND_OP_LINEARDODGE_EXT(1000148025),
    BLEND_OP_LINEARBURN_EXT(1000148026),
    BLEND_OP_VIVIDLIGHT_EXT(1000148027),
    BLEND_OP_LINEARLIGHT_EXT(1000148028),
    BLEND_OP_PINLIGHT_EXT(1000148029),
    BLEND_OP_HARDMIX_EXT(1000148030),
    BLEND_OP_HSL_HUE_EXT(1000148031),
    BLEND_OP_HSL_SATURATION_EXT(1000148032),
    BLEND_OP_HSL_COLOR_EXT(1000148033),
    BLEND_OP_HSL_LUMINOSITY_EXT(1000148034),
    BLEND_OP_PLUS_EXT(1000148035),
    BLEND_OP_PLUS_CLAMPED_EXT(1000148036),
    BLEND_OP_PLUS_CLAMPED_ALPHA_EXT(1000148037),
    BLEND_OP_PLUS_DARKER_EXT(1000148038),
    BLEND_OP_MINUS_EXT(1000148039),
    BLEND_OP_MINUS_CLAMPED_EXT(1000148040),
    BLEND_OP_CONTRAST_EXT(1000148041),
    BLEND_OP_INVERT_OVG_EXT(1000148042),
    BLEND_OP_RED_EXT(1000148043),
    BLEND_OP_GREEN_EXT(1000148044),
    BLEND_OP_BLUE_EXT(1000148045)
    //endregion
}

data class ColorBlendAttachmentState(
    val blendEnable: Boolean = false,
    val srcColorBlendFactor: BlendFactor = BlendFactor.BLEND_FACTOR_SRC_ALPHA,
    val dstColorBlendFactor: BlendFactor = BlendFactor.BLEND_FACTOR_ONE_MINUS_SRC_ALPHA,
    val colorBlendOp: BlendOp = BlendOp.BLEND_OP_ADD,
    val srcAlphaBlendFactor: BlendFactor = BlendFactor.BLEND_FACTOR_ONE,
    val dstAlphaBlendFactor: BlendFactor = BlendFactor.BLEND_FACTOR_ZERO,
    val alphaBlendOp: BlendOp = BlendOp.BLEND_OP_ADD,
    val colorWriteMask: Int = VK_COLOR_COMPONENT_R_BIT or VK_COLOR_COMPONENT_G_BIT or VK_COLOR_COMPONENT_B_BIT or VK_COLOR_COMPONENT_A_BIT
) : PipelineState {
    override val dynamic = false
    override val dynamicStates = listOf<DynamicState>()

    context(VkPipelineColorBlendAttachmentState)
    fun apply() {
        blendEnable(blendEnable)
        srcColorBlendFactor(srcColorBlendFactor.vkBlendFactor)
        dstColorBlendFactor(dstColorBlendFactor.vkBlendFactor)
        colorBlendOp(colorBlendOp.vkBlendOp)
        srcAlphaBlendFactor(srcColorBlendFactor.vkBlendFactor)
        dstAlphaBlendFactor(dstColorBlendFactor.vkBlendFactor)
        alphaBlendOp(colorBlendOp.vkBlendOp)
        colorWriteMask(colorWriteMask)
    }
}

enum class LogicOp(val vkLogicOp: Int) {
    LOGIC_OP_CLEAR(0),
    LOGIC_OP_AND(1),
    LOGIC_OP_AND_REVERSE(2),
    LOGIC_OP_COPY(3),
    LOGIC_OP_AND_INVERTED(4),
    LOGIC_OP_NO_OP(5),
    LOGIC_OP_XOR(6),
    LOGIC_OP_OR(7),
    LOGIC_OP_NOR(8),
    LOGIC_OP_EQUIVALENT(9),
    LOGIC_OP_INVERT(10),
    LOGIC_OP_OR_REVERSE(11),
    LOGIC_OP_COPY_INVERTED(12),
    LOGIC_OP_OR_INVERTED(13),
    LOGIC_OP_NAND(14),
    LOGIC_OP_SET(15),
}

data class ColorBlendState(
    val flags: VkPipelineColorBlendStateCreateFlagBits = VkPipelineColorBlendStateCreateFlagBits.NONE,
    val logicOpEnable: Boolean = false,
    val logicOp: LogicOp = LogicOp.LOGIC_OP_COPY,
    val colorAttachments: List<ColorBlendAttachmentState>,
    val blendConstants: List<Float> = listOf(0f, 0f, 0f, 0f)
) : PipelineState {
    override val dynamic = false
    override val dynamicStates = listOf<DynamicState>()

    context(MemoryStack)
    fun getVkPipelineColorBlendStateCreateInfo(): VkPipelineColorBlendStateCreateInfo {
        val blendConstantsBuf = callocFloat(4)
        blendConstants.forEachIndexed { index, fl -> blendConstantsBuf.put(index, fl) }
        val colorAttachmentsBuf = VkPipelineColorBlendAttachmentState.calloc(colorAttachments.size, this@MemoryStack)
        colorAttachments.forEachIndexed { index, colorBlendAttachmentState ->
            with(colorAttachmentsBuf[index]) { colorBlendAttachmentState.apply() }
        }

        return VkPipelineColorBlendStateCreateInfo.calloc(this@MemoryStack)
            .sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO)
            .flags(flags.bits)
            .logicOpEnable(logicOpEnable)
            .logicOp(logicOp.vkLogicOp)
            .blendConstants(blendConstantsBuf)
            .pAttachments(colorAttachmentsBuf)
    }
}

@JvmInline
value class VkPipelineColorBlendStateCreateFlagBits private constructor(val bits: Int) {
    infix fun or(other: VkPipelineColorBlendStateCreateFlagBits) =
        VkPipelineColorBlendStateCreateFlagBits(this.bits or other.bits)

    infix fun and(other: VkPipelineColorBlendStateCreateFlagBits) =
        VkPipelineColorBlendStateCreateFlagBits(this.bits and other.bits)

    operator fun contains(other: VkPipelineColorBlendStateCreateFlagBits) =
        (other and this).bits != 0

    companion object {
        val NONE = VkPipelineColorBlendStateCreateFlagBits(0)
        val VK_PIPELINE_COLOR_BLEND_STATE_CREATE_RASTERIZATION_ORDER_ATTACHMENT_ACCESS_BIT_EXT =
            VkPipelineColorBlendStateCreateFlagBits(0x00000001)
        val VK_PIPELINE_COLOR_BLEND_STATE_CREATE_RASTERIZATION_ORDER_ATTACHMENT_ACCESS_BIT_ARM =
            VK_PIPELINE_COLOR_BLEND_STATE_CREATE_RASTERIZATION_ORDER_ATTACHMENT_ACCESS_BIT_EXT
    }
}
