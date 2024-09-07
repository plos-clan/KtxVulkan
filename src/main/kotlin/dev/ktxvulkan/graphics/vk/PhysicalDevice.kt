package dev.ktxvulkan.graphics.vk

import dev.ktxvulkan.graphics.Window
import io.github.oshai.kotlinlogging.KLoggable
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.EXTDescriptorIndexing.VK_EXT_DESCRIPTOR_INDEXING_EXTENSION_NAME
import org.lwjgl.vulkan.KHRBufferDeviceAddress.VK_KHR_BUFFER_DEVICE_ADDRESS_EXTENSION_NAME
import org.lwjgl.vulkan.KHRDeferredHostOperations.VK_KHR_DEFERRED_HOST_OPERATIONS_EXTENSION_NAME
import org.lwjgl.vulkan.KHRPipelineLibrary.VK_KHR_PIPELINE_LIBRARY_EXTENSION_NAME
import org.lwjgl.vulkan.KHRShaderFloatControls.VK_KHR_SHADER_FLOAT_CONTROLS_EXTENSION_NAME
import org.lwjgl.vulkan.KHRSpirv14.VK_KHR_SPIRV_1_4_EXTENSION_NAME
import org.lwjgl.vulkan.KHRSurface.*
import org.lwjgl.vulkan.KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME
import org.lwjgl.vulkan.VK10.*


class PhysicalDevice(val instance: Instance, private val window: Window) : KLoggable {
    override val logger = logger()
    val swapchainSupport: SwapchainSupportDetails
    val queueFamilyIndices: QueueFamilyIndices
    val vkPhysicalDevice: VkPhysicalDevice
    private val vkPhysicalDeviceProperties: VkPhysicalDeviceProperties
    private val vkPhysicalDeviceMemoryProperties: VkPhysicalDeviceMemoryProperties
    val vkQueueFamilyProperties: VkQueueFamilyProperties.Buffer
    private val msaaSamples: Int

    init {
        MemoryStack.stackPush().use { stack ->
            val deviceCountBuf = stack.callocInt(1)
            val deviceCount = deviceCountBuf.let {
                vkEnumeratePhysicalDevices(instance.vkInstance, it, null)
                it[0]
            }
            if (deviceCount == 0) {
                throw RuntimeException("failed to find GPUs with Vulkan support!")
            }
            val devicesBuf = stack.callocPointer(deviceCount)
            vkEnumeratePhysicalDevices(instance.vkInstance, deviceCountBuf, devicesBuf)
            val devices = buildList {
                for (i in 0..<deviceCountBuf[0]) {
                    val temp = VkPhysicalDevice(devicesBuf[i], instance.vkInstance)
                    add(temp)
                }
            }
            val candidates = mutableMapOf<Int, VkPhysicalDevice>()
            for (device in devices) {
                val score = rateDeviceSuitability(device)
                candidates[score] = device
            }

            val (score, maxSuitablePhysicalDevice) = candidates.maxBy { it.key }
            if (score > 0) vkPhysicalDevice = maxSuitablePhysicalDevice
            else throw RuntimeException("failed to find a suitable GPU!")

            swapchainSupport = querySwapChainSupport(vkPhysicalDevice)
            queueFamilyIndices = findQueueFamilies(vkPhysicalDevice)

            vkPhysicalDeviceProperties = VkPhysicalDeviceProperties.calloc()
            vkGetPhysicalDeviceProperties(vkPhysicalDevice, vkPhysicalDeviceProperties)
            logger.debug("{} was chosen", vkPhysicalDeviceProperties.deviceNameString())

            vkPhysicalDeviceMemoryProperties = VkPhysicalDeviceMemoryProperties.calloc()
            vkGetPhysicalDeviceMemoryProperties(vkPhysicalDevice, vkPhysicalDeviceMemoryProperties)

            val queueFamilyPropertiesCount = IntArray(1)
            vkGetPhysicalDeviceQueueFamilyProperties(vkPhysicalDevice, queueFamilyPropertiesCount, null)
            vkQueueFamilyProperties = VkQueueFamilyProperties.calloc(queueFamilyPropertiesCount[0])
            vkGetPhysicalDeviceQueueFamilyProperties(vkPhysicalDevice, queueFamilyPropertiesCount, vkQueueFamilyProperties)

            msaaSamples = getMaxUsableMSAASamples()
        }
    }

