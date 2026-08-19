plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.gradleup.shadow")
    war
}


kotlin {
    group = "de.uni_muenster.imi.oegd.webapp"
}

repositories {
    mavenCentral()
}


val ktor_version = project.findProperty("ktor_version") as String

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
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

tasks.shadowJar {
    manifest {
        attributes("Main-Class" to "de.uni_muenster.imi.oegd.webapp.EntrypointsKt")
    }

    archiveFileName.set("MDROReport-Light.jar")
}