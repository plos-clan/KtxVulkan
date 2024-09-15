package dev.ktxvulkan.graphics.vk

import org.lwjgl.vulkan.VK10.VK_CULL_MODE_NONE
import org.lwjgl.vulkan.VK10.VK_POLYGON_MODE_FILL

data class RasterizerState(
    val depthClampEnable: Boolean = false,
    val rasterizerDiscardEnable: Boolean = false,
    val polygonMode: Int = VK_POLYGON_MODE_FILL,
    val lineWidth: Float = 1.0f,
    val cullMode: CullMode
) {
}

enum class CullMode(val cullMode: Int, val frontFace: Int) {
    NONE(VK_CULL_MODE_NONE, -1),

}