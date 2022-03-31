plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
    id("org.openjfx.javafxplugin")
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
            attributes(Pair("Main-Class", "de.uni_muenster.imi.oegd.testdataGenerator.Main"))
        }
        archiveFileName.set("MDTestdataGenerator.jar")
    }
}

javafx {
    modules("javafx.base","javafx.controls","javafx.fxml", "javafx.graphics")
}
