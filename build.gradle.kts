import org.gradle.api.file.DuplicatesStrategy

plugins {
    java
    id("org.springframework.boot") version "3.4.2"
    id("io.spring.dependency-management") version "1.1.7"
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
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-cache")

    // Database
    runtimeOnly("org.postgresql:postgresql:42.7.3")
    runtimeOnly("com.h2database:h2:2.3.232")

    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.36")
    annotationProcessor("org.projectlombok:lombok:1.18.36")

    // Telegram
    implementation("org.telegram:telegrambots-client:9.2.1")
    implementation("org.telegram:telegrambots-longpolling:9.2.1")

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