    fun destroy() {
        vkPhysicalDeviceProperties.free()
        vkPhysicalDeviceMemoryProperties.free()
    }

    private fun getMaxUsableMSAASamples(): Int {
        val counts = vkPhysicalDeviceProperties.limits().framebufferColorSampleCounts() and
                vkPhysicalDeviceProperties.limits().framebufferDepthSampleCounts() and
                vkPhysicalDeviceProperties.limits().framebufferStencilSampleCounts()
        if (counts and VK_SAMPLE_COUNT_64_BIT != 0) return VK_SAMPLE_COUNT_64_BIT

        if (counts and VK_SAMPLE_COUNT_32_BIT != 0) return VK_SAMPLE_COUNT_32_BIT
        if (counts and VK_SAMPLE_COUNT_16_BIT != 0) return VK_SAMPLE_COUNT_16_BIT
        if (counts and VK_SAMPLE_COUNT_8_BIT != 0) return VK_SAMPLE_COUNT_8_BIT
        if (counts and VK_SAMPLE_COUNT_4_BIT != 0) return VK_SAMPLE_COUNT_4_BIT
        if (counts and VK_SAMPLE_COUNT_2_BIT != 0) return VK_SAMPLE_COUNT_2_BIT

        return VK_SAMPLE_COUNT_1_BIT;
    }

    private fun rateDeviceSuitability(device: VkPhysicalDevice): Int {
        val deviceProperties = VkPhysicalDeviceProperties.calloc()
        vkGetPhysicalDeviceProperties(device, deviceProperties)
        val deviceFeatures = VkPhysicalDeviceFeatures.calloc()
        vkGetPhysicalDeviceFeatures(device, deviceFeatures)
        var score = 0

        // Discrete GPUs have a significant performance advantage
        if (deviceProperties.deviceType() == VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU) score += 1000
        // Maximum possible size of textures affects graphics quality
        score += deviceProperties.limits().maxImageDimension2D()
        if (!isDeviceSuitable(device, deviceProperties, deviceFeatures))
            score = 0

        logger.debug("\t score: {}", score)

        val indices = findQueueFamilies(device)
        if (!indices.isComplete()) {
            return 0
        }

        return score
    }

    private fun checkDeviceExtensionSupport(device: VkPhysicalDevice): Boolean {
        val requiredExtensions = deviceExtensions.toMutableList()
        MemoryStack.stackPush().use { stack ->
            val extensionCountBuf = IntArray(1)
            vkEnumerateDeviceExtensionProperties(device, null as CharSequence?, extensionCountBuf, null)
            val extensionCount = extensionCountBuf[0]

            val availableExtensionsBuf = VkExtensionProperties.calloc(extensionCount)
            vkEnumerateDeviceExtensionProperties(device, null as CharSequence?, extensionCountBuf, availableExtensionsBuf)
            val availableExtensions = buildList {
                for (i in 0..<extensionCount) {
                    add(availableExtensionsBuf[i])
                }
            }

            logger.debug("\t {} device extensions supported:", extensionCount)

            for ( extension in availableExtensions) {
                val specVersion = extension.specVersion()
                logger.trace(
                    "\t\t {} - {}.{}.{}",
                    extension.extensionNameString(),
                    VK_API_VERSION_MAJOR(specVersion),
                    VK_API_VERSION_MINOR(specVersion),
                    VK_API_VERSION_PATCH(specVersion)
                )
                requiredExtensions.remove(extension.extensionNameString())
            }
        }

        return requiredExtensions.isEmpty()
    }

    private fun isDeviceSuitable(
        device: VkPhysicalDevice,
        deviceProperties: VkPhysicalDeviceProperties,
        deviceFeatures: VkPhysicalDeviceFeatures
    ): Boolean {
        logger.debug("{} type:{}", deviceProperties.deviceNameString(), deviceProperties.deviceType())

        val extensionsSupported = checkDeviceExtensionSupport(device)
        if (!extensionsSupported) {
            logger.debug("\t\t Device does not satisfy extension requirement")
        }

        var swapChainAdequate = false
        if (extensionsSupported) {
            val swapChainSupport = querySwapChainSupport(device)
            swapChainAdequate = swapChainSupport.formats.isNotEmpty() && swapChainSupport.presentModes.isNotEmpty()
        }

        return deviceProperties.deviceType() == VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU &&
                deviceFeatures.geometryShader() && deviceFeatures.samplerAnisotropy() && extensionsSupported && swapChainAdequate
    }

