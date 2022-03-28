plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
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

tasks {
    shadowJar {
        manifest {
            attributes(Pair("Main-Class", "de.uni_muenster.imi.oegd.testdataGenerator.TestdataGeneratorKt"))
        }
        archiveFileName.set("MDTestdataGenerator.jar")
    }
}
