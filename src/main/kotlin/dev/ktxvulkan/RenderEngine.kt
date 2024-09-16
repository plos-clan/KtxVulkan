package dev.ktxvulkan

import dev.ktxvulkan.graphics.Window
import dev.ktxvulkan.graphics.utils.VertexAttribute
import dev.ktxvulkan.graphics.utils.VertexFormat
import dev.ktxvulkan.graphics.utils.vkCheckResult
import dev.ktxvulkan.graphics.vk.*
import dev.ktxvulkan.graphics.vk.buffer.PMVertexBuffer
import dev.ktxvulkan.graphics.vk.comand.CommandPool
import dev.ktxvulkan.graphics.vk.pipeline.*
import dev.ktxvulkan.utils.math.Vec4f
import io.github.oshai.kotlinlogging.KLoggable
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.system.MemoryStack
import org.lwjgl.util.shaderc.Shaderc.shaderc_glsl_fragment_shader
import org.lwjgl.util.shaderc.Shaderc.shaderc_glsl_vertex_shader
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRSwapchain.*
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VK13.*


object RenderEngine : KLoggable {
    private var ended: Boolean = false
    override val logger = logger()

    lateinit var window: Window
    lateinit var instance: Instance
    lateinit var physicalDevice: PhysicalDevice
    lateinit var device: Device
    lateinit var swapchain: Swapchain
    lateinit var pipeline: GraphicsPipeline
    lateinit var vertexBuffer: PMVertexBuffer
    lateinit var commandPool: CommandPool
    lateinit var commandBuffer: VkCommandBuffer
    var imageAvailableSemaphore: Long = 0
    var renderFinishedSemaphore: Long = 0
    var inFlightFence: Long = 0

    var clearColor = Vec4f(0f, 0f, 0f, 1f)
    var clearDepth = 1.0f
    var clearStencil = 0
    val stencilOpState = StencilOpState()

    fun initialize() {
        window = Window()
        instance = Instance(true)
        window.createSurface(instance)
        physicalDevice = PhysicalDevice(instance, window)
        device = Device(physicalDevice)
        commandPool = CommandPool(device)
        commandBuffer = commandPool.primaryBuffer
        swapchain = Swapchain(device, window)

        val descriptorSetLayout = DescriptorSetLayout.build(device) {
            uniformBuffer(stageFlags = VK_SHADER_STAGE_VERTEX_BIT)
            combinedImageSampler(stageFlags = VK_SHADER_STAGE_FRAGMENT_BIT)
        }
        pipeline = GraphicsPipeline(
            device, swapchain,
            listOf(
                GraphicsPipeline.ShaderStage(
                    VK_SHADER_STAGE_VERTEX_BIT,
                    ShaderModule.cached(
                        device,
                        "engine/shaders/shader.vert",
                        shaderc_glsl_vertex_shader
                    )
                ),
                GraphicsPipeline.ShaderStage(
                    VK_SHADER_STAGE_FRAGMENT_BIT,
                    ShaderModule.cached(
                        device,
                        "engine/shaders/shader.frag",
                        shaderc_glsl_fragment_shader
                    )
                )
            ),
            PipelineStates(
                VertexInputState(
                    VertexFormat(listOf(
                        VertexAttribute.R32G32B32_SFLOAT, // vec3 pos
                        VertexAttribute.R32G32B32A32_SFLOAT, // vec4 color
//                        VertexAttribute.R32G32_SFLOAT // vec2 uv
                    ))
                ),
                InputAssemblyState(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST),
                ViewportState(1, 1, null, null),
                RasterizerState(
                    depthClampEnable = false,
                    rasterizerDiscardEnable = false,
                    polygonMode = VK_POLYGON_MODE_FILL,
                    lineWidth = 1.0f,
                    cullMode = CullMode.None,
                    depthBiasEnable = false
                ),
                MultisamplingState(
                    physicalDevice.msaaSamples,
                    sampleShadingEnable = false,
                    minSampleShading = 0.2f
                ),
                DepthStencilState(
                    depthTestEnable = true,
                    depthWriteEnable = true,
                    depthCompareOp = CompareOp.COMPARE_OP_LESS,
                    depthBoundsTestEnable = false,
                    stencilTestEnable = true
                ),
                ColorBlendState(
                    logicOpEnable = false,
                    logicOp = LogicOp.LOGIC_OP_COPY,
                    colorAttachments = listOf(ColorBlendAttachmentState(blendEnable = false))
                ),
                setOf(
                    DynamicState.DYNAMIC_STATE_VIEWPORT,
                    DynamicState.DYNAMIC_STATE_SCISSOR,
                    DynamicState.DYNAMIC_STATE_STENCIL_OP,
                    DynamicState.DYNAMIC_STATE_STENCIL_TEST_ENABLE,
                    DynamicState.DYNAMIC_STATE_STENCIL_WRITE_MASK,
                    DynamicState.DYNAMIC_STATE_STENCIL_COMPARE_MASK,
                    DynamicState.DYNAMIC_STATE_PRIMITIVE_TOPOLOGY
                )
            ),
            listOf(descriptorSetLayout)
        )
        vertexBuffer = PMVertexBuffer(device)
        createSyncObjects()
    }

