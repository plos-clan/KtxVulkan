package dev.ktxvulkan.graphics.vk

import dev.ktxvulkan.graphics.utils.vkCheckResult
import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding
import org.lwjgl.vulkan.VkDescriptorSetLayoutCreateInfo
import java.nio.LongBuffer

class DescriptorSetLayout(val device: Device, val bindings: List<DescriptorSetLayoutBinding>, val vkDescriptorSetLayout: Long) {

    class Builder {
        private val bindings = mutableListOf<DescriptorSetLayoutBinding>()

        fun uniformBuffer(count: Int = 1, stageFlags: Int = VK_SHADER_STAGE_ALL): Builder {
            bindings.add(
                DescriptorSetLayoutBinding(
                    VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER,
                    count,
                    stageFlags,
                    0
                )
            )
            return this
        }

        fun combinedImageSampler(count: Int = 1, stageFlags: Int = VK_SHADER_STAGE_ALL): Builder {
            bindings.add(
                DescriptorSetLayoutBinding(
                    VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
                    count,
                    stageFlags,
                    0
                )
            )
            return this
        }

        fun custom(
            type: Int,
            count: Int,
            stageFlags: Int,
            pImmutableSamplers: Long = 0
        ): Builder {
            bindings.add(
                DescriptorSetLayoutBinding(
                    type, count, stageFlags, pImmutableSamplers
                )
            )
            return this
        }

        fun build(device: Device): DescriptorSetLayout {
            MemoryStack.stackPush().use { stack ->
                val bindings = bindings.toList()
                val bindingsBuffer = VkDescriptorSetLayoutBinding.calloc(bindings.size, stack)

                bindings.forEachIndexed { index, descriptorSetLayoutBinding ->
                    val longBuffer = stack.callocLong(1).put(0, descriptorSetLayoutBinding.pImmutableSamplers)
                    val vkDescriptorSetLayoutBinding = VkDescriptorSetLayoutBinding.calloc(stack)
                        .descriptorType(descriptorSetLayoutBinding.type)
                        .descriptorCount(descriptorSetLayoutBinding.count)
                        .stageFlags(descriptorSetLayoutBinding.stageFlags)
                        .pImmutableSamplers(longBuffer)
                        .binding(index)
                    bindingsBuffer.put(index, vkDescriptorSetLayoutBinding)
                }

                val vkDescriptorSetLayoutCreateInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
                    .pBindings(bindingsBuffer)

                val vkDescriptorSetLayoutBuffer = stack.callocLong(1)
                val vkCreateDescriptorSetLayoutResult =
                    vkCreateDescriptorSetLayout(
                        device.vkDevice, vkDescriptorSetLayoutCreateInfo,
                        null, vkDescriptorSetLayoutBuffer
                    )
                vkCheckResult(vkCreateDescriptorSetLayoutResult, "Failed to create vkDescriptorSetLayout: $bindings")

                return DescriptorSetLayout(device, bindings, vkDescriptorSetLayoutBuffer[0])
            }
        }
    }

    data class DescriptorSetLayoutBinding(
        val type: Int,
        val count: Int,
        val stageFlags: Int,
        val pImmutableSamplers: Long = 0
    )
}