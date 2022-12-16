package model

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.request.*

/**
 * Common interface for both REST and local BaseX instance
 */
interface IBaseXClient : AutoCloseable {
    suspend fun executeXQuery(xquery: String): String

    /**
     * Info for serialization
     */
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
        install(Auth) {
            basic {
                credentials { BasicAuthCredentials(this@RestClient.username, this@RestClient.password) }
                sendWithoutRequest { true }
            }
        }
        install(HttpTimeout) {
            requestTimeoutMillis = Long.MAX_VALUE
        }
    }

    override fun close() {
        client.close()
    }

    override suspend fun executeXQuery(xquery: String): String {
        try {
            return this.client.post("$baseURL/$database") {
                setBody("<query><text><![CDATA[ $xquery ]]></text></query>")
            }.body()
        } catch (e: Exception) {
            println("Error when executing: $xquery")
            e.printStackTrace()
            throw e
        }
    }

    override fun getInfo() = RestConnectionInfo(baseURL, database)


}
