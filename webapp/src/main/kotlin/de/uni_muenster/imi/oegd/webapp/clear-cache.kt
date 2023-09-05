package de.uni_muenster.imi.oegd.webapp

import net.harawata.appdirs.AppDirsFactory
import java.io.File

fun main() {
    val userCacheDir = System.getenv("mrereport.cachedir") ?: AppDirsFactory.getInstance()
        .getUserCacheDir("mrereport", "1.0", "IMI")!!

    val file = File(userCacheDir)
    println(file)
    file.deleteRecursively()
    println(file.exists())
}