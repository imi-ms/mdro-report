package de.uni_muenster.imi.oegd.baseX

import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.features.auth.*
import io.ktor.client.features.auth.providers.*
import io.ktor.client.request.*
import org.basex.BaseXServer
import org.basex.api.client.ClientSession
import org.basex.core.Context
import org.basex.core.cmd.*
import java.io.File
import java.nio.file.Files

/**
 * Common interface for both REST and local BaseX instance
 */
interface IBaseXClient {
    fun close()

    suspend fun executeXQuery(xquery: String): String
}

class RestClient(
    private val baseURL: String,
    private val database: String,
    private val username: String,
    private val password: String
) :
    IBaseXClient {
    private val client = getConnection()

    override fun close() {
        client.close()
    }

    private fun getConnection(): HttpClient {
        val that = this
        return HttpClient {
            install(Auth) {
                basic {
                    credentials {
                        BasicAuthCredentials(that.username, that.password)
                    }
                    sendWithoutRequest { true }
                }
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 60000
            }
        }
    }

    override suspend fun executeXQuery(xquery: String): String {
        val adjustedXQuery = xquery.replace("\"REPLACED LATER\"", "")
        val result = this.client.post<String> {
            url("$baseURL/$database")
            body = """<query> <text> <![CDATA[ $adjustedXQuery ]]> </text> </query>"""
        }
        return result
    }

}
class LocalBaseXClient(directory: File, port: Int = 8081) : IBaseXClient {

    private lateinit var session: ClientSession
    private val context: Context = Context()
    private lateinit var server: BaseXServer

    init {
        try {
            this.server = BaseXServer("-p $port")
            this.session = ClientSession("localhost", port, "admin", "admin")
            CreateDB("LocalDB").execute(context)
            processDirectory(directory, context)
            Optimize().execute(context)

        } catch (e: Exception) {
            //TODO: Include Error in application (... please try again)
        }
    }

    override suspend fun executeXQuery(xquery: String): String {
        return XQuery(xquery).execute(context)
    }

    override fun close() {
        server.stop()
        //Drop local DB
        DropDB("LocalDB").execute(context)
        context.close()
    }


    private fun processDirectory(directory: File, context: Context){
        directory.walk()
            .filter { item -> Files.isRegularFile(item.toPath())}
            .forEach { Add("", it.absolutePath).execute(context) }
    }
}
