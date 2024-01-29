plugins {
    kotlin("jvm")
}

kotlin {
    group = "de.uni_muenster.imi.oegd.baseX"
    version = "1.0"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.basex:basex:10.7")
    implementation(project(":webapp"))
}