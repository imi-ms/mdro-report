import org.panteleyev.jpackage.JPackageTask

plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
    id("org.openjfx.javafxplugin")
    id("org.panteleyev.jpackageplugin")
}

kotlin {
    group = "de.uni_muenster.imi.oegd.testdataGenerator"
    version = "1.2.1"
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

javafx {
    modules("javafx.base","javafx.controls","javafx.fxml", "javafx.graphics")
}
