plugins {
    alias(libs.plugins.springBoot)
    alias(libs.plugins.springDependencyManagement)
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSpring)
}

springBoot {
    mainClass.set("com.ainsoft.ai.AinsoftAiApplicationKt")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_24)
    }
}

group = "com.ainsoft.ai"
version = "0.0.1-SNAPSHOT"
description = "Ainsoft AI Application"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(24))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform(libs.spring.ai.bom))
    implementation(libs.spring.boot.starter.web)

    implementation("org.springframework.ai:spring-ai-client-chat")
    implementation("org.springframework.ai:spring-ai-ollama")
    implementation("org.springframework.ai:spring-ai-starter-model-ollama")
    implementation(libs.spring.ai.starter.model.openai)
    implementation("org.springframework.ai:spring-ai-starter-model-chat-memory")
    implementation("io.github.givimad:piper-jni:1.2.0-c0670df")
    implementation(libs.spring.ai.advisors.vector.store)
    implementation(libs.spring.ai.rag)
    implementation(libs.spring.ai.vector.store)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.springdoc.openapi.starter.webmvc.ui)
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlin.stdlib.jdk8)
    annotationProcessor(libs.spring.boot.configuration.processor)

    testImplementation(libs.spring.boot.starter.test)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
