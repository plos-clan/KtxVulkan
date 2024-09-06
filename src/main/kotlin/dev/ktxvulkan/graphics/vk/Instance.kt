package dev.ktxvulkan.graphics.vk

import dev.ktxvulkan.graphics.utils.OSType
import dev.ktxvulkan.graphics.utils.getOS
import dev.ktxvulkan.graphics.utils.vkCheckResult
import io.github.oshai.kotlinlogging.KLoggable
import org.lwjgl.PointerBuffer
import org.lwjgl.glfw.GLFWVulkan
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.EXTDebugUtils.*
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO
import org.lwjgl.vulkan.VK10.vkCreateInstance
import org.lwjgl.vulkan.VK13.*


class Instance(validate: Boolean) : KLoggable {
    override val logger = logger()
    val MESSAGE_SEVERITY_BITMASK: Int = VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT or
            VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT
    val MESSAGE_TYPE_BITMASK: Int = VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT or
            VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT or
            VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT
    val PORTABILITY_EXTENSION: String = "VK_KHR_portability_enumeration"

    private val vkInstance: VkInstance
    private val debugUtils: VkDebugUtilsMessengerCreateInfoEXT
    private val vkDebugHandle: Long

    init {
        MemoryStack.stackPush().use { stack ->
            val appName = stack.UTF8("KtxVulkan")
            val vkApplicationInfo = VkApplicationInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
                .pApplicationName(appName)
                .applicationVersion(1)
                .pEngineName(appName)
                .engineVersion(0)
                .apiVersion(VK_API_VERSION_1_3)

            val validationLayers = getSupportedValidationLayers()
            val numValidationLayers = validationLayers.size
            var supportsValidation = validate
            if (validate && numValidationLayers == 0) {
                supportsValidation = false
                logger.warn("Request validation but no supported validation layers found. Falling back to no validation")
            }
            logger.debug("Validation: {}", supportsValidation)


            // Set required  layers
            val requiredLayers = stack.mallocPointer(numValidationLayers)
            if (supportsValidation) {
                for (i in 0 until numValidationLayers) {
                    logger.debug("Using validation layer [{}]", validationLayers[i])
                    requiredLayers.put(i, stack.ASCII(validationLayers[i]))
                }
            }

            val instanceExtensions = getInstanceExtensionProperties()

            // GLFW Extension
            val glfwExtensions = GLFWVulkan.glfwGetRequiredInstanceExtensions()
                ?: throw RuntimeException("Failed to find the GLFW platform surface extensions")

            val requiredExtensions: PointerBuffer

            val usePortability = instanceExtensions.contains(PORTABILITY_EXTENSION) && getOS() == OSType.MACOS
            if (supportsValidation) {
                val vkDebugUtilsExtension = stack.UTF8(VK_EXT_DEBUG_UTILS_EXTENSION_NAME)
                val numExtensions =
                    if (usePortability) glfwExtensions.remaining() + 2 else glfwExtensions.remaining() + 1
                requiredExtensions = stack.mallocPointer(numExtensions)
                requiredExtensions.put(glfwExtensions).put(vkDebugUtilsExtension)
                if (usePortability) {
                    requiredExtensions.put(stack.UTF8(PORTABILITY_EXTENSION))
                }
            } else {
                val numExtensions = if (usePortability) glfwExtensions.remaining() + 1 else glfwExtensions.remaining()
                requiredExtensions = stack.mallocPointer(numExtensions)
                requiredExtensions.put(glfwExtensions)
                if (usePortability) {
                    requiredExtensions.put(stack.UTF8(KHRPortabilitySubset.VK_KHR_PORTABILITY_SUBSET_EXTENSION_NAME))
                }
            }
            requiredExtensions.flip()

            var extension = MemoryUtil.NULL
            if (supportsValidation) {
                debugUtils = createDebugCallBack()
                extension = debugUtils.address()
            } else throw IllegalStateException("Does not support validation")

            // Create instance info
            val vkInstanceCreateInfo = VkInstanceCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                .pNext(extension)
                .pApplicationInfo(vkApplicationInfo)
                .ppEnabledLayerNames(requiredLayers)
                .ppEnabledExtensionNames(requiredExtensions)
            if (usePortability) {
                vkInstanceCreateInfo.flags(0x00000001) // VK_INSTANCE_CREATE_ENUMERATE_PORTABILITY_BIT_KHR
            }

            val pInstance = stack.mallocPointer(1)
            val vkCreateInstanceResult = vkCreateInstance(vkInstanceCreateInfo, null, pInstance)
            vkCheckResult(vkCreateInstanceResult, "Error while creating instance")
            vkInstance = VkInstance(pInstance[0], vkInstanceCreateInfo)

            val longBuff = stack.mallocLong(1)
            val vkCreateDebugUtilsMessengerEXTResult =
                vkCreateDebugUtilsMessengerEXT(vkInstance, debugUtils, null, longBuff)
            vkCheckResult(vkCreateDebugUtilsMessengerEXTResult, "Error while creating debug utils")
            vkDebugHandle = longBuff[0]
        }
    }

