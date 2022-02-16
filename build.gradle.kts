plugins {
    kotlin("multiplatform") version "1.6.10"
    application
//    id("org.openjfx.javafxplugin") version "0.0.10"
}

group = "me.oehmj"
version = "1.0"

repositories {
    jcenter()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven")
}

//javafx {
//    modules = listOf("javafx.controls")
//}


val ktor_version = "1.6.3"
val kotlinx_html_version = "0.7.2"
kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
        withJava()
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }
    js(IR) {
        binaries.executable()
        browser {
            commonWebpackConfig {
                cssSupport.enabled = true
            }
        }
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-core:$ktor_version")
                implementation("io.ktor:ktor-client-auth:$ktor_version")
                implementation("org.jetbrains.kotlinx:kotlinx-html:$kotlinx_html_version")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation("io.ktor:ktor-server-netty:1.6.3")
                implementation("io.ktor:ktor-html-builder:1.6.3")
                implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:$kotlinx_html_version")
                implementation("ch.qos.logback:logback-classic:1.2.3")
                implementation("io.ktor:ktor-client-cio:$ktor_version")
                implementation("io.ktor:ktor-client-auth-jvm:$ktor_version")
                implementation("io.ktor:ktor-webjars:$ktor_version")
//                implementation("io.ktor:ktor-statuspages:$ktor_version")
                //webjars
                implementation("org.webjars.npm:jquery:3.3.1")
                implementation("org.webjars.npm:bootstrap:4.3.1")
            }
        }
        val jvmTest by getting
        val jsMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-html-js:$kotlinx_html_version")
            }
        }
        val jsTest by getting
    }
}

application {
    mainClass.set("me.oehmj.application.ServerKt")
}

tasks.named<Copy>("jvmProcessResources") {
    val jsBrowserDistribution = tasks.named("jsBrowserDistribution")
    from(jsBrowserDistribution)
}

tasks.named<JavaExec>("run") {
    dependsOn(tasks.named<Jar>("jvmJar"))
    classpath(tasks.named<Jar>("jvmJar"))
}