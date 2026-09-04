package de.uni_muenster.imi.oegd.webapp.model

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.request.*
import java.io.File
import kotlin.io.println

/**
 * Common interface for both REST and local BaseX instance
 */
interface IBaseXClient : AutoCloseable {
    suspend fun executeXQuery(xquery: String): String

    /** Info for serialization*/
    fun getInfo(): BasexInfo
}

/**
 * Make REST calls to an external BaseX server
 */
class RestClient(
    val baseURL: String,
    val database: String,
    private val username: String,
    private val password: String
) : IBaseXClient {
    private val client = HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = Long.MAX_VALUE
        }
    }


    override suspend fun executeXQuery(xquery: String): String {
        println("executing xquery = $xquery")
        try {
            return client.post("$baseURL/$database") {
                basicAuth(username, password)
                setBody("<query><text><![CDATA[ $xquery ]]></text></query>")
            }.body<String>().also { println(it) }
        } catch (e: Exception) {
            println("Error $e when executing XQuery: '$xquery'!")
            e.printStackTrace(System.out)
            throw e
        }
    }

    suspend fun uploadTestdata(folder: File) {
        for (file in folder.listFiles()) {
            try {
                println(file.name)
                client.post("$baseURL/$database") {
                    basicAuth(username, password)
                    setBody("<commands><check input='$database'/><put path='${file.nameWithoutExtension}'>${file.readText()}</put></commands>")
                }.body<String>().also { println(it) }
            } catch (e: Exception) {
                println("Error $e!")
                e.printStackTrace(System.out)
            }
        }
    }


    override fun close() {
        client.close()
    }

    override fun getInfo() = RestConnectionInfo(baseURL, database)

}


suspend fun main() {
    RestClient("https://basex-pseudo.ukmuenster.de/rest", "test", "oehm", "5ZjULwRa2MSMJ82")
        .uploadTestdata(File("testdata"))
}