package dev.ktxvulkan.graphics.vk

data class QueueFamilyIndices(var graphicsFamily: Int = -1, var presentFamily: Int = -1) {
    fun isComplete() = graphicsFamily != -1 && presentFamily != -1
}