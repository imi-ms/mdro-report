plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.github.johnrengelman.shadow")
    war
}

war {
    webAppDirName = "webapp"
}

kotlin {
    group = "de.uni_muenster.imi.oegd.webapp"
}

repositories {
    mavenCentral()
}


val ktor_version: String by project
val kotlinx_html_version = "0.12.0"
dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:$kotlinx_html_version")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
    implementation("net.harawata:appdirs:1.3.0")

    implementation("io.ktor:ktor-server-netty-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-html-builder-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-servlet-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-status-pages:$ktor_version")
    implementation("io.ktor:ktor-client-cio-jvm:$ktor_version")
    implementation("io.ktor:ktor-client-auth-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-call-logging-jvm:$ktor_version")

    implementation("javax.xml.parsers:jaxp-api:1.4.5") //StAX XML API
}

tasks {
    shadowJar {
        manifest {
            attributes("Main-Class" to "de.uni_muenster.imi.oegd.webapp.EntrypointsKt")
        }

        archiveFileName.set("MDROReport-Light.jar")
    }
}