    fun createSyncObjects() {
        MemoryStack.stackPush().use { stack ->
            val semaphoreInfo = VkSemaphoreCreateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO)
            val fenceInfo = VkFenceCreateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO)
                .flags(VK_FENCE_CREATE_SIGNALED_BIT)

            val buf = stack.callocLong(1)
            vkCheckResult(vkCreateSemaphore(device.vkDevice, semaphoreInfo, null, buf), "failed to create semaphore")
            imageAvailableSemaphore = buf[0]
            vkCheckResult(vkCreateSemaphore(device.vkDevice, semaphoreInfo, null, buf), "failed to create semaphore")
            renderFinishedSemaphore = buf[0]
            vkCheckResult(vkCreateFence(device.vkDevice, fenceInfo, null, buf), "failed to create fence")
            inFlightFence = buf[0]
        }
    }

    private fun recordCommandBuffer(imageIndex: Int, drawOp: () -> Unit) {
        MemoryStack.stackPush().use { stack ->
            val beginInfo = VkCommandBufferBeginInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                .pInheritanceInfo(null)

            vkCheckResult(
                vkBeginCommandBuffer(commandBuffer, beginInfo),
                "failed to begin recording command buffer!"
            )

            val renderPassInfo = VkRenderPassBeginInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
                .renderPass(swapchain.renderPass.vkRenderPass)
                .framebuffer(swapchain.framebuffers[imageIndex].handle)
                .renderArea { area ->
                    area.offset { it.x(0).y(0) }
                        .extent(swapchain.swapchainExtent)
                }
            val clearValues = VkClearValue.calloc(2, stack)
            clearValues[0].color {
                clearColor.apply(it)
            }
            clearValues[1].depthStencil {
                it.depth(clearDepth).stencil(clearStencil)
            }
            renderPassInfo.clearValueCount(2).pClearValues(clearValues)

            vkCmdBeginRenderPass(commandBuffer, renderPassInfo, VK_SUBPASS_CONTENTS_INLINE)

            vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.vkPipeline)

            val viewPort = VkViewport.calloc(1, stack)
            viewPort[0].x(0f).y(0f)
                .width(swapchain.swapchainExtent.width().toFloat())
                .height(swapchain.swapchainExtent.height().toFloat())
                .minDepth(0f)
                .maxDepth(1f)
            vkCmdSetViewport(commandBuffer, 0, viewPort)

            val scissor = VkRect2D.calloc(1, stack)
            scissor[0].offset { it.x(0).y(0) }
                .extent(swapchain.swapchainExtent)
            vkCmdSetScissor(commandBuffer, 0, scissor)

            vkCmdSetStencilTestEnable(commandBuffer, false)
            vkCmdSetStencilOp(commandBuffer, 1, VK_STENCIL_OP_KEEP, VK_STENCIL_OP_KEEP, VK_STENCIL_OP_KEEP, VK_COMPARE_OP_NEVER)

            drawOp()

            vkCmdEndRenderPass(commandBuffer)

            vkCheckResult(vkEndCommandBuffer(commandBuffer), "failed to record command buffer!")
        }
    }

    private fun flushSwapchain() {
        val widthBuf = IntArray(1)
        val heightBuf = IntArray(1)
        var width = 0
        var height = 0
        while (width == 0 || height == 0) {
            glfwGetFramebufferSize(window.handle, widthBuf, heightBuf)
            width = widthBuf[0]
            height = heightBuf[0]
            glfwWaitEvents()
        }
        device.waitIdle()
        swapchain.destroy(surface = false)
        swapchain = Swapchain(device, window)
    }

    fun run() {
        while (!window.shouldClose()) {
            glfwPollEvents()

            MemoryStack.stackPush().use { stack ->
                with(stack) {
                    val pFence = callocLong(1).put(0, inFlightFence)
                    vkWaitForFences(device.vkDevice, pFence, true, Long.MAX_VALUE)

                    val imageIndexBuf = callocInt(1)
                    val acquireResult = vkAcquireNextImageKHR(
                        device.vkDevice, swapchain.vkSwapchain,
                        Long.MAX_VALUE, imageAvailableSemaphore,
                        VK_NULL_HANDLE, imageIndexBuf
                    )
                    if (acquireResult == VK_ERROR_OUT_OF_DATE_KHR || acquireResult == VK_SUBOPTIMAL_KHR) {
                        flushSwapchain()
                        return@use
                    }
                    val imageIndex = imageIndexBuf[0]

                    vkResetFences(device.vkDevice, pFence)

                    vkResetCommandBuffer(commandBuffer, 0)
                    recordCommandBuffer(imageIndex) {
                        vertexBuffer.apply {
                            vertex(0f, -0.5f, 0f, 1f, 0f, 0f, 1f)
                            vertex(0.5f, 0.5f, 0f, 0f, 1f, 0f, 1f)
                            vertex(-0.5f, 0.5f, 0f, 0f, 0f, 1f, 1f)
                            draw(commandBuffer, VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST)
                        }
                    }

                    val submitInfo = VkSubmitInfo.calloc(this)
                        .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)

                    val waitSemaphores = callocLong(1).put(0, imageAvailableSemaphore)
                    val waitStages = callocInt(1).put(0, VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)

                    submitInfo.waitSemaphoreCount(1)
                        .pWaitSemaphores(waitSemaphores)
                        .pWaitDstStageMask(waitStages)
                        .pCommandBuffers(callocPointer(1).put(0, commandBuffer))

                    val signalSemaphores = callocLong(1).put(0, renderFinishedSemaphore)

                    submitInfo.pSignalSemaphores(signalSemaphores)

                    vkCheckResult(vkQueueSubmit(device.graphicsQueue, submitInfo, inFlightFence), "failed to submit queue")

                    val presentInfo = VkPresentInfoKHR.calloc(this)
                        .sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
                        .pWaitSemaphores(signalSemaphores)
                        .swapchainCount(1)
                        .pSwapchains(callocLong(1).put(0, swapchain.vkSwapchain))
                        .pImageIndices(callocInt(1).put(0, imageIndex))

                    val result = vkQueuePresentKHR(device.presentQueue, presentInfo)
                    if (result == VK_SUBOPTIMAL_KHR || result == VK_ERROR_OUT_OF_DATE_KHR) {
                        flushSwapchain()
                    } else ;/* vkCheckResult(result, "unexpected failure in vkQueuePresentKHR")*/
                }
            }
        }

        device.waitIdle()
        ended = true
    }

    fun cleanup() {
        if (!ended) return
        vkDestroySemaphore(device.vkDevice, imageAvailableSemaphore, null)
        vkDestroySemaphore(device.vkDevice, renderFinishedSemaphore, null)
        vkDestroyFence(device.vkDevice, inFlightFence, null)
        commandPool.destroy()
        vertexBuffer.destroy()
        pipeline.destroy()
        swapchain.destroy()
        window.destroy()
        device.destroy()
        physicalDevice.destroy()
        instance.destroy()
    }
}