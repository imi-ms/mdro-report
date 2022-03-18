package de.uni_muenster.imi.oegd.webapp

import de.uni_muenster.imi.oegd.common.Germtype
import de.uni_muenster.imi.oegd.common.GlobalData
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDateTime

class CachingUtility() {

    @JvmName("cacheOverview")
    fun cache(germ: Germtype, data: List<OverviewEntry>) {
        var cache: CacheData
        if(cacheExists()) {
            cache = getCache()

            cache = if(cache.germCache.none { it.type == germ.germtype }) {
                addGermWithOverviewEntries(cache, germ, data)
            } else {
                addOverviewEntriesToExistingGermCache(cache, germ, data)
            }
        } else {
            cache = createCache()
            cache = addGermWithOverviewEntries(cache, germ, data)
        }
        writeCache(cache)
    }

    @JvmName("cacheCaseList")
    fun cache(germ: Germtype, data: List<Map<String, String>>) {
        var cache: CacheData
        if(cacheExists()) {
            cache = getCache()

            cache = if(cache.germCache.none { it.type == germ.germtype }) {
                addGermWithCaseList(cache, germ, data)
            } else {
                addCaseListToExistingGermCache(cache, germ, data)
            }
        } else {
            cache = createCache()
            cache = addGermWithCaseList(cache, germ, data)
        }
        writeCache(cache)
    }

    fun getOverviewEntryOrNull(germ: Germtype): List<OverviewEntry>? {
        if(cacheExists()){
            val cache = getCache()
            val germCache = getGermCacheForGermtype(cache, germ) ?: return null
            if(germCache.overviewEntries.isNotEmpty()) {
                return germCache.overviewEntries
            }
        }
        return null
    }

    fun getCaseListOrNull(germ: Germtype): List<Map<String, String>>? {
        if(cacheExists()){
            val cache = getCache()
            val germCache = getGermCacheForGermtype(cache, germ) ?: return null
            if(germCache.caseList.isNotEmpty()) {
                return germCache.caseList
            }
        }
        return null
    }

    private fun createCache(): CacheData {
        return CacheData(
            CacheMetadata(
                LocalDateTime.now().toString(),
                LocalDateTime.now().toString(),
                GlobalData.url,
                GlobalData.database
            ),
            mutableListOf()
        )
    }

    private fun addOverviewEntriesToExistingGermCache(
        cache: CacheData,
        germ: Germtype,
        overviewData: List<OverviewEntry>
    ): CacheData {
        for(germCache in cache.germCache) {
            if(germCache.type == germ.germtype) {
                germCache.overviewEntries = overviewData
                germCache.overviewTimeCreated = LocalDateTime.now().toString()
                cache.metadata.timeUpdated = LocalDateTime.now().toString()
                break
            }
        }
        return cache
    }

    private fun addCaseListToExistingGermCache(
        cache: CacheData,
        germ: Germtype,
        caseList: List<Map<String, String>>
    ): CacheData {
        for(germCache in cache.germCache) {
            if(germCache.type == germ.germtype) {
                germCache.caseList = caseList
                germCache.caseListTimeCreated = LocalDateTime.now().toString()
                cache.metadata.timeUpdated = LocalDateTime.now().toString()
                break
            }
        }
        return cache
    }

    private fun addGermWithOverviewEntries(
        cache: CacheData,
        germ: Germtype,
        overviewData: List<OverviewEntry>
    ): CacheData {
        cache.germCache.add(
            GermCache(
                type = germ.germtype,
                overviewEntries = overviewData,
                overviewTimeCreated = LocalDateTime.now().toString(),
                caseList = listOf(),
                caseListTimeCreated = ""
            )
        )

        cache.metadata.timeUpdated = LocalDateTime.now().toString()
        return cache
    }

    private fun addGermWithCaseList(
        cache: CacheData,
        germ: Germtype,
        caseList: List<Map<String, String>>
    ): CacheData {
        cache.germCache.add(
            GermCache(
                type = germ.germtype,
                overviewEntries = listOf(),
                overviewTimeCreated = "",
                caseList = caseList,
                caseListTimeCreated = LocalDateTime.now().toString()
            )
        )
        cache.metadata.timeUpdated = LocalDateTime.now().toString()
        return cache
    }

    private fun cacheExists(): Boolean {
        return File("${GlobalData.database}.mdreport").exists() //TODO: Add caching path as property
    }

    private fun writeCache(cache: CacheData) {
        val json = Json.encodeToString(cache)
        File("${GlobalData.database}.mdreport").writeText(json) //TODO: Add caching path as property
    }

    private fun getCache(): CacheData {
        val json = File("${GlobalData.database}.mdreport").readText() //TODO: Add caching path as property
        return Json.decodeFromString(json)
    }

    private fun getGermCacheForGermtype(cache: CacheData, germ: Germtype): GermCache? {
        try {
            return cache.germCache.filter { it.type == germ.germtype }[0] //Only once in cache if exists
        } catch (_: Exception) { }
        return null
    }


}

