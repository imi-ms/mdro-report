import org.panteleyev.jpackage.JPackageTask

System.setProperty("user.dir", project.projectDir.toString())

plugins {
    kotlin("jvm") version "1.6.10"
    kotlin("plugin.serialization") version "1.6.10"
    id("java")
    id("application")
    id("org.openjfx.javafxplugin") version "0.0.12"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("org.panteleyev.jpackageplugin") version "1.3.1"
}

kotlin {
    group = "de.uni_muenster.imi.oegd"
    version = "1.0"
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation(project(":application"))
}

// Add logging dependencies to all subprojects
subprojects {
    plugins.withType(JavaPlugin::class) {
        dependencies {
            implementation("ch.qos.logback:logback-classic:1.2.3")
            implementation("io.github.microutils:kotlin-logging:2.1.21")
        }
    }
}

//CREATES EXECUTABLE JAR
application {
    mainClass.set("de.uni_muenster.imi.oegd.application.Main")
    applicationDefaultJvmArgs = listOf(
        "-Dio.netty.tryReflectionSetAccessible=true"
    )
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

    appName = "MDReport"
    vendor = "Institut für Medizinische Informatik Münster"

    mainJar = tasks.jar.get().archiveFileName.get()
    mainClass = "de.uni_muenster.imi.oegd.application.Main"

    javaOptions = listOf("-Dfile.encoding=UTF-8")
    type = org.panteleyev.jpackage.ImageType.APP_IMAGE
}

tasks.register<JPackageTask>("CreateEXE") {
    dependsOn("build", "copyDependencies", "copyJar")

    input = "$buildDir/jars"
    destination = "$buildDir/dist"

    appName = "MDReport"
    vendor = "Institut für Medizinische Informatik Münster"

    mainJar = tasks.jar.get().archiveFileName.get()
    mainClass = "de.uni_muenster.imi.oegd.application.Main"

    javaOptions = listOf("-Dfile.encoding=UTF-8")
    type = org.panteleyev.jpackage.ImageType.EXE

    winDirChooser = true
    winMenu = true
}

tasks.getByPath("build").finalizedBy("CreateAppImage")

tasks {
    shadowJar {
        archiveFileName.set("MDReport-Full.jar")
    }
}