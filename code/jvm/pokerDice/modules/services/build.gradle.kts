plugins {
    kotlin("jvm")
}

group = "poker"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    api(project(":modules:domain"))
    implementation(project(":modules:repository"))

    // To get the DI annotation
    implementation("jakarta.inject:jakarta.inject-api:2.0.1")

    // To use Kotlin specific date and time functions
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")

    // To use SLF4J
    implementation("org.slf4j:slf4j-api:2.0.16")

    // To use the JDBI-based repository implementation on the tests
    testImplementation(project(":modules:repositoryJdbi"))
    testImplementation("org.jdbi:jdbi3-core:3.37.1")
    testImplementation("org.postgresql:postgresql:42.7.2")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
    if (System.getenv("DB_URL") == null) {
        environment("DB_URL", "jdbc:postgresql://localhost:5432/db?user=poker_user&password=poker_pass")
    }
    dependsOn(":modules:repositoryJdbi:dbTestsWait")
    finalizedBy(":modules:repositoryJdbi:dbTestsDown")
}

kotlin {
    jvmToolchain(21)
}
