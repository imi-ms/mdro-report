import org.panteleyev.jpackage.JPackageTask

System.setProperty("user.dir", project.projectDir.toString())

plugins {
    kotlin("jvm") version "1.6.20"
    kotlin("plugin.serialization") version "1.6.20"
    id("java")
    id("application")
    id("org.openjfx.javafxplugin") version "0.0.12"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("org.panteleyev.jpackageplugin") version "1.3.1"
}

buildscript {
    repositories {
        mavenCentral() // For the ProGuard Gradle Plugin and anything else.
    }
    dependencies {
        classpath("com.guardsquare:proguard-gradle:7.2.1")  // The ProGuard Gradle plugin.
    }
}

kotlin {
    group = "de.uni_muenster.imi.oegd"
    version = "1.2.2"
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
    dependsOn(tasks.shadowJar)
    from(tasks.shadowJar).into("$buildDir/jars")
}

tasks.register<proguard.gradle.ProGuardTask>("minimizedJar") {
    dependsOn("copyJar")
    verbose()
//    ignorewarnings()

    injars("$buildDir/jars/MREReport-Full.jar")
    outjars("$buildDir/minJar/MREReport.min.jar")

    val javaHome = System.getProperty("java.home")
    // Automatically handle the Java version of this build.
    if (System.getProperty("java.version").startsWith("1.")) {
        // Before Java 9, the runtime classes were packaged in a single jar file.
        libraryjars("$javaHome/lib/rt.jar")
    } else {
        // As of Java 9, the runtime classes are packaged in modular jmod files.
        for (module in listOf(
            "java.base", "jdk.xml.dom", "jdk.jsobject", "java.xml", "java.desktop",
            "java.datatransfer", "java.logging", "java.management", "java.naming", "java.net.http",
            "java.xml.crypto", "java.sql", "java.scripting"
        )) {
            libraryjars(
                mapOf("jarfilter" to "!**.jar", "filter" to "!module-info.class"),
                "$javaHome/jmods/$module.jmod"
            )
        }
    }

//    overloadaggressively()
//    repackageclasses("")
    allowaccessmodification()

    printmapping("build/proguard-mapping.txt")
    dontobfuscate()
    dontoptimize()
//    dontwarn()

    keep("class de.uni_muenster.imi.oegd.application.Main { *;  }")
    keep("public class com.sun.javafx.tk.quantum.QuantumToolkit { *;  }")
    keep("class com.sun.glass.ui.** { *;  }")
    keep("class com.sun.javafx.** { *;  }")
    keep("class javafx.fxml.** { *;  }")
    keep("class javafx.scene.** { *;  }")
    keep("class javafx.geometry.** { *;  }")
    keep("class javafx.css.** { *;  }")
    keep("class com.sun.prism.shader.** { *;  }")
    keep("class com.sun.prism.es2.** { *;  }")
    keep("class com.sun.scenario.effect.impl.prism.** { *;  }")
    keep("class com.sun.scenario.effect.impl.es2.** { *;  }")
    keep("public class com.sun.prism.**Pipeline { *;  }")
    keep("class com.sun.glass.ui.win.WinPlatformFactory")
    keep("class kotlin.reflect.jvm.internal.** { *;  }")
    keep("class kotlin.text.RegexOption { *;  }")
    keep("public class ch.qos.logback.** { *; }")
    dontwarn("ch.qos.logback.**")
    dontwarn("groovy.**")
    dontwarn("org.codehaus.groovy.**")
    dontwarn("io.netty.**")
    dontwarn("com.sun.javafx.logging.jfr.**")
    dontwarn("kotlinx.coroutines.debug.**")
    dontwarn("com.sun.marlin.**")
    dontwarn("io.ktor.server.**")
    ignorewarnings()
    adaptresourcefilenames("**.so")
    adaptresourcefilecontents("**.so")
//    dontwarn("org.w3c.**")

}


tasks.register<JPackageTask>("CreateAppImage") {
    dependsOn("build", "minimizedJar")

    input = "$buildDir/minJar"
    destination = "$buildDir/dist"

    appName = "MRE-Report"
    vendor = "Institut für Medizinische Informatik Münster"

    mainJar = "MREReport.min.jar"
    mainClass = "de.uni_muenster.imi.oegd.application.Main"

    javaOptions = listOf("-Dfile.encoding=UTF-8")
    type = org.panteleyev.jpackage.ImageType.APP_IMAGE
}

tasks.register<JPackageTask>("CreateEXE") {
    dependsOn("build", "minimizedJar")

    input = "$buildDir/minJar"
    destination = "$buildDir/dist"

    appName = "MRE-Report"
    vendor = "Institut für Medizinische Informatik Münster"

    mainJar = "MREReport.min.jar"
    mainClass = "de.uni_muenster.imi.oegd.application.Main"

    javaOptions = listOf("-Dfile.encoding=UTF-8")
    type = org.panteleyev.jpackage.ImageType.EXE

    winDirChooser = true
    winMenu = true
}

tasks {
    shadowJar {
        archiveFileName.set("MREReport-Full.jar")
        exclude {
            it.path.contains("META-INF/resources/webjars")
                    && it.name !in setOf(
                "jquery.min.js", "Chart.min.js", "bootstrap.min.js", "bootstrap-icons.css",
                "bootstrap-icons.woff2", "bootstrap-icons.woff", "bootstrap.min.css"
            )
        }
        exclude { it.path.startsWith("META-INF/maven") }
    }
}

