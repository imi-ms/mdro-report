package de.uni_muenster.imi.oegd.common

import de.uni_muenster.imi.oegd.webapp.BasexInfo
import de.uni_muenster.imi.oegd.webapp.RestConnectionInfo
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.features.auth.*
import io.ktor.client.features.auth.providers.*
import io.ktor.client.request.*

/**
 * Common interface for both REST and local BaseX instance
 */
interface IBaseXClient : AutoCloseable {
    suspend fun executeXQuery(xquery: String): String
    fun getInfo(): BasexInfo
}

/**
 * Make REST calls to an external BaseX server
 */
class RestClient(
    private val baseURL: String,
    private val database: String,
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
        return this.client.post<String>("$baseURL/$database") {
            body = """<query><text><![CDATA[ $xquery ]]></text></query>"""
        }
    }

    override fun getInfo() = RestConnectionInfo(baseURL, database)


}
