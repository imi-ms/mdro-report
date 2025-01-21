plugins {
    kotlin("jvm")
}

kotlin {
    group = "de.uni_muenster.imi.oegd.baseX"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(project(":webapp"))
    implementation("org.basex:basex:11.6")
}