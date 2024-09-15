package dev.ktxvulkan.graphics.vk

import dev.ktxvulkan.graphics.utils.vkCheckResult
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding
import org.lwjgl.vulkan.VkDescriptorSetLayoutCreateInfo

class DescriptorSetLayout(val device: Device, val bindings: List<DescriptorSetLayoutBinding>, val vkDescriptorSetLayout: Long) {
    fun destroy() {
        vkDestroyDescriptorSetLayout(device.vkDevice, vkDescriptorSetLayout, null)
    }

    companion object {
        fun build(device: Device, builder: Builder.() -> Unit): DescriptorSetLayout {
            val build = Builder()
            build.builder()
            return build.build(device)
        }
    }

    class Builder {
        private val bindings = mutableListOf<DescriptorSetLayoutBinding>()

        fun uniformBuffer(count: Int = 1, stageFlags: Int = VK_SHADER_STAGE_ALL): Builder {
            bindings.add(
                DescriptorSetLayoutBinding(
                    VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER,
                    count,
                    stageFlags
                )
            )
            return this
        }

        fun combinedImageSampler(count: Int = 1, stageFlags: Int = VK_SHADER_STAGE_ALL): Builder {
            bindings.add(
                DescriptorSetLayoutBinding(
                    VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
                    count,
                    stageFlags
                )
            )
            return this
        }

        fun custom(
            type: Int,
            count: Int,
            stageFlags: Int,
            immutableSamplers: List<Long> = emptyList()
        ): Builder {
            bindings.add(
                DescriptorSetLayoutBinding(
                    type, count, stageFlags, immutableSamplers
                )
            )
            return this
        }

        fun build(device: Device): DescriptorSetLayout {
            MemoryStack.stackPush().use { stack ->
                val bindings = bindings.toList()
                val bindingsBuffer = VkDescriptorSetLayoutBinding.calloc(bindings.size, stack)

                bindings.forEachIndexed { index, descriptorSetLayoutBinding ->
                    val vkDescriptorSetLayoutBinding = VkDescriptorSetLayoutBinding.calloc(stack)
                        .descriptorType(descriptorSetLayoutBinding.type)
                        .descriptorCount(descriptorSetLayoutBinding.count)
                        .stageFlags(descriptorSetLayoutBinding.stageFlags)
                        .binding(index)

                    if (descriptorSetLayoutBinding.immutableSamplers.isNotEmpty()) {
                        val samplers = descriptorSetLayoutBinding.immutableSamplers
                        val pImmutableSamplers = stack.callocLong(samplers.size)
                        samplers.forEachIndexed { index, l -> pImmutableSamplers.put(index, l) }
                        vkDescriptorSetLayoutBinding.pImmutableSamplers(pImmutableSamplers)
                    }

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
        val immutableSamplers: List<Long> = emptyList()
    )
}