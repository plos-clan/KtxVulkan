package dev.ktxvulkan.graphics.vk

import dev.ktxvulkan.graphics.utils.ShaderCompiler
import dev.ktxvulkan.graphics.utils.vkCheckResult
import dev.ktxvulkan.utils.ResourceHelper
import io.github.oshai.kotlinlogging.KLoggable
import io.github.oshai.kotlinlogging.KLogger
import org.lwjgl.system.MemoryStack
import org.lwjgl.util.shaderc.Shaderc
import org.lwjgl.util.shaderc.Shaderc.shaderc_compilation_status_success
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkShaderModuleCreateInfo
import kotlin.system.measureNanoTime

class ShaderModule(val device: Device, val name: String, val binary: ByteArray, val kind: Int, val entryName: String) : KLoggable {
    override val logger = logger()
    val vkShaderModule: Long

    init {
        MemoryStack.stackPush().use { stack ->
            val byteBuffer = stack.calloc(binary.size)
            byteBuffer.put(binary)
            byteBuffer.flip()
            val vkShaderModuleCreateInfo = VkShaderModuleCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
                .pCode(byteBuffer)

            val vkShaderModuleBuffer = stack.callocLong(1)
            val vkCreateShaderModuleResult =
                vkCreateShaderModule(device.vkDevice, vkShaderModuleCreateInfo, null, vkShaderModuleBuffer)
            vkCheckResult(vkCreateShaderModuleResult, "Failed to create shader module (kind$kind-$name)")
            vkShaderModule = vkShaderModuleBuffer[0]
        }
    }

    fun destroy() {
        vkDestroyShaderModule(device.vkDevice, vkShaderModule, null)
    }

    companion object : KLoggable {
        override val logger = logger()

        fun compile(device: Device, name: String, kind: Int): ShaderModule {
            val source = ResourceHelper.getResourceStream(name)!!.use {
                it.readBytes()
            }.decodeToString()

            val binary: ByteArray

            val timeUsed = measureNanoTime {
                val compiler = ShaderCompiler()
                val result = compiler.compileGlslToSpv(source, kind, name)
                if (result.compilationStatus != shaderc_compilation_status_success) {
                    logger.error("Failed to compile shader ($name, $kind), " +
                            "${result.numErrors} error(s) occurred: \n${result.errorMessage}")
                    throw IllegalStateException()
                }

                val remaining = result.binary!!.remaining()
                binary = ByteArray(remaining)
                result.binary!!.get(binary)
            }
            logger.info("Successfully compiled shader $name in ${timeUsed}ns.")

            return ShaderModule(device, name, binary, kind, "main")
        }

        fun cached(device: Device, name: String, kind: Int): ShaderModule {
            val binary = ResourceHelper.getResourceStream("$name.spv")?.use {
                it.readBytes()
            }
            return if (binary == null) {
                compile(device, name, kind)
            } else {
                ShaderModule(device, name, binary, kind, "main")
            }
        }
    }
}