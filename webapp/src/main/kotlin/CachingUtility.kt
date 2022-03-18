package de.uni_muenster.imi.oegd.webapp

import de.uni_muenster.imi.oegd.common.GermType
import de.uni_muenster.imi.oegd.common.GlobalData
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDateTime


fun MutableList<GermInfo>.findOrCreateByType(id: GermType): GermInfo {
    return find { it.type == id.germtype }
        ?: GermInfo(
            type = id.germtype,
        ).also { this.add(it) }
}


class CachingUtility() {

    @JvmName("cacheOverview")
    fun cache(germ: GermType, data: List<OverviewEntry>) {
        val cache = if (cacheExists()) getCache() else createCache()
        cache.metadata.timeUpdated = LocalDateTime.now().toString()
        cache.germCache.findOrCreateByType(germ).apply {
            overviewEntries = data
            overviewTimeCreated = LocalDateTime.now().toString()
        }
        writeCache(cache)
    }

    @JvmName("cacheCaseList")
    fun cache(germ: GermType, data: List<Map<String, String>>) {
        val cache = if (cacheExists()) getCache() else createCache()

        cache.metadata.timeUpdated = LocalDateTime.now().toString()
        cache.germCache.findOrCreateByType(germ).apply {
            caseList = data
            caseListTimeCreated = LocalDateTime.now().toString()
        }

        writeCache(cache)
    }

    fun clearCaseListCache(germ: GermType) {
        val cache = if (cacheExists()) getCache() else createCache()

        cache.metadata.timeUpdated = LocalDateTime.now().toString()
        cache.germCache.findOrCreateByType(germ).apply {
            caseList = null
            caseListTimeCreated = null
        }

        writeCache(cache)
    }

    fun clearOverviewCache(germ: GermType) {
        val cache = if (cacheExists()) getCache() else createCache()

        cache.metadata.timeUpdated = LocalDateTime.now().toString()
        cache.germCache.findOrCreateByType(germ).apply {
            overviewEntries = null
            overviewTimeCreated = null
        }

        writeCache(cache)
    }


    private fun createCache(): CacheData {
        return CacheData(
            metadata = CacheMetadata(
                LocalDateTime.now().toString(),
                LocalDateTime.now().toString(),
                GlobalData.url,
                GlobalData.database
            ),
            germCache = mutableListOf()
        )
    }


    private fun cacheExists(): Boolean {
        return File("${GlobalData.database}.mdreport").exists()
    }

    private fun writeCache(cache: CacheData) {
        val json = Json.encodeToString(cache)
        File("${GlobalData.database}.mdreport").writeText(json) //TODO: Add caching path as property
    }

    fun getCache(): CacheData {
        val json = File("${GlobalData.database}.mdreport").readText() //TODO: Add caching path as property
        return Json.decodeFromString(json)
    }

    fun getGermForGermtype(germ: GermType): GermInfo? {
        return getCache().germCache.find { it.type == germ.germtype }
    }


}

