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

    implementation(project(":baseX"))
    implementation(project(":webapp"))
    implementation("io.ktor:ktor-server-netty-jvm:$ktor_version")
}

val javafx_version: String by project

javafx {
    version = javafx_version
    modules(
        "javafx.base", "javafx.controls", "javafx.fxml",
        "javafx.graphics", "javafx.web"
    )
}
