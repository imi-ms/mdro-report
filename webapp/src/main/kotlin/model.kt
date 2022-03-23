package de.uni_muenster.imi.oegd.webapp

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CacheData(
    @SerialName("meta") val metadata: CacheMetadata,
    @SerialName("germdata") val germCache: MutableList<GermInfo>,
    @SerialName("globaldata") val globalCache: GlobalInfo,
)

@Serializable
data class CacheMetadata(
    @SerialName("time_created") val timeCreated: String,
    @SerialName("time_updated") var timeUpdated: String,
    @SerialName("basex") val basex: BasexInfo,
)

@Serializable
sealed class BasexInfo {
}

@Serializable
@SerialName("rest")
data class RestConnectionInfo(
    @SerialName("server_url") val serverUrl: String,
    @SerialName("database_id") val databaseId: String,
) : BasexInfo()

@Serializable
@SerialName("local")
data class LocalBasexInfo(
    @SerialName("directory") val directory: String,
) : BasexInfo()


@Serializable
data class GermInfo(
    @SerialName("type") val type: String,
    @SerialName("overview_entries") var overviewEntries: List<OverviewEntry>? = null,
    @SerialName("overview_created") var overviewTimeCreated: String? = null, //null means overview data is not yet retrieved
    @SerialName("case_list") var caseList: List<Map<String, String>>? = null,
    @SerialName("case_list_created") var caseListTimeCreated: String? = null //null means case list data is not yet retrieved
)

@Serializable
data class GlobalInfo(
    @SerialName("overview_entries") var overviewEntries: List<OverviewEntry>? = null,
    @SerialName("overview_created") var overviewTimeCreated: String? = null, //null means overview data is not yet retrieved
) {
    fun clear() {
        overviewEntries = null
        overviewTimeCreated = null
    }
}

@Serializable
data class OverviewEntry(val title: String, val query: String, val data: String)
