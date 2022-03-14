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

val ktor_version: String by project
dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("io.ktor:ktor-server-netty:$ktor_version")


    implementation(project(":baseX"))
    implementation(project(":common"))
    implementation(project(":webapp"))
}

javafx {
    modules("javafx.base","javafx.controls","javafx.fxml",
        "javafx.graphics","javafx.web")
}
