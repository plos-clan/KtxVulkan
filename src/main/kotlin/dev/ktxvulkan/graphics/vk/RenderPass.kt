package dev.ktxvulkan.graphics.vk

import dev.ktxvulkan.graphics.utils.vkCheckResult
import io.github.oshai.kotlinlogging.KLoggable
import io.github.oshai.kotlinlogging.KLogger
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR
import org.lwjgl.vulkan.VK10.*


class RenderPass(val device: Device, swapchain: Swapchain) : KLoggable {
    override val logger = logger()
    val vkRenderPass: Long

    init {
        MemoryStack.stackPush().use { stack ->
            val colorAttachment = VkAttachmentDescription.calloc(stack)
                .format(swapchain.swapchainImageFormat)
                .samples(device.physicalDevice.msaaSamples)
                .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
                .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
                .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                .finalLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)

            val depthAttachment = VkAttachmentDescription.calloc(stack)
                .format(findDepthFormat())
                .samples(device.physicalDevice.msaaSamples)
                .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
                .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
                .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                .finalLayout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL)

            val colorAttachmentResolve = VkAttachmentDescription.calloc(stack)
                .format(swapchain.swapchainImageFormat)
                .samples(VK_SAMPLE_COUNT_1_BIT)
                .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
                .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
                .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                .finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR)

            val colorAttachmentRef = VkAttachmentReference.calloc(stack)
                .attachment(0)
                .layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)

            val depthAttachmentRef = VkAttachmentReference.calloc(stack)
                .attachment(1)
                .layout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL)

            val colorAttachmentResolveRef = VkAttachmentReference.calloc(stack)
                .attachment(2)
                .layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)

            val subpass = VkSubpassDescription.calloc(stack)
                .pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
                .colorAttachmentCount(1)
                .pColorAttachments(VkAttachmentReference.calloc(1, stack).put(0, colorAttachmentRef))
                .pDepthStencilAttachment(depthAttachmentRef)
                .pResolveAttachments(VkAttachmentReference.calloc(1, stack).put(0, colorAttachmentResolveRef))

            val subpassDependency = VkSubpassDependency.calloc(stack)
                .srcSubpass(VK_SUBPASS_EXTERNAL)
                .dstSubpass(0)
                .srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT or VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT)
                .srcAccessMask(0)
                .dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT or VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT)
                .dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT or VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT)

            val attachments = VkAttachmentDescription.calloc(3, stack)
                .put(0, colorAttachment)
                .put(1, depthAttachment)
                .put(2, colorAttachmentResolve)

            val vkRenderPassCreateInfo = VkRenderPassCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
                .pAttachments(attachments)
                .pSubpasses(VkSubpassDescription.calloc(1, stack).put(0, subpass))
                .pDependencies(VkSubpassDependency.calloc(1, stack).put(0, subpassDependency))

            val pRenderPass = LongArray(1)
            val vkCreateRenderPassResult =
                vkCreateRenderPass(device.vkDevice, vkRenderPassCreateInfo, null, pRenderPass)
            vkCheckResult(vkCreateRenderPassResult, "failed to create render pass")

            logger.info("successfully created render pass")

            vkRenderPass = pRenderPass[0]
        }
    }

    fun findSupportedFormat(candidates: List<Int>, tiling: Int, features: Int): Int {
        MemoryStack.stackPush().use { stack ->
            for (format in candidates) {
                val props = VkFormatProperties.calloc(stack)
                vkGetPhysicalDeviceFormatProperties(device.physicalDevice.vkPhysicalDevice, format, props)

                if (tiling == VK_IMAGE_TILING_LINEAR && (props.linearTilingFeatures() and features) == features) {
                    return format
                }
                else if (tiling == VK_IMAGE_TILING_OPTIMAL
                    && (props.optimalTilingFeatures() and features) == features) {
                    return format
                }
            }
        }

        throw RuntimeException("failed to find supported format!")
    }

    fun findDepthFormat(): Int {
        return findSupportedFormat(
            listOf(VK_FORMAT_D32_SFLOAT, VK_FORMAT_D32_SFLOAT_S8_UINT, VK_FORMAT_D24_UNORM_S8_UINT),
            VK_IMAGE_TILING_OPTIMAL,
            VK_FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT
        )
    }

    fun destroy() {
        vkDestroyRenderPass(device.vkDevice, vkRenderPass, null)
    }
}