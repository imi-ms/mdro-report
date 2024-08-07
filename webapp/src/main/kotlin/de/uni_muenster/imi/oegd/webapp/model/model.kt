package de.uni_muenster.imi.oegd.webapp.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json

/**
 * Parameters that effect the XQueries
 */
@Serializable
data class XQueryParams(
    val year: Int? = null,
)

@Serializable
data class FilterParams(val caseTypes: List<CaseType> = listOf(CaseType.STATIONAER))

@Serializable(with = CustomSerializer::class)
data class Params(val xquery: XQueryParams, val filter: FilterParams) {
    companion object {
        fun fromJson(json: String?): Params? {
            if (json == null || json == "null") {
                return null
            }
            //TODO: Remove replace function: Somehow the behaviour of JavaFX client and Firefox is different i guess or the issue is caused by post forms?
            return Json.decodeFromString(json.replace("%22", "\""))
        }
    }

    fun toJson() = Json.encodeToString(this)
}

@Serializable
@SerialName("Customer")
private data class ParamsSurrogate(val year: Int? = null, val caseTypes: List<CaseType> = listOf(CaseType.STATIONAER))
object CustomSerializer : KSerializer<Params> {
    override val descriptor: SerialDescriptor = ParamsSurrogate.serializer().descriptor
    override fun deserialize(decoder: Decoder): Params {
        val surrogate = decoder.decodeSerializableValue(ParamsSurrogate.serializer())
        return Params(XQueryParams(surrogate.year), FilterParams(surrogate.caseTypes))
    }

    override fun serialize(encoder: Encoder, value: Params) {
        val surrogate = ParamsSurrogate(value.xquery.year, value.filter.caseTypes)
        encoder.encodeSerializableValue(ParamsSurrogate.serializer(), surrogate)
    }
}


enum class GermType(val germtype: String) {
    MRSA("MRSA"),
    MRGN("MRGN"),
    VRE("VRE")
}

enum class CaseType(val basexName: List<String>) {
    AMBULANT(listOf("A", "AMBULANT")),
    STATIONAER(listOf("S", "STATIONAER")),
    TEILSTATIONAER(listOf("TS", "TEILSTATIONAER")),
    NACHSTATIONAER(listOf("NS", "NACHSTATIONAER"));
    //TODO: VORSTATIONAER ???
}




@Serializable
data class CacheData(
    @SerialName("meta") val metadata: CacheMetadata,
    @SerialName("germdata") val germCache: MutableList<GermInfo>,
    @SerialName("globaldata") var globalCache: GlobalInfo
)

@Serializable
data class CacheMetadata(
    @SerialName("time_created") val timeCreated: String,
    @SerialName("time_updated") var timeUpdated: String,
    @SerialName("basex") val basex: BasexInfo,
    @SerialName("xquery_params") val xQueryParams: XQueryParams,
)

@Serializable
sealed class BasexInfo

@Serializable
@SerialName("rest")
data class RestConnectionInfo(
    @SerialName("server_url") val serverUrl: String,
    @SerialName("database_id") val databaseId: String,
) : BasexInfo()

@Serializable
@SerialName("local")
data class LocalBasexInfo(val directory: String) : BasexInfo()


@Serializable
data class GermInfo(
    @SerialName("type") val type: String,
    @SerialName("overview_entries") var overviewEntries: Map<CaseType, List<OverviewEntry>>? = null,
    @SerialName("case_list") var caseList: List<Map<String, String>>? = null,
    @SerialName("created") var created: String? = null //null means case list data is not yet retrieved
)

@Serializable
data class GlobalInfo(
    @SerialName("overview_entries") var overviewEntries: Map<CaseType, List<OverviewEntry>>? = null,
    @SerialName("overview_created") var created: String? = null, //null means overview data is not yet retrieved
) {
    fun clear() {
        overviewEntries = null
        created = null
    }
}

@Serializable
data class OverviewEntry(var title: String, var query: String, var data: String) {
    constructor(title: String, query: String, data: Number) : this(title, query, data.toString())
}
