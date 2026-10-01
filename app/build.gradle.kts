plugins {
    id("com.android.application")
}

val gdxVersion = "1.14.2"

// libGDX ships its native libraries as classifier jars; they are unpacked into jniLibs below.
val natives: Configuration by configurations.creating

android {
    namespace = "com.cacao.libgdxgame"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.cacao.libgdxgame"
        minSdk = 21
        targetSdk = 36
        versionCode = 4
        versionName = "1.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Signed with the debug key so the release APK installs out of the box.
            // Replace with your own signingConfig before publishing.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    sourceSets {
        getByName("main") {
            jniLibs.directories.add(layout.buildDirectory.dir("gdx-natives").get().asFile.path)
        }
    }
}

dependencies {
    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-backend-android:$gdxVersion")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-armeabi-v7a")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-arm64-v8a")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86_64")
}

val copyAndroidNatives by tasks.registering(Sync::class) {
    val outDir = layout.buildDirectory.dir("gdx-natives")
    into(outDir)
    natives.resolvedConfiguration.resolvedArtifacts.forEach { artifact ->
        val abi = artifact.classifier!!.removePrefix("natives-")
        from(zipTree(artifact.file)) {
            include("*.so")
            into(abi)
        }
    }
}

tasks.named("preBuild") { dependsOn(copyAndroidNatives) }
