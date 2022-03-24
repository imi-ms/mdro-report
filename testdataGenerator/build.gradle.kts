plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
    id("application")
}

kotlin {
    group = "de.uni_muenster.imi.oegd.testdataGenerator"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.redundent:kotlin-xml-builder:1.7.4")
}

application {
    mainClass.set("de.uni_muenster.imi.oegd.testdataGenerator.CLIGenerator")
}

tasks {
    shadowJar {
        archiveFileName.set("MDTestdataGenerator.jar")
    }
}
