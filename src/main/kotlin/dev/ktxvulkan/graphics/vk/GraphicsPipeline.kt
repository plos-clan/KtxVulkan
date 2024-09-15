package dev.ktxvulkan.graphics.vk

import dev.ktxvulkan.graphics.utils.VertexFormat

class GraphicsPipeline(
    val shaderStages: List<ShaderStage>,
    val vertexFormat: VertexFormat,
    val rasterizerState: RasterizerState
) {

    data class ShaderStage(val stage: Int, val shaderModule: ShaderModule, val pName: String)
}