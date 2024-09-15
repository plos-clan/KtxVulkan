package dev.ktxvulkan.graphics.utils

import org.lwjgl.system.MemoryStack
import org.lwjgl.util.shaderc.Shaderc

class ShaderCompiler {
    var available = true; private set
    private val compiler = Shaderc.shaderc_compiler_initialize()

    fun compileGlslToSpv(
        source: String,
        kind: Int,
        fileName: String,
        entryPoint: String = "main",
        options: CompilerOptions? = null
    ): CompilationResult {
        checkAvailability()
        MemoryStack.stackPush().use { stack ->
            val sourceBuffer = stack.ASCII(source)
            val fileNameBuffer = stack.ASCII(fileName)
            val entryPointBuffer = stack.ASCII(entryPoint)
            val result = Shaderc.shaderc_compile_into_spv(
                compiler, sourceBuffer, kind, fileNameBuffer,
                entryPointBuffer, options?.options ?: 0)
            return CompilationResult(result)
        }
    }

    private fun checkAvailability() { require(available) }

    fun destroy() {
        Shaderc.shaderc_compiler_release(compiler)
        available = false
    }

    class CompilationResult(val handle: Long) {
        val errorMessage = Shaderc.shaderc_result_get_error_message(handle)
        val compilationStatus = Shaderc.shaderc_result_get_compilation_status(handle)
        val numWarnings = Shaderc.shaderc_result_get_num_warnings(handle)
        val numErrors = Shaderc.shaderc_result_get_num_errors(handle)
        val binaryLength = Shaderc.shaderc_result_get_length(handle)
        val binary by lazy { Shaderc.shaderc_result_get_bytes(handle) }
    }

    class CompilerOptions {
        var available = true; private set
        val options = Shaderc.shaderc_compile_options_initialize()
            get() {
                checkAvailability()
                return field
            }

        private fun checkAvailability() { require(available) }

        fun destroy() {
            Shaderc.shaderc_compile_options_release(options)
            available = false
        }
    }
}