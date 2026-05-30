plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

group = "pokerDice"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // --- Módulos internos ---
    implementation(project(":modules:repositoryJdbi"))
    implementation(project(":modules:services"))
    implementation(project(":modules:http"))

    // --- Spring Boot principais ---
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // --- Jackson + Kotlin reflection ---
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // --- Base de dados ---
    implementation("org.jdbi:jdbi3-core:3.37.1")
    implementation("org.postgresql:postgresql:42.7.2")

    // --- Kotlin datetime ---
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")

    // --- Password encoder ---
    implementation("org.springframework.security:spring-security-core:6.5.4")

    // --- Dependências de teste ---
    testImplementation(kotlin("test"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

tasks.bootRun {
    environment("DB_URL", "jdbc:postgresql://db-tests:5432/poker_db?user=poker_user&password=poker_pass")
}

tasks.register<Copy>("extractUberJar") {
    dependsOn(tasks.bootJar)

    // pega dinamicamente o JAR gerado pelo bootJar
    val bootJarFile =
        tasks.bootJar
            .get()
            .archiveFile
            .get()
            .asFile

    from(zipTree(bootJarFile))
    into("build/dependency")
}

/**
 * Tags das imagens Docker
 */
val dockerImageTagJvm = "poker-dice-jvm"
val dockerImageTagPostgresTest = "poker-dice-postgres-test"
val dockerImageTagUbuntu = "poker-dice-ubuntu"
val dockerImageNginx = "poker-nginx"
val dockerExe = "docker"

tasks.register<Exec>("buildImageJvm") {
    dependsOn("extractUberJar")
    commandLine("docker", "build", "-t", dockerImageTagJvm, "-f", "tests/Dockerfile-jvm", ".")
}

tasks.register<Exec>("buildImagePostgresTest") {
    commandLine(
        "docker",
        "build",
        "-t",
        dockerImageTagPostgresTest,
        "-f",
        "tests/Dockerfile-postgres-test",
        "../..",
    )
}

tasks.register<Exec>("buildImageUbuntu") {
    commandLine("docker", "build", "-t", dockerImageTagUbuntu, "-f", "tests/Dockerfile-ubuntu", ".")
}

tasks.register<Exec>("buildImageNginx") {
    commandLine(dockerExe, "build", "-t", dockerImageNginx, "-f", "code/jvm/pokerDice/modules/host/tests/Dockerfile-nginx", ".")
    workingDir(file("../../../../../"))
}

tasks.register("buildImageAll") {
    dependsOn("buildImageJvm")
    dependsOn("buildImagePostgresTest")
    dependsOn("buildImageUbuntu")
    dependsOn("buildImageNginx")
}

tasks.register<Exec>("allUp") {
    commandLine("docker", "compose", "up", "--force-recreate", "-d")
}

tasks.register<Exec>("allDown") {
    commandLine("docker", "compose", "down")
}