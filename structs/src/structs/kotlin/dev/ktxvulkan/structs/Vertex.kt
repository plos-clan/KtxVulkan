package dev.ktxvulkan.structs

import dev.luna5ama.kmogus.struct.Struct

@Struct
interface Vertex {
    val pos: Vec3f32
    val color: Vec4f32
}