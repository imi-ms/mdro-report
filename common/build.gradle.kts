plugins {
    kotlin("jvm")
}

kotlin {
    group = "de.uni_muenster.imi.oegd.common"
    version = "1.0"
}

repositories {
    mavenCentral()
}

val ktor_version: String by project

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-auth:$ktor_version")
    implementation("io.ktor:ktor-client-logging:$ktor_version")
}