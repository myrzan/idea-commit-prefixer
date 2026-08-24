import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    // IntelliJ Platform Gradle Plugin 2.x. Версия 1.x (org.jetbrains.intellij)
    // объявлена deprecated и не собирает под платформы 2024.3+.
    id("org.jetbrains.intellij.platform") version "2.18.1"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

java {
    toolchain {
        // 2023.3 работает на JBR 17; байткод 17 читают и все новые IDE.
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    // Тестовый раннер платформы стартует через JUnit 4, даже если сами тесты
    // написаны на Jupiter.
    testRuntimeOnly("junit:junit:4.13.2")

    intellijPlatform {
        create(
            providers.gradleProperty("platformType"),
            providers.gradleProperty("platformVersion"),
        )
        bundledPlugins(
            providers.gradleProperty("platformBundledPlugins").map { it.split(',').map(String::trim) },
        )

        pluginVerifier()
        zipSigner()
        testFramework(TestFrameworkType.JUnit5)
    }
}

intellijPlatform {
    pluginConfiguration {
        version = providers.gradleProperty("pluginVersion")

        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            // Пусто => совместимость со всеми будущими сборками IDE.
            untilBuild = provider { null }
        }
    }

    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }

    // ./gradlew verifyPlugin — прогон IntelliJ Plugin Verifier.
    // Точечно: ./gradlew verifyPlugin -PverifierIdes=PS-2026.2.1
    pluginVerification {
        ides {
            create(
                providers.gradleProperty("verifierIdes").map { it.split(',').map(String::trim) },
            )
        }
    }
}

tasks {
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release = 17
    }

    test {
        useJUnitPlatform()
    }

    wrapper {
        gradleVersion = "9.7.1"
    }
}
