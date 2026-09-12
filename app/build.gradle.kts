import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.File
import java.util.Calendar
import javax.inject.Inject

interface InjectedExecOps {
    @get:Inject
    val execOperations: ExecOperations
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "io.github.rhythmcache.dioxamine"
    compileSdk = 37

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("KEYSTORE_PATH")
            if (keystorePath != null) {
                storeFile = rootProject.file(keystorePath)
                storePassword = System.getenv("RELEASE_STORE_PASSWORD")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
            }
        }
    }

    defaultConfig {
        applicationId = "io.github.rhythmcache.dioxamine"
        minSdk = 24
        targetSdk = 36
        versionCode = 10004
        versionName = "0.1.0-fluxdevx"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }

        val currentYear = Calendar.getInstance().get(Calendar.YEAR).toString()

        buildConfigField("String", "APP_NAME", "\"FluxDevX\"")
        buildConfigField("String", "AUTHOR", "\"Alexdev01-spectrum\"")
        buildConfigField("String", "COPYRIGHT_YEAR", "\"$currentYear\"")
        buildConfigField("String", "GITHUB_URL", "\"https://github.com/Alexdev01-spectrum/FluxDevX\"")
        buildConfigField("String", "TELEGRAM_URL", "\"https://t.me/tr1ple_fault\"")
        buildConfigField("String", "SOURCE_CODE_URL", "\"https://github.com/Alexdev01-spectrum/FluxDevX\"")
        buildConfigField("String", "DOCUMENTATION_URL", "\"https://github.com/Alexdev01-spectrum/FluxDevX#readme\"")
        buildConfigField("String", "TERMINAL_PLUGIN_URL", "\"https://github.com/rhythmcache/Terminal\"")
        buildConfigField("String", "PLUGIN_DOCS_URL", "\"https://rhythmcache.github.io/Dioxamine/book/plugins/overview.html\"")
        buildConfigField("String", "TRANSLATION_URL", "\"https://github.com/Alexdev01-spectrum/FluxDevX#readme\"")
    }

    packaging {
        resources {
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/NOTICE.md"
            excludes += "META-INF/NOTICE.txt"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/*.kotlin_module"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }

        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.webkit)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.adb.kt)
    implementation(libs.fastboot.kt)
    implementation(libs.qrose)

    testImplementation(libs.junit)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

val androidComponents =
    extensions.getByType<ApplicationAndroidComponentsExtension>()

val scrcpyDir = rootProject.file("scrcpy")
val assetsDir = layout.projectDirectory.dir("src/main/assets")

fun isWindows(): Boolean = System.getProperty("os.name").lowercase().contains("win")

fun findAndroidJar(sdkDir: File, compileSdk: Int?): File {
    val sdk = compileSdk ?: error("compileSdk is not set")
    val platformsDir = sdkDir.resolve("platforms")
    val candidates = platformsDir.listFiles()?.asSequence()?.filter { file ->
        file.isDirectory && file.name.matches(Regex("""android-$sdk(\..*)?""")) && file.resolve("android.jar").isFile
    }?.sortedWith(compareBy<File> { if (it.name == "android-$sdk") 0 else 1 }.thenBy { it.name })?.toList().orEmpty()
    return candidates.firstOrNull()?.resolve("android.jar") ?: error("Android platform $sdk with android.jar not found in $platformsDir")
}

val buildScrcpyServer = tasks.register<GradleBuild>("buildScrcpyServer") {
    onlyIf { scrcpyDir.exists() }
    dir = scrcpyDir
    tasks = listOf("server:assembleRelease")
    doLast {
        val built = scrcpyDir.resolve("server/build/outputs/apk/release/server-release-unsigned.apk")
        if (!built.exists()) error("Expected built APK not found at $built")
        val dest = assetsDir.file("scrcpy-server.jar").asFile
        dest.parentFile.mkdirs()
        built.copyTo(dest, overwrite = true)
        logger.lifecycle("scrcpy-server.jar -> $dest")
    }
}

val dioxAgentStubClassesDir = layout.buildDirectory.dir("dioxagent-stub-classes")
val dioxAgentClassesDir = layout.buildDirectory.dir("dioxagent-classes")
val dioxAgentDexDir = layout.buildDirectory.dir("dioxagent-dex")

val compileDioxAgentStub = tasks.register<JavaExec>("compileDioxAgentStub") {
    mainClass.set("com.sun.tools.javac.Main")
    classpath = files(org.gradle.internal.jvm.Jvm.current().toolsJar ?: files())
    doFirst {
        val classesDir = dioxAgentStubClassesDir.get().asFile.apply { mkdirs() }
        val sdkDir = androidComponents.sdkComponents.sdkDirectory.get().asFile
        val androidJar = findAndroidJar(sdkDir, android.compileSdk)
        val stubRoot = rootProject.file("src_ext/stub")
        val stubSources = stubRoot.walkTopDown().filter { it.isFile && it.extension == "java" }.map { it.absolutePath }.toList()
        if (stubSources.isEmpty()) error("No stub sources found under $stubRoot")
        args = listOf("--release", "17", "-cp", androidJar.absolutePath, "-d", classesDir.absolutePath) + stubSources
    }
}

val jarDioxAgentStub = tasks.register<Exec>("jarDioxAgentStub") {
    dependsOn(compileDioxAgentStub)
    val stubJarFile = layout.buildDirectory.file("dioxagent-stub.jar")
    doFirst {
        val classesDir = dioxAgentStubClassesDir.get().asFile
        val javaHome = File(System.getProperty("java.home"))
        val jarExe = javaHome.resolve("bin").resolve(if (isWindows()) "jar.exe" else "jar")
        if (!jarExe.exists()) error("jar executable not found at $jarExe")
        val out = stubJarFile.get().asFile
        out.parentFile.mkdirs()
        commandLine(jarExe.absolutePath, "cf", out.absolutePath, "-C", classesDir.absolutePath, ".")
    }
}

val compileDioxAgentJava = tasks.register<JavaExec>("compileDioxAgentJava") {
    dependsOn(jarDioxAgentStub)
    mainClass.set("com.sun.tools.javac.Main")
    classpath = files(org.gradle.internal.jvm.Jvm.current().toolsJar ?: files())
    doFirst {
        val classesDir = dioxAgentClassesDir.get().asFile.apply { mkdirs() }
        val sdkDir = androidComponents.sdkComponents.sdkDirectory.get().asFile
        val androidJar = findAndroidJar(sdkDir, android.compileSdk)
        val stubJar = layout.buildDirectory.file("dioxagent-stub.jar").get().asFile
        val src = rootProject.file("src_ext/DioxAgent.java")
        args = listOf("--release", "17", "-cp", "${androidJar.absolutePath}${File.pathSeparator}${stubJar.absolutePath}", "-d", classesDir.absolutePath, src.absolutePath)
    }
}

val dexDioxAgent = tasks.register<Exec>("dexDioxAgent") {
    dependsOn(compileDioxAgentJava)
    doFirst {
        val classesDir = dioxAgentClassesDir.get().asFile
        val dexOutDir = dioxAgentDexDir.get().asFile.apply { mkdirs() }
        val sdkDir = androidComponents.sdkComponents.sdkDirectory.get().asFile
        val d8Name = if (isWindows()) "d8.bat" else "d8"
        val d8 = sdkDir.resolve("build-tools").listFiles()?.filter { it.isDirectory }?.sortedDescending()?.map { it.resolve(d8Name) }?.firstOrNull { it.exists() } ?: error("d8 not found under $sdkDir/build-tools")
        val classFiles = classesDir.walkTopDown().filter { it.isFile && it.extension == "class" }.map { it.absolutePath }.toList()
        val androidJar = findAndroidJar(sdkDir, android.compileSdk)
        commandLine(listOf(d8.absolutePath, "--output", dexOutDir.absolutePath, "--min-api", "21", "--lib", androidJar.absolutePath) + classFiles)
    }
    doLast {
        val dexOut = dioxAgentDexDir.get().file("classes.dex").asFile
        val dest = assetsDir.file("diox-agent.jar").asFile
        dest.parentFile.mkdirs()
        dexOut.copyTo(dest, overwrite = true)
        assetsDir.file("pkg-dump.jar").asFile.delete()
        logger.lifecycle("diox-agent.jar -> $dest")
    }
}

val buildDioxAgentJar = tasks.register("buildDioxAgentJar") { dependsOn(dexDioxAgent) }

val buildDxlsNative = tasks.register("buildDxlsNative") {
    val execOps = project.objects.newInstance<InjectedExecOps>().execOperations
    doLast {
        val ndkPath = android.ndkPath
        val sdkDir = androidComponents.sdkComponents.sdkDirectory.get().asFile
        val osName = System.getProperty("os.name").lowercase()
        val osPrefix = when {
            osName.contains("win") -> "windows"
            osName.contains("mac") || osName.contains("darwin") -> "darwin"
            else -> "linux"
        }
        fun ndkFromSdk(): File? {
            val ndkParent = sdkDir.resolve("ndk")
            return ndkParent.listFiles()?.filter { it.isDirectory }?.sortedByDescending { it.name }?.firstOrNull()
        }
        val ndkRoot = ndkPath?.let(::File)?.takeIf { it.exists() } ?: ndkFromSdk() ?: error("Could not locate the Android NDK. Install NDK (Side by side) from SDK Manager.")
        val clangBin = ndkRoot.resolve("toolchains/llvm/prebuilt/$osPrefix-x86_64/bin")
        val cFile = rootProject.file("src_ext/dxls.c")
        val outDir = assetsDir.asFile
        outDir.mkdirs()
        data class Abi(val name: String, val triple: String)
        val abis = listOf(Abi("arm64-v8a", "aarch64-linux-android21"), Abi("armeabi-v7a", "armv7a-linux-androideabi21"), Abi("x86", "i686-linux-android21"), Abi("x86_64", "x86_64-linux-android21"))
        abis.forEach { abi ->
            val clang = clangBin.resolve(if (isWindows()) "clang.exe" else "clang")
            val output = outDir.resolve("dxls-${abi.name}")
            execOps.exec { spec -> spec.executable = clang.absolutePath; spec.args = listOf("--target=${abi.triple}", "-O2", "-fPIE", "-pie", cFile.absolutePath, "-o", output.absolutePath) }
            logger.lifecycle("${abi.name} dxls built")
        }
    }
}

tasks.named("preBuild") { dependsOn(buildScrcpyServer, buildDioxAgentJar, buildDxlsNative) }
