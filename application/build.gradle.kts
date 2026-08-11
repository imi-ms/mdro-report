plugins {
    kotlin("jvm")
    id("org.openjfx.javafxplugin")
}

kotlin {
    group = "de.uni_muenster.imi.oegd.application"
}

repositories {
    mavenCentral()
}

val ktor_version = findProperty("ktor_version") as String

dependencies {
    implementation(kotlin("stdlib"))
    implementation(project(":baseX"))
    implementation(project(":webapp"))
    implementation("io.ktor:ktor-server-netty-jvm:$ktor_version")
}

javafx {
    version = project.findProperty("javafx_version") as String
    modules("javafx.base", "javafx.controls", "javafx.fxml", "javafx.graphics", "javafx.web")
}
