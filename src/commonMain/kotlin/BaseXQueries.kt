import io.ktor.client.*
import io.ktor.client.features.auth.*
import io.ktor.client.features.auth.providers.*
import io.ktor.client.request.*

object BaseXQueries {
    //language=XPath
    fun getMRSA() =
        "for \$x in /patient/case/labReport/sample/germ/comment[contains(@class,\"MRSA\")]\nwhere \$x/../../../../@type=\"S\"\n\nlet \$ids:=\$x/../../../../@id\ngroup by \$ids\nlet \$station := string-join(\$x/../../../../location[@till > subsequence(\$x/../../../sample/@from,1,1) and @from < subsequence(\$x/../../../sample/@from,1,1)]/@clinic,'; ')\n\nreturn \n(\$x/../../../../@id || \"&#9;||&#9;\" || subsequence(\$x/../../../request/@from,1,1) || \"&#9;||&#9;\"  || subsequence(\$x/../../../sample/@display,1,1) || \"&#9;||&#9;\"  || subsequence(\$x/../../../../hygiene-message/@infection,1,1) || \"&#9;||&#9;\"  || subsequence(\$x/../../../../hygiene-message/@nosocomial,1,1) || \"&#9;||&#9;\" || subsequence(\$x/../../../request/@sender,1,1)|| \"&#9;||&#9;\" || \$station|| \"&#9;||&#9;\" ||   subsequence(\$x/../../germ/pcr-meta[@k=\"Spa\"]/@v,1,1)|| \"&#9;||&#9;\" ||subsequence(\$x/../../germ/pcr-meta[@k=\"ClusterType\"]/@v,1,1) )\n"

}

interface IBaseXClient {
    fun close()

    suspend fun executeXQuery(xquery: String): String
}

class BaseXClient(
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
        }
    }

    override suspend fun executeXQuery(xquery: String): String {
        val result = this.client.post<String> {
            url("$baseURL/$database")
            body = """<query> <text> <![CDATA[ $xquery ]]> </text> </query>"""
        }
        return result
    }


}