    private fun querySwapChainSupport(device: VkPhysicalDevice): SwapchainSupportDetails {
        val capabilities = VkSurfaceCapabilitiesKHR.calloc()
        vkGetPhysicalDeviceSurfaceCapabilitiesKHR(device, window.surface, capabilities)

        val intBuffer = IntArray(1)

        val formatCount = intBuffer.let {
            vkGetPhysicalDeviceSurfaceFormatsKHR(device, window.surface, it, null)
            it[0]
        }

        val formats = if (formatCount != 0) {
            buildList {
                val vkSurfaceFormatKHRs = VkSurfaceFormatKHR.calloc(formatCount)
                vkGetPhysicalDeviceSurfaceFormatsKHR(device, window.surface, intBuffer, vkSurfaceFormatKHRs)
                for (i in 0..<formatCount) add(vkSurfaceFormatKHRs[i])
            }
        } else emptyList()

        val presentModeCount = intBuffer.let {
            vkGetPhysicalDeviceSurfacePresentModesKHR(device, window.surface, it, null)
            it[0]
        }
        val presentModes = if (presentModeCount != 0) {
            buildList {
                val vkSurfacePresentModeKHR = IntArray(presentModeCount)
                vkGetPhysicalDeviceSurfacePresentModesKHR(device, window.surface, intBuffer, vkSurfacePresentModeKHR)
                for (i in 0..<presentModeCount) add(vkSurfacePresentModeKHR[i])
            }
        } else emptyList()

        return SwapchainSupportDetails(capabilities, formats, presentModes)
    }

    private fun findQueueFamilies(device: VkPhysicalDevice): QueueFamilyIndices {
        val indices = QueueFamilyIndices()
        MemoryStack.stackPush().use { stack ->
            val intBuf = IntArray(1)
            val queueFamilyCount = intBuf.let {
                vkGetPhysicalDeviceQueueFamilyProperties(device, it, null)
                it[0]
            }
            logger.trace("\t Device has {} queue families", queueFamilyCount)
            val vkQueueFamilyPropertiesBuf = VkQueueFamilyProperties.calloc(queueFamilyCount, stack)
            vkGetPhysicalDeviceQueueFamilyProperties(device, intBuf, vkQueueFamilyPropertiesBuf)
            val vkQueueFamilyProperties = buildList {
                for (i in 0..<queueFamilyCount) add(vkQueueFamilyPropertiesBuf[i])
            }
            var i = 0
            for (queueFamily in vkQueueFamilyProperties) {
                if (queueFamily.queueFlags() and VK_QUEUE_GRAPHICS_BIT != 0
                    && indices.graphicsFamily == -1) indices.graphicsFamily = i

                val presentSupport = IntArray(1).let {
                    vkGetPhysicalDeviceSurfaceSupportKHR(device, i, window.surface, it)
                    it[0] != 0
                }
                if (presentSupport && i != indices.graphicsFamily) {
                    indices.presentFamily = i
                }
                if (indices.isComplete()) break

                i++
            }
        }

        
        return indices
    }

    data class SwapchainSupportDetails(
        val capabilities: VkSurfaceCapabilitiesKHR,
        val formats: List<VkSurfaceFormatKHR>,
        val presentModes: List<Int>
    )

    data class QueueFamilyIndices(var graphicsFamily: Int = -1, var presentFamily: Int = -1) {
        fun isComplete() = graphicsFamily != -1 && presentFamily != -1
    }

    companion object {
        val deviceExtensions = listOf(
            VK_KHR_SWAPCHAIN_EXTENSION_NAME,
            VK_EXT_DESCRIPTOR_INDEXING_EXTENSION_NAME,
            VK_KHR_BUFFER_DEVICE_ADDRESS_EXTENSION_NAME,
//            VK_KHR_ACCELERATION_STRUCTURE_EXTENSION_NAME,
//            VK_KHR_RAY_TRACING_PIPELINE_EXTENSION_NAME,
//            VK_KHR_RAY_QUERY_EXTENSION_NAME,
            VK_KHR_PIPELINE_LIBRARY_EXTENSION_NAME,
            VK_KHR_DEFERRED_HOST_OPERATIONS_EXTENSION_NAME,
            VK_KHR_SPIRV_1_4_EXTENSION_NAME,
            VK_KHR_SHADER_FLOAT_CONTROLS_EXTENSION_NAME
        )
    }
}