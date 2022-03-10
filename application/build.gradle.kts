plugins {
    kotlin("jvm")
    id("org.openjfx.javafxplugin")
}

kotlin {
    group = "de.uni_muenster.imi.oegd.application"
    version = "1.0"
}

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven")
}

val ktor_version = "1.6.3"
val kotlinx_html_version = "0.7.2"
dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-auth:$ktor_version")
    implementation("org.jetbrains.kotlinx:kotlinx-html:$kotlinx_html_version")
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:$kotlinx_html_version")
    implementation("io.ktor:ktor-server-netty:1.6.3")
    implementation("io.ktor:ktor-html-builder:1.6.3")
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("io.ktor:ktor-client-cio:$ktor_version")
    implementation("io.ktor:ktor-client-auth-jvm:$ktor_version")
    implementation("io.ktor:ktor-webjars:$ktor_version")
    implementation(project(":baseX"))
    implementation(project(":webapp"))
}

javafx {
    modules("javafx.base","javafx.controls","javafx.fxml",
        "javafx.graphics","javafx.web")
}
