import org.panteleyev.jpackage.JPackageTask

plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
    id("org.openjfx.javafxplugin")
    id("org.panteleyev.jpackageplugin")
}

kotlin {
    group = "de.uni_muenster.imi.oegd.testdataGenerator"
    jvmToolchain(17)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.redundent:kotlin-xml-builder:1.9.1")
}

tasks {
    shadowJar {
        manifest {
            attributes("Main-Class" to "de.uni_muenster.imi.oegd.testdataGenerator.Main")
        }
        archiveFileName.set("MRETestdataGenerator.jar")
    }
}

//FOLLOWING TASKS CREATE SYSTEM DEPENDENT BINARY WITH JRE
task("copyDependencies", Copy::class) {
    from(configurations.runtimeClasspath).into("$buildDir/jars")
}

task("copyJar", Copy::class) {
    from(tasks.jar).into("$buildDir/jars")
}

tasks.register<JPackageTask>("CreateAppImage") {
    dependsOn("build", "copyDependencies", "copyJar")

    input = "$buildDir/jars"
    destination = "$buildDir/dist"

    appName = "MREReport-Testdata-Generator"
    vendor = "Institut für Medizinische Informatik Münster"

    mainJar = tasks.jar.get().archiveFileName.get()
    mainClass = "de.uni_muenster.imi.oegd.testdataGenerator.Main"

    javaOptions = listOf("-Dfile.encoding=UTF-8")
    type = org.panteleyev.jpackage.ImageType.APP_IMAGE
}

val javafx_version: String by project

javafx {
    version = javafx_version
    modules("javafx.base", "javafx.controls", "javafx.fxml", "javafx.graphics")
}
