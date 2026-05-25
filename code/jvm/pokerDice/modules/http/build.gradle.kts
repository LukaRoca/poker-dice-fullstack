plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
}

group = "poker"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":modules:services"))
    implementation(project(":modules:domain"))

    // To use Spring MVC and the Servlet API
    implementation("org.springframework:spring-webmvc:6.2.10")
    compileOnly("jakarta.servlet:jakarta.servlet-api:6.1.0")

    // To use SLF4J
    implementation("org.slf4j:slf4j-api:2.0.16")

    // To use Kotlin specific date and time functions
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")
}

tasks.test {
    useJUnitPlatform()
    if (System.getenv("DB_URL") == null) {
        environment("DB_URL", "jdbc:postgresql://db-tests:5432/poker_db?user=poker_user&password=poker_pass")
    }
}
kotlin {
    jvmToolchain(21)
}
