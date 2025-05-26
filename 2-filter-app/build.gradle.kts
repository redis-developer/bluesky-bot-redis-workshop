plugins {
    java
    id("org.springframework.boot") version "3.4.5"
    id("io.spring.dependency-management") version "1.1.7"
}
val springAiVersion by extra("1.0.0-RC1")

group = "com.redis"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")

    // Redis OM Spring
    implementation("com.redis.om:redis-om-spring:1.0.0-RC2")
    implementation("com.redis.om:redis-om-spring-ai:1.0.0-RC2")
    annotationProcessor("com.redis.om:redis-om-spring:1.0.0-RC2")

    // DJL for machine learning
    implementation("ai.djl:api:0.33.0")
    implementation("ai.djl.huggingface:tokenizers:0.33.0")
    implementation("ai.djl.pytorch:pytorch-engine:0.33.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
dependencyManagement {
    imports {
        mavenBom("org.springframework.ai:spring-ai-bom:$springAiVersion")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
