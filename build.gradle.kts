plugins {
    id("java")
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

group = "ru.lewis.leykabot"
version = "1.0-SNAPSHOT"

repositories {
    maven { url = uri("https://jitpack.io") }
    mavenCentral()
}

dependencies {
    // Spring
    implementation(libs.spring.boot.starter)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-cache")

    // Database
    runtimeOnly(libs.postgresql)
    runtimeOnly("com.h2database:h2:2.3.232")

    // Lombok
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    // Telegram
    implementation(libs.telegram.client)
    implementation(libs.telegram.longpolling)

    implementation("org.ton.ton4j:smartcontract:1.3.5")

    implementation("com.github.ben-manes.caffeine:caffeine:3.2.3")
    implementation("com.itextpdf:itextpdf:5.5.13.3")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<org.springframework.boot.gradle.tasks.bundling.BootJar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}