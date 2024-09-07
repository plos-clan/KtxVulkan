package dev.ktxvulkan.graphics.vk

import dev.ktxvulkan.graphics.Window
import dev.ktxvulkan.graphics.utils.vkCheckResult
import io.github.oshai.kotlinlogging.KLoggable
import org.lwjgl.glfw.GLFW.glfwGetFramebufferSize
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.KHRSurface.*
import org.lwjgl.vulkan.KHRSwapchain.*
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkExtent2D
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR
import org.lwjgl.vulkan.VkSurfaceFormatKHR
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR

class Swapchain(val device: Device, val window: Window) : KLoggable {
    override val logger = logger()
    val vkSwapchain: Long
    val swapchainImages: List<Image>
    val swapchainImageFormat: Int
    val swapchainExtent: VkExtent2D
    val renderPass: RenderPass

    init {
        val swapchainSupport = device.physicalDevice.swapchainSupport
        val surfaceFormat = chooseSwapSurfaceFormat(swapchainSupport.formats)
        val presentMode = chooseSwapPresentMode(swapchainSupport.presentModes)
        val extent = chooseSwapExtent(swapchainSupport.capabilities)

        var imageCount = swapchainSupport.capabilities.minImageCount() + 1

        if (swapchainSupport.capabilities.maxImageCount() in 1..<imageCount) {
            imageCount = swapchainSupport.capabilities.maxImageCount()
        }
        val expectedImageCount =
            (swapchainSupport.capabilities.minImageCount() + 1).coerceIn(
                1, swapchainSupport.capabilities.maxImageCount()
            )
        if (expectedImageCount != imageCount) {
            logger.error("Image Count doesn't match: {}, expected {}", imageCount, expectedImageCount);
        }

        MemoryStack.stackPush().use { stack ->
            val vkSwapchainCreateInfoKHR = VkSwapchainCreateInfoKHR.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
                .surface(window.surface)
                .minImageCount(imageCount)
                .imageFormat(surfaceFormat.format())
                .imageColorSpace(surfaceFormat.colorSpace())
                .imageExtent(extent)
                .imageArrayLayers(1)
                .imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT)
                .preTransform(swapchainSupport.capabilities.currentTransform())
                .compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR) // Ignore alpha channel in blending with other windows
                .presentMode(presentMode)
                .clipped(true)
                .oldSwapchain(0) // leave this null because we will not recreate the swapchain

            val indices = device.physicalDevice.queueFamilyIndices
            val queueFamilyIndices = stack.ints(indices.graphicsFamily, indices.presentFamily)
            if (indices.graphicsFamily != indices.presentFamily) {
                logger.debug("swapchain is configured to concurrent image usage due to different queues")
                vkSwapchainCreateInfoKHR
                    .imageSharingMode(VK_SHARING_MODE_CONCURRENT)
                    .queueFamilyIndexCount(2)
                    .pQueueFamilyIndices(queueFamilyIndices)
            } else {
                logger.debug("swapchain is configured to serial image usage")
                vkSwapchainCreateInfoKHR
                    .imageSharingMode(VK_SHARING_MODE_EXCLUSIVE)
                    .queueFamilyIndexCount(0)
                    .pQueueFamilyIndices(null)
            }

            val pVkSwapchain = stack.callocLong(1)
            val vkCreateSwapchainKHRResult =
                vkCreateSwapchainKHR(device.vkDevice, vkSwapchainCreateInfoKHR, null, pVkSwapchain)
            vkCheckResult(vkCreateSwapchainKHRResult, "failed to create swapchain")
            vkSwapchain = pVkSwapchain[0]

            logger.info("successfully created swapchain")
            val intBuffer = stack.callocInt(1)
            vkGetSwapchainImagesKHR(device.vkDevice, vkSwapchain, intBuffer, null)
            val swapchainImages = stack.mallocLong(intBuffer[0])
            vkGetSwapchainImagesKHR(device.vkDevice, vkSwapchain, intBuffer, swapchainImages)
            swapchainImageFormat = surfaceFormat.format()
            swapchainExtent = extent
            this.swapchainImages = buildList {
                for (i in 0..<intBuffer[0])
                    add(Image(device, swapchainImages[i], swapchainImageFormat, swapchainExtent))
            }
        }

        renderPass = RenderPass(device, this)
    }

    fun createSwapchainImageViews() {
        swapchainImages.forEachIndexed { index, image ->
            image.createImageView(
                VK_IMAGE_ASPECT_COLOR_BIT,
                1
            )
            logger.info("successfully created swapchain image view {}", index)
        }
    }

    private fun chooseSwapExtent(capabilities: VkSurfaceCapabilitiesKHR): VkExtent2D {
        if (capabilities.currentExtent().width() != Int.MAX_VALUE) {
            return capabilities.currentExtent()
        } else {
            val widthBuf = IntArray(1)
            val heightBuf = IntArray(1)
            glfwGetFramebufferSize(window.handle, widthBuf, heightBuf)
            val width = widthBuf[0]
            val height = heightBuf[0]

            val actualExtent = VkExtent2D.calloc().width(width).height(height)

            actualExtent.width(
                actualExtent.width().coerceIn(
                    capabilities.minImageExtent().width(),
                    capabilities.maxImageExtent().width()
                )
            )
            actualExtent.height(
                actualExtent.height().coerceIn(
                    capabilities.minImageExtent().height(),
                    capabilities.maxImageExtent().height()
                )
            )

            return actualExtent
        }
    }

    private fun chooseSwapSurfaceFormat(availableFormats: List<VkSurfaceFormatKHR>): VkSurfaceFormatKHR {
        for (availableFormat in availableFormats) {
            if (availableFormat.format() == VK_FORMAT_B8G8R8A8_SRGB
                && availableFormat.colorSpace() == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR) {
                return availableFormat
            }
        }
        // TODO: Complete the fallback choice logic
        return availableFormats[0]
    }

    private fun chooseSwapPresentMode(availablePresentModes: List<Int>): Int {
        for (availablePresentMode in availablePresentModes) {
            if (availablePresentMode == VK_PRESENT_MODE_MAILBOX_KHR) {
                return availablePresentMode
            }
        }

        return VK_PRESENT_MODE_FIFO_KHR
    }

    fun destroy() {
        renderPass.destroy()
        swapchainImages.forEach { it.destroy(destroyImage = false) }
        vkDestroySwapchainKHR(device.vkDevice, vkSwapchain, null)
        vkDestroySurfaceKHR(device.physicalDevice.instance.vkInstance, window.surface, null)
    }
}
