pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    // Позволяет Gradle самому подтянуть JDK 17, если его нет на машине/CI.
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

// Влияет только на имя артефакта. Идентичность плагина на Marketplace задаёт
// <id> в plugin.xml, его менять нельзя.
rootProject.name = "idea-commit-prefixer"
