import org.panteleyev.jpackage.JPackageTask

plugins {
    kotlin("jvm")
    id("com.gradleup.shadow")
    id("org.openjfx.javafxplugin")
    id("org.panteleyev.jpackageplugin")
}

kotlin {
    group = "de.uni_muenster.imi.oegd.testdataGenerator"
    jvmToolchain(21)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.redundent:kotlin-xml-builder:1.9.1")
}

javafx {
    version = project.findProperty("javafx_version") as String
    modules("javafx.base", "javafx.controls", "javafx.fxml", "javafx.graphics")
}

tasks {
    shadowJar {
        mainClass.set("de.uni_muenster.imi.oegd.testdataGenerator.TestdataMain")
        archiveFileName.set("MDROTestdataGenerator.jar")
    }
}

//FOLLOWING TASKS CREATE SYSTEM DEPENDENT BINARY WITH JRE
tasks.register<Copy>("copyDependencies") {
    from(configurations.runtimeClasspath).into(layout.buildDirectory.dir("jars"))
}

tasks.register<Copy>("copyJar") {
    dependsOn(tasks.shadowJar)
    from(tasks.jar).into(layout.buildDirectory.dir("jars"))
}

tasks.register<JPackageTask>("CreateEXE") {
    dependsOn("build", "copyDependencies", "copyJar")

    input = layout.buildDirectory.dir("jars")
    destination = layout.buildDirectory.dir("dist")

    appName = "MDRO-Report Testdata-Generator"
    vendor = "Institute for Medical Informatics Muenster"
    appVersion = "1.0"

    mainJar = tasks.jar.get().archiveFileName.get()
    mainClass = "de.uni_muenster.imi.oegd.testdataGenerator.TestdataMain"

    javaOptions = listOf("-Dfile.encoding=UTF-8")
    type = org.panteleyev.jpackage.ImageType.EXE

    winDirChooser = true
    winMenu = true
}


