package de.uni_muenster.imi.oegd.webapp

import kotlinx.serialization.Serializable

@Serializable
data class OverviewEntry(val title: String, val query: String, val data: String)
