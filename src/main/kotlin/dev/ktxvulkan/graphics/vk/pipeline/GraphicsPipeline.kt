package dev.ktxvulkan.graphics.vk.pipeline

import dev.ktxvulkan.graphics.utils.vkCheckResult
import dev.ktxvulkan.graphics.vk.DescriptorSetLayout
import dev.ktxvulkan.graphics.vk.Device
import dev.ktxvulkan.graphics.vk.ShaderModule
import dev.ktxvulkan.graphics.vk.Swapchain
import io.github.oshai.kotlinlogging.KLoggable
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*

class GraphicsPipeline(
    val device: Device,
    val swapchain: Swapchain,
    val shaderStages: List<ShaderStage>,
    val pipelineStates: PipelineStates,
    val descriptorSetLayouts: List<DescriptorSetLayout>
) : KLoggable {
    private var vkPipeline: Long
    private var vkPipelineLayout: Long
    override val logger = logger()

    init {
        MemoryStack.stackPush().use { stack ->
            with(stack) {
                val shaderStageInfos = VkPipelineShaderStageCreateInfo.calloc(shaderStages.size, stack)
                shaderStages.forEachIndexed { index, shaderStage ->
                    shaderStageInfos[index]
                        .sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                        .stage(shaderStage.stage)
                        .module(shaderStage.shaderModule.vkShaderModule)
                        .pName(stack.ASCII(shaderStage.shaderModule.entryName))
                }

                val vertexInputState = pipelineStates.vertexInputState.getVkPipelineVertexInputStateCreateInfo()
                val inputAssemblyState = pipelineStates.inputAssemblyState.getVkPipelineInputAssemblyStateCreateInfo()
                val viewPortState = pipelineStates.viewPortState.getVkPipelineViewportStateCreateInfo()
                val rasterizerState = pipelineStates.rasterizerState.getVkPipelineRasterizationStateCreateInfo()
                val multisamplingState = pipelineStates.multisamplingState.getVkPipelineMultisampleStateCreateInfo()
                val depthStencilState = pipelineStates.depthStencilState.getVkPipelineDepthStencilStateCreateInfo()
                val colorBlendState = pipelineStates.colorBlendState.getVkPipelineColorBlendStateCreateInfo()
                val dynamicState = pipelineStates.getVkPipelineDynamicStateCreateInfo()

                val pSetLayouts = stack.callocLong(descriptorSetLayouts.size)
                descriptorSetLayouts.forEachIndexed { index, descriptorSetLayout ->
                    pSetLayouts.put(index, descriptorSetLayout.vkDescriptorSetLayout)
                }

                val pipelineLayoutInfo = VkPipelineLayoutCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
                    .pSetLayouts(pSetLayouts)

                val pipelineLayoutBuf = stack.callocLong(1)
                val vkCreatePipelineLayoutResult =
                    vkCreatePipelineLayout(device.vkDevice, pipelineLayoutInfo, null, pipelineLayoutBuf)
                vkCheckResult(vkCreatePipelineLayoutResult, "Failed to create pipeline layout")
                logger.info("successfully created pipeline layout")
                vkPipelineLayout = pipelineLayoutBuf[0]

                val pipelineInfo = VkGraphicsPipelineCreateInfo.calloc(1, stack).also {
                    it[0].sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO)
                        .pStages(shaderStageInfos)
                        .pVertexInputState(vertexInputState)
                        .pInputAssemblyState(inputAssemblyState)
                        .pViewportState(viewPortState)
                        .pRasterizationState(rasterizerState)
                        .pMultisampleState(multisamplingState)
                        .pDepthStencilState(depthStencilState)
                        .pColorBlendState(colorBlendState)
                        .pDynamicState(dynamicState)
                        .layout(vkPipelineLayout)
                        .renderPass(swapchain.renderPass.vkRenderPass)
                        .subpass(0)
                        .basePipelineHandle(0)
                        .basePipelineIndex(-1)
                }

                val vkPipelineBuf = stack.callocLong(1)
                val vkCreateGraphicsPipelinesResult =
                    vkCreateGraphicsPipelines(
                        device.vkDevice, 0,
                        pipelineInfo, null, vkPipelineBuf
                    )
                vkCheckResult(vkCreateGraphicsPipelinesResult, "failed to create graphics pipeline")
                vkPipeline = vkPipelineBuf[0]

                logger.info("successfully created graphics pipeline")
            }
        }
    }

    fun destroy() {
        shaderStages.forEach { it.shaderModule.destroy() }
        descriptorSetLayouts.forEach { it.destroy() }
        vkDestroyPipelineLayout(device.vkDevice, vkPipelineLayout, null)
        vkDestroyPipeline(device.vkDevice, vkPipeline, null)
    }

    data class ShaderStage(val stage: Int, val shaderModule: ShaderModule)
}