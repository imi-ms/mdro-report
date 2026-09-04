package de.uni_muenster.imi.oegd.webapp

import net.harawata.appdirs.AppDirsFactory
import java.io.File

/** helper function for deleting the cache during development **/
fun main() {
 clearCache()
}

fun clearCache() {
    val userCacheDir = System.getenv("mdroreport.cachedir") ?: AppDirsFactory.getInstance()
        .getUserCacheDir("mdroreport", "1.0", "IMI")!!

    val file = File(userCacheDir)
    println(file)
    file.deleteRecursively()
    println(file.exists())
}