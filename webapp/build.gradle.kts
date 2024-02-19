plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.github.johnrengelman.shadow")
    id("war")
}

war {
    webAppDirName = "webapp"
}

kotlin {
    group = "de.uni_muenster.imi.oegd.webapp"
    jvmToolchain(17)
}

repositories {
    mavenCentral()
}


val ktor_version: String by project
val kotlinx_html_version = "0.11.0"
dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:$kotlinx_html_version")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    implementation("net.harawata:appdirs:1.2.1")

    implementation("io.ktor:ktor-server-netty-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-html-builder-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-servlet-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-status-pages:$ktor_version")
    implementation("io.ktor:ktor-client-cio-jvm:$ktor_version")
    implementation("io.ktor:ktor-client-auth-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-call-logging-jvm:$ktor_version")
}

tasks {
    shadowJar {
        manifest {
            attributes("Main-Class" to "de.uni_muenster.imi.oegd.webapp.EntrypointsKt")
        }

        archiveFileName.set("MREReport-Light.jar")
    }
}