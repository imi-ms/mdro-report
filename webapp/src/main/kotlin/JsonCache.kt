package de.uni_muenster.imi.oegd.webapp

import kotlinx.serialization.*

@Serializable
data class CacheData(
    @SerialName("meta") val metadata: CacheMetadata,
    @SerialName("data") val germCache: MutableList<GermCache>
)

@Serializable
data class CacheMetadata(
    @SerialName("time_created") val timeCreated: String,
    @SerialName("time_updated") var timeUpdated: String,
    @SerialName("server_url") val serverUrl: String,
    @SerialName("database_id") val databaseId: String
)

@Serializable
data class GermCache(
    @SerialName("type") val type: String,
    @SerialName("overview_entries") var overviewEntries: List<OverviewEntry>,
    @SerialName("overview_created") var overviewTimeCreated: String,
    @SerialName("case_list") var caseList: List<Map<String, String>>,
    @SerialName("case_list_created") var caseListTimeCreated: String
)