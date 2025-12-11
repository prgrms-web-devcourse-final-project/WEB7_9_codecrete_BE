plugins {
    java
    id("org.springframework.boot") version "3.5.8"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.back"
version = "0.0.1-SNAPSHOT"
description = "WEB7_9_codecrete_BE"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // 스프링 웹
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-websocket")

    // 웹 클라이언트
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // 캐싱
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // JWT (jjwt)
    implementation("io.jsonwebtoken:jjwt-api:0.12.5")
    implementation("io.jsonwebtoken:jjwt-impl:0.12.5")
    implementation("io.jsonwebtoken:jjwt-jackson:0.12.5")

    // 시큐리티
    implementation("org.springframework.boot:spring-boot-starter-security")

    // 유효성 검증
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // JPA, JDBC
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    runtimeOnly("com.h2database:h2")
    runtimeOnly("com.mysql:mysql-connector-j")

    // 롬복
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // 테스트
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // 스프링 문서화
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.13")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
