plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
    kotlin("jvm") version "2.0.21" apply false
    kotlin("plugin.spring") version "2.0.21" apply false
    id("org.springframework.boot") version "3.5.5" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1" apply false
}
rootProject.name = "pokerDice"

// Inclui os módulos com paths relativos
include("modules:domain")
include("modules:repository")
include("modules:services")
include("modules:http")
include("modules:host")
include("modules:repositoryJdbi")

// Define o diretório físico de cada módulo
project(":modules:domain").projectDir = file("modules/domain")
project(":modules:repository").projectDir = file("modules/repository")
project(":modules:services").projectDir = file("modules/services")
project(":modules:http").projectDir = file("modules/http")
project(":modules:host").projectDir = file("modules/host")
project(":modules:repositoryJdbi").projectDir = file("modules/repositoryJdbi")
