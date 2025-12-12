plugins {
    java
    id("org.springframework.boot") version "3.5.8"
    id("io.spring.dependency-management") version "1.1.7"
    jacoco
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
//    implementation("org.springframework.boot:spring-boot-starter-security")

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

    // XML 파싱
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml")
}

tasks.withType<Test> {
    useJUnitPlatform()
}



/** -----------------------------
 *  JaCoCo
 *  ----------------------------- */
jacoco {
    toolVersion = "0.8.12" // Java 21 호환
}

/** 공통 커버리지 제외 패턴 */
val coverageExcludes = listOf(
    "**/*Application*",
    "**/config/**",
    "**/dto/**",
    "**/error/**",
    "**/exception/**",
    "**/*Repository.class"
)

/** -----------------------------
 *  Test 공통 설정 (로깅/요약/실패수집)
 *  ----------------------------- */
tasks.withType<Test>().configureEach {
    useJUnitPlatform()

    testLogging {
        events("PASSED", "FAILED", "SKIPPED")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
        showStandardStreams = true
    }

    // 각 Test task의 JaCoCo 실행파일 경로를 명시
    extensions.configure(org.gradle.testing.jacoco.plugins.JacocoTaskExtension::class) {
        val execName = if (name == "test") "test.exec" else "${name}.exec"
        setDestinationFile(layout.buildDirectory.file("jacoco/$execName").get().asFile)
    }


    val failed = mutableListOf<Triple<String, String, String?>>() // class, method, msg
    addTestListener(object : org.gradle.api.tasks.testing.TestListener {
        override fun beforeSuite(suite: TestDescriptor) {}
        override fun beforeTest(testDescriptor: TestDescriptor) {}
        override fun afterTest(desc: TestDescriptor, result: TestResult) {
            if (result.resultType == TestResult.ResultType.FAILURE) {
                val clazz = desc.className ?: "(unknown-class)"
                val method = desc.name
                val msg = result.exception?.message?.lineSequence()?.firstOrNull()
                failed += Triple(clazz, method, msg)
            }
        }

        override fun afterSuite(suite: TestDescriptor, result: TestResult) {
            if (suite.parent == null) {
                println(
                    """
                    ------------------------
                    ✅ TEST RESULT SUMMARY
                    Total tests : ${result.testCount}
                    Passed      : ${result.successfulTestCount}
                    Failed      : ${result.failedTestCount}
                    Skipped     : ${result.skippedTestCount}
                    ------------------------
                    """.trimIndent()
                )
                val out = layout.buildDirectory.file("reports/tests/failed-tests.txt").get().asFile
                out.parentFile.mkdirs()
                if (failed.isNotEmpty()) {
                    val RED = "\u001B[31m"
                    val RESET = "\u001B[0m"
                    println("❌ FAILED TESTS (${failed.size})")
                    failed.forEachIndexed { i, (c, m, msg) ->
                        println("${RED}${i + 1}. $c#$m${if (msg != null) "  —  $msg" else ""}${RESET}")
                    }
                    out.printWriter().use { pw ->
                        pw.println("FAILED TESTS (${failed.size})")
                        failed.forEach { (c, m, msg) ->
                            pw.println("$c#$m${if (msg != null) " — $msg" else ""}")
                        }
                        pw.println()
                        pw.println("Patterns for --tests:")
                        failed.forEach { (c, m, _) -> pw.println("--tests \"$c.$m\"") }
                    }
                    println("📄 Saved failed list -> ${out.absolutePath}")
                } else {
                    out.writeText("No failures 🎉")
                }
            }
        }
    })
}

/** -----------------------------
 *  기본 test 태스크
 *  ----------------------------- */
tasks.named<Test>("test") {
    if (project.findProperty("includeIntegration") == "true") {
        systemProperty("junit.platform.tags.includes", "integration,unit")
    } else {
        systemProperty("junit.platform.tags.excludes", "integration")
    }
    finalizedBy(tasks.jacocoTestReport)
}

tasks.register<Test>("fullTest") {
    description = "Run unit + integration tests"
    group = "verification"

    val testSourceSet = sourceSets.named("test").get()
    testClassesDirs = testSourceSet.output.classesDirs
    classpath = testSourceSet.runtimeClasspath

    useJUnitPlatform()
    shouldRunAfter(tasks.named("test"))

    extensions.configure(org.gradle.testing.jacoco.plugins.JacocoTaskExtension::class) {
        setDestinationFile(layout.buildDirectory.file("jacoco/fullTest.exec").get().asFile)
    }
    finalizedBy("jacocoFullTestReport")
}

/** -----------------------------
 *  JaCoCo 리포트 (test)
 *  ----------------------------- */
tasks.jacocoTestReport {
    dependsOn(tasks.named("test"))

    // test.exec 을 명시적으로 사용
    executionData(layout.buildDirectory.file("jacoco/test.exec"))

    reports {
        xml.required.set(true)
        xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/xml/jacocoTestReport.xml"))
        html.required.set(true)
        csv.required.set(false)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/html"))
    }
    classDirectories.setFrom(
        files(
            classDirectories.files.map {
                fileTree(it) { exclude(coverageExcludes) }
            }
        )
    )
}

/** -----------------------------
 *  JaCoCo 리포트 (fullTest)
 *  ----------------------------- */
tasks.register<JacocoReport>("jacocoFullTestReport") {
    dependsOn(tasks.named("fullTest"))

    executionData(fileTree(layout.buildDirectory.dir("jacoco")) { include("*.exec") })

    reports {
        xml.required.set(true)
        xml.outputLocation.set(layout.buildDirectory.file("reports/jacocoFull/xml/jacocoFullTestReport.xml"))
        html.required.set(true)
        csv.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacocoFull/html"))
    }

    val main = sourceSets.named("main").get()
    sourceDirectories.setFrom(main.allSource.srcDirs)
    classDirectories.setFrom(
        files(
            main.output.classesDirs.files.map {
                fileTree(it) { exclude(coverageExcludes) }
            }
        )
    )
}