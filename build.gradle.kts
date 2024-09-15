import org.gradle.internal.os.OperatingSystem

plugins {
	kotlin("jvm") version "2.0.20"

	id("com.google.devtools.ksp")
	id("dev.luna5ama.kmogus-struct-plugin") apply false

	application
}

allprojects {
	repositories {
		mavenCentral()
		maven("https://maven.luna5ama.dev/")
		maven("https://jitpack.io/")
	}
}

val lwjglVersion = "3.3.4"

val lwjglNatives = Pair(
	System.getProperty("os.name")!!,
	System.getProperty("os.arch")!!
).let { (name, arch) ->
	when {
		arrayOf("Linux", "SunOS", "Unit").any { name.startsWith(it) } ->
			if (arrayOf("arm", "aarch64").any { arch.startsWith(it) })
				"natives-linux${if (arch.contains("64") || arch.startsWith("armv8")) "-arm64" else "-arm32"}"
			else if (arch.startsWith("ppc"))
				"natives-linux-ppc64le"
			else if (arch.startsWith("riscv"))
				"natives-linux-riscv64"
			else
				"natives-linux"
		arrayOf("Mac OS X", "Darwin").any { name.startsWith(it) }     ->
			"natives-macos${if (arch.startsWith("aarch64")) "-arm64" else ""}"
		arrayOf("Windows").any { name.startsWith(it) }                ->
			if (arch.contains("64"))
				"natives-windows${if (arch.startsWith("aarch64")) "-arm64" else ""}"
			else
				"natives-windows-x86"
		else                                                                            ->
			throw Error("Unrecognized or unsupported platform. Please set \"lwjglNatives\" manually")
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("io.github.oshai:kotlin-logging-jvm:4.0.0")
	implementation("org.apache.logging.log4j:log4j-api:2.23.1")
	implementation("org.apache.logging.log4j:log4j-core:2.23.1")
	implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.23.1")
	implementation("net.java.dev.jna:jna:5.14.0")
	implementation("dev.luna5ama:kmogus-struct-api:1.0-SNAPSHOT")
	implementation(project(":structs"))

	implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))

	implementation("org.lwjgl", "lwjgl")
	implementation("org.lwjgl", "lwjgl-assimp")
	implementation("org.lwjgl", "lwjgl-freetype")
	implementation("org.lwjgl", "lwjgl-glfw")
	implementation("org.lwjgl", "lwjgl-jemalloc")
	implementation("org.lwjgl", "lwjgl-openal")
	implementation("org.lwjgl", "lwjgl-stb")
	implementation("org.lwjgl", "lwjgl-vma")
	implementation("org.lwjgl", "lwjgl-shaderc")
	implementation("org.lwjgl", "lwjgl-vulkan")
	runtimeOnly("org.lwjgl", "lwjgl", classifier = lwjglNatives)
	runtimeOnly("org.lwjgl", "lwjgl-assimp", classifier = lwjglNatives)
	runtimeOnly("org.lwjgl", "lwjgl-freetype", classifier = lwjglNatives)
	runtimeOnly("org.lwjgl", "lwjgl-glfw", classifier = lwjglNatives)
	runtimeOnly("org.lwjgl", "lwjgl-jemalloc", classifier = lwjglNatives)
	runtimeOnly("org.lwjgl", "lwjgl-openal", classifier = lwjglNatives)
	runtimeOnly("org.lwjgl", "lwjgl-stb", classifier = lwjglNatives)
	runtimeOnly("org.lwjgl", "lwjgl-vma", classifier = lwjglNatives)
	runtimeOnly("org.lwjgl", "lwjgl-shaderc", classifier = lwjglNatives)
	if (lwjglNatives == "natives-macos" || lwjglNatives == "natives-macos-arm64") runtimeOnly("org.lwjgl", "lwjgl-vulkan", classifier = lwjglNatives)
}

application {
	mainClass = "dev.ktxvulkan.Application"
}

kotlin {
	compilerOptions {
		freeCompilerArgs = listOf("-Xcontext-receivers")
	}
}