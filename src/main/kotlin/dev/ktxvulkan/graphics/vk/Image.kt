package dev.ktxvulkan.graphics.vk

import dev.ktxvulkan.graphics.utils.vkCheckResult
import dev.ktxvulkan.graphics.vk.buffer.findMemoryType
import io.github.oshai.kotlinlogging.KLoggable
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*

/**
 * Simple wrapper of VkImage
 * Only supports single image view yet
 */
@Suppress("LeakingThis")
open class Image(
    val device: Device,
    val vkImage: Long,
    val format: Int,
    val extent: VkExtent2D,
    val vkDeviceMemory: Long = 0
) : KLoggable {
    final override val logger = logger()
    var imageView = 0L; private set

    fun createImageView(aspectFlags: Int, mipmapLevels: Int) {
        if (destroyImageView()) {
            logger.debug("Image {} has a existed image view {}, deleting", vkImage, imageView)
        }
        MemoryStack.stackPush().use { stack ->
            val vkImageViewCreateInfo = VkImageViewCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                .image(vkImage)
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
        if (destroyImage) {
            vkDestroyImage(device.vkDevice, vkImage, null)
            if (vkDeviceMemory != 0L) {
                vkFreeMemory(device.vkDevice, vkDeviceMemory, null)
            }
        }
    }

    companion object {
//        private fun findMemoryType(device: Device, typeFilter: Int, properties: Int): Int {
//            MemoryStack.stackPush().use { stack ->
//                val memProperties = VkPhysicalDeviceMemoryProperties.calloc()
//                vkGetPhysicalDeviceMemoryProperties(device.physicalDevice.vkPhysicalDevice, memProperties)
//
//                for (i in 0..<memProperties.memoryTypeCount()) {
//                    if (memProperties.memoryTypes()[i].propertyFlags() and properties == properties
//                        && typeFilter and (1 shl i) != 0) return i
//                }
//
//                throw IllegalStateException("failed to find suitable memory type!");
//            }
//        }

        fun create(
            device: Device,
            width: Int,
            height: Int,
            mipmapLevels: Int,
            numSample: Int,
            format: Int,
            tiling: Int,
            usage: Int,
            memoryProperties: Int
        ): Image {
            MemoryStack.stackPush().use { stack ->
                val imageInfo = VkImageCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
                    .imageType(VK_IMAGE_TYPE_2D)
                    .extent { it.width(width).height(height).depth(1) }
                    .mipLevels(mipmapLevels)
                    .arrayLayers(1)
                    .format(format)
                    .tiling(tiling)
                    .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                    .usage(usage)
                    .samples(numSample)
                    .sharingMode(VK_SHARING_MODE_EXCLUSIVE)
                val imageBuf = stack.callocLong(1)
                val vkCreateImageResult = vkCreateImage(device.vkDevice, imageInfo, null, imageBuf)
                vkCheckResult(vkCreateImageResult, "failed to create image")
                val imageHandle = imageBuf[0]

                val memRequirements = VkMemoryRequirements.calloc(stack)
                vkGetImageMemoryRequirements(device.vkDevice, imageHandle, memRequirements)

                val allocInfo = VkMemoryAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                    .allocationSize(memRequirements.size())
                    .memoryTypeIndex(findMemoryType(device, memRequirements.memoryTypeBits(), memoryProperties))

                vkAllocateMemory(device.vkDevice, allocInfo, null, imageBuf)
                val imageMemory = imageBuf[0]

                vkBindImageMemory(device.vkDevice, imageHandle, imageMemory, 0)

                return Image(device, imageHandle, format, VkExtent2D.create().width(width).height(height), imageMemory)
            }
        }
    }
}