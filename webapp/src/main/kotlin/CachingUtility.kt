package de.uni_muenster.imi.oegd.webapp

import de.uni_muenster.imi.oegd.common.GermType
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


    fun cache(xQueryParams: XQueryParams, germ: GermInfo) {
        val cache = getOrCreateCache(xQueryParams)
        cache.metadata.timeUpdated = LocalDateTime.now().toString()
        cache.germCache.removeIf { it.type == germ.type }
        cache.germCache.add(germ)
        writeCache(cache)
    }

    fun cache(xQueryParams: XQueryParams, data: GlobalInfo) {
        val cache = getOrCreateCache(xQueryParams)
        cache.metadata.timeUpdated = LocalDateTime.now().toString()
        cache.globalCache = data
        writeCache(cache)
    }


    private fun getOrCreateCache(xQueryParams: XQueryParams) =
        if (!cacheExists(xQueryParams)) createCache(xQueryParams) else getCache(xQueryParams) ?: createCache(
            xQueryParams
        )


    fun clearGlobalInfoCache(xQueryParams: XQueryParams) {
        val cache = getOrCreateCache(xQueryParams)

        cache!!.metadata.timeUpdated = LocalDateTime.now().toString()
        cache.globalCache.clear()

        writeCache(cache)
    }

    fun clearGermInfo(xQueryParams: XQueryParams, germ: GermType) {
        val cache = getOrCreateCache(xQueryParams)

        cache!!.metadata.timeUpdated = LocalDateTime.now().toString()
        cache.germCache.removeIf { it.type == germ.germtype }

        writeCache(cache)
    }


    fun uploadExistingCache(cache: String) {
        File(cacheDirectory).mkdirs()
        val xQueryParams = Json.decodeFromString<CacheData>(cache).metadata.xQueryParams
        File(cacheDirectory, getCacheFileName(xQueryParams)).writeText(cache)
    }


    private fun createCache(xQueryParams: XQueryParams): CacheData {
        return CacheData(
            metadata = CacheMetadata(
                timeCreated = LocalDateTime.now().toString(),
                timeUpdated = LocalDateTime.now().toString(),
                basex = basexInfo,
                xQueryParams = xQueryParams,
            ),
            germCache = mutableListOf(),
            globalCache = GlobalInfo(),
        )
    }


    private fun cacheExists(xQueryParams: XQueryParams): Boolean {
        return File(cacheDirectory, getCacheFileName(xQueryParams)).exists()
    }

    private fun writeCache(cache: CacheData) {
        val json = Json.encodeToString(cache)
        File(cacheDirectory).mkdirs()
        File(
            cacheDirectory,
            getCacheFileName(cache.metadata.xQueryParams)
        ).writeText(json) //TODO: Add caching path as property
    }

    fun getCache(xQueryParams: XQueryParams): CacheData? {
        if (cacheExists(xQueryParams)) {
            return try {
                val json =
                    File(cacheDirectory, getCacheFileName(xQueryParams)).readText() //TODO: Add caching path as property
                Json.decodeFromString(json)
            } catch (e: Exception) {
                null
            }
        }
        return null
    }

    fun getGermForGermtype(xQueryParams: XQueryParams, germ: GermType): GermInfo? {
        return getCache(xQueryParams)?.germCache?.find { it.type == germ.germtype }
    }

    fun getGlobalInfo(xQueryParams: XQueryParams): GlobalInfo? {
        return getCache(xQueryParams)?.globalCache
    }

    private val cacheDirectory: String by lazy {
        val userCacheDir = AppDirsFactory.getInstance().getUserCacheDir("mdreport", "1.0", "IMI")!!
        log.info { "Using '$userCacheDir' as cache directory!" }
        userCacheDir
    }

    fun getCacheFileName(xQueryParams: XQueryParams): String {
        fun sanitizeFilename(inputName: String) = inputName.replace(Regex("[^a-zA-Z0-9_]"), "_")
        val prefix = if (basexInfo is RestConnectionInfo) {
            "${sanitizeFilename(basexInfo.serverUrl)}-${sanitizeFilename(basexInfo.databaseId)}"
        } else {
            basexInfo as LocalBasexInfo
            "local-${sanitizeFilename(basexInfo.directory)}"
        }
        return "$prefix--${xQueryParams.year}.mdreport"
    }

}

