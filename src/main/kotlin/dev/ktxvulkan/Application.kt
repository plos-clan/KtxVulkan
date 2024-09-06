package dev.ktxvulkan

object Application {
    @JvmStatic
    fun main(args: Array<String>) {
        println("hello world!")
        RenderEngine.initialize()
        RenderEngine.run()
        RenderEngine.cleanup()
    }
}