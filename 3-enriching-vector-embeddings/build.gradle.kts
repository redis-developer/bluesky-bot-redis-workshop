plugins {
    java
    id("org.springframework.boot") version "3.4.5"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.redis"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")

    implementation("org.springframework.ai:spring-ai-transformers:1.0.0-M6")
    implementation("org.springframework.ai:spring-ai-openai:1.0.0-M6")
    implementation("org.springframework.ai:spring-ai-bedrock:1.0.0-M6")
    implementation("org.springframework.ai:spring-ai-ollama:1.0.0-M6")
    implementation("org.springframework.ai:spring-ai-azure-openai:1.0.0-M6")
    implementation("org.springframework.ai:spring-ai-vertex-ai-embedding:1.0.0-M6")

    // Redis OM Spring
    implementation("com.redis.om:redis-om-spring:1.0.0-RC2")
    implementation("com.redis.om:redis-om-spring-ai:1.0.0-RC2")
    annotationProcessor("com.redis.om:redis-om-spring:1.0.0-RC2")

    // DJL for machine learning
    implementation("ai.djl:api:0.33.0")
    implementation("ai.djl.huggingface:tokenizers:0.33.0")
    implementation("ai.djl.pytorch:pytorch-engine:0.33.0")
    implementation("ai.djl.spring:djl-spring-boot-starter-autoconfigure:0.26")
    implementation("ai.djl.spring:djl-spring-boot-starter-pytorch-auto:0.26")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
