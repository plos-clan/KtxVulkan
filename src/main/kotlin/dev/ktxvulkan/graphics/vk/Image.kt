package dev.ktxvulkan.graphics.vk

import dev.ktxvulkan.graphics.utils.vkCheckResult
import io.github.oshai.kotlinlogging.KLoggable
import io.github.oshai.kotlinlogging.KLogger
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkExtent2D
import org.lwjgl.vulkan.VkImageViewCreateInfo

/**
 * Simple wrapper of VkImage
 * Only supports single image view yet
 */
@Suppress("LeakingThis")
open class Image(val device: Device, val image: Long, val format: Int, val extent: VkExtent2D) : KLoggable {
    final override val logger = logger()
    var imageView = 0L

    fun createImageView(aspectFlags: Int, mipmapLevels: Int) {
        if (destroyImageView()) {
            logger.debug("Image {} has a existed image view {}, deleting", image, imageView)
        }
        MemoryStack.stackPush().use { stack ->
            val vkImageViewCreateInfo = VkImageViewCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                .image(image)
                .viewType(VK_IMAGE_VIEW_TYPE_2D)
                .format(format).apply {
                    subresourceRange().apply {
                        aspectMask(aspectFlags)
                        baseMipLevel(0)
                        levelCount(mipmapLevels)
                        baseArrayLayer(0)
                        layerCount(1)
                    }
                }

            val imageView = stack.callocLong(1)
            val vkCreateImageViewResult =
                vkCreateImageView(device.vkDevice, vkImageViewCreateInfo, null, imageView)
            vkCheckResult(vkCreateImageViewResult, "failed to create image view")
            this.imageView = imageView[0]
        }
    }

    fun destroyImageView(): Boolean {
        val imageView = imageView
        if (imageView != 0L) {
            vkDestroyImageView(device.vkDevice, imageView, null)
            return true
        }
        return false
    }

    fun destroy(destroyImageView: Boolean = true, destroyImage: Boolean = true) {
        if (destroyImageView) destroyImageView()
        if (destroyImage) vkDestroyImage(device.vkDevice, image, null)
    }
}