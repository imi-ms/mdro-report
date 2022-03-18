package de.uni_muenster.imi.oegd.webapp

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CacheData(
    @SerialName("meta") val metadata: CacheMetadata,
    @SerialName("data") val germCache: MutableList<GermInfo>
)

@Serializable
data class CacheMetadata(
    @SerialName("time_created") val timeCreated: String,
    @SerialName("time_updated") var timeUpdated: String,
    @SerialName("server_url") val serverUrl: String,
    @SerialName("database_id") val databaseId: String
)

@Serializable
data class GermInfo(
    @SerialName("type") val type: String,
    @SerialName("overview_entries") var overviewEntries: List<OverviewEntry>? = null,
    @SerialName("overview_created") var overviewTimeCreated: String? = null, //null means overview data is not yet retrieved
    @SerialName("case_list") var caseList: List<Map<String, String>>? = null,
    @SerialName("case_list_created") var caseListTimeCreated: String? = null //null means case list data is not yet retrieved
)

@Serializable
data class OverviewEntry(val title: String, val query: String, val data: String)
