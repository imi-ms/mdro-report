plugins {
    kotlin("jvm")
}

kotlin {
    group = "de.uni_muenster.imi.oegd.testdataGenerator"
}

repositories {
    mavenCentral()
}


dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.redundent:kotlin-xml-builder:1.7.4")
}