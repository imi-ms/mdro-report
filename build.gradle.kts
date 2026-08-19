import org.gradle.kotlin.dsl.register
import org.panteleyev.jpackage.JPackageTask

System.setProperty("user.dir", project.projectDir.toString())

plugins {
    kotlin("jvm") version "2.4.10"
    kotlin("plugin.serialization") version "2.4.10"
    java
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("com.gradleup.shadow") version "9.6.1"
    id("org.panteleyev.jpackageplugin") version "2.1.0"
}


kotlin {
    group = "de.uni_muenster.imi.oegd"
    version = "1.6.8"
    jvmToolchain(21)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(project(":application"))
}

// Add logging dependencies to all subprojects
subprojects {
    plugins.withType<JavaPlugin> {
        dependencies {
            implementation("ch.qos.logback:logback-classic:1.6.3")
            implementation("io.github.microutils:kotlin-logging:3.0.5")
        }
    }
}

application {
    mainClass.set("de.uni_muenster.imi.oegd.application.Main")
    applicationDefaultJvmArgs = listOf("-Dio.netty.tryReflectionSetAccessible=true")
}


tasks.shadowJar {
    archiveFileName.set("MDROReport-Full.jar")
    exclude {
        //Only include minified versions of webjar library into the distributed bundle
        "META-INF/resources/webjars" in it.path
                && it.name !in setOf(
            "jquery.min.js", "Chart.min.js", "bootstrap.bundle.min.js", "bootstrap-icons.css",
            "bootstrap-icons.woff2", "bootstrap-icons.woff", "bootstrap.min.css"
        )
    }
    exclude { it.path.startsWith("META-INF/maven") }
}


//FOLLOWING TASKS CREATE SYSTEM DEPENDENT BINARY WITH JRE
val copyDependencies = tasks.register<Copy>("copyDependencies") {
    description = "copy all dependencies"
    from(configurations.runtimeClasspath)
        .into(layout.buildDirectory.get().dir("jars"))
}

val copyJar = tasks.register<Copy>("copyJar") {
    description = "copy all jar files"
    dependsOn(tasks.shadowJar)

    from(tasks.shadowJar.get().archiveFile)
        .into(layout.buildDirectory.get().dir("jars"))
}

tasks.register<JPackageTask>("CreateAppImage") {
    dependsOn("build", copyJar)

    input = layout.buildDirectory.dir("jars")
    destination = layout.buildDirectory.dir("dist")


    appName = "MDRO-Report"
    vendor = "Institute of Medical Informatics & Institute of Hygiene Münster"

    mainJar = tasks.shadowJar.get().archiveFileName.get()
    mainClass = "de.uni_muenster.imi.oegd.application.Main"

    javaOptions = listOf("-Dfile.encoding=UTF-8")
    type = org.panteleyev.jpackage.ImageType.APP_IMAGE
}

tasks.register<JPackageTask>("CreateEXE") {
    dependsOn("build", copyJar)

    input = layout.buildDirectory.dir("jars")
    destination = layout.buildDirectory.dir("dist")

    appName = "MDRO-Report"
    vendor = "Institute of Medical Informatics & Institute of Hygiene Münster"

    mainJar = tasks.shadowJar.get().archiveFileName.get()
    mainClass = "de.uni_muenster.imi.oegd.application.Main"

    javaOptions = listOf("-Dfile.encoding=UTF-8")
    type = org.panteleyev.jpackage.ImageType.EXE

    winDirChooser = true
    winMenu = true
}
