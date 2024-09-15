package dev.ktxvulkan.utils.math

import org.lwjgl.vulkan.VkClearColorValue

data class Vec4f(val x: Float, val y: Float, val z: Float, val w: Float) {
    fun x(new: Float) = Vec4f(new, y, z, w)
    fun y(new: Float) = Vec4f(x, new, z, w)
    fun z(new: Float) = Vec4f(x, y, new, w)
    fun w(new: Float) = Vec4f(x, y, z, new)

    fun apply(dst: VkClearColorValue) {
        dst.float32(0, x).float32(1, y).float32(2, z).float32(3, w)
    }

    operator fun plus(other: Vec4f) = Vec4f(x + other.x, y + other.y, z + other.z, w + other.w)
    operator fun minus(other: Vec4f) = Vec4f(x - other.x, y - other.y, z - other.z, w - other.w)
    operator fun times(other: Vec4f) = Vec4f(x * other.x, y * other.y, z * other.z, w * other.w)
    operator fun div(other: Vec4f) = Vec4f(x / other.x, y / other.y, z / other.z, w / other.w)
    operator fun unaryMinus() = Vec4f(-x, -y, -z, -w)
    operator fun unaryPlus() = this
}