import org.gradle.api.file.DuplicatesStrategy

plugins {
    id("java")
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

group = "ru.lewis.leykabot"
version = "1.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
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

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.bootJar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveFileName.set("LeykaBot-1.0-SNAPSHOT.jar")
}

tasks.jar {
    enabled = false
}