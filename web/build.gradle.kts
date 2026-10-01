import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
}

val composeVersion = "1.12.1"

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName.set("memorium")
        browser {
            commonWebpackConfig {
                outputFileName = "memorium.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        all { languageSettings.optIn("kotlin.js.ExperimentalWasmJsInterop") }
        wasmJsMain {
            // The game assets (textures, font, sounds) are shared with the Android app and served as-is.
            resources.srcDir(rootProject.file("app/src/main/assets"))
            dependencies {
                implementation("org.jetbrains.compose.runtime:runtime:$composeVersion")
                implementation("org.jetbrains.compose.animation:animation:$composeVersion")
                implementation("org.jetbrains.compose.foundation:foundation:$composeVersion")
                implementation("org.jetbrains.compose.ui:ui:$composeVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-browser:0.5.0")
            }
        }
    }
}
