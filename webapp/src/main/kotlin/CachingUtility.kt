package de.uni_muenster.imi.oegd.webapp

import de.uni_muenster.imi.oegd.common.GermType
import de.uni_muenster.imi.oegd.common.GlobalData
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import net.harawata.appdirs.AppDirsFactory
import java.io.File
import java.time.LocalDateTime


fun MutableList<GermInfo>.findOrCreateByType(id: GermType): GermInfo {
    return find { it.type == id.germtype }
        ?: GermInfo(
            type = id.germtype,
        ).also { this.add(it) }
}


class CachingUtility(private val basexInfo: BasexInfo) {
    private val log = KotlinLogging.logger { }


    @JvmName("cacheOverview")
    fun cache(germ: GermType, data: List<OverviewEntry>) {
        val cache = if (cacheExists()) getCache() else createCache()
        cache!!.metadata.timeUpdated = LocalDateTime.now().toString()
        cache.germCache.findOrCreateByType(germ).apply {
            overviewEntries = data
            overviewTimeCreated = LocalDateTime.now().toString()
        }
        writeCache(cache)
    }

    @JvmName("cacheCaseList")
    fun cache(germ: GermType, data: List<Map<String, String>>) {
        val cache = if (cacheExists()) getCache() else createCache()

        cache!!.metadata.timeUpdated = LocalDateTime.now().toString()
        cache.germCache.findOrCreateByType(germ).apply {
            caseList = data
            caseListTimeCreated = LocalDateTime.now().toString()
        }

        writeCache(cache)
    }

    fun cache(data: List<OverviewEntry>) {
        val cache = if (cacheExists()) getCache() else createCache()

        cache!!.metadata.timeUpdated = LocalDateTime.now().toString()
        cache.globalCache.apply {
            overviewEntries = data
            overviewTimeCreated = LocalDateTime.now().toString()
        }
        writeCache(cache)
    }

    fun clearCaseListCache(germ: GermType) {
        val cache = if (cacheExists()) getCache() else createCache()

        cache!!.metadata.timeUpdated = LocalDateTime.now().toString()
        cache.germCache.findOrCreateByType(germ).apply {
            caseList = null
            caseListTimeCreated = null
        }

        writeCache(cache)
    }

    fun clearOverviewCache(germ: GermType) {
        val cache = if (cacheExists()) getCache() else createCache()

        cache!!.metadata.timeUpdated = LocalDateTime.now().toString()
        cache.germCache.findOrCreateByType(germ).apply {
            overviewEntries = null
            overviewTimeCreated = null
        }

        writeCache(cache)
    }

    fun clearGlobalInfoCache() {
        val cache = if (cacheExists()) getCache() else createCache()

        cache!!.metadata.timeUpdated = LocalDateTime.now().toString()
        cache.globalCache.clear()

        writeCache(cache)
    }



    fun uploadExistingCache(cache: String) {
        File(cacheDirectory).mkdirs()
        File(cacheDirectory, cacheFilename).writeText(cache)
    }


    private fun createCache(): CacheData {
        return CacheData(
            metadata = CacheMetadata(
                timeCreated = LocalDateTime.now().toString(),
                timeUpdated = LocalDateTime.now().toString(),
                basex = basexInfo
            ),
            germCache = mutableListOf(),
            globalCache = GlobalInfo()
        )
    }


    private fun cacheExists(): Boolean {
        return File(cacheDirectory, cacheFilename).exists()
    }

    private fun writeCache(cache: CacheData) {
        val json = Json.encodeToString(cache)
        File(cacheDirectory).mkdirs()
        File(cacheDirectory, cacheFilename).writeText(json) //TODO: Add caching path as property
    }

    fun getCache(): CacheData? {
        if(cacheExists()) {
            val json = File(cacheDirectory, cacheFilename).readText() //TODO: Add caching path as property
            return try {
                Json.decodeFromString(json)
            } catch (e: Exception) {
                null
            }
        }
        return null
    }

    fun getGermForGermtype(germ: GermType): GermInfo? {
        return getCache()?.germCache?.find { it.type == germ.germtype }
    }

    fun getGlobalInfo(): GlobalInfo? {
        return getCache()?.globalCache
    }

    private val cacheDirectory: String by lazy {
        val userCacheDir = AppDirsFactory.getInstance().getUserCacheDir("mdreport", "1.0", "IMI")!!
        log.info { "Using '$userCacheDir' as cache directory!" }
        userCacheDir
    }

    val cacheFilename: String by lazy {
        fun sanitizeFilename(inputName: String) = inputName.replace(Regex("[^a-zA-Z0-9_]"), "_")
        "${sanitizeFilename(GlobalData.url)}-${sanitizeFilename(GlobalData.database)}.mdreport"
    }

}

