plugins {
    id("org.jetbrains.intellij.platform") version "2.3.0"
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
}

group = "com.zinki"
version = "1.1.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        goland("2024.3")
        bundledPlugin("org.jetbrains.plugins.terminal")
    }
}

intellijPlatform {
    pluginConfiguration {
        name = "Zinki for Claude"
        version = "1.1.0"
        ideaVersion {
            sinceBuild = "243"
        }
    }
    instrumentCode = false
}

kotlin {
    jvmToolchain(21)
}
