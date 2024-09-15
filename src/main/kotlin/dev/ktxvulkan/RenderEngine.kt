package dev.ktxvulkan

import dev.ktxvulkan.graphics.Window
import dev.ktxvulkan.graphics.utils.VertexAttribute
import dev.ktxvulkan.graphics.utils.VertexFormat
import dev.ktxvulkan.graphics.vk.*
import dev.ktxvulkan.graphics.vk.pipeline.*
import io.github.oshai.kotlinlogging.KLoggable
import org.lwjgl.glfw.GLFW.glfwPollEvents
import org.lwjgl.util.shaderc.Shaderc.shaderc_glsl_fragment_shader
import org.lwjgl.util.shaderc.Shaderc.shaderc_glsl_vertex_shader
import org.lwjgl.vulkan.VK10.*


object RenderEngine : KLoggable {
    override val logger = logger()

    lateinit var window: Window
    lateinit var instance: Instance
    lateinit var physicalDevice: PhysicalDevice
    lateinit var device: Device
    lateinit var swapchain: Swapchain
    lateinit var pipeline: GraphicsPipeline

    fun initialize() {
        window = Window()
        instance = Instance(true)
        window.createSurface(instance)
        physicalDevice = PhysicalDevice(instance, window)
        device = Device(physicalDevice)
        swapchain = Swapchain(device, window)
        swapchain.createSwapchainImageViews()

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
                    stencilTestEnable = false
                ),
                ColorBlendState(
                    logicOpEnable = false,
                    logicOp = LogicOp.LOGIC_OP_COPY,
                    colorAttachments = listOf(ColorBlendAttachmentState(blendEnable = false)),
                ),
                setOf(
                    DynamicState.DYNAMIC_STATE_VIEWPORT,
                    DynamicState.DYNAMIC_STATE_SCISSOR
                )
            ),
            listOf(descriptorSetLayout)
        )
    }

    fun run() {
        while (!window.shouldClose()) {
            glfwPollEvents()
            window.swapBuffer()
        }
    }

    fun cleanup() {
        pipeline.destroy()
        swapchain.destroy()
        window.destroy()
        device.destroy()
        physicalDevice.destroy()
        instance.destroy()
    }
}