    private fun createDebugCallBack(): VkDebugUtilsMessengerCreateInfoEXT {
        return VkDebugUtilsMessengerCreateInfoEXT
            .calloc()
            .sType(VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT)
            .messageSeverity(MESSAGE_SEVERITY_BITMASK)
            .messageType(MESSAGE_TYPE_BITMASK)
            .pfnUserCallback { messageSeverity: Int, messageTypes: Int, pCallbackData: Long, pUserData: Long ->
                val callbackData = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData)
                if ((messageSeverity and VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT) != 0) {
                    logger.info("VkDebugUtilsCallback, {}", callbackData.pMessageString())
                } else if ((messageSeverity and VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT) != 0) {
                    logger.warn("VkDebugUtilsCallback, {}", callbackData.pMessageString())
                } else if ((messageSeverity and VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT) != 0) {
                    logger.error("VkDebugUtilsCallback, {}", callbackData.pMessageString())
                } else {
                    logger.debug("VkDebugUtilsCallback, {}", callbackData.pMessageString())
                }
                VK_FALSE
            }
    }

    private fun getInstanceExtensionProperties(): Set<String> = buildSet {
        MemoryStack.stackPush().use { stack ->
            val extensionCountBuf = IntArray(1)
            vkEnumerateInstanceExtensionProperties(null as CharSequence?, extensionCountBuf, null)
            val extensionCount = extensionCountBuf[0]
            val extensions = VkExtensionProperties.calloc(extensionCount, stack)
            vkEnumerateInstanceExtensionProperties(null as CharSequence?, extensionCountBuf, extensions)
            for (props in extensions) {
                val name = props.extensionNameString()
                add(name)
            }
        }
    }

    private fun getSupportedValidationLayers(): List<String> {
        MemoryStack.stackPush().use { stack ->
            val numLayersArr = stack.callocInt(1)
            vkEnumerateInstanceLayerProperties(numLayersArr, null)
            val numLayers = numLayersArr[0]
            logger.debug("Instance supports [{}] layers", numLayers)

            val propsBuf = VkLayerProperties.calloc(numLayers, stack)
            vkEnumerateInstanceLayerProperties(numLayersArr, propsBuf)
            val supportedLayers = mutableListOf<String>()
            for (i in 0..<numLayers) {
                val props = propsBuf[i]
                val layerName = props.layerNameString()
                supportedLayers.add(layerName)
                logger.debug("Supported layer [{}]", layerName)
            }

            val layersToUse = mutableListOf<String>()

            // Main validation layer
            if (supportedLayers.contains("VK_LAYER_KHRONOS_validation")) {
                layersToUse.add("VK_LAYER_KHRONOS_validation")
                return layersToUse
            }

            // Fallback 1
            if (supportedLayers.contains("VK_LAYER_LUNARG_standard_validation")) {
                layersToUse.add("VK_LAYER_LUNARG_standard_validation")
                return layersToUse
            }

            // Fallback 2 (set)
            val requestedLayers = mutableListOf<String>()
            requestedLayers.add("VK_LAYER_GOOGLE_threading")
            requestedLayers.add("VK_LAYER_LUNARG_parameter_validation")
            requestedLayers.add("VK_LAYER_LUNARG_object_tracker")
            requestedLayers.add("VK_LAYER_LUNARG_core_validation")
            requestedLayers.add("VK_LAYER_GOOGLE_unique_objects")
            return requestedLayers.stream().filter { it in supportedLayers }.toList()
        }
    }

    fun destroy() {
        vkDestroyInstance(vkInstance, null)
    }
}