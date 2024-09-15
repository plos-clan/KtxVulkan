package dev.ktxvulkan.structs

import dev.luna5ama.kmogus.struct.Struct

@Struct
interface Vertex {
    val pos: Vec3f32
    val color: Vec3f32
    val texCoord: Vec2f32
    val normal: Vec3f32
}