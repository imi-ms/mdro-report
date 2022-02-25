System.setProperty( "user.dir", project.projectDir.toString() )

plugins {
    kotlin("jvm") version "1.6.10"
    id("application")
    id("org.openjfx.javafxplugin") version "0.0.12"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

kotlin {
    group = "de.uni_muenster.imi.oegd"
    version = "1.0"
}

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation(project(":application"))
}

application {
    mainClass.set("de.uni_muenster.imi.oegd.application.Main")
    /*applicationDefaultJvmArgs = listOf(
        "--add-opens",
        "java.base/jdk.internal.misc=ALL-UNNAMED",
        "-Dio.netty.tryReflectionSetAccessible=true"
    )*/
}