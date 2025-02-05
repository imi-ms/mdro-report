import org.panteleyev.jpackage.JPackageTask

System.setProperty("user.dir", project.projectDir.toString())

plugins {
    kotlin("jvm") version "2.1.10"
    kotlin("plugin.serialization") version "2.1.10"
    java
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("org.panteleyev.jpackageplugin") version "1.5.2"
}


kotlin {
    group = "de.uni_muenster.imi.oegd"
    version = "1.5.0"
    jvmToolchain(21)
}

java {
//    sourceCompatibility = JavaVersion.VERSION_17
//    targetCompatibility = JavaVersion.VERSION_17
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
    plugins.withType(JavaPlugin::class) {
        dependencies {
            implementation("ch.qos.logback:logback-classic:1.5.16")
            implementation("io.github.microutils:kotlin-logging:3.0.5")
        }
    }
}

//CREATES EXECUTABLE JAR
application {
    mainClass.set("de.uni_muenster.imi.oegd.application.Main")
    applicationDefaultJvmArgs = listOf("-Dio.netty.tryReflectionSetAccessible=true")
}

//FOLLOWING TASKS CREATE SYSTEM DEPENDENT BINARY WITH JRE
task("copyDependencies", Copy::class) {
    from(configurations.runtimeClasspath)
        .into(layout.buildDirectory.get().dir("jars"))
}

task("copyJar", Copy::class) {
    dependsOn(tasks.shadowJar)

    from(tasks.shadowJar.get().archiveFile)
        .into(layout.buildDirectory.get().dir("jars"))
}


tasks.register<JPackageTask>("CreateAppImage") {
    dependsOn("build", "copyJar")

    input = "${layout.buildDirectory.get()}/jars"
    destination = "${layout.buildDirectory.get()}/dist"

    appName = "MDRO-Report"
    vendor = "Institut für Medizinische Informatik Münster"

    mainJar = tasks.shadowJar.get().archiveFileName.get()
    mainClass = "de.uni_muenster.imi.oegd.application.Main"

    javaOptions = listOf("-Dfile.encoding=UTF-8")
    type = org.panteleyev.jpackage.ImageType.APP_IMAGE
}

tasks.register<JPackageTask>("CreateEXE") {
    dependsOn("build", "copyJar")

    input = "${layout.buildDirectory.get()}/jars"
    destination = "${layout.buildDirectory.get()}/dist"

    appName = "MDRO-Report"
    vendor = "Institut für Medizinische Informatik Münster"

    mainJar = tasks.shadowJar.get().archiveFileName.get()
    mainClass = "de.uni_muenster.imi.oegd.application.Main"

    javaOptions = listOf("-Dfile.encoding=UTF-8")
    type = org.panteleyev.jpackage.ImageType.EXE

    winDirChooser = true
    winMenu = true
}

tasks {
    shadowJar {
        archiveFileName.set("MDROReport-Full.jar")
        exclude {
            //Only include minified versions of webjar library into the distributed bundle
            it.path.contains("META-INF/resources/webjars")
                    && it.name !in setOf(
                "jquery.min.js", "Chart.min.js", "bootstrap.bundle.min.js", "bootstrap-icons.css",
                "bootstrap-icons.woff2", "bootstrap-icons.woff", "bootstrap.min.css"
            )
        }
        exclude { it.path.startsWith("META-INF/maven") }
    }
}
