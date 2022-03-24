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
}

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven")
}


val ktor_version: String by project
val kotlinx_html_version = "0.7.2"
dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("io.ktor:ktor-html-builder:$ktor_version")
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:$kotlinx_html_version")
    implementation("io.ktor:ktor-client-cio:$ktor_version")
    implementation("io.ktor:ktor-client-auth-jvm:$ktor_version")
    implementation("io.ktor:ktor-webjars:$ktor_version")
    implementation("io.ktor:ktor-server-servlet:$ktor_version")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
    implementation("net.harawata:appdirs:1.2.1")


    //webjars
    implementation("org.webjars.npm:jquery:3.3.1")
    implementation("org.webjars.npm:bootstrap:4.3.1")
    implementation("org.webjars.npm:bootstrap-icons:1.8.1")
    implementation("org.webjars.npm:popper.js:1.16.1")
    implementation("org.webjars.npm:github-com-chartjs-Chart-js:2.8.0")
}

tasks {
    shadowJar {
        manifest {
            attributes(Pair("Main-Class", "de.uni_muenster.imi.oegd.webapp.ServerKt"))
        }
        archiveFileName.set("MDReport-Light.jar")
    }
}