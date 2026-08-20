import org.panteleyev.jpackage.JPackageTask
import java.awt.Image.SCALE_SMOOTH
import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_INT_ARGB
import java.util.Locale
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam


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
    implementation("org.redundent:kotlin-xml-builder:1.9.3")
}

javafx {
    version = project.findProperty("javafx_version") as String
    modules("javafx.base", "javafx.controls", "javafx.fxml", "javafx.graphics")
}

tasks.shadowJar {
    mainClass.set("de.uni_muenster.imi.oegd.testdataGenerator.TestdataMain")
    archiveFileName.set("MDROTestdataGenerator.jar")
}

val copyLogo = tasks.register<Copy>("copyLogo") {
    description = "copy the logo file from the application subproject"
    from(project(":application").file("src/main/resources/logo")).into(layout.buildDirectory.dir("resources"))
}
tasks.build {
    dependsOn(copyLogo)
}

//FOLLOWING TASKS CREATE SYSTEM DEPENDENT BINARY WITH JRE
val copyDependencies = tasks.register<Copy>("copyDependencies") {
    from(configurations.runtimeClasspath).into(layout.buildDirectory.dir("jars"))
}

val copyJar = tasks.register<Copy>("copyJar") {
    dependsOn(tasks.shadowJar)
    from(tasks.jar).into(layout.buildDirectory.dir("jars"))
}
val createIco = tasks.register<ConvertPngToIcoTask>("createIco") {
    inputFile.set(layout.projectDirectory.file("src/main/resources/label.png"))
    outputFile.set(layout.buildDirectory.file("generated/label.ico"))
}


tasks.register<JPackageTask>("CreateEXE") {
    dependsOn(tasks.build, copyDependencies, copyJar, createIco)

    input = layout.buildDirectory.dir("jars")
    destination = layout.buildDirectory.dir("dist")

    appName = "MDRO-Report Testdata-Generator"
    vendor = "Institute for Medical Informatics Muenster"
    appVersion = project.parent?.version.toString()

    mainJar = tasks.jar.get().archiveFileName.get()
    mainClass = "de.uni_muenster.imi.oegd.testdataGenerator.TestdataMain"

    javaOptions = listOf("-Dfile.encoding=UTF-8")
    type = org.panteleyev.jpackage.ImageType.EXE

    icon = layout.buildDirectory.file("resources/logo.ico")

    winDirChooser = true
    winMenu = true
}


