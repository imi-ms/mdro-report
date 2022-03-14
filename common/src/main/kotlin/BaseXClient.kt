package de.uni_muenster.imi.oegd.common

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
            requestTimeoutMillis = 60_000
        }
    }

    override fun close() {
        client.close()
    }

    override suspend fun executeXQuery(xquery: String): String {
        return this.client.post<String>("$baseURL/$database") {
            body = """<query> <text> <![CDATA[ $xquery ]]> </text> </query>"""
        }
    }

}
