package dev.ktxvulkan.graphics.vk

import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkExtent2D
import org.lwjgl.vulkan.VkFramebufferCreateInfo

class Framebuffer(val device: Device, val attachments: List<Long>, val renderPass: RenderPass, extent: VkExtent2D) {
    val handle: Long

    init {
        MemoryStack.stackPush().use { stack ->
            val pAttachments = stack.callocLong(attachments.size)
            attachments.forEachIndexed { index, l -> pAttachments.put(index, l) }
            val framebufferInfo = VkFramebufferCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
                .renderPass(renderPass.vkRenderPass)
                .attachmentCount(attachments.size)
                .pAttachments(pAttachments)
                .width(extent.width())
                .height(extent.height())
                .layers(1)

            val buf = stack.callocLong(1)
            vkCreateFramebuffer(renderPass.device.vkDevice, framebufferInfo, null, buf)
            handle = buf[0]
        }
    }

    fun destroy() {
        vkDestroyFramebuffer(device.vkDevice, handle, null)
    }
}