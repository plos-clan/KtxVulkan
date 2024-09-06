package dev.ktxvulkan.graphics.utils

import org.lwjgl.vulkan.VK10.VK_SUCCESS

fun vkCheckResult(err: Int, errMsg: String) {
    if (err != VK_SUCCESS) {
        throw RuntimeException("$errMsg: $err")
    